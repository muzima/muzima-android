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

import static com.muzima.util.Constants.ServerSettings.DEFAULT_ENCOUNTER_LOCATION_SETTING;
import static com.muzima.util.Constants.ServerSettings.DEFAULT_LOGGED_IN_USER_AS_ENCOUNTER_PROVIDER_SETTING;
import static com.muzima.util.Constants.ServerSettings.GPS_FEATURE_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.NOTIFICATION_FEATURE_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.ONLINE_ONLY_MODE_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.PATIENT_IDENTIFIER_AUTOGENERATTION_SETTING;
import static com.muzima.util.Constants.ServerSettings.SHR_FEATURE_ENABLED_SETTING;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS;
import static com.muzima.utils.Constants.STANDARD_DATE_TIMEZONE_FORMAT;
import static com.muzima.utils.DeviceDetailsUtil.generatePseudoDeviceId;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.DownloadManager;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import android.widget.Toast;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.AppUsageLogs;
import com.muzima.api.model.Concept;
import com.muzima.api.model.Form;
import com.muzima.api.model.Location;
import com.muzima.api.model.Media;
import com.muzima.api.model.MediaCategory;
import com.muzima.api.model.MuzimaSetting;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Person;
import com.muzima.api.model.Provider;
import com.muzima.api.model.ReportDataset;
import com.muzima.api.model.SetupConfigurationTemplate;
import com.muzima.api.model.User;
import com.muzima.controller.AppUsageLogsController;
import com.muzima.controller.CohortController;
import com.muzima.controller.ConceptController;
import com.muzima.controller.FCMTokenController;
import com.muzima.controller.FormController;
import com.muzima.controller.LocationController;
import com.muzima.controller.MediaCategoryController;
import com.muzima.controller.MediaController;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.PatientController;
import com.muzima.controller.ProviderController;
import com.muzima.controller.ReportDatasetController;
import com.muzima.controller.SetupConfigurationController;
import com.muzima.model.CompleteFormWithPatientData;
import com.muzima.model.IncompleteFormWithPatientData;
import com.muzima.model.collections.CompleteFormsWithPatientData;
import com.muzima.model.collections.IncompleteFormsWithPatientData;
import com.muzima.service.MuzimaSyncService;
import com.muzima.service.OnlineOnlyModePreferenceService;
import com.muzima.service.RequireMedicalRecordNumberPreferenceService;
import com.muzima.service.SHRStatusPreferenceService;
import com.muzima.service.WizardFinishPreferenceService;
import com.muzima.tasks.MuzimaAsyncTask;
import com.muzima.util.JsonUtils;
import com.muzima.util.MuzimaSettingUtils;
import com.muzima.utils.Constants;
import com.muzima.utils.MemoryUtil;
import com.muzima.utils.NetworkUtils;
import com.muzima.utils.ProcessedTemporaryFormDataCleanUpIntent;
import com.muzima.utils.StringUtils;
import com.muzima.utils.SyncCohortsAndPatientFullDataIntent;
import com.muzima.utils.SyncSettingsIntent;
import com.muzima.view.MainDashboardActivity;
import com.muzima.view.forms.SyncFormIntent;
import com.muzima.view.forms.SyncFormTemplateIntent;
import com.muzima.view.initialwizard.GuidedConfigurationWizardActivity;
import com.muzima.view.patients.SyncPatientDataIntent;
import com.muzima.view.reports.SyncAllPatientReports;

import org.apache.lucene.queryParser.ParseException;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@SuppressLint("NewApi")
public class MuzimaJobScheduler extends JobService {

    private MuzimaSyncService muzimaSynService;
    private String authenticatedUserUuid;
    private User authenticatedUser;
    private Person person;
    private boolean isAuthPerson = false;
    private MuzimaSettingController muzimaSettingController;
    private SetupConfigurationController setupConfigurationController;
    private String pseudoDeviceId;
    private String username;
    private SetupConfigurationTemplate configBeforeConfigUpdate;
    private FCMTokenController fcmTokenController;
    private AppUsageLogsController  appUsageLogsController;

    @Override
    public void onCreate() {
        super.onCreate();
        MuzimaApplication muzimaApplication = (MuzimaApplication) getApplicationContext();
        muzimaSettingController = muzimaApplication.getMuzimaSettingController();
        muzimaSynService = muzimaApplication.getMuzimaSyncService();
        setupConfigurationController = muzimaApplication.getSetupConfigurationController();
        fcmTokenController = muzimaApplication.getFCMTokenController();
        appUsageLogsController = muzimaApplication.getAppUsageLogsController();
        pseudoDeviceId = generatePseudoDeviceId();
        username = muzimaApplication.getAuthenticatedUserId();
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
                FormController formController = ((MuzimaApplication) getApplicationContext()).getFormController();
                try {
                    if(formController.countAllCompleteForms() > 0 && NetworkUtils.isConnectedToNetwork(getApplicationContext())) {
                        SimpleDateFormat simpleDateTimezoneFormat = new SimpleDateFormat(STANDARD_DATE_TIMEZONE_FORMAT);
                        AppUsageLogs lastUploadLog = appUsageLogsController.getAppUsageLogByKeyAndUserName(com.muzima.util.Constants.AppUsageLogs.LAST_UPLOAD_TIME, username);
                        if (lastUploadLog != null) {
                            lastUploadLog.setLogvalue(simpleDateTimezoneFormat.format(new Date()));
                            lastUploadLog.setUpdateDatetime(new Date());
                            lastUploadLog.setUserName(username);
                            lastUploadLog.setDeviceId(pseudoDeviceId);
                            lastUploadLog.setLogSynced(false);
                            appUsageLogsController.saveOrUpdateAppUsageLog(lastUploadLog);
                        } else {
                            AppUsageLogs newUploadTime = new AppUsageLogs();
                            newUploadTime.setUuid(UUID.randomUUID().toString());
                            newUploadTime.setLogKey(com.muzima.util.Constants.AppUsageLogs.LAST_UPLOAD_TIME);
                            newUploadTime.setLogvalue(simpleDateTimezoneFormat.format(new Date()));
                            newUploadTime.setUpdateDatetime(new Date());
                            newUploadTime.setUserName(username);
                            newUploadTime.setDeviceId(pseudoDeviceId);
                            newUploadTime.setLogSynced(false);
                            appUsageLogsController.saveOrUpdateAppUsageLog(newUploadTime);
                        }
                    }
                } catch (IOException e) {
                    Log.e(getClass().getSimpleName(),"Encountered IO Exception ",e);
                } catch (ParseException e) {
                    Log.e(getClass().getSimpleName(),"Encountered Parse Exception ",e);
                } catch (FormController.FormFetchException e) {
                    Log.e(getClass().getSimpleName(), "Encountered exception while fetching forms ",e);
                }
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
        Boolean wasConfigUpdateDone = false;
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                configBeforeConfigUpdate = setupConfigurationController.getActiveSetupConfigurationTemplate();
                for (SetupConfigurationTemplate template : setupConfigurationController.getSetupConfigurationTemplates()) {
                    int[] templateResult = downloadAndSaveUpdatedSetupConfigurationTemplate(template.getUuid());
                    if (templateResult[0] == SUCCESS && wasConfigUpdateDone) {
                        List<MuzimaSetting> settings = muzimaSettingController.getSettingsFromSetupConfigurationTemplate(template.getUuid());

                        updateSettingsPreferences(settings);

                        try {
                            SimpleDateFormat simpleDateTimezoneFormat = new SimpleDateFormat(STANDARD_DATE_TIMEZONE_FORMAT);
                            AppUsageLogs lastSetupUpdateLog = appUsageLogsController.getAppUsageLogByKeyAndUserName(com.muzima.util.Constants.AppUsageLogs.SETUP_UPDATE_TIME, username);
                            if(lastSetupUpdateLog != null){
                                lastSetupUpdateLog.setLogvalue(simpleDateTimezoneFormat.format(new Date()));
                                lastSetupUpdateLog.setUpdateDatetime(new Date());
                                lastSetupUpdateLog.setUserName(username);
                                lastSetupUpdateLog.setDeviceId(pseudoDeviceId);
                                lastSetupUpdateLog.setLogSynced(false);
                                appUsageLogsController.saveOrUpdateAppUsageLog(lastSetupUpdateLog);
                            }else{
                                AppUsageLogs newSetupUpdateTime = new AppUsageLogs();
                                newSetupUpdateTime.setUuid(UUID.randomUUID().toString());
                                newSetupUpdateTime.setLogKey(com.muzima.util.Constants.AppUsageLogs.SETUP_UPDATE_TIME);
                                newSetupUpdateTime.setLogvalue(simpleDateTimezoneFormat.format(new Date()));
                                newSetupUpdateTime.setUpdateDatetime(new Date());
                                newSetupUpdateTime.setUserName(username);
                                newSetupUpdateTime.setDeviceId(pseudoDeviceId);
                                newSetupUpdateTime.setLogSynced(false);
                                appUsageLogsController.saveOrUpdateAppUsageLog(newSetupUpdateTime);
                            }
                        } catch (IOException e) {
                            Log.e(getClass().getSimpleName(),"Encountered IO Exception ",e);
                        } catch (ParseException e) {
                            Log.e(getClass().getSimpleName(),"Encountered Parse Exception ",e);
                        }
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
            new MediaCategorySyncBackgroundTask().execute();
            if(wasConfigUpdateDone) {
                if (!muzimaSettingController.isOnlineOnlyModeEnabled())
                    new DownloadAndDeleteCohortsBasedOnConfigChangesBackgroundTask().execute();
                new DownloadAndDeleteLocationBasedOnConfigChangesBackgroundTask().execute();
                new DownloadAndDeleteProvidersBasedOnConfigChangesBackgroundTask().execute();
                new DownloadAndDeleteConceptsBasedOnConfigChangesBackgroundTask().execute();
            }
        }

        public int[] downloadAndSaveUpdatedSetupConfigurationTemplate(String uuid) {
            int[] result = new int[2];
            try {
                SetupConfigurationTemplate setupConfigurationTemplate =
                        setupConfigurationController.downloadUpdatedSetupConfigurationTemplate(uuid);
                result[0] = SUCCESS;
                if (setupConfigurationTemplate != null) {
                    result[1] = 1;
                    wasConfigUpdateDone = true;
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
            preferenceSettings.add(DEFAULT_LOGGED_IN_USER_AS_ENCOUNTER_PROVIDER_SETTING);
            preferenceSettings.add(DEFAULT_ENCOUNTER_LOCATION_SETTING);
            preferenceSettings.add(ONLINE_ONLY_MODE_ENABLED_SETTING);

            boolean onlineModeBeforeConfigUpdate = false;

            String configJsonBeforeConfigUpdate = configBeforeConfigUpdate.getConfigJson();
            List<Object> settingsBeforeConfigUpdate = JsonUtils.readAsObjectList(configJsonBeforeConfigUpdate, "$['config']['settings']");
            for (Object setting : settingsBeforeConfigUpdate) {
                net.minidev.json.JSONObject setting1 = (net.minidev.json.JSONObject) setting;
                String property = (String)setting1.get("property");
                if(property.equals(ONLINE_ONLY_MODE_ENABLED_SETTING)){
                    onlineModeBeforeConfigUpdate = (Boolean) setting1.get("value");
                }
            }

            for (MuzimaSetting muzimaSetting : muzimaSettings) {
                configSettings.add(muzimaSetting.getProperty());
                if (MuzimaSettingUtils.isGpsFeatureEnabledSetting(muzimaSetting)) {
                    ((MuzimaApplication) context).getGPSFeaturePreferenceService().updateGPSDataPreferenceSettings();
                } else if (MuzimaSettingUtils.isSHRFeatureEnabledSetting(muzimaSetting)) {
                    new SHRStatusPreferenceService(((MuzimaApplication) context)).updateSHRStatusPreference();
                } else if (MuzimaSettingUtils.isPatientIdentifierAutogenerationSetting(muzimaSetting)) {
                    new RequireMedicalRecordNumberPreferenceService(((MuzimaApplication) context)).updateRequireMedicalRecordNumberPreference();
                }else if (MuzimaSettingUtils.isOnlineOnlyModeSetting(muzimaSetting)) {
                    if(onlineModeBeforeConfigUpdate != muzimaSetting.getValueBoolean()){
                        muzimaSettingController.updateTheme();
                        if(muzimaSetting.getValueBoolean()) {
                            Intent intent;
                            intent = new Intent(((MuzimaApplication) context), MainDashboardActivity.class);
                            intent.putExtra("OnlineMode", muzimaSetting.getValueBoolean());
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            }
                            ((MuzimaApplication) context).startActivity(intent);
                        }else{
                            ActivityManager am = (ActivityManager) ((MuzimaApplication) context).getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
                            ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
                            Intent intent = new Intent();
                            intent.setComponent(cn);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            }
                            ((MuzimaApplication) context).getApplicationContext().startActivity(intent);
                        }
                    }
                    new OnlineOnlyModePreferenceService(((MuzimaApplication) context)).updateOnlineOnlyModePreferenceValue();
                } else if(muzimaSetting.getProperty().equals(DEFAULT_LOGGED_IN_USER_AS_ENCOUNTER_PROVIDER_SETTING)){
                    boolean isDefaultLoggedInUserAsEncounterProvider = muzimaSettingController.isDefaultLoggedInUserAsEncounterProvider();

                    Resources resources = context.getResources();
                    String key = resources.getString(R.string.preference_encounter_provider_key);
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
                    settings.edit()
                            .putBoolean(key, isDefaultLoggedInUserAsEncounterProvider)
                            .apply();
                } else if(muzimaSetting.getProperty().equals(DEFAULT_ENCOUNTER_LOCATION_SETTING)){
                    MuzimaSetting encounterLocationIdSetting = null;
                    try {
                        encounterLocationIdSetting = muzimaSettingController.getSettingByProperty(DEFAULT_ENCOUNTER_LOCATION_SETTING);
                        if(encounterLocationIdSetting != null) {
                            Location defaultEncounterLocation = ((MuzimaApplication) context).getLocationController().getLocationById(Integer.valueOf(encounterLocationIdSetting.getValueString()));
                            if(defaultEncounterLocation != null){
                                Context context = getApplicationContext();
                                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                                Resources resources = context.getResources();
                                String key = resources.getString(R.string.preference_default_encounter_location);

                                preferences.edit()
                                        .putString(key, String.valueOf(defaultEncounterLocation.getId()))
                                        .apply();
                            }
                        }
                    } catch (MuzimaSettingController.MuzimaSettingFetchException e) {
                        Log.e(getClass().getSimpleName(), "Encountered Exception while fetching setting ", e);
                    } catch (LocationController.LocationLoadException e) {
                        Log.e(getClass().getSimpleName(), "Encountered Exception while fetching location ", e);
                    }
                }else if(muzimaSetting.getProperty().equals(NOTIFICATION_FEATURE_ENABLED_SETTING)){
                    try {
                        fcmTokenController.sendTokenToServer();
                    } catch (IOException e) {
                        Log.e(getClass().getSimpleName(), "Encountered Exception while sending token to server ", e);
                    }
                }
            }

            /*check if the 6 mobile settings preferences are in setup else default to global, might have been deleted from the config*/
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
            } else if (settingProperty.equals(ONLINE_ONLY_MODE_ENABLED_SETTING)) {
                new OnlineOnlyModePreferenceService(((MuzimaApplication) context)).updateOnlineOnlyModePreferenceValue();
            }else if(settingProperty.equals(DEFAULT_LOGGED_IN_USER_AS_ENCOUNTER_PROVIDER_SETTING)){
                boolean isDefaultLoggedInUserAsEncounterProvider = muzimaSettingController.isDefaultLoggedInUserAsEncounterProvider();

                Resources resources = context.getResources();
                String key = resources.getString(R.string.preference_encounter_provider_key);
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
                settings.edit()
                        .putBoolean(key, isDefaultLoggedInUserAsEncounterProvider)
                        .apply();
            } else if(settingProperty.equals(DEFAULT_ENCOUNTER_LOCATION_SETTING)){
                MuzimaSetting encounterLocationIdSetting = null;
                try {
                    encounterLocationIdSetting = muzimaSettingController.getSettingByProperty(DEFAULT_ENCOUNTER_LOCATION_SETTING);
                    if(encounterLocationIdSetting != null) {
                        Location defaultEncounterLocation = ((MuzimaApplication) context).getLocationController().getLocationById(Integer.valueOf(encounterLocationIdSetting.getValueString()));
                        if(defaultEncounterLocation != null){
                            Context context = getApplicationContext();
                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                            Resources resources = context.getResources();
                            String key = resources.getString(R.string.preference_default_encounter_location);

                            preferences.edit()
                                    .putString(key, String.valueOf(defaultEncounterLocation.getId()))
                                    .apply();
                        }
                    }
                } catch (MuzimaSettingController.MuzimaSettingFetchException e) {
                    Log.e(getClass().getSimpleName(), "Encountered Exception while fetching setting ", e);
                } catch (LocationController.LocationLoadException e) {
                    Log.e(getClass().getSimpleName(), "Encountered Exception while fetching location ", e);
                }
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
                List<Integer> datasetIds = new ArrayList<>();
                List<Integer> datasetIdsBeforeConfigUpdate = new ArrayList<>();

                SetupConfigurationTemplate activeSetupConfig = setupConfigurationController.getActiveSetupConfigurationTemplate();
                String configJson = activeSetupConfig.getConfigJson();
                List<Object> datasets = JsonUtils.readAsObjectList(configJson, "$['config']['datasets']");
                for (Object dataset : datasets) {
                    net.minidev.json.JSONObject dataset1 = (net.minidev.json.JSONObject) dataset;
                    Integer datasetId = (Integer)dataset1.get("id");
                    datasetIds.add(datasetId);
                }

                String configJsonBeforeConfigUpdate = configBeforeConfigUpdate.getConfigJson();
                List<Object> datasetsBeforeConfigUpdate = JsonUtils.readAsObjectList(configJsonBeforeConfigUpdate, "$['config']['datasets']");
                for (Object dataset : datasetsBeforeConfigUpdate) {
                    net.minidev.json.JSONObject dataset1 = (net.minidev.json.JSONObject) dataset;
                    Integer datasetId = (Integer)dataset1.get("id");
                    datasetIdsBeforeConfigUpdate.add(datasetId);
                }

                List<Integer> datasetToDeleteIds = new ArrayList<>();
                List<Integer> datasetToDownload= new ArrayList<>();

                //Get datasets previously downloaded but not in the updated config
                for(Integer datasetId: datasetIdsBeforeConfigUpdate){
                    if(!datasetIds.contains(datasetId)){
                        datasetToDeleteIds.add(datasetId);
                    }
                }

                //sync the downloaded datasets with changes
                if(datasetIdsBeforeConfigUpdate.size() > 0){
                    List<ReportDataset> reportDatasetList = reportDatasetController.downloadReportDatasets(datasetIdsBeforeConfigUpdate, true);
                    reportDatasetController.saveReportDatasets(reportDatasetList);
                }

                //Get Added datasets to updated config
                for(Integer datasetId : datasetIds){
                    if(!datasetIdsBeforeConfigUpdate.contains(datasetId)){
                        datasetToDownload.add(datasetId);
                    }
                }

                if(datasetToDeleteIds.size() > 0) {
                    reportDatasetController.deleteReportDatasets(datasetToDeleteIds);
                }

                if(datasetToDownload.size()>0){
                    //Download Added datasets
                    List<ReportDataset> reportDatasetList = reportDatasetController.downloadReportDatasets(datasetToDownload, false);
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
                List<String> formUuids = new ArrayList<>();
                List<String> formUuidsBeforeConfigUpdate = new ArrayList<>();

                SetupConfigurationTemplate activeSetupConfig = setupConfigurationController.getActiveSetupConfigurationTemplate();
                String configJson = activeSetupConfig.getConfigJson();
                List<Object> forms = JsonUtils.readAsObjectList(configJson, "$['config']['forms']");
                for (Object form : forms) {
                    net.minidev.json.JSONObject form1 = (net.minidev.json.JSONObject) form;
                    String formUuid = form1.get("uuid").toString();
                    formUuids.add(formUuid);
                }

                String configJsonBeforeConfigUpdate = configBeforeConfigUpdate.getConfigJson();
                List<Object> formsBeforeConfigUpdate = JsonUtils.readAsObjectList(configJsonBeforeConfigUpdate, "$['config']['forms']");
                for (Object form : formsBeforeConfigUpdate) {
                    net.minidev.json.JSONObject form1 = (net.minidev.json.JSONObject) form;
                    String formUuid = form1.get("uuid").toString();
                    formUuidsBeforeConfigUpdate.add(formUuid);
                }

                List<String> formTemplatesToDeleteUuids = new ArrayList<>();
                List<String>  formTemplateToDownload= new ArrayList<>();

                //Get forms previously downloaded but not in the updated config
                for(String formUuid: formUuidsBeforeConfigUpdate){
                    if(!formUuids.contains(formUuid)){
                        formTemplatesToDeleteUuids.add(formUuid);
                    }
                }

                //Get Added forms to updated config
                for(String formUuid : formUuids){
                    if(!formUuidsBeforeConfigUpdate.contains(formUuid)){
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
                List<String> cohortUuids = new ArrayList<>();
                List<String> cohortUuidsBeforeUpdate = new ArrayList<>();

                SetupConfigurationTemplate activeSetupConfig = setupConfigurationController.getActiveSetupConfigurationTemplate();
                String configJson = activeSetupConfig.getConfigJson();
                List<Object> cohorts = JsonUtils.readAsObjectList(configJson, "$['config']['cohorts']");
                for (Object cohort : cohorts) {
                    net.minidev.json.JSONObject cohort1 = (net.minidev.json.JSONObject) cohort;
                    String cohortUuid = cohort1.get("uuid").toString();
                    cohortUuids.add(cohortUuid);
                }


                String configJsonBeforeConfigUpdate = configBeforeConfigUpdate.getConfigJson();
                List<Object> cohortsBeforeConfigUpdate = JsonUtils.readAsObjectList(configJsonBeforeConfigUpdate, "$['config']['cohorts']");
                for (Object cohort : cohortsBeforeConfigUpdate) {
                    net.minidev.json.JSONObject cohort1 = (net.minidev.json.JSONObject) cohort;
                    String cohortUuid = cohort1.get("uuid").toString();
                    cohortUuidsBeforeUpdate.add(cohortUuid);
                }

                List<String> cohortsToSetAsUnsyncedUuids = new ArrayList<>();
                List<String> cohortsToDownload= new ArrayList<>();

                //Get cohorts previously in config but not in the updated config
                for(String cohortUuid: cohortUuidsBeforeUpdate){
                    if(!cohortUuids.contains(cohortUuid)){
                        cohortsToSetAsUnsyncedUuids.add(cohortUuid);
                    }
                }

                //Get Added cohorts to updated config
                for(String cohortUuid : cohortUuids){
                    if(!cohortUuidsBeforeUpdate.contains(cohortUuid)){
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
                List<String> locationUuids = new ArrayList<>();
                List<String> locationUuidsBeforeConfigUpdate = new ArrayList<>();

                SetupConfigurationTemplate activeSetupConfig = setupConfigurationController.getActiveSetupConfigurationTemplate();
                String configJson = activeSetupConfig.getConfigJson();
                List<Object> locations = JsonUtils.readAsObjectList(configJson, "$['config']['locations']");
                for (Object location : locations) {
                    net.minidev.json.JSONObject location1 = (net.minidev.json.JSONObject) location;
                    String locationUuid = location1.get("uuid").toString();
                    locationUuids.add(locationUuid);
                }

                String configJsonBeforeConfigUpdate = configBeforeConfigUpdate.getConfigJson();
                List<Object> locationsBeforeConfigUpdate = JsonUtils.readAsObjectList(configJsonBeforeConfigUpdate, "$['config']['locations']");
                for (Object location : locationsBeforeConfigUpdate) {
                    net.minidev.json.JSONObject location1 = (net.minidev.json.JSONObject) location;
                    String locationUuid = location1.get("uuid").toString();
                    locationUuidsBeforeConfigUpdate.add(locationUuid);
                }

                List<String> locationsToBeDeleted = new ArrayList<>();
                List<String> locationsToDownload= new ArrayList<>();

                //Get locations previously in config  but not in the updated config
                for(String locationUuid: locationUuidsBeforeConfigUpdate){
                    if(!locationUuids.contains(locationUuid)){
                        locationsToBeDeleted.add(locationUuid);
                    }
                }

                //Get Added locations to updated config
                for(String locationUuid : locationUuids){
                    if(!locationUuidsBeforeConfigUpdate.contains(locationUuid)){
                        locationsToDownload.add(locationUuid);
                    }
                }

                if(locationsToBeDeleted.size()>0) {
                    locationController.deleteLocationsByUuids(locationsToBeDeleted);
                }

                if(locationsToDownload.size()>0) {
                    List<Location> locationList = locationController.downloadLocationsFromServerByUuid(locationsToDownload.stream().toArray(String[]::new));
                    locationController.saveLocations(locationList);
                }

            } catch (SetupConfigurationController.SetupConfigurationFetchException e){
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not get the active config ",e);
            }  catch (LocationController.LocationDeleteException e) {
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not delete locations ",e);
            } catch (LocationController.LocationDownloadException e) {
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not download locations ",e);
            } catch (LocationController.LocationSaveException e) {
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not save locations ",e);
            } catch (IOException e) {
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not get locations ",e);
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
                List<String> providerUuids = new ArrayList<>();
                List<String> providerUuidsBeforeConfigUpdate = new ArrayList<>();

                SetupConfigurationTemplate activeSetupConfig = setupConfigurationController.getActiveSetupConfigurationTemplate();
                String configJson = activeSetupConfig.getConfigJson();
                List<Object> providers = JsonUtils.readAsObjectList(configJson, "$['config']['providers']");
                for (Object provider : providers) {
                    net.minidev.json.JSONObject provider1 = (net.minidev.json.JSONObject) provider;
                    String providerUuid = provider1.get("uuid").toString();
                    providerUuids.add(providerUuid);
                }

                String configJsonBeforeConfigUpdate = configBeforeConfigUpdate.getConfigJson();
                List<Object> providersBeforeConfigUpdate = JsonUtils.readAsObjectList(configJsonBeforeConfigUpdate, "$['config']['providers']");
                for (Object provider : providersBeforeConfigUpdate) {
                    net.minidev.json.JSONObject provider1 = (net.minidev.json.JSONObject) provider;
                    String providerUuid = provider1.get("uuid").toString();
                    providerUuidsBeforeConfigUpdate.add(providerUuid);
                }

                List<String> providersToBeDeleted = new ArrayList<>();
                List<String> providersToDownload= new ArrayList<>();

                //Get providers previously downloaded but not in the updated config
                for(String providerUuid: providerUuidsBeforeConfigUpdate){
                    if(!providerUuids.contains(providerUuid)){
                        providersToBeDeleted.add(providerUuid);
                    }
                }

                //Get Added providers to updated config
                for(String providerUuid : providerUuids){
                    if(!providerUuidsBeforeConfigUpdate.contains(providerUuid)){
                        providersToDownload.add(providerUuid);
                    }
                }

                if(providersToBeDeleted.size()>0) {
                    providerController.deleteProvidersByUuids(providersToBeDeleted);
                }

                if(providersToDownload.size()>0) {
                    List<Provider> providerList = providerController.downloadProvidersFromServerByUuid(providersToDownload.stream().toArray(String[]::new));
                    providerController.saveProviders(providerList);
                }

            } catch (SetupConfigurationController.SetupConfigurationFetchException e){
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not get the active config ",e);
            } catch (ProviderController.ProviderDeleteException e) {
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not delete providers ",e);
            } catch (ProviderController.ProviderDownloadException e) {
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not download providers ",e);
            } catch (ProviderController.ProviderSaveException e) {
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not save providers ",e);
            } catch (IOException e) {
                Log.e(MuzimaJobScheduler.class.getSimpleName(),"Could not load providers ",e);
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
                List<String> conceptUuids = new ArrayList<>();
                List<String> conceptUuidsBeforeConfigUpdate = new ArrayList<>();

                SetupConfigurationTemplate activeSetupConfig = setupConfigurationController.getActiveSetupConfigurationTemplate();
                String configJson = activeSetupConfig.getConfigJson();
                List<Object> concepts = JsonUtils.readAsObjectList(configJson, "$['config']['concepts']");
                for (Object concept : concepts) {
                    net.minidev.json.JSONObject concept1 = (net.minidev.json.JSONObject) concept;
                    String conceptUuid = concept1.get("uuid").toString();
                    conceptUuids.add(conceptUuid);
                }

                String configJsonBeforeConfigUpdate = configBeforeConfigUpdate.getConfigJson();
                List<Object> conceptsBeforeConfigUpdate = JsonUtils.readAsObjectList(configJsonBeforeConfigUpdate, "$['config']['concepts']");
                for (Object concept : conceptsBeforeConfigUpdate) {
                    net.minidev.json.JSONObject concept1 = (net.minidev.json.JSONObject) concept;
                    String conceptUuid = concept1.get("uuid").toString();
                    conceptUuidsBeforeConfigUpdate.add(conceptUuid);
                }

                List<Concept> conceptsToBeDeleted = new ArrayList<>();
                List<String> conceptsToDownload= new ArrayList<>();

                //Get concepts previously downloaded but not in the updated config
                for(String conceptUuid: conceptUuidsBeforeConfigUpdate){
                    if(!conceptUuids.contains(conceptUuid)){
                        Concept concept = conceptController.getConceptByUuid(conceptUuid);
                        if(concept != null){
                            conceptsToBeDeleted.add(concept);
                        }

                    }
                }

                //Get Added concepts to updated config
                for(String conceptUuid : conceptUuids){
                    if(!conceptUuidsBeforeConfigUpdate.contains(conceptUuid)){
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

    private class SyncAppUsageLogsBackgroundTask extends AsyncTask<Void,Void,Void>{

        @Override
        protected Void doInBackground(Void... voids) {
            Context context = getApplicationContext();
            AppUsageLogsController appUsageLogsController = ((MuzimaApplication) context).getAppUsageLogsController();

            try {
                List<AppUsageLogs> appUsageLogs = appUsageLogsController.getAllAppUsageLogs();
                appUsageLogsController.syncAppUsageLogs(appUsageLogs);
            } catch (IOException e) {
                Log.e(getClass().getSimpleName(),"Encounter an IO exception",e);
            }

            return null;
        }
    }

    private class MediaCategorySyncBackgroundTask extends AsyncTask<Void,Void,Void>{

        @Override
        protected Void doInBackground(Void... input) {
            Context context = getApplicationContext();
            MediaCategoryController mediaCategoryController = ((MuzimaApplication) context).getMediaCategoryController();
            try {
                //Get media Categories in the config
                List<String> mediaCategoryUuids = new ArrayList<>();
                List<String> mediaCategoryUuidsBeforeConfigUpdate = new ArrayList<>();

                SetupConfigurationTemplate activeSetupConfig = setupConfigurationController.getActiveSetupConfigurationTemplate();
                String configJson = activeSetupConfig.getConfigJson();
                List<Object> mediaCategoryList = JsonUtils.readAsObjectList(configJson, "$['config']['mediaCategories']");
                for (Object mediaCategory : mediaCategoryList) {
                    net.minidev.json.JSONObject mediaCategory1 = (net.minidev.json.JSONObject) mediaCategory;
                    String mediaCategoryUuid = mediaCategory1.get("uuid").toString();
                    mediaCategoryUuids.add(mediaCategoryUuid);
                }

                String configJsonBeforeConfigUpdate = configBeforeConfigUpdate.getConfigJson();
                List<Object> mediaCategoryBeforeConfigUpdate = JsonUtils.readAsObjectList(configJsonBeforeConfigUpdate, "$['config']['mediaCategories']");
                for (Object mediaCategory : mediaCategoryBeforeConfigUpdate) {
                    net.minidev.json.JSONObject mediaCategory1 = (net.minidev.json.JSONObject) mediaCategory;
                    String mediaCategoryUuid = mediaCategory1.get("uuid").toString();
                    mediaCategoryUuidsBeforeConfigUpdate.add(mediaCategoryUuid);
                }

                List<String> mediaCategoryToBeDeleted = new ArrayList<>();
                List<String> mediaCategoryToDownload= new ArrayList<>();
                List<String> mediaCategoryToCheckForUpdates = new ArrayList<>();

                //Get mediaCategory previously downloaded but not in the updated config
                for(String mediaCategoryUuid: mediaCategoryUuidsBeforeConfigUpdate){
                    if(!mediaCategoryUuids.contains(mediaCategoryUuid)){
                        mediaCategoryToBeDeleted.add(mediaCategoryUuid);
                    }else{
                        mediaCategoryToCheckForUpdates.add(mediaCategoryUuid);
                    }
                }

                //Get Added mediaCategory to updated config
                for(String mediaCategoryUuid : mediaCategoryUuids){
                    if(!mediaCategoryUuidsBeforeConfigUpdate.contains(mediaCategoryUuid)){
                        mediaCategoryToDownload.add(mediaCategoryUuid);
                    }
                }

                if(mediaCategoryToBeDeleted.size()>0) {
                    mediaCategoryController.deleteMediaCategory(mediaCategoryToBeDeleted);
                }

                if(mediaCategoryToCheckForUpdates.size()>0){
                    List<MediaCategory> mediaCategoryListToUpdate = mediaCategoryController.downloadMediaCategory(mediaCategoryToCheckForUpdates, true);
                    mediaCategoryController.updateMediaCategory(mediaCategoryListToUpdate);
                }

                if(mediaCategoryToDownload.size()>0) {
                    List<MediaCategory> downloadedMediaList = mediaCategoryController.downloadMediaCategory(mediaCategoryToDownload, false);
                    mediaCategoryController.saveMediaCategory(downloadedMediaList);
                }
            } catch (MediaCategoryController.MediaCategoryDownloadException e) {
                Log.e(getClass().getSimpleName(), "Encountered an error while downloading media categories");
            } catch (MediaCategoryController.MediaCategorySaveException e) {
                Log.e(getClass().getSimpleName(), "Encountered an error while saving media categories");
            } catch (SetupConfigurationController.SetupConfigurationFetchException e) {
                Log.e(getClass().getSimpleName(), "Encountered an error while getting config");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            new DownloadAndDeleteMediaBasedOnConfigChangesBackgroundTask().execute();
        }
    }

    private class DownloadAndDeleteMediaBasedOnConfigChangesBackgroundTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                //update memory space app usage log
                AppUsageLogs appUsageLogs = appUsageLogsController.getAppUsageLogByKey(com.muzima.util.Constants.AppUsageLogs.AVAILABLE_INTERNAL_SPACE);
                String availableMemory = MemoryUtil.getFormattedMemory(MemoryUtil.getAvailableInternalMemorySize());

                if(appUsageLogs != null) {
                    if(!availableMemory.equals(appUsageLogs.getLogvalue())) {
                        appUsageLogs.setLogvalue(availableMemory);
                        appUsageLogs.setUpdateDatetime(new Date());
                        appUsageLogs.setUserName(username);
                        appUsageLogs.setDeviceId(pseudoDeviceId);
                        appUsageLogs.setLogSynced(false);
                        appUsageLogsController.saveOrUpdateAppUsageLog(appUsageLogs);
                    }
                }else{
                    AppUsageLogs availableSpace = new AppUsageLogs();
                    availableSpace.setUuid(UUID.randomUUID().toString());
                    availableSpace.setLogKey(com.muzima.util.Constants.AppUsageLogs.AVAILABLE_INTERNAL_SPACE);
                    availableSpace.setLogvalue(availableMemory);
                    availableSpace.setUpdateDatetime(new Date());
                    availableSpace.setUserName(username);
                    availableSpace.setDeviceId(pseudoDeviceId);
                    availableSpace.setLogSynced(false);
                    appUsageLogsController.saveOrUpdateAppUsageLog(availableSpace);
                }

                MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
                MediaCategoryController mediaCategoryController = ((MuzimaApplication) getApplicationContext()).getMediaCategoryController();
                List<MediaCategory> mediaCategoryList= mediaCategoryController.getMediaCategories();
                List<String> mediaCategoryUuids = new ArrayList<>();
                if(mediaCategoryList.size()>0) {
                    for (MediaCategory mediaCategory : mediaCategoryList) {
                        mediaCategoryUuids.add(mediaCategory.getUuid());
                    }
                    List<Media> mediaList = muzimaSyncService.downloadMedia(mediaCategoryUuids, true);
                    long totalFileSize = MemoryUtil.getTotalMediaFileSize(mediaList);
                    long availableSpace = MemoryUtil.getAvailableInternalMemorySize();
                    if(availableSpace>totalFileSize) {
                        muzimaSyncService.saveMedia(mediaList);
                        for (Media media : mediaList) {
                            downloadFile(media);
                        }
                    }else{
                        AppUsageLogs noEnoughSpaceLog = appUsageLogsController.getAppUsageLogByKey(com.muzima.util.Constants.AppUsageLogs.NO_ENOUGH_SPACE_DEVICES);
                        String requiredMemory = MemoryUtil.getFormattedMemory(MemoryUtil.getAvailableInternalMemorySize());
                        if(noEnoughSpaceLog != null) {
                            noEnoughSpaceLog.setLogvalue("Required: "+requiredMemory+ " Available: "+availableMemory);
                            noEnoughSpaceLog.setUpdateDatetime(new Date());
                            noEnoughSpaceLog.setUserName(username);
                            noEnoughSpaceLog.setDeviceId(pseudoDeviceId);
                            noEnoughSpaceLog.setLogSynced(false);
                            appUsageLogsController.saveOrUpdateAppUsageLog(noEnoughSpaceLog);
                        }else{
                            AppUsageLogs newNoEnoughSpaceLog = new AppUsageLogs();
                            newNoEnoughSpaceLog.setUuid(UUID.randomUUID().toString());
                            newNoEnoughSpaceLog.setLogKey(com.muzima.util.Constants.AppUsageLogs.NO_ENOUGH_SPACE_DEVICES);
                            newNoEnoughSpaceLog.setLogvalue("Required: "+requiredMemory+ " Available: "+availableMemory);
                            newNoEnoughSpaceLog.setUpdateDatetime(new Date());
                            newNoEnoughSpaceLog.setUserName(username);
                            newNoEnoughSpaceLog.setDeviceId(pseudoDeviceId);
                            newNoEnoughSpaceLog.setLogSynced(false);
                            appUsageLogsController.saveOrUpdateAppUsageLog(newNoEnoughSpaceLog);
                        }
                        MemoryUtil.showAlertDialog(availableSpace,totalFileSize, getApplication().getApplicationContext());
                    }
                }
            }  catch (MediaCategoryController.MediaCategoryFetchException e) {
                Log.e(getClass().getSimpleName(), "Encountered an error while saving media");
            } catch (IOException e) {
                Log.e(getClass().getSimpleName(),"Encountered IOException ",e);
            } catch (ParseException e) {
                Log.e(getClass().getSimpleName(),"Encountered ParseException ",e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            new SyncAppUsageLogsBackgroundTask().execute();
        }

        public void downloadFile(Media media){
            try {
                //Delete file if exists
                String mimeType = media.getMimeType();
                String PATH = Objects.requireNonNull(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)).getAbsolutePath();
                File file = new File(PATH + "/"+media.getName()+"."+mimeType.substring(mimeType.lastIndexOf("/") + 1));
                if(file.exists())
                    file.delete();

                if(!media.isRetired()) {
                    //Enqueue the file for download
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(media.getUrl() + ""));
                    request.setTitle(media.getName());
                    request.setDescription(media.getDescription());
                    request.allowScanningByMediaScanner();
                    request.setAllowedOverMetered(true);
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, media.getName()+"."+mimeType.substring(mimeType.lastIndexOf("/") + 1));
                    DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    dm.enqueue(request);
                }
            } catch (Exception e) {
                Log.e(getClass().getSimpleName(), "Error ", e);
            }
        }
    }
}
