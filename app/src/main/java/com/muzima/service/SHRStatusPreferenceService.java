package com.muzima.service;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import com.muzima.MuzimaApplication;
import com.muzima.R;

public class SHRStatusPreferenceService extends PreferenceService{
    private final SharedPreferences settings;
    private final MuzimaApplication application;
    public SHRStatusPreferenceService(MuzimaApplication application) {
        super(application.getApplicationContext());
        settings = PreferenceManager.getDefaultSharedPreferences(context);
        this.application = application;
    }

    public void saveSHRStatusPreference() {
        boolean enableSHR = application.getMuzimaSettingController()
                .isSHREnabled();
        Resources resources = context.getResources();
        String key = resources.getString(R.string.preference_enable_shr_key);

        settings.edit()
                .putBoolean(key, enableSHR)
                .apply();
    }
}
