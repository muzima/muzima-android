package com.muzima.messaging.sqlite.database.models;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.SpannableString;

import com.muzima.messaging.sqlite.database.MmsSmsColumns;
import com.muzima.messaging.sqlite.database.SmsDatabase;
import com.muzima.model.SignalRecipient;

public abstract class DisplayRecord {
    protected final Context context;
    protected final long type;

    private final SignalRecipient recipient;
    private final long dateSent;
    private final long dateReceived;
    private final long threadId;
    private final String body;
    private final int deliveryStatus;
    private final int deliveryReceiptCount;
    private final int readReceiptCount;

    DisplayRecord(Context context, String body, SignalRecipient recipient, long dateSent,
                  long dateReceived, long threadId, int deliveryStatus, int deliveryReceiptCount,
                  long type, int readReceiptCount) {
        this.context = context.getApplicationContext();
        this.threadId = threadId;
        this.recipient = recipient;
        this.dateSent = dateSent;
        this.dateReceived = dateReceived;
        this.type = type;
        this.body = body;
        this.deliveryReceiptCount = deliveryReceiptCount;
        this.readReceiptCount = readReceiptCount;
        this.deliveryStatus = deliveryStatus;
    }

    public @NonNull
    String getBody() {
        return body == null ? "" : body;
    }

    public boolean isFailed() {
        return
                MmsSmsColumns.Types.isFailedMessageType(type) ||
                        MmsSmsColumns.Types.isPendingSecureSmsFallbackType(type) ||
                        deliveryStatus >= SmsDatabase.Status.STATUS_FAILED;
    }

    public boolean isPending() {
        return MmsSmsColumns.Types.isPendingMessageType(type) &&
                !MmsSmsColumns.Types.isIdentityVerified(type) &&
                !MmsSmsColumns.Types.isIdentityDefault(type);
    }

    public boolean isOutgoing() {
        return MmsSmsColumns.Types.isOutgoingMessageType(type);
    }

    public abstract SpannableString getDisplayBody();

    public SignalRecipient getRecipient() {
        return recipient;
    }

    public long getDateSent() {
        return dateSent;
    }

    public long getDateReceived() {
        return dateReceived;
    }

    public long getThreadId() {
        return threadId;
    }

    public boolean isKeyExchange() {
        return SmsDatabase.Types.isKeyExchangeType(type);
    }

    public boolean isEndSession() {
        return SmsDatabase.Types.isEndSessionType(type);
    }

    public boolean isGroupUpdate() {
        return SmsDatabase.Types.isGroupUpdate(type);
    }

    public boolean isGroupQuit() {
        return SmsDatabase.Types.isGroupQuit(type);
    }

    public boolean isGroupAction() {
        return isGroupUpdate() || isGroupQuit();
    }

    public boolean isExpirationTimerUpdate() {
        return SmsDatabase.Types.isExpirationTimerUpdate(type);
    }

    public boolean isCallLog() {
        return SmsDatabase.Types.isCallLog(type);
    }

    public boolean isJoined() {
        return SmsDatabase.Types.isJoinedType(type);
    }

    public boolean isIncomingCall() {
        return SmsDatabase.Types.isIncomingCall(type);
    }

    public boolean isOutgoingCall() {
        return SmsDatabase.Types.isOutgoingCall(type);
    }

    public boolean isMissedCall() {
        return SmsDatabase.Types.isMissedCall(type);
    }

    public boolean isVerificationStatusChange() {
        return SmsDatabase.Types.isIdentityDefault(type) || SmsDatabase.Types.isIdentityVerified(type);
    }

    public int getDeliveryStatus() {
        return deliveryStatus;
    }

    public int getDeliveryReceiptCount() {
        return deliveryReceiptCount;
    }

    public int getReadReceiptCount() {
        return readReceiptCount;
    }

    public boolean isDelivered() {
        return (deliveryStatus >= SmsDatabase.Status.STATUS_COMPLETE &&
                deliveryStatus < SmsDatabase.Status.STATUS_PENDING) || deliveryReceiptCount > 0;
    }

    public boolean isRemoteRead() {
        return readReceiptCount > 0;
    }

    public boolean isPendingInsecureSmsFallback() {
        return SmsDatabase.Types.isPendingInsecureSmsFallbackType(type);
    }
}
