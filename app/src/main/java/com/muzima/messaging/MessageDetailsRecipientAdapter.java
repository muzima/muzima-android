package com.muzima.messaging;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;

import com.muzima.R;
import com.muzima.messaging.mms.GlideRequests;
import com.muzima.messaging.sqlite.database.models.MessageRecord;
import com.muzima.messaging.utils.Conversions;
import com.muzima.model.SignalRecipient;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class MessageDetailsRecipientAdapter extends BaseAdapter implements AbsListView.RecyclerListener {

    private final Context context;
    private final GlideRequests glideRequests;
    private final MessageRecord record;
    private final List<RecipientDeliveryStatus> members;
    private final boolean isPushGroup;

    public MessageDetailsRecipientAdapter(@NonNull Context context, @NonNull GlideRequests glideRequests,
                                          @NonNull MessageRecord record, @NonNull List<RecipientDeliveryStatus> members,
                                          boolean isPushGroup) {
        this.context = context;
        this.glideRequests = glideRequests;
        this.record = record;
        this.isPushGroup = isPushGroup;
        this.members = members;
    }

    @Override
    public int getCount() {
        return members.size();
    }

    @Override
    public Object getItem(int position) {
        return members.get(position);
    }

    @Override
    public long getItemId(int position) {
        try {
            return Conversions.byteArrayToLong(MessageDigest.getInstance("SHA1").digest(members.get(position).recipient.getAddress().serialize().getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.message_recipient_list_item, parent, false);
        }

        RecipientDeliveryStatus member = members.get(position);

        ((MessageRecipientListItem) convertView).set(glideRequests, record, member, isPushGroup);
        return convertView;
    }

    @Override
    public void onMovedToScrapHeap(View view) {
        ((MessageRecipientListItem) view).unbind();
    }


    public static class RecipientDeliveryStatus {

        public enum Status {
            UNKNOWN, PENDING, SENT, DELIVERED, READ
        }

        private final SignalRecipient recipient;
        private final Status deliveryStatus;
        private final boolean isUnidentified;
        private final long timestamp;

        public RecipientDeliveryStatus(SignalRecipient recipient, Status deliveryStatus, boolean isUnidentified, long timestamp) {
            this.recipient = recipient;
            this.deliveryStatus = deliveryStatus;
            this.isUnidentified = isUnidentified;
            this.timestamp = timestamp;
        }

        public Status getDeliveryStatus() {
            return deliveryStatus;
        }

        public boolean isUnidentified() {
            return isUnidentified;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public SignalRecipient getRecipient() {
            return recipient;
        }

    }
}
