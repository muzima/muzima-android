package com.muzima.service;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import com.muzima.MuzimaApplication;
import com.muzima.R;

public class GPSFeaturePreferenceService extends PreferenceService {

    private final SharedPreferences preferences;
    private final MuzimaApplication application;

    public GPSFeaturePreferenceService(MuzimaApplication muzimaApplication) {
        super(muzimaApplication);
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.application = muzimaApplication;
    }

    public void updateGPSDataPreferenceSettings(){
        boolean isGPDDataCollectionEnabled = application.getMuzimaSettingController()
                .isGPSDataEnabled();
        Resources resources = context.getResources();
        String key = resources.getString(R.string.preference_enable_gps_key);

        preferences.edit()
                .putBoolean(key, isGPDDataCollectionEnabled)
                .apply();
    }

    public Boolean isGPSDataCollectionSettingEnabled(){
        Resources resources = context.getResources();
        String key = resources.getString(R.string.preference_enable_gps_key);
        return preferences.getBoolean(key,false);
    }

}
