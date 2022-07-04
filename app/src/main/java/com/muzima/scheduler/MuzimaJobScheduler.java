/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.scheduler;

import static com.muzima.util.Constants.ServerSettings.GPS_FEATURE_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.PATIENT_IDENTIFIER_AUTOGENERATTION_SETTING;
import static com.muzima.util.Constants.ServerSettings.SHR_FEATURE_ENABLED_SETTING;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS;

import android.annotation.SuppressLint;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import android.widget.Toast;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Cohort;
import com.muzima.api.model.Concept;
import com.muzima.api.model.Form;
import com.muzima.api.model.Location;
import com.muzima.api.model.MuzimaSetting;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Person;
import com.muzima.api.model.Provider;
import com.muzima.api.model.ReportDataset;
import com.muzima.api.model.SetupConfigurationTemplate;
import com.muzima.api.model.User;
import com.muzima.controller.CohortController;
import com.muzima.controller.ConceptController;
import com.muzima.controller.FormController;
import com.muzima.controller.LocationController;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.PatientController;
import com.muzima.controller.ProviderController;
import com.muzima.controller.ReportDatasetController;
import com.muzima.controller.SetupConfigurationController;
import com.muzima.model.CompleteFormWithPatientData;
import com.muzima.model.DownloadedForm;
import com.muzima.model.IncompleteFormWithPatientData;
import com.muzima.model.collections.CompleteFormsWithPatientData;
import com.muzima.model.collections.DownloadedForms;
import com.muzima.model.collections.IncompleteFormsWithPatientData;
import com.muzima.service.MuzimaSyncService;
import com.muzima.service.RequireMedicalRecordNumberPreferenceService;
import com.muzima.service.SHRStatusPreferenceService;
import com.muzima.service.WizardFinishPreferenceService;
import com.muzima.util.JsonUtils;
import com.muzima.util.MuzimaSettingUtils;
import com.muzima.utils.Constants;
import com.muzima.utils.ProcessedTemporaryFormDataCleanUpIntent;
import com.muzima.utils.StringUtils;
import com.muzima.utils.SyncCohortsAndPatientFullDataIntent;
import com.muzima.utils.SyncSettingsIntent;
import com.muzima.view.forms.SyncFormIntent;
import com.muzima.view.forms.SyncFormTemplateIntent;
import com.muzima.view.patients.SyncPatientDataIntent;
import com.muzima.view.reports.SyncAllPatientReports;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressLint("NewApi")
public class MuzimaJobScheduler extends JobService {

    private MuzimaSyncService muzimaSynService;
    private String authenticatedUserUuid;
    private User authenticatedUser;
    private Person person;
    private boolean isAuthPerson = false;
    private MuzimaSettingController muzimaSettingController;
    private SetupConfigurationController setupConfigurationController;

    @Override
    public void onCreate() {
        super.onCreate();
        MuzimaApplication muzimaApplication = (MuzimaApplication) getApplicationContext();
        muzimaSettingController = muzimaApplication.getMuzimaSettingController();
        muzimaSynService = muzimaApplication.getMuzimaSyncService();
        setupConfigurationController = muzimaApplication.getSetupConfigurationController();
        authenticatedUser = muzimaApplication.getAuthenticatedUser();
        if (authenticatedUser != null){
            person = authenticatedUser.getPerson();

            if (person != null){
                authenticatedUserUuid = person.getUuid();
                isAuthPerson = true;
            }else{
                isAuthPerson = false;
            }

        }else {
            isAuthPerson = false;
            Log.i(getClass().getSimpleName(), "Authenticated user is not a person");
        }
    }

    @Override
    public boolean onStartJob(final JobParameters params) {

        if (authenticatedUser == null || !isAuthPerson) {
            onStopJob(params);
        } else {
            //execute job
            Toast.makeText(getApplicationContext(), R.string.info_background_data_sync_started,Toast.LENGTH_LONG).show();
            handleBackgroundWork(params);
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.i(getClass().getSimpleName(), "mUzima Job Service stopped" + params.getJobId());
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(getClass().getSimpleName(), "Service destroyed");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(getClass().getSimpleName(), "Downloading messages in Job");
        return START_NOT_STICKY;
    }

    private void handleBackgroundWork(JobParameters parameters) {
        if (parameters == null) {
            Log.e(getClass().getSimpleName(), "Parameters for job is null");
        } else {
            new SyncSetupConfigTemplatesBackgroundTask().execute();
            new CohortsAndPatientFullDataSyncBackgroundTask().execute();
            new FormDataUploadBackgroundTask().execute();
            new ProcessedTemporaryFormDataCleanUpBackgroundTask().execute();
            new SyncSettinsBackgroundTask().execute();
            if(muzimaSettingController.isClinicalSummaryEnabled()) {
                new SyncAllPatientReportsBackgroundTask().execute();
            }
            new FormMetaDataSyncBackgroundTask().execute();
        }
    }

    private class  ProcessedTemporaryFormDataCleanUpBackgroundTask extends AsyncTask<Void,Void,Void>{

        @Override
        protected Void doInBackground(Void... voids) {
           new ProcessedTemporaryFormDataCleanUpIntent(getApplicationContext()).start();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private class CohortsAndPatientFullDataSyncBackgroundTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            if (new WizardFinishPreferenceService(MuzimaJobScheduler.this).isWizardFinished()) {
                new SyncCohortsAndPatientFullDataIntent(getApplicationContext()).start();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private class FormDataUploadBackgroundTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            if (new WizardFinishPreferenceService(getApplicationContext()).isWizardFinished()) {
                RealTimeFormUploader.getInstance().uploadAllCompletedForms(getApplicationContext(),true);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private class FormMetaDataSyncBackgroundTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Context context = getApplicationContext();
                if (new WizardFinishPreferenceService(context).isWizardFinished() &&
                        !((MuzimaApplication) context).getFormController().isFormWithPatientDataAvailable(context)) {

                    new SyncFormIntent(getApplicationContext()).start();
                } else {
                    Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not sync form metadata. Incomplete/unsyched forms exist");
                }
            } catch (FormController.FormFetchException e){
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not sync form metadata",e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private class SyncSettinsBackgroundTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            new SyncSettingsIntent(getApplicationContext()).start();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private class SyncAllPatientReportsBackgroundTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            new SyncAllPatientReports(getApplicationContext()).start();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private class SyncSetupConfigTemplatesBackgroundTask extends AsyncTask<Void,Void,Void> {
        Context context = getApplicationContext();
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                for (SetupConfigurationTemplate template : setupConfigurationController.getSetupConfigurationTemplates()) {
                    int[] templateResult = downloadAndSaveUpdatedSetupConfigurationTemplate(template.getUuid());
                    if (templateResult[0] == SUCCESS) {
                        List<MuzimaSetting> settings = muzimaSettingController.getSettingsFromSetupConfigurationTemplate(template.getUuid());

                        updateSettingsPreferences(settings);
                    }
                }
            } catch (SetupConfigurationController.SetupConfigurationFetchException e) {
                Log.e(getClass().getSimpleName(),"Exception while fetching setup configs ",e);
            } catch (MuzimaSettingController.MuzimaSettingFetchException e) {
                Log.e(getClass().getSimpleName(),"Exception while fetching config settings ",e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            new SyncReportDatasetsBackgroundTask().execute();
            new FormTemplateSyncBackgroundTask().execute();
            new DownloadAndDeleteCohortsBasedOnConfigChangesBackgroundTask().execute();
            new DownloadAndDeleteLocationBasedOnConfigChangesBackgroundTask().execute();
            new DownloadAndDeleteProvidersBasedOnConfigChangesBackgroundTask().execute();
            new DownloadAndDeleteConceptsBasedOnConfigChangesBackgroundTask().execute();
        }

        public int[] downloadAndSaveUpdatedSetupConfigurationTemplate(String uuid) {
            int[] result = new int[2];
            try {
                SetupConfigurationTemplate setupConfigurationTemplate =
                        setupConfigurationController.downloadUpdatedSetupConfigurationTemplate(uuid);
                result[0] = SUCCESS;
                if (setupConfigurationTemplate != null) {
                    result[1] = 1;
                    setupConfigurationController.updateSetupConfigurationTemplate(setupConfigurationTemplate);
                }

            } catch (SetupConfigurationController.SetupConfigurationDownloadException e) {
                Log.e(getClass().getSimpleName(), "Exception when trying to download setup configs");
                result[0] = Constants.DataSyncServiceConstants.SyncStatusConstants.DOWNLOAD_ERROR;
            } catch (SetupConfigurationController.SetupConfigurationSaveException e) {
                Log.e(getClass().getSimpleName(), "Exception when trying to save setup configs");
                result[0] = Constants.DataSyncServiceConstants.SyncStatusConstants.SAVE_ERROR;
            }
            return result;
        }

        public void updateSettingsPreferences(List<MuzimaSetting> muzimaSettings) {
            List<String> configSettings = new ArrayList<>();
            List<String> preferenceSettings = new ArrayList<>();
            preferenceSettings.add(SHR_FEATURE_ENABLED_SETTING);
            preferenceSettings.add(GPS_FEATURE_ENABLED_SETTING);
            preferenceSettings.add(PATIENT_IDENTIFIER_AUTOGENERATTION_SETTING);
            for (MuzimaSetting muzimaSetting : muzimaSettings) {
                configSettings.add(muzimaSetting.getProperty());
                if (MuzimaSettingUtils.isGpsFeatureEnabledSetting(muzimaSetting)) {
                    ((MuzimaApplication) context).getGPSFeaturePreferenceService().updateGPSDataPreferenceSettings();
                } else if (MuzimaSettingUtils.isSHRFeatureEnabledSetting(muzimaSetting)) {
                    new SHRStatusPreferenceService(((MuzimaApplication) context)).updateSHRStatusPreference();
                } else if (MuzimaSettingUtils.isPatientIdentifierAutogenerationSetting(muzimaSetting)) {
                    new RequireMedicalRecordNumberPreferenceService(((MuzimaApplication) context)).updateRequireMedicalRecordNumberPreference();
                }
            }

            /*check if the 3 mobile settings preferences are in setup else default to global, might have been deleted from the config*/
            for(String settingProperty : preferenceSettings){
                if(!configSettings.contains(settingProperty)){
                    defaultToGlobalSettings(settingProperty);
                }
            }
        }

        public void defaultToGlobalSettings(String settingProperty){
            if (settingProperty.equals(GPS_FEATURE_ENABLED_SETTING)) {
                ((MuzimaApplication) context).getGPSFeaturePreferenceService().updateGPSDataPreferenceSettings();
            } else if (settingProperty.equals(SHR_FEATURE_ENABLED_SETTING)) {
                new SHRStatusPreferenceService(((MuzimaApplication) context)).updateSHRStatusPreference();
            } else if (settingProperty.equals(PATIENT_IDENTIFIER_AUTOGENERATTION_SETTING)) {
                new RequireMedicalRecordNumberPreferenceService(((MuzimaApplication) context)).updateRequireMedicalRecordNumberPreference();
            }
        }
    }

    private class SyncReportDatasetsBackgroundTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            Context context = getApplicationContext();
            ReportDatasetController reportDatasetController = ((MuzimaApplication) context).getReportDatasetController();
            try {
                //Get datasets in the config
                String configJson = "";
                List<Integer> datasetIds = new ArrayList<>();

                SetupConfigurationTemplate activeSetupConfig = setupConfigurationController.getActiveSetupConfigurationTemplate();
                configJson = activeSetupConfig.getConfigJson();
                List<Object> datasets = JsonUtils.readAsObjectList(configJson, "$['config']['datasets']");
                for (Object dataset : datasets) {
                    net.minidev.json.JSONObject dataset1 = (net.minidev.json.JSONObject) dataset;
                    Integer datasetId = (Integer)dataset1.get("id");
                    datasetIds.add(datasetId);
                }

                List<ReportDataset> reportDatasets = reportDatasetController.getReportDatasets();
                List<Integer> datasetToDeleteIds = new ArrayList<>();
                List<Integer> downloadedDatasetIds = new ArrayList<>();
                List<Integer> datasetToDownload= new ArrayList<>();

                //Get datasets previously downloaded but not in the updated config
                for(ReportDataset downloadedDataset: reportDatasets){
                    if(!datasetIds.contains(downloadedDataset.getDatasetDefinitionId())){
                        datasetToDeleteIds.add(downloadedDataset.getDatasetDefinitionId());
                    }
                    downloadedDatasetIds.add(downloadedDataset.getDatasetDefinitionId());
                }

                //sync the downloaded datasets with changes
                if(downloadedDatasetIds.size() > 0){
                    List<ReportDataset> reportDatasetList = reportDatasetController.downloadReportDatasets(downloadedDatasetIds, true);
                    reportDatasetController.saveReportDatasets(reportDatasetList);
                }

                //Get Added datasets to updated config
                for(Integer datasetId : datasetIds){
                    if(!downloadedDatasetIds.contains(datasetId)){
                        datasetToDownload.add(datasetId);
                    }
                }

                if(datasetToDeleteIds.size() > 0) {
                    reportDatasetController.deleteReportDatasets(datasetToDeleteIds);
                }

                if(datasetToDownload.size()>0){
                    //Download Added datasets
                    List<ReportDataset> reportDatasetList = reportDatasetController.downloadReportDatasets(downloadedDatasetIds, false);
                    reportDatasetController.saveReportDatasets(reportDatasetList);
                }
            } catch (ReportDatasetController.ReportDatasetSaveException reportDatasetSaveException) {
                reportDatasetSaveException.printStackTrace();
            } catch (SetupConfigurationController.SetupConfigurationFetchException setupConfigurationFetchException) {
                setupConfigurationFetchException.printStackTrace();
            } catch (ReportDatasetController.ReportDatasetDownloadException reportDatasetDownloadException) {
                reportDatasetDownloadException.printStackTrace();
            } catch (ReportDatasetController.ReportDatasetFetchException reportDatasetFetchException) {
                reportDatasetFetchException.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private class FormTemplateSyncBackgroundTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Context context = getApplicationContext();
                FormController formController = ((MuzimaApplication) context).getFormController();
                //Get forms in the config
                String configJson = "";
                List<String> formUuids = new ArrayList<>();

                SetupConfigurationTemplate activeSetupConfig = setupConfigurationController.getActiveSetupConfigurationTemplate();
                configJson = activeSetupConfig.getConfigJson();
                List<Object> forms = JsonUtils.readAsObjectList(configJson, "$['config']['forms']");
                for (Object form : forms) {
                    net.minidev.json.JSONObject form1 = (net.minidev.json.JSONObject) form;
                    String formUuid = form1.get("uuid").toString();
                    formUuids.add(formUuid);
                }

                DownloadedForms downloadedForms = formController.getAllDownloadedForms();
                List<String> formTemplatesToDeleteUuids = new ArrayList<>();
                List<String> downloadedFormUuids = new ArrayList<>();
                List<String>  formTemplateToDownload= new ArrayList<>();

                //Get forms previously downloaded but not in the updated config
                for(DownloadedForm downloadedForm: downloadedForms){
                    if(!formUuids.contains(downloadedForm.getFormUuid())){
                        formTemplatesToDeleteUuids.add(downloadedForm.getFormUuid());
                    }
                    downloadedFormUuids.add(downloadedForm.getFormUuid());
                }

                //Get Added forms to updated config
                for(String formUuid : formUuids){
                    if(!downloadedFormUuids.contains(formUuid)){
                        formTemplateToDownload.add(formUuid);
                    }
                }

                //Get Forms with Updates
                List<Form> allForms = formController.getAllAvailableForms();
                for (Form form : allForms) {
                    if (form.isUpdateAvailable() && formUuids.contains(form.getUuid())) {
                        formTemplateToDownload.add(form.getUuid());
                    }
                }

                boolean isFormWithPatientDataAvailable = formController.isFormWithPatientDataAvailable(context);

                if(!isFormWithPatientDataAvailable){
                    String[] formsToDownload = formTemplateToDownload.stream().toArray(String[]::new);

                    if(formTemplatesToDeleteUuids.size()>0)
                        formController.deleteFormTemplatesByUUID(formTemplatesToDeleteUuids);

                    if(formTemplateToDownload.size()>0)
                        new SyncFormTemplateIntent(context, formsToDownload).start();
                }else{
                    List<String> formsWithPatientData = new ArrayList<>();

                    CompleteFormsWithPatientData completeFormsWithPatientData = formController.getAllCompleteFormsWithPatientData(context, StringUtils.EMPTY);
                    IncompleteFormsWithPatientData incompleteFormsWithPatientData = formController.getAllIncompleteFormsWithPatientData(StringUtils.EMPTY);

                    for(CompleteFormWithPatientData completeFormWithPatientData : completeFormsWithPatientData){
                        formsWithPatientData.add(completeFormWithPatientData.getFormUuid());
                    }

                    for(IncompleteFormWithPatientData inCompleteFormWithPatientData : incompleteFormsWithPatientData){
                        formsWithPatientData.add(inCompleteFormWithPatientData.getFormUuid());
                    }

                    for(String formTemplateToDeleteUuid : formTemplatesToDeleteUuids) {
                        if (!formsWithPatientData.contains(formTemplateToDeleteUuid)) {
                            //Delete form template
                            formController.deleteFormTemplatesByUUID(Collections.singletonList(formTemplateToDeleteUuid));
                        }
                    }

                    List<String> formsToDownloadUuids = new ArrayList<>();
                    for(String formTemplateUuidToDownload : formTemplateToDownload) {
                        if (!formsWithPatientData.contains(formTemplateUuidToDownload)) {
                            formsToDownloadUuids.add(formTemplateUuidToDownload);

                        }
                    }
                    if(formsToDownloadUuids.size()>0){
                        //Download Templates
                        new SyncFormTemplateIntent(context, formsToDownloadUuids.stream().toArray(String[]::new)).start();
                    }
                }
            } catch (FormController.FormFetchException e){
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not fetch downloaded forms ",e);
            } catch (SetupConfigurationController.SetupConfigurationFetchException e){
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not get the active config ",e);
            } catch (FormController.FormDeleteException e) {
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not delete form templates ",e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private class DownloadAndDeleteCohortsBasedOnConfigChangesBackgroundTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Context context = getApplicationContext();
                CohortController cohortController = ((MuzimaApplication) context).getCohortController();
                //Get cohorts in the config
                String configJson = "";
                List<String> cohortUuids = new ArrayList<>();

                SetupConfigurationTemplate activeSetupConfig = setupConfigurationController.getActiveSetupConfigurationTemplate();
                configJson = activeSetupConfig.getConfigJson();
                List<Object> cohorts = JsonUtils.readAsObjectList(configJson, "$['config']['cohorts']");
                for (Object cohort : cohorts) {
                    net.minidev.json.JSONObject cohort1 = (net.minidev.json.JSONObject) cohort;
                    String cohortUuid = cohort1.get("uuid").toString();
                    cohortUuids.add(cohortUuid);
                }

                List<Cohort> syncedCohorts = cohortController.getSyncedCohorts();
                List<String> cohortsToSetAsUnsyncedUuids = new ArrayList<>();
                List<String> downloadedCohortUuids = new ArrayList<>();
                List<String> cohortsToDownload= new ArrayList<>();

                //Get cohorts previously downloaded but not in the updated config
                for(Cohort cohort: syncedCohorts){
                    if(!cohortUuids.contains(cohort.getUuid())){
                        cohortsToSetAsUnsyncedUuids.add(cohort.getUuid());
                    }
                    downloadedCohortUuids.add(cohort.getUuid());
                }

                //Get Added cohorts to updated config
                for(String cohortUuid : cohortUuids){
                    if(!downloadedCohortUuids.contains(cohortUuid)){
                        cohortsToDownload.add(cohortUuid);
                    }
                }

                if(cohortsToSetAsUnsyncedUuids.size()>0) {
                    cohortController.setSyncStatus(cohortsToSetAsUnsyncedUuids.stream().toArray(String[]::new), 0);
                    cohortController.deletePatientsNotBelongingToAnotherCohortByCohortUuids(cohortsToSetAsUnsyncedUuids);
                    cohortController.deleteAllCohortMembersByCohortUuids(cohortsToSetAsUnsyncedUuids);

                }

                if(cohortsToDownload.size()>0)
                    new SyncPatientDataIntent(context, cohortsToDownload.stream().toArray(String[]::new)).start();

            } catch (SetupConfigurationController.SetupConfigurationFetchException e){
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not get the active config ",e);
            } catch (CohortController.CohortFetchException e) {
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not be able to fetch cohort ",e);
            } catch (CohortController.CohortUpdateException e) {
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not able to update cohort ",e);
            } catch (CohortController.CohortReplaceException e) {
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not able to replace cohort ",e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private class DownloadAndDeleteLocationBasedOnConfigChangesBackgroundTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Context context = getApplicationContext();
                LocationController locationController = ((MuzimaApplication) context).getLocationController();
                //Get locations in the config
                String configJson = "";
                List<String> locationUuids = new ArrayList<>();

                SetupConfigurationTemplate activeSetupConfig = setupConfigurationController.getActiveSetupConfigurationTemplate();
                configJson = activeSetupConfig.getConfigJson();
                List<Object> locations = JsonUtils.readAsObjectList(configJson, "$['config']['locations']");
                for (Object location : locations) {
                    net.minidev.json.JSONObject location1 = (net.minidev.json.JSONObject) location;
                    String locationUuid = location1.get("uuid").toString();
                    locationUuids.add(locationUuid);
                }

                List<Location> downloadedLocations = locationController.getAllLocations();
                List<Location> locationsToBeDeleted = new ArrayList<>();
                List<String> downloadedLocationUuids = new ArrayList<>();
                List<String> locationsToDownload= new ArrayList<>();

                //Get locations previously downloaded but not in the updated config
                for(Location location: downloadedLocations){
                    if(!locationUuids.contains(location.getUuid())){
                        locationsToBeDeleted.add(location);
                    }
                    downloadedLocationUuids.add(location.getUuid());
                }

                //Get Added locations to updated config
                for(String locationUuid : locationUuids){
                    if(!downloadedLocationUuids.contains(locationUuid)){
                        locationsToDownload.add(locationUuid);
                    }
                }

                if(locationsToBeDeleted.size()>0) {
                    locationController.deleteLocations(locationsToBeDeleted);
                }

                if(locationsToDownload.size()>0) {
                    List<Location> locationList = locationController.downloadLocationsFromServerByUuid(locationsToDownload.stream().toArray(String[]::new));
                    locationController.saveLocations(locationList);
                }

            } catch (SetupConfigurationController.SetupConfigurationFetchException e){
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not get the active config ",e);
            } catch (LocationController.LocationLoadException e) {
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not get locations ",e);
            } catch (LocationController.LocationDeleteException e) {
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not delete locations ",e);
            } catch (LocationController.LocationDownloadException e) {
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not download locations ",e);
            } catch (LocationController.LocationSaveException e) {
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not save locations ",e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private class DownloadAndDeleteProvidersBasedOnConfigChangesBackgroundTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Context context = getApplicationContext();
                ProviderController providerController = ((MuzimaApplication) context).getProviderController();
                //Get providers in the config
                String configJson = "";
                List<String> providerUuids = new ArrayList<>();

                SetupConfigurationTemplate activeSetupConfig = setupConfigurationController.getActiveSetupConfigurationTemplate();
                configJson = activeSetupConfig.getConfigJson();
                List<Object> providers = JsonUtils.readAsObjectList(configJson, "$['config']['providers']");
                for (Object provider : providers) {
                    net.minidev.json.JSONObject provider1 = (net.minidev.json.JSONObject) provider;
                    String providerUuid = provider1.get("uuid").toString();
                    providerUuids.add(providerUuid);
                }

                List<Provider> syncedproviders = providerController.getAllProviders();
                List<Provider> providersToBeDeleted = new ArrayList<>();
                List<String> downloadedproviderUuids = new ArrayList<>();
                List<String> providersToDownload= new ArrayList<>();

                //Get providers previously downloaded but not in the updated config
                for(Provider provider: syncedproviders){
                    if(!providerUuids.contains(provider.getUuid())){
                        providersToBeDeleted.add(provider);
                    }
                    downloadedproviderUuids.add(provider.getUuid());
                }

                //Get Added providers to updated config
                for(String providerUuid : providerUuids){
                    if(!downloadedproviderUuids.contains(providerUuid)){
                        providersToDownload.add(providerUuid);
                    }
                }

                if(providersToBeDeleted.size()>0) {
                    providerController.deleteProviders(providersToBeDeleted);
                }

                if(providersToDownload.size()>0) {
                    List<Provider> providerList = providerController.downloadProvidersFromServerByUuid(providersToDownload.stream().toArray(String[]::new));
                    providerController.saveProviders(providerList);
                }

            } catch (SetupConfigurationController.SetupConfigurationFetchException e){
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not get the active config ",e);
            } catch (ProviderController.ProviderLoadException e) {
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not load providers ",e);
            } catch (ProviderController.ProviderDeleteException e) {
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not delete providers ",e);
            } catch (ProviderController.ProviderDownloadException e) {
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not download providers ",e);
            } catch (ProviderController.ProviderSaveException e) {
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not save providers ",e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private class  DownloadAndDeleteConceptsBasedOnConfigChangesBackgroundTask extends AsyncTask<Void,Void,Void>{
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Context context = getApplicationContext();
                ConceptController conceptController = ((MuzimaApplication) context).getConceptController();
                ObservationController observationController = ((MuzimaApplication) context).getObservationController();
                //Get concepts in the config
                String configJson = "";
                List<String> conceptUuids = new ArrayList<>();

                SetupConfigurationTemplate activeSetupConfig = setupConfigurationController.getActiveSetupConfigurationTemplate();
                configJson = activeSetupConfig.getConfigJson();
                List<Object> concepts = JsonUtils.readAsObjectList(configJson, "$['config']['concepts']");
                for (Object concept : concepts) {
                    net.minidev.json.JSONObject concept1 = (net.minidev.json.JSONObject) concept;
                    String conceptUuid = concept1.get("uuid").toString();
                    conceptUuids.add(conceptUuid);
                }

                List<Concept> downloadedConcepts = conceptController.getConcepts();
                List<Concept> conceptsToBeDeleted = new ArrayList<>();
                List<String> downloadedConceptUuids = new ArrayList<>();
                List<String> conceptsToDownload= new ArrayList<>();

                //Get concepts previously downloaded but not in the updated config
                for(Concept concept: downloadedConcepts){
                    if(!conceptUuids.contains(concept.getUuid())){
                        conceptsToBeDeleted.add(concept);
                    }
                    downloadedConceptUuids.add(concept.getUuid());
                }

                //Get Added concepts to updated config
                for(String conceptUuid : conceptUuids){
                    if(!downloadedConceptUuids.contains(conceptUuid)){
                        conceptsToDownload.add(conceptUuid);
                    }
                }

                if(conceptsToBeDeleted.size()>0) {
                    conceptController.deleteConcepts(conceptsToBeDeleted);
                    observationController.deleteAllObservations(conceptsToBeDeleted);
                }

                if(conceptsToDownload.size()>0) {
                    List<Patient> patients = ((MuzimaApplication) context).getPatientController().getAllPatients();
                    List<String> patientUuids = new ArrayList<>();
                    for(Patient patient : patients){
                        patientUuids.add(patient.getUuid());
                    }
                    List<List<String>> slicedPatientUuids = split(patientUuids);
                    List<List<String>> slicedConceptUuids = split(conceptsToDownload);

                    List<Concept> conceptList = conceptController.downloadConceptsByUuid(conceptsToDownload.stream().toArray(String[]::new));
                    if(conceptList.size()>0){
                        List<Observation> observations = new ArrayList<>();

                        conceptController.saveConcepts(conceptList);
                        for (List<String> slicedPatientUuid : slicedPatientUuids) {
                            for (List<String> slicedConceptUuid : slicedConceptUuids) {
                                List<Observation> observationsDownloaded = observationController.downloadObservationsForAddedConceptsByPatientUuidsAndConceptUuids(slicedPatientUuid, slicedConceptUuid,activeSetupConfig.getUuid());

                                if(observationsDownloaded.size() > 0){
                                    observations.addAll(observationsDownloaded);
                                }
                            }
                        }
                        if(observations.size() > 0){
                            observationController.saveObservations(observations);
                        }
                    }
                }

            } catch (SetupConfigurationController.SetupConfigurationFetchException e){
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not get the active config ",e);
            } catch (ConceptController.ConceptFetchException e) {
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not get concepts ",e);
            } catch (ConceptController.ConceptDeleteException e) {
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not delete concepts ",e);
            } catch (ObservationController.DeleteObservationException e) {
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not delete observations ",e);
            } catch (ConceptController.ConceptDownloadException e) {
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not download concepts ",e);
            } catch (PatientController.PatientLoadException e) {
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not load patients ",e);
            } catch (ObservationController.DownloadObservationException e) {
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not download observations ",e);
            } catch (ObservationController.SaveObservationException e) {
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not save observations ",e);
            } catch (ConceptController.ConceptSaveException e) {
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not save concepts ",e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }

        private List<List<String>> split(final List<String> strings) {
            List<List<String>> lists = new ArrayList<>();

            int count = 0;
            boolean hasElements = !strings.isEmpty();
            while (hasElements) {
                int startElement = count * 100;
                int endElement = ++count * 100;
                hasElements = strings.size() > endElement;
                if (hasElements) {
                    lists.add(strings.subList(startElement, endElement));
                } else {
                    lists.add(strings.subList(startElement, strings.size()));
                }
            }

            return lists;
        }
    }
}
