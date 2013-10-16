package com.muzima.view.cohort;

import android.content.Intent;
import android.content.SharedPreferences;
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
import com.muzima.view.MainActivity;
import com.muzima.view.preferences.ConceptPreferenceActivity;

import java.util.ArrayList;
import java.util.List;

import static com.muzima.utils.Constants.DataSyncServiceConstants.*;


public class CustomConceptWizardActivity extends ConceptPreferenceActivity {
    private static final String TAG = CustomConceptWizardActivity.class.getSimpleName();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Button nextButton = (Button) findViewById(R.id.next);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                markWizardHasEnded();
                downloadObservationAndEncounter();
                navigateToNextActivity();
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

    private void downloadObservationAndEncounter() {
        Intent intent = new Intent(getApplicationContext(), DataSyncService.class);
        intent.putExtra(SYNC_TYPE, SYNC_PATIENTS_DATA_ONLY);
        intent.putExtra(CREDENTIALS, credentials().getCredentialsArray());
        try {
            CohortController cohortController = ((MuzimaApplication) getApplicationContext()).getCohortController();
            List<Cohort> allCohorts = cohortController.getAllCohorts();
            ArrayList<String> cohortsUuid = new ArrayList<String>();
            for (Cohort cohort : allCohorts){
                cohortsUuid.add(cohort.getUuid());
            }
            intent.putExtra(COHORT_IDS, cohortsUuid.toArray(new String[allCohorts.size()]));
            startService(intent);
        } catch (CohortController.CohortFetchException e) {
            Log.w(TAG, "Exception occurred while fetching local cohorts " + e);
            Toast.makeText(getApplicationContext(), "Something went wrong while downloading relevant patient data", Toast.LENGTH_SHORT).show();
        }
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
    }

    private void navigateToPreviousActivity() {
        Intent intent = new Intent(getApplicationContext(), FormTemplateWizardActivity.class);
        startActivity(intent);
    }
}