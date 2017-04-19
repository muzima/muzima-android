/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.tasks;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import com.muzima.R;
import com.muzima.view.preferences.SettingsActivity;

import com.muzima.view.preferences.settings.SettingsPreferenceFragment;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;


public class ValidateURLTask extends AsyncTask<String, Void, Boolean> {

    private final SettingsPreferenceFragment settingsPreferenceFragment;
    private ProgressDialog progressDialog;

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
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(strings[0] + "/ws/rest/v1/session");
        try {
            HttpResponse httpResponse = httpclient.execute(httpGet);
            if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        progressDialog.dismiss();
        settingsPreferenceFragment.validationURLResult(aBoolean);
        super.onPostExecute(aBoolean);
    }
}
