package com.muzima.controller;

import android.util.Log;
import com.muzima.api.model.MuzimaSetting;
import com.muzima.api.service.MuzimaSettingService;
import org.apache.lucene.queryParser.ParseException;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import static com.muzima.util.Constants.ServerSettings.PATIENT_IDENTIFIER_AUTOGENERATTION_SETTING;

public class MuzimaSettingController {
    private static final String TAG = "MuzimaSettingController";
    MuzimaSettingService settingService;

    public MuzimaSettingController(MuzimaSettingService settingService){
        this.settingService = settingService;
    }

    public MuzimaSetting getSettingByProperty(String property) throws MuzimaSettingFetchException{
        try {
            return settingService.getSettingByProperty(property);
        } catch (IOException e){
            throw new MuzimaSettingFetchException(e);
        } catch (ParseException e){
            throw new MuzimaSettingFetchException(e);
        }
    }

    public MuzimaSetting getSettingByUuid(String uuid) throws MuzimaSettingFetchException{
        try {
            return settingService.getSettingByUuid(uuid);
        } catch (IOException e){
            throw new MuzimaSettingFetchException(e);
        }
    }

    public void saveSetting(MuzimaSetting setting) throws MuzimaSettingSaveException {
        try{
            settingService.saveSetting(setting);
        }catch(IOException e){
            throw new MuzimaSettingSaveException(e);
        }catch(NullPointerException e){
            throw new MuzimaSettingSaveException(e);
        }
    }

    public void updateSetting(MuzimaSetting setting) throws MuzimaSettingSaveException {
        try{
            settingService.updateSetting(setting);
        }catch(IOException e){
            throw new MuzimaSettingSaveException(e);
        }
    }

    public void saveOrUpdateSetting(MuzimaSetting setting) throws MuzimaSettingSaveException {
        try{
            if(settingService.getSettingByProperty(setting.getProperty()) != null) {
                settingService.updateSetting(setting);
            } else {
                settingService.saveSetting(setting);
            }
        }catch(IOException e){
            throw new MuzimaSettingSaveException(e);
        }catch(ParseException e){
            throw new MuzimaSettingSaveException(e);
        }catch(NullPointerException e){
            throw new MuzimaSettingSaveException(e);
        }
    }

    public List<MuzimaSetting> downloadAllSettings() throws MuzimaSettingDownloadException{
        try {
            return settingService.downloadAllSettings(new Date());
        } catch (IOException e) {
            throw new MuzimaSettingDownloadException(e);
        }
    }

    public MuzimaSetting downloadSettingByUuid(String uuid) throws MuzimaSettingDownloadException{
        try {
            return settingService.downloadSettingByUuid(uuid);
        } catch (IOException e) {
            throw new MuzimaSettingDownloadException(e);
        }
    }

    public MuzimaSetting downloadSettingByProperty(String property) throws MuzimaSettingDownloadException{
        try {
            return settingService.downloadSettingByProperty(property);
        } catch (IOException e) {
            throw new MuzimaSettingDownloadException(e);
        }
    }

    public List<String> getNonDownloadedMandatorySettingsProperties()throws MuzimaSettingFetchException{
        try {
            return settingService.getNonDownloadedMandatorySettingsProperties();
        }catch (IOException e){
            throw new MuzimaSettingFetchException(e);
        } catch (ParseException e){
            throw new MuzimaSettingFetchException(e);
        }
    }

    public Boolean isAllMandatorySettingsDownloaded() throws MuzimaSettingFetchException{
        try {
            return settingService.isAllMandatorySettingsDownloaded();
        } catch (IOException e){
            throw new MuzimaSettingFetchException(e);
        } catch (ParseException e){
            throw new MuzimaSettingFetchException(e);
        }
    }

    public Boolean isMedicalRecordNumberRequiredDuringRegistration(){
        boolean requireMedicalRecordNumber = true;//should require identifier by default
        try {
            MuzimaSetting autogenerateIdentifierSetting = getSettingByProperty(PATIENT_IDENTIFIER_AUTOGENERATTION_SETTING);
            if (autogenerateIdentifierSetting != null) {
                //if identifier auto-generation if disabled, require identifier input
                requireMedicalRecordNumber = !autogenerateIdentifierSetting.getValueBoolean();
            }
        } catch (MuzimaSettingFetchException e){
            Log.e(TAG, "Could not fetch requireMedicalRecordNumber setting. ", e);
        }
        return requireMedicalRecordNumber;
    }

    public static class MuzimaSettingFetchException extends Throwable{
        public MuzimaSettingFetchException(Throwable throwable){
            super(throwable);
        }
    }

    public static class MuzimaSettingSaveException extends Throwable{
        public MuzimaSettingSaveException(Throwable throwable){
            super(throwable);
        }
    }

    public static class MuzimaSettingDownloadException extends Throwable{
        public MuzimaSettingDownloadException(Throwable throwable){
            super(throwable);
        }
    }
}


