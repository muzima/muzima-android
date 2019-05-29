package com.muzima.messaging;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.view.View;

import com.muzima.R;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.model.Reminder;

public class ShareReminder extends Reminder {

    public ShareReminder(final @NonNull Context context) {
        super(context.getString(R.string.reminder_header_share_title),
                context.getString(R.string.reminder_header_share_text));

        setDismissListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                TextSecurePreferences.setPromptedShare(context, true);
            }
        });

        setOkListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                TextSecurePreferences.setPromptedShare(context, true);
                context.startActivity(new Intent(context, InviteActivity.class));
            }
        });
    }

    public static boolean isEligible(final @NonNull Context context) {
        if (!TextSecurePreferences.isPushRegistered(context) ||
                TextSecurePreferences.hasPromptedShare(context))
        {
            return false;
        }

        Cursor cursor = null;
        try {
            cursor = DatabaseFactory.getThreadDatabase(context).getConversationList();
            return cursor.getCount() >= 1;
        } finally {
            if (cursor != null) cursor.close();
        }
    }
}
