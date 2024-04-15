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

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.muzima.api.model.APIName.DOWNLOAD_SETUP_CONFIGURATIONS;
import static com.muzima.util.Constants.ServerSettings.DEFAULT_ENCOUNTER_LOCATION_SETTING;

import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.adapters.setupconfiguration.GuidedSetupActionLogAdapter;
import com.muzima.adapters.setupconfiguration.GuidedSetupCardsViewPagerAdapter;
import com.muzima.api.model.AppUsageLogs;
import com.muzima.api.model.Form;
import com.muzima.api.model.LastSyncTime;
import com.muzima.api.model.Location;
import com.muzima.api.model.Media;
import com.muzima.api.model.MuzimaSetting;
import com.muzima.api.model.SetupConfigurationTemplate;
import com.muzima.api.model.User;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.controller.AppUsageLogsController;
import com.muzima.controller.FormController;
import com.muzima.controller.LocationController;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.controller.SetupConfigurationController;
import com.muzima.model.SetupActionLogModel;
import com.muzima.service.DefaultEncounterLocationPreferenceService;
import com.muzima.service.MuzimaSyncService;
import com.muzima.service.SntpService;
import com.muzima.service.WizardFinishPreferenceService;
import com.muzima.tasks.MuzimaAsyncTask;
import com.muzima.util.JsonUtils;

import com.muzima.utils.Constants;
import com.muzima.utils.MemoryUtil;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.MainDashboardActivity;
import com.muzima.view.main.HTCMainActivity;

import net.minidev.json.JSONObject;

import org.apache.lucene.queryParser.ParseException;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.muzima.utils.DeviceDetailsUtil;

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
    private boolean isOnlineOnlyModeEnabled;
    private String setupConfigTemplateUuid;
    private PowerManager.WakeLock wakeLock = null;
    private static final int EXTERNAL_STORAGE_MANAGEMENT = 9002;
    private List<Media> mediaList = new ArrayList<>();

    MuzimaSetting setting = null;

    public void onCreate(Bundle savedInstanceState) {
        ThemeUtils.getInstance().onCreate(this,false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guided_setup_wizard);
        initializeResources();
        startViewPagerAnimation();
        downloadConfigurationByUuid();
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

    private void downloadConfigurationByUuid() {
        new MuzimaAsyncTask<Void, Void, int[]>() {

            @Override
            protected void onPreExecute() {
                ((MuzimaApplication) getApplication()).cancelTimer();
                keepPhoneAwake(true);
                setupConfigTemplateUuid = getIntent().getStringExtra(SETUP_CONFIG_UUID_INTENT_KEY);
            }

            @Override
            protected int[] doInBackground(Void... voids) {
                return downloadSetupConfiguration(setupConfigTemplateUuid);
            }

            @Override
            protected void onPostExecute(int[] result) {
                Log.i(getClass().getSimpleName(), "Restarting timeout timer!");
                ((MuzimaApplication) getApplication()).restartTimer();
                if (result[0] != Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS) {
                    Toast.makeText(GuidedConfigurationWizardActivity.this,
                            getString(R.string.error_setup_configuration_template_download), Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        LastSyncTimeService lastSyncTimeService =
                                ((MuzimaApplication) getApplicationContext()).getMuzimaContext().getLastSyncTimeService();
                        SntpService sntpService = ((MuzimaApplication) getApplicationContext()).getSntpService();
                        LastSyncTime lastSyncTime = new LastSyncTime(DOWNLOAD_SETUP_CONFIGURATIONS, sntpService.getTimePerDeviceTimeZone());
                        lastSyncTimeService.saveLastSyncTime(lastSyncTime);
                    } catch (IOException e) {
                        Log.i(getClass().getSimpleName(), "Error setting Setup Configuration sync time.");
                    }
                    keepPhoneAwake(false);
                    initiateSetupConfiguration();
                }
            }

            @Override
            protected void onBackgroundError(Exception e) {

            }
        }.execute();
    }

    private int[] downloadSetupConfiguration(String setupConfigUuid) {
        MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
        return muzimaSyncService.downloadSetupConfigurationTemplate(setupConfigUuid);
    }

    private void keepPhoneAwake(boolean awakeState) {
        Log.d(getClass().getSimpleName(), "Launching wake state: " + awakeState);
        if (awakeState) {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, UUID.randomUUID().toString());
            wakeLock.acquire();
        } else {
            if (wakeLock != null) {
                wakeLock.release();
            }
        }
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


        //setupConfigurationTemplate.getConfigJson();
        finishSetupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new WizardFinishPreferenceService(GuidedConfigurationWizardActivity.this).finishWizard();
                Intent intent;
                if (isAtsSetup()) {
                    intent = new Intent(getApplicationContext(), HTCMainActivity.class);
                } else {
                    intent = new Intent(getApplicationContext(), MainDashboardActivity.class);
                }

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
                    resultStatus = Constants.SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else if (result[0] == Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS) {
                    if (result[1] == 0) {
                        resultDescription = getString(R.string.info_settings_not_download);
                    } else if (result[1] == 1) {
                        resultDescription = getString(R.string.info_setting_downloaded);
                    } else {
                        resultDescription = getString(R.string.info_settings_downloaded, result[1]);
                    }
                    resultStatus = Constants.SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else {
                    wizardcompletedSuccessfully = false;
                    resultDescription = getString(R.string.error_settings_download);
                    resultStatus = Constants.SetupLogConstants.ACTION_FAILURE_STATUS_LOG;
                }

                MuzimaSettingController muzimaSettingController = ((MuzimaApplication) getApplicationContext()).getMuzimaSettingController();
                try {
                    setting = muzimaSettingController.getSettingByProperty("Program.defintion");
                } catch (MuzimaSettingController.MuzimaSettingFetchException e) {
                    e.printStackTrace();
                }

                downloadSettingsLog.setSetupActionResult(resultDescription);
                downloadSettingsLog.setSetupActionResultStatus(resultStatus);
                onQueryTaskFinish();
                updateOnlineOnlyModeSettingValue();
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
                    resultStatus = Constants.SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else if (result[0] == Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS) {
                    if (result[1] == 1) {
                        resultDescription = getString(R.string.info_cohort_downloaded);
                    } else {
                        resultDescription = getString(R.string.info_cohorts_downloaded, result[1]);
                    }
                    resultStatus = Constants.SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else {
                    wizardcompletedSuccessfully = false;
                    resultDescription = getString(R.string.error_cohort_download);
                    resultStatus = Constants.SetupLogConstants.ACTION_FAILURE_STATUS_LOG;
                }
                downloadCohortsLog.setSetupActionResult(resultDescription);
                downloadCohortsLog.setSetupActionResultStatus(resultStatus);
                onQueryTaskFinish();

                if(isOnlineOnlyModeEnabled) {
                    downloadForms();
                } else {
                    downloadAndSavePatients();
                }
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
                    resultStatus = Constants.SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else if (result[0] == Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS) {
                    if (result[1] == 1 && result[2] == 1) {
                        resultDescription = getString(R.string.info_cohort_patient_download);
                    } else if (result[1] == 1) {
                        resultDescription = getString(R.string.info_cohorts_patient_download, result[2]);
                    } else if (result[2] == 1) {
                        resultDescription = getString(R.string.info_cohort_patients_download, result[1]);
                    } else {
                        resultDescription = getString(R.string.info_cohorts_patients_download, result[1], result[2]);
                    }
                    resultStatus = Constants.SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else {
                    wizardcompletedSuccessfully = false;
                    resultDescription = getString(R.string.error_patient_download);
                    resultStatus = Constants.SetupLogConstants.ACTION_FAILURE_STATUS_LOG;

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
                    resultStatus = Constants.SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else if (result[0] == Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS) {
                    if (result[1] == 1) {
                        resultDescription = getString(R.string.info_form_downloaded);
                    } else {
                        resultDescription = getString(R.string.info_forms_downloaded, result[1]);
                    }
                    resultStatus = Constants.SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else {
                    wizardcompletedSuccessfully = false;
                    resultDescription = getString(R.string.error_form_download);
                    resultStatus = Constants.SetupLogConstants.ACTION_FAILURE_STATUS_LOG;
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
                    resultStatus = Constants.SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else if (result[0] == Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS) {
                    if (result[1] == 1) {
                        resultDescription = getString(R.string.info_form_template_downloaded);
                    } else {
                        resultDescription = getString(R.string.info_form_templates_downloaded, result[1]);
                    }
                    resultStatus = Constants.SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else {
                    wizardcompletedSuccessfully = false;
                    resultDescription = getString(R.string.error_form_templates_download);
                    resultStatus = Constants.SetupLogConstants.ACTION_FAILURE_STATUS_LOG;
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
                LocationController locationController = ((MuzimaApplication) getApplicationContext()).getLocationController();
                List<String> uuids = extractLocationsUuids();
                if (!uuids.isEmpty()) {
                    MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
                    int[] result = muzimaSyncService.downloadLocations(uuids.toArray(new String[uuids.size()]));
                    try {
                        MuzimaSetting encounterLocationIdSetting = muzimaSettingController.getSettingByProperty(DEFAULT_ENCOUNTER_LOCATION_SETTING);
                        if(encounterLocationIdSetting != null) {
                            if(encounterLocationIdSetting.getValueString() != null) {
                                Location defaultEncounterLocation = locationController.getLocationById(Integer.valueOf(encounterLocationIdSetting.getValueString()));
                                if (defaultEncounterLocation != null) {
                                    Context context = getApplicationContext();
                                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                                    Resources resources = context.getResources();
                                    String key = resources.getString(R.string.preference_default_encounter_location);

                                    preferences.edit()
                                            .putString(key, String.valueOf(defaultEncounterLocation.getId()))
                                            .apply();
                                }
                            }
                        }
                    } catch (MuzimaSettingController.MuzimaSettingFetchException e) {
                        Log.e(getClass().getSimpleName(), "Encountered an error while fetching setting ",e);
                    } catch (LocationController.LocationLoadException e) {
                        Log.e(getClass().getSimpleName(), "Encountered an error while fetching location ",e);
                    }

                    return result;
                }
                return null;
            }

            @Override
            protected void onPostExecute(int[] result) {
                String resultDescription = null;
                String resultStatus = null;
                if (result == null) {
                    resultDescription = getString(R.string.info_location_not_downloaded);
                    resultStatus = Constants.SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else if (result[0] == Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS) {
                    if (result[1] == 1) {
                        resultDescription = getString(R.string.info_location_downloaded);
                    } else {
                        resultDescription = getString(R.string.info_locations_downloaded, result[1]);
                    }
                    resultStatus = Constants.SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else {
                    wizardcompletedSuccessfully = false;
                    resultDescription = getString(R.string.error_location_download);
                    resultStatus = Constants.SetupLogConstants.ACTION_FAILURE_STATUS_LOG;
                }
                if(!isOnlineOnlyModeEnabled){
                    checkIfCohortWithFilterByLocationExists();
                } else {
                    downloadProviders();
                }
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
                    int results[] =  muzimaSyncService.downloadProviders(uuids.toArray(new String[uuids.size()]));

                    MuzimaSettingController muzimaSettingController = ((MuzimaApplication) getApplicationContext()).getMuzimaSettingController();
                    boolean isDefaultLoggedInUserAsEncounterProvider = muzimaSettingController.isDefaultLoggedInUserAsEncounterProvider();

                    if(isDefaultLoggedInUserAsEncounterProvider) {
                        Context context = getApplicationContext();
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                        Resources resources = context.getResources();
                        String key = resources.getString(R.string.preference_encounter_provider_key);

                        preferences.edit()
                                .putBoolean(key,isDefaultLoggedInUserAsEncounterProvider)
                                .apply();
                    }
                    return results;
                }
                return null;
            }

            @Override
            protected void onPostExecute(int[] result) {
                String resultDescription = null;
                String resultStatus = null;
                if (result == null) {
                    resultDescription = getString(R.string.info_provider_not_downloaded);
                    resultStatus = Constants.SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else if (result[0] == Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS) {
                    if (result[1] == 1) {
                        resultDescription = getString(R.string.info_provider_downloaded);
                    } else {
                        resultDescription = getString(R.string.info_providers_downloaded, result[1]);
                    }
                    resultStatus = Constants.SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else {
                    wizardcompletedSuccessfully = false;
                    resultDescription = getString(R.string.error_provider_download);
                    resultStatus = Constants.SetupLogConstants.ACTION_FAILURE_STATUS_LOG;
                }
                downloadProvidersLog.setSetupActionResult(resultDescription);
                downloadProvidersLog.setSetupActionResultStatus(resultStatus);
                onQueryTaskFinish();
                // analisar este ponto
                if ((setting != null && setting.getValueString() != null) && setting.getValueString().equals("ATS")) {
                    downloadHtcPersons();
                } else {
                    downloadCohorts();
                }

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
                    resultStatus = Constants.SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else if (result[0] == Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS) {
                    if (result[1] == 1) {
                        resultDescription = getString(R.string.info_concept_downloaded);
                    } else {
                        resultDescription = getString(R.string.info_concepts_downloaded, result[1]);
                    }
                    resultStatus = Constants.SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else {
                    wizardcompletedSuccessfully = false;
                    resultDescription = getString(R.string.error_concept_download);
                    resultStatus = Constants.SetupLogConstants.ACTION_FAILURE_STATUS_LOG;
                }
                downloadConceptsLog.setSetupActionResult(resultDescription);
                downloadConceptsLog.setSetupActionResultStatus(resultStatus);
                onQueryTaskFinish();
                if(!isOnlineOnlyModeEnabled) {
                    downloadObservations();
                } else {
                    downloadReportDatasets();
                }
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

                    String[] cohortUuidsArray = uuids.toArray(new String[uuids.size()]);
                    int[] resultForPatientObs = muzimaSyncService.downloadObservationsForPatientsByCohortUUIDs(
                            cohortUuidsArray, false);
                    if(((MuzimaApplication) getApplicationContext()).getMuzimaSettingController().isRelationshipEnabled()) {
                        muzimaSyncService.downloadObservationsForAllPersons(false);
                        if(((MuzimaApplication) getApplicationContext()).getMuzimaSettingController().isPatientTagGenerationEnabled())
                            muzimaSyncService.updatePersonTagsByCohortUuids(cohortUuidsArray);
                    }
                    return resultForPatientObs;

                }
                return null;
            }

            @Override
            protected void onPostExecute(int[] result) {
                String resultDescription = null;
                String resultStatus = null;
                if (result == null) {
                    resultDescription = getString(R.string.info_observation_patient_not_downloaded);
                    resultStatus = Constants.SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else if (result[0] == Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS) {
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
                    resultStatus = Constants.SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else {
                    wizardcompletedSuccessfully = false;
                    resultDescription = getString(R.string.error_observation_download);
                    resultStatus = Constants.SetupLogConstants.ACTION_FAILURE_STATUS_LOG;
                }
                downloadObservationsLog.setSetupActionResult(resultDescription);
                downloadObservationsLog.setSetupActionResultStatus(resultStatus);
                onQueryTaskFinish();
                downloadReportDatasets();
            }

            @Override
            protected void onBackgroundError(Exception e) {

            }
        }.execute();
    }

    private void downloadReportDatasets() {
        final SetupActionLogModel downloadReportDatasetLog = new SetupActionLogModel();
        addSetupActionLog(downloadReportDatasetLog);
        new MuzimaAsyncTask<Void, Void, int[]>() {
            @Override
            protected void onPreExecute() {
                downloadReportDatasetLog.setSetupAction(getString(R.string.info_report_dataset_download_in_progress));
                onQueryTaskStarted();
            }

            @Override
            protected int[] doInBackground(Void... voids) {
                List<Integer> datasetDefinitionIds = extractDatasetDefinitionIds();
                MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
                int[] resultForReportDataset = muzimaSyncService.downloadReportDatasets(datasetDefinitionIds, false);
                return resultForReportDataset;
            }

            @Override
            protected void onPostExecute(int[] result) {
                String resultDescription = null;
                String resultStatus = null;
                if (result == null) {
                    resultDescription = getString(R.string.info_report_datasets_not_downloaded);
                    resultStatus = Constants.SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else if (result[0] == Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS) {
                    int downloadedReportDatasets = result[1];
                    if (downloadedReportDatasets == 0) {
                        resultDescription = getString(R.string.info_report_datasets_not_downloaded);
                    } else {
                        resultDescription = getString(R.string.info_report_dataset_downloaded, downloadedReportDatasets);
                    }
                    resultStatus = Constants.SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else {
                    wizardcompletedSuccessfully = false;
                    resultDescription = getString(R.string.error_report_dataset_download);
                    resultStatus = Constants.SetupLogConstants.ACTION_FAILURE_STATUS_LOG;
                }

                downloadReportDatasetLog.setSetupActionResult(resultDescription);
                downloadReportDatasetLog.setSetupActionResultStatus(resultStatus);

                onQueryTaskFinish();
                downloadMediaCategories();
            }

            @Override
            protected void onBackgroundError(Exception e) {

            }
        }.execute();
    }


    private void downloadMediaCategories() {
        final SetupActionLogModel downloadMediaCategoryLog = new SetupActionLogModel();
        addSetupActionLog(downloadMediaCategoryLog);
        new MuzimaAsyncTask<Void, Void, int[]>() {
            @Override
            protected void onPreExecute() {
                downloadMediaCategoryLog.setSetupAction(getString(R.string.info_media_category_download_in_progress));
                onQueryTaskStarted();
            }

            @Override
            protected int[] doInBackground(Void... voids) {
                MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
                List<String> mediaCategoryUuids = extractMediaCategoryUuids();
                int[] resultForMediaCategory= muzimaSyncService.downloadMediaCategories(mediaCategoryUuids);
                return resultForMediaCategory;
            }

            @Override
            protected void onPostExecute(int[] result) {
                String resultDescription = null;
                String resultStatus = null;
                if (result == null) {
                    resultDescription = getString(R.string.info_media_categories_not_downloaded);
                    resultStatus = Constants.SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else if (result[0] == Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS) {
                    int downloadedCategories = result[1];
                    if (downloadedCategories == 0) {
                        resultDescription = getString(R.string.info_media_categories_not_downloaded);
                    } else {
                        resultDescription = getString(R.string.info_media_category_downloaded, downloadedCategories);
                    }
                    resultStatus = Constants.SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else {
                    wizardcompletedSuccessfully = false;
                    resultDescription = getString(R.string.error_media_category_download);
                    resultStatus = Constants.SetupLogConstants.ACTION_FAILURE_STATUS_LOG;
                }

                downloadMediaCategoryLog.setSetupActionResult(resultDescription);
                downloadMediaCategoryLog.setSetupActionResultStatus(resultStatus);

                onQueryTaskFinish();
                if(result[1]>0) {
                    if (checkPermissionForStoragePermission()) {
                        downloadMedia();
                    } else {
                        requestPermission();
                    }
                }else{
                    downloadMedia();
                }
            }

            @Override
            protected void onBackgroundError(Exception e) {

            }
        }.execute();
    }

    private void downloadMedia() {
        final SetupActionLogModel downloadMediaLog = new SetupActionLogModel();
        addSetupActionLog(downloadMediaLog);
        new MuzimaAsyncTask<Void, Void, int[]>() {
            @Override
            protected void onPreExecute() {
                downloadMediaLog.setSetupAction(getString(R.string.info_media_download_in_progress));
                onQueryTaskStarted();
            }

            @Override
            protected int[] doInBackground(Void... voids) {
                MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
                List<String> mediaUuids = extractMediaCategoryUuids();

                mediaList = muzimaSyncService.downloadMedia(mediaUuids, false);

                int[] resultForMedia = null;
                long totalFileSize = MemoryUtil.getTotalMediaFileSize(mediaList);
                long availableSpace = MemoryUtil.getAvailableInternalMemorySize();
                if(availableSpace>totalFileSize) {
                    if(mediaList.size()>0){
                        resultForMedia = muzimaSyncService.saveMedia(mediaList);
                        for (Media media : mediaList) {
                            downloadFile(media);
                        }
                    }
                }else {
                    String loggedInUser = ((MuzimaApplication) getApplicationContext()).getAuthenticatedUserId();
                    String pseudoDeviceId = DeviceDetailsUtil.generatePseudoDeviceId();
                    AppUsageLogsController appUsageLogsController = ((MuzimaApplication) getApplicationContext()).getAppUsageLogsController();
                    AppUsageLogs noEnoughSpaceLog = null;
                    try {
                        String availableMemory = MemoryUtil.getFormattedMemory(MemoryUtil.getAvailableInternalMemorySize());
                        noEnoughSpaceLog = appUsageLogsController.getAppUsageLogByKey(com.muzima.util.Constants.AppUsageLogs.NO_ENOUGH_SPACE_DEVICES);
                        String requiredMemory = MemoryUtil.getFormattedMemory(MemoryUtil.getAvailableInternalMemorySize());
                        if (noEnoughSpaceLog != null) {
                            noEnoughSpaceLog.setLogvalue("Required: " + requiredMemory + " Available: " + availableMemory);
                            noEnoughSpaceLog.setUpdateDatetime(new Date());
                            noEnoughSpaceLog.setUserName(loggedInUser);
                            noEnoughSpaceLog.setDeviceId(pseudoDeviceId);
                            noEnoughSpaceLog.setLogSynced(false);
                            appUsageLogsController.saveOrUpdateAppUsageLog(noEnoughSpaceLog);
                        } else {
                            AppUsageLogs newNoEnoughSpaceLog = new AppUsageLogs();
                            newNoEnoughSpaceLog.setUuid(UUID.randomUUID().toString());
                            newNoEnoughSpaceLog.setLogKey(com.muzima.util.Constants.AppUsageLogs.NO_ENOUGH_SPACE_DEVICES);
                            newNoEnoughSpaceLog.setLogvalue("Required: " + requiredMemory + " Available: " + availableMemory);
                            newNoEnoughSpaceLog.setUpdateDatetime(new Date());
                            newNoEnoughSpaceLog.setUserName(loggedInUser);
                            newNoEnoughSpaceLog.setDeviceId(pseudoDeviceId);
                            newNoEnoughSpaceLog.setLogSynced(false);
                            appUsageLogsController.saveOrUpdateAppUsageLog(newNoEnoughSpaceLog);
                        }
                        MemoryUtil.showAlertDialog(availableSpace, totalFileSize, GuidedConfigurationWizardActivity.this);
                    } catch (IOException e) {
                        Log.e(getClass().getSimpleName(),"Encountered IOException ",e);
                    } catch (ParseException e) {
                        Log.e(getClass().getSimpleName(),"Encountered parse exception ",e);
                    }
                }
                return resultForMedia;
            }

            @Override
            protected void onPostExecute(int[] result) {
                String resultDescription = null;
                String resultStatus = null;
                if (result == null) {
                    resultDescription = getString(R.string.info_media_not_downloaded);
                    resultStatus = Constants.SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else if (result[0] == Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS) {
                    int downloadedCategories = result[1];
                    if (downloadedCategories == 0) {
                        resultDescription = getString(R.string.info_media_not_downloaded);
                    } else {
                        resultDescription = getString(R.string.info_media_downloaded, downloadedCategories);
                    }
                    resultStatus = Constants.SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else {
                    wizardcompletedSuccessfully = false;
                    resultDescription = getString(R.string.error_media_download);
                    resultStatus = Constants.SetupLogConstants.ACTION_FAILURE_STATUS_LOG;
                }

                downloadMediaLog.setSetupActionResult(resultDescription);
                downloadMediaLog.setSetupActionResultStatus(resultStatus);

                onQueryTaskFinish();
                downloadDerivedConcepts();
            }

            @Override
            protected void onBackgroundError(Exception e) {

            }
        }.execute();
    }


    public void downloadDerivedConcepts(){
        final SetupActionLogModel downloadDerivedConceptsLog = new SetupActionLogModel();
        addSetupActionLog(downloadDerivedConceptsLog);
        new MuzimaAsyncTask<Void, Void, int[]>() {
            @Override
            protected void onPreExecute() {
                downloadDerivedConceptsLog.setSetupAction(getString(R.string.info_derived_concept_download));
                onQueryTaskStarted();
            }

            @Override
            protected int[] doInBackground(Void... voids) {
                List<String> uuids = extractDerivedConceptsUuids();
                if (!uuids.isEmpty()) {
                    MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
                    return muzimaSyncService.downloadDerivedConcepts(uuids.toArray(new String[uuids.size()]));

                }
                return null;
            }

            @Override
            protected void onPostExecute(int[] result) {
                String resultDescription = null;
                String resultStatus = null;
                if (result == null) {
                    resultDescription = getString(R.string.info_derived_concept_not_downloaded);
                    resultStatus = Constants.SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else if (result[0] == Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS) {
                    if (result[1] == 1) {
                        resultDescription = getString(R.string.info_derived_concept_downloaded);
                    } else {
                        resultDescription = getString(R.string.info_derived_concepts_downloaded, result[1]);
                    }
                    resultStatus = Constants.SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else {
                    wizardcompletedSuccessfully = false;
                    resultDescription = getString(R.string.error_derived_concept_download);
                    resultStatus = Constants.SetupLogConstants.ACTION_FAILURE_STATUS_LOG;
                }
                downloadDerivedConceptsLog.setSetupActionResult(resultDescription);
                downloadDerivedConceptsLog.setSetupActionResultStatus(resultStatus);
                onQueryTaskFinish();
                if(!isOnlineOnlyModeEnabled) {
                    downloadDerivedObservations();
                }
            }

            @Override
            protected void onBackgroundError(Exception e) {

            }
        }.execute();
    }

    public void downloadDerivedObservations(){
        final SetupActionLogModel downloadDerivedObservationsLog = new SetupActionLogModel();
        addSetupActionLog(downloadDerivedObservationsLog);
        new MuzimaAsyncTask<Void, Void, int[]>() {
            @Override
            protected void onPreExecute() {
                downloadDerivedObservationsLog.setSetupAction(getString(R.string.info_derived_observation_download));
                onQueryTaskStarted();
            }

            @Override
            protected int[] doInBackground(Void... voids) {
                List<String> uuids = extractCohortsUuids();
                if (!uuids.isEmpty()) {
                    MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();

                    String[] cohortUuidsArray = uuids.toArray(new String[uuids.size()]);
                    int[] resultForPatientObs = muzimaSyncService.downloadDerivedObservationsForPatientsByCohortUUIDs(
                            cohortUuidsArray, false);
                    if(((MuzimaApplication) getApplicationContext()).getMuzimaSettingController().isRelationshipEnabled()) {
                        muzimaSyncService.downloadDerivedObservationsForAllPersons(false);
                    }
                    return resultForPatientObs;

                }
                return null;
            }

            @Override
            protected void onPostExecute(int[] result) {
                String resultDescription = null;
                String resultStatus = null;
                if (result == null) {
                    resultDescription = getString(R.string.info_derived_observation_patient_not_downloaded);
                    resultStatus = Constants.SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else if (result[0] == Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS) {
                    int downloadedObs = result[1];
                    int patients = result[3];
                    if (downloadedObs == 1 && patients == 1) {
                        resultDescription = getString(R.string.info_derived_observation_patient_downloaded);
                    } else if (downloadedObs == 1) {
                        resultDescription = getString(R.string.info_derived_observation_patients_downloaded, patients);
                    } else if (patients == 1) {
                        resultDescription = getString(R.string.info_derived_observations_patient_downloaded, patients);
                    } else if (downloadedObs == 0) {
                        resultDescription = getString(R.string.info_derived_observation_patient_not_downloaded);
                    } else {
                        resultDescription = getString(R.string.info_derived_observations_patients_downloaded, downloadedObs, patients);
                    }
                    resultStatus = Constants.SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else {
                    wizardcompletedSuccessfully = false;
                    resultDescription = getString(R.string.error_derived_observation_download);
                    resultStatus = Constants.SetupLogConstants.ACTION_FAILURE_STATUS_LOG;
                }
                downloadDerivedObservationsLog.setSetupActionResult(resultDescription);
                downloadDerivedObservationsLog.setSetupActionResultStatus(resultStatus);
                onQueryTaskFinish();
            }

            @Override
            protected void onBackgroundError(Exception e) {

            }
        }.execute();
    }

    public void downloadHtcPersons(){
        final SetupActionLogModel downloadHtcPersonsLog = new SetupActionLogModel();
        addSetupActionLog(downloadHtcPersonsLog);
        new MuzimaAsyncTask<Void, Void, int[]>() {
            @Override
            protected void onPreExecute() {
                downloadHtcPersonsLog.setSetupAction(getString(R.string.info_htc_persons_download));
                onQueryTaskStarted();
            }

            @Override
            protected int[] doInBackground(Void... voids) {
                User authenticatedUser = ((MuzimaApplication) getApplication()).getAuthenticatedUser();

                if (authenticatedUser.getUuid() != null) {
                    MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();

                    int[] resultForHtcPersons = muzimaSyncService.downloadHtcPersons(authenticatedUser.getUuid());

                    return resultForHtcPersons;

                }
                return null;
            }

            @Override
            protected void onPostExecute(int[] result) {
                String resultDescription = null;
                String resultStatus = null;
                if (result == null) {
                    resultDescription = getString(R.string.info_htc_person_not_downloaded);
                    resultStatus = Constants.SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else if (result[0] == Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS) {
                    if (result[1] == 1) {
                        resultDescription = getString(R.string.info_htc_person_downloaded);
                    } else {
                        resultDescription = getString(R.string.info_htc_persons_downloaded, result[3]);
                    }
                    resultStatus = Constants.SetupLogConstants.ACTION_SUCCESS_STATUS_LOG;
                } else  {
                    wizardcompletedSuccessfully = false;
                    resultDescription = getString(R.string.error_htc_persons_download);
                    resultStatus = Constants.SetupLogConstants.ACTION_FAILURE_STATUS_LOG;
                }
                downloadHtcPersonsLog.setSetupActionResult(resultDescription);
                downloadHtcPersonsLog.setSetupActionResultStatus(resultStatus);
                onQueryTaskFinish();
            }

            @Override
            protected void onBackgroundError(Exception e) {

            }
        }.execute();
    }
    public void downloadFile(Media media){
        try {
            //Delete file if exists
            String mimeType = media.getMimeType();
            String PATH = Objects.requireNonNull(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)).getAbsolutePath();
            File file = new File(PATH + "/"+media.getName()+"."+mimeType.substring(mimeType.lastIndexOf("/") + 1));
            String mediaName = media.getName()+"."+mimeType.substring(mimeType.lastIndexOf("/") + 1);
            if(mimeType.substring(mimeType.lastIndexOf("/") + 1).equals("vnd.ms-excel")){
                file = new File(PATH + "/"+media.getName()+".xls");
                mediaName = media.getName()+".xls";
            }else if(mimeType.substring(mimeType.lastIndexOf("/") + 1).equals("vnd.openxmlformats-officedocument.spreadsheetml.sheet")){
                file = new File(PATH + "/"+media.getName()+".xlsx");
                mediaName = media.getName()+".xlsx";
            }else if(mimeType.substring(mimeType.lastIndexOf("/") + 1).equals("msword")){
                file = new File(PATH + "/"+media.getName()+".doc");
                mediaName = media.getName()+".doc";
            }else if(mimeType.substring(mimeType.lastIndexOf("/") + 1).equals("vnd.openxmlformats-officedocument.wordprocessingml.document")){
                file = new File(PATH + "/"+media.getName()+".docx");
                mediaName = media.getName()+".docx";
            }else if(mimeType.substring(mimeType.lastIndexOf("/") + 1).equals("vnd.ms-powerpoint")){
                file = new File(PATH + "/"+media.getName()+".ppt");
                mediaName = media.getName()+".ppt";
            }else if(mimeType.substring(mimeType.lastIndexOf("/") + 1).equals("vnd.openxmlformats-officedocument.presentationml.presentation")){
                file = new File(PATH + "/"+media.getName()+".pptx");
                mediaName = media.getName()+".pptx";
            }

            if(file.exists()) {
                file.delete();
                getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
            }

            if(!media.isRetired()) {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(media.getUrl() + ""));
                request.setTitle(media.getName());
                request.setDescription(media.getDescription());
                request.allowScanningByMediaScanner();
                request.setAllowedOverMetered(true);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, mediaName);
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                dm.enqueue(request);
            }
        }catch (Exception e) {
            Log.e(getClass().getSimpleName(), "Error ", e);
        }
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

    private List<String> extractDerivedConceptsUuids() {
        List<String> derivedConceptsUuids = new ArrayList<>();
        List<Object> objects = JsonUtils.readAsObjectList(setupConfigurationTemplate.getConfigJson(), "$['config']['derivedConcepts']");
        if (objects != null) {
            for (Object object : objects) {
                JSONObject derivedConcept = (JSONObject) object;
                derivedConceptsUuids.add((String) derivedConcept.get("uuid"));
            }
        }
        return derivedConceptsUuids;
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
        int TOTAL_WIZARD_STEPS = isOnlineOnlyModeEnabled ? 11 : 14;
        if (isAtsSetup()) {
            TOTAL_WIZARD_STEPS = 4;
        }
        if (wizardLevel == (TOTAL_WIZARD_STEPS)) {

            String loggedInUser = ((MuzimaApplication) getApplicationContext()).getAuthenticatedUserId();
            String pseudoDeviceId = DeviceDetailsUtil.generatePseudoDeviceId();
            AppUsageLogsController appUsageLogsController = ((MuzimaApplication) getApplicationContext()).getAppUsageLogsController();
            SimpleDateFormat simpleDateTimezoneFormat = new SimpleDateFormat(Constants.STANDARD_DATE_TIMEZONE_FORMAT);
            try {
                AppUsageLogs setupConfig = new AppUsageLogs();
                setupConfig.setUuid(UUID.randomUUID().toString());
                setupConfig.setLogKey(com.muzima.util.Constants.AppUsageLogs.SET_UP_CONFIG_UUID);
                setupConfig.setLogvalue(setupConfigTemplateUuid);
                setupConfig.setUpdateDatetime(new Date());
                setupConfig.setUserName(loggedInUser);
                setupConfig.setDeviceId(pseudoDeviceId);
                setupConfig.setLogSynced(false);
                appUsageLogsController.saveOrUpdateAppUsageLog(setupConfig);

                AppUsageLogs setUpTime = new AppUsageLogs();
                setUpTime.setUuid(UUID.randomUUID().toString());
                setUpTime.setLogKey(com.muzima.util.Constants.AppUsageLogs.SETUP_TIME);
                setUpTime.setLogvalue(simpleDateTimezoneFormat.format(new Date()));
                setUpTime.setUpdateDatetime(new Date());
                setUpTime.setUserName(loggedInUser);
                setUpTime.setDeviceId(pseudoDeviceId);
                setUpTime.setLogSynced(false);
                appUsageLogsController.saveOrUpdateAppUsageLog(setUpTime);

                AppUsageLogs availableSpace = new AppUsageLogs();
                availableSpace.setUuid(UUID.randomUUID().toString());
                availableSpace.setLogKey(com.muzima.util.Constants.AppUsageLogs.AVAILABLE_INTERNAL_SPACE);
                availableSpace.setLogvalue(MemoryUtil.getFormattedMemory(MemoryUtil.getAvailableInternalMemorySize()));
                availableSpace.setUpdateDatetime(new Date());
                availableSpace.setUserName(loggedInUser);
                availableSpace.setDeviceId(pseudoDeviceId);
                availableSpace.setLogSynced(false);
                appUsageLogsController.saveOrUpdateAppUsageLog(availableSpace);

            } catch (IOException e) {
                Log.e(getClass().getSimpleName(),"Encountered an exception",e);
            } catch (ParseException e) {
                Log.e(getClass().getSimpleName(),"Encountered an exception",e);
            }

            if (wizardcompletedSuccessfully) {

                initialSetupStatusTextView.setText(getString(R.string.info_setup_complete_success));
            } else {
                initialSetupStatusTextView.setText(getString(R.string.info_setup_complete_fail));
                initialSetupStatusTextView.setTextColor(Color.RED);
            }
            mainProgressbar.setProgress(TOTAL_WIZARD_STEPS);
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

    private void updateOnlineOnlyModeSettingValue(){
        isOnlineOnlyModeEnabled = ((MuzimaApplication) getApplicationContext()).getMuzimaSettingController().isOnlineOnlyModeEnabled();

        if (isAtsSetup()) {
            mainProgressbar.setMax(4);
        } else {
            mainProgressbar.setMax(isOnlineOnlyModeEnabled ? 11 : 14);
        }
    }

    private List<Integer> extractDatasetDefinitionIds() {
        List<Integer> datasetIds = new ArrayList<>();
        List<Object> objects = JsonUtils.readAsObjectList(setupConfigurationTemplate.getConfigJson(), "$['config']['datasets']");
        if (objects != null) {
            for (Object object : objects) {
                JSONObject dataset = (JSONObject) object;
                datasetIds.add((Integer) dataset.get("id"));
            }
        }
        return datasetIds;
    }

    private boolean isAtsSetup() {
        List<Object> objects = JsonUtils.readAsObjectList(setupConfigurationTemplate.getConfigJson(), "$['config']['settings']");
        MuzimaSetting muzimaSetting = new MuzimaSetting();

        if (objects != null) {
            for (Object object : objects) {
                JSONObject dataset = (JSONObject) object;
                if (dataset.get("value").equals("ATS")) {
                    muzimaSetting.setName((String) dataset.get("name"));
                    muzimaSetting.setValueString((String) dataset.get("value"));
                    muzimaSetting.setUuid((String) dataset.get("uuid"));
                }
            }
        }
        return muzimaSetting.getValueString() != null && muzimaSetting.getValueString().equals("ATS");
    }
    private List<String> extractMediaCategoryUuids() {
        List<String> mediaCategoryUuids = new ArrayList<>();
        List<Object> objects = JsonUtils.readAsObjectList(setupConfigurationTemplate.getConfigJson(), "$['config']['mediaCategories']");
        if (objects != null) {
            for (Object object : objects) {
                JSONObject mediaCategory = (JSONObject) object;
                mediaCategoryUuids.add((String)mediaCategory.get("uuid"));
            }
        }
        return mediaCategoryUuids;
    }

    private boolean checkPermissionForStoragePermission() {
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.R){
            return Environment.isExternalStorageManager();
        } else {
            int result = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
            int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), READ_EXTERNAL_STORAGE);
            boolean granted = result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
            return granted;
        }
    }

    private void requestPermission() {
        Log.e(getClass().getSimpleName(),"Permissions requesting");
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.R){
            try{
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s", new Object[]{getApplicationContext().getPackageName()})));
                startActivityForResult(intent, EXTERNAL_STORAGE_MANAGEMENT);
            }catch(Exception e){
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, EXTERNAL_STORAGE_MANAGEMENT);
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE}, 200);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == EXTERNAL_STORAGE_MANAGEMENT) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    downloadMedia();
                } else {
                    Toast.makeText(this, "Allow permission for storage access!", Toast.LENGTH_SHORT).show();
                }
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 200) {
            if (grantResults.length > 0) {
                downloadMedia();
            }
        }
    }
}
