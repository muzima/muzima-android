/*
 * Copyright (c) 2014 - 2017. The Trustees of Indiana University, Moi University
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
import com.muzima.service.MuzimaSyncService;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants;

public class SyncFormDataTask extends AsyncTask<String, Void, Boolean> {
    private SettingsPreferenceFragment settingsPreferenceFragment;
    private MuzimaSyncService muzimaSyncService;
    private ProgressDialog progressDialog;

    public SyncFormDataTask(SettingsPreferenceFragment settingsPreferenceFragment) {
        this.settingsPreferenceFragment = settingsPreferenceFragment;
        muzimaSyncService = ((MuzimaApplication)settingsPreferenceFragment.getActivity().getApplication()).getMuzimaSyncService();
    }

    @Override
    protected Boolean doInBackground(String... params) {
        int[] result = muzimaSyncService.uploadAllCompletedForms();
        return result[0] == SyncStatusConstants.SUCCESS;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressDialog = new ProgressDialog(settingsPreferenceFragment.getContext());
        progressDialog.setMessage(settingsPreferenceFragment.getString(R.string.title_data_synchronize));

        progressDialog.show();
    }

    @Override
    protected void onPostExecute(Boolean r) {
        super.onPostExecute(r);
        progressDialog.dismiss();
        settingsPreferenceFragment.syncedFormData(r);
    }
}
