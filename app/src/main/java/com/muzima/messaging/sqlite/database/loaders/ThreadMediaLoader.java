package com.muzima.messaging.sqlite.database.loaders;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;

import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.messaging.tasks.AbstractCursorLoader;
import com.muzima.model.SignalRecipient;

public class ThreadMediaLoader extends AbstractCursorLoader {

    private final SignalAddress address;
    private final boolean gallery;

    public ThreadMediaLoader(@NonNull Context context, @NonNull SignalAddress address, boolean gallery) {
        super(context);
        this.address = address;
        this.gallery = gallery;
    }

    @Override
    public Cursor getCursor() {
        long threadId = DatabaseFactory.getThreadDatabase(getContext()).getThreadIdFor(SignalRecipient.from(getContext(), address, true));

        if (gallery)
            return DatabaseFactory.getMediaDatabase(getContext()).getGalleryMediaForThread(threadId);
        else
            return DatabaseFactory.getMediaDatabase(getContext()).getDocumentMediaForThread(threadId);
    }

    public SignalAddress getAddress() {
        return address;
    }

}
