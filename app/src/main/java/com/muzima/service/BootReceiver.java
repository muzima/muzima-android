package com.muzima.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.muzima.MuzimaApplication;
import com.muzima.messaging.jobs.PushNotificationReceiveJob;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        MuzimaApplication.getInstance(context).getJobManager().add(new PushNotificationReceiveJob(context));
    }
}
