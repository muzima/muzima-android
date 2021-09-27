/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.initialwizard;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.ViewPager2;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.adapters.setupconfiguration.GuidedSetupActionLogAdapter;
import com.muzima.adapters.setupconfiguration.GuidedSetupCardsViewPagerAdapter;
import com.muzima.api.model.Form;
import com.muzima.api.model.Location;
import com.muzima.api.model.SetupConfigurationTemplate;
import com.muzima.controller.FCMTokenContoller;
import com.muzima.controller.FormController;
import com.muzima.controller.LocationController;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.controller.SetupConfigurationController;
import com.muzima.model.SetupActionLogModel;
import com.muzima.service.DefaultEncounterLocationPreferenceService;
import com.muzima.service.MuzimaSyncService;
import com.muzima.service.WizardFinishPreferenceService;
import com.muzima.tasks.MuzimaAsyncTask;
import com.muzima.util.JsonUtils;
import com.muzima.utils.Constants;
import com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants;
import com.muzima.utils.Constants.SetupLogConstants;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.MainDashboardActivity;

import net.minidev.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("staticFieldLeak")
public class GuidedConfigurationWizardActivity extends BroadcastListenerActivity implements ListAdapter.BackgroundListQueryTaskListener {
    public static final String SETUP_CONFIG_UUID_INTENT_KEY = "SETUP_CONFIG_UUID";
    private SetupConfigurationTemplate setupConfigurationTemplate;
    private String progressUpdateMessage;
    private int wizardLevel = 0;
    private boolean wizardcompletedSuccessfully = true;
    private GuidedSetupActionLogAdapter setupActionLogAdapter;
    private ProgressBar mainProgressbar;
    private ListView setupLogsListView;
    private View setupLogsContainer;
    private TextView initialSetupStatusTextView;
    private Button finishSetupButton;
    private ViewPager viewPager;
    private ViewPager viewPagerLg;
    private CountDownTimer countDownTimer;
    private ImageView firstDotView;
    private ImageView secondDotView;
    private ImageView thirdDotView;
    private GuidedSetupCardsViewPagerAdapter guidedSetupCardsViewPagerAdapter;
    private int pageCount;

    public void onCreate(Bundle savedInstanceState) {
        ThemeUtils.getInstance().onCreate(this,false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guided_setup_wizard);
        initializeResources();
        initiateSetupConfiguration();
        startViewPagerAnimation();
    }

    private void startViewPagerAnimation() {
        countDownTimer = new CountDownTimer(1000 * 120, 6000) {
            @Override
            public void onTick(final long tick) {
                if(pageCount>55) pageCount=0;
                viewPager.setCurrentItem(pageCount, true);
                viewPagerLg.setCurrentItem(pageCount, true);
                updateStepper(pageCount);
                pageCount = pageCount + 1;
                viewPagerLg.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if(event.getAction() == MotionEvent.ACTION_DOWN) {
                            countDownTimer.cancel();
                        }
                        if(event.getAction() == MotionEvent.ACTION_UP){
                            countDownTimer.start();
                        }
                        return false;
                    }
                });
            }

            @Override
            public void onFinish() {
                countDownTimer.cancel();
                startViewPagerAnimation();
            }
        }.start();
    }

    private void updateStepper(int page) {
        if (page >= 0 && page <= 4) {
            firstDotView.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_blue_dot));
            secondDotView.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_light_blue_dot));
            thirdDotView.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_light_blue_dot));
        } else if (page >= 5 && page <= 9) {
            firstDotView.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_light_blue_dot));
            secondDotView.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_blue_dot));
            thirdDotView.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_light_blue_dot));
        } else {
            firstDotView.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_light_blue_dot));
            secondDotView.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_light_blue_dot));
            thirdDotView.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_blue_dot));
        }
    }

    private void initializeResources() {
        finishSetupButton = findViewById(R.id.finish);
        mainProgressbar = findViewById(R.id.setup_progress_bar);
        initialSetupStatusTextView = findViewById(R.id.setup_progress_status_message);
        viewPager = findViewById(R.id.setup_progress_background);
        firstDotView = findViewById(R.id.first_page_dot_view);
        secondDotView = findViewById(R.id.second_page_dot_view);
        thirdDotView = findViewById(R.id.third_page_dot_view);
        viewPagerLg = findViewById(R.id.setup_progress_background_lg);
        guidedSetupCardsViewPagerAdapter = new GuidedSetupCardsViewPagerAdapter(getSupportFragmentManager(), getApplicationContext());
        viewPager.setAdapter(guidedSetupCardsViewPagerAdapter);
        viewPagerLg.setAdapter(guidedSetupCardsViewPagerAdapter);
        finishSetupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new WizardFinishPreferenceService(GuidedConfigurationWizardActivity.this).finishWizard();
                Intent intent = new Intent(getApplicationContext(), MainDashboardActivity.class);
                startActivity(intent);
                finish();
            }
        });
        setupActionLogAdapter = new GuidedSetupActionLogAdapter(this, R.id.setup_logs_list);
        setupLogsListView = findViewById(R.id.setup_logs_list);
        setupLogsContainer = findViewById(R.id.setup_logs_layout);
        setupLogsListView.setAdapter(setupActionLogAdapter);
        setupLogsContainer.setVisibility(View.GONE);
        setupLogsListView.setDividerHeight(0);
        finishSetupButton.setVisibility(View.GONE);
        viewPager.setVisibility(View.GONE);
        viewPagerLg.setVisibility(View.VISIBLE);
        logEvent("VIEW_GUIDED_SETUP_METHOD");
    }

    private void initiateSetupConfiguration() {
        String setupConfigTemplateUuid = getIntent().getStringExtra(SETUP_CONFIG_UUID_INTENT_KEY);
        fetchConfigurationTemplate(setupConfigTemplateUuid);
        downloadSettings();
    }

    private void fetchConfigurationTemplate(String setupConfigTemplateUuid) {
        try {
            SetupConfigurationController setupConfigurationController =
                    ((MuzimaApplication) getApplicationContext()).getSetupConfigurationController();
            setupConfigurationTemplate = setupConfigurationController.getSetupConfigurationTemplate(setupConfigTemplateUuid);
        } catch (SetupConfigurationController.SetupConfigurationFetchException e) {
            Log.e(getClass().getSimpleName(), "Could not get setup configuration template", e);
        }
    }

    private void downloadSettings() {
        final SetupActionLogModel downloadSettingsLog = new SetupActionLogModel();
        addSetupActionLog(downloadSettingsLog);
        new MuzimaAsyncTask<Void, Void, int[]>() {
            @Override
            protected void onPreExecute() {
                downloadSettingsLog.setSetupAction(getString(R.string.info_settings_download_progress));
                onQueryTaskStarted();
            }

            @Override
            protected int[] doInBackground(Void... voids) {
                MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
                return muzimaSyncService.downloadNewSettings();
            }

            @Override
            protected void onPostExecute(int[] result) {
                String resultStatus = null;
                String resultDescription = null;
                if (result == null) {
                    resultDescription = getString(R.string.info_cohort_not_downloaded);
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else if (result[0] == SyncStatusConstants.SUCCESS) {
                    if (result[1] == 0) {
                        resultDescription = getString(R.string.info_settings_not_download);
                    } else if (result[1] == 1) {
                        resultDescription = getString(R.string.info_setting_downloaded);
                    } else {
                        resultDescription = getString(R.string.info_settings_downloaded, result[1]);
                    }
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else {
                    wizardcompletedSuccessfully = false;
                    resultDescription = getString(R.string.error_settings_download);
                    resultStatus = SetupLogConstants.ACTION_FAILURE_STATUS_LOG;
                }
                downloadSettingsLog.setSetupActionResult(resultDescription);
                downloadSettingsLog.setSetupActionResultStatus(resultStatus);
                onQueryTaskFinish();
                downloadLocations();
            }

            @Override
            protected void onBackgroundError(Exception e) {

            }
        }.execute();
    }

    private void downloadCohorts() {
        final SetupActionLogModel downloadCohortsLog = new SetupActionLogModel();
        addSetupActionLog(downloadCohortsLog);
        new MuzimaAsyncTask<Void, Void, int[]>() {
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
                String resultStatus = null;
                String resultDescription = null;
                if (result == null) {
                    resultDescription = getString(R.string.info_cohort_not_downloaded);
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else if (result[0] == SyncStatusConstants.SUCCESS) {
                    if (result[1] == 1) {
                        resultDescription = getString(R.string.info_cohort_downloaded);
                    } else {
                        resultDescription = getString(R.string.info_cohorts_downloaded, result[1]);
                    }
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else {
                    wizardcompletedSuccessfully = false;
                    resultDescription = getString(R.string.error_cohort_download);
                    resultStatus = SetupLogConstants.ACTION_FAILURE_STATUS_LOG;
                }
                downloadCohortsLog.setSetupActionResult(resultDescription);
                downloadCohortsLog.setSetupActionResultStatus(resultStatus);
                onQueryTaskFinish();
                downloadAndSavePatients();
            }

            @Override
            protected void onBackgroundError(Exception e) {

            }
        }.execute();
    }

    private void downloadAndSavePatients() {
        final SetupActionLogModel downloadPatientsLog = new SetupActionLogModel();
        addSetupActionLog(downloadPatientsLog);
        new MuzimaAsyncTask<Void, Void, int[]>() {
            @Override
            protected void onPreExecute() {
                downloadPatientsLog.setSetupAction(getString(R.string.info_patient_download));
                onQueryTaskStarted();
            }

            @Override
            protected int[] doInBackground(Void... voids) {
                List<String> uuids = extractCohortsUuids();
                MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
                muzimaSyncService.downloadRelationshipsTypes();

                if (!uuids.isEmpty()) {
                    int[] resultForPatients = muzimaSyncService.downloadPatientsForCohorts(uuids.toArray(new String[uuids.size()]));

                    if (resultForPatients[0] == Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS) {
                        muzimaSyncService.downloadRelationshipsForPatientsByCohortUUIDs(uuids.toArray(new String[uuids.size()]));
                    }

                    return resultForPatients;
                }
                return null;
            }

            @Override
            protected void onPostExecute(int[] result) {
                String resultDescription = null;
                String resultStatus = null;
                if (result == null) {
                    resultDescription = getString(R.string.info_cohort_patient_not_download);
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else if (result[0] == SyncStatusConstants.SUCCESS) {
                    if (result[1] == 1 && result[2] == 1) {
                        resultDescription = getString(R.string.info_cohort_patient_download);
                    } else if (result[1] == 1) {
                        resultDescription = getString(R.string.info_cohorts_patient_download, result[2]);
                    } else if (result[2] == 1) {
                        resultDescription = getString(R.string.info_cohort_patients_download, result[1]);
                    } else {
                        resultDescription = getString(R.string.info_cohorts_patients_download, result[1], result[2]);
                    }
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else {
                    wizardcompletedSuccessfully = false;
                    resultDescription = getString(R.string.error_patient_download);
                    resultStatus = SetupLogConstants.ACTION_FAILURE_STATUS_LOG;

                }
                downloadPatientsLog.setSetupActionResult(resultDescription);
                downloadPatientsLog.setSetupActionResultStatus(resultStatus);
                onQueryTaskFinish();
                downloadForms();
            }

            @Override
            protected void onBackgroundError(Exception e) {

            }
        }.execute();
    }

    private void downloadForms() {
        final SetupActionLogModel downloadFormsLog = new SetupActionLogModel();
        addSetupActionLog(downloadFormsLog);
        new MuzimaAsyncTask<Void, Void, int[]>() {
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
                String resultStatus = null;
                if (result == null) {
                    resultDescription = getString(R.string.info_form_not_downloaded);
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else if (result[0] == SyncStatusConstants.SUCCESS) {
                    if (result[1] == 1) {
                        resultDescription = getString(R.string.info_form_downloaded);
                    } else {
                        resultDescription = getString(R.string.info_forms_downloaded, result[1]);
                    }
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else {
                    wizardcompletedSuccessfully = false;
                    resultDescription = getString(R.string.error_form_download);
                    resultStatus = SetupLogConstants.ACTION_FAILURE_STATUS_LOG;
                }
                downloadFormsLog.setSetupActionResult(resultDescription);
                downloadFormsLog.setSetupActionResultStatus(resultStatus);
                onQueryTaskFinish();
                downloadFormTemplates();
            }

            @Override
            protected void onBackgroundError(Exception e) {

            }
        }.execute();
    }

    private void downloadFormTemplates() {
        final SetupActionLogModel downloadFormTemplatesLog = new SetupActionLogModel();
        addSetupActionLog(downloadFormTemplatesLog);
        new MuzimaAsyncTask<Void, Void, int[]>() {
            @Override
            protected void onPreExecute() {
                downloadFormTemplatesLog.setSetupAction(getString(R.string.info_form_template_download));
                onQueryTaskStarted();
            }

            @Override
            protected int[] doInBackground(Void... voids) {
                try {
                    List<String> formTemplateUuidsFromFormTemplates = extractFormTemplatesUuids();

                    if (isRelationshipFeatureEnabled()) {
                        List<String> relationshipFormTemplateUuids = extractRelationshipFormTemplatesUuids();
                        for (String relationshipFormTemplateUuid : relationshipFormTemplateUuids) {
                            if (!formTemplateUuidsFromFormTemplates.contains(relationshipFormTemplateUuid))
                                formTemplateUuidsFromFormTemplates.add(relationshipFormTemplateUuid);
                        }
                    }
                    if (!formTemplateUuidsFromFormTemplates.isEmpty()) {
                        MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
                        return muzimaSyncService.downloadFormTemplates(formTemplateUuidsFromFormTemplates.toArray(new String[formTemplateUuidsFromFormTemplates.size()]), false);

                    }
                } catch (FormController.FormFetchException e) {
                    Log.e(getClass().getSimpleName(), "Form fetch error: ");
                    downloadFormTemplatesLog.setSetupActionResult("Form download completed with an error");
                }

                return null;

            }

            @Override
            protected void onPostExecute(int[] result) {
                String resultDescription = null;
                String resultStatus = null;
                if (result == null) {
                    resultDescription = getString(R.string.info_form_template_not_downloaded);
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else if (result[0] == SyncStatusConstants.SUCCESS) {
                    if (result[1] == 1) {
                        resultDescription = getString(R.string.info_form_template_downloaded);
                    } else {
                        resultDescription = getString(R.string.info_form_templates_downloaded, result[1]);
                    }
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else {
                    wizardcompletedSuccessfully = false;
                    resultDescription = getString(R.string.error_form_templates_download);
                    resultStatus = SetupLogConstants.ACTION_FAILURE_STATUS_LOG;
                }
                downloadFormTemplatesLog.setSetupActionResult(resultDescription);
                downloadFormTemplatesLog.setSetupActionResultStatus(resultStatus);
                onQueryTaskFinish();
                downloadConcepts();
            }

            @Override
            protected void onBackgroundError(Exception e) {

            }
        }.execute();
    }

    private boolean isRelationshipFeatureEnabled() {
        return ((MuzimaApplication) getApplicationContext()).getMuzimaSettingController()
                .isRelationshipEnabled();
    }

    private List<String> extractRelationshipFormTemplatesUuids() throws FormController.FormFetchException {
        List<String> formUuids = new ArrayList<>();
        List<Form> availableForms = ((MuzimaApplication) getApplicationContext()).getFormController().getAllAvailableForms();
        for (Form availableForm : availableForms) {
            if (availableForm.getDiscriminator().equalsIgnoreCase(Constants.FORM_JSON_DISCRIMINATOR_RELATIONSHIP))
                formUuids.add(availableForm.getUuid());
        }
        return formUuids;
    }

    private void downloadLocations() {
        final SetupActionLogModel downloadLocationsLog = new SetupActionLogModel();
        addSetupActionLog(downloadLocationsLog);
        new MuzimaAsyncTask<Void, Void, int[]>() {
            @Override
            protected void onPreExecute() {
                downloadLocationsLog.setSetupAction(getString(R.string.info_location_download));
                onQueryTaskStarted();
            }

            @Override
            protected int[] doInBackground(Void... voids) {
                MuzimaSettingController muzimaSettingController = ((MuzimaApplication) getApplicationContext()).getMuzimaSettingController();
                boolean notificationSetting = muzimaSettingController.isPushNotificationsEnabled();
                if(notificationSetting) {
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(GuidedConfigurationWizardActivity.this);
                    String appTokenKey = GuidedConfigurationWizardActivity.this.getResources().getString(R.string.preference_app_token);
                    String token = settings.getString(appTokenKey, null);
                    FCMTokenContoller fcmTokenContoller = ((MuzimaApplication) getApplicationContext()).getFCMTokenController();
                    try {
                        fcmTokenContoller.sendTokenToServer(token, ((MuzimaApplication) getApplicationContext()).getAuthenticatedUser().getSystemId());
                    } catch (IOException e) {
                        Log.e(getClass().getSimpleName(), "Exception thrown while sending token to server" + e);
                    }
                }
                List<String> uuids = extractLocationsUuids();
                if (!uuids.isEmpty()) {
                    MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
                    return muzimaSyncService.downloadLocations(uuids.toArray(new String[uuids.size()]));

                }
                return null;
            }

            @Override
            protected void onPostExecute(int[] result) {
                String resultDescription = null;
                String resultStatus = null;
                if (result == null) {
                    resultDescription = getString(R.string.info_location_not_downloaded);
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else if (result[0] == SyncStatusConstants.SUCCESS) {
                    if (result[1] == 1) {
                        resultDescription = getString(R.string.info_location_downloaded);
                    } else {
                        resultDescription = getString(R.string.info_locations_downloaded, result[1]);
                    }
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else {
                    wizardcompletedSuccessfully = false;
                    resultDescription = getString(R.string.error_location_download);
                    resultStatus = SetupLogConstants.ACTION_FAILURE_STATUS_LOG;
                }
                checkIfCohortWithFilterByLocationExists();
                downloadLocationsLog.setSetupActionResult(resultDescription);
                downloadLocationsLog.setSetupActionResultStatus(resultStatus);
                onQueryTaskFinish();
            }

            @Override
            protected void onBackgroundError(Exception e) {

            }
        }.execute();
    }

    private void downloadProviders() {
        final SetupActionLogModel downloadProvidersLog = new SetupActionLogModel();
        addSetupActionLog(downloadProvidersLog);
        new MuzimaAsyncTask<Void, Void, int[]>() {
            @Override
            protected void onPreExecute() {
                downloadProvidersLog.setSetupAction(getString(R.string.info_provider_download));
                onQueryTaskStarted();
            }

            @Override
            protected int[] doInBackground(Void... voids) {
                List<String> uuids = extractProvidersUuids();
                if (!uuids.isEmpty()) {
                    MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
                    return muzimaSyncService.downloadProviders(uuids.toArray(new String[uuids.size()]));

                }
                return null;
            }

            @Override
            protected void onPostExecute(int[] result) {
                String resultDescription = null;
                String resultStatus = null;
                if (result == null) {
                    resultDescription = getString(R.string.info_provider_not_downloaded);
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else if (result[0] == SyncStatusConstants.SUCCESS) {
                    if (result[1] == 1) {
                        resultDescription = getString(R.string.info_provider_downloaded);
                    } else {
                        resultDescription = getString(R.string.info_providers_downloaded, result[1]);
                    }
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else {
                    wizardcompletedSuccessfully = false;
                    resultDescription = getString(R.string.error_provider_download);
                    resultStatus = SetupLogConstants.ACTION_FAILURE_STATUS_LOG;
                }
                downloadProvidersLog.setSetupActionResult(resultDescription);
                downloadProvidersLog.setSetupActionResultStatus(resultStatus);
                onQueryTaskFinish();
                downloadCohorts();
            }

            @Override
            protected void onBackgroundError(Exception e) {

            }
        }.execute();
    }

    private void downloadConcepts() {
        final SetupActionLogModel downloadConceptsLog = new SetupActionLogModel();
        addSetupActionLog(downloadConceptsLog);
        new MuzimaAsyncTask<Void, Void, int[]>() {
            @Override
            protected void onPreExecute() {
                downloadConceptsLog.setSetupAction(getString(R.string.info_concept_download));
                onQueryTaskStarted();
            }

            @Override
            protected int[] doInBackground(Void... voids) {
                List<String> uuids = extractConceptsUuids();
                if (!uuids.isEmpty()) {
                    MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
                    return muzimaSyncService.downloadConcepts(uuids.toArray(new String[uuids.size()]));

                }
                return null;
            }

            @Override
            protected void onPostExecute(int[] result) {
                String resultDescription = null;
                String resultStatus = null;
                if (result == null) {
                    resultDescription = getString(R.string.info_concept_not_downloaded);
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else if (result[0] == SyncStatusConstants.SUCCESS) {
                    if (result[1] == 1) {
                        resultDescription = getString(R.string.info_concept_downloaded);
                    } else {
                        resultDescription = getString(R.string.info_concepts_downloaded, result[1]);
                    }
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else {
                    wizardcompletedSuccessfully = false;
                    resultDescription = getString(R.string.error_concept_download);
                    resultStatus = SetupLogConstants.ACTION_FAILURE_STATUS_LOG;
                }
                downloadConceptsLog.setSetupActionResult(resultDescription);
                downloadConceptsLog.setSetupActionResultStatus(resultStatus);
                onQueryTaskFinish();
                downloadEncounters();
                downloadObservations();
            }

            @Override
            protected void onBackgroundError(Exception e) {

            }
        }.execute();
    }

    private void downloadEncounters() {
        final SetupActionLogModel downloadEncountersLog = new SetupActionLogModel();
        addSetupActionLog(downloadEncountersLog);
        new MuzimaAsyncTask<Void, Void, int[]>() {
            @Override
            protected void onPreExecute() {
                downloadEncountersLog.setSetupAction(getString(R.string.info_encounter_download));
                onQueryTaskStarted();
            }

            @Override
            protected int[] doInBackground(Void... voids) {
                List<String> uuids = extractCohortsUuids();
                if (!uuids.isEmpty()) {
                    MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
                    return muzimaSyncService.downloadEncountersForPatientsByCohortUUIDs(uuids.toArray(new String[uuids.size()]), false);

                }
                return null;
            }

            @Override
            protected void onPostExecute(int[] result) {
                String resultDescription = null;
                String resultStatus = null;
                if (result == null) {
                    resultDescription = getString(R.string.info_encounter_patient_not_downloaded);
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else if (result[0] == SyncStatusConstants.SUCCESS) {
                    int downloadedEncounters = result[1];
                    int patients = result[3];
                    if (downloadedEncounters == 1 && patients == 1) {
                        resultDescription = getString(R.string.info_encounter_patient_downloaded);
                    } else if (downloadedEncounters == 1) {
                        resultDescription = getString(R.string.info_encounter_patients_downloaded, patients);
                    } else if (patients == 1) {
                        resultDescription = getString(R.string.info_encounters_patient_downloaded, downloadedEncounters);
                    } else if (downloadedEncounters == 0) {
                        resultDescription = getString(R.string.info_encounter_patient_not_downloaded);
                    } else {
                        resultDescription = getString(R.string.info_encounters_patients_downloaded, downloadedEncounters, patients);
                    }
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else {
                    wizardcompletedSuccessfully = false;
                    resultDescription = getString(R.string.error_encounter_download);
                    resultStatus = SetupLogConstants.ACTION_FAILURE_STATUS_LOG;
                }
                downloadEncountersLog.setSetupActionResult(resultDescription);
                downloadEncountersLog.setSetupActionResultStatus(resultStatus);
                onQueryTaskFinish();
            }

            @Override
            protected void onBackgroundError(Exception e) {

            }
        }.execute();
    }

    private void downloadObservations() {
        final SetupActionLogModel downloadObservationsLog = new SetupActionLogModel();
        addSetupActionLog(downloadObservationsLog);
        new MuzimaAsyncTask<Void, Void, int[]>() {
            @Override
            protected void onPreExecute() {
                downloadObservationsLog.setSetupAction(getString(R.string.info_observation_download));
                onQueryTaskStarted();
            }

            @Override
            protected int[] doInBackground(Void... voids) {
                List<String> uuids = extractCohortsUuids();
                if (!uuids.isEmpty()) {
                    MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
                    return muzimaSyncService.downloadObservationsForPatientsByCohortUUIDs(
                            uuids.toArray(new String[uuids.size()]), false);

                }
                return null;
            }

            @Override
            protected void onPostExecute(int[] result) {
                String resultDescription = null;
                String resultStatus = null;
                if (result == null) {
                    resultDescription = getString(R.string.info_observation_patient_not_downloaded);
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else if (result[0] == SyncStatusConstants.SUCCESS) {
                    int downloadedObs = result[1];
                    int patients = result[3];
                    if (downloadedObs == 1 && patients == 1) {
                        resultDescription = getString(R.string.info_observation_patient_downloaded);
                    } else if (downloadedObs == 1) {
                        resultDescription = getString(R.string.info_observation_patients_downloaded, patients);
                    } else if (patients == 1) {
                        resultDescription = getString(R.string.info_observations_patient_downloaded, patients);
                    } else if (downloadedObs == 0) {
                        resultDescription = getString(R.string.info_observation_patient_not_downloaded);
                    } else {
                        resultDescription = getString(R.string.info_observations_patients_downloaded, downloadedObs, patients);
                    }
                    resultStatus = SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else {
                    wizardcompletedSuccessfully = false;
                    resultDescription = getString(R.string.error_observation_download);
                    resultStatus = SetupLogConstants.ACTION_FAILURE_STATUS_LOG;
                }
                downloadObservationsLog.setSetupActionResult(resultDescription);
                downloadObservationsLog.setSetupActionResultStatus(resultStatus);
                onQueryTaskFinish();
            }

            @Override
            protected void onBackgroundError(Exception e) {

            }
        }.execute();
    }

    private List<String> extractConceptsUuids() {
        List<String> conceptsUuids = new ArrayList<>();
        List<Object> objects = JsonUtils.readAsObjectList(setupConfigurationTemplate.getConfigJson(), "$['config']['concepts']");
        if (objects != null) {
            for (Object object : objects) {
                JSONObject cohort = (JSONObject) object;
                conceptsUuids.add((String) cohort.get("uuid"));
            }
        }
        return conceptsUuids;
    }

    private List<String> extractProvidersUuids() {
        List<String> providerUuids = new ArrayList<>();
        List<Object> objects = JsonUtils.readAsObjectList(setupConfigurationTemplate.getConfigJson(), "$['config']['providers']");
        if (objects != null) {
            for (Object object : objects) {
                JSONObject cohort = (JSONObject) object;
                providerUuids.add((String) cohort.get("uuid"));
            }
        }
        return providerUuids;
    }

    private List<String> extractLocationsUuids() {
        List<String> locationUuids = new ArrayList<>();
        List<Object> objects = JsonUtils.readAsObjectList(setupConfigurationTemplate.getConfigJson(), "$['config']['locations']");
        if (objects != null) {
            for (Object object : objects) {
                JSONObject cohort = (JSONObject) object;
                locationUuids.add((String) cohort.get("uuid"));
            }
        }
        return locationUuids;
    }

    private List<String> extractFormTemplatesUuids() {
        List<String> formsuuids = new ArrayList<>();
        List<Object> objects = JsonUtils.readAsObjectList(setupConfigurationTemplate.getConfigJson(), "$['config']['forms']");
        if (objects != null) {
            for (Object object : objects) {
                JSONObject cohort = (JSONObject) object;
                formsuuids.add((String) cohort.get("uuid"));
            }
        }
        return formsuuids;
    }

    private List<String> extractCohortsUuids() {
        List<String> cohortUuids = new ArrayList<>();
        List<Object> objects = JsonUtils.readAsObjectList(setupConfigurationTemplate.getConfigJson(), "$['config']['cohorts']");
        if (objects != null) {
            for (Object object : objects) {
                JSONObject cohort = (JSONObject) object;
                cohortUuids.add((String) cohort.get("uuid"));
            }
        }
        return cohortUuids;
    }

    public void checkIfCohortWithFilterByLocationExists() {
        boolean isCohortLocationBased = false;
        List<Object> objects = JsonUtils.readAsObjectList(setupConfigurationTemplate.getConfigJson(), "$['config']['cohorts']");
        if (objects != null) {
            for (Object object : objects) {
                JSONObject cohort = (JSONObject) object;
                if (cohort.get("isFilterByLocationEnabled") != null) {
                    if ((Boolean) cohort.get("isFilterByLocationEnabled")) {
                        isCohortLocationBased = true;
                    }
                }
            }
        }
        if (isCohortLocationBased) {
            showAlertDialog();
        } else {
            downloadProviders();
        }
    }

    private void showAlertDialog() {
        AlertDialog.Builder alertDialogBuider = new AlertDialog.Builder(this);
        alertDialogBuider.setTitle(R.string.title_default_encounter_location);
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice);
        final MuzimaApplication muzimaApplication = (MuzimaApplication) this.getApplication();
        final LocationController locationController = muzimaApplication.getLocationController();
        List<Location> locations = new ArrayList<>();
        try {
            locations = locationController.getAllLocations();
        } catch (LocationController.LocationLoadException e) {
            Log.e(getClass().getSimpleName(), e.getMessage());
        }

        for (Location location : locations) {
            arrayAdapter.add(location.getId() + "-" + location.getName());
        }

        alertDialogBuider.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String strName = arrayAdapter.getItem(which);
                DefaultEncounterLocationPreferenceService defaultEncounterLocationPreferenceService
                        = new DefaultEncounterLocationPreferenceService(muzimaApplication);
                String[] location = strName.split("-");
                defaultEncounterLocationPreferenceService.setDefaultEncounterLocationPreference(location[0]);
                Toast.makeText(GuidedConfigurationWizardActivity.this, strName, Toast.LENGTH_LONG).show();
                downloadProviders();
            }
        });
        AlertDialog alertDialog = alertDialogBuider.create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();
    }

    private void addSetupActionLog(SetupActionLogModel setupActionLogModel) {
        setupActionLogAdapter.add(setupActionLogModel);
    }

    private synchronized void incrementWizardStep() {
        mainProgressbar.setProgress(wizardLevel);
        wizardLevel++;
    }

    private synchronized void evaluateFinishStatus() {
        int TOTAL_WIZARD_STEPS = 10;
        if (wizardLevel == (TOTAL_WIZARD_STEPS)) {
            if (wizardcompletedSuccessfully) {
                initialSetupStatusTextView.setText(getString(R.string.info_setup_complete_success));
            } else {
                initialSetupStatusTextView.setText(getString(R.string.info_setup_complete_fail));
                initialSetupStatusTextView.setTextColor(Color.RED);
            }
            mainProgressbar.setProgress(10);
            finishSetupButton.setVisibility(View.VISIBLE);
            setupLogsContainer.setVisibility(View.VISIBLE);
            viewPager.setVisibility(View.VISIBLE);
            viewPagerLg.setVisibility(View.GONE);
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
    public void onQueryTaskCancelled() {
    }

    @Override
    public void onQueryTaskCancelled(Object errorDefinition) {
    }
}
