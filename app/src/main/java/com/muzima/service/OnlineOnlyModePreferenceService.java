package com.muzima.service;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.muzima.MuzimaApplication;
import com.muzima.R;

import static com.muzima.util.Constants.ServerSettings.ONLINE_ONLY_MODE_ENABLED_DEFAULT_VALUE;

public class OnlineOnlyModePreferenceService extends PreferenceService{

    private final SharedPreferences settings;
    private final MuzimaApplication application;

    public OnlineOnlyModePreferenceService(MuzimaApplication application){
        super(application.getApplicationContext());
        settings = PreferenceManager.getDefaultSharedPreferences(context);
        this.application = application;
    }

    public Boolean getOnlineOnlyModePreferenceValue(){
        String key = context.getResources().getString(R.string.preference_online_only_mode);
        return settings.getBoolean(key,ONLINE_ONLY_MODE_ENABLED_DEFAULT_VALUE);
    }

    public void updateOnlineOnlyModePreferenceValue(){
        boolean onlineOnlyModeEnabled = application.getMuzimaSettingController().isOnlineOnlyModeEnabled();
        String key = context.getResources().getString(R.string.preference_online_only_mode);
        settings.edit().putBoolean(key,onlineOnlyModeEnabled).apply();

        if(onlineOnlyModeEnabled){
            key = context.getResources().getString(R.string.preference_real_time_sync);
            settings.edit().putBoolean(key,true).apply();
        }
    }
}
