package com.muzima.service;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.Log;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.MuzimaSetting;
import com.muzima.controller.MuzimaSettingController;

import static com.muzima.util.Constants.ServerSettings.PATIENT_IDENTIFIER_AUTOGENERATTION_SETTING;
import static com.muzima.util.Constants.ServerSettings.PATIENT_IDENTIFIER_AUTOGENERATTION_SETTING_DEFAULT_VALUE;

public class RequireMedicalRecordNumberPreferenceService extends PreferenceService{
    public static final String TAG = ("ReqMedicalRecNumPrefSvc");
    private SharedPreferences settings;
    private MuzimaApplication application;
    public RequireMedicalRecordNumberPreferenceService(MuzimaApplication application) {
        super(application.getApplicationContext());
        settings = PreferenceManager.getDefaultSharedPreferences(context);
        this.application = application;
    }

    public Boolean getRequireMedicalRecordNumberPreferenceValue(){
        String key = context.getResources().getString(R.string.preference_require_medical_record_number);
        return settings.getBoolean(key,PATIENT_IDENTIFIER_AUTOGENERATTION_SETTING_DEFAULT_VALUE);
    }

    public void getAndSaveRequireMedicalRecordNumberPreference(){
        try {
            MuzimaSetting setting = application.getMuzimaSettingController()
                    .getSettingByProperty(PATIENT_IDENTIFIER_AUTOGENERATTION_SETTING);
            saveRequireMedicalRecordNumberPreference(setting);
        } catch (MuzimaSettingController.MuzimaSettingFetchException e){

        }
    }

    private void saveRequireMedicalRecordNumberPreference(MuzimaSetting muzimaSetting) {
        if(muzimaSetting != null) {
            Resources resources = context.getResources();
            String key = resources.getString(R.string.preference_require_medical_record_number);

            settings.edit()
                    .putBoolean(key, muzimaSetting.getValueBoolean())
                    .apply();
        } else {
            Log.e(TAG,"Cannot save preference. Provided setting is null");
        }
    }
}
