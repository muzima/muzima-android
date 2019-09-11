package com.muzima.service;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import com.muzima.MuzimaApplication;
import com.muzima.R;

public class RelationshipPreferenceService extends PreferenceService {

    private final SharedPreferences preferences;
    private final MuzimaApplication application;

    public RelationshipPreferenceService(MuzimaApplication muzimaApplication) {
        super(muzimaApplication);
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.application = muzimaApplication;
    }

    public void updateRelationshipPreferenceSettings(){
        Resources resources = context.getResources();
        String key = resources.getString(R.string.preference_enable_relationship_key);

        preferences.edit().putBoolean(key, application.getMuzimaSettingController().isRelationshipEnabled()).apply();
    }
}
