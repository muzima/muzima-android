package com.muzima.messaging.sqlite.database.models;

import android.content.Context;
import android.text.SpannableString;

import com.muzima.R;
import com.muzima.messaging.sqlite.database.MmsSmsColumns;
import com.muzima.messaging.sqlite.database.SmsDatabase;
import com.muzima.messaging.sqlite.database.documents.IdentityKeyMismatch;
import com.muzima.model.SignalRecipient;

import java.util.LinkedList;
import java.util.List;

public class SmsMessageRecord extends MessageRecord {
    public SmsMessageRecord(Context context, long id,
                            String body, SignalRecipient recipient,
                            SignalRecipient individualRecipient,
                            int recipientDeviceId,
                            long dateSent, long dateReceived,
                            int deliveryReceiptCount,
                            long type, long threadId,
                            int status, List<IdentityKeyMismatch> mismatches,
                            int subscriptionId, long expiresIn, long expireStarted,
                            int readReceiptCount, boolean unidentified) {
        super(context, id, body, recipient, individualRecipient, recipientDeviceId,
                dateSent, dateReceived, threadId, status, deliveryReceiptCount, type,
                mismatches, new LinkedList<>(), subscriptionId,
                expiresIn, expireStarted, readReceiptCount, unidentified);
    }

    public long getType() {
        return type;
    }

    @Override
    public SpannableString getDisplayBody() {
        if (SmsDatabase.Types.isFailedDecryptType(type)) {
            return emphasisAdded(context.getString(R.string.warning_bad_encrypted_message));
        } else if (isCorruptedKeyExchange()) {
            return emphasisAdded(context.getString(R.string.warning_received_corrupted_key_exchange_message));
        } else if (isInvalidVersionKeyExchange()) {
            return emphasisAdded(context.getString(R.string.warning_received_key_exchange_message_for_invalid_protocol_version));
        } else if (MmsSmsColumns.Types.isLegacyType(type)) {
            return emphasisAdded(context.getString(R.string.emphasis_encrypted_with_a_legacy_protocol_version_that_is_no_longer_supported));
        } else if (isBundleKeyExchange()) {
            return emphasisAdded(context.getString(R.string.emphasis_received_message_with_new_safety_number_tap_to_process));
        } else if (isKeyExchange() && isOutgoing()) {
            return new SpannableString("");
        } else if (isKeyExchange() && !isOutgoing()) {
            return emphasisAdded(context.getString(R.string.emphasis_received_key_exchange_message_tap_to_process));
        } else if (SmsDatabase.Types.isDuplicateMessageType(type)) {
            return emphasisAdded(context.getString(R.string.warning_duplicate_message));
        } else if (SmsDatabase.Types.isNoRemoteSessionType(type)) {
            return emphasisAdded(context.getString(R.string.warning_message_encrypted_for_non_existing_session));
        } else if (isEndSession() && isOutgoing()) {
            return emphasisAdded(context.getString(R.string.emphasis_secure_session_reset));
        } else if (isEndSession()) {
            return emphasisAdded(context.getString(R.string.emphasis_secure_session_reset_by_s, getIndividualRecipient().toShortString()));
        } else {
            return super.getDisplayBody();
        }
    }

    @Override
    public boolean isMms() {
        return false;
    }

    @Override
    public boolean isMmsNotification() {
        return false;
    }
}
