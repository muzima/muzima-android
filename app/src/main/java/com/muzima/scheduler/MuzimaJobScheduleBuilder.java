/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.scheduler;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;

import androidx.annotation.RequiresApi;

import android.widget.Toast;

import com.muzima.MuzimaApplication;
import com.muzima.R;

import static android.content.Context.JOB_SCHEDULER_SERVICE;
import static com.muzima.utils.Constants.DataSyncServiceConstants.MuzimaJobSchedulerConstants.MESSAGE_SYNC_JOB_ID;
import static com.muzima.utils.Constants.DataSyncServiceConstants.MuzimaJobSchedulerConstants.MUZIMA_JOB_PERIODIC;

public class MuzimaJobScheduleBuilder {
    private MuzimaApplication muzimaApplication;
    private Context context;

    public MuzimaJobScheduleBuilder(Context context) {
        this.muzimaApplication = (MuzimaApplication) context.getApplicationContext();
        this.context = context;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void schedulePeriodicBackgroundJob(int delay, boolean isManualSync) {
        if (isManualSync) {
            final Handler handler = new Handler();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (!isJobAlreadyScheduled(context))
                        handleScheduledPeriodicDataSyncJob();
                    else
                        Toast.makeText(context, context.getResources().getString(R.string.general_sync_service_already_running), Toast.LENGTH_LONG).show();
                }
            };
            handler.postDelayed(runnable, delay);
        } else {
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (preferences.getBoolean(context.getResources().getString(R.string.preference_real_time_sync), false)) {
                final Handler handler = new Handler();
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        if (!isJobAlreadyScheduled(context)) {
                            handleScheduledPeriodicDataSyncJob();
                        }
                        handler.postDelayed(this, MUZIMA_JOB_PERIODIC);
                    }
                };
                handler.postDelayed(runnable, delay);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static boolean isJobAlreadyScheduled(Context context) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);

        boolean hasBeenScheduled = false;

        for (JobInfo jobInfo : scheduler.getAllPendingJobs()) {
            if (jobInfo.getId() == MESSAGE_SYNC_JOB_ID) {
                hasBeenScheduled = true;
                break;
            }
        }

        return hasBeenScheduled;
    }

    private void handleScheduledPeriodicDataSyncJob() {
        ComponentName componentName = new ComponentName(context, MuzimaJobScheduler.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {

            JobInfo mUzimaJobInfo;

            mUzimaJobInfo = new JobInfo
                    .Builder(MESSAGE_SYNC_JOB_ID, componentName)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setOverrideDeadline(MUZIMA_JOB_PERIODIC)
                    .build();

            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);
            if (jobScheduler != null) {
                jobScheduler.schedule(mUzimaJobInfo);
            }
        }

    }

    private void cancelPeriodicBackgroundDataSyncJob() {
        JobScheduler jobScheduler = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            jobScheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);
            jobScheduler.cancel(MESSAGE_SYNC_JOB_ID);
            Toast.makeText(context, R.string.info_data_sync_cancelled, Toast.LENGTH_SHORT).show();

        }
    }
}
