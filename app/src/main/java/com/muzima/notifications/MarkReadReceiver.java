package com.muzima.notifications;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.muzima.MuzimaApplication;
import com.muzima.messaging.jobs.MultiDeviceReadUpdateJob;
import com.muzima.messaging.jobs.SendReadReceiptJob;
import com.muzima.messaging.sqlite.database.DatabaseFactory;
import com.muzima.messaging.sqlite.database.MessagingDatabase;
import com.muzima.messaging.sqlite.database.MessagingDatabase.SyncMessageId;
import com.muzima.messaging.sqlite.database.MessagingDatabase.MarkedMessageInfo;
import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.service.ExpiringMessageManager;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MarkReadReceiver extends BroadcastReceiver {

    private static final String TAG = MarkReadReceiver.class.getSimpleName();
    public static final  String CLEAR_ACTION = "com.muzima.notifications.CLEAR";
    public static final  String THREAD_IDS_EXTRA = "thread_ids";
    public static final  String NOTIFICATION_ID_EXTRA = "notification_id";

    @SuppressLint("StaticFieldLeak")
    @Override
    public void onReceive(final Context context, Intent intent) {
        if (!CLEAR_ACTION.equals(intent.getAction()))
            return;

        final long[] threadIds = intent.getLongArrayExtra(THREAD_IDS_EXTRA);

        if (threadIds != null) {
            NotificationManagerCompat.from(context).cancel(intent.getIntExtra(NOTIFICATION_ID_EXTRA, -1));

            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    List<MarkedMessageInfo> messageIdsCollection = new LinkedList<>();

                    for (long threadId : threadIds) {
                        Log.i(TAG, "Marking as read: " + threadId);
                        List<MarkedMessageInfo> messageIds = DatabaseFactory.getThreadDatabase(context).setRead(threadId, true);
                        messageIdsCollection.addAll(messageIds);
                    }

                    process(context, messageIdsCollection);

                    MessageNotifier.updateNotification(context);

                    return null;
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    public static void process(@NonNull Context context, @NonNull List<MarkedMessageInfo> markedReadMessages) {
        if (markedReadMessages.isEmpty()) return;

        List<SyncMessageId> syncMessageIds = new LinkedList<>();

        for (MarkedMessageInfo messageInfo : markedReadMessages) {
            scheduleDeletion(context, messageInfo.getExpirationInfo());
            syncMessageIds.add(messageInfo.getSyncMessageId());
        }

        MuzimaApplication.getInstance(context)
                .getJobManager()
                .add(new MultiDeviceReadUpdateJob(context, syncMessageIds));

        Map<SignalAddress, List<SyncMessageId>> addressMap = Stream.of(markedReadMessages)
                .map(MarkedMessageInfo::getSyncMessageId)
                .collect(Collectors.groupingBy(SyncMessageId::getAddress));

        for (SignalAddress address : addressMap.keySet()) {
            List<Long> timestamps = Stream.of(addressMap.get(address)).map(MessagingDatabase.SyncMessageId::getTimetamp).toList();

            MuzimaApplication.getInstance(context)
                    .getJobManager()
                    .add(new SendReadReceiptJob(context, address, timestamps));
        }
    }

    private static void scheduleDeletion(Context context, MessagingDatabase.ExpirationInfo expirationInfo) {
        if (expirationInfo.getExpiresIn() > 0 && expirationInfo.getExpireStarted() <= 0) {
            ExpiringMessageManager expirationManager = MuzimaApplication.getInstance(context).getExpiringMessageManager();

            if (expirationInfo.isMms()) DatabaseFactory.getMmsDatabase(context).markExpireStarted(expirationInfo.getId());
            else DatabaseFactory.getSmsDatabase(context).markExpireStarted(expirationInfo.getId());

            expirationManager.scheduleDeletion(expirationInfo.getId(), expirationInfo.isMms(), expirationInfo.getExpiresIn());
        }
    }
}
