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


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.R;

import com.muzima.domain.Credentials;
import com.muzima.service.MuzimaLoggerService;
import com.muzima.utils.MuzimaPreferences;
import com.muzima.utils.StringUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.initialwizard.OnboardScreenActivity;
import com.muzima.view.initialwizard.TermsAndPolicyActivity;
import com.muzima.view.login.LoginActivity;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;


public class BaseAuthenticatedActivity extends AppCompatActivity {
    private static final String TAG = "BaseAuthenticatedActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        checkDisclaimerOrCredentials();
        ThemeUtils.getInstance().onResume(this);
        Log.i(TAG, "onResume: BaseAuthenticatedActivity setCurrentActivity " + this.getClass().getSimpleName());
        ((MuzimaApplication) getApplication()).setCurrentActivity(this);

    }

    private boolean checkDisclaimerOrCredentials() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String disclaimerKey = getResources().getString(R.string.preference_disclaimer);
        boolean disclaimerAccepted = settings.getBoolean(disclaimerKey, false);
        if (!MuzimaPreferences.getOnBoardingCompletedPreference(getApplicationContext())) {
            Intent intent = new Intent(this, OnboardScreenActivity.class);
            startActivity(intent);
            finish();
            return true;
        } else if (!disclaimerAccepted) {
            Intent intent = new Intent(this, TermsAndPolicyActivity.class);
            startActivity(intent);
            finish();
            return true;
        } else if (new Credentials(this).isEmpty()) {
            launchLoginActivity(false);
            return true;
        }
        return false;
    }

    public void logEvent(String tag, String details) {
        if (StringUtils.isEmpty(details)) {
            details = "{}";
        }
        MuzimaApplication muzimaApplication = (MuzimaApplication) getApplicationContext();
        MuzimaLoggerService.log(muzimaApplication, tag, details);
    }

    public void launchLoginActivity(boolean isFirstLaunch) {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra(LoginActivity.isFirstLaunch, isFirstLaunch);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    protected void logEvent(String tag) {
        logEvent(tag, null);
    }
}
