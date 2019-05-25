package com.muzima.service;

import android.content.Context;
import android.content.Intent;

import com.muzima.MuzimaApplication;
import com.muzima.messaging.TextSecurePreferences;
import com.muzima.messaging.jobs.RotateCertificateJob;

import java.util.concurrent.TimeUnit;

public class RotateSenderCertificateListener extends PersistentAlarmManagerListener {

    private static final long INTERVAL = TimeUnit.DAYS.toMillis(1);

    @Override
    protected long getNextScheduledExecutionTime(Context context) {
        return TextSecurePreferences.getUnidentifiedAccessCertificateRotationTime(context);
    }

    @Override
    protected long onAlarm(Context context, long scheduledTime) {
        MuzimaApplication.getInstance(context)
                .getJobManager()
                .add(new RotateCertificateJob(context));

        long nextTime = System.currentTimeMillis() + INTERVAL;
        TextSecurePreferences.setUnidentifiedAccessCertificateRotationTime(context, nextTime);

        return nextTime;
    }

    public static void schedule(Context context) {
        new RotateSenderCertificateListener().onReceive(context, new Intent());
    }
}
