/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.preferences;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatDelegate;

import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.service.MuzimaLoggerService;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.StringUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.MainDashboardActivity;
import com.muzima.view.login.LoginActivity;
import com.muzima.view.preferences.settings.SettingsPreferenceFragment;

public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private AppCompatDelegate delegate;
    private final LanguageUtil languageUtil = new LanguageUtil();

    @Override
    public void onUserInteraction() {
        ((MuzimaApplication) getApplication()).restartTimer();
        super.onUserInteraction();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.getInstance().onCreate(this,false);
        languageUtil.onCreate(this);
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        setTitle(R.string.general_settings);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsPreferenceFragment()).commit();
        setupActionBar();
        logEvent("VIEW_SETTINGS");
    }

    @Override
    protected void onResume() {
        super.onResume();
        languageUtil.onResume(this);
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * Set up the {@link android.app.ActionBar}.
     */
    private void setupActionBar() {
        if (getDelegate().getSupportActionBar() != null) {
            getDelegate().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private AppCompatDelegate getDelegate() {
        if (delegate == null) {
            delegate = AppCompatDelegate.create(this, null);
        }
        return delegate;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                launchDashboard();
                return true;
        }
        return false;
    }

    private void launchDashboard() {
        Intent intent = new Intent(getApplicationContext(), MainDashboardActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        String lightModePreferenceKey = getResources().getString(R.string.preference_light_mode);
        String localePreferenceKey = getResources().getString(R.string.preference_app_language);
        if (key.equals(lightModePreferenceKey) || key.equals(localePreferenceKey)) {
            ThemeUtils.getInstance().onResume(this);
        }
    }

    public void launchLoginActivity(boolean isFirstLaunch) {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(LoginActivity.isFirstLaunch, isFirstLaunch);
        startActivity(intent);
        finish();
    }

    public void logEvent(String tag, String details) {
        if (StringUtils.isEmpty(details)) {
            details = "{}";
        }
        MuzimaApplication muzimaApplication = (MuzimaApplication) getApplicationContext();
        MuzimaLoggerService.log(muzimaApplication, tag, details);
    }

    protected void logEvent(String tag) {
        logEvent(tag, null);
    }

    @Override
    public void onBackPressed() {
        launchDashboard();
    }
}
