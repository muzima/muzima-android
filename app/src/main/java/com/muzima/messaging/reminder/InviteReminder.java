package com.muzima.messaging.reminder;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.view.View;

import com.muzima.R;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.model.Reminder;
import com.muzima.model.SignalRecipient;

public class InviteReminder extends Reminder {

    public InviteReminder(final @NonNull Context context,
                          final @NonNull SignalRecipient recipient)
    {
        super(context.getString(R.string.general_invite_to_muzima),
                context.getString(R.string.general_invite_text, recipient.toShortString()));

        setDismissListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                new AsyncTask<Void,Void,Void>() {

                    @Override protected Void doInBackground(Void... params) {
                        DatabaseFactory.getRecipientDatabase(context).setSeenInviteReminder(recipient, true);
                        return null;
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });
    }
}
