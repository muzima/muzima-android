package com.muzima.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.controller.CohortController;
import com.muzima.controller.FormController;
import com.muzima.controller.PatientController;
import com.muzima.search.api.util.StringUtil;
import com.muzima.view.cohort.CohortActivity;
import com.muzima.view.forms.FormsActivity;
import com.muzima.view.patients.PatientsListActivity;
import com.muzima.view.preferences.SettingsActivity;

public class MainActivity extends BroadcastListenerActivity {
    private static final String TAG = "MainActivity";
    private View mMainView;
    private BackgroundQueryTask mBackgroundQueryTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMainView = getLayoutInflater().inflate(R.layout.activity_dashboard, null);
        setContentView(mMainView);

        setupActionbar();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getSupportMenuInflater().inflate(R.menu.dashboard, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_settings:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_help:
                intent = new Intent(this, HelpActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_logout:
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                String passwordKey = getResources().getString(R.string.preference_password);
                settings.edit()
                        .putString(passwordKey, StringUtil.EMPTY)
                        .commit();

                launchLoginActivity(false);
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
        if (view.getId() == R.id.quickSearch) {
            intent.putExtra(PatientsListActivity.QUICK_SEARCH, "true");
        }
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
     * Called when the user clicks the Notices area
     */
    public void noticesList(View view) {
        Intent intent = new Intent(this, NoticeListActivity.class);
        startActivity(intent);
    }

    /**
     * Called when the user clicks the Register Client Button
     */
    public void registerClient(View view) {
        Intent intent = new Intent(this, RegisterClientActivity.class);
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
            try {
                homeActivityMetadata.totalCohorts = cohortController.getTotalCohortsCount();
                homeActivityMetadata.syncedCohorts = cohortController.getSyncedCohortsCount();
                homeActivityMetadata.syncedPatients = patientController.getTotalPatientsCount();
                homeActivityMetadata.totalForms = formController.getTotalFormCount();
                homeActivityMetadata.downloadedForms = formController.getDownloadedFormsCount();
                homeActivityMetadata.incompleteForms = formController.getAllIncompleteFormsSize();
                homeActivityMetadata.completeAndUnsyncedForms = formController.getAllCompleteFormsSize();
            } catch (CohortController.CohortFetchException e) {
                Log.w(TAG, "CohortFetchException occurred while fetching metadata in MainActivityBackgroundTask");
            } catch (PatientController.PatientLoadException e) {
                Log.w(TAG, "PatientLoadException occurred while fetching metadata in MainActivityBackgroundTask");
            } catch (FormController.FormFetchException e) {
                Log.w(TAG, "FormFetchException occurred while fetching metadata in MainActivityBackgroundTask");
            }
            return homeActivityMetadata;
        }

        @Override
        protected void onPostExecute(HomeActivityMetadata homeActivityMetadata) {
            TextView cohortsCountView = (TextView) mMainView.findViewById(R.id.cohortsCount);
            cohortsCountView.setText(homeActivityMetadata.syncedCohorts + "/" + homeActivityMetadata.totalCohorts);
            TextView cohortsDescriptionView = (TextView) mMainView.findViewById(R.id.cohortDescription);
            cohortsDescriptionView.setText(homeActivityMetadata.syncedCohorts + " Synced, " + homeActivityMetadata.totalCohorts + " Total");

            TextView patientsCountView = (TextView) mMainView.findViewById(R.id.patientsCount);
            patientsCountView.setText(homeActivityMetadata.syncedPatients + "/##");

            TextView formsCount = (TextView) mMainView.findViewById(R.id.formsCount);
            formsCount.setText(homeActivityMetadata.downloadedForms + "/" + homeActivityMetadata.totalForms);

            TextView formsDescription = (TextView) mMainView.findViewById(R.id.formDescription);
            formsDescription.setText(homeActivityMetadata.incompleteForms + " Incomplete, "
                    + homeActivityMetadata.completeAndUnsyncedForms + " Complete");
        }
    }

    private static class HomeActivityMetadata {
        int totalCohorts;
        int syncedCohorts;
        int totalPatients;
        int syncedPatients;
        int overdueReminders;
        int upcomingReminders;
        int incompleteForms;
        int completeAndUnsyncedForms;
        int downloadedForms;
        int totalForms;
        int totalNotices;
        int unreadNotices;
        int recommendations;
    }

    private void setupActionbar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(false);
    }

    private void executeBackgroundTask() {
        mBackgroundQueryTask = new BackgroundQueryTask();
        mBackgroundQueryTask.execute();
    }
}
