/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.tasks;

import android.app.ProgressDialog;
import android.os.AsyncTask;

import com.muzima.R;

import com.muzima.view.preferences.settings.SettingsPreferenceFragment;
import com.muzima.utils.NetworkUtils;


public class ValidateURLTask extends AsyncTask<String, Void, Boolean> {

    private final SettingsPreferenceFragment settingsPreferenceFragment;
    private ProgressDialog progressDialog;
    private NetworkUtils networkUtils;

    public ValidateURLTask(SettingsPreferenceFragment settingsPreferenceFragment) {
        this.settingsPreferenceFragment = settingsPreferenceFragment;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressDialog = new ProgressDialog(settingsPreferenceFragment.getActivity());
        progressDialog.setMessage(settingsPreferenceFragment.getActivity().getString(R.string.info_url_validate));
        progressDialog.show();
    }

    @Override
    protected Boolean doInBackground(String... strings) {
        String url;
        try {
            url=strings[0]+ "/ws/rest/v1/session";

            return com.muzima.util.NetworkUtils.isAddressReachable(url);

        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        progressDialog.dismiss();
        settingsPreferenceFragment.validationURLResult(aBoolean);
        super.onPostExecute(aBoolean);
    }
}
