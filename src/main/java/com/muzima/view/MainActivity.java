package com.muzima.view;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.controller.CohortController;
import com.muzima.controller.FormController;
import com.muzima.controller.NotificationController;
import com.muzima.controller.PatientController;
import com.muzima.domain.Credentials;
import com.muzima.view.cohort.CohortActivity;
import com.muzima.view.forms.FormsActivity;
import com.muzima.view.forms.RegistrationFormsActivity;
import com.muzima.view.patients.PatientsListActivity;
import org.apache.lucene.queryParser.ParseException;

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

        setupActionbar();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStart() {
        super.onStart();
        executeBackgroundTask();
    }

    @Override
    protected void onPause() {
        super.onPause();
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
        Intent intent = new Intent(this, PatientsListActivity.class);
        intent.putExtra(PatientsListActivity.COHORT_NAME, PatientsListActivity.NOTIFICATIONS);
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
                homeActivityMetadata.newNotifications =  notificationController.getAllNotificationsBySenderCount(credentials.getUserName(),null);
            } catch (CohortController.CohortFetchException e) {
                Log.w(TAG, "CohortFetchException occurred while fetching metadata in MainActivityBackgroundTask");
            } catch (PatientController.PatientLoadException e) {
                Log.w(TAG, "PatientLoadException occurred while fetching metadata in MainActivityBackgroundTask");
            } catch (FormController.FormFetchException e) {
                Log.w(TAG, "FormFetchException occurred while fetching metadata in MainActivityBackgroundTask");
            } catch (NotificationController.NotificationFetchException e) {
                Log.w(TAG, "NotificationFetchException occurred while fetching metadata in MainActivityBackgroundTask");
            } catch (ParseException e) {
                Log.w(TAG, "NotificationFetchException occurred while fetching metadata in MainActivityBackgroundTask");
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
            notificationsDescription.setText(homeActivityMetadata.newNotifications + " New Notifications");

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

        // notifications
        int newNotifications;

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
