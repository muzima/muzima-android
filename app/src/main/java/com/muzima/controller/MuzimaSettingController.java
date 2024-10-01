/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.controller;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Cohort;
import com.muzima.api.model.LastSyncTime;
import com.muzima.api.model.MuzimaSetting;
import com.muzima.api.model.SetupConfigurationTemplate;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.api.service.MuzimaSettingService;

import com.muzima.api.service.SetupConfigurationService;
import com.muzima.service.SntpService;
import com.muzima.utils.MuzimaPreferences;
import com.muzima.utils.StringUtils;
import com.muzima.view.MainDashboardActivity;

import org.apache.lucene.queryParser.ParseException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.muzima.api.model.APIName.DOWNLOAD_SETTINGS;
import static com.muzima.util.Constants.ServerSettings.ALLOCATION_TAG_GENERATION;
import static com.muzima.util.Constants.ServerSettings.AUTOMATIC_FORM_SYNC_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.BARCODE_FEATURE_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.BOTTOM_NAVIGATION_COHORT_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.BOTTOM_NAVIGATION_FORM_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.CLINICAL_SUMMARY_FEATURE_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.COHORT_FILTER_CONCEPT_MAP;
import static com.muzima.util.Constants.ServerSettings.COHORT_FILTER_DERIVED_CONCEPT_MAP;
import static com.muzima.util.Constants.ServerSettings.CONFIDENTIALITY_NOTICE_DISPLAY_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.CONTACT_LISTING_UNDER_CLIENT_SUMMARY_SETTING;
import static com.muzima.util.Constants.ServerSettings.ClEAR_APP_DATA_IF_NEW_USER_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.DEFAULT_LOGGED_IN_USER_AS_ENCOUNTER_PROVIDER_SETTING;
import static com.muzima.util.Constants.ServerSettings.DISALLOW_SERVER_PATIENT_SEARCH;
import static com.muzima.util.Constants.ServerSettings.DISPLAY_ONLY_COHORTS_IN_CONFIG_SETTING;
import static com.muzima.util.Constants.ServerSettings.DISPLAY_ONLY_FORMS_IN_CONFIG_SETTING;
import static com.muzima.util.Constants.ServerSettings.FGH_CUSTOM_CLIENT_ADDRESS;
import static com.muzima.util.Constants.ServerSettings.FGH_CUSTOM_CLIENT_SUMMARY_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.FORM_DUPLICATE_CHECK_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.GEOMAPPING_FEATURE_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.GPS_FEATURE_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.HISTORICAL_DATA_TAB_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.MULTIPLE_CONFIGS_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.OBS_LISTING_UNDER_CLIENT_SUMMARY_SETTING;
import static com.muzima.util.Constants.ServerSettings.ONLINE_ONLY_MODE_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.PATIENT_IDENTIFIER_AUTOGENERATTION_SETTING;
import static com.muzima.util.Constants.ServerSettings.PATIENT_LIST_HIDE_WITH_TAGS_SETTING;
import static com.muzima.util.Constants.ServerSettings.PATIENT_REGISTRATION_BUTTON_ACTION_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.PATIENT_REGISTRATION_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.RELATIONSHIP_FEATURE_ENABLED;
import static com.muzima.util.Constants.ServerSettings.NOTIFICATION_FEATURE_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.SHR_FEATURE_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.DEMOGRAPHICS_UPDATE_MANUAL_REVIEW_REQUIRED_SETTING;
import static com.muzima.util.Constants.ServerSettings.SINGLE_ELEMENT_ENTRY_FEATURE_ENABLED_SETTING;

import static com.muzima.util.Constants.ServerSettings.TAG_GENERATION_ENABLED_SETTING;

import static com.muzima.util.Constants.ServerSettings.ADD_CONTACT_POPUP_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.FGH_CUSTOM_CONFIDANT_INFORMATION_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.FGH_CUSTOM_CLINICAL_INFORMATION_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.USERNAME_AUTO_POPULATION_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.PATIENT_FUZZY_SEARCH_ENABLED_SETTING;

public class MuzimaSettingController {
    private final MuzimaSettingService settingService;
    private final LastSyncTimeService lastSyncTimeService;
    private final SntpService sntpService;
    private final SetupConfigurationService setupConfigurationService;
    private final MuzimaApplication muzimaApplication;
    private final CohortController cohortController;


    public MuzimaSettingController(MuzimaSettingService settingService, LastSyncTimeService lastSyncTimeService,
                                   SntpService sntpService, SetupConfigurationService setupConfigurationService, MuzimaApplication muzimaApplication) {
        this.settingService = settingService;
        this.lastSyncTimeService = lastSyncTimeService;
        this.sntpService = sntpService;
        this.setupConfigurationService = setupConfigurationService;
        this.muzimaApplication = muzimaApplication;
        this.cohortController = muzimaApplication.getCohortController();
    }

    public MuzimaSetting getSettingByProperty(String property) throws MuzimaSettingFetchException {
        try {
            MuzimaSetting configLevelSetting = getSetupConfigurationSettingByKey("property", property);
            if(configLevelSetting != null){
                return configLevelSetting;
            }
            return settingService.getSettingByProperty(property);
        } catch (IOException | ArrayIndexOutOfBoundsException | ParseException e) { //Fails with ArrayIndexOutOfBoundsException onCall #getSettingByProperty
            throw new MuzimaSettingFetchException(e);
        }
    }

    public MuzimaSetting getSetupConfigurationSettingByKey(String keyType, String keyValue) throws MuzimaSettingFetchException{
        try{
            // Currently mUzima supports one config. So the expectation here is that only one config may be available
            // If the app is ever modified to support multiple configs, there should be an option to specify
            // the priority of the configs so that the setting in highest priority config is returned to avoid ambiguity
            SetupConfigurationTemplate activeSetupConfig = null;
            try{
                activeSetupConfig = muzimaApplication.getSetupConfigurationController().getActiveSetupConfigurationTemplate();
            } catch (SetupConfigurationController.SetupConfigurationFetchException e) {
                Log.d(getClass().getSimpleName(), "Cannot load active config",e);
            }

            if(activeSetupConfig == null){
                List<SetupConfigurationTemplate> configurationTemplates = setupConfigurationService.getSetupConfigurationTemplates();
                activeSetupConfig = configurationTemplates.get(0);
            }

            JSONObject configJson = new JSONObject(activeSetupConfig.getConfigJson());
            configJson = configJson.getJSONObject("config");
            if(configJson.has("settings")) {
                JSONArray settingsArray = configJson.getJSONArray("settings");
                for (int i = 0; i < settingsArray.length(); i++) {
                    JSONObject settingObject = settingsArray.getJSONObject(i);
                    if (settingObject.has(keyType) && keyValue.equals(settingObject.get(keyType))) {
                        return parseMuzimaSettingFromJsonObjectRepresentation(settingObject);
                    }
                }
            }
        } catch (Exception e ) {
            throw new MuzimaSettingFetchException(e);
        }
        return null;
    }

    public List<MuzimaSetting> getSettingsFromSetupConfigurationTemplate(String templateUuid) throws MuzimaSettingFetchException {
        List<MuzimaSetting> settings = new ArrayList<>();
        try {
            SetupConfigurationTemplate template = setupConfigurationService.getSetupConfigurationTemplate(templateUuid);
            JSONObject configJson = new JSONObject(template.getConfigJson());
            configJson = configJson.getJSONObject("config");
            if(configJson.has("settings")) {
                JSONArray settingsArray = configJson.getJSONArray("settings");
                for (int i = 0; i < settingsArray.length(); i++) {
                    JSONObject settingObject = settingsArray.getJSONObject(i);
                    MuzimaSetting setting = parseMuzimaSettingFromJsonObjectRepresentation(settingObject);
                    settings.add(setting);
                }
            }
        } catch (IOException | JSONException e ) {
            throw new MuzimaSettingFetchException(e);
        }
        return settings;
    }

    private MuzimaSetting parseMuzimaSettingFromJsonObjectRepresentation(JSONObject settingObject) throws JSONException {
        MuzimaSetting muzimaSetting = new MuzimaSetting();
        muzimaSetting.setProperty((String)settingObject.get("property"));

        if(settingObject.has("uuid")) {
            muzimaSetting.setUuid((String) settingObject.get("uuid"));
        }

        if(settingObject.has("name")) {
            muzimaSetting.setName((String) settingObject.get("name"));
        }

        if(settingObject.has("description")) {
            muzimaSetting.setDescription((String) settingObject.get("description"));
        }

        if(settingObject.has("datatype")) {
            String settingDataType = (String) settingObject.get("datatype");
            muzimaSetting.setSettingDataType(settingDataType);
            if(settingObject.has("value")) {
                if ("BOOLEAN".equals(settingDataType)) {
                    muzimaSetting.setValueBoolean((Boolean) settingObject.get("value"));
                } else {
                    muzimaSetting.setValueString((String) settingObject.get("value"));
                }
            }
        }
        return muzimaSetting;
    }

    public void saveOrUpdateSetting(List<MuzimaSetting> settings) throws MuzimaSettingSaveException {
        for(MuzimaSetting setting: settings){
            saveOrUpdateSetting(setting);
        }
    }

    public void saveOrUpdateSetting(MuzimaSetting setting) throws MuzimaSettingSaveException {
        if(setting!=null) {
            try {
                MuzimaSetting preExistingSetting = settingService.getSettingByProperty(setting.getProperty());
                if (preExistingSetting != null) {
                    settingService.updateSetting(setting);
                    if (setting.getProperty().equals(ONLINE_ONLY_MODE_ENABLED_SETTING) &&
                            preExistingSetting.getValueBoolean() != setting.getValueBoolean()) {
                        toggleTheme();
                        if (!setting.getValueBoolean()) {
                            ActivityManager am = (ActivityManager) muzimaApplication.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
                            ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
                            Intent intent = new Intent();
                            intent.setComponent(cn);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            }
                            muzimaApplication.getApplicationContext().startActivity(intent);
                        } else {
                            Intent intent;
                            intent = new Intent(muzimaApplication, MainDashboardActivity.class);
                            intent.putExtra("OnlineMode", setting.getValueBoolean());
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            }
                            muzimaApplication.startActivity(intent);
                        }
                    }
                } else {
                    settingService.saveSetting(setting);
                }

                if (setting.getProperty().equals(NOTIFICATION_FEATURE_ENABLED_SETTING) && getSettingByProperty(NOTIFICATION_FEATURE_ENABLED_SETTING).getValueBoolean()) {
                    muzimaApplication.getFCMTokenController().sendTokenToServer();
                }

            } catch (IOException | NullPointerException | ParseException | MuzimaSettingFetchException e) {
                throw new MuzimaSettingSaveException(e);
            }
        }
    }

    public void toggleTheme(){
        String lightModeKey = muzimaApplication.getApplicationContext().getResources().getString(R.string.preference_light_mode);
        boolean isLightThemeEnabled = MuzimaPreferences.getBooleanPreference(muzimaApplication.getApplicationContext(), lightModeKey, false);

        MuzimaPreferences.setBooleanPreference(muzimaApplication.getApplicationContext(), lightModeKey, !isLightThemeEnabled);
    }

    public List<MuzimaSetting> downloadChangedSettingsSinceLastSync() throws MuzimaSettingDownloadException {
        try {
            LastSyncTime lastSyncTime = lastSyncTimeService.getFullLastSyncTimeInfoFor(DOWNLOAD_SETTINGS);
            Date lastSyncDate = null;
            if(lastSyncTime != null){
                lastSyncDate = lastSyncTime.getLastSyncDate();
            }
            List<MuzimaSetting> muzimaSettings = settingService.downloadAllSettings(lastSyncDate);
            LastSyncTime newLastSyncTime = new LastSyncTime(DOWNLOAD_SETTINGS, sntpService.getTimePerDeviceTimeZone());
            lastSyncTimeService.saveLastSyncTime(newLastSyncTime);
            return muzimaSettings;
        } catch (IOException e) {
            throw new MuzimaSettingDownloadException(e);
        }
    }

    public MuzimaSetting downloadSettingByProperty(String property) throws MuzimaSettingDownloadException {
        try {
            return settingService.downloadSettingByProperty(property);
        } catch (IOException e) {
            throw new MuzimaSettingDownloadException(e);
        }
    }

    public List<String> getNonDownloadedMandatorySettingsProperties() throws MuzimaSettingFetchException {
        try {
            return settingService.getNonDownloadedMandatorySettingsProperties();
        } catch (IOException | ParseException e) {
            throw new MuzimaSettingFetchException(e);
        }
    }

    public Boolean isAllMandatorySettingsDownloaded() throws MuzimaSettingFetchException {
        try {
            return settingService.isAllMandatorySettingsDownloaded();
        } catch (IOException | ParseException e) {
            throw new MuzimaSettingFetchException(e);
        }
    }

    public Boolean isMedicalRecordNumberRequiredDuringRegistration() {
        boolean requireMedicalRecordNumber = true;//should require identifier by default
        try {
            MuzimaSetting autogenerateIdentifierSetting = getSettingByProperty(PATIENT_IDENTIFIER_AUTOGENERATTION_SETTING);
            if (autogenerateIdentifierSetting != null) {
                //if identifier auto-generation if disabled, require identifier input
                requireMedicalRecordNumber = !autogenerateIdentifierSetting.getValueBoolean();
            }
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "Could not fetch requireMedicalRecordNumber setting. ", e);
        }
        return requireMedicalRecordNumber;
    }

    public Boolean isSHREnabled() {
        boolean enableSHR = false;
        try {
            MuzimaSetting muzimaSHRStatus = getSettingByProperty(SHR_FEATURE_ENABLED_SETTING);
            if (muzimaSHRStatus == null) {
                enableSHR = false;
            } else {
                enableSHR = muzimaSHRStatus.getValueBoolean();
            }
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "Could not fetch SHR feature setting. ", e);
        }
        return enableSHR;
    }

    public Boolean isGPSDataEnabled() {
        boolean isGPSDataEnabled = false;
        MuzimaSetting muzimaSetting = null;
        try {
            muzimaSetting = getSettingByProperty(GPS_FEATURE_ENABLED_SETTING);
            if (muzimaSetting != null) {
                isGPSDataEnabled = muzimaSetting.getValueBoolean();
                return isGPSDataEnabled;
            } else {
                Log.e(getClass().getSimpleName(), "muzima GPS Feature setting is missing on this server");
                return false;
            }
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "muzima GPS Feature setting is missing on this server");
            return false;
        }
    }

    public Boolean isClinicalSummaryEnabled() {
        boolean isClinicalSummaryEnabled = false;
        try {
            MuzimaSetting clinicalSummaryStatus = getSettingByProperty(CLINICAL_SUMMARY_FEATURE_ENABLED_SETTING);
            if (clinicalSummaryStatus == null) {
                isClinicalSummaryEnabled = false;
            } else {
                isClinicalSummaryEnabled = clinicalSummaryStatus.getValueBoolean();
            }
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "Could not fetch clinical summary feature setting. ", e);
        }
        return isClinicalSummaryEnabled;
    }

    public Boolean isRelationshipEnabled() {
        try {
            MuzimaSetting muzimaSetting = getSettingByProperty(RELATIONSHIP_FEATURE_ENABLED);
            if (muzimaSetting != null)
                return muzimaSetting.getValueBoolean();
            else
                Log.e(getClass().getSimpleName(), "muzima Relationship Feature setting is missing on this server");
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "muzima Relationship Feature setting is missing on this server");
        }
        return false;
    }

    public Boolean isDemographicsUpdateManulReviewNeeded() {
        try {
            MuzimaSetting muzimaSetting = getSettingByProperty(DEMOGRAPHICS_UPDATE_MANUAL_REVIEW_REQUIRED_SETTING);
            if (muzimaSetting != null)
                return muzimaSetting.getValueBoolean();
            else
                Log.e(getClass().getSimpleName(), "muzima demographicsUpdateManualReviewRequired setting is missing on this server");
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "muzima Relationship Feature setting is missing on this server");
        }
        return false;
    }

    public Boolean isGeoMappingEnabled() {
        try {
            MuzimaSetting muzimaSetting = getSettingByProperty(GEOMAPPING_FEATURE_ENABLED_SETTING);
            if (muzimaSetting != null) {
                return muzimaSetting.getValueBoolean();
            }
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "Could not fetch muzima Geomapping Feature setting",e);
        }
        return false;
    }

    public Boolean isPushNotificationsEnabled() {
        try {
            MuzimaSetting muzimaSetting = getSettingByProperty(NOTIFICATION_FEATURE_ENABLED_SETTING);
            if (muzimaSetting != null)
                return muzimaSetting.getValueBoolean();
            else
                Log.e(getClass().getSimpleName(), "muzima notification Feature setting is missing on this server");
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "muzima notification Feature setting is missing on this server");
        }
        return false;
    }

    public Boolean isBarcodeEnabled() {
        try {
            MuzimaSetting muzimaSetting = getSettingByProperty(BARCODE_FEATURE_ENABLED_SETTING);
            if (muzimaSetting != null)
                return muzimaSetting.getValueBoolean();
            else
                Log.e(getClass().getSimpleName(), "muzima Barcode setting is missing on this server");
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "muzima Barcode setting is missing on this server");
        }
        return false;
    }

    public Boolean isSingleElementEntryEnabled() {
        try {
            MuzimaSetting muzimaSetting = getSettingByProperty(SINGLE_ELEMENT_ENTRY_FEATURE_ENABLED_SETTING);
            if (muzimaSetting != null)
                return muzimaSetting.getValueBoolean();
            else
                Log.d(getClass().getSimpleName(), "muzima single element entry setting is missing on this server");
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "muzima single element entry setting is missing on this server");
        }
        return false;
    }

    public Boolean isOnlineOnlyModeEnabled() {
        try {
            MuzimaSetting muzimaSetting = getSettingByProperty(ONLINE_ONLY_MODE_ENABLED_SETTING);
            if (muzimaSetting != null) {
                return muzimaSetting.getValueBoolean();
            }
            else
                Log.e(getClass().getSimpleName(), "muzima online only mode setting is missing");
        } catch (MuzimaSettingFetchException e) {
            Log.d(getClass().getSimpleName(), "There was an error while loading muzima online only mode setting",e);
        }
        return false;
    }

    public Boolean isPatientTagGenerationEnabled() {
        try {
            MuzimaSetting muzimaSetting = getSettingByProperty(TAG_GENERATION_ENABLED_SETTING);
            if (muzimaSetting != null)
                return muzimaSetting.getValueBoolean();
            else
                Log.e(getClass().getSimpleName(), "Tag generation setting is missing on this server");
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "Tag generation setting is missing on this server");
        }
        return false;
    }

    public Boolean isDisplayOnlyCohortsInConfigEnabled() {
        try {
            MuzimaSetting muzimaSetting = getSettingByProperty(DISPLAY_ONLY_COHORTS_IN_CONFIG_SETTING);
            if (muzimaSetting != null)
                return muzimaSetting.getValueBoolean();
            else
                Log.e(getClass().getSimpleName(), "The cohort Display filter setting is missing on this server");
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "The cohort Display filter setting is missing on this server");
        }
        return false;
    }

    public Boolean isDisplayOnlyFormsInConfigEnabled() {
        try {
            MuzimaSetting muzimaSetting = getSettingByProperty(DISPLAY_ONLY_FORMS_IN_CONFIG_SETTING);
            if (muzimaSetting != null)
                return muzimaSetting.getValueBoolean();
            else
                Log.e(getClass().getSimpleName(), "The Form Display filter setting is missing on this server");
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "The Form Display filter setting is missing on this server");
        }
        return false;
    }

    public Boolean isContactListingOnPatientSummaryEnabled() {
        try {
            MuzimaSetting muzimaSetting = getSettingByProperty(CONTACT_LISTING_UNDER_CLIENT_SUMMARY_SETTING);
            if (muzimaSetting != null)
                return muzimaSetting.getValueBoolean();
            else
                Log.e(getClass().getSimpleName(), "Contact Listing setting is missing on this server");
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "Contact Listing setting is missing on this server");
        }
        return false;
    }

    public Boolean isObsListingOnPatientSummaryEnabled() {
        try {
            MuzimaSetting muzimaSetting = getSettingByProperty(OBS_LISTING_UNDER_CLIENT_SUMMARY_SETTING);
            if (muzimaSetting != null)
                return muzimaSetting.getValueBoolean();
            else
                Log.e(getClass().getSimpleName(), "Obs Listing setting is missing on this server");
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "Obs Listing setting is missing on this server");
        }
        return false;
    }

    public Boolean isHistoricalDataTabEnabled() {
        try {
            MuzimaSetting muzimaSetting = getSettingByProperty(HISTORICAL_DATA_TAB_ENABLED_SETTING);
            if (muzimaSetting != null)
                return muzimaSetting.getValueBoolean();
            else
                Log.e(getClass().getSimpleName(), "Historical Data Tab setting is missing on this server");
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "Could not fetch Historical Data Tab setting");
        }
        return true;
    }

    public Boolean isDisallowServerPatientSearch() {
        try {
            MuzimaSetting muzimaSetting = getSettingByProperty(DISALLOW_SERVER_PATIENT_SEARCH);
            if (muzimaSetting != null)
                return muzimaSetting.getValueBoolean();
            else
                Log.e(getClass().getSimpleName(), "Patient server search setting is missing on this server");
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "Patient server search setting is missing on this server");
        }
        return false;
    }

    public Boolean isDefaultLoggedInUserAsEncounterProvider() {
        try {
            MuzimaSetting muzimaSetting = getSettingByProperty(DEFAULT_LOGGED_IN_USER_AS_ENCOUNTER_PROVIDER_SETTING);
            if (muzimaSetting != null)
                return muzimaSetting.getValueBoolean();
            else
                Log.e(getClass().getSimpleName(), "Encounter Provider setting is missing on this server");
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "Encounter Provider setting is missing on this server");
        }
        return false;
    }

    public Boolean isPatientRegistrationEnabled() {
        try {
            MuzimaSetting muzimaSetting = getSettingByProperty(PATIENT_REGISTRATION_ENABLED_SETTING);
            if (muzimaSetting != null)
                return muzimaSetting.getValueBoolean();
            else
                Log.e(getClass().getSimpleName(), "Patient registration setting is missing on this server");
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "Patient registration setting is missing on this server");
        }
        return true;
    }

    public Boolean isPatientSearchBeforeRegistrationEnabled() {
        try {
            MuzimaSetting muzimaSetting = getSettingByProperty(PATIENT_REGISTRATION_BUTTON_ACTION_ENABLED_SETTING);
            if (muzimaSetting != null)
                return muzimaSetting.getValueBoolean();
            else
                Log.e(getClass().getSimpleName(), "Patient registration Button action setting is missing on this server");
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "Patient registration Button action setting is missing on this server");
        }
        return true;
    }

    public Boolean isBottomNavigationCohortEnabled() {
        try {
            MuzimaSetting muzimaSetting = getSettingByProperty(BOTTOM_NAVIGATION_COHORT_ENABLED_SETTING);
            if (muzimaSetting != null)
                return muzimaSetting.getValueBoolean();
            else
                Log.e(getClass().getSimpleName(), "Bottom nav cohort setting is missing on this server");
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "Bottom nav cohort setting setting is missing on this server");
        }
        return true;
    }

    public Boolean isBottomNavigationFormEnabled() {
        try {
            MuzimaSetting muzimaSetting = getSettingByProperty(BOTTOM_NAVIGATION_FORM_ENABLED_SETTING);
            if (muzimaSetting != null) {
                return muzimaSetting.getValueBoolean();
            } else
                Log.e(getClass().getSimpleName(), "Bottom nav form setting is missing on this server");
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "Bottom nav form setting is missing on this server");
        }
        return true;
    }

    public Boolean isFGHCustomClientSummaryEnabled() {
        try {
            MuzimaSetting muzimaSetting = getSettingByProperty(FGH_CUSTOM_CLIENT_SUMMARY_ENABLED_SETTING);
            if (muzimaSetting != null)
                return muzimaSetting.getValueBoolean();
            else
                Log.e(getClass().getSimpleName(), "FGH custom setting is missing on this server");
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "FGH custom setting is missing on this server");
        }
        return false;
    }

    public Boolean isFuzzySearchEnabled() {
        try {
            MuzimaSetting muzimaSetting = getSettingByProperty(PATIENT_FUZZY_SEARCH_ENABLED_SETTING);
            if (muzimaSetting != null)
                return muzimaSetting.getValueBoolean();
            else
                Log.e(getClass().getSimpleName(), "Patient Fuzzy Search setting is missing on this server");
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "Patient Fuzzy Search setting is missing on this server");
        }
        return false;
    }

    public Boolean isAllocationTagGenerationEnabled() {
        try {
            MuzimaSetting muzimaSetting = getSettingByProperty(ALLOCATION_TAG_GENERATION);
            if (muzimaSetting != null)
                return muzimaSetting.getValueBoolean();
            else
                Log.e(getClass().getSimpleName(), "Allocation tag generation setting is missing on this server");
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "Allocation tag generation setting is missing on this server");
        }
        return true;
    }

    public Boolean isFGHCustomClientAddressEnabled() {
        try {
            MuzimaSetting muzimaSetting = getSettingByProperty(FGH_CUSTOM_CLIENT_ADDRESS);
            if (muzimaSetting != null)
                return muzimaSetting.getValueBoolean();
            else
                Log.e(getClass().getSimpleName(), "Allocation tag generation setting is missing on this server");
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "Allocation tag generation setting is missing on this server");
        }
        return false;
    }

    public Boolean isFGHCustomConfidantOptionEnabled() {
        try {
            MuzimaSetting muzimaSetting = getSettingByProperty(FGH_CUSTOM_CONFIDANT_INFORMATION_ENABLED_SETTING);
            if (muzimaSetting != null)
                return muzimaSetting.getValueBoolean();
            else
                Log.e(getClass().getSimpleName(), "FGH custom setting is missing on this server");
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "FGH custom setting is missing on this server");
        }
        return false;
    }

    public Boolean isFGHCustomClinicalOptionEnabled() {
        try {
            MuzimaSetting muzimaSetting = getSettingByProperty(FGH_CUSTOM_CLINICAL_INFORMATION_ENABLED_SETTING);
            if (muzimaSetting != null)
                return muzimaSetting.getValueBoolean();
            else
                Log.e(getClass().getSimpleName(), "FGH custom setting is missing on this server");
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "FGH custom setting is missing on this server");
        }
        return false;
    }

    public Boolean isHTCAddContactOptionEnabled() {
        try {
            MuzimaSetting muzimaSetting = getSettingByProperty(ADD_CONTACT_POPUP_ENABLED_SETTING);
            if (muzimaSetting != null)
                return muzimaSetting.getValueBoolean();
            else
                Log.e(getClass().getSimpleName(), "FGH custom setting is missing on this server");
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "FGH custom setting is missing on this server");
        }
        return false;
    }

    public Boolean isFormDuplicateCheckEnabled(){
        try {
            MuzimaSetting muzimaSetting = getSettingByProperty(FORM_DUPLICATE_CHECK_ENABLED_SETTING);
            if (muzimaSetting != null)
                return muzimaSetting.getValueBoolean();
            else
                Log.e(getClass().getSimpleName(), "Setting is missing on this server");
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "Setting is missing on this server");
        }
        return true;
    }

    public Boolean isRealTimeSyncEnabled(){
        try {
            if(isOnlineOnlyModeEnabled()){
                return true;
            } else {
                MuzimaSetting muzimaSetting = getSettingByProperty(AUTOMATIC_FORM_SYNC_ENABLED_SETTING);
                if (muzimaSetting != null)
                    return muzimaSetting.getValueBoolean();
                else
                    Log.e(getClass().getSimpleName(), "Setting is missing on this server");
            }
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "Setting is missing on this server");
        }
        return false;
    }

    public Boolean isConfidentialityNoticeDisplayEnabled(){
        try {
            MuzimaSetting muzimaSetting = getSettingByProperty(CONFIDENTIALITY_NOTICE_DISPLAY_ENABLED_SETTING);
            if (muzimaSetting != null)
                return muzimaSetting.getValueBoolean();
            else
                Log.e(getClass().getSimpleName(), "Setting is missing on this server");
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "Setting is missing on this server");
        }
        return false;
    }

    public Boolean isClearAppDataIfNewUserEnabled(){
        try {
            MuzimaSetting muzimaSetting = getSettingByProperty(ClEAR_APP_DATA_IF_NEW_USER_ENABLED_SETTING);
            if (muzimaSetting != null)
                return muzimaSetting.getValueBoolean();
            else
                Log.e(getClass().getSimpleName(), "Setting is missing on this server");
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "Setting is missing on this server");
        }
        return false;
    }


    public boolean isSameDerivedConceptUsedToFilterMoreThanOneCohort(String derivedConceptUuid) {
        try {
            MuzimaSetting muzimaSetting = getSettingByProperty(COHORT_FILTER_DERIVED_CONCEPT_MAP);
            List<Cohort> cohorts = new ArrayList<>();
            if(isMultipleConfigsSupported()){
                cohorts = cohortController.getAllCohorts();
            }else{
                cohorts = cohortController.getCohorts();
            }

            if (muzimaSetting != null) {
                String settingValue = muzimaSetting.getValueString();
                if(settingValue != null) {
                    List<String> conceptUuids = new ArrayList<>();
                    for (Cohort cohort : cohorts) {
                        if (cohortController.isDownloaded(cohort)) {
                            JSONObject jsonObject = new JSONObject(settingValue);
                            Object derivedConceptObject = null;
                            String derivedConceptUuidInObject = "";
                            if (jsonObject.has(cohort.getUuid())) {
                                derivedConceptObject = jsonObject.get(cohort.getUuid());
                                if (derivedConceptObject != null && derivedConceptObject instanceof JSONArray) {
                                    JSONArray jsonArray = (JSONArray) derivedConceptObject;
                                    for (int i = 0; i < jsonArray.length(); i++) {
                                        derivedConceptUuidInObject = jsonArray.get(i).toString();
                                        if (!derivedConceptUuidInObject.isEmpty()) {
                                            if(derivedConceptUuidInObject.equals(derivedConceptUuid)){
                                                if(!conceptUuids.contains(derivedConceptUuid)){
                                                    conceptUuids.add(derivedConceptUuid);
                                                }else{
                                                    return true;
                                                }
                                            }
                                        }
                                    }
                                }else{
                                    derivedConceptUuidInObject = derivedConceptObject.toString();
                                    if (!derivedConceptUuidInObject.isEmpty()) {
                                        if(derivedConceptUuidInObject.equals(derivedConceptUuid)){
                                            if(!conceptUuids.contains(derivedConceptUuid)){
                                                conceptUuids.add(derivedConceptUuid);
                                            }else{
                                                return true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Log.e(getClass().getSimpleName(), "Setting is missing on this server");
            }
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "Setting is missing on this server");
        } catch (CohortController.CohortFetchException e) {
            Log.e(getClass().getSimpleName(), "Error while fetching cohorts");
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "Error while parsing json object");
        }
        return false;
    }

    public boolean isSameConceptUsedToFilterMoreThanOneCohort(String conceptUuid) {
        try {
            MuzimaSetting muzimaSetting = getSettingByProperty(COHORT_FILTER_CONCEPT_MAP);
            if (muzimaSetting != null) {
                String settingValue = muzimaSetting.getValueString();
                if(settingValue != null) {
                    List<String> conceptUuids = new ArrayList<>();
                    for (Cohort cohort : cohortController.getCohorts()) {
                        if (cohortController.isDownloaded(cohort)) {
                            JSONObject jsonObject = new JSONObject(settingValue);
                            Object conceptObject = null;
                            String conceptUuidInObject = "";
                            if (jsonObject.has(cohort.getUuid())) {
                                conceptObject = jsonObject.get(cohort.getUuid());
                                if (conceptObject != null && conceptObject instanceof JSONArray) {
                                    JSONArray jsonArray = (JSONArray) conceptObject;
                                    for (int i = 0; i < jsonArray.length(); i++) {
                                        conceptUuidInObject = jsonArray.get(i).toString();
                                        if (!conceptUuidInObject.isEmpty()) {
                                            if(conceptUuidInObject.equals(conceptUuid)){
                                                if(!conceptUuids.contains(conceptUuid)){
                                                    conceptUuids.add(conceptUuid);
                                                }else{
                                                    return true;
                                                }
                                            }
                                        }
                                    }
                                }else{
                                    conceptUuidInObject = conceptObject.toString();
                                    if (!conceptUuidInObject.isEmpty()) {
                                        if(conceptUuidInObject.equals(conceptUuid)){
                                            if(!conceptUuids.contains(conceptUuid)){
                                                conceptUuids.add(conceptUuid);
                                            }else{
                                                return true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Log.e(getClass().getSimpleName(), "Setting is missing on this server");
            }
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "Setting is missing on this server");
        } catch (CohortController.CohortFetchException e) {
            Log.e(getClass().getSimpleName(), "Error while fetching cohorts");
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "Error while parsing json object");
        }

        return false;
    }

    public Boolean isUsernameAutoPopulationEnabled(){
        try {
            MuzimaSetting muzimaSetting = getSettingByProperty(USERNAME_AUTO_POPULATION_ENABLED_SETTING);
            if (muzimaSetting != null)
                return muzimaSetting.getValueBoolean();
            else
                Log.e(getClass().getSimpleName(), "Setting is missing on this server");
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "Setting is missing on this server");
        }
        return false;
    }

    public Boolean isMultipleConfigsSupported(){
        try {
            MuzimaSetting muzimaSetting = getSettingByProperty(MULTIPLE_CONFIGS_ENABLED_SETTING);
            if (muzimaSetting != null)
                return muzimaSetting.getValueBoolean();
            else
                Log.e(getClass().getSimpleName(), "Setting is missing on the server");
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "Setting is missing on the server");
        }
        return false;
    }

    public List<String> getTagsForPatientsToHide() throws MuzimaSettingFetchException {
        List<String> tags = new ArrayList<>();
        try {
            MuzimaSetting muzimaSetting = getSettingByProperty(PATIENT_LIST_HIDE_WITH_TAGS_SETTING);
            if (muzimaSetting != null) {
                if (!StringUtils.isEmpty(muzimaSetting.getValueString()))
                    tags = Arrays.asList(muzimaSetting.getValueString().split("\\s*,\\s*"));
            } else
                Log.d(getClass().getSimpleName(), "Setting is missing on the server");
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "Setting is missing on the server");
        }
        return tags;
    }

    public static class MuzimaSettingFetchException extends Throwable {
        MuzimaSettingFetchException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class MuzimaSettingSaveException extends Throwable {
        MuzimaSettingSaveException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class MuzimaSettingDownloadException extends Throwable {
        MuzimaSettingDownloadException(Throwable throwable) {
            super(throwable);
        }
    }
}


