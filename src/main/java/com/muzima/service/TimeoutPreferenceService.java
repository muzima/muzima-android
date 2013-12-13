package com.muzima.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.muzima.R;

public class TimeoutPreferenceService extends PreferenceService {
    private static String DEFAULT_TIMEOUT_IN_MIN = "5";
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
