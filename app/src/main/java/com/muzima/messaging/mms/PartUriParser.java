package com.muzima.messaging.mms;

import android.content.ContentUris;
import android.net.Uri;

import com.muzima.messaging.attachments.AttachmentId;

public class PartUriParser {
    private final Uri uri;

    public PartUriParser(Uri uri) {
        this.uri = uri;
    }

    public AttachmentId getPartId() {
        return new AttachmentId(getId(), getUniqueId());
    }

    private long getId() {
        return ContentUris.parseId(uri);
    }

    private long getUniqueId() {
        return Long.parseLong(uri.getPathSegments().get(1));
    }
}
