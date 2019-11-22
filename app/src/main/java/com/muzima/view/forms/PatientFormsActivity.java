/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.MuzimaPagerAdapter;
import com.muzima.adapters.forms.PatientFormsPagerAdapter;
import com.muzima.api.model.Patient;
import com.muzima.service.GPSFeaturePreferenceService;
import com.muzima.service.MuzimaGPSLocationService;
import com.muzima.utils.Constants;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.patients.PatientSummaryActivity;

import static com.muzima.utils.Constants.MuzimaGPSLocationConstants.LOCATION_ACCESS_PERMISSION_REQUEST_CODE;


public class PatientFormsActivity extends FormsActivityBase {
    private Patient patient;
    private final ThemeUtils themeUtils = new ThemeUtils();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        themeUtils.onCreate(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_pager);
        Intent intent = getIntent();
        patient = (Patient) intent.getSerializableExtra(PatientSummaryActivity.PATIENT);
        initPager();
        initPagerIndicator();
        getSupportActionBar().setTitle(patient.getSummary());
        requestGPSLocationPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        themeUtils.onResume(this);
    }


    @Override
    protected MuzimaPagerAdapter createFormsPagerAdapter() {
        return new PatientFormsPagerAdapter(getApplicationContext(), getSupportFragmentManager(), patient);
    }

    @Override
    protected void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        int syncStatus = intent.getIntExtra(Constants.DataSyncServiceConstants.SYNC_STATUS, Constants.DataSyncServiceConstants.SyncStatusConstants.UNKNOWN_ERROR);
        int syncType = intent.getIntExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, -1);

        if (syncType == Constants.DataSyncServiceConstants.SYNC_REAL_TIME_UPLOAD_FORMS) {
            SharedPreferences sp = getSharedPreferences("COMPLETED_FORM_AREA_IN_FOREGROUND", MODE_PRIVATE);
            if (sp.getBoolean("active", false)) {
                if (syncStatus == Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS) {
                    ((PatientFormsPagerAdapter) formsPagerAdapter).onFormUploadFinish();
                }
            }
        }
    }

    public void requestGPSLocationPermissions() {
        MuzimaApplication muzimaApplication = (MuzimaApplication) getApplication();
        muzimaApplication.getMuzimaGPSLocationService().requestGPSLocationPermissions(this);
    }
}
