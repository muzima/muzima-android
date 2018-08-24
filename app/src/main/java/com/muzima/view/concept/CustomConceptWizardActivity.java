/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.concept;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.view.Menu;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Cohort;
import com.muzima.controller.CohortController;
import com.muzima.domain.Credentials;
import com.muzima.service.MuzimaSyncService;
import com.muzima.service.WizardFinishPreferenceService;
import com.muzima.utils.Constants;
import com.muzima.utils.StringUtils;
import com.muzima.view.InstallBarCodeWizardActivity;
import com.muzima.view.provider.CustomProviderWizardActivity;
import com.muzima.view.progressdialog.MuzimaProgressDialog;
import com.muzima.view.preferences.ConceptPreferenceActivity;

import java.util.ArrayList;
import java.util.List;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants;


public class CustomConceptWizardActivity extends ConceptPreferenceActivity {
    private MuzimaProgressDialog muzimaProgressDialog;
    private Credentials credentials;
    private boolean isProcessDialogOn = false;
    private PowerManager.WakeLock wakeLock = null ;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        credentials = new Credentials(this);

        Button nextButton = findViewById(R.id.next);
        muzimaProgressDialog = new MuzimaProgressDialog(this);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turnOnProgressDialog(getString(R.string.info_encounter_observation_download));
                new AsyncTask<Void, Void, int[]>() {

                    @Override
                    protected void onPreExecute() {
                        Log.i(getClass().getSimpleName(), "Canceling timeout timer!") ;
                        ((MuzimaApplication) getApplication()).cancelTimer();
                        keepPhoneAwake() ;
                    }

                    @Override
                    protected int[] doInBackground(Void... voids) {
                        return downloadObservationAndEncounter();
                    }

                    @Override
                    protected void onPostExecute(int[] results) {
                        dismissProgressDialog();
                        if (results[0] != SyncStatusConstants.SUCCESS) {
                            Toast.makeText(CustomConceptWizardActivity.this, getString(R.string.error_cohort_load), Toast.LENGTH_SHORT).show();
                        } else {
                            if (results[1] != SyncStatusConstants.SUCCESS) {
                                Toast.makeText(CustomConceptWizardActivity.this, getString(R.string.error_encounter_observation_download), Toast.LENGTH_SHORT).show();
                            }
                            if (results[2] != SyncStatusConstants.SUCCESS) {
                                Toast.makeText(CustomConceptWizardActivity.this, getString(R.string.error_patient_encounter_download), Toast.LENGTH_SHORT).show();
                            }
                        }
                        new WizardFinishPreferenceService(CustomConceptWizardActivity.this).finishWizard();
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

    private int[] downloadObservationAndEncounter() {
        MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();

        int[] results = new int[3];
        if(muzimaSyncService.authenticate(credentials.getCredentialsArray()) == SyncStatusConstants.AUTHENTICATION_SUCCESS){

            String[] cohortsUuidDownloaded = getDownloadedCohortUuids();
             if(cohortsUuidDownloaded==null){
                 results[0] = SyncStatusConstants.LOAD_ERROR;
                 return results;
             } else{
                 results[0] = SyncStatusConstants.SUCCESS;
             }
            int[] downloadObservationsResult = muzimaSyncService.downloadObservationsForPatientsByCohortUUIDs(cohortsUuidDownloaded, false);

            int[] downloadEncountersResult = muzimaSyncService.downloadEncountersForPatientsByCohortUUIDs(cohortsUuidDownloaded, false);

            results[1] = downloadObservationsResult[0];
            results[2] = downloadEncountersResult[0];
        }
        return results;
    }

    private String[] getDownloadedCohortUuids() {
        CohortController cohortController = ((MuzimaApplication) getApplicationContext()).getCohortController();
        List<Cohort> allCohorts = null;
        try {
            allCohorts = cohortController.getAllCohorts();
        } catch (CohortController.CohortFetchException e) {
            Log.w(getClass().getSimpleName(), "Exception occurred while fetching local cohorts ", e);
            return null;
        }
        ArrayList<String> cohortsUuid = new ArrayList<>();
        for (Cohort cohort : allCohorts){
            cohortsUuid.add(cohort.getUuid());
        }
        return cohortsUuid.toArray(new String[allCohorts.size()]);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(isProcessDialogOn){
            turnOnProgressDialog(getString(R.string.info_encounter_observation_download));
        }
    }

    private void keepPhoneAwake() {
        Log.d(getClass().getSimpleName(), "Launching wake state: " + true) ;
        if (true) {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
            wakeLock.acquire();
        } else {
            if(wakeLock != null) {
                wakeLock.release();
            }
        }
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_custom_concept_wizard;
    }

    private void navigateToNextActivity() {
        Intent intent = new Intent(getApplicationContext(), InstallBarCodeWizardActivity.class);
        startActivity(intent);
        finish();
    }

    private void navigateToPreviousActivity() {
        Intent intent = new Intent(getApplicationContext(), CustomProviderWizardActivity.class);
        startActivity(intent);
        finish();
    }

    private void turnOnProgressDialog(String title){
        muzimaProgressDialog.show(title);
        isProcessDialogOn = true;
    }

    private void dismissProgressDialog(){
        if (muzimaProgressDialog != null){
            muzimaProgressDialog.dismiss();
            isProcessDialogOn = false;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent){
        super.onReceive(context,intent);
        String message = intent.getStringExtra(Constants.ProgressDialogConstants.PROGRESS_UPDATE_MESSAGE);
        if(!StringUtils.isEmpty(message)){
            muzimaProgressDialog.updateMessage(message);
        }
    }
}