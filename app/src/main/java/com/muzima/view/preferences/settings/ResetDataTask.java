/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.preferences.settings;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.domain.Credentials;
import com.muzima.service.CohortPrefixPreferenceService;
import com.muzima.service.CredentialsPreferenceService;
import com.muzima.service.WizardFinishPreferenceService;
import com.muzima.view.preferences.SettingsActivity;

class ResetDataTask extends AsyncTask<String, Void, Void> {
    private final SettingsActivity settingsActivity;
    private final String newUrl;
    private ProgressDialog progressDialog;

    public ResetDataTask(SettingsActivity settingsActivity, String newUrl) {
        this.settingsActivity = settingsActivity;
        this.newUrl = newUrl;
    }

    @Override
    protected Void doInBackground(String... params) {
        resetData();
        return null;
    }

    private void resetData() {
        ((MuzimaApplication)settingsActivity.getApplication()).clearApplicationData();
        SettingsActivity context = settingsActivity;
        new WizardFinishPreferenceService(context).resetWizard();
        new CohortPrefixPreferenceService(context).clearPrefixes();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressDialog = new ProgressDialog(settingsActivity);
        progressDialog.setMessage(settingsActivity.getString(R.string.title_data_reset));
        progressDialog.show();
    }

    @Override
    protected void onPostExecute(Void v) {
        new CredentialsPreferenceService(settingsActivity).saveCredentials(new Credentials(newUrl, null, null));
        progressDialog.dismiss();
        super.onPostExecute(v);
        settingsActivity.launchLoginActivity(true);
    }
}
