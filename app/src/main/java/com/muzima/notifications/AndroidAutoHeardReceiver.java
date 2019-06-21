package com.muzima.notifications;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.MessagingDatabase.MarkedMessageInfo;

import java.util.LinkedList;
import java.util.List;

public class AndroidAutoHeardReceiver extends BroadcastReceiver {

        public static final String TAG = AndroidAutoHeardReceiver.class.getSimpleName();
        public static final String HEARD_ACTION = "com.muzima.notifications.ANDROID_AUTO_HEARD";
        public static final String THREAD_IDS_EXTRA = "car_heard_thread_ids";
        public static final String NOTIFICATION_ID_EXTRA = "car_notification_id";

        @SuppressLint("StaticFieldLeak")
        @Override
        public void onReceive(final Context context, Intent intent)
        {
            if (!HEARD_ACTION.equals(intent.getAction()))
                return;

            final long[] threadIds = intent.getLongArrayExtra(THREAD_IDS_EXTRA);

            if (threadIds != null) {
                int notificationId = intent.getIntExtra(NOTIFICATION_ID_EXTRA, -1);
                NotificationManagerCompat.from(context).cancel(notificationId);

                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        List<MarkedMessageInfo> messageIdsCollection = new LinkedList<>();

                        for (long threadId : threadIds) {
                            Log.i(TAG, "Marking meassage as read: " + threadId);
                            List<MarkedMessageInfo> messageIds = DatabaseFactory.getThreadDatabase(context).setRead(threadId, true);

                            messageIdsCollection.addAll(messageIds);
                        }

                        MessageNotifier.updateNotification(context);
                        MarkReadReceiver.process(context, messageIdsCollection);

                        return null;
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }
}
