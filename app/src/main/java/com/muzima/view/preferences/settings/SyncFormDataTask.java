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
import android.os.Build;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.service.MuzimaSyncService;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants;

class SyncFormDataTask extends AsyncTask<String, Void, Boolean> {
    private final SettingsPreferenceFragment settingsPreferenceFragment;
    private final MuzimaSyncService muzimaSyncService;
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            progressDialog = new ProgressDialog(settingsPreferenceFragment.getContext());
        }else{
            progressDialog = new ProgressDialog(settingsPreferenceFragment.getActivity());
        }
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
