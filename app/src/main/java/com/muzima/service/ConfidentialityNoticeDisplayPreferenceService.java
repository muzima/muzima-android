package com.muzima.service;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.muzima.MuzimaApplication;
import com.muzima.R;

import static com.muzima.util.Constants.ServerSettings.CONFIDENTIALITY_NOTICE_DISPLAY_ENABLED_DEFAULT_VALUE;
public class ConfidentialityNoticeDisplayPreferenceService extends PreferenceService{

    private final SharedPreferences settings;
    private final MuzimaApplication application;

    public ConfidentialityNoticeDisplayPreferenceService(MuzimaApplication application){
        super(application.getApplicationContext());
        settings = PreferenceManager.getDefaultSharedPreferences(context);
        this.application = application;
    }
    public Boolean getConfidentialityNoticeDisplayPreferenceValue(){
        String key = context.getResources().getString(R.string.preference_confidentiality_notice_display);
        return settings.getBoolean(key,CONFIDENTIALITY_NOTICE_DISPLAY_ENABLED_DEFAULT_VALUE);
    }

    public void updateConfidentialityNoticeDisplayPreferenceValue(){
        boolean confidentialityNoticeDisplayEnabled = application.getMuzimaSettingController().isConfidentialityNoticeDisplayEnabled();
        String key = context.getResources().getString(R.string.preference_confidentiality_notice_display);
        settings.edit().putBoolean(key,confidentialityNoticeDisplayEnabled).apply();
    }
}
