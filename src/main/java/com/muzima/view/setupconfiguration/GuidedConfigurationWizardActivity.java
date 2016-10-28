package com.muzima.view.setupconfiguration;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.SetupConfigurationTemplate;
import com.muzima.controller.SetupConfigurationController;
import com.muzima.service.MuzimaSyncService;
import com.muzima.service.WizardFinishPreferenceService;
import com.muzima.util.JsonUtils;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.MainActivity;
import net.minidev.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants;

public class GuidedConfigurationWizardActivity extends BroadcastListenerActivity implements ListAdapter.BackgroundListQueryTaskListener {
    private SetupConfigurationTemplate setupConfigurationTemplate;
    private TextView progressUpdateView;
    private String progressUpdateMessage;
    private final int TOTAL_WIZARD_STEPS = 6;
    private int wizardStatus =0;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guided_setup_wizard);
        progressUpdateView = (TextView)findViewById(R.id.activity_log);
        Button finishSetupButton = (Button) findViewById(R.id.finish);

        finishSetupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new WizardFinishPreferenceService(GuidedConfigurationWizardActivity.this).finishWizard();
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        runSetupConfiguration();
    }

    private void runSetupConfiguration(){
        String setupConfigTemplateUuid = getIntent().getStringExtra("SETUP_CONFIG_UUID");
        fetchConfigurationTemplate(setupConfigTemplateUuid);
        downloadAndSavePatients();
    }

    private void fetchConfigurationTemplate(String setupConfigTemplateUuid){
        try {
            SetupConfigurationController setupConfigurationController =
                    ((MuzimaApplication)getApplicationContext()).getSetupConfigurationController();
            setupConfigurationTemplate = setupConfigurationController.getSetupConfigurationTemplate(setupConfigTemplateUuid);
        }catch (SetupConfigurationController.SetupConfigurationFetchException e){
            e.printStackTrace();
        }
    }

    private void downloadAndSavePatients() {
        new AsyncTask<Void, Void, int[]>() {
            @Override
            protected void onPreExecute() {
                updateProgress("Downloading patient cohorts");
                onQueryTaskStarted();
            }
            @Override
            protected int[] doInBackground(Void... voids) {
                List<String> uuids = extractCohortsUuids();
                if (!uuids.isEmpty())

                {

                    MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
                    return muzimaSyncService.downloadPatientsForCohorts(uuids.toArray(new String[uuids.size()]));

                }
                return null;
            }
            @Override
            protected void onPostExecute(int[] result) {
                String status=null;
                if (result != null && result[0] == SyncStatusConstants.SUCCESS) {
                    status = getString(R.string.info_new_patient_download, result[1]);
                } else {
                    status = "Could not download cohorts";
                }
                updateProgress(status);
                onQueryTaskFinish();
                downloadForms();
            }
        }.execute();
    }

    private void downloadForms() {
        new AsyncTask<Void, Void, int[]>() {
            @Override
            protected void onPreExecute() {
                updateProgress("Downloading Form Templates");
                onQueryTaskStarted();
            }
            @Override
            protected int[] doInBackground(Void... voids) {
                List<String> uuids = extractFormsUuids();
                if (!uuids.isEmpty())

                {
                    MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
                    return muzimaSyncService.downloadFormTemplates(uuids.toArray(new String[uuids.size()]),false);

                }
                return null;
            }
            @Override
            protected void onPostExecute(int[] result) {
                String status = null;
                if (result != null && result[0] == SyncStatusConstants.SUCCESS) {
                    status = "Downloaded "+result[1]+" form templates";
                } else{
                    status = "Could not download forms";
                }
                updateProgress(status);
                onQueryTaskFinish();
                downloadProviders();
                downloadLocations();
                downloadConcepts();
            }
        }.execute();
    }
    private void downloadLocations() {
        new AsyncTask<Void, Void, int[]>() {
            @Override
            protected void onPreExecute() {
                updateProgress("Downloading Locations");
                onQueryTaskStarted();
            }
            @Override
            protected int[] doInBackground(Void... voids) {
                List<String> uuids = extractLocationsUuids();
                if (!uuids.isEmpty())

                {
                    MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
                    return muzimaSyncService.downloadLocations(uuids.toArray(new String[uuids.size()]));

                }
                return null;
            }
            @Override
            protected void onPostExecute(int[] result) {
                String status =null;
                if (result != null && result[0] == SyncStatusConstants.SUCCESS) {
                    status = "Downloaded " + result[1] + " locations";
                } else {
                    status = "Could not download locations";
                }
                updateProgress(status);
                onQueryTaskFinish();
            }
        }.execute();
    }
    private void downloadProviders() {
        new AsyncTask<Void, Void, int[]>() {
            @Override
            protected void onPreExecute() {
                updateProgress("Downloading Providers");
                onQueryTaskStarted();
            }
            @Override
            protected int[] doInBackground(Void... voids) {
                List<String> uuids = extractProvidersUuids();
                if (!uuids.isEmpty())

                {
                    MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
                    return muzimaSyncService.downloadProviders(uuids.toArray(new String[uuids.size()]));

                }
                return null;
            }
            @Override
            protected void onPostExecute(int[] result) {
                String status =null;
                if (result != null && result[0] == SyncStatusConstants.SUCCESS) {
                    status = "Downloaded " + result[1] + " providers";
                } else {
                    status = "Could not download providers";
                }
                updateProgress(status);
                onQueryTaskFinish();
            }
        }.execute();
    }
    private void downloadConcepts() {
        new AsyncTask<Void, Void, int[]>() {
            @Override
            protected void onPreExecute() {
                updateProgress("Downloading Concepts");
                onQueryTaskStarted();
            }
            @Override
            protected int[] doInBackground(Void... voids) {
                List<String> uuids = extractConceptsUuids();
                if (!uuids.isEmpty())

                {
                    MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
                    return muzimaSyncService.downloadConcepts(uuids.toArray(new String[uuids.size()]));

                }
                return null;
            }
            @Override
            protected void onPostExecute(int[] result) {
                String status =null;
                if (result != null && result[0] == SyncStatusConstants.SUCCESS) {
                    status = "Downloaded " + result[1] + " concepts";
                } else {
                    status = "Could not download concepts";
                }
                updateProgress(status);
                onQueryTaskFinish();
                downloadEncountersAndObservations();
            }
        }.execute();
    }
    private void downloadEncountersAndObservations() {
        new AsyncTask<Void, Void, int[]>() {
            @Override
            protected void onPreExecute() {
                updateProgress("Downloading Encounters and Observations");
                onQueryTaskStarted();
            }
            @Override
            protected int[] doInBackground(Void... voids) {
                List<String> uuids = extractCohortsUuids();
                if (!uuids.isEmpty())

                {
                    MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
                    return muzimaSyncService.downloadEncountersForPatientsByCohortUUIDs(uuids.toArray(new String[uuids.size()]),false);

                }
                return null;
            }
            @Override
            protected void onPostExecute(int[] result) {
                String status =null;
                if (result != null && result[0] == SyncStatusConstants.SUCCESS) {
                    status = "Downloaded  obs and encounters";
                } else {
                    status = "Could not download obs and encounters";
                }
                updateProgress(status);
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

    private List<String> extractFormsUuids(){
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
    private synchronized void updateProgress(String status){
        progressUpdateView.append("\n"+status);
    }

    private synchronized void incrementWizardStep(){
        wizardStatus++;
    }

    private synchronized void evaluateFinishStatus(){
        if(wizardStatus == (TOTAL_WIZARD_STEPS)) {
            updateProgress("\n\nSetup completed successfully");
            LinearLayout progressBarLayout = (LinearLayout) findViewById(R.id.progress_bar_container);
            progressBarLayout.setVisibility(View.GONE);
            LinearLayout nextButtonLayout = (LinearLayout) findViewById(R.id.next_button_layout);
            nextButtonLayout.setVisibility(View.VISIBLE);
        }
    }
    @Override
    public void onQueryTaskStarted() {
    }

    @Override
    public void onQueryTaskFinish() {
        incrementWizardStep();
        evaluateFinishStatus();
    }

    @Override
    public void onQueryTaskCancelled(){}

    @Override
    public void onQueryTaskCancelled(Object errorDefinition){}

}
