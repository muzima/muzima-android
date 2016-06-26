/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import static com.muzima.utils.Constants.DataSyncServiceConstants;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants;

public abstract class BroadcastListenerActivity extends BaseFragmentActivity {
    private static final String TAG = BroadcastListenerActivity.class.getSimpleName();
    public static final String MESSAGE_SENT_ACTION = "com.muzima.MESSAGE_RECEIVED_ACTION";

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BroadcastListenerActivity.this.onReceive(context, intent);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(MESSAGE_SENT_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    protected void onReceive(Context context, Intent intent){
        displayToast(intent);
    }

    private void displayToast(Intent intent) {
        int syncStatus = intent.getIntExtra(DataSyncServiceConstants.SYNC_STATUS,
                SyncStatusConstants.UNKNOWN_ERROR);

        String msg = "Download Complete with status " + syncStatus;
        Log.i(TAG, msg);

        if (syncStatus == SyncStatusConstants.DOWNLOAD_ERROR) {
            msg = "An error occurred while downloading data form server";
        } else if (syncStatus == SyncStatusConstants.AUTHENTICATION_ERROR) {
            msg = "Authentication error occurred";
        } else if (syncStatus == SyncStatusConstants.DELETE_ERROR) {
            msg = "An error occurred while deleting data from local repo";
        } else if (syncStatus == SyncStatusConstants.SAVE_ERROR) {
            msg = "An error occurred while saving data to local repo";
        } else if (syncStatus == SyncStatusConstants.CONNECTION_ERROR) {
            msg = "Connection error occurred while downloading data";
        } else if (syncStatus == SyncStatusConstants.PARSING_ERROR) {
            msg = "Parse exception has been thrown while fetching data";
        } else if (syncStatus == SyncStatusConstants.LOAD_ERROR) {
            msg = "Load exception has been thrown while loading data";
        } else if (syncStatus == SyncStatusConstants.UPLOAD_ERROR) {
            msg = "Exception has been thrown while uploading data";
        } else if(syncStatus == SyncStatusConstants.SUCCESS){
            int syncType = intent.getIntExtra(DataSyncServiceConstants.SYNC_TYPE, -1);
            int downloadCount = intent.getIntExtra(DataSyncServiceConstants.DOWNLOAD_COUNT_PRIMARY, 0);
            msg = "Downloaded " + downloadCount;
            if(syncType == DataSyncServiceConstants.SYNC_FORMS){
                int deletedFormCount = intent.getIntExtra(DataSyncServiceConstants.DELETED_COUNT_PRIMARY,0);
                msg += " forms";
                if(deletedFormCount > 0){
                    msg += "  Deleted " + deletedFormCount + " forms";
                }
            }else if(syncType == DataSyncServiceConstants.SYNC_TEMPLATES){
                msg += " form templates and " + intent.getIntExtra(DataSyncServiceConstants.DOWNLOAD_COUNT_SECONDARY, 0) + " related concepts";
            } else if(syncType == DataSyncServiceConstants.SYNC_COHORTS){
                msg += " new cohorts";
            } else if(syncType == DataSyncServiceConstants.SYNC_PATIENTS_FULL_DATA){
                int downloadCountSec = intent.getIntExtra(DataSyncServiceConstants.DOWNLOAD_COUNT_SECONDARY, 0);
                msg += " new patients for " + downloadCountSec + " cohorts. Still downloading observations and encounters";
            } else if(syncType == DataSyncServiceConstants.SYNC_PATIENTS_ONLY){
                int downloadCountSec = intent.getIntExtra(DataSyncServiceConstants.DOWNLOAD_COUNT_SECONDARY, 0);
                msg += " patients for " + downloadCountSec + " cohorts.";
            } else if(syncType == DataSyncServiceConstants.SYNC_OBSERVATIONS){
                msg += " new observations";
            } else if(syncType == DataSyncServiceConstants.SYNC_ENCOUNTERS){
                msg += " new encounters";
            } else if(syncType == DataSyncServiceConstants.SYNC_UPLOAD_FORMS){
                msg = "Upload form data success.";
            }else if(syncType == DataSyncServiceConstants.SYNC_REAL_TIME_UPLOAD_FORMS){
                msg = "Real time upload of form data successful.";
            }
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
