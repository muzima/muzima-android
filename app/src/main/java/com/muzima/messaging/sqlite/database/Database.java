package com.muzima.messaging.sqlite.database;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.support.annotation.NonNull;

import com.muzima.messaging.sqlite.database.helpers.SQLCipherOpenHelper;

import java.util.Set;

public abstract class Database {
    protected static final String ID_WHERE = "_id = ?";

    protected SQLCipherOpenHelper databaseHelper;
    protected final Context context;

    public Database(Context context, SQLCipherOpenHelper databaseHelper) {
        this.context        = context;
        this.databaseHelper = databaseHelper;
    }

    protected void notifyConversationListeners(Set<Long> threadIds) {
        for (long threadId : threadIds)
            notifyConversationListeners(threadId);
    }

    protected void notifyConversationListeners(long threadId) {
        context.getContentResolver().notifyChange(DatabaseContentProviders.Conversation.getUriForThread(threadId), null);
    }

    protected void notifyConversationListListeners() {
        context.getContentResolver().notifyChange(DatabaseContentProviders.ConversationList.CONTENT_URI, null);
    }

    protected void setNotifyConverationListeners(Cursor cursor, long threadId) {
        cursor.setNotificationUri(context.getContentResolver(), DatabaseContentProviders.Conversation.getUriForThread(threadId));
    }

    protected void setNotifyConverationListListeners(Cursor cursor) {
        cursor.setNotificationUri(context.getContentResolver(), DatabaseContentProviders.ConversationList.CONTENT_URI);
    }

    protected void registerAttachmentListeners(@NonNull ContentObserver observer) {
        context.getContentResolver().registerContentObserver(DatabaseContentProviders.Attachment.CONTENT_URI,
                true,
                observer);
    }

    protected void notifyAttachmentListeners() {
        context.getContentResolver().notifyChange(DatabaseContentProviders.Attachment.CONTENT_URI, null);
    }

    public void reset(SQLCipherOpenHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }
}
