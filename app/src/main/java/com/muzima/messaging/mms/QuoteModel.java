package com.muzima.messaging.mms;

import android.support.annotation.Nullable;

import com.muzima.messaging.attachments.Attachment;
import com.muzima.messaging.sqlite.database.SignalAddress;

import java.util.List;

public class QuoteModel {
    private final long id;
    private final SignalAddress author;
    private final String text;
    private final boolean missing;
    private final List<Attachment> attachments;

    public QuoteModel(long id, SignalAddress author, String text, boolean missing, @Nullable List<Attachment> attachments) {
        this.id = id;
        this.author = author;
        this.text = text;
        this.missing = missing;
        this.attachments = attachments;
    }

    public long getId() {
        return id;
    }

    public SignalAddress getAuthor() {
        return author;
    }

    public String getText() {
        return text;
    }

    public boolean isOriginalMissing() {
        return missing;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }
}
