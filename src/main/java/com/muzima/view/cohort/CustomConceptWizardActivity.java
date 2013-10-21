package com.muzima.view.cohort;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.actionbarsherlock.view.Menu;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Cohort;
import com.muzima.controller.CohortController;
import com.muzima.service.DataSyncService;
import com.muzima.service.MuzimaSyncService;
import com.muzima.utils.Constants;
import com.muzima.view.MainActivity;
import com.muzima.view.forms.MuzimaProgressDialog;
import com.muzima.view.preferences.ConceptPreferenceActivity;

import java.util.ArrayList;
import java.util.List;

import static com.muzima.utils.Constants.DataSyncServiceConstants.*;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.AUTHENTICATION_SUCCESS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS;


public class CustomConceptWizardActivity extends ConceptPreferenceActivity {
    private static final String TAG = CustomConceptWizardActivity.class.getSimpleName();
    private MuzimaProgressDialog muzimaProgressDialog;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Button nextButton = (Button) findViewById(R.id.next);
        muzimaProgressDialog = new MuzimaProgressDialog(this);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                muzimaProgressDialog.show("Downloading Observations and Encounters...");
                new AsyncTask<Void, Void, int[]>() {
                    @Override
                    protected int[] doInBackground(Void... voids) {
                        return downloadObservationAndEncounter();
                    }

                    @Override
                    protected void onPostExecute(int[] results) {
                        if (results[0] != SUCCESS) {
                            Toast.makeText(CustomConceptWizardActivity.this, "Could not download observations for patients", Toast.LENGTH_SHORT).show();
                        }
                        if (results[1] != SUCCESS) {
                            Toast.makeText(CustomConceptWizardActivity.this, "Could not download encounters for patients", Toast.LENGTH_SHORT).show();
                        }
                        markWizardHasEnded();
                        muzimaProgressDialog.dismiss();
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
        disableSettingsMenu(menu);
        return true;
    }

    private int[] downloadObservationAndEncounter() {
        MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();

        int[] results = new int[2];
        if(muzimaSyncService.authenticate(credentials().getCredentialsArray())==AUTHENTICATION_SUCCESS){

            String[] cohortsUuidDownloaded = getDownloadedCohortUuids();

            int[] downloadObservationsResult = muzimaSyncService.downloadObservationsForPatients(cohortsUuidDownloaded);

            int[] downloadEncountersResult = muzimaSyncService.downloadEncountersForPatients(cohortsUuidDownloaded);

            results[0] = downloadObservationsResult[0];
            results[1] = downloadEncountersResult[0];
        }
        return results;
    }

    private String[] getDownloadedCohortUuids() {
        CohortController cohortController = ((MuzimaApplication) getApplicationContext()).getCohortController();
        List<Cohort> allCohorts = null;
        try {
            allCohorts = cohortController.getAllCohorts();
        } catch (CohortController.CohortFetchException e) {
            Log.w(TAG, "Exception occurred while fetching local cohorts " + e);
            Toast.makeText(getApplicationContext(), "Something went wrong while downloading relevant patient data", Toast.LENGTH_SHORT).show();
        }
        ArrayList<String> cohortsUuid = new ArrayList<String>();
        for (Cohort cohort : allCohorts){
            cohortsUuid.add(cohort.getUuid());
        }
        return cohortsUuid.toArray(new String[allCohorts.size()]);
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_custom_concept_wizard;
    }

    private void markWizardHasEnded() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String wizardFinishedKey = getResources().getString(R.string.preference_wizard_finished);
        settings.edit()
                .putBoolean(wizardFinishedKey, true)
                .commit();
    }

    private void navigateToNextActivity() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void navigateToPreviousActivity() {
        Intent intent = new Intent(getApplicationContext(), FormTemplateWizardActivity.class);
        startActivity(intent);
        finish();
    }
}