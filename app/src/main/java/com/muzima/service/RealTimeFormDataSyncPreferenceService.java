package com.muzima.service;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import com.muzima.MuzimaApplication;
import com.muzima.R;

public class RealTimeFormDataSyncPreferenceService extends PreferenceService{

    private final SharedPreferences preferences;
    private final MuzimaApplication application;

    public RealTimeFormDataSyncPreferenceService(MuzimaApplication muzimaApplication) {
        super(muzimaApplication);
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.application = muzimaApplication;
    }

    public void updateRealTimeSyncPreferenceSettings(){
        boolean isRealTimeSyncEnabled = application.getMuzimaSettingController()
                .isRealTimeSyncEnabled();
        Resources resources = context.getResources();
        String key = resources.getString(R.string.preference_real_time_sync);

        preferences.edit()
                .putBoolean(key, isRealTimeSyncEnabled)
                .apply();
    }

    public Boolean isRealTimeSyncSettingEnabled(){
        Resources resources = context.getResources();
        String key = resources.getString(R.string.preference_real_time_sync);
        return preferences.getBoolean(key,false);
    }
}
