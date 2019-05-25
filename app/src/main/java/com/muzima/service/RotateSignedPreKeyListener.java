package com.muzima.service;

import android.content.Context;
import android.content.Intent;

import com.muzima.MuzimaApplication;
import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.jobs.RotateSignedPreKeyJob;

import java.util.concurrent.TimeUnit;

public class RotateSignedPreKeyListener extends PersistentAlarmManagerListener {

    private static final long INTERVAL = TimeUnit.DAYS.toMillis(2);

    @Override
    protected long getNextScheduledExecutionTime(Context context) {
        return TextSecurePreferences.getSignedPreKeyRotationTime(context);
    }

    @Override
    protected long onAlarm(Context context, long scheduledTime) {
        if (scheduledTime != 0 && TextSecurePreferences.isPushRegistered(context)) {
            MuzimaApplication.getInstance(context)
                    .getJobManager()
                    .add(new RotateSignedPreKeyJob(context));
        }

        long nextTime = System.currentTimeMillis() + INTERVAL;
        TextSecurePreferences.setSignedPreKeyRotationTime(context, nextTime);

        return nextTime;
    }

    public static void schedule(Context context) {
        new RotateSignedPreKeyListener().onReceive(context, new Intent());
    }
}
