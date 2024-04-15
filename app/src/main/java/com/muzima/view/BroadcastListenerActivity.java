/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view;


import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.controller.SetupConfigurationController;
import com.muzima.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static com.muzima.service.DataSyncService.hasOngoingSyncTasks;

import com.muzima.utils.Constants;

public abstract class BroadcastListenerActivity extends BaseAuthenticatedActivity {
    public static final String MESSAGE_SENT_ACTION = "com.muzima.MESSAGE_RECEIVED_ACTION";
    public static final String PROGRESS_UPDATE_ACTION = "com.muzima.PROGRESS_UPDATE_ACTION";
    public static final String SYNC_COMPLETED_ACTION = "com.muzima.SYNC_COMPLETED_ACTION";
    public static final String SYNC_STARTED_ACTION = "com.muzima.SYNC_STARTED_ACTION";
    private static  final List<String> dataSyncProgressLog = new ArrayList<>();
    private ArrayAdapter<String> dataSyncProgressMessageArrayAdapter;
    private static boolean isSyncRunning;
    private boolean syncErrorOccured = false;
    private ImageView imageView;
    private TextView titleTextView;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
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
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(SYNC_STARTED_ACTION));
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(SYNC_COMPLETED_ACTION));
        isSyncRunning = hasOngoingSyncTasks();
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
        return action.equals(PROGRESS_UPDATE_ACTION);
    }

    private boolean isSyncStartedIntent(Intent intent){
        String action = intent.getAction();
        return action.equals(SYNC_STARTED_ACTION);
    }

    private boolean isSyncCompletedIntent(Intent intent){
        String action = intent.getAction();
        return action.equals(SYNC_COMPLETED_ACTION);
    }
    private void displayToast(Intent intent) {
        if(isProgessUpdate(intent)){
            //ToDo: put logic to log update status
            return;
        }

        if(isSyncStartedIntent(intent)){
            isSyncRunning = true;
            if(dataSyncProgressLog != null) {
                dataSyncProgressLog.clear();
            }

            if(titleTextView != null){
                titleTextView.setText(R.string.info_sync_progress);
            }

            startProgressAnimation();
            updateSyncProgressWidgets(isSyncRunning);
            return;
        }

        int syncStatus = intent.getIntExtra(Constants.DataSyncServiceConstants.SYNC_STATUS,
                Constants.DataSyncServiceConstants.SyncStatusConstants.UNKNOWN_ERROR);


        if(isSyncCompletedIntent(intent)){
            isSyncRunning = false;

            if(titleTextView != null){
                titleTextView.setText(R.string.info_sync_completed);
            }
            stopProgressAnimation();
            updateSyncProgressWidgets(isSyncRunning);
            return;
        }

        String msg = intent.getStringExtra(Constants.DataSyncServiceConstants.SYNC_RESULT_MESSAGE);

        switch (syncStatus) {
            case Constants.DataSyncServiceConstants.SyncStatusConstants.DOWNLOAD_ERROR:
                msg = getString(R.string.error_data_download);
                syncErrorOccured = true;
                break;
            case Constants.DataSyncServiceConstants.SyncStatusConstants.AUTHENTICATION_ERROR:
                msg = getString(R.string.error_authentication_occur);
                syncErrorOccured = true;
                break;
            case Constants.DataSyncServiceConstants.SyncStatusConstants.DELETE_ERROR:
                msg = getString(R.string.error_local_repo_data_delete);
                syncErrorOccured = true;
                break;
            case Constants.DataSyncServiceConstants.SyncStatusConstants.SAVE_ERROR:
                msg = getString(R.string.error_data_save);
                syncErrorOccured = true;
                break;
            case Constants.DataSyncServiceConstants.SyncStatusConstants.LOCAL_CONNECTION_ERROR:
                msg = getString(R.string.error_local_connection_unavailable);
                syncErrorOccured = true;
                break;
            case Constants.DataSyncServiceConstants.SyncStatusConstants.SERVER_CONNECTION_ERROR:
                msg = getString(R.string.error_server_connection_unavailable);
                syncErrorOccured = true;
                break;
            case Constants.DataSyncServiceConstants.SyncStatusConstants.PARSING_ERROR:
                msg = getString(R.string.error_parse_exception_data_fetch);
                syncErrorOccured = true;
                break;
            case Constants.DataSyncServiceConstants.SyncStatusConstants.LOAD_ERROR:
                msg = getString(R.string.error_exception_data_load);
                syncErrorOccured = true;
                break;
            case Constants.DataSyncServiceConstants.SyncStatusConstants.UPLOAD_ERROR:
                msg = getString(R.string.error_exception_data_upload);
                syncErrorOccured = true;
                break;
            case Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS:
                int syncType = intent.getIntExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, -1);
                int downloadCount = intent.getIntExtra(Constants.DataSyncServiceConstants.DOWNLOAD_COUNT_PRIMARY, 0);

                if (isAtsUser()) {
                    switch (syncType) {
                        case Constants.DataSyncServiceConstants.SYNC_HTC_PERSONS:
                            msg = getString(R.string.info_real_time_upload_success);
                            break;
                    }
                } else {
                    switch (syncType) {
                        case Constants.DataSyncServiceConstants.SYNC_FORMS:
                            int deletedFormCount = intent.getIntExtra(Constants.DataSyncServiceConstants.DELETED_COUNT_PRIMARY, 0);
                            msg = getString(R.string.info_forms_downloaded, downloadCount);
                            if (deletedFormCount > 0) {
                                msg = getString(R.string.info_form_download_delete, downloadCount, deletedFormCount);
                            }
                            break;
                        case Constants.DataSyncServiceConstants.SYNC_TEMPLATES:
                            msg = getString(R.string.info_form_template_concept_download, downloadCount, intent.getIntExtra(Constants.DataSyncServiceConstants.DOWNLOAD_COUNT_SECONDARY, 0));
                            break;
                        case Constants.DataSyncServiceConstants.SYNC_COHORTS_METADATA:
                            msg = getString(R.string.info_new_cohort_download, downloadCount);
                            break;
                        case Constants.DataSyncServiceConstants.SYNC_SELECTED_COHORTS_PATIENTS_FULL_DATA: {
                            int downloadCountSec = intent.getIntExtra(Constants.DataSyncServiceConstants.DOWNLOAD_COUNT_SECONDARY, 0);
                            msg = getString(R.string.info_cohort_new_patient_download, downloadCount, downloadCountSec) + getString(R.string.info_patient_data_download);
                            break;
                        }
                        case Constants.DataSyncServiceConstants.SYNC_SELECTED_COHORTS_PATIENTS_ONLY: {
                            int downloadCountSec = intent.getIntExtra(Constants.DataSyncServiceConstants.DOWNLOAD_COUNT_SECONDARY, 0);
                            msg = getString(R.string.info_cohorts_patients_download, downloadCount, downloadCountSec);
                            break;
                        }
                        case Constants.DataSyncServiceConstants.SYNC_OBSERVATIONS:
                            msg = getString(R.string.info_new_observation_download, downloadCount);
                            break;
                        case Constants.DataSyncServiceConstants.SYNC_ENCOUNTERS:
                            msg = getString(R.string.info_new_encounter_download, downloadCount);
                            break;
                        case Constants.DataSyncServiceConstants.SYNC_UPLOAD_FORMS:
                            msg = getString(R.string.info_form_data_upload_sucess);
                            break;
                        case Constants.DataSyncServiceConstants.SYNC_REAL_TIME_UPLOAD_FORMS:
                            msg = getString(R.string.info_real_time_upload_success);
                            break;
                        case Constants.DataSyncServiceConstants.SYNC_PATIENT_REPORTS_HEADERS:
                            msg = getString(R.string.info_patient_reports_downloaded, downloadCount);
                            break;
                        case Constants.DataSyncServiceConstants.SYNC_PATIENT_REPORTS:
                            msg = getString(R.string.info_patient_reports_downloaded, downloadCount);
                            break;
                        case Constants.DataSyncServiceConstants.SYNC_HTC_PERSONS:
                            msg = getString(R.string.info_real_time_upload_success);
                    }
                    break;
                }
        }

        if(StringUtils.isEmpty(msg)){
            msg = getString(R.string.info_download_complete, syncStatus) + " Sync type = " + intent.getIntExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, -1);
        }

        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        dataSyncProgressLog.add(msg);
        if(dataSyncProgressMessageArrayAdapter != null) {
            dataSyncProgressMessageArrayAdapter.notifyDataSetChanged();
        }
    }

    private boolean isAtsUser() {
        try {
            return ((MuzimaApplication)getApplication()).getSetupConfigurationController().getAllSetupConfigurations().get(0).getUuid().equals("1eaa9574-fa5a-4655-bd63-466b538c5b5d");
        } catch (SetupConfigurationController.SetupConfigurationDownloadException e) {
            throw new RuntimeException(e);
        }
    }

    protected final void showBackgroundSyncProgressDialog(Context context){

        if(dataSyncProgressMessageArrayAdapter == null){
            dataSyncProgressMessageArrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, dataSyncProgressLog);
        }

        View progressListView = getLayoutInflater().inflate(R.layout.sync_progress_layout_list, null,false);
        ListView listView = progressListView.findViewById(R.id.listView);
        listView.setAdapter(dataSyncProgressMessageArrayAdapter);

        if(listView.getParent() != null){
            ((ViewGroup) listView.getParent()).removeView(listView);
        }
        View syncTitleView = getLayoutInflater().inflate(R.layout.sync_progress_layout_title,null,false);
        if(syncTitleView.getParent() != null){
            ((ViewGroup) syncTitleView.getParent()).removeView(syncTitleView);
        }

        imageView = syncTitleView.findViewById(R.id.iv_refresh_icon);
        if(isSyncRunning){
            startProgressAnimation();
        }

        titleTextView = syncTitleView.findViewById(R.id.sync_progress_title);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(listView)
                .setCancelable(true)
                .setCustomTitle(syncTitleView)
                .setNegativeButton(R.string.general_hide,new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .create()
                .show();
    }

    public boolean isDataSyncRunning(){
        return isSyncRunning;
    }

    public boolean isSyncCompletedWithError(){
        return syncErrorOccured;
    }

    private void startProgressAnimation(){
        if(imageView != null) {
            RotateAnimation rotateAnim = new RotateAnimation(0.0f, 360.0f,
                    RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                    RotateAnimation.RELATIVE_TO_SELF, 0.5f);

            rotateAnim.setInterpolator(new LinearInterpolator());
            rotateAnim.setDuration(1000);
            rotateAnim.setRepeatCount(Animation.INFINITE);
            imageView.startAnimation(rotateAnim);
        }
    }

    private void stopProgressAnimation(){
        if(imageView != null) {
            imageView.clearAnimation();
        }
    }

    protected void updateSyncProgressWidgets(boolean isSyncRunning){
        //Sub-classes that have widgets to be updated need to override this
    }

    protected void notifySyncStarted(){
        if(dataSyncProgressLog != null) {
            dataSyncProgressLog.clear();
        }
    }


}
