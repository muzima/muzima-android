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

import static com.muzima.util.Constants.ServerSettings.AUTOMATIC_FORM_SYNC_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.CONFIDENTIALITY_NOTICE_DISPLAY_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.DEFAULT_ENCOUNTER_LOCATION_SETTING;
import static com.muzima.util.Constants.ServerSettings.DEFAULT_LOGGED_IN_USER_AS_ENCOUNTER_PROVIDER_SETTING;
import static com.muzima.util.Constants.ServerSettings.FORM_DUPLICATE_CHECK_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.GPS_FEATURE_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.NOTIFICATION_FEATURE_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.ONLINE_ONLY_MODE_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.PATIENT_IDENTIFIER_AUTOGENERATTION_SETTING;
import static com.muzima.util.Constants.ServerSettings.SHR_FEATURE_ENABLED_SETTING;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS;
import static com.muzima.utils.Constants.STANDARD_DATE_TIMEZONE_FORMAT;
import static com.muzima.utils.DeviceDetailsUtil.generatePseudoDeviceId;
import static com.muzima.view.BroadcastListenerActivity.SYNC_COMPLETED_ACTION;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.AppUsageLogs;
import com.muzima.api.model.Location;
import com.muzima.api.model.MuzimaSetting;
import com.muzima.api.model.Person;
import com.muzima.api.model.SetupConfigurationTemplate;
import com.muzima.api.model.User;
import com.muzima.controller.AppUsageLogsController;
import com.muzima.controller.FCMTokenController;
import com.muzima.controller.FormController;
import com.muzima.controller.LocationController;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.controller.SetupConfigurationController;
import com.muzima.service.ConfidentialityNoticeDisplayPreferenceService;
import com.muzima.service.FormDuplicateCheckPreferenceService;
import com.muzima.service.MuzimaSyncService;
import com.muzima.service.OnlineOnlyModePreferenceService;
import com.muzima.service.WizardFinishPreferenceService;
import com.muzima.util.JsonUtils;
import com.muzima.util.MuzimaSettingUtils;
import com.muzima.utils.Constants;
import com.muzima.utils.DownloadAndDeleteCohortsBasedOnConfigChangesIntent;
import com.muzima.utils.DownloadAndDeleteConceptAndObservationBasedOnConfigChangesIntent;
import com.muzima.utils.DownloadAndDeleteDerivedConceptAndObservationBasedOnConfigChangesIntent;
import com.muzima.utils.DownloadAndDeleteLocationBasedOnConfigChangesIntent;
import com.muzima.utils.DownloadAndDeleteProvidersBasedOnConfigChangesIntent;
import com.muzima.utils.MuzimaPreferences;
import com.muzima.utils.ProcessedTemporaryFormDataCleanUpIntent;
import com.muzima.utils.StringUtils;
import com.muzima.utils.SyncCohortsAndPatientFullDataIntent;
import com.muzima.utils.SyncDatasetsIntent;
import com.muzima.utils.SyncMediaCategoryIntent;
import com.muzima.utils.SyncMediaIntent;
import com.muzima.utils.SyncSettingsIntent;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.MainDashboardActivity;
import com.muzima.view.forms.SyncFormIntent;
import com.muzima.view.reports.SyncAllPatientReports;

import org.apache.lucene.queryParser.ParseException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

    public static final String MESSAGE_SENT_ACTION = "com.muzima.MESSAGE_RECEIVED_ACTION";

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MuzimaJobScheduler.this.onReceive(intent);
        }
    };

    protected void onReceive(Intent intent){
        if(intent.getAction().equals(SYNC_COMPLETED_ACTION)){
            new SyncAppUsageLogsBackgroundTask().execute();
            LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        } else {
            displayToast(intent);
        }
    }

    @Override
    public boolean onStartJob(final JobParameters params) {
        if (authenticatedUser == null || !isAuthPerson) {
            onStopJob(params);
        } else {
            //execute job
            Log.i(getClass().getSimpleName(), "Service Started ===");
            LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(MESSAGE_SENT_ACTION));
            LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(SYNC_COMPLETED_ACTION));

            Intent syncStartedBroadcastIntent = new Intent();
            syncStartedBroadcastIntent.setAction(BroadcastListenerActivity.SYNC_STARTED_ACTION);
            LocalBroadcastManager.getInstance(this).sendBroadcast(syncStartedBroadcastIntent);

            handleBackgroundWork(params);
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.i(getClass().getSimpleName(), "mUzima Job Service stopped ==== " + params.getJobId());
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(getClass().getSimpleName(), "Service destroyed ====");
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
            new FormDataUploadBackgroundTask().execute();
            new SyncSetupConfigTemplatesBackgroundTask().execute();
            new CohortsAndPatientFullDataSyncBackgroundTask().execute();
            new ProcessedTemporaryFormDataCleanUpBackgroundTask().execute();
            new SyncSettingsBackgroundTask().execute();
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

    private class SyncSettingsBackgroundTask extends AsyncTask<Void,Void,Void> {
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
            new DownloadAndDeleteDerivedConceptsBasedOnConfigChangesBackgroundTask().execute();
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
            preferenceSettings.add(FORM_DUPLICATE_CHECK_ENABLED_SETTING);
            preferenceSettings.add(AUTOMATIC_FORM_SYNC_ENABLED_SETTING);

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
               if (MuzimaSettingUtils.isOnlineOnlyModeSetting(muzimaSetting)) {
                    if(onlineModeBeforeConfigUpdate != muzimaSetting.getValueBoolean()){
                        muzimaSettingController.toggleTheme();
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
                    MuzimaPreferences.setBooleanPreference(context, key, isDefaultLoggedInUserAsEncounterProvider);
                } else if(muzimaSetting.getProperty().equals(DEFAULT_ENCOUNTER_LOCATION_SETTING)){
                    MuzimaSetting encounterLocationIdSetting = null;
                    try {
                        encounterLocationIdSetting = muzimaSettingController.getSettingByProperty(DEFAULT_ENCOUNTER_LOCATION_SETTING);
                        if(encounterLocationIdSetting != null) {
                            Location defaultEncounterLocation = ((MuzimaApplication) context).getLocationController().getLocationById(Integer.valueOf(encounterLocationIdSetting.getValueString()));
                            if(defaultEncounterLocation != null){
                                Context context = getApplicationContext();
                                Resources resources = context.getResources();
                                String key = resources.getString(R.string.preference_default_encounter_location);

                                MuzimaPreferences.setStringPreference(getApplicationContext(), key, String.valueOf(defaultEncounterLocation.getId()));
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
                }else if(muzimaSetting.getProperty().equals(FORM_DUPLICATE_CHECK_ENABLED_SETTING)){
                    new FormDuplicateCheckPreferenceService(((MuzimaApplication) context)).updateFormDuplicateCheckPreferenceSettings();
                }else if(muzimaSetting.getProperty().equals(CONFIDENTIALITY_NOTICE_DISPLAY_ENABLED_SETTING)){
                    new ConfidentialityNoticeDisplayPreferenceService(((MuzimaApplication) context)).updateConfidentialityNoticeDisplayPreferenceValue();
                }
            }

            /*check if the mobile settings preferences are in setup else default to global, might have been deleted from the config*/
            for(String settingProperty : preferenceSettings){
                if(!configSettings.contains(settingProperty)){
                    defaultToGlobalSettings(settingProperty);
                }
            }
        }

        public void defaultToGlobalSettings(String settingProperty){
            if (settingProperty.equals(ONLINE_ONLY_MODE_ENABLED_SETTING)) {
                new OnlineOnlyModePreferenceService(((MuzimaApplication) context)).updateOnlineOnlyModePreferenceValue();
            }else if(settingProperty.equals(DEFAULT_LOGGED_IN_USER_AS_ENCOUNTER_PROVIDER_SETTING)){
                boolean isDefaultLoggedInUserAsEncounterProvider = muzimaSettingController.isDefaultLoggedInUserAsEncounterProvider();

                Resources resources = context.getResources();
                String key = resources.getString(R.string.preference_encounter_provider_key);
                MuzimaPreferences.setBooleanPreference(context, key, isDefaultLoggedInUserAsEncounterProvider);
            } else if(settingProperty.equals(DEFAULT_ENCOUNTER_LOCATION_SETTING)){
                MuzimaSetting encounterLocationIdSetting = null;
                try {
                    encounterLocationIdSetting = muzimaSettingController.getSettingByProperty(DEFAULT_ENCOUNTER_LOCATION_SETTING);
                    if(encounterLocationIdSetting != null && !StringUtils.isEmpty(encounterLocationIdSetting.getValueString())) {
                        Location defaultEncounterLocation = ((MuzimaApplication) context).getLocationController().getLocationById(Integer.valueOf(encounterLocationIdSetting.getValueString()));
                        if(defaultEncounterLocation != null){
                            Resources resources = context.getResources();
                            String key = resources.getString(R.string.preference_default_encounter_location);
                            MuzimaPreferences.setStringPreference(context, key, String.valueOf(defaultEncounterLocation.getId()));
                        }
                    }
                } catch (MuzimaSettingController.MuzimaSettingFetchException e) {
                    Log.e(getClass().getSimpleName(), "Encountered Exception while fetching setting ", e);
                } catch (LocationController.LocationLoadException e) {
                    Log.e(getClass().getSimpleName(), "Encountered Exception while fetching location ", e);
                }
            }else if(settingProperty.equals(NOTIFICATION_FEATURE_ENABLED_SETTING)){
                try {
                    fcmTokenController.sendTokenToServer();
                } catch (IOException e) {
                    Log.e(getClass().getSimpleName(), "Encountered Exception while sending token to server ", e);
                }
            }else if(settingProperty.equals(FORM_DUPLICATE_CHECK_ENABLED_SETTING)){
                new FormDuplicateCheckPreferenceService(((MuzimaApplication) context)).updateFormDuplicateCheckPreferenceSettings();
            }else if(settingProperty.equals(CONFIDENTIALITY_NOTICE_DISPLAY_ENABLED_SETTING)){
                new ConfidentialityNoticeDisplayPreferenceService(((MuzimaApplication) context)).updateConfidentialityNoticeDisplayPreferenceValue();
            }
        }
    }

    private class SyncReportDatasetsBackgroundTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            new SyncDatasetsIntent(getApplicationContext(), configBeforeConfigUpdate).start();
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
            MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
            muzimaSyncService.SyncFormTemplates(configBeforeConfigUpdate);
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
            new DownloadAndDeleteCohortsBasedOnConfigChangesIntent(getApplicationContext(), configBeforeConfigUpdate).start();
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
            new DownloadAndDeleteLocationBasedOnConfigChangesIntent(getApplicationContext(), configBeforeConfigUpdate).start();
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
            new DownloadAndDeleteProvidersBasedOnConfigChangesIntent(getApplicationContext(), configBeforeConfigUpdate).start();
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
            new DownloadAndDeleteConceptAndObservationBasedOnConfigChangesIntent(getApplicationContext(), configBeforeConfigUpdate).start();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private class  DownloadAndDeleteDerivedConceptsBasedOnConfigChangesBackgroundTask extends AsyncTask<Void,Void,Void>{
        @Override
        protected Void doInBackground(Void... voids) {
            new DownloadAndDeleteDerivedConceptAndObservationBasedOnConfigChangesIntent(getApplicationContext(), configBeforeConfigUpdate).start();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
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
            new SyncMediaCategoryIntent(getApplicationContext(), configBeforeConfigUpdate).start();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private class DownloadAndDeleteMediaBasedOnConfigChangesBackgroundTask extends AsyncTask<Void,Void,Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            new SyncMediaIntent(getApplicationContext()).start();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private void displayToast(Intent intent) {
        int syncStatus = intent.getIntExtra(Constants.DataSyncServiceConstants.SYNC_STATUS,
                Constants.DataSyncServiceConstants.SyncStatusConstants.UNKNOWN_ERROR);

        String msg = intent.getStringExtra(Constants.DataSyncServiceConstants.SYNC_RESULT_MESSAGE);

        switch (syncStatus) {
            case Constants.DataSyncServiceConstants.SyncStatusConstants.DOWNLOAD_ERROR:
                msg = getString(R.string.error_data_download);
                break;
            case Constants.DataSyncServiceConstants.SyncStatusConstants.AUTHENTICATION_ERROR:
                msg = getString(R.string.error_authentication_occur);
                break;
            case Constants.DataSyncServiceConstants.SyncStatusConstants.DELETE_ERROR:
                msg = getString(R.string.error_local_repo_data_delete);
                break;
            case Constants.DataSyncServiceConstants.SyncStatusConstants.SAVE_ERROR:
                msg = getString(R.string.error_data_save);
                break;
            case Constants.DataSyncServiceConstants.SyncStatusConstants.LOCAL_CONNECTION_ERROR:
                msg = getString(R.string.error_local_connection_unavailable);
                break;
            case Constants.DataSyncServiceConstants.SyncStatusConstants.SERVER_CONNECTION_ERROR:
                msg = getString(R.string.error_server_connection_unavailable);
                break;
            case Constants.DataSyncServiceConstants.SyncStatusConstants.PARSING_ERROR:
                msg = getString(R.string.error_parse_exception_data_fetch);
                break;
            case Constants.DataSyncServiceConstants.SyncStatusConstants.LOAD_ERROR:
                msg = getString(R.string.error_exception_data_load);
                break;
            case Constants.DataSyncServiceConstants.SyncStatusConstants.UPLOAD_ERROR:
                msg = getString(R.string.error_exception_data_upload);
                break;
            case Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS:
                int syncType = intent.getIntExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, -1);
                int downloadCount = intent.getIntExtra(Constants.DataSyncServiceConstants.DOWNLOAD_COUNT_PRIMARY, 0);

                switch (syncType) {
                    case Constants.DataSyncServiceConstants.SYNC_CONCEPTS_AND_OBS_BASED_ON_CHANGES_IN_CONFIG: {
                        int deleteCount = intent.getIntExtra(Constants.DataSyncServiceConstants.DELETED_COUNT_PRIMARY, 0);
                        msg = getString(R.string.info_concepts_downloaded_deleted, downloadCount, deleteCount);
                        break;
                    }
                    case Constants.DataSyncServiceConstants.SYNC_MEDIA_CATEGORIES: {
                        new DownloadAndDeleteMediaBasedOnConfigChangesBackgroundTask().execute();
                        break;
                    }
                }
                break;
        }
    }
}
