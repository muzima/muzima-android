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

import static com.muzima.util.Constants.ServerSettings.CONFIDENTIALITY_NOTICE_DISPLAY_ENABLED_DEFAULT_VALUE;
public class ConfidentialityNoticeDisplayPreferenceService extends PreferenceService{

    private final MuzimaApplication application;

    public ConfidentialityNoticeDisplayPreferenceService(MuzimaApplication application){
        super(application.getApplicationContext());
        this.application = application;
    }
    public Boolean getConfidentialityNoticeDisplayPreferenceValue(){
        String key = context.getResources().getString(R.string.preference_confidentiality_notice_display);
        return MuzimaPreferences.getBooleanPreference(context, key, CONFIDENTIALITY_NOTICE_DISPLAY_ENABLED_DEFAULT_VALUE);
    }

    public void updateConfidentialityNoticeDisplayPreferenceValue(){
        boolean confidentialityNoticeDisplayEnabled = application.getMuzimaSettingController().isConfidentialityNoticeDisplayEnabled();
        String key = context.getResources().getString(R.string.preference_confidentiality_notice_display);
        MuzimaPreferences.setBooleanPreference(context, key, confidentialityNoticeDisplayEnabled);
    }
}
