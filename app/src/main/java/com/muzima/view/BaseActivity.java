/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view;

import android.os.Bundle;
import com.muzima.MuzimaApplication;
import com.muzima.service.MuzimaLoggerService;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.StringUtils;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

    private final LanguageUtil languageUtil = new LanguageUtil();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        languageUtil.onCreate(this);
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    private void setupActionBar() {
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
            supportActionBar.setDisplayShowTitleEnabled(true);
        }
    }

    @Override
    public void onUserInteraction() {
        ((MuzimaApplication) getApplication()).restartTimer();
        super.onUserInteraction();
    }

    @Override
    protected void onResume() {
        super.onResume();
        languageUtil.onResume(this);
        ((MuzimaApplication) getApplication()).setCurrentActivity(this);
    }
    protected void logEvent(String tag, String details){
        if(StringUtils.isEmpty(details)){
            details = "{}";
        }
        MuzimaApplication muzimaApplication = (MuzimaApplication)getApplicationContext();
        MuzimaLoggerService.log(muzimaApplication,tag,  details);
    }

    protected void logEvent(String tag){
        logEvent(tag,null);
    }
}
