package com.muzima.messaging.sqlite.database.models;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.muzima.messaging.mms.SlideDeck;
import com.muzima.messaging.sqlite.database.SignalAddress;

public class Quote {
    private final long id;
    private final SignalAddress author;
    private final String text;
    private final boolean missing;
    private final SlideDeck attachment;

    public Quote(long id, @NonNull SignalAddress author, @Nullable String text, boolean missing, @NonNull SlideDeck attachment) {
        this.id = id;
        this.author = author;
        this.text = text;
        this.missing = missing;
        this.attachment = attachment;
    }

    public long getId() {
        return id;
    }

    public @NonNull SignalAddress getAuthor() {
        return author;
    }

    public @Nullable String getText() {
        return text;
    }

    public boolean isOriginalMissing() {
        return missing;
    }

    public @NonNull SlideDeck getAttachment() {
        return attachment;
    }
}
