package com.muzima.messaging.sqlite.database.models;

import android.content.Context;
import android.text.SpannableString;

import com.muzima.R;
import com.muzima.messaging.mms.SlideDeck;
import com.muzima.messaging.sqlite.database.MmsDatabase;
import com.muzima.messaging.sqlite.database.SmsDatabase.Status;
import com.muzima.messaging.sqlite.database.documents.IdentityKeyMismatch;
import com.muzima.messaging.sqlite.database.documents.NetworkFailure;
import com.muzima.model.SignalRecipient;

import java.util.Collections;
import java.util.LinkedList;

public class NotificationMmsMessageRecord extends MmsMessageRecord {

    private final byte[] contentLocation;
    private final long messageSize;
    private final long expiry;
    private final int status;
    private final byte[] transactionId;

    public NotificationMmsMessageRecord(Context context, long id, SignalRecipient conversationRecipient,
                                        SignalRecipient individualRecipient, int recipientDeviceId,
                                        long dateSent, long dateReceived, int deliveryReceiptCount,
                                        long threadId, byte[] contentLocation, long messageSize,
                                        long expiry, int status, byte[] transactionId, long mailbox,
                                        int subscriptionId, SlideDeck slideDeck, int readReceiptCount) {
        super(context, id, "", conversationRecipient, individualRecipient, recipientDeviceId,
                dateSent, dateReceived, threadId, Status.STATUS_NONE, deliveryReceiptCount, mailbox,
                new LinkedList<IdentityKeyMismatch>(), new LinkedList<NetworkFailure>(), subscriptionId,
                0, 0, slideDeck, readReceiptCount, null, Collections.emptyList(), false);

        this.contentLocation = contentLocation;
        this.messageSize = messageSize;
        this.expiry = expiry;
        this.status = status;
        this.transactionId = transactionId;
    }

    public byte[] getTransactionId() {
        return transactionId;
    }

    public int getStatus() {
        return this.status;
    }

    public byte[] getContentLocation() {
        return contentLocation;
    }

    public long getMessageSize() {
        return (messageSize + 1023) / 1024;
    }

    public long getExpiration() {
        return expiry * 1000;
    }

    @Override
    public boolean isOutgoing() {
        return false;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public boolean isPending() {
        return false;
    }

    @Override
    public boolean isMmsNotification() {
        return true;
    }

    @Override
    public boolean isMediaPending() {
        return true;
    }

    @Override
    public SpannableString getDisplayBody() {
        if (status == MmsDatabase.Status.DOWNLOAD_INITIALIZED) {
            return emphasisAdded(context.getString(R.string.general_multimedia_message));
        } else if (status == MmsDatabase.Status.DOWNLOAD_CONNECTING) {
            return emphasisAdded(context.getString(R.string.general_downloading_mms_message));
        } else {
            return emphasisAdded(context.getString(R.string.error_downloading_mms_message));
        }
    }
}
