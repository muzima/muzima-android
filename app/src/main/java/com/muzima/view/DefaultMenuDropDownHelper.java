/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import com.muzima.R;
import com.muzima.tasks.EncryptedSharedHealthRecordSyncTask;
import com.muzima.utils.StringUtils;
import com.muzima.view.login.LoginActivity;
import com.muzima.view.preferences.SettingsActivity;
import com.muzima.view.reports.ProviderReportListActivity;

public class DefaultMenuDropDownHelper {
    public static final int DEFAULT_MENU = R.menu.dashboard;
    private final Activity activity;

    public DefaultMenuDropDownHelper(Activity activity){
        this.activity = activity;
    }

    public void launchLoginActivity(boolean isFirstLaunch) {
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.putExtra(LoginActivity.isFirstLaunch, isFirstLaunch);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case android.R.id.home:
                activity.finish();
                return true;
            case R.id.action_settings:
                intent = new Intent(activity, SettingsActivity.class);
                activity.startActivity(intent);
                return true;
            case R.id.action_provider_reports:
                intent = new Intent(activity, ProviderReportListActivity.class);
                activity.startActivity(intent);
                return true;
            case R.id.action_help:
                intent = new Intent(activity, HelpActivity.class);
                activity.startActivity(intent);
                return true;
            case R.id.action_logout:
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
                String passwordKey = activity.getResources().getString(R.string.preference_password);
                settings.edit()
                        .putString(passwordKey, StringUtils.EMPTY)
                        .commit();

                launchLoginActivity(false);
                activity.finish();
                return true;
            case R.id.menu_SHR_data_sync:
                EncryptedSharedHealthRecordSyncTask.uploadEncryptedSharedHealthRecords(activity.getApplicationContext());
                return true;
            default:
                return false;
        }
    }

    public void removeSettingsMenu(Menu menu) {
        MenuItem menuSettings = menu.findItem(R.id.action_settings);
        menuSettings.setVisible(false);
    }
}
