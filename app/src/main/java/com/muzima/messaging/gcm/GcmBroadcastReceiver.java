package com.muzima.messaging.gcm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.jobmanager.dependencies.InjectableType;
import com.muzima.messaging.jobmanager.requirements.NetworkRequirement;
import com.muzima.messaging.jobs.PushContentReceiveJob;
import com.muzima.messaging.jobs.PushNotificationReceiveJob;
import com.muzima.notifications.NotificationChannels;
import com.muzima.service.GenericForegroundService;
import com.muzima.utils.PowerManagerCompat;
import com.muzima.utils.concurrent.SignalExecutors;

import org.whispersystems.signalservice.api.SignalServiceMessageReceiver;
import org.whispersystems.signalservice.internal.util.Util;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

public class GcmBroadcastReceiver extends WakefulBroadcastReceiver implements InjectableType {

    private static final String TAG = GcmBroadcastReceiver.class.getSimpleName();

    private static final Executor MESSAGE_EXECUTOR = SignalExecutors.newCachedSingleThreadExecutor("GcmMessageProcessing");

    private static int activeCount = 0;

    @Inject
    SignalServiceMessageReceiver messageReceiver;

    @Override
    public void onReceive(Context context, Intent intent) {
        MuzimaApplication.getInstance(context).injectDependencies(this);

        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
        String messageType = gcm.getMessageType(intent);

        if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
            Log.i(TAG, "GCM message...");

            if (!TextSecurePreferences.isPushRegistered(context)) {
                Log.w(TAG, "Not push registered!");
                return;
            }

            String receiptData = intent.getStringExtra("receipt");

            if (!TextUtils.isEmpty(receiptData)) handleReceivedMessage(context, receiptData);
            else if (intent.hasExtra("notification")) handleReceivedNotification(context);
        }
    }

    private void handleReceivedMessage(Context context, String data) {
        MuzimaApplication.getInstance(context)
                .getJobManager()
                .add(new PushContentReceiveJob(context, data));
    }

    private void handleReceivedNotification(Context context) {
        if (!incrementActiveGcmCount()) {
            Log.i(TAG, "Skipping GCM processing -- there's already one enqueued.");
            return;
        }

        TextSecurePreferences.setNeedsMessagePull(context, true);

        long startTime = System.currentTimeMillis();
        BroadcastReceiver.PendingResult callback = goAsync();
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        boolean doze = PowerManagerCompat.isDeviceIdleMode(powerManager);
        boolean network = new NetworkRequirement(context).isPresent();

        final Object foregroundLock = new Object();
        final AtomicBoolean foregroundRunning = new AtomicBoolean(false);
        final AtomicBoolean taskCompleted = new AtomicBoolean(false);

        if (doze || !network) {
            Log.i(TAG, "Starting a foreground task because we may be operating in a constrained environment. Doze: " + doze + " Network: " + network);
            showForegroundNotification(context);
            foregroundRunning.set(true);
            callback.finish();
        }

        MESSAGE_EXECUTOR.execute(() -> {
            try {
                new PushNotificationReceiveJob(context).pullAndProcessMessages(messageReceiver, TAG, startTime);
            } catch (IOException e) {
                Log.i(TAG, "Failed to retrieve the envelope. Scheduling on JobManager.", e);
                 MuzimaApplication.getInstance(context)
                        .getJobManager()
                        .add(new PushNotificationReceiveJob(context));
            } finally {
                synchronized (foregroundLock) {
                    if (foregroundRunning.getAndSet(false)) {
                        GenericForegroundService.stopForegroundTask(context);
                    } else {
                        callback.finish();
                    }
                    taskCompleted.set(true);
                }

                decrementActiveGcmCount();
                Log.i(TAG, "Processing complete.");
            }
        });

        if (!foregroundRunning.get()) {
            new Thread("GcmForegroundServiceTimer") {
                @Override
                public void run() {
                    Util.sleep(4500);
                    synchronized (foregroundLock) {
                        if (!taskCompleted.get() && !foregroundRunning.getAndSet(true)) {
                            Log.i(TAG, "Starting a foreground task because the job is running long.");
                            showForegroundNotification(context);
                            callback.finish();
                        }
                    }
                }
            }.start();
        }
    }

    private void showForegroundNotification(@NonNull Context context) {
        GenericForegroundService.startForegroundTask(context,
                context.getString(R.string.general_retrieving_a_message),
                NotificationChannels.OTHER,
                R.drawable.ic_signal_downloading);
    }

    private static synchronized boolean incrementActiveGcmCount() {
        if (activeCount < 2) {
            activeCount++;
            return true;
        }
        return false;
    }

    private static synchronized void decrementActiveGcmCount() {
        activeCount--;
    }
}
