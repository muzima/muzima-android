/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.APIName;
import com.muzima.api.model.LastSyncTime;
import com.muzima.api.model.Patient;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.view.BroadcastListenerActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.muzima.utils.Constants.DataSyncServiceConstants;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants;
import static com.muzima.utils.Constants.NotificationStatusConstants;
import static java.util.Arrays.asList;

public class DataSyncService extends IntentService {

    private static final int MUZIMA_NOTIFICATION = 0;
    private String notificationServiceRunning;
    private String notificationServiceFinished;
    private String notificationMsg;
    private MuzimaSyncService muzimaSyncService;

    public DataSyncService() {
        super("DataSyncService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationServiceRunning = getString(R.string.info_muzima_sync_service_in_progress);
        notificationServiceFinished = getString(R.string.info_muzima_sync_service_finish);
        muzimaSyncService = ((MuzimaApplication) getApplication()).getMuzimaSyncService();
        updateNotificationMsg(notificationServiceRunning);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        int syncType = intent.getIntExtra(DataSyncServiceConstants.SYNC_TYPE, -1);
        Intent broadcastIntent = new Intent();
        String[] credentials = intent.getStringArrayExtra(DataSyncServiceConstants.CREDENTIALS);
        broadcastIntent.setAction(BroadcastListenerActivity.MESSAGE_SENT_ACTION);
        broadcastIntent.putExtra(DataSyncServiceConstants.SYNC_TYPE, syncType);

        switch (syncType) {
            case DataSyncServiceConstants.SYNC_FORMS:
                updateNotificationMsg(getString(R.string.info_form_metadata_download));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    int[] result = muzimaSyncService.downloadForms();
                    String msg = getString(R.string.info_form_download_delete,result[1], result[2]);
                    prepareBroadcastMsgForDownloadForms(broadcastIntent, result, msg);
                    saveSyncTime(result,APIName.DOWNLOAD_FORMS);
                }
                break;
            case DataSyncServiceConstants.SYNC_TEMPLATES:
                String[] formIds = intent.getStringArrayExtra(DataSyncServiceConstants.FORM_IDS);
                updateNotificationMsg(getString(R.string.info_form_template_with_count_download, formIds.length ));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    int[] result = muzimaSyncService.downloadFormTemplatesAndRelatedMetadata(formIds, true);
                    String msg = getString(R.string.info_form_template_concept_download,result[1],result[2]);
                    broadcastIntent.putExtra(DataSyncServiceConstants.DOWNLOAD_COUNT_SECONDARY, result[2]);
                    prepareBroadcastMsg(broadcastIntent, result, msg);
                }
                break;
            case DataSyncServiceConstants.SYNC_COHORTS:
                updateNotificationMsg(getString(R.string.info_cohort_download));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    int[] result = muzimaSyncService.downloadCohorts();
                    String msg = getString(R.string.info_new_cohort_download_delete,result[1],result[2]);
                    prepareBroadcastMsg(broadcastIntent, result, msg);
                    saveSyncTime(result, APIName.DOWNLOAD_COHORTS);
                    consolidateAndSyncIndependentPatients(broadcastIntent);
                }
                break;
            case DataSyncServiceConstants.SYNC_PATIENTS_FULL_DATA:
                String[] cohortIds = intent.getStringArrayExtra(DataSyncServiceConstants.COHORT_IDS);
                updateNotificationMsg(getString(R.string.info_patient_download));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    downloadPatients(broadcastIntent, cohortIds);
                    downloadObservationsAndEncounters(broadcastIntent, cohortIds);
                }
                break;
            case DataSyncServiceConstants.SYNC_PATIENTS_ONLY:
                String[] cohortIdsToDownload = intent.getStringArrayExtra(DataSyncServiceConstants.COHORT_IDS);
                updateNotificationMsg(getString(R.string.info_patient_download));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    downloadPatients(broadcastIntent, cohortIdsToDownload);
                }
                break;
            case DataSyncServiceConstants.SYNC_PATIENTS_DATA_ONLY:
                String[] savedCohortIds = intent.getStringArrayExtra(DataSyncServiceConstants.COHORT_IDS);
                updateNotificationMsg(getString(R.string.info_patient_data_download));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    downloadObservationsAndEncounters(broadcastIntent, savedCohortIds);
                }
                break;
            case DataSyncServiceConstants.SYNC_UPLOAD_FORMS:
                updateNotificationMsg(getString(R.string.info_form_upload));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    int[] result = muzimaSyncService.uploadAllCompletedForms();
                    broadcastIntent.putExtra(DataSyncServiceConstants.SYNC_TYPE, DataSyncServiceConstants.SYNC_UPLOAD_FORMS);
                    prepareBroadcastMsgForFormUpload(broadcastIntent, result, getString(R.string.info_form_upload_success));
                }
                break;
            case DataSyncServiceConstants.DOWNLOAD_PATIENT_ONLY:
                String[] patientsToBeDownloaded = intent.getStringArrayExtra(DataSyncServiceConstants.PATIENT_UUID_FOR_DOWNLOAD);
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    downloadPatientsWithObsAndEncounters(broadcastIntent, patientsToBeDownloaded);
                }
                break;
            case DataSyncServiceConstants.SYNC_NOTIFICATIONS:
                String receiverUUid = intent.getStringExtra(NotificationStatusConstants.RECEIVER_UUID);
                String[] downloadedCohortIds = intent.getStringArrayExtra(DataSyncServiceConstants.COHORT_IDS);
                updateNotificationMsg(getString(R.string.info_notification_download_in_progress));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    int[] result = muzimaSyncService.downloadNotifications(receiverUUid);
                    String msg = getString(R.string.info_notification_download,result[1]);
                    prepareBroadcastMsg(broadcastIntent, result, msg);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);

                    downloadObservationsAndEncounters(broadcastIntent, downloadedCohortIds);
                }
                break;
            case DataSyncServiceConstants.SYNC_REAL_TIME_UPLOAD_FORMS:
                updateNotificationMsg(getString(R.string.info_real_time_upload));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    int[] result = muzimaSyncService.uploadAllCompletedForms();
                    broadcastIntent.putExtra(DataSyncServiceConstants.SYNC_TYPE, DataSyncServiceConstants.SYNC_REAL_TIME_UPLOAD_FORMS);
                    prepareBroadcastMsgForFormUpload(broadcastIntent, result, getString(R.string.info_real_time_upload_success));
                }
                break;
            default:
                break;
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
    }

    private void consolidateAndSyncIndependentPatients(Intent broadcastIntent) {
        muzimaSyncService.consolidatePatients();
        List<Patient> patients = muzimaSyncService.updatePatientsNotPartOfCohorts();

        List<String> patientUuids = muzimaSyncService.getPatientUuids(patients);
        downloadPatientsWithObsAndEncounters(broadcastIntent,patientUuids.toArray(new String[patientUuids.size()]));
    }

    private void downloadPatientsWithObsAndEncounters(Intent broadcastIntent, String[] patientUUIDs) {
        if(patientUUIDs.length == 0){
            return;
        }
        int[] resultForPatients = muzimaSyncService.downloadPatients(patientUUIDs);
        broadCastMessageForPatients(broadcastIntent, resultForPatients, patientUUIDs);
        List<String> patientUUIDList = new ArrayList<>(asList(patientUUIDs));
        if (isSuccess(resultForPatients)) {
            int[] resultForObs = muzimaSyncService.downloadObservationsForPatientsByPatientUUIDs(patientUUIDList, true);
            broadCastMessageForObservationDownload(broadcastIntent, resultForObs);

            int[] resultForEncounters = muzimaSyncService.downloadEncountersForPatientsByPatientUUIDs(patientUUIDList, true);
            broadCastMessageForEncounters(broadcastIntent, resultForEncounters);
        }
    }

    private void downloadObservationsAndEncounters(Intent broadcastIntent, String[] savedCohortIds) {
        int[] resultForObservations = muzimaSyncService.downloadObservationsForPatientsByCohortUUIDs(savedCohortIds, true);
        broadCastMessageForObservationDownload(broadcastIntent, resultForObservations);

        int[] resultForEncounters = muzimaSyncService.downloadEncountersForPatientsByCohortUUIDs(savedCohortIds, true);
        broadCastMessageForEncounters(broadcastIntent, resultForEncounters);
    }

    private void downloadPatients(Intent broadcastIntent, String[] cohortIds) {
        int[] resultForPatients = muzimaSyncService.downloadPatientsForCohorts(cohortIds);
        broadCastMessageForPatients(broadcastIntent, resultForPatients);
    }

    private void broadCastMessageForEncounters(Intent broadcastIntent, int[] resultForEncounters) {
        String msgForEncounters = getString(R.string.info_new_encounter_download_delete,resultForEncounters[1] ,resultForEncounters[2]);
        prepareBroadcastMsg(broadcastIntent, resultForEncounters, msgForEncounters);
        broadcastIntent.putExtra(DataSyncServiceConstants.SYNC_TYPE, DataSyncServiceConstants.SYNC_ENCOUNTERS);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
    }

    private void broadCastMessageForObservationDownload(Intent broadcastIntent, int[] resultForObservations) {
        String msgForObservations = getString(R.string.info_new_observation_download_delete,resultForObservations[1] , resultForObservations[2]);
        prepareBroadcastMsg(broadcastIntent, resultForObservations, msgForObservations);
        broadcastIntent.putExtra(DataSyncServiceConstants.SYNC_TYPE, DataSyncServiceConstants.SYNC_OBSERVATIONS);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
    }

    private void broadCastMessageForPatients(Intent broadcastIntent, int[] resultForPatients) {
        String msgForPatients = getString(R.string.info_new_patient_download,resultForPatients[1]);
        prepareBroadcastMsg(broadcastIntent, resultForPatients, msgForPatients);
        if (isSuccess(resultForPatients) && resultForPatients.length > 1) {
            broadcastIntent.putExtra(DataSyncServiceConstants.DOWNLOAD_COUNT_SECONDARY, resultForPatients[1]);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
    }
    private void broadCastMessageForPatients(Intent broadcastIntent, int[] resultForPatients, String[] patientUUIDs) {
        String msgForPatients = getString(R.string.info_new_patient_download,resultForPatients[1]);
        prepareBroadcastMsg(broadcastIntent, resultForPatients, msgForPatients);
        if (isSuccess(resultForPatients) && resultForPatients.length > 1) {
            broadcastIntent.putExtra(DataSyncServiceConstants.DOWNLOAD_COUNT_SECONDARY, resultForPatients[1]);
            broadcastIntent.putExtra(DataSyncServiceConstants.PATIENT_UUID_FOR_DOWNLOAD, patientUUIDs);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
    }

    private void prepareBroadcastMsg(Intent broadcastIntent, int[] result, String msg) {
        broadcastIntent.putExtra(DataSyncServiceConstants.SYNC_STATUS, result[0]);
        if (isSuccess(result)) {
            broadcastIntent.putExtra(DataSyncServiceConstants.DOWNLOAD_COUNT_PRIMARY, result[1]);
            updateNotificationMsg(msg);
        }
    }

    private void prepareBroadcastMsgForDownloadForms(Intent broadcastIntent, int[] result, String msg) {
        broadcastIntent.putExtra(DataSyncServiceConstants.SYNC_STATUS, result[0]);
        if (isSuccess(result)) {
            broadcastIntent.putExtra(DataSyncServiceConstants.DOWNLOAD_COUNT_PRIMARY, result[1]);
            broadcastIntent.putExtra(DataSyncServiceConstants.DELETED_COUNT_PRIMARY,result[2]);
            updateNotificationMsg(msg);
        }
    }

    private boolean isSuccess(int[] result) {
        return result[0] == SyncStatusConstants.SUCCESS;
    }

    private void prepareBroadcastMsgForFormUpload(Intent broadcastIntent, int[] result, String msg) {
        broadcastIntent.putExtra(DataSyncServiceConstants.SYNC_STATUS, result[0]);
        if (isSuccess(result)) {
            updateNotificationMsg(msg);
        }
    }

    private void saveSyncTime(int[] result, APIName apiName) {
        if (isSuccess(result)) {
            LastSyncTimeService lastSyncTimeService = null;
            try {
                lastSyncTimeService = ((MuzimaApplication) getApplication()).getMuzimaContext().getLastSyncTimeService();
                SntpService sntpService = ((MuzimaApplication)getApplicationContext()).getSntpService();
                LastSyncTime lastSyncTime = new LastSyncTime(apiName, sntpService.getLocalTime());
                lastSyncTimeService.saveLastSyncTime(lastSyncTime);
            } catch (IOException e) {
                Log.i(getClass().getSimpleName(), "Error setting last sync time.");
            }
        }
    }

    private boolean authenticationSuccessful(String[] credentials, Intent broadcastIntent) {
        int authenticationStatus = muzimaSyncService.authenticate(credentials);
        if (authenticationStatus != SyncStatusConstants.AUTHENTICATION_SUCCESS) {
            broadcastIntent.putExtra(DataSyncServiceConstants.SYNC_STATUS, authenticationStatus);
            return false;
        }
        return true;
    }

    @Override
    public void onDestroy() {
        showNotification(notificationServiceFinished, notificationMsg);
        super.onDestroy();
    }

    private void showNotification(String title, String msg) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(), 0))
                        .setSmallIcon(R.drawable.ic_launcher_logo)
                        .setContentTitle(title)
                        .setContentText(msg)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(msg));

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(MUZIMA_NOTIFICATION, mBuilder.getNotification());
    }


    private void updateNotificationMsg(String msg) {
        notificationMsg = msg;
        showNotification(notificationServiceRunning, notificationMsg);
    }
}
