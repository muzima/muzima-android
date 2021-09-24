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
import android.view.Menu;
import android.view.MenuItem;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.controller.SmartCardController;
import com.muzima.domain.Credentials;
import com.muzima.service.MuzimaLoggerService;
import com.muzima.utils.MuzimaPreferences;
import com.muzima.utils.StringUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.initialwizard.OnboardScreenActivity;
import com.muzima.view.initialwizard.TermsAndPolicyActivity;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;


public class BaseAuthenticatedActivity extends AppCompatActivity {
    private static final String TAG = "BaseFragmentActivity";
    private DefaultMenuDropDownHelper dropDownHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
        dropDownHelper = new DefaultMenuDropDownHelper(this);
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
            dropDownHelper.launchLoginActivity(false);
            return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(DefaultMenuDropDownHelper.DEFAULT_MENU, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem syncSHRMenuItem = menu.findItem(R.id.menu_SHR_data_sync);
        if (syncSHRMenuItem != null) {
            try {
                int count = ((MuzimaApplication) getApplicationContext()).getSmartCardController().getSmartCardRecordWithNonUploadedData().size();
                if (count > 0) {
                    syncSHRMenuItem.setVisible(true);
                    syncSHRMenuItem.setTitle(getString(R.string.menu_SHR_data_sync, count));
                } else {
                    syncSHRMenuItem.setVisible(false);
                }
            } catch (SmartCardController.SmartCardRecordFetchException e) {
                Log.e(BaseAuthenticatedActivity.class.getSimpleName(), "Error fetching smartcard records");
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean result = dropDownHelper.onOptionsItemSelected(item);
        return result || super.onOptionsItemSelected(item);
    }

    protected void removeSettingsMenu(Menu menu) {
        dropDownHelper.removeSettingsMenu(menu);
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
}
