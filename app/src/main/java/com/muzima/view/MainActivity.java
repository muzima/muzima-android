/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.appcompat.app.ActionBar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.User;
import com.muzima.controller.CohortController;
import com.muzima.controller.FormController;
import com.muzima.controller.NotificationController;
import com.muzima.controller.PatientController;
import com.muzima.domain.Credentials;
import com.muzima.scheduler.RealTimeFormUploader;
import com.muzima.service.WizardFinishPreferenceService;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.MuzimaPreferenceUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.cohort.CohortActivity;
import com.muzima.view.forms.FormsActivity;
import com.muzima.view.notifications.NotificationsListActivity;
import com.muzima.view.patients.PatientsListActivity;

import org.apache.lucene.queryParser.ParseException;

import java.util.Locale;

import static com.muzima.utils.Constants.NotificationStatusConstants.NOTIFICATION_UNREAD;

public class MainActivity extends BroadcastListenerActivity {
    private static final String TAG = "MainActivity";
    private View mMainView;
    private BackgroundQueryTask mBackgroundQueryTask;
    private Credentials credentials;

    private final ThemeUtils themeUtils = new ThemeUtils();
    private final LanguageUtil languageUtil = new LanguageUtil();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeUtils.onCreate(this);
        languageUtil.onCreate(this);
        super.onCreate(savedInstanceState);
        credentials = new Credentials(this);
        mMainView = getLayoutInflater().inflate(R.layout.activity_dashboard, null);
        setContentView(mMainView);
        RealTimeFormUploader.getInstance().uploadAllCompletedForms(getApplicationContext(), false);
        setupActionbar();
        logEvent("VIEW_DASHBOARD", null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        themeUtils.onResume(this);
        languageUtil.onResume(this);
        showIncompleteWizardWarning();
        executeBackgroundTask();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void showIncompleteWizardWarning() {
        if (!new WizardFinishPreferenceService(this).isWizardFinished()) {
            if (checkIfDisclaimerIsAccepted()) {
                Toast.makeText(getApplicationContext(), getString(R.string.error_wizard_interrupted), Toast.LENGTH_LONG)
                        .show();
            }
        }
    }

    private boolean checkIfDisclaimerIsAccepted() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String disclaimerKey = getResources().getString(R.string.preference_disclaimer);
        return settings.getBoolean(disclaimerKey, false);
    }

    @Override
    protected void onStop() {
        if (mBackgroundQueryTask != null) {
            mBackgroundQueryTask.cancel(true);
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        String localePref = MuzimaPreferenceUtils.getSelectedUserLocalePreference(MainActivity.this);
        Boolean isUserPreferenceThemeLightMode = PreferenceManager.getDefaultSharedPreferences(MainActivity.this)
                .getBoolean(getResources().getString(R.string.preference_light_mode), false);
        String currentAppLocalePreference = PreferenceManager.getDefaultSharedPreferences(MainActivity.this)
                .getString(getResources().getString(R.string.preference_app_language),getResources().getString(R.string.language_english));
        Boolean isPreviousThemeLightMode = MuzimaPreferenceUtils.getIsLightModeThemeSelectedPreference(MainActivity.this);
        String previousAppLocalePreference = MuzimaPreferenceUtils.getAppLocalePreference(MainActivity.this);

        if (isUserPreferenceThemeLightMode.equals(isPreviousThemeLightMode) && Locale.getDefault().toString().equalsIgnoreCase(localePref)
                && currentAppLocalePreference.equalsIgnoreCase(previousAppLocalePreference)) {
            Log.i(TAG, "onDestroy:  this is not a theme change or local change logout user,onDestroy");
            ((MuzimaApplication) getApplication()).logOut();
        } else {
            Log.i(TAG, "onDestroy: application logout is NOT necessary, updating variables");
            boolean isLightThemeModeSelected = PreferenceManager.getDefaultSharedPreferences(MainActivity.this)
                    .getBoolean(getResources().getString(R.string.preference_light_mode), false);

            String preferredAppLocale = PreferenceManager.getDefaultSharedPreferences(MainActivity.this)
                    .getString(getResources().getString(R.string.preference_app_language),getString(R.string.language_english));

            MuzimaPreferenceUtils.setLightModeThemeSelectedPreference(MainActivity.this, isLightThemeModeSelected);
            MuzimaPreferenceUtils.setSelectedUserLocalePreference(MainActivity.this, Locale.getDefault().toString());
            MuzimaPreferenceUtils.setAppLocalePreference(MainActivity.this,preferredAppLocale);
        }

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        showAlertDialog();
    }

    private void showAlertDialog() {
        new AlertDialog.Builder(MainActivity.this)
                .setCancelable(true)
                .setIcon(themeUtils.getIconWarning(this))
                .setTitle(getResources().getString(R.string.title_logout_confirm))
                .setMessage(getResources().getString(R.string.warning_logout_confirm))
                .setPositiveButton(getString(R.string.general_yes), dialogYesClickListener())
                .setNegativeButton(getString(R.string.general_no), null)
                .create()
                .show();
    }

    private Dialog.OnClickListener dialogYesClickListener() {
        return new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ((MuzimaApplication) getApplication()).logOut();
                finish();
                System.exit(0);
            }
        };
    }

    /**
     * Called when the user clicks the Cohort area
     */
    public void cohortList(View view) {
        Intent intent = new Intent(this, CohortActivity.class);
        startActivity(intent);
    }

    /**
     * Called when the user clicks the Clients area or Search Clients Button
     */
    public void patientList(View view) {
        Intent intent = new Intent(this, PatientsListActivity.class);
        startActivity(intent);
    }

    /**
     * Called when the user clicks the Forms area
     */
    public void formsList(View view) {
        Intent intent = new Intent(this, FormsActivity.class);
        startActivity(intent);
    }

    /**
     * Called when the user clicks the Notifications area
     */
    public void notificationsList(View view) {
        Intent intent = new Intent(this, NotificationsListActivity.class);
        startActivity(intent);
    }

    class BackgroundQueryTask extends AsyncTask<Void, Void, HomeActivityMetadata> {

        @Override
        protected HomeActivityMetadata doInBackground(Void... voids) {
            MuzimaApplication muzimaApplication = (MuzimaApplication) getApplication();
            HomeActivityMetadata homeActivityMetadata = new HomeActivityMetadata();
            CohortController cohortController = muzimaApplication.getCohortController();
            PatientController patientController = muzimaApplication.getPatientController();
            FormController formController = muzimaApplication.getFormController();
            NotificationController notificationController = muzimaApplication.getNotificationController();
            try {
                homeActivityMetadata.totalCohorts = cohortController.countAllCohorts();
                homeActivityMetadata.syncedCohorts = cohortController.countSyncedCohorts();
                homeActivityMetadata.isCohortUpdateAvailable = cohortController.isUpdateAvailable();
                homeActivityMetadata.syncedPatients = patientController.countAllPatients();
                homeActivityMetadata.isFormTemplateUpdateAvailable = formController.isFormTemplateUpdatesAvailable();
                homeActivityMetadata.incompleteForms = formController.countAllIncompleteForms();
                homeActivityMetadata.completeAndUnsyncedForms = formController.countAllCompleteForms();

                // Notifications
                User authenticatedUser = ((MuzimaApplication) getApplicationContext()).getAuthenticatedUser();
                if (authenticatedUser != null) {
                    homeActivityMetadata.newNotifications = notificationController
                            .getAllNotificationsByReceiverCount(authenticatedUser.getPerson().getUuid(), NOTIFICATION_UNREAD);
                    homeActivityMetadata.totalNotifications = notificationController
                            .getAllNotificationsByReceiverCount(authenticatedUser.getPerson().getUuid(), null);
                } else {
                    homeActivityMetadata.newNotifications = 0;
                    homeActivityMetadata.totalNotifications = 0;
                }
            } catch (CohortController.CohortFetchException e) {
                Log.w(getClass().getSimpleName(), "CohortFetchException occurred while fetching metadata in MainActivityBackgroundTask", e);
            } catch (PatientController.PatientLoadException e) {
                Log.w(getClass().getSimpleName(), "PatientLoadException occurred while fetching metadata in MainActivityBackgroundTask", e);
            } catch (FormController.FormFetchException e) {
                Log.w(getClass().getSimpleName(), "FormFetchException occurred while fetching metadata in MainActivityBackgroundTask", e);
            } catch (NotificationController.NotificationFetchException e) {
                Log.w(getClass().getSimpleName(), "NotificationFetchException occurred while fetching metadata in MainActivityBackgroundTask", e);
            } catch (ParseException e) {
                Log.w(getClass().getSimpleName(), "ParseException occurred while fetching metadata in MainActivityBackgroundTask", e);
            }
            return homeActivityMetadata;
        }

        @Override
        protected void onPostExecute(HomeActivityMetadata homeActivityMetadata) {
            TextView cohortsDescriptionView = mMainView.findViewById(R.id.cohortDescription);
            cohortsDescriptionView.setText(getString(R.string.hint_dashboard_cohorts_description,
                    homeActivityMetadata.syncedCohorts, homeActivityMetadata.totalCohorts));

            ImageView cortUpdateAvailable = (ImageView) mMainView.findViewById(R.id.pendingUpdateImg);
            if (homeActivityMetadata.isCohortUpdateAvailable) {
                cortUpdateAvailable.setVisibility(View.VISIBLE);
            } else {
                cortUpdateAvailable.setVisibility(View.GONE);
            }

            TextView patientDescriptionView = mMainView.findViewById(R.id.patientDescription);
            patientDescriptionView.setText(getString(R.string.hint_dashboard_clients_description,
                    homeActivityMetadata.syncedPatients));

            ImageView pendingFormUpdateImg = (ImageView) mMainView.findViewById(R.id.pendingFormUpdateImg);
            if (homeActivityMetadata.isFormTemplateUpdateAvailable) {
                pendingFormUpdateImg.setVisibility(View.VISIBLE);
            } else {
                pendingFormUpdateImg.setVisibility(View.GONE);
            }

            TextView formsDescription = mMainView.findViewById(R.id.formDescription);
            formsDescription.setText(getString(R.string.hint_dashboard_forms_description,
                    homeActivityMetadata.incompleteForms, homeActivityMetadata.completeAndUnsyncedForms));

            TextView notificationsDescription = mMainView.findViewById(R.id.notificationDescription);
            notificationsDescription.setText(getString(R.string.hint_dashboard_notifications_description,
                    homeActivityMetadata.newNotifications, homeActivityMetadata.totalNotifications));

            TextView currentUser = findViewById(R.id.currentUser);
            currentUser.setText(getResources().getString(R.string.general_welcome) + " " + credentials.getUserName());
        }
    }

    private static class HomeActivityMetadata {
        int totalCohorts;
        int syncedCohorts;
        int syncedPatients;
        int incompleteForms;
        int completeAndUnsyncedForms;
        int newNotifications;
        int totalNotifications;
        boolean isCohortUpdateAvailable;
        boolean isFormTemplateUpdateAvailable;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard, menu);
        return true;
    }

    private void setupActionbar() {
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(false);
    }

    private void executeBackgroundTask() {
        mBackgroundQueryTask = new BackgroundQueryTask();
        mBackgroundQueryTask.execute();
    }
}
