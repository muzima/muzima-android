/*
 * Copyright (c) 2014 - 2017. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;
import com.muzima.R;

import static com.muzima.utils.Constants.DataSyncServiceConstants;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants;

public abstract class BroadcastListenerActivity extends BaseFragmentActivity {
    private static final String TAG = BroadcastListenerActivity.class.getSimpleName();
    public static final String MESSAGE_SENT_ACTION = "com.muzima.MESSAGE_RECEIVED_ACTION";
    public static final String PROGRESS_UPDATE_ACTION = "com.muzima.PROGRESS_UPDATE_ACTION";

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
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(PROGRESS_UPDATE_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    protected void onReceive(Context context, Intent intent){
        displayToast(intent);
    }

    private boolean isProgessUpdate(Intent intent){
        String action = intent.getAction();
        return action == PROGRESS_UPDATE_ACTION;
    }
    private void displayToast(Intent intent) {
        if(isProgessUpdate(intent)){
            //ToDo: put logic to log update status
            return;
        }
        int syncStatus = intent.getIntExtra(DataSyncServiceConstants.SYNC_STATUS,
                SyncStatusConstants.UNKNOWN_ERROR);

        String msg = getString(R.string.info_download_complete, syncStatus);
        Log.i(TAG, msg);

        if (syncStatus == SyncStatusConstants.DOWNLOAD_ERROR) {
            msg = getString(R.string.error_data_download);
        } else if (syncStatus == SyncStatusConstants.AUTHENTICATION_ERROR) {
            msg = getString(R.string.error_authentication_occur);
        } else if (syncStatus == SyncStatusConstants.DELETE_ERROR) {
            msg = getString(R.string.error_local_repo_data_delete);
        } else if (syncStatus == SyncStatusConstants.SAVE_ERROR) {
            msg = getString(R.string.error_data_save);
        } else if (syncStatus == SyncStatusConstants.LOCAL_CONNECTION_ERROR) {
            msg = getString(R.string.error_local_connection_unavailable);
        } else if (syncStatus == SyncStatusConstants.SERVER_CONNECTION_ERROR) {
            msg = getString(R.string.error_server_connection_unavailable);
        } else if (syncStatus == SyncStatusConstants.PARSING_ERROR) {
            msg = getString(R.string.error_parse_exception_data_fetch);
        } else if (syncStatus == SyncStatusConstants.LOAD_ERROR) {
            msg = getString(R.string.error_exception_data_load);
        } else if (syncStatus == SyncStatusConstants.UPLOAD_ERROR) {
            msg = getString(R.string.error_exception_data_upload);
        } else if(syncStatus == SyncStatusConstants.SUCCESS){
            int syncType = intent.getIntExtra(DataSyncServiceConstants.SYNC_TYPE, -1);
            int downloadCount = intent.getIntExtra(DataSyncServiceConstants.DOWNLOAD_COUNT_PRIMARY, 0);

            if(syncType == DataSyncServiceConstants.SYNC_FORMS){
                int deletedFormCount = intent.getIntExtra(DataSyncServiceConstants.DELETED_COUNT_PRIMARY,0);
                msg = getString(R.string.info_form_downloaded, downloadCount);
                if(deletedFormCount > 0){
                    msg = getString(R.string.info_form_download_delete, downloadCount, deletedFormCount);
                }
            } else if(syncType == DataSyncServiceConstants.SYNC_TEMPLATES){
                msg = getString(R.string.info_form_template_concept_download, downloadCount , intent.getIntExtra(DataSyncServiceConstants.DOWNLOAD_COUNT_SECONDARY, 0));
            } else if(syncType == DataSyncServiceConstants.SYNC_COHORTS){
                msg = getString(R.string.info_new_cohort_download, downloadCount);
            } else if(syncType == DataSyncServiceConstants.SYNC_PATIENTS_FULL_DATA){
                int downloadCountSec = intent.getIntExtra(DataSyncServiceConstants.DOWNLOAD_COUNT_SECONDARY, 0);
                msg =getString(R.string.info_cohort_new_patient_download, downloadCount, downloadCountSec)+ getString(R.string.info_patient_data_download);
            } else if(syncType == DataSyncServiceConstants.SYNC_PATIENTS_ONLY){
                int downloadCountSec = intent.getIntExtra(DataSyncServiceConstants.DOWNLOAD_COUNT_SECONDARY, 0);
                msg =getString(R.string.info_cohort_patient_download, downloadCount ,downloadCountSec);
            } else if(syncType == DataSyncServiceConstants.SYNC_OBSERVATIONS){
                msg = getString(R.string.info_new_observation_download, downloadCount);
            } else if(syncType == DataSyncServiceConstants.SYNC_ENCOUNTERS){
                msg= getString(R.string.info_new_encounter_download, downloadCount);
            } else if(syncType == DataSyncServiceConstants.SYNC_UPLOAD_FORMS){
                msg = getString(R.string.info_form_data_upload_sucess);
            } else if(syncType == DataSyncServiceConstants.SYNC_REAL_TIME_UPLOAD_FORMS){
                msg = getString(R.string.info_real_time_upload_success);
            } else if(syncType == DataSyncServiceConstants.SYNC_NOTIFICATIONS){
                msg = getString(R.string.info_notification_download, downloadCount);
            }
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
