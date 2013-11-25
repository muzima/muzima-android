package com.muzima.view.preferences.settings;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import com.muzima.MuzimaApplication;
import com.muzima.service.MuzimaSyncService;
import com.muzima.view.preferences.SettingsActivity;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS;

public class SyncFormDataTask extends AsyncTask<String, Void, Boolean> {
    private SettingsActivity settingsActivity;
    private MuzimaSyncService muzimaSyncService;
    private ProgressDialog progressDialog;

    public SyncFormDataTask(SettingsActivity settingsActivity) {
        this.settingsActivity = settingsActivity;
        muzimaSyncService = ((MuzimaApplication) settingsActivity.getApplication()).getMuzimaSyncService();
    }

    @Override
    protected Boolean doInBackground(String... params) {
        int[] result = muzimaSyncService.uploadAllCompletedForms();
        return result[0] == SUCCESS;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressDialog = new ProgressDialog(settingsActivity);
        progressDialog.setMessage("Step 2: Synchronising Local Data");

        progressDialog.show();
    }

    @Override
    protected void onPostExecute(Boolean r) {
        super.onPostExecute(r);
        progressDialog.dismiss();
        settingsActivity.SyncedFormData(r);
    }
}
