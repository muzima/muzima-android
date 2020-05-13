package com.muzima.service;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import com.muzima.MuzimaApplication;
import com.muzima.R;

public class DefaultEncounterLocationPreferenceService extends  PreferenceService{
    private final SharedPreferences settings;
    private final MuzimaApplication application;

    public DefaultEncounterLocationPreferenceService(MuzimaApplication application) {
        super(application.getApplicationContext());
        settings = PreferenceManager.getDefaultSharedPreferences(context);
        this.application = application;
    }

    public void setDefaultEncounterLocationPreference(String defaultEncounterLocation) {
        Resources resources = context.getResources();
        String key = resources.getString(R.string.preference_default_encounter_location);

        settings.edit()
                .putString(key, defaultEncounterLocation)
                .apply();
    }
}
