/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

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

    public void updateSHRStatusPreference() {
        boolean enableSHR = application.getMuzimaSettingController()
                .isSHREnabled();
        Resources resources = context.getResources();
        String key = resources.getString(R.string.preference_enable_shr_key);

        settings.edit()
                .putBoolean(key, enableSHR)
                .apply();
    }

    public Boolean isSHRStatusSettingEnabled(){
        Resources resources = context.getResources();
        String key = resources.getString(R.string.preference_enable_shr_key);
        return settings.getBoolean(key,false);
    }
}
