package com.muzima.messaging.jobs;

import android.content.Context;
import android.support.annotation.NonNull;

import com.muzima.MuzimaApplication;
import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.attachments.Attachment;
import com.muzima.messaging.crypto.UnidentifiedAccessUtil;
import com.muzima.messaging.exceptions.InsecureFallbackApprovalException;
import com.muzima.messaging.exceptions.MmsException;
import com.muzima.messaging.exceptions.RetryLaterException;
import com.muzima.messaging.exceptions.UndeliverableMessageException;
import com.muzima.messaging.jobmanager.SafeData;
import com.muzima.messaging.jobmanager.dependencies.InjectableType;
import com.muzima.messaging.mms.MediaConstraints;
import com.muzima.messaging.mms.OutgoingMediaMessage;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.MmsDatabase;
import com.muzima.messaging.sqlite.database.NoSuchMessageException;
import com.muzima.messaging.sqlite.database.RecipientDatabase;
import com.muzima.messaging.sqlite.database.RecipientDatabase.UnidentifiedAccessMode;
import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.model.SignalRecipient;
import com.muzima.service.ExpiringMessageManager;

import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceMessageSender;
import org.whispersystems.signalservice.api.crypto.UntrustedIdentityException;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachment;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;
import org.whispersystems.signalservice.api.messages.shared.SharedContact;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;
import org.whispersystems.signalservice.api.push.exceptions.UnregisteredUserException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import androidx.work.Data;
import androidx.work.WorkerParameters;

public class PushMediaSendJob extends PushSendJob implements InjectableType {

    private static final long serialVersionUID = 1L;

    private static final String TAG = PushMediaSendJob.class.getSimpleName();

    private static final String KEY_MESSAGE_ID = "message_id";

    @Inject
    transient SignalServiceMessageSender messageSender;

    private long messageId;

    public PushMediaSendJob(@NonNull Context context, @NonNull WorkerParameters workerParameters) {
        super(context, workerParameters);
    }

    public PushMediaSendJob(Context context, long messageId, SignalAddress destination) {
        super(context, constructParameters(destination));
        this.messageId = messageId;
    }

    @Override
    protected void initialize(@NonNull SafeData data) {
        messageId = data.getLong(KEY_MESSAGE_ID);
    }

    @Override
    protected @NonNull
    Data serialize(@NonNull Data.Builder dataBuilder) {
        return dataBuilder.putLong(KEY_MESSAGE_ID, messageId).build();
    }

    @Override
    protected void onAdded() {
        DatabaseFactory.getMmsDatabase(context).markAsSending(messageId);
    }

    @Override
    public void onPushSend()
            throws RetryLaterException, MmsException, NoSuchMessageException,
            UndeliverableMessageException {
        ExpiringMessageManager expirationManager = MuzimaApplication.getInstance(context).getExpiringMessageManager();
        MmsDatabase database = DatabaseFactory.getMmsDatabase(context);
        OutgoingMediaMessage message = database.getOutgoingMessage(messageId);

        if (database.isSent(messageId)) {
            warn(TAG, "Message " + messageId + " was already sent. Ignoring.");
            return;
        }

        try {
            log(TAG, "Sending message: " + messageId);

            SignalRecipient recipient = message.getRecipient().resolve();
            byte[] profileKey = recipient.getProfileKey();
            RecipientDatabase.UnidentifiedAccessMode accessMode = recipient.getUnidentifiedAccessMode();

            boolean unidentified = deliver(message);

            database.markAsSent(messageId, true);
            markAttachmentsUploaded(messageId, message.getAttachments());
            database.markUnidentified(messageId, unidentified);

            if (TextSecurePreferences.isUnidentifiedDeliveryEnabled(context)) {
                if (unidentified && accessMode == UnidentifiedAccessMode.UNKNOWN && profileKey == null) {
                    log(TAG, "Marking recipient as UD-unrestricted following a UD send.");
                    DatabaseFactory.getRecipientDatabase(context).setUnidentifiedAccessMode(recipient, UnidentifiedAccessMode.UNRESTRICTED);
                } else if (unidentified && accessMode == UnidentifiedAccessMode.UNKNOWN) {
                    log(TAG, "Marking recipient as UD-enabled following a UD send.");
                    DatabaseFactory.getRecipientDatabase(context).setUnidentifiedAccessMode(recipient, UnidentifiedAccessMode.ENABLED);
                } else if (!unidentified && accessMode != UnidentifiedAccessMode.DISABLED) {
                    log(TAG, "Marking recipient as UD-disabled following a non-UD send.");
                    DatabaseFactory.getRecipientDatabase(context).setUnidentifiedAccessMode(recipient, UnidentifiedAccessMode.DISABLED);
                }
            }

            if (message.getExpiresIn() > 0 && !message.isExpirationUpdate()) {
                database.markExpireStarted(messageId);
                expirationManager.scheduleDeletion(messageId, true, message.getExpiresIn());
            }

            log(TAG, "Sent message: " + messageId);

        } catch (InsecureFallbackApprovalException ifae) {
            warn(TAG, "Failure", ifae);
            database.markAsPendingInsecureSmsFallback(messageId);
            notifyMediaMessageDeliveryFailed(context, messageId);
            MuzimaApplication.getInstance(context).getJobManager().add(new DirectoryRefreshJob(context, false));
        } catch (UntrustedIdentityException uie) {
            warn(TAG, "Failure", uie);
            database.addMismatchedIdentity(messageId, SignalAddress.fromSerialized(uie.getE164Number()), uie.getIdentityKey());
            database.markAsSentFailed(messageId);
        }
    }

    @Override
    public boolean onShouldRetry(Exception exception) {
        if (exception instanceof RetryLaterException) return true;
        return false;
    }

    @Override
    public void onCanceled() {
        DatabaseFactory.getMmsDatabase(context).markAsSentFailed(messageId);
        notifyMediaMessageDeliveryFailed(context, messageId);
    }

    private boolean deliver(OutgoingMediaMessage message)
            throws RetryLaterException, InsecureFallbackApprovalException, UntrustedIdentityException,
            UndeliverableMessageException {
        if (message.getRecipient() == null) {
            throw new UndeliverableMessageException("No destination address.");
        }

        try {
            rotateSenderCertificateIfNecessary();

            SignalServiceAddress address = getPushAddress(message.getRecipient().getAddress());
            MediaConstraints mediaConstraints = MediaConstraints.getPushMediaConstraints();
            List<Attachment> scaledAttachments = scaleAndStripExifFromAttachments(mediaConstraints, message.getAttachments());
            List<SignalServiceAttachment> attachmentStreams = getAttachmentsFor(scaledAttachments);
            Optional<byte[]> profileKey = getProfileKey(message.getRecipient());
            Optional<SignalServiceDataMessage.Quote> quote = getQuoteFor(message);
            List<SharedContact> sharedContacts = getSharedContactsFor(message);
            SignalServiceDataMessage mediaMessage = SignalServiceDataMessage.newBuilder()
                    .withBody(message.getBody())
                    .withAttachments(attachmentStreams)
                    .withTimestamp(message.getSentTimeMillis())
                    .withExpiration((int) (message.getExpiresIn() / 1000))
                    .withProfileKey(profileKey.orNull())
                    .withQuote(quote.orNull())
                    .withSharedContacts(sharedContacts)
                    .asExpirationUpdate(message.isExpirationUpdate())
                    .build();

            return messageSender.sendMessage(address, UnidentifiedAccessUtil.getAccessFor(context, message.getRecipient()), mediaMessage).getSuccess().isUnidentified();
        } catch (UnregisteredUserException e) {
            warn(TAG, e);
            throw new InsecureFallbackApprovalException(e);
        } catch (FileNotFoundException e) {
            warn(TAG, e);
            throw new UndeliverableMessageException(e);
        } catch (IOException e) {
            warn(TAG, e);
            throw new RetryLaterException(e);
        }
    }

}
