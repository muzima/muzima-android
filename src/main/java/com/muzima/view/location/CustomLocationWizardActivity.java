/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
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
import com.actionbarsherlock.view.Menu;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.domain.Credentials;
import com.muzima.view.provider.CustomProviderWizardActivity;
import com.muzima.view.forms.FormTemplateWizardActivity;
import com.muzima.view.forms.MuzimaProgressDialog;
import com.muzima.view.preferences.LocationPreferenceActivity;

public class CustomLocationWizardActivity extends LocationPreferenceActivity {

    private static final String TAG = "CustomLocationWizardActivity";
    private MuzimaProgressDialog muzimaProgressDialog;
    protected Credentials credentials;
    private boolean isProcessDialogOn = false;
    private PowerManager powerManager = null;
    private PowerManager.WakeLock wakeLock = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        credentials = new Credentials(this);

        Button nextButton = (Button) findViewById(R.id.next);
        muzimaProgressDialog = new MuzimaProgressDialog(this);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AsyncTask<Void, Void, int[]>() {

                    @Override
                    protected void onPreExecute() {
                        Log.i(TAG, "Canceling timeout timer!");
                        ((MuzimaApplication) getApplication()).cancelTimer();
                        keepPhoneAwake(true);
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

        Button previousButton = (Button) findViewById(R.id.previous);
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
            turnOnProgressDialog("Downloading Observations and Encounters...");
        }
    }

    private void keepPhoneAwake(boolean awakeState) {
        Log.d(TAG, "Launching wake state: " + awakeState);
        if (awakeState) {
            powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
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
