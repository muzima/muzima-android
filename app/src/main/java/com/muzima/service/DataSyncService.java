/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.FormData;
import com.muzima.api.model.Patient;
import com.muzima.api.model.SetupConfigurationTemplate;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.utils.Constants;
import com.muzima.view.BroadcastListenerActivity;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

public class DataSyncService extends IntentService {
    private static final int MUZIMA_NOTIFICATION = 0;
    private String notificationServiceRunning;
    private String notificationServiceFinished;
    private String notificationMsg;
    private MuzimaSyncService muzimaSyncService;
    private SetupConfigurationTemplate configBeforeUpdate;
    private final static List<Integer> ongoingSyncTasks = new ArrayList<>();

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

    public static void addQueuedSyncType(Integer syncType){
        ongoingSyncTasks.add(syncType);
    }

    public static boolean hasOngoingSyncTasks(){
        return !ongoingSyncTasks.isEmpty();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Integer syncType = intent.getIntExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, -1);
        configBeforeUpdate = (SetupConfigurationTemplate) intent.getSerializableExtra(Constants.DataSyncServiceConstants.CONFIG_BEFORE_UPDATE);
        Intent broadcastIntent = new Intent();
        String[] credentials = intent.getStringArrayExtra(Constants.DataSyncServiceConstants.CREDENTIALS);
        broadcastIntent.setAction(BroadcastListenerActivity.MESSAGE_SENT_ACTION);
        broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, syncType);

        switch (syncType) {
            case Constants.DataSyncServiceConstants.SYNC_FORMS:
                updateNotificationMsg(getString(R.string.info_form_metadata_download));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    int[] result = muzimaSyncService.downloadForms();
                    String msg = getString(R.string.info_form_download_delete, result[1], result[2]);
                    prepareBroadcastMsgForDownloadForms(broadcastIntent, result, msg);
                }
                break;
            case Constants.DataSyncServiceConstants.SYNC_TEMPLATES:
                String[] formIds = intent.getStringArrayExtra(Constants.DataSyncServiceConstants.FORM_IDS);
                updateNotificationMsg(getString(R.string.info_form_template_with_count_download, formIds.length));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    int[] result = muzimaSyncService.downloadFormTemplatesAndRelatedMetadata(formIds, true);
                    String msg = getString(R.string.info_form_template_concept_download, result[1], result[2]);
                    broadcastIntent.putExtra(Constants.DataSyncServiceConstants.DOWNLOAD_COUNT_SECONDARY, result[2]);
                    prepareBroadcastMsg(broadcastIntent, result, msg);
                }
                break;
            case Constants.DataSyncServiceConstants.SYNC_HTC_PERSONS:
                updateNotificationMsg(getString(R.string.info_htc_data_upload));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    int[] result = muzimaSyncService.uploadAllPendingHtcData();
                    broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, Constants.DataSyncServiceConstants.SYNC_HTC_PERSONS);
                    prepareBroadcastMsgForFormUpload(broadcastIntent, result, getString(R.string.info_htc_data_upload_success));
                    LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
                }
                break;
            case Constants.DataSyncServiceConstants.SYNC_COHORTS_METADATA:
                updateNotificationMsg(getString(R.string.info_cohort_download));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    int[] result = muzimaSyncService.downloadCohorts();
                    String msg = getString(R.string.info_new_cohort_download_delete, result[1], result[2]);
                    prepareBroadcastMsg(broadcastIntent, result, msg);
                }
                break;
            case Constants.DataSyncServiceConstants.SYNC_COHORTS_AND_ALL_PATIENTS_FULL_DATA:
                updateNotificationMsg(getString(R.string.info_patient_data_download));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    syncCohortsAndAllPatientsFullData(broadcastIntent);
                }
                break;
            case Constants.DataSyncServiceConstants.CLEAN_UP_PROCESSED_TEMPORARY_FORM_DATA:
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    checkAndDeleteTemporaryDataForProcessedFormData(broadcastIntent);
                }
                break;
            case Constants.DataSyncServiceConstants.SYNC_SELECTED_COHORTS_PATIENTS_FULL_DATA:
                String[] cohortIds = intent.getStringArrayExtra(Constants.DataSyncServiceConstants.COHORT_IDS);
                updateNotificationMsg(getString(R.string.info_patient_data_download));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    downloadPatientsInCohorts(broadcastIntent, cohortIds);
                    consolidatePatients();
                    downloadObservations(broadcastIntent, cohortIds);
                }
                break;
            case Constants.DataSyncServiceConstants.UPDATE_PATIENT_TAGS:
                String[] patientUuids = intent.getStringArrayExtra(Constants.DataSyncServiceConstants.PATIENT_UUIDS);
                muzimaSyncService.updatePatientTags(asList(patientUuids));
                break;
            case Constants.DataSyncServiceConstants.SYNC_SELECTED_COHORTS_PATIENTS_ONLY:
                String[] cohortIdsToDownload = intent.getStringArrayExtra(Constants.DataSyncServiceConstants.COHORT_IDS);
                updateNotificationMsg(getString(R.string.info_patient_download));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    downloadPatientsInCohorts(broadcastIntent, cohortIdsToDownload);
                    consolidatePatients();
                }
                break;
            case Constants.DataSyncServiceConstants.SYNC_SELECTED_COHORTS_PATIENTS_DATA_ONLY:
                String[] savedCohortIds = intent.getStringArrayExtra(Constants.DataSyncServiceConstants.COHORT_IDS);
                updateNotificationMsg(getString(R.string.info_patient_data_download));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    downloadObservations(broadcastIntent, savedCohortIds);
                }
                break;
            case Constants.DataSyncServiceConstants.SYNC_UPLOAD_FORMS:
                updateNotificationMsg(getString(R.string.info_form_upload));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    int[] result = muzimaSyncService.uploadAllCompletedForms();
                    broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, Constants.DataSyncServiceConstants.SYNC_UPLOAD_FORMS);
                    prepareBroadcastMsgForFormUpload(broadcastIntent, result, getString(R.string.info_form_upload_success));
                    LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
                }
                break;
            case Constants.DataSyncServiceConstants.DOWNLOAD_SELECTED_PATIENTS_FULL_DATA:
                String[] patientsToBeDownloaded = intent.getStringArrayExtra(Constants.DataSyncServiceConstants.PATIENT_UUID_FOR_DOWNLOAD);
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    downloadPatientsWithObsAndEncounters(broadcastIntent, patientsToBeDownloaded);
                }
                break;
            case Constants.DataSyncServiceConstants.SYNC_PATIENT_REPORTS_HEADERS:
                String patientUUid = intent.getStringExtra(Constants.SyncPatientReportsConstants.PATIENT_UUID);
                updateNotificationMsg(getString(R.string.info_patient_reports_download_in_progress));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    int[] result = muzimaSyncService.downloadPatientReportHeaders(patientUUid);
                    String msg = getString(R.string.info_patient_reports_downloaded, result[1]);
                    prepareBroadcastMsg(broadcastIntent, result, msg);
                }
                break;
            case Constants.DataSyncServiceConstants.SYNC_PATIENT_REPORTS:
                String[] reportUuids = intent.getStringArrayExtra(Constants.SyncPatientReportsConstants.REPORT_UUIDS);
                updateNotificationMsg(getString(R.string.info_patient_reports_download_in_progress));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    int[] result = muzimaSyncService.downloadPatientReportsByUuid(reportUuids);
                    String msg = getString(R.string.info_patient_reports_downloaded, result[1]);
                    prepareBroadcastMsg(broadcastIntent, result, msg);
                }
                break;
            case Constants.DataSyncServiceConstants.SYNC_REAL_TIME_UPLOAD_FORMS:
                updateNotificationMsg(getString(R.string.info_real_time_upload));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    int[] result = muzimaSyncService.uploadAllCompletedForms();
                    broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, Constants.DataSyncServiceConstants.SYNC_REAL_TIME_UPLOAD_FORMS);
                    prepareBroadcastMsgForFormUpload(broadcastIntent, result, getString(R.string.info_real_time_upload_success));
                    LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
                }
                break;
            case Constants.DataSyncServiceConstants.SYNC_SETTINGS:
                updateNotificationMsg(getString(R.string.info_settings_update));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    int[] result = muzimaSyncService.downloadNewSettings();
                    broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, Constants.DataSyncServiceConstants.SYNC_SETTINGS);
                    prepareBroadcastMsgForSettingsDownload(broadcastIntent, result);
                }
                break;
            case Constants.DataSyncServiceConstants.SYNC_ALL_PATIENT_REPORT_HEADERS_AND_REPORTS:
                updateNotificationMsg(getString(R.string.info_patient_reports_download_in_progress));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    int[] result = muzimaSyncService.downloadAllPatientReportHeadersAndReports();
                    String msg = getString(R.string.info_patient_reports_downloaded, result[1]);
                    prepareBroadcastMsg(broadcastIntent, result, msg);
                }
                break;
            case Constants.DataSyncServiceConstants.SYNC_SETUP_CONFIGURATION_TEMPLATES:
                updateNotificationMsg(getString(R.string.info_setup_configuration_template_download_in_progress));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    int[] result = muzimaSyncService.updateSetupConfigurationTemplates();
                    String msg = getString(R.string.info_setup_configuration_templates_downloaded, result[1]);
                    prepareBroadcastMsg(broadcastIntent, result, msg);
                }
                break;
            case Constants.DataSyncServiceConstants.SYNC_REPORT_DATASETS:
                updateNotificationMsg(getString(R.string.info_report_dataset_download_in_progress));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    int[] result = muzimaSyncService.downloadReportDatasetsForDownloadedReports(true);
                    String msg = getString(R.string.info_report_dataset_downloaded, result[1]);
                    prepareBroadcastMsg(broadcastIntent, result, msg);
                }
                break;
            case Constants.DataSyncServiceConstants.SYNC_MEDIA_CATEGORIES:
                updateNotificationMsg(getString(R.string.info_media_category_download_in_progress));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    int[] result = muzimaSyncService.SyncMediaCategory(configBeforeUpdate);
                    String msg = getString(R.string.info_media_category_downloaded, result[1]);
                    prepareBroadcastMsg(broadcastIntent, result, msg);
                }
                break;
            case Constants.DataSyncServiceConstants.SYNC_DATASETS:
                updateNotificationMsg(getString(R.string.info_report_dataset_download_in_progress));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    int[] result = muzimaSyncService.SyncDatasets(configBeforeUpdate);
                    String msg = getString(R.string.info_report_dataset_downloaded, result[1]);
                    prepareBroadcastMsg(broadcastIntent, result, msg);
                }
                break;
            case Constants.DataSyncServiceConstants.SYNC_MEDIA:
                updateNotificationMsg(getString(R.string.info_media_download_in_progress));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    int[] result = muzimaSyncService.SyncMedia();
                    String msg = getString(R.string.info_media_downloaded, result[1]);
                    prepareBroadcastMsg(broadcastIntent, result, msg);
                }
                break;
            case Constants.DataSyncServiceConstants.SYNC_PATIENT_FULL_PATIENT_DATA_BASED_ON_COHORT_CHANGES_IN_CONFIG:
                updateNotificationMsg(getString(R.string.info_cohort_download));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    muzimaSyncService.SyncPatientFullDataBasedOnCohortChangesInConfig(configBeforeUpdate);
                }
                break;
            case Constants.DataSyncServiceConstants.SYNC_LOCATIONS_BASED_ON_CHANGES_IN_CONFIG:
                updateNotificationMsg(getString(R.string.info_location_download));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    int[] result = muzimaSyncService.DownloadAndDeleteLocationBasedOnConfigChanges(configBeforeUpdate);
                    String msg = getString(R.string.info_locations_downloaded_deleted, result[1], result[2]);
                    broadcastIntent.putExtra(Constants.DataSyncServiceConstants.DELETED_COUNT_PRIMARY, result[2]);
                    prepareBroadcastMsg(broadcastIntent, result, msg);
                }
                break;
            case Constants.DataSyncServiceConstants.SYNC_PROVIDERS_BASED_ON_CHANGES_IN_CONFIG:
                updateNotificationMsg(getString(R.string.info_provider_download));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    int[] result = muzimaSyncService.DownloadAndDeleteProvidersBasedOnConfigChanges(configBeforeUpdate);
                    String msg = getString(R.string.info_provider_downloaded_deleted, result[1], result[2]);
                    broadcastIntent.putExtra(Constants.DataSyncServiceConstants.DELETED_COUNT_PRIMARY, result[2]);
                    prepareBroadcastMsg(broadcastIntent, result, msg);
                }
                break;
            case Constants.DataSyncServiceConstants.SYNC_CONCEPTS_AND_OBS_BASED_ON_CHANGES_IN_CONFIG:
                updateNotificationMsg(getString(R.string.info_concept_download));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    int[] result = muzimaSyncService.DownloadAndDeleteConceptAndObservationBasedOnConfigChanges(configBeforeUpdate);
                    broadCastMessageForNewConceptsDownloaded(broadcastIntent, result);
                }
                break;
            case Constants.DataSyncServiceConstants.SYNC_DERIVED_CONCEPTS_AND_OBS_BASED_ON_CHANGES_IN_CONFIG:
                updateNotificationMsg(getString(R.string.info_derived_concept_download));
                if (authenticationSuccessful(credentials, broadcastIntent)) {
                    int[] result = muzimaSyncService.DownloadAndDeleteDerivedConceptAndObservationBasedOnConfigChanges(configBeforeUpdate, true);
                    broadCastMessageForNewDerivedConceptsDownloaded(broadcastIntent, result);
                }
                break;
            default:
                break;
        }
        ongoingSyncTasks.remove(syncType);
        if(ongoingSyncTasks.isEmpty()){
            Intent syncCompletedBroadcastIntent = new Intent();
            syncCompletedBroadcastIntent.setAction(BroadcastListenerActivity.SYNC_COMPLETED_ACTION);
            LocalBroadcastManager.getInstance(this).sendBroadcast(syncCompletedBroadcastIntent);
        }
    }

    private void syncCohortsAndAllPatientsFullData(Intent broadcastIntent) {
        //sync cohorts
        int[] result = muzimaSyncService.downloadCohorts();
        String msg = getString(R.string.info_new_cohort_download_delete, result[1], result[2]);
        prepareBroadcastMsg(broadcastIntent, result, msg);

        //Sync cohort updates
        result = muzimaSyncService.downloadPatientsForCohortsWithUpdatesAvailable();
        List<Patient> updatedPatients = muzimaSyncService.updatePatientsNotPartOfCohorts();

        if (updatedPatients.size() > 0) {
            result[1] += updatedPatients.size();
            result[0] = Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS;
        }

        if (isSuccess(result)) {
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

        //Sync Relationships for all clients
        muzimaSyncService.downloadRelationshipsTypes();
        int[] resultForRelationships = muzimaSyncService.downloadRelationshipsForPatientsByPatientUUIDs(patientUUIDList);
        broadCastMessageForRelationshipsDownload(broadcastIntent, resultForRelationships);

        MuzimaSettingController muzimaSettingController = ((MuzimaApplication) getApplication()).getMuzimaSettingController();

        if (muzimaSettingController.isRelationshipEnabled()) {
            muzimaSyncService.downloadObservationsForAllPersons(true);
            muzimaSyncService.downloadDerivedObservationsForAllPersons(true);
        }

        if (muzimaSettingController.isPatientTagGenerationEnabled()) {
            muzimaSyncService.updatePatientTags(patientUUIDList);
            if (muzimaSettingController.isRelationshipEnabled())
                muzimaSyncService.updatePersonTags(patientUUIDList);
        }

        muzimaSyncService.downloadDerivedObservationsForPatientsByPatientUUIDs(patientUUIDList, true);

        muzimaSyncService.downloadSummariesForPatientsByPatientUUIDs(patientUUIDList);

    }

    private void downloadPatientsWithObsAndEncounters(Intent broadcastIntent, String[] patientUUIDs) {
        if (patientUUIDs.length == 0) {
            return;
        }
        int[] resultForPatients = muzimaSyncService.downloadPatients(patientUUIDs);
        broadCastMessageForPatients(broadcastIntent, resultForPatients, patientUUIDs);
        if (isSuccess(resultForPatients)) {
            List<String> patientUUIDList = new ArrayList<>(asList(patientUUIDs));
            int[] resultForObs = muzimaSyncService.downloadObservationsForPatientsByPatientUUIDs(patientUUIDList, true);
            broadCastMessageForObservationDownload(broadcastIntent, resultForObs);

            int[] resultForRelationships = muzimaSyncService.downloadRelationshipsForPatientsByPatientUUIDs(patientUUIDList);
            broadCastMessageForRelationshipsDownload(broadcastIntent, resultForRelationships);
            MuzimaSettingController muzimaSettingController = ((MuzimaApplication) getApplication()).getMuzimaSettingController();

            if (muzimaSettingController.isRelationshipEnabled()) {
                muzimaSyncService.downloadObservationsForAllPersons(true);
                muzimaSyncService.downloadDerivedObservationsForAllPersons(true);
            }

            if (muzimaSettingController.isPatientTagGenerationEnabled()) {
                muzimaSyncService.updatePatientTags(patientUUIDList);
                if (muzimaSettingController.isRelationshipEnabled())
                    muzimaSyncService.updatePersonTags(patientUUIDList);

            }
        }
    }

    private void downloadObservations(Intent broadcastIntent, String[] savedCohortIds) {
        int[] resultForObservations = muzimaSyncService.downloadObservationsForPatientsByCohortUUIDs(savedCohortIds, true);
        broadCastMessageForObservationDownload(broadcastIntent, resultForObservations);

        MuzimaSettingController muzimaSettingController = ((MuzimaApplication) getApplication()).getMuzimaSettingController();

        if (muzimaSettingController.isRelationshipEnabled()) {
            muzimaSyncService.downloadObservationsForAllPersons(true);
            muzimaSyncService.downloadDerivedObservationsForAllPersons(true);
        }

        if (muzimaSettingController.isPatientTagGenerationEnabled()) {
            muzimaSyncService.updatePatientTags(muzimaSyncService.getUuidsForPatientsInCohorts(savedCohortIds));
            if (muzimaSettingController.isRelationshipEnabled())
                muzimaSyncService.updatePersonTags(muzimaSyncService.getUuidsForPatientsInCohorts(savedCohortIds));

        }
    }

    private void downloadPatientsInCohorts(Intent broadcastIntent, String[] cohortIds) {
        int[] resultForPatients = muzimaSyncService.downloadPatientsForCohorts(cohortIds);
        broadCastMessageForPatientsInCohorts(broadcastIntent, resultForPatients);
    }

    private void consolidatePatients() {
        muzimaSyncService.consolidatePatients();
    }

    private void checkAndDeleteTemporaryDataForProcessedFormData(Intent broadcastIntent) {
        List<FormData> archivedFormData = muzimaSyncService.getArchivedFormData();
        if (archivedFormData.size() > 0) {
            updateNotificationMsg(getString(R.string.info_submitted_form_data_status_check));
            int[] result = muzimaSyncService.checkAndDeleteTemporaryDataForProcessedFormData(archivedFormData);
            broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_TYPE,
                    Constants.DataSyncServiceConstants.CLEAN_UP_PROCESSED_TEMPORARY_FORM_DATA);
            broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_STATUS, result[0]);
            String msg;
            if (isSuccess(result)) {
                msg = getString(R.string.info_submitted_form_data_status, result[1], result[2], result[3], result[4]);
                updateNotificationMsg(msg);
            } else {
                msg = getString(R.string.info_submitted_form_data_status_check_failure);
                updateNotificationMsg(msg);
            }
            broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_RESULT_MESSAGE, msg);
            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
        }
    }

    private void broadCastMessageForEncounters(Intent broadcastIntent, int[] resultForEncounters) {
        String msgForEncounters = getString(R.string.info_new_encounter_download_delete, resultForEncounters[1], resultForEncounters[2]);
        broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_RESULT_MESSAGE, msgForEncounters);
        broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, Constants.DataSyncServiceConstants.SYNC_ENCOUNTERS);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
    }

    private void broadCastMessageForObservationDownload(Intent broadcastIntent, int[] resultForObservations) {
        String msgForObservations = getString(R.string.info_new_observation_download_delete, resultForObservations[1], resultForObservations[2]);
        broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_STATUS, resultForObservations[0]);
        broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_RESULT_MESSAGE, msgForObservations);
        broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, Constants.DataSyncServiceConstants.SYNC_OBSERVATIONS);
        if (isSuccess(resultForObservations)) {
            updateNotificationMsg(msgForObservations);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
    }

    private void broadCastMessageForPatientsInCohorts(Intent broadcastIntent, int[] resultForPatients) {
        broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_STATUS, resultForPatients[0]);
        if (isSuccess(resultForPatients) && resultForPatients.length > 1) {
            String msg = getString(R.string.info_new_patient_download, resultForPatients[1]);
            broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_RESULT_MESSAGE, msg);
            updateNotificationMsg(msg);
            broadcastIntent.putExtra(Constants.DataSyncServiceConstants.DOWNLOAD_COUNT_PRIMARY, resultForPatients[1]);
            broadcastIntent.putExtra(Constants.DataSyncServiceConstants.DOWNLOAD_COUNT_SECONDARY, resultForPatients[2]);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
    }

    private void broadCastMessageForPatients(Intent broadcastIntent, int[] resultForPatients, String[] patientUUIDs) {
        broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_STATUS, resultForPatients[0]);
        if (isSuccess(resultForPatients) && resultForPatients.length > 1) {
            String msg = getString(R.string.info_new_patient_download, resultForPatients[1]);
            broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_RESULT_MESSAGE, msg);
            updateNotificationMsg(msg);
            broadcastIntent.putExtra(Constants.DataSyncServiceConstants.DOWNLOAD_COUNT_PRIMARY, resultForPatients[1]);
            broadcastIntent.putExtra(Constants.DataSyncServiceConstants.PATIENT_UUID_FOR_DOWNLOAD, patientUUIDs);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
    }

    private void broadCastMessageForRelationshipsDownload(Intent broadcastIntent, int[] resultForRelationships) {
        if (resultForRelationships != null) {
            String msgForRelationships = getString(R.string.info_relationships_download, resultForRelationships[1], resultForRelationships[2]);
            broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_RESULT_MESSAGE, msgForRelationships);
            prepareBroadcastMsg(broadcastIntent, resultForRelationships, msgForRelationships);
            broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, Constants.DataSyncServiceConstants.SYNC_RELATIONSHIPS);
        }
    }

    private void prepareBroadcastMsg(Intent broadcastIntent, int[] result, String msg) {
        broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_STATUS, result[0]);
        broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_RESULT_MESSAGE, msg);
        if (isSuccess(result)) {
            broadcastIntent.putExtra(Constants.DataSyncServiceConstants.DOWNLOAD_COUNT_PRIMARY, result[1]);
            updateNotificationMsg(msg);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
    }

    private void prepareBroadcastMsgForDownloadForms(Intent broadcastIntent, int[] result, String msg) {
        broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_STATUS, result[0]);
        broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_RESULT_MESSAGE, msg);
        if (isSuccess(result)) {
            broadcastIntent.putExtra(Constants.DataSyncServiceConstants.DOWNLOAD_COUNT_PRIMARY, result[1]);
            broadcastIntent.putExtra(Constants.DataSyncServiceConstants.DELETED_COUNT_PRIMARY, result[2]);
            updateNotificationMsg(msg);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
    }

    private boolean isSuccess(int[] result) {
        return result[0] == Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS;
    }

    private void prepareBroadcastMsgForFormUpload(Intent broadcastIntent, int[] result, String msg) {
        broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_STATUS, result[0]);
        if (isSuccess(result)) {
            updateNotificationMsg(msg);
        }
    }

    private void prepareBroadcastMsgForSettingsDownload(Intent broadcastIntent, int[] result) {
        broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_STATUS, result[0]);
        if (isSuccess(result)) {
            if (result[1] > 0) {
                String msg = getString(R.string.info_settings_downloaded, result[1]);
                broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_RESULT_MESSAGE, msg);
                broadcastIntent.putExtra(Constants.DataSyncServiceConstants.DOWNLOAD_COUNT_PRIMARY, result[1]);
                LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
            }
        } else {
            String msg = getString(R.string.error_settings_download);
            broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_RESULT_MESSAGE, msg);
            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
        }
    }

    private boolean authenticationSuccessful(String[] credentials, Intent broadcastIntent) {
        int authenticationStatus = muzimaSyncService.authenticate(credentials);
        if (authenticationStatus != Constants.DataSyncServiceConstants.SyncStatusConstants.AUTHENTICATION_SUCCESS) {
            broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_STATUS, authenticationStatus);
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

    private void broadCastMessageForNewConceptsDownloaded(Intent broadcastIntent, int[] result){
        String msg = getString(R.string.info_concepts_downloaded_deleted, result[1], result[2]);
        broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_STATUS, result[0]);
        broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_RESULT_MESSAGE, msg);
        if (isSuccess(result)) {
            broadcastIntent.putExtra(Constants.DataSyncServiceConstants.DELETED_COUNT_PRIMARY, result[2]);
            updateNotificationMsg(msg);
        }
        broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, Constants.DataSyncServiceConstants.SYNC_CONCEPTS_AND_OBS_BASED_ON_CHANGES_IN_CONFIG);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);

        broadCastMessageForObservationDownloadForNewConcepts(result);
    }

    private void broadCastMessageForObservationDownloadForNewConcepts(int[] resultForObservations) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(BroadcastListenerActivity.MESSAGE_SENT_ACTION);
        String msgForObservations = getString(R.string.info_obs_concept_downloaded, resultForObservations[3], resultForObservations[1], resultForObservations[4]);
        broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_STATUS, resultForObservations[0]);
        broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_RESULT_MESSAGE, msgForObservations);
        if (isSuccess(resultForObservations)) {
            broadcastIntent.putExtra(Constants.DataSyncServiceConstants.DOWNLOAD_COUNT_PRIMARY, resultForObservations[3]);
            broadcastIntent.putExtra(Constants.DataSyncServiceConstants.DOWNLOAD_COUNT_SECONDARY, resultForObservations[1]);
            broadcastIntent.putExtra(Constants.DataSyncServiceConstants.DELETED_COUNT_PRIMARY, resultForObservations[4]);
            updateNotificationMsg(msgForObservations);
        }
        broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, Constants.DataSyncServiceConstants.SYNC_OBS_BASED_ON_CONCEPTS_ADDED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
    }

    private void broadCastMessageForNewDerivedConceptsDownloaded(Intent broadcastIntent, int[] result){
        String msg = getString(R.string.info_derived_concepts_downloaded_deleted, result[1], result[2]);
        broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_STATUS, result[0]);
        broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_RESULT_MESSAGE, msg);
        if (isSuccess(result)) {
            broadcastIntent.putExtra(Constants.DataSyncServiceConstants.DELETED_COUNT_PRIMARY, result[2]);
            updateNotificationMsg(msg);
        }
        broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, Constants.DataSyncServiceConstants.SYNC_DERIVED_CONCEPTS_AND_OBS_BASED_ON_CHANGES_IN_CONFIG);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);

        broadCastMessageForDerivedObservationDownloadForNewConcepts(result);
    }

    private void broadCastMessageForDerivedObservationDownloadForNewConcepts(int[] resultForObservations) {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(BroadcastListenerActivity.MESSAGE_SENT_ACTION);
        String msgForObservations = getString(R.string.info_derived_observations_patients_downloaded, resultForObservations[3] , resultForObservations[4]);
        broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_STATUS, resultForObservations[0]);
        broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_RESULT_MESSAGE, msgForObservations);
        if (isSuccess(resultForObservations)) {
            broadcastIntent.putExtra(Constants.DataSyncServiceConstants.DOWNLOAD_COUNT_PRIMARY, resultForObservations[3]);
            broadcastIntent.putExtra(Constants.DataSyncServiceConstants.DOWNLOAD_COUNT_SECONDARY, resultForObservations[1]);
            broadcastIntent.putExtra(Constants.DataSyncServiceConstants.DELETED_COUNT_PRIMARY, resultForObservations[4]);
            updateNotificationMsg(msgForObservations);
        }
        broadcastIntent.putExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, Constants.DataSyncServiceConstants.SYNC_DERIVED_OBS);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
    }
}
