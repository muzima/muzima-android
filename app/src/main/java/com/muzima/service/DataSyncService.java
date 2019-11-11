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
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.FormData;
import com.muzima.utils.Constants;
import com.muzima.view.BroadcastListenerActivity;

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
            case DataSyncServiceConstants.SYNC_COHORTS_METADATA:
                updateNotificationMsg(getString(R.string.info_cohort_download));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    int[] result = muzimaSyncService.downloadCohorts();
                    String msg = getString(R.string.info_new_cohort_download_delete,result[1],result[2]);
                    prepareBroadcastMsg(broadcastIntent, result, msg);
                }
                break;
            case DataSyncServiceConstants.SYNC_COHORTS_AND_ALL_PATIENTS_FULL_DATA:
                updateNotificationMsg(getString(R.string.info_patient_data_download));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    syncCohortsAndAllPatientsFullData(broadcastIntent);
                }
                break;
            case DataSyncServiceConstants.CLEAN_UP_PROCESSED_TEMPORARY_FORM_DATA:
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    checkAndDeleteTemporaryDataForProcessedFormData(broadcastIntent);
                }
                break;
            case DataSyncServiceConstants.SYNC_SELECTED_COHORTS_PATIENTS_FULL_DATA:
                String[] cohortIds = intent.getStringArrayExtra(DataSyncServiceConstants.COHORT_IDS);
                updateNotificationMsg(getString(R.string.info_patient_data_download));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    downloadPatientsInCohorts(broadcastIntent, cohortIds);
                    downloadObservationsAndEncounters(broadcastIntent, cohortIds);
                }
                break;
            case DataSyncServiceConstants.SYNC_SELECTED_COHORTS_PATIENTS_ONLY:
                String[] cohortIdsToDownload = intent.getStringArrayExtra(DataSyncServiceConstants.COHORT_IDS);
                updateNotificationMsg(getString(R.string.info_patient_download));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    downloadPatientsInCohorts(broadcastIntent, cohortIdsToDownload);
                }
                break;
            case DataSyncServiceConstants.SYNC_SELECTED_COHORTS_PATIENTS_DATA_ONLY:
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
                    LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
                }
                break;
            case DataSyncServiceConstants.DOWNLOAD_SELECTED_PATIENTS_FULL_DATA:
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
            case DataSyncServiceConstants.SYNC_PATIENT_REPORTS_HEADERS:
                String patientUUid = intent.getStringExtra(Constants.SyncPatientReportsConstants.PATIENT_UUID);
                updateNotificationMsg(getString(R.string.info_patient_reports_download_in_progress));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    int[] result = muzimaSyncService.downloadPatientReportHeaders(patientUUid);
                    String msg = getString(R.string.info_patient_reports_downloaded,result[1]);
                    prepareBroadcastMsg(broadcastIntent, result, msg);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
                }
                break;
            case DataSyncServiceConstants.SYNC_PATIENT_REPORTS:
                String[] reportUuids = intent.getStringArrayExtra(Constants.SyncPatientReportsConstants.REPORT_UUIDS);
                updateNotificationMsg(getString(R.string.info_patient_reports_download_in_progress));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    int[] result = muzimaSyncService.downloadPatientReportsByUuid(reportUuids);
                    String msg = getString(R.string.info_patient_reports_downloaded,result[1]);
                    prepareBroadcastMsg(broadcastIntent, result, msg);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
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
            case DataSyncServiceConstants.SYNC_SETTINGS:
                updateNotificationMsg(getString(R.string.info_settings_update));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    int[] result = muzimaSyncService.downloadNewSettings();
                    broadcastIntent.putExtra(DataSyncServiceConstants.SYNC_TYPE, DataSyncServiceConstants.SYNC_SETTINGS);
                    prepareBroadcastMsgForSettingsDownload(broadcastIntent, result);
                }
                break;
            default:
                break;
        }
    }

    private void syncCohortsAndAllPatientsFullData(Intent broadcastIntent){
        //sync cohorts
        int[] result = muzimaSyncService.downloadCohorts();
        String msg = getString(R.string.info_new_cohort_download_delete,result[1],result[2]);
        prepareBroadcastMsg(broadcastIntent, result, msg);

        //Sync cohort updates
        result = muzimaSyncService.downloadPatientsForCohortsWithUpdatesAvailable();
        if(isSuccess(result)) {
            msg = getString(R.string.info_cohorts_patients_download, result[1], result[2]);
        } else {
            msg = getString(R.string.info_cohort_patient_not_download);
        }
        prepareBroadcastMsg(broadcastIntent, result, msg);

        //consolidate locally registered patients with newly registered records at server side
        muzimaSyncService.consolidatePatients();

        List<String> patientUUIDList = muzimaSyncService.getUuidsForAllPatientsFromLocalStorage();

        //Sync Obs for all patients
        int[] resultForObs = muzimaSyncService.downloadObservationsForPatientsByPatientUUIDs(patientUUIDList, true);
        broadCastMessageForObservationDownload(broadcastIntent, resultForObs);

        //Sync Encounters for all patients
        int[] resultForEncounters = muzimaSyncService.downloadEncountersForPatientsByPatientUUIDs(patientUUIDList, true);
        broadCastMessageForEncounters(broadcastIntent, resultForEncounters);
    }

    private void downloadPatientsWithObsAndEncounters(Intent broadcastIntent, String[] patientUUIDs) {
        if(patientUUIDs.length == 0){
            return;
        }
        int[] resultForPatients = muzimaSyncService.downloadPatients(patientUUIDs);
        broadCastMessageForPatients(broadcastIntent, resultForPatients, patientUUIDs);
        if (isSuccess(resultForPatients)) {
            List<String> patientUUIDList = new ArrayList<>(asList(patientUUIDs));
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

    private void downloadPatientsInCohorts(Intent broadcastIntent, String[] cohortIds) {
        int[] resultForPatients = muzimaSyncService.downloadPatientsForCohorts(cohortIds);
        broadCastMessageForPatientsInCohorts(broadcastIntent, resultForPatients);
    }

    private void checkAndDeleteTemporaryDataForProcessedFormData(Intent broadcastIntent){
        List<FormData> archivedFormData = muzimaSyncService.getArchivedFormData();
        if(archivedFormData.size() > 0) {
            updateNotificationMsg(getString(R.string.info_submitted_form_data_status_check));
            int[] result = muzimaSyncService.checkAndDeleteTemporaryDataForProcessedFormData(archivedFormData);
            broadcastIntent.putExtra(DataSyncServiceConstants.SYNC_TYPE,
                    DataSyncServiceConstants.CLEAN_UP_PROCESSED_TEMPORARY_FORM_DATA);
            broadcastIntent.putExtra(DataSyncServiceConstants.SYNC_STATUS, result[0]);
            String msg;
            if (isSuccess(result)) {
                msg = getString(R.string.info_submitted_form_data_status, result[1], result[2], result[3], result[4]);
                updateNotificationMsg(msg);
            } else {
                msg = getString(R.string.info_submitted_form_data_status_check_failure);
                updateNotificationMsg(msg);
            }
            broadcastIntent.putExtra(DataSyncServiceConstants.SYNC_RESULT_MESSAGE, msg);
            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
        }

    }

    private void broadCastMessageForEncounters(Intent broadcastIntent, int[] resultForEncounters) {
        String msgForEncounters = getString(R.string.info_new_encounter_download_delete,resultForEncounters[1] ,resultForEncounters[2]);
        broadcastIntent.putExtra(DataSyncServiceConstants.SYNC_RESULT_MESSAGE, msgForEncounters);
        prepareBroadcastMsg(broadcastIntent, resultForEncounters, msgForEncounters);
        broadcastIntent.putExtra(DataSyncServiceConstants.SYNC_TYPE, DataSyncServiceConstants.SYNC_ENCOUNTERS);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
    }

    private void broadCastMessageForObservationDownload(Intent broadcastIntent, int[] resultForObservations) {
        String msgForObservations = getString(R.string.info_new_observation_download_delete,resultForObservations[1] , resultForObservations[2]);
        broadcastIntent.putExtra(DataSyncServiceConstants.SYNC_RESULT_MESSAGE, msgForObservations);
        prepareBroadcastMsg(broadcastIntent, resultForObservations, msgForObservations);
        broadcastIntent.putExtra(DataSyncServiceConstants.SYNC_TYPE, DataSyncServiceConstants.SYNC_OBSERVATIONS);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
    }

    private void broadCastMessageForPatientsInCohorts(Intent broadcastIntent, int[] resultForPatients) {
        broadcastIntent.putExtra(DataSyncServiceConstants.SYNC_STATUS, resultForPatients[0]);
        if (isSuccess(resultForPatients) && resultForPatients.length > 1) {
            String msg = getString(R.string.info_new_patient_download,resultForPatients[1]);
            updateNotificationMsg(msg);
            broadcastIntent.putExtra(DataSyncServiceConstants.DOWNLOAD_COUNT_PRIMARY, resultForPatients[1]);
            broadcastIntent.putExtra(DataSyncServiceConstants.DOWNLOAD_COUNT_SECONDARY, resultForPatients[2]);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
    }
    private void broadCastMessageForPatients(Intent broadcastIntent, int[] resultForPatients, String[] patientUUIDs) {
        broadcastIntent.putExtra(DataSyncServiceConstants.SYNC_STATUS, resultForPatients[0]);
        if (isSuccess(resultForPatients) && resultForPatients.length > 1) {
            String msg = getString(R.string.info_new_patient_download,resultForPatients[1]);
            updateNotificationMsg(msg);
            broadcastIntent.putExtra(DataSyncServiceConstants.DOWNLOAD_COUNT_PRIMARY, resultForPatients[1]);
            broadcastIntent.putExtra(DataSyncServiceConstants.PATIENT_UUID_FOR_DOWNLOAD, patientUUIDs);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
    }

    private void prepareBroadcastMsg(Intent broadcastIntent, int[] result, String msg) {
        broadcastIntent.putExtra(DataSyncServiceConstants.SYNC_STATUS, result[0]);
        broadcastIntent.putExtra(DataSyncServiceConstants.SYNC_RESULT_MESSAGE, msg);
        if (isSuccess(result)) {
            broadcastIntent.putExtra(DataSyncServiceConstants.DOWNLOAD_COUNT_PRIMARY, result[1]);
            updateNotificationMsg(msg);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
    }

    private void prepareBroadcastMsgForDownloadForms(Intent broadcastIntent, int[] result, String msg) {
        broadcastIntent.putExtra(DataSyncServiceConstants.SYNC_STATUS, result[0]);
        broadcastIntent.putExtra(DataSyncServiceConstants.SYNC_RESULT_MESSAGE, msg);
        if (isSuccess(result)) {
            broadcastIntent.putExtra(DataSyncServiceConstants.DOWNLOAD_COUNT_PRIMARY, result[1]);
            broadcastIntent.putExtra(DataSyncServiceConstants.DELETED_COUNT_PRIMARY,result[2]);
            updateNotificationMsg(msg);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
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

    private void prepareBroadcastMsgForSettingsDownload(Intent broadcastIntent, int[] result) {
        broadcastIntent.putExtra(DataSyncServiceConstants.SYNC_STATUS, result[0]);
        if (isSuccess(result)) {
            if(result[1]>0) {
                String msg = getString(R.string.info_settings_downloaded, result[1]);
                broadcastIntent.putExtra(DataSyncServiceConstants.SYNC_RESULT_MESSAGE, msg);
                broadcastIntent.putExtra(DataSyncServiceConstants.DOWNLOAD_COUNT_PRIMARY, result[1]);
                LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
            }
        } else {
            String msg = getString(R.string.error_settings_download);
            broadcastIntent.putExtra(DataSyncServiceConstants.SYNC_RESULT_MESSAGE, msg);
            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
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
