/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.preferences.settings;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import com.muzima.MuzimaApplication;
import com.muzima.service.MuzimaSyncService;
import com.muzima.view.preferences.SettingsActivity;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants;

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
        return result[0] == SyncStatusConstants.SUCCESS;
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
        settingsActivity.syncedFormData(r);
    }
}
