package com.muzima.messaging.adapters;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.muzima.messaging.Unbindable;
import com.muzima.messaging.contactshare.Contact;
import com.muzima.messaging.mms.GlideRequests;
import com.muzima.messaging.sqlite.database.models.MessageRecord;
import com.muzima.messaging.sqlite.database.models.MmsMessageRecord;
import com.muzima.model.SignalRecipient;

import org.whispersystems.libsignal.util.guava.Optional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public interface BindableConversationItem extends Unbindable {
    public void bind(@NonNull MessageRecord messageRecord,
                     @NonNull Optional<MessageRecord> previousMessageRecord,
                     @NonNull Optional<MessageRecord> nextMessageRecord,
                     @NonNull GlideRequests glideRequests,
                     @NonNull Locale locale,
                     @NonNull Set<MessageRecord> batchSelected,
                     @NonNull SignalRecipient recipients,
                     boolean pulseHighlight);

    MessageRecord getMessageRecord();

    void setEventListener(@Nullable EventListener listener);

    interface EventListener {
        void onQuoteClicked(MmsMessageRecord messageRecord);
        void onSharedContactDetailsClicked(@NonNull Contact contact, @NonNull View avatarTransitionView);
        void onAddToContactsClicked(@NonNull Contact contact);
        void onMessageSharedContactClicked(@NonNull List<SignalRecipient> choices);
        void onInviteSharedContactClicked(@NonNull List<SignalRecipient> choices);
    }
}
