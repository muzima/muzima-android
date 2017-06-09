/*
 * Copyright (c) 2014 - 2017. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */
package com.muzima.view.progressdialog;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.muzima.utils.Constants;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.SyncIntent;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_TYPE;

public class ProgressDialogUpdateIntentService extends IntentService{
    public ProgressDialogUpdateIntentService(){
        super(Constants.ProgressDialogConstants.PROGRESS_UPDATE_ACTION);
    }

    @Override
    public void onHandleIntent(Intent intent){
        Intent progressUpdateIntent = intent;
        progressUpdateIntent.setAction(BroadcastListenerActivity.PROGRESS_UPDATE_ACTION);
        LocalBroadcastManager.getInstance(this).sendBroadcast(progressUpdateIntent);

    }
}
