package com.muzima.messaging;

import android.support.annotation.NonNull;

import com.muzima.messaging.mms.GlideRequests;
import com.muzima.messaging.sqlite.database.models.ThreadRecord;

import java.util.Locale;
import java.util.Set;

public interface BindableConversationListItem extends Unbindable {

    public void bind(@NonNull ThreadRecord thread,
                     @NonNull GlideRequests glideRequests,
                     @NonNull Locale locale,
                     @NonNull Set<Long> typingThreads,
                     @NonNull Set<Long> selectedThreads, boolean batchMode);
}
