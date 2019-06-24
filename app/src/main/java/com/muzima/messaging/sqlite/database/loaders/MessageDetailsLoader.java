package com.muzima.messaging.sqlite.database.loaders;

import android.content.Context;
import android.database.Cursor;

import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.MmsSmsDatabase;
import com.muzima.messaging.tasks.AbstractCursorLoader;

public class MessageDetailsLoader extends AbstractCursorLoader {
    private final String type;
    private final long messageId;

    public MessageDetailsLoader(Context context, String type, long messageId) {
        super(context);
        this.type = type;
        this.messageId = messageId;
    }

    @Override
    public Cursor getCursor() {
        switch (type) {
            case MmsSmsDatabase.SMS_TRANSPORT:
                return DatabaseFactory.getSmsDatabase(context).getMessageCursor(messageId);
            case MmsSmsDatabase.MMS_TRANSPORT:
                return DatabaseFactory.getMmsDatabase(context).getMessage(messageId);
            default:
                throw new AssertionError("no valid message type specified");
        }
    }
}
