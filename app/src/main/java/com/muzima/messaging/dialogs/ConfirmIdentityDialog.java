package com.muzima.messaging.dialogs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.messaging.crypto.storage.TextSecureIdentityKeyStore;
import com.muzima.messaging.sms.MessageSender;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.MmsDatabase;
import com.muzima.messaging.sqlite.database.MmsSmsDatabase;
import com.muzima.messaging.sqlite.database.PushDatabase;
import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.messaging.sqlite.database.SmsDatabase;
import com.muzima.messaging.sqlite.database.documents.IdentityKeyMismatch;
import com.muzima.messaging.sqlite.database.models.MessageRecord;
import com.muzima.messaging.utils.VerifySpan;
import com.muzima.model.SignalRecipient;
import com.muzima.utils.Base64;

import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.signalservice.api.messages.SignalServiceEnvelope;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos;

import java.io.IOException;

import static org.whispersystems.libsignal.SessionCipher.SESSION_LOCK;

public class ConfirmIdentityDialog extends AlertDialog {

    @SuppressWarnings("unused")
    private static final String TAG = ConfirmIdentityDialog.class.getSimpleName();

    private DialogInterface.OnClickListener callback;

    public ConfirmIdentityDialog(Context context,
                                 MessageRecord messageRecord,
                                 IdentityKeyMismatch mismatch) {
        super(context);

        SignalRecipient recipient = SignalRecipient.from(context, mismatch.getAddress(), false);
        String name = recipient.toShortString();
        String introduction = String.format(context.getString(R.string.ConfirmIdentityDialog_your_safety_number_with_s_has_changed), name, name);
        SpannableString spannableString = new SpannableString(introduction + " " +
                context.getString(R.string.ConfirmIdentityDialog_you_may_wish_to_verify_your_safety_number_with_this_contact));

        spannableString.setSpan(new VerifySpan(context, mismatch),
                introduction.length() + 1, spannableString.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        setTitle(name);
        setMessage(spannableString);

        setButton(AlertDialog.BUTTON_POSITIVE, context.getString(R.string.ConfirmIdentityDialog_accept), new AcceptListener(messageRecord, mismatch, recipient.getAddress()));
        setButton(AlertDialog.BUTTON_NEGATIVE, context.getString(android.R.string.cancel), new CancelListener());
    }

    @Override
    public void show() {
        super.show();
        ((TextView) this.findViewById(android.R.id.message))
                .setMovementMethod(LinkMovementMethod.getInstance());
    }

    public void setCallback(DialogInterface.OnClickListener callback) {
        this.callback = callback;
    }

    private class AcceptListener implements DialogInterface.OnClickListener {

        private final MessageRecord messageRecord;
        private final IdentityKeyMismatch mismatch;
        private final SignalAddress address;

        private AcceptListener(MessageRecord messageRecord, IdentityKeyMismatch mismatch, SignalAddress address) {
            this.messageRecord = messageRecord;
            this.mismatch = mismatch;
            this.address = address;
        }

        @SuppressLint("StaticFieldLeak")
        @Override
        public void onClick(DialogInterface dialog, int which) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    synchronized (SESSION_LOCK) {
                        SignalProtocolAddress mismatchAddress = new SignalProtocolAddress(address.toPhoneString(), 1);
                        TextSecureIdentityKeyStore identityKeyStore = new TextSecureIdentityKeyStore(getContext());

                        identityKeyStore.saveIdentity(mismatchAddress, mismatch.getIdentityKey(), true);
                    }

                    processMessageRecord(messageRecord);
                    processPendingMessageRecords(messageRecord.getThreadId(), mismatch);

                    return null;
                }

                private void processMessageRecord(MessageRecord messageRecord) {
                    if (messageRecord.isOutgoing()) processOutgoingMessageRecord(messageRecord);
                    else processIncomingMessageRecord(messageRecord);
                }

                private void processPendingMessageRecords(long threadId, IdentityKeyMismatch mismatch) {
                    MmsSmsDatabase mmsSmsDatabase = DatabaseFactory.getMmsSmsDatabase(getContext());
                    Cursor cursor = mmsSmsDatabase.getIdentityConflictMessagesForThread(threadId);
                    MmsSmsDatabase.Reader reader = mmsSmsDatabase.readerFor(cursor);
                    MessageRecord record;

                    try {
                        while ((record = reader.getNext()) != null) {
                            for (IdentityKeyMismatch recordMismatch : record.getIdentityKeyMismatches()) {
                                if (mismatch.equals(recordMismatch)) {
                                    processMessageRecord(record);
                                }
                            }
                        }
                    } finally {
                        if (reader != null)
                            reader.close();
                    }
                }

                private void processOutgoingMessageRecord(MessageRecord messageRecord) {
                    SmsDatabase smsDatabase = DatabaseFactory.getSmsDatabase(getContext());
                    MmsDatabase mmsDatabase = DatabaseFactory.getMmsDatabase(getContext());

                    if (messageRecord.isMms()) {
                        mmsDatabase.removeMismatchedIdentity(messageRecord.getId(),
                                mismatch.getAddress(),
                                mismatch.getIdentityKey());

                        if (messageRecord.getRecipient().isPushGroupRecipient()) {
                            MessageSender.resendGroupMessage(getContext(), messageRecord, mismatch.getAddress());
                        } else {
                            MessageSender.resend(getContext(), messageRecord);
                        }
                    } else {
                        smsDatabase.removeMismatchedIdentity(messageRecord.getId(),
                                mismatch.getAddress(),
                                mismatch.getIdentityKey());

                        MessageSender.resend(getContext(), messageRecord);
                    }
                }

                private void processIncomingMessageRecord(MessageRecord messageRecord) {
                    try {
                        PushDatabase pushDatabase = DatabaseFactory.getPushDatabase(getContext());
                        SmsDatabase smsDatabase = DatabaseFactory.getSmsDatabase(getContext());

                        smsDatabase.removeMismatchedIdentity(messageRecord.getId(),
                                mismatch.getAddress(),
                                mismatch.getIdentityKey());

                        boolean legacy = !messageRecord.isContentBundleKeyExchange();

                        SignalServiceEnvelope envelope = new SignalServiceEnvelope(SignalServiceProtos.Envelope.Type.PREKEY_BUNDLE_VALUE,
                                messageRecord.getIndividualRecipient().getAddress().toPhoneString(),
                                messageRecord.getRecipientDeviceId(),
                                messageRecord.getDateSent(),
                                legacy ? Base64.decode(messageRecord.getBody()) : null,
                                !legacy ? Base64.decode(messageRecord.getBody()) : null,
                                0, null);

                        long pushId = pushDatabase.insert(envelope);
//ToDO ++++++++ reenable pushDecryptJob
//                        MuzimaApplication.getInstance(getContext())
//                                .getJobManager()
//                                .add(new PushDecryptJob(getContext(), pushId, messageRecord.getId()));
                    } catch (IOException e) {
                        throw new AssertionError(e);
                    }
                }

            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            if (callback != null) callback.onClick(null, 0);
        }
    }

    private class CancelListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (callback != null) callback.onClick(null, 0);
        }
    }
}
