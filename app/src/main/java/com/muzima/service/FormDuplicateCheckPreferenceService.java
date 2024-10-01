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

import android.content.res.Resources;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.utils.MuzimaPreferences;

public class FormDuplicateCheckPreferenceService extends PreferenceService{
    private final MuzimaApplication application;

    public FormDuplicateCheckPreferenceService(MuzimaApplication muzimaApplication) {
        super(muzimaApplication);
        this.application = muzimaApplication;
    }

    public void updateFormDuplicateCheckPreferenceSettings(){
        boolean isFormDuplicateCheckEnabled = application.getMuzimaSettingController()
                .isFormDuplicateCheckEnabled();
        Resources resources = context.getResources();
        String key = resources.getString(R.string.preference_duplicate_form_data_key);

        MuzimaPreferences.setBooleanPreference(context,key, isFormDuplicateCheckEnabled);
    }

    public Boolean isFormDuplicateCheckSettingEnabled(){
        Resources resources = context.getResources();
        String key = resources.getString(R.string.preference_duplicate_form_data_key);
        return MuzimaPreferences.getBooleanPreference(context, key, false);
    }
}
