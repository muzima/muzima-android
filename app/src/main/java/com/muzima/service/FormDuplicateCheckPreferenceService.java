package com.muzima.service;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import com.muzima.MuzimaApplication;
import com.muzima.R;


public class FormDuplicateCheckPreferenceService extends PreferenceService{
    private final SharedPreferences preferences;
    private final MuzimaApplication application;

    public FormDuplicateCheckPreferenceService(MuzimaApplication muzimaApplication) {
        super(muzimaApplication);
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.application = muzimaApplication;
    }

    public void updateFormDuplicateCheckPreferenceSettings(){
        boolean isFormDuplicateCheckEnabled = application.getMuzimaSettingController()
                .isFormDuplicateCheckEnabled();
        Resources resources = context.getResources();
        String key = resources.getString(R.string.preference_duplicate_form_data_key);

        preferences.edit()
                .putBoolean(key, isFormDuplicateCheckEnabled)
                .apply();
    }

    public Boolean isFormDuplicateCheckSettingEnabled(){
        Resources resources = context.getResources();
        String key = resources.getString(R.string.preference_duplicate_form_data_key);
        return preferences.getBoolean(key,true);
    }
}
