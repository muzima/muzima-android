package com.muzima.scheduler;

import android.annotation.SuppressLint;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.User;
import com.muzima.controller.NotificationController;
import com.muzima.service.DataSyncService;
import com.muzima.service.MuzimaSyncService;

import static com.muzima.utils.Constants.DataSyncServiceConstants.MuzimaJobSchedularConstants.JOB_INDICATOR_STOP;
import static com.muzima.utils.Constants.DataSyncServiceConstants.MuzimaJobSchedularConstants.MUZIMA_JOB_SCHEDULE_INTENT;
import static com.muzima.utils.Constants.DataSyncServiceConstants.MuzimaJobSchedularConstants.WORK_DURATION_KEY;

@SuppressLint("NewApi")
public class MuzimaJobScheduler extends JobService {

    final String TAG = getClass().getSimpleName();
    private NotificationController notificationController;
    private MuzimaSyncService muzimaSynService;
    private String authenticatedUserUuid;
    private User authenticatedUser;

    @Override
    public void onCreate() {
        super.onCreate();
        MuzimaApplication muzimaApplication = (MuzimaApplication) getApplicationContext();
        notificationController = muzimaApplication.getNotificationController();
        muzimaSynService = muzimaApplication.getMuzimaSyncService();
        authenticatedUser = muzimaApplication.getAuthenticatedUser();
        Log.e(TAG, "=========================== Downloading messages in Job==================");
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        Log.e("===","=========================== Downloading messages in Job==================");

        if (authenticatedUser == null){
            onStopJob(params);
        }else{
            //execute job
            handleBackgroundWork(params);
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.e(getClass().getSimpleName(),"mUzima Job Service stopped" + params.getJobId());
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Service destroyed");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("===","=========================== Downloading messages in Job==================");
        return START_NOT_STICKY;
    }

    private void handleBackgroundWork(JobParameters parameters){
        muzimaSynService.downloadNotifications(authenticatedUserUuid);
        //todo execute task here
    }

}
