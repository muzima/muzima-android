package com.muzima.controller;

import android.util.Log;

import com.muzima.api.model.LastSyncTime;
import com.muzima.api.model.MuzimaSetting;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.api.service.MuzimaSettingService;

import com.muzima.service.SntpService;
import org.apache.lucene.queryParser.ParseException;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import static com.muzima.api.model.APIName.DOWNLOAD_SETTINGS;
import static com.muzima.util.Constants.ServerSettings.CLINICAL_SUMMARY_FEATURE_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.GPS_FEATURE_ENABLED_SETTING;
import static com.muzima.util.Constants.ServerSettings.PATIENT_IDENTIFIER_AUTOGENERATTION_SETTING;
import static com.muzima.util.Constants.ServerSettings.RELATIONSHIP_FEATURE_ENABLED;
import static com.muzima.util.Constants.ServerSettings.SHR_FEATURE_ENABLED_SETTING;

public class MuzimaSettingController {
    private final MuzimaSettingService settingService;
    private final LastSyncTimeService lastSyncTimeService;
    private final SntpService sntpService;

    public MuzimaSettingController(MuzimaSettingService settingService, LastSyncTimeService lastSyncTimeService,
                                   SntpService sntpService) {
        this.settingService = settingService;
        this.lastSyncTimeService = lastSyncTimeService;
        this.sntpService = sntpService;
    }

    public MuzimaSetting getSettingByProperty(String property) throws MuzimaSettingFetchException {
        try {
            return settingService.getSettingByProperty(property);
        } catch (IOException | ParseException e) {
            throw new MuzimaSettingFetchException(e);
        }
    }

    public MuzimaSetting getSettingByUuid(String uuid) throws MuzimaSettingFetchException {
        try {
            return settingService.getSettingByUuid(uuid);
        } catch (IOException e) {
            throw new MuzimaSettingFetchException(e);
        }
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


