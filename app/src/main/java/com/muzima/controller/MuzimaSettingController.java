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

import android.util.Log;

import com.muzima.api.model.LastSyncTime;
import com.muzima.api.model.MuzimaSetting;
import com.muzima.api.model.SetupConfigurationTemplate;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.api.service.MuzimaSettingService;

import com.muzima.api.service.SetupConfigurationService;
import com.muzima.service.SntpService;
import org.apache.lucene.queryParser.ParseException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.muzima.api.model.APIName.DOWNLOAD_SETTINGS;
import static com.muzima.util.Constants.ServerSettings.BARCODE_FEATURE_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.CLINICAL_SUMMARY_FEATURE_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.GEOMAPPING_FEATURE_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.GPS_FEATURE_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.PATIENT_IDENTIFIER_AUTOGENERATTION_SETTING;
import static com.muzima.util.Constants.ServerSettings.RELATIONSHIP_FEATURE_ENABLED;
import static com.muzima.util.Constants.ServerSettings.NOTIFICATION_FEATURE_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.SHR_FEATURE_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.DEMOGRAPHICS_UPDATE_MANUAL_REVIEW_REQUIRED_SETTING;
import static com.muzima.util.Constants.ServerSettings.SINGLE_ELEMENT_ENTRY_FEATURE_ENABLED_SETTING;

public class MuzimaSettingController {
    private final MuzimaSettingService settingService;
    private final LastSyncTimeService lastSyncTimeService;
    private final SntpService sntpService;
    private final SetupConfigurationService setupConfigurationService;

    public MuzimaSettingController(MuzimaSettingService settingService, LastSyncTimeService lastSyncTimeService,
                                   SntpService sntpService, SetupConfigurationService setupConfigurationService) {
        this.settingService = settingService;
        this.lastSyncTimeService = lastSyncTimeService;
        this.sntpService = sntpService;
        this.setupConfigurationService = setupConfigurationService;
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

    public MuzimaSetting getSettingByUuid(String uuid) throws MuzimaSettingFetchException {
        try {
            MuzimaSetting configLevelSetting = getSetupConfigurationSettingByKey("uuid", uuid);
            if(configLevelSetting != null){
                return configLevelSetting;
            }
            return settingService.getSettingByUuid(uuid);
        } catch (IOException e) {
            throw new MuzimaSettingFetchException(e);
        }
    }

    public MuzimaSetting getSetupConfigurationSettingByKey(String keyType, String keyValue) throws MuzimaSettingFetchException{
        try{
            // Currently mUzima supports one config. So the expectation here is that only one config may be available
            // If the app is ever modified to support multiple configs, there should be an option to specify
            // the priority of the configs so that the setting in highest priority config is returned to avoid ambiguity
            List<SetupConfigurationTemplate> configurationTemplates = setupConfigurationService.getSetupConfigurationTemplates();
            for(SetupConfigurationTemplate configurationTemplate:configurationTemplates){
                JSONObject configJson = new JSONObject(configurationTemplate.getConfigJson());
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
            }
        } catch (IOException | JSONException e ) {
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

    public void saveSetting(MuzimaSetting setting) throws MuzimaSettingSaveException {
        try {
            settingService.saveSetting(setting);
        } catch (IOException | NullPointerException e) {
            throw new MuzimaSettingSaveException(e);
        }
    }

    public void updateSetting(MuzimaSetting setting) throws MuzimaSettingSaveException {
        try {
            settingService.updateSetting(setting);
        } catch (IOException e) {
            throw new MuzimaSettingSaveException(e);
        }
    }

    public void saveOrUpdateSetting(List<MuzimaSetting> settings) throws MuzimaSettingSaveException {
        for(MuzimaSetting setting: settings){
            saveOrUpdateSetting(setting);
        }
    }

    public void saveOrUpdateSetting(MuzimaSetting setting) throws MuzimaSettingSaveException {
        try {
            if (settingService.getSettingByProperty(setting.getProperty()) != null) {
                settingService.updateSetting(setting);
            } else {
                settingService.saveSetting(setting);
            }
        } catch (IOException | NullPointerException | ParseException e) {
            throw new MuzimaSettingSaveException(e);
        }
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

    public MuzimaSetting downloadSettingByUuid(String uuid) throws MuzimaSettingDownloadException {
        try {
            return settingService.downloadSettingByUuid(uuid);
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
                Log.e(getClass().getSimpleName(), "muzima single element entry setting is missing on this server");
        } catch (MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "muzima single element entry setting is missing on this server");
        }
        return false;
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


