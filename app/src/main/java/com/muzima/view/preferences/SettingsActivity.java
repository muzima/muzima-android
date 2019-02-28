/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.preferences;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v7.app.AppCompatDelegate;
import android.view.Menu;
import android.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.view.DefaultMenuDropDownHelper;
import com.muzima.view.login.LoginActivity;
import com.muzima.view.preferences.settings.SettingsPreferenceFragment;

public class SettingsActivity extends PreferenceActivity{

    private AppCompatDelegate delegate;

    @Override
    public void onUserInteraction() {
        ((MuzimaApplication) getApplication()).restartTimer();
        super.onUserInteraction();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsPreferenceFragment()).commit();
        setupActionBar();
    }

    /**
     * Set up the {@link android.app.ActionBar}.
     */
    private void setupActionBar() {
        getDelegate().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private AppCompatDelegate getDelegate() {
        if (delegate == null) {
            delegate = AppCompatDelegate.create(this, null);
        }
        return delegate;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(DefaultMenuDropDownHelper.DEFAULT_MENU, menu);
        MenuItem menuSettings = menu.findItem(R.id.action_settings);
        if (menuSettings != null) menuSettings.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        DefaultMenuDropDownHelper dropDownHelper = new DefaultMenuDropDownHelper(this);
        boolean result = dropDownHelper.onOptionsItemSelected(item);
        return result || super.onOptionsItemSelected(item);
    }

    public void launchLoginActivity(boolean isFirstLaunch) {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(LoginActivity.isFirstLaunch, isFirstLaunch);
        startActivity(intent);
        finish();
    }
}
