/*
 * Copyright (c) 2014 - 2017. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.location;

import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;
import android.widget.Button;

import android.view.Menu;
import com.muzima.R;
import com.muzima.domain.Credentials;
import com.muzima.view.provider.ProviderListActivity;
import com.muzima.view.progressdialog.MuzimaProgressDialog;
import com.muzima.view.preferences.LocationPreferenceActivity;


public class LocationListActivity extends LocationPreferenceActivity {
    private static final String TAG = "LocationListActivity";
    private MuzimaProgressDialog muzimaProgressDialog;
    protected Credentials credentials;
    private boolean isProcessDialogOn = false;
    private PowerManager powerManager = null;
    private PowerManager.WakeLock wakeLock = null ;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        credentials = new Credentials(this);

        Button nextButton = (Button) findViewById(R.id.next);
        muzimaProgressDialog = new MuzimaProgressDialog(this);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                        navigateToNextActivity();
                    }
            });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(isProcessDialogOn){
            turnOnProgressDialog(getString(R.string.info_encounter_observation_download));
        }
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_location_list;
    }

    private void navigateToNextActivity() {
        Intent intent = new Intent(getApplicationContext(), ProviderListActivity.class);
        startActivity(intent);
        finish();
    }

    private void turnOnProgressDialog(String message){
        muzimaProgressDialog.show(message);
        isProcessDialogOn = true;
    }

}