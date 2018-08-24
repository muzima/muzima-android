/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.setupconfiguration;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.adapters.setupconfiguration.GuidedSetupActionLogAdapter;
import com.muzima.api.model.SetupConfigurationTemplate;
import com.muzima.controller.SetupConfigurationController;
import com.muzima.model.SetupActionLogModel;
import com.muzima.service.LandingPagePreferenceService;
import com.muzima.service.MuzimaSyncService;
import com.muzima.service.WizardFinishPreferenceService;
import com.muzima.util.JsonUtils;
import com.muzima.view.BroadcastListenerActivity;

import net.minidev.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants;
import com.muzima.utils.Constants.SetupLogConstants;

public class GuidedConfigurationWizardActivity extends BroadcastListenerActivity implements ListAdapter.BackgroundListQueryTaskListener {
    public static final String SETUP_CONFIG_UUID_INTENT_KEY = "SETUP_CONFIG_UUID";
    private SetupConfigurationTemplate setupConfigurationTemplate;
    private String progressUpdateMessage;
    private int wizardLevel =0;
    private boolean wizardcompletedSuccessfully = true;
    private GuidedSetupActionLogAdapter setupActionLogAdapter;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guided_setup_wizard);
        Button finishSetupButton = findViewById(R.id.finish);

        finishSetupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new WizardFinishPreferenceService(GuidedConfigurationWizardActivity.this).finishWizard();
                Intent intent = new LandingPagePreferenceService(getApplicationContext()).getLandingPageActivityLauchIntent();
                startActivity(intent);
                finish();
            }
        });
        setupActionLogAdapter = new GuidedSetupActionLogAdapter(getApplicationContext(),R.id.setup_logs_list);
        ListView setupLogsListView = findViewById(R.id.setup_logs_list);
        setupLogsListView.setAdapter(setupActionLogAdapter);

        initiateSetupConfiguration();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        removeSettingsMenu(menu);
        return true;
    }

    private void initiateSetupConfiguration(){
        String setupConfigTemplateUuid = getIntent().getStringExtra(SETUP_CONFIG_UUID_INTENT_KEY);
        fetchConfigurationTemplate(setupConfigTemplateUuid);
        downloadCohorts();
    }

    private void fetchConfigurationTemplate(String setupConfigTemplateUuid){
        try {
            SetupConfigurationController setupConfigurationController =
                    ((MuzimaApplication)getApplicationContext()).getSetupConfigurationController();
            setupConfigurationTemplate = setupConfigurationController.getSetupConfigurationTemplate(setupConfigTemplateUuid);
        }catch (SetupConfigurationController.SetupConfigurationFetchException e){
            Log.e(getClass().getSimpleName(), "Could not get setup configuration template", e);
        }
    }

    private void downloadCohorts() {
        final SetupActionLogModel downloadCohortsLog = new SetupActionLogModel();
        addSetupActionLog(downloadCohortsLog);
        new AsyncTask<Void, Void, int[]>() {
            @Override
            protected void onPreExecute() {
                downloadCohortsLog.setSetupAction(getString(R.string.info_cohort_download));
                onQueryTaskStarted();
            }
            @Override
            protected int[] doInBackground(Void... voids) {
                MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
                return muzimaSyncService.downloadCohorts();
            }
            @Override
            protected void onPostExecute(int[] result) {
                String resultStatus=null;
                String resultDescription=null;
                if (result == null){
                    resultDescription = getString(R.string.info_cohort_not_downloaded);
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else if(result[0] == SyncStatusConstants.SUCCESS) {
                    if(result[1] == 1) {
                        resultDescription = getString(R.string.info_cohort_downloaded);
                    } else {
                        resultDescription = getString(R.string.info_cohorts_downloaded, result[1]);
                    }
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else {
                    wizardcompletedSuccessfully=false;
                    resultDescription = getString(R.string.error_cohort_download);
                    resultStatus = SetupLogConstants.ACTION_FAILURE_STATUS_LOG;
                }
                downloadCohortsLog.setSetupActionResult(resultDescription);
                downloadCohortsLog.setSetupActionResultStatus(resultStatus);
                onQueryTaskFinish();
                downloadAndSavePatients();
            }
        }.execute();
    }

    private void downloadAndSavePatients() {
        final SetupActionLogModel downloadPatientsLog = new SetupActionLogModel();
        addSetupActionLog(downloadPatientsLog);
        new AsyncTask<Void, Void, int[]>() {
            @Override
            protected void onPreExecute() {
                downloadPatientsLog.setSetupAction(getString(R.string.info_patient_download));
                onQueryTaskStarted();
            }
            @Override
            protected int[] doInBackground(Void... voids) {
                List<String> uuids = extractCohortsUuids();
                if (!uuids.isEmpty()){
                    MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
                    return muzimaSyncService.downloadPatientsForCohorts(uuids.toArray(new String[uuids.size()]));
                }
                return null;
            }
            @Override
            protected void onPostExecute(int[] result) {
                String resultDescription=null;
                String resultStatus=null;
                if (result == null) {
                    resultDescription = getString(R.string.info_cohort_patient_not_download);
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else if (result[0] == SyncStatusConstants.SUCCESS) {
                    if(result[1] == 1 && result[2] == 1){
                        resultDescription = getString(R.string.info_cohort_patient_download);
                    } else if(result[1] == 1){
                        resultDescription = getString(R.string.info_cohorts_patient_download, result[2]);
                    } else if(result[2] == 1){
                        resultDescription = getString(R.string.info_cohort_patients_download, result[1]);
                    } else {
                        resultDescription = getString(R.string.info_cohorts_patients_download, result[1],result[2]);
                    }
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else {
                    wizardcompletedSuccessfully=false;
                    resultDescription = getString(R.string.error_patient_download);
                    resultStatus = SetupLogConstants.ACTION_FAILURE_STATUS_LOG;

                }
                downloadPatientsLog.setSetupActionResult(resultDescription);
                downloadPatientsLog.setSetupActionResultStatus(resultStatus);
                onQueryTaskFinish();
                downloadForms();
            }
        }.execute();
    }

    private void downloadForms() {
        final SetupActionLogModel downloadFormsLog = new SetupActionLogModel();
        addSetupActionLog(downloadFormsLog);
        new AsyncTask<Void, Void, int[]>() {
            @Override
            protected void onPreExecute() {
                downloadFormsLog.setSetupAction(getString(R.string.info_form_download));
                onQueryTaskStarted();
            }
            @Override
            protected int[] doInBackground(Void... voids) {
                MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
                return muzimaSyncService.downloadForms();
            }
            @Override
            protected void onPostExecute(int[] result) {
                String resultDescription = null;
                String resultStatus=null;
                if (result == null){
                    resultDescription = getString(R.string.info_form_not_downloaded);
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else if(result[0] == SyncStatusConstants.SUCCESS) {
                    if(result[1] == 1) {
                        resultDescription = getString(R.string.info_form_downloaded);
                    } else {
                        resultDescription = getString(R.string.info_forms_downloaded, result[1]);
                    }
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else{
                    wizardcompletedSuccessfully=false;
                    resultDescription = getString(R.string.error_form_download);
                    resultStatus = SetupLogConstants.ACTION_FAILURE_STATUS_LOG;
                }
                downloadFormsLog.setSetupActionResult(resultDescription);
                downloadFormsLog.setSetupActionResultStatus(resultStatus);
                onQueryTaskFinish();
                downloadFormTemplates();
            }
        }.execute();
    }

    private void downloadFormTemplates() {
        final SetupActionLogModel downloadFormTemplatesLog = new SetupActionLogModel();
        addSetupActionLog(downloadFormTemplatesLog);
        new AsyncTask<Void, Void, int[]>() {
            @Override
            protected void onPreExecute() {
                downloadFormTemplatesLog.setSetupAction(getString(R.string.info_form_template_download));
                onQueryTaskStarted();
            }
            @Override
            protected int[] doInBackground(Void... voids) {
                List<String> uuids = extractFormTemplatesUuids();
                if (!uuids.isEmpty()){
                    MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
                    return muzimaSyncService.downloadFormTemplates(uuids.toArray(new String[uuids.size()]),false);

                }
                return null;
            }
            @Override
            protected void onPostExecute(int[] result) {
                String resultDescription = null;
                String resultStatus=null;
                if(result == null){
                    resultDescription = getString(R.string.info_form_template_not_downloaded);
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else if (result[0] == SyncStatusConstants.SUCCESS) {
                    if(result[1] == 1) {
                        resultDescription = getString(R.string.info_form_template_downloaded);
                    } else {
                        resultDescription = getString(R.string.info_form_templates_downloaded, result[1]);
                    }
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else{
                    wizardcompletedSuccessfully=false;
                    resultDescription = getString(R.string.error_form_templates_download);
                    resultStatus = SetupLogConstants.ACTION_FAILURE_STATUS_LOG;
                }
                downloadFormTemplatesLog.setSetupActionResult(resultDescription);
                downloadFormTemplatesLog.setSetupActionResultStatus(resultStatus);
                onQueryTaskFinish();
                downloadProviders();
                downloadLocations();
                downloadConcepts();
            }
        }.execute();
    }
    private void downloadLocations() {
        final SetupActionLogModel downloadLocationsLog = new SetupActionLogModel();
        addSetupActionLog(downloadLocationsLog);
        new AsyncTask<Void, Void, int[]>() {
            @Override
            protected void onPreExecute() {
                downloadLocationsLog.setSetupAction(getString(R.string.info_location_download));
                onQueryTaskStarted();
            }
            @Override
            protected int[] doInBackground(Void... voids) {
                List<String> uuids = extractLocationsUuids();
                if (!uuids.isEmpty()){
                    MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
                    return muzimaSyncService.downloadLocations(uuids.toArray(new String[uuids.size()]));

                }
                return null;
            }
            @Override
            protected void onPostExecute(int[] result) {
                String resultDescription =null;
                String resultStatus=null;
                if (result == null){
                    resultDescription = getString(R.string.info_location_not_downloaded);
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                }else if(result[0] == SyncStatusConstants.SUCCESS) {
                    if(result[1] == 1) {
                        resultDescription = getString(R.string.info_location_downloaded);
                    } else {
                        resultDescription = getString(R.string.info_locations_downloaded,result[1]);
                    }
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else {
                    wizardcompletedSuccessfully=false;
                    resultDescription = getString(R.string.error_location_download);
                    resultStatus = SetupLogConstants.ACTION_FAILURE_STATUS_LOG;
                }
                downloadLocationsLog.setSetupActionResult(resultDescription);
                downloadLocationsLog.setSetupActionResultStatus(resultStatus);
                onQueryTaskFinish();
            }
        }.execute();
    }
    private void downloadProviders() {
        final SetupActionLogModel downloadProvidersLog = new SetupActionLogModel();
        addSetupActionLog(downloadProvidersLog);
        new AsyncTask<Void, Void, int[]>() {
            @Override
            protected void onPreExecute() {
                downloadProvidersLog.setSetupAction(getString(R.string.info_provider_download));
                onQueryTaskStarted();
            }
            @Override
            protected int[] doInBackground(Void... voids) {
                List<String> uuids = extractProvidersUuids();
                if (!uuids.isEmpty()){
                    MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
                    return muzimaSyncService.downloadProviders(uuids.toArray(new String[uuids.size()]));

                }
                return null;
            }
            @Override
            protected void onPostExecute(int[] result) {
                String resultDescription =null;
                String resultStatus=null;
                if (result == null){
                    resultDescription = getString(R.string.info_provider_not_downloaded);
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else if( result[0] == SyncStatusConstants.SUCCESS) {
                    if(result[1] == 1) {
                        resultDescription = getString(R.string.info_provider_downloaded);
                    } else {
                        resultDescription = getString(R.string.info_providers_downloaded, result[1]);
                    }
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else {
                    wizardcompletedSuccessfully=false;
                    resultDescription = getString(R.string.error_provider_download);
                    resultStatus = SetupLogConstants.ACTION_FAILURE_STATUS_LOG;
                }
                downloadProvidersLog.setSetupActionResult(resultDescription);
                downloadProvidersLog.setSetupActionResultStatus(resultStatus);
                onQueryTaskFinish();
            }
        }.execute();
    }
    private void downloadConcepts() {
        final SetupActionLogModel downloadConceptsLog = new SetupActionLogModel();
        addSetupActionLog(downloadConceptsLog);
        new AsyncTask<Void, Void, int[]>() {
            @Override
            protected void onPreExecute() {
                downloadConceptsLog.setSetupAction(getString(R.string.info_concept_download));
                onQueryTaskStarted();
            }
            @Override
            protected int[] doInBackground(Void... voids) {
                List<String> uuids = extractConceptsUuids();
                if (!uuids.isEmpty()){
                    MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
                    return muzimaSyncService.downloadConcepts(uuids.toArray(new String[uuids.size()]));

                }
                return null;
            }
            @Override
            protected void onPostExecute(int[] result) {
                String resultDescription =null;
                String resultStatus=null;
                if (result == null){
                    resultDescription = getString(R.string.info_concept_not_downloaded);
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else if(result[0] == SyncStatusConstants.SUCCESS) {
                    if(result[1] == 1) {
                        resultDescription = getString(R.string.info_concept_downloaded);
                    } else {
                        resultDescription = getString(R.string.info_concepts_downloaded, result[1]);
                    }
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else {
                    wizardcompletedSuccessfully=false;
                    resultDescription = getString(R.string.error_concept_download);
                    resultStatus = SetupLogConstants.ACTION_FAILURE_STATUS_LOG;
                }
                downloadConceptsLog.setSetupActionResult(resultDescription);
                downloadConceptsLog.setSetupActionResultStatus(resultStatus);
                onQueryTaskFinish();
                downloadEncounters();
                downloadObservations();
            }
        }.execute();
    }
    private void downloadEncounters() {
        final SetupActionLogModel downloadEncountersLog = new SetupActionLogModel();
        addSetupActionLog(downloadEncountersLog);
        new AsyncTask<Void, Void, int[]>() {
            @Override
            protected void onPreExecute() {
                downloadEncountersLog.setSetupAction(getString(R.string.info_encounter_download));
                onQueryTaskStarted();
            }
            @Override
            protected int[] doInBackground(Void... voids) {
                List<String> uuids = extractCohortsUuids();
                if (!uuids.isEmpty()){
                    MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
                    return muzimaSyncService.downloadEncountersForPatientsByCohortUUIDs(uuids.toArray(new String[uuids.size()]),false);

                }
                return null;
            }
            @Override
            protected void onPostExecute(int[] result) {
                String resultDescription =null;
                String resultStatus=null;
                if (result == null){
                    resultDescription = getString(R.string.info_encounter_patient_not_downloaded);
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else if(result[0] == SyncStatusConstants.SUCCESS) {
                    int downloadedEncounters = result[1];
                    int patients = result[2];
                    if(downloadedEncounters == 1 && patients == 1) {
                        resultDescription = getString(R.string.info_encounter_patient_downloaded);
                    } else if(downloadedEncounters == 1) {
                        resultDescription = getString(R.string.info_encounter_patients_downloaded, patients);
                    } else if(patients == 1) {
                        resultDescription = getString(R.string.info_encounters_patient_downloaded, downloadedEncounters);
                    } else if(downloadedEncounters == 0) {
                        resultDescription = getString(R.string.info_encounter_patient_not_downloaded);
                    } else {
                        resultDescription = getString(R.string.info_encounters_patients_downloaded, downloadedEncounters, patients);
                    }
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else {
                    wizardcompletedSuccessfully=false;
                    resultDescription = getString(R.string.error_encounter_download);
                    resultStatus = SetupLogConstants.ACTION_FAILURE_STATUS_LOG;
                }
                downloadEncountersLog.setSetupActionResult(resultDescription);
                downloadEncountersLog.setSetupActionResultStatus(resultStatus);
                onQueryTaskFinish();
            }
        }.execute();
    }
    private void downloadObservations() {
        final SetupActionLogModel downloadObservationsLog = new SetupActionLogModel();
        addSetupActionLog(downloadObservationsLog);
        new AsyncTask<Void, Void, int[]>() {
            @Override
            protected void onPreExecute() {
                downloadObservationsLog.setSetupAction(getString(R.string.info_observation_download));
                onQueryTaskStarted();
            }
            @Override
            protected int[] doInBackground(Void... voids) {
                List<String> uuids = extractCohortsUuids();
                if (!uuids.isEmpty()){
                    MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
                    return muzimaSyncService.downloadObservationsForPatientsByCohortUUIDs(
                            uuids.toArray(new String[uuids.size()]),false);

                }
                return null;
            }
            @Override
            protected void onPostExecute(int[] result) {
                String resultDescription =null;
                String resultStatus=null;
                if (result == null){
                    resultDescription = getString(R.string.info_observation_patient_not_downloaded);
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else if(result[0] == SyncStatusConstants.SUCCESS) {
                    int downloadedObs = result[1];
                    int patients = result[2];
                    if(downloadedObs == 1 && patients == 1) {
                        resultDescription = getString(R.string.info_observation_patient_downloaded);
                    } else if(downloadedObs == 1) {
                        resultDescription = getString(R.string.info_observation_patients_downloaded, patients);
                    } else if(patients == 1) {
                        resultDescription = getString(R.string.info_observations_patient_downloaded, downloadedObs);
                    } else if(downloadedObs == 0) {
                        resultDescription = getString(R.string.info_observation_patient_not_downloaded);
                    } else {
                        resultDescription = getString(R.string.info_observations_patients_downloaded, downloadedObs, patients);
                    }
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else {
                    wizardcompletedSuccessfully=false;
                    resultDescription = getString(R.string.error_observation_download);
                    resultStatus = SetupLogConstants.ACTION_FAILURE_STATUS_LOG;
                }
                downloadObservationsLog.setSetupActionResult(resultDescription);
                downloadObservationsLog.setSetupActionResultStatus(resultStatus);
                onQueryTaskFinish();
            }
        }.execute();
    }

    private List<String> extractConceptsUuids(){
        List<String> conceptsUuids = new ArrayList<>();
        List<Object> objects = JsonUtils.readAsObjectList(setupConfigurationTemplate.getConfigJson(),"$['config']['concepts']");
        if(objects != null){
            for(Object object:objects){
                JSONObject cohort = (JSONObject)object;
                conceptsUuids.add((String)cohort.get("uuid"));
            }
        }
        return conceptsUuids;
    }

    private List<String> extractProvidersUuids(){
        List<String> providerUuids = new ArrayList<>();
        List<Object> objects = JsonUtils.readAsObjectList(setupConfigurationTemplate.getConfigJson(),"$['config']['providers']");
        if(objects != null){
            for(Object object:objects){
                JSONObject cohort = (JSONObject)object;
                providerUuids.add((String)cohort.get("uuid"));
            }
        }
        return providerUuids;
    }

    private List<String> extractLocationsUuids(){
        List<String> locationUuids = new ArrayList<>();
        List<Object> objects = JsonUtils.readAsObjectList(setupConfigurationTemplate.getConfigJson(),"$['config']['locations']");
        if(objects != null){
            for(Object object:objects){
                JSONObject cohort = (JSONObject)object;
                locationUuids.add((String)cohort.get("uuid"));
            }
        }
        return locationUuids;
    }

    private List<String> extractFormTemplatesUuids(){
        List<String> formsuuids = new ArrayList<>();
        List<Object> objects = JsonUtils.readAsObjectList(setupConfigurationTemplate.getConfigJson(),"$['config']['forms']");
        if(objects != null){
            for(Object object:objects){
                JSONObject cohort = (JSONObject)object;
                formsuuids.add((String)cohort.get("uuid"));
            }
        }
        return formsuuids;
    }

    private List<String> extractCohortsUuids(){
        List<String> cohortUuids = new ArrayList<>();
        List<Object> objects = JsonUtils.readAsObjectList(setupConfigurationTemplate.getConfigJson(),"$['config']['cohorts']");
        if(objects != null){
            for(Object object:objects){
                JSONObject cohort = (JSONObject)object;
                cohortUuids.add((String)cohort.get("uuid"));
            }
        }
        return cohortUuids;
    }

    private void addSetupActionLog(SetupActionLogModel setupActionLogModel){
        setupActionLogAdapter.add(setupActionLogModel);
    }

    private synchronized void incrementWizardStep(){
        wizardLevel++;
    }

    private synchronized void evaluateFinishStatus(){
        int TOTAL_WIZARD_STEPS = 9;
        if(wizardLevel == (TOTAL_WIZARD_STEPS)) {
            TextView finalResult = findViewById(R.id.setup_actions_final_result);
            if(wizardcompletedSuccessfully){
                finalResult.setText(getString(R.string.info_setup_complete_success));
            } else{
                finalResult.setText(getString(R.string.info_setup_complete_fail));
                finalResult.setTextColor(Color.RED);
            }
            LinearLayout progressBarLayout = findViewById(R.id.progress_bar_container);
            progressBarLayout.setVisibility(View.GONE);
            LinearLayout nextButtonLayout = findViewById(R.id.next_button_layout);
            nextButtonLayout.setVisibility(View.VISIBLE);
        }
    }
    @Override
    public void onQueryTaskStarted() {
        setupActionLogAdapter.notifyDataSetChanged();
    }

    @Override
    public void onQueryTaskFinish() {
        setupActionLogAdapter.notifyDataSetChanged();
        incrementWizardStep();
        evaluateFinishStatus();
    }

    @Override
    public void onQueryTaskCancelled(){}

    @Override
    public void onQueryTaskCancelled(Object errorDefinition){}
}
