/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.muzima.R;

public class TimeoutPreferenceService extends PreferenceService {
    private static final String DEFAULT_TIMEOUT_IN_MIN = "5";
    private final SharedPreferences settings;

    public TimeoutPreferenceService(Context context) {
        super(context);
        settings = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public int getTimeout(){
        String timeoutKey = context.getResources().getString(R.string.preference_timeout);
        return Integer.valueOf(settings.getString(timeoutKey, DEFAULT_TIMEOUT_IN_MIN));

    }
}
