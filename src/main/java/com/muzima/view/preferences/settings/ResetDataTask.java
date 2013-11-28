package com.muzima.view.preferences.settings;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import com.muzima.MuzimaApplication;
import com.muzima.domain.Credentials;
import com.muzima.service.CohortPrefixPreferenceService;
import com.muzima.service.CredentialsPreferenceService;
import com.muzima.service.WizardFinishPreferenceService;
import com.muzima.view.preferences.SettingsActivity;

public class ResetDataTask extends AsyncTask<String, Void, Void> {
    private SettingsActivity settingsActivity;
    private String newUrl;
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
        progressDialog.setMessage("Step 3: Resetting Data");
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
