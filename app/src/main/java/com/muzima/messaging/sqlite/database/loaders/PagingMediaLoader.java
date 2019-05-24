package com.muzima.messaging.sqlite.database.loaders;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import com.muzima.messaging.attachments.AttachmentId;
import com.muzima.messaging.mms.PartAuthority;
import com.muzima.messaging.sqlite.database.AttachmentDatabase;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.utils.AsyncLoader;
import com.muzima.model.SignalRecipient;

public class PagingMediaLoader extends AsyncLoader<Pair<Cursor, Integer>> {

    @SuppressWarnings("unused")
    private static final String TAG = PagingMediaLoader.class.getSimpleName();

    private final SignalRecipient recipient;
    private final Uri uri;
    private final boolean leftIsRecent;

    public PagingMediaLoader(@NonNull Context context, @NonNull SignalRecipient recipient, @NonNull Uri uri, boolean leftIsRecent) {
        super(context);
        this.recipient = recipient;
        this.uri = uri;
        this.leftIsRecent = leftIsRecent;
    }

    @Nullable
    @Override
    public Pair<Cursor, Integer> loadInBackground() {
        long threadId = DatabaseFactory.getThreadDatabase(getContext()).getThreadIdFor(recipient);
        Cursor cursor = DatabaseFactory.getMediaDatabase(getContext()).getGalleryMediaForThread(threadId);

        while (cursor != null && cursor.moveToNext()) {
            AttachmentId attachmentId = new AttachmentId(cursor.getLong(cursor.getColumnIndexOrThrow(AttachmentDatabase.ROW_ID)), cursor.getLong(cursor.getColumnIndexOrThrow(AttachmentDatabase.UNIQUE_ID)));
            Uri attachmentUri = PartAuthority.getAttachmentDataUri(attachmentId);

            if (attachmentUri.equals(uri)) {
                return new Pair<>(cursor, leftIsRecent ? cursor.getPosition() : cursor.getCount() - 1 - cursor.getPosition());
            }
        }

        return null;
    }
}
