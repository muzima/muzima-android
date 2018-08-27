/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.location;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.view.Menu;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.domain.Credentials;
import com.muzima.view.provider.CustomProviderWizardActivity;
import com.muzima.view.forms.FormTemplateWizardActivity;
import com.muzima.view.progressdialog.MuzimaProgressDialog;
import com.muzima.view.preferences.LocationPreferenceActivity;

public class CustomLocationWizardActivity extends LocationPreferenceActivity {

    private MuzimaProgressDialog muzimaProgressDialog;
    private boolean isProcessDialogOn = false;
    private PowerManager.WakeLock wakeLock = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Credentials credentials = new Credentials(this);

        Button nextButton = findViewById(R.id.next);
        muzimaProgressDialog = new MuzimaProgressDialog(this);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AsyncTask<Void, Void, int[]>() {

                    @Override
                    protected void onPreExecute() {
                        Log.i(getClass().getSimpleName(), "Canceling timeout timer!");
                        ((MuzimaApplication) getApplication()).cancelTimer();
                        keepPhoneAwake();
                    }

                    @Override
                    protected int[] doInBackground(Void... voids) {
                        return new int[3];
                    }

                    @Override
                    protected void onPostExecute(int[] results) {
                        dismissProgressDialog();
                        navigateToNextActivity();
                    }
                }.execute();
            }
        });

        Button previousButton = findViewById(R.id.previous);
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToPreviousActivity();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        removeSettingsMenu(menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isProcessDialogOn) {
            turnOnProgressDialog(getString(R.string.info_encounter_observation_download));
        }
    }

    private void keepPhoneAwake() {
        Log.d(getClass().getSimpleName(), "Launching wake state: " + true);
        if (true) {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
            wakeLock.acquire();
        } else {
            if (wakeLock != null) {
                wakeLock.release();
            }
        }
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_custom_location_wizard;
    }

    private void navigateToNextActivity() {
        Intent intent = new Intent(getApplicationContext(), CustomProviderWizardActivity.class);
        startActivity(intent);
        finish();
    }

    private void navigateToPreviousActivity() {
        Intent intent = new Intent(getApplicationContext(), FormTemplateWizardActivity.class);
        startActivity(intent);
        finish();
    }

    private void turnOnProgressDialog(String message) {
        muzimaProgressDialog.show(message);
        isProcessDialogOn = true;
    }

    private void dismissProgressDialog() {
        if (muzimaProgressDialog != null) {
            muzimaProgressDialog.dismiss();
            isProcessDialogOn = false;
        }
    }
}
