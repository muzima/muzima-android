/*
 * Copyright (c) Vanderbilt University Medical Center and Lambda Informatics.
 * All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.service;

import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.utils.MuzimaPreferences;

import static com.muzima.util.Constants.ServerSettings.ONLINE_ONLY_MODE_ENABLED_DEFAULT_VALUE;

public class OnlineOnlyModePreferenceService extends PreferenceService{

    private final MuzimaApplication application;

    public OnlineOnlyModePreferenceService(MuzimaApplication application){
        super(application.getApplicationContext());
        this.application = application;
    }

    public Boolean getOnlineOnlyModePreferenceValue(){
        String key = context.getResources().getString(R.string.preference_online_only_mode);
        return MuzimaPreferences.getBooleanPreference(context, key, ONLINE_ONLY_MODE_ENABLED_DEFAULT_VALUE);
    }

    public void updateOnlineOnlyModePreferenceValue(){
        boolean onlineOnlyModeEnabled = application.getMuzimaSettingController().isOnlineOnlyModeEnabled();
        String key = context.getResources().getString(R.string.preference_online_only_mode);
        MuzimaPreferences.setBooleanPreference(context, key,onlineOnlyModeEnabled);
    }
}
