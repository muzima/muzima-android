/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
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
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
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
import com.muzima.view.cohort.CohortActivity;
import com.muzima.view.forms.FormsActivity;
import com.muzima.view.forms.RegistrationFormsActivity;
import com.muzima.view.notifications.NotificationsListActivity;
import com.muzima.view.patients.PatientsListActivity;
import org.apache.lucene.queryParser.ParseException;

import static com.muzima.utils.Constants.NotificationStatusConstants.NOTIFICATION_UNREAD;

public class MainActivity extends BroadcastListenerActivity {
    private static final String TAG = "MainActivity";
    private View mMainView;
    private BackgroundQueryTask mBackgroundQueryTask;
    private Credentials credentials;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        credentials = new Credentials(this);
        mMainView = getLayoutInflater().inflate(R.layout.activity_dashboard, null);
        setContentView(mMainView);
        setTitle(R.string.homepage);
        RealTimeFormUploader.getInstance().uploadAllCompletedForms(getApplicationContext());
        setupActionbar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        showIncompleteWizardWarning();
        executeBackgroundTask();
    }

    private void showIncompleteWizardWarning() {
        if (!new WizardFinishPreferenceService(this).isWizardFinished() ) {
            if(checkIfDisclaimerIsAccepted()){
                Toast
                        .makeText(getApplicationContext(), getString(R.string.rerun_wizard_message), Toast.LENGTH_LONG)
                        .show();
            }

        }
    }

    private boolean checkIfDisclaimerIsAccepted() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String disclaimerKey = getResources().getString(R.string.preference_disclaimer);
        boolean disclaimerAccepted = settings.getBoolean(disclaimerKey, false);
        return disclaimerAccepted;
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
        ((MuzimaApplication) getApplication()).logOut();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        showAlertDialog();
    }

    private void showAlertDialog() {
        new AlertDialog.Builder(MainActivity.this)
                .setCancelable(true)
                .setIcon(getResources().getDrawable(R.drawable.ic_warning))
                .setTitle(getResources().getString(R.string.confirm))
                .setMessage(getResources().getString(R.string.exit_app_message))
                .setPositiveButton(getString(R.string.yes_button_label), dialogYesClickListener())
                .setNegativeButton(getString(R.string.no_button_label), null)
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

    /**
     * Called when the user clicks the Register Client Button
     */
    public void registerClient(View view) {
        Intent intent = new Intent(this, RegistrationFormsActivity.class);
        startActivity(intent);
    }

    public class BackgroundQueryTask extends AsyncTask<Void, Void, HomeActivityMetadata> {

        @Override
        protected HomeActivityMetadata doInBackground(Void... voids) {
            MuzimaApplication muzimaApplication = (MuzimaApplication) getApplication();
            HomeActivityMetadata homeActivityMetadata = new HomeActivityMetadata();
            CohortController cohortController = muzimaApplication.getCohortController();
            PatientController patientController = muzimaApplication.getPatientController();
            FormController formController = muzimaApplication.getFormController();
            NotificationController notificationController = muzimaApplication.getNotificationController();
            try {
                homeActivityMetadata.totalCohorts = cohortController.getTotalCohortsCount();
                homeActivityMetadata.syncedCohorts = cohortController.getSyncedCohortsCount();
                homeActivityMetadata.syncedPatients = patientController.getTotalPatientsCount();
                homeActivityMetadata.incompleteForms = formController.getAllIncompleteFormsSize();
                homeActivityMetadata.completeAndUnsyncedForms = formController.getAllCompleteFormsSize();

                // Notifications
                User authenticatedUser = ((MuzimaApplication) getApplicationContext()).getAuthenticatedUser();
                if (authenticatedUser != null) {
                    homeActivityMetadata.newNotifications = notificationController.getAllNotificationsByReceiverCount(authenticatedUser.getPerson().getUuid(), NOTIFICATION_UNREAD);
                    homeActivityMetadata.totalNotifications = notificationController.getAllNotificationsByReceiverCount(authenticatedUser.getPerson().getUuid(), null);
                } else {
                    homeActivityMetadata.newNotifications = 0;
                    homeActivityMetadata.totalNotifications = 0;
                }
            } catch (CohortController.CohortFetchException e) {
                Log.w(TAG, "CohortFetchException occurred while fetching metadata in MainActivityBackgroundTask", e);
            } catch (PatientController.PatientLoadException e) {
                Log.w(TAG, "PatientLoadException occurred while fetching metadata in MainActivityBackgroundTask", e);
            } catch (FormController.FormFetchException e) {
                Log.w(TAG, "FormFetchException occurred while fetching metadata in MainActivityBackgroundTask", e);
            } catch (NotificationController.NotificationFetchException e) {
                Log.w(TAG, "NotificationFetchException occurred while fetching metadata in MainActivityBackgroundTask", e);
            } catch (ParseException e) {
                Log.w(TAG, "ParseException occurred while fetching metadata in MainActivityBackgroundTask", e);
            }
            return homeActivityMetadata;
        }

        @Override
        protected void onPostExecute(HomeActivityMetadata homeActivityMetadata) {
            TextView cohortsDescriptionView = (TextView) mMainView.findViewById(R.id.cohortDescription);
            cohortsDescriptionView.setText(homeActivityMetadata.syncedCohorts + " Synced, " + homeActivityMetadata.totalCohorts + " Total");

            TextView patientDescriptionView = (TextView) mMainView.findViewById(R.id.patientDescription);
            patientDescriptionView.setText(homeActivityMetadata.syncedPatients + " Synced");

            TextView formsDescription = (TextView) mMainView.findViewById(R.id.formDescription);
            formsDescription.setText(homeActivityMetadata.incompleteForms + " Incomplete, "
                    + homeActivityMetadata.completeAndUnsyncedForms + " Complete");

            TextView notificationsDescription = (TextView) mMainView.findViewById(R.id.notificationDescription);
            notificationsDescription.setText(homeActivityMetadata.newNotifications + " New, "
                    + homeActivityMetadata.totalNotifications + " Total");

            TextView currentUser = (TextView) findViewById(R.id.currentUser);
            currentUser.setText(getResources().getString(R.string.currentUser) + " " + credentials.getUserName());
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.dashboard, menu);
        return true;
    }

    private void setupActionbar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(false);
    }

    private void executeBackgroundTask() {
        mBackgroundQueryTask = new BackgroundQueryTask();
        mBackgroundQueryTask.execute();
    }
}
