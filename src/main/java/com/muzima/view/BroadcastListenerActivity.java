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
import com.muzima.R;

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

        String msg = getString(R.string.info_download_complete) + syncStatus;
        Log.i(TAG, msg);

        if (syncStatus == SyncStatusConstants.DOWNLOAD_ERROR) {
            msg = getString(R.string.error_data_from_server_download);
        } else if (syncStatus == SyncStatusConstants.AUTHENTICATION_ERROR) {
            msg = getString(R.string.error_authentication_error_occur);
        } else if (syncStatus == SyncStatusConstants.DELETE_ERROR) {
            msg = getString(R.string.error_data_from_local_repo_delete);
        } else if (syncStatus == SyncStatusConstants.SAVE_ERROR) {
            msg = getString(R.string.error_data_to_local_repo_save);
        } else if (syncStatus == SyncStatusConstants.CONNECTION_ERROR) {
            msg = getString(R.string.error_connection_error_occured_data_download);
        } else if (syncStatus == SyncStatusConstants.PARSING_ERROR) {
            msg = getString(R.string.error_parse_exception_thrown_data_fetch);
        } else if (syncStatus == SyncStatusConstants.LOAD_ERROR) {
            msg = getString(R.string.error_load_exception_thrown_data_load);
        } else if (syncStatus == SyncStatusConstants.UPLOAD_ERROR) {
            msg = getString(R.string.error_exception_thrown_data_upload);
        } else if(syncStatus == SyncStatusConstants.SUCCESS){
            int syncType = intent.getIntExtra(DataSyncServiceConstants.SYNC_TYPE, -1);
            int downloadCount = intent.getIntExtra(DataSyncServiceConstants.DOWNLOAD_COUNT_PRIMARY, 0);

            if(syncType == DataSyncServiceConstants.SYNC_FORMS){
                int deletedFormCount = intent.getIntExtra(DataSyncServiceConstants.DELETED_COUNT_PRIMARY,0);
                msg = String.format(getString(R.string.info_forms_downloaded), downloadCount);
                if(deletedFormCount > 0){
                    msg = String.format(getString(R.string.info_forms_download_delete) ,downloadCount, deletedFormCount);
                }
            }else if(syncType == DataSyncServiceConstants.SYNC_TEMPLATES){
                msg = String.format(getString(R.string.info_form_templates_and_related_concepts_download), downloadCount , intent.getIntExtra(DataSyncServiceConstants.DOWNLOAD_COUNT_SECONDARY, 0));
            } else if(syncType == DataSyncServiceConstants.SYNC_COHORTS){
                msg = String.format(getString(R.string.info_new_cohorts_download), downloadCount);
            } else if(syncType == DataSyncServiceConstants.SYNC_PATIENTS_FULL_DATA){
                int downloadCountSec = intent.getIntExtra(DataSyncServiceConstants.DOWNLOAD_COUNT_SECONDARY, 0);
                msg =String.format(getString(R.string.info_new_patients_download), downloadCount, downloadCountSec)+ getString(R.string.info_observations_and_encounters_still_download);
            } else if(syncType == DataSyncServiceConstants.SYNC_PATIENTS_ONLY){
                int downloadCountSec = intent.getIntExtra(DataSyncServiceConstants.DOWNLOAD_COUNT_SECONDARY, 0);
                msg =String.format(getString(R.string.info_patients_for_cohorts_download), downloadCount ,downloadCountSec);
            } else if(syncType == DataSyncServiceConstants.SYNC_OBSERVATIONS){
                msg = String.format(getString(R.string.info_new_observations_download), downloadCount);
            } else if(syncType == DataSyncServiceConstants.SYNC_ENCOUNTERS){
                msg= String.format(getString(R.string.info_new_encounters_download), downloadCount);
            } else if(syncType == DataSyncServiceConstants.SYNC_UPLOAD_FORMS){
                msg = getString(R.string.info_form_data_sucessful_upload);
            }else if(syncType == DataSyncServiceConstants.SYNC_REAL_TIME_UPLOAD_FORMS){
                msg = getString(R.string.info_forms_successful_real_time_data_upload);
            }
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
