package com.muzima.messaging.sqlite.database.models;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;

import com.muzima.R;
import com.muzima.messaging.sqlite.database.MmsSmsColumns;
import com.muzima.messaging.sqlite.database.SmsDatabase;
import com.muzima.messaging.utils.ExpirationUtil;
import com.muzima.model.SignalRecipient;

public class ThreadRecord extends DisplayRecord {
    private @NonNull
    final Context context;
    private @Nullable
    final Uri snippetUri;
    private final long count;
    private final int unreadCount;
    private final int distributionType;
    private final boolean archived;
    private final long expiresIn;
    private final long lastSeen;

    public ThreadRecord(@NonNull Context context, @NonNull String body, @Nullable Uri snippetUri,
                        @NonNull SignalRecipient recipient, long date, long count, int unreadCount,
                        long threadId, int deliveryReceiptCount, int status, long snippetType,
                        int distributionType, boolean archived, long expiresIn, long lastSeen,
                        int readReceiptCount) {
        super(context, body, recipient, date, date, threadId, status, deliveryReceiptCount, snippetType, readReceiptCount);
        this.context = context.getApplicationContext();
        this.snippetUri = snippetUri;
        this.count = count;
        this.unreadCount = unreadCount;
        this.distributionType = distributionType;
        this.archived = archived;
        this.expiresIn = expiresIn;
        this.lastSeen = lastSeen;
    }

    public @Nullable
    Uri getSnippetUri() {
        return snippetUri;
    }

    @Override
    public SpannableString getDisplayBody() {
        if (isGroupUpdate()) {
            return emphasisAdded(context.getString(R.string.thread_record_group_updated));
        } else if (isGroupQuit()) {
            return emphasisAdded(context.getString(R.string.thread_record_left_the_group));
        } else if (isKeyExchange()) {
            return emphasisAdded(context.getString(R.string.conversation_list_item_key_exchange_message));
        } else if (SmsDatabase.Types.isFailedDecryptType(type)) {
            return emphasisAdded(context.getString(R.string.message_display_helper_bad_encrypted_message));
        } else if (SmsDatabase.Types.isNoRemoteSessionType(type)) {
            return emphasisAdded(context.getString(R.string.message_display_helper_message_encrypted_for_non_existing_session));
        } else if (SmsDatabase.Types.isEndSessionType(type)) {
            return emphasisAdded(context.getString(R.string.thread_record_secure_session_reset));
        } else if (MmsSmsColumns.Types.isLegacyType(type)) {
            return emphasisAdded(context.getString(R.string.message_record_message_encrypted_with_a_legacy_protocol_version_that_is_no_longer_supported));
        } else if (MmsSmsColumns.Types.isDraftMessageType(type)) {
            String draftText = context.getString(R.string.thread_record_draft);
            return emphasisAdded(draftText + " " + getBody(), 0, draftText.length());
        } else if (SmsDatabase.Types.isOutgoingCall(type)) {
            return emphasisAdded(context.getString(R.string.thread_record_called));
        } else if (SmsDatabase.Types.isIncomingCall(type)) {
            return emphasisAdded(context.getString(R.string.thread_record_called_you));
        } else if (SmsDatabase.Types.isMissedCall(type)) {
            return emphasisAdded(context.getString(R.string.thread_record_missed_call));
        } else if (SmsDatabase.Types.isJoinedType(type)) {
            return emphasisAdded(context.getString(R.string.thread_record_s_is_on_signal, getRecipient().toShortString()));
        } else if (SmsDatabase.Types.isExpirationTimerUpdate(type)) {
            int seconds = (int) (getExpiresIn() / 1000);
            if (seconds <= 0) {
                return emphasisAdded(context.getString(R.string.thread_record_disappearing_messages_disabled));
            }
            String time = ExpirationUtil.getExpirationDisplayValue(context, seconds);
            return emphasisAdded(context.getString(R.string.thread_record_disappearing_message_time_updated_to_s, time));
        } else if (SmsDatabase.Types.isIdentityUpdate(type)) {
            if (getRecipient().isGroupRecipient())
                return emphasisAdded(context.getString(R.string.thread_record_safety_number_changed));
            else
                return emphasisAdded(context.getString(R.string.thread_record_your_safety_number_with_s_has_changed, getRecipient().toShortString()));
        } else if (SmsDatabase.Types.isIdentityVerified(type)) {
            return emphasisAdded(context.getString(R.string.thread_record_you_marked_verified));
        } else if (SmsDatabase.Types.isIdentityDefault(type)) {
            return emphasisAdded(context.getString(R.string.thread_record_you_marked_unverified));
        } else {
            if (TextUtils.isEmpty(getBody())) {
                return new SpannableString(emphasisAdded(context.getString(R.string.thread_record_media_message)));
            } else {
                return new SpannableString(getBody());
            }
        }
    }

    private SpannableString emphasisAdded(String sequence) {
        return emphasisAdded(sequence, 0, sequence.length());
    }

    private SpannableString emphasisAdded(String sequence, int start, int end) {
        SpannableString spannable = new SpannableString(sequence);
        spannable.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC),
                start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannable;
    }

    public long getCount() {
        return count;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public long getDate() {
        return getDateReceived();
    }

    public boolean isArchived() {
        return archived;
    }

    public int getDistributionType() {
        return distributionType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public long getLastSeen() {
        return lastSeen;
    }
}
