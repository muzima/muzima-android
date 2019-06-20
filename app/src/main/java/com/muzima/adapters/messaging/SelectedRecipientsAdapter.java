package com.muzima.adapters.messaging;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.muzima.R;
import com.muzima.model.SignalRecipient;

import org.whispersystems.libsignal.util.guava.Optional;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class SelectedRecipientsAdapter extends BaseAdapter {
    @NonNull private Context context;
    @Nullable private OnRecipientDeletedListener onRecipientDeletedListener;
    @NonNull private List<RecipientWrapper> recipients;

    public SelectedRecipientsAdapter(@NonNull Context context) {
        this(context, Collections.<SignalRecipient>emptyList());
    }

    public SelectedRecipientsAdapter(@NonNull Context context,
                                     @NonNull Collection<SignalRecipient> existingRecipients) {
        this.context = context;
        this.recipients = wrapExistingMembers(existingRecipients);
    }

    public void add(@NonNull SignalRecipient recipient, boolean isPush) {
        if (!find(recipient).isPresent()) {
            RecipientWrapper wrapper = new RecipientWrapper(recipient, true, isPush);
            this.recipients.add(0, wrapper);
            notifyDataSetChanged();
        }
    }

    public Optional<RecipientWrapper> find(@NonNull SignalRecipient recipient) {
        RecipientWrapper found = null;
        for (RecipientWrapper wrapper : recipients) {
            if (wrapper.getRecipient().equals(recipient)) found = wrapper;
        }
        return Optional.fromNullable(found);
    }

    public void remove(@NonNull SignalRecipient recipient) {
        Optional<RecipientWrapper> match = find(recipient);
        if (match.isPresent()) {
            recipients.remove(match.get());
            notifyDataSetChanged();
        }
    }

    public Set<SignalRecipient> getRecipients() {
        final Set<SignalRecipient> recipientSet = new HashSet<>(recipients.size());
        for (RecipientWrapper wrapper : recipients) {
            recipientSet.add(wrapper.getRecipient());
        }
        return recipientSet;
    }

    @Override
    public int getCount() {
        return recipients.size();
    }

    public boolean hasNonPushMembers() {
        for (RecipientWrapper wrapper : recipients) {
            if (!wrapper.isPush()) return true;
        }
        return false;
    }

    @Override
    public Object getItem(int position) {
        return recipients.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View v, final ViewGroup parent) {
        if (v == null) {
            v = LayoutInflater.from(context).inflate(R.layout.selected_recipient_list_item, parent, false);
        }

        final RecipientWrapper recipientWrapper = (RecipientWrapper) getItem(position);
        final SignalRecipient signalRecipient = recipientWrapper.getRecipient();
        final boolean modifiable = recipientWrapper.isModifiable();

        TextView name = (TextView) v.findViewById(R.id.name);
        TextView phone = (TextView) v.findViewById(R.id.phone);
        ImageButton delete = (ImageButton) v.findViewById(R.id.delete);

        name.setText(signalRecipient.getName());
        phone.setText(signalRecipient.getAddress().serialize());
        delete.setVisibility(modifiable ? View.VISIBLE : View.GONE);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onRecipientDeletedListener != null) {
                    onRecipientDeletedListener.onRecipientDeleted(recipients.get(position).getRecipient());
                }
            }
        });

        return v;
    }

    private static List<RecipientWrapper> wrapExistingMembers(Collection<SignalRecipient> recipients) {
        final LinkedList<RecipientWrapper> wrapperList = new LinkedList<>();
        for (SignalRecipient recipient : recipients) {
            wrapperList.add(new RecipientWrapper(recipient, false, true));
        }
        return wrapperList;
    }

    public void setOnRecipientDeletedListener(@Nullable OnRecipientDeletedListener listener) {
        onRecipientDeletedListener = listener;
    }

    public interface OnRecipientDeletedListener {
        void onRecipientDeleted(SignalRecipient recipient);
    }

    public static class RecipientWrapper {
        private final SignalRecipient recipient;
        private final boolean modifiable;
        private final boolean push;

        public RecipientWrapper(final @NonNull SignalRecipient recipient,
                                final boolean modifiable,
                                final boolean push) {
            this.recipient = recipient;
            this.modifiable = modifiable;
            this.push = push;
        }

        public @NonNull
        SignalRecipient getRecipient() {
            return recipient;
        }

        public boolean isModifiable() {
            return modifiable;
        }

        public boolean isPush() {
            return push;
        }
    }
}
