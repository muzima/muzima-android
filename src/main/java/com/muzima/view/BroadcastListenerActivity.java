package com.muzima.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import static com.muzima.utils.Constants.DataSyncServiceConstants.*;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.*;

public abstract class BroadcastListenerActivity extends BaseFragmentActivity {
    private static final String TAG = "BroadcastListenerActivity";
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
        int syncStatus = intent.getIntExtra(SYNC_STATUS, UNKNOWN_ERROR);

        String msg = "Download Complete with status " + syncStatus;
        Log.i(TAG, msg);

        if (syncStatus == DOWNLOAD_ERROR) {
            msg = "An error occurred while downloading data form server";
        } else if (syncStatus == AUTHENTICATION_ERROR) {
            msg = "Authentication error occurred";
        } else if (syncStatus == DELETE_ERROR) {
            msg = "An error occurred while deleting data from local repo";
        } else if (syncStatus == SAVE_ERROR) {
            msg = "An error occurred while saving data to local repo";
        } else if (syncStatus == CONNECTION_ERROR) {
            msg = "Connection error occurred while downloading data";
        } else if (syncStatus == PARSING_ERROR) {
            msg = "Parse exception has been thrown while fetching data";
        } else if (syncStatus == LOAD_ERROR) {
            msg = "Load exception has been thrown while loading data";
        } else if (syncStatus == UPLOAD_ERROR) {
            msg = "Exception has been thrown while uploading data";
        } else if(syncStatus == SUCCESS){
            int syncType = intent.getIntExtra(SYNC_TYPE, -1);
            int downloadCount = intent.getIntExtra(DOWNLOAD_COUNT_PRIMARY, 0);
            msg = "Downloaded " + downloadCount;
            if(syncType == SYNC_FORMS){
                msg += " forms";
            }else if(syncType == SYNC_TEMPLATES){
                msg += " form templates and " + intent.getIntExtra(DOWNLOAD_COUNT_SECONDARY, 0) + " related concepts";
            } else if(syncType == SYNC_COHORTS){
                msg += " new cohorts";
            } else if(syncType == SYNC_PATIENTS_FULL_DATA){
                int downloadCountSec = intent.getIntExtra(DOWNLOAD_COUNT_SECONDARY, 0);
                msg += " patients for " + downloadCountSec + " cohorts. Still downloading observations and encounters";
            } else if(syncType == SYNC_PATIENTS_ONLY){
                int downloadCountSec = intent.getIntExtra(DOWNLOAD_COUNT_SECONDARY, 0);
                msg += " patients for " + downloadCountSec + " cohorts.";
            } else if(syncType == SYNC_OBSERVATIONS){
                msg += " observations";
            } else if(syncType == SYNC_ENCOUNTERS){
                msg += " encounters";
            } else if(syncType == SYNC_UPLOAD_FORMS){
                msg = "Upload forms success.";
            }

        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
