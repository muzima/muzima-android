package com.muzima.messaging.sqlite.database.models;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.SpannableString;

import com.muzima.R;
import com.muzima.messaging.contactshare.Contact;
import com.muzima.messaging.mms.SlideDeck;
import com.muzima.messaging.sqlite.database.MmsDatabase;
import com.muzima.messaging.sqlite.database.SmsDatabase;
import com.muzima.messaging.sqlite.database.documents.IdentityKeyMismatch;
import com.muzima.messaging.sqlite.database.documents.NetworkFailure;
import com.muzima.model.SignalRecipient;

import java.util.List;

public class MediaMmsMessageRecord extends MmsMessageRecord {
    private final static String TAG = MediaMmsMessageRecord.class.getSimpleName();

    private final Context context;
    private final int partCount;

    public MediaMmsMessageRecord(Context context, long id, SignalRecipient conversationRecipient,
                                 SignalRecipient individualRecipient, int recipientDeviceId,
                                 long dateSent, long dateReceived, int deliveryReceiptCount,
                                 long threadId, String body,
                                 @NonNull SlideDeck slideDeck,
                                 int partCount, long mailbox,
                                 List<IdentityKeyMismatch> mismatches,
                                 List<NetworkFailure> failures, int subscriptionId,
                                 long expiresIn, long expireStarted, int readReceiptCount,
                                 @Nullable Quote quote, @Nullable List<Contact> contacts,
                                 boolean unidentified) {
        super(context, id, body, conversationRecipient, individualRecipient, recipientDeviceId, dateSent,
                dateReceived, threadId, SmsDatabase.Status.STATUS_NONE, deliveryReceiptCount, mailbox, mismatches, failures,
                subscriptionId, expiresIn, expireStarted, slideDeck, readReceiptCount, quote, contacts, unidentified);

        this.context = context.getApplicationContext();
        this.partCount = partCount;
    }

    public int getPartCount() {
        return partCount;
    }

    @Override
    public boolean isMmsNotification() {
        return false;
    }

    @Override
    public SpannableString getDisplayBody() {
        if (MmsDatabase.Types.isFailedDecryptType(type)) {
            return emphasisAdded(context.getString(R.string.mms_message_record_bad_encrypted_mms_message));
        } else if (MmsDatabase.Types.isDuplicateMessageType(type)) {
            return emphasisAdded(context.getString(R.string.sms_message_record_duplicate_message));
        } else if (MmsDatabase.Types.isNoRemoteSessionType(type)) {
            return emphasisAdded(context.getString(R.string.mms_message_record_mms_message_encrypted_for_non_existing_session));
        } else if (isLegacyMessage()) {
            return emphasisAdded(context.getString(R.string.message_record_message_encrypted_with_a_legacy_protocol_version_that_is_no_longer_supported));
        }

        return super.getDisplayBody();
    }
}
