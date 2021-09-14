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
