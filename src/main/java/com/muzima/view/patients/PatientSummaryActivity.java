package com.muzima.view.patients;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Patient;
import com.muzima.controller.FormController;
import com.muzima.controller.PatientController;
import com.muzima.view.forms.PatientFormsActivity;

import static com.muzima.utils.DateUtils.getFormattedDate;

public class PatientSummaryActivity extends SherlockActivity {
    private static final String TAG = "PatientSummaryActivity";

    public static final String PATIENT_ID = "patientId";
    public static final String PATIENT_SUMMARY = "patientSummary";

    private String patientId;
    private String patientSummary;
    private BackgroundQueryTask mBackgroundQueryTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_summary);

        Bundle intentExtras = getIntent().getExtras();
        if (intentExtras != null) {
            patientId = intentExtras.getString(PATIENT_ID);
            patientSummary = intentExtras.getString(PATIENT_SUMMARY);
        }

        setupActionbar();
        try {
            setupPatientMetadata();
        } catch (PatientController.PatientLoadException e) {
            Toast.makeText(this, "An error occurred while fetching patient", Toast.LENGTH_SHORT).show();
            finish();
        }

        executeBackgroundTask();
    }

    @Override
    protected void onStop() {
        if(mBackgroundQueryTask != null){
            mBackgroundQueryTask.cancel(true);
        }
        super.onStop();
    }

    private void setupActionbar() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void setupPatientMetadata() throws PatientController.PatientLoadException {
        PatientController patientController = ((MuzimaApplication) getApplicationContext()).getPatientController();
        Patient patient = patientController.getPatientByUuid(patientId);

        TextView patientName = (TextView) findViewById(R.id.patientName);
        patientName.setText(patient.getFamilyName() + ", " + patient.getGivenName() + " " + patient.getMiddleName());

        ImageView genderIcon = (ImageView) findViewById(R.id.genderImg);
        int genderDrawable = patient.getGender().equalsIgnoreCase("M") ? R.drawable.ic_male : R.drawable.ic_female;
        genderIcon.setImageDrawable(getResources().getDrawable(genderDrawable));

        TextView dob = (TextView) findViewById(R.id.dob);
        dob.setText("DOB: " + getFormattedDate(patient.getBirthdate()));

        TextView patientIdentifier = (TextView) findViewById(R.id.patientIdentifier);
        patientIdentifier.setText(patient.getIdentifier());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getSupportMenuInflater().inflate(R.menu.client_summary, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void showForms(View v) {
        Intent intent = new Intent(this, PatientFormsActivity.class);
        intent.putExtra(PatientSummaryActivity.PATIENT_ID, patientId);
        startActivity(intent);
    }

    public void showObservations(View v) {
        Intent intent = new Intent(PatientSummaryActivity.this, PatientObservationsActivity.class);
        intent.putExtra(PATIENT_ID, patientId);
        intent.putExtra(PATIENT_SUMMARY, patientSummary);
        startActivity(intent);
    }


    /**
     * Called when the user clicks the Clients Encounters Button or Search Clients Observations Button
     */
    public void clientObservations(View view) {
        Intent intent = new Intent(this, PatientObservationsActivity.class);
        if (view.getId() == R.id.quickSearch) {
            intent.putExtra("quickSearch", "true");
        }
        startActivity(intent);
    }

    private static class PatientSummaryActivityMetadata{
        int recommendedForms;
        int incompleteForms;
        int completeForms;
    }

    public class BackgroundQueryTask extends AsyncTask<Void, Void, PatientSummaryActivityMetadata> {

        @Override
        protected PatientSummaryActivityMetadata doInBackground(Void... voids) {
            MuzimaApplication muzimaApplication = (MuzimaApplication) getApplication();
            PatientSummaryActivityMetadata patientSummaryActivityMetadata = new PatientSummaryActivityMetadata();
            PatientController patientController = muzimaApplication.getPatientController();
            FormController formController = muzimaApplication.getFormController();
            try {
                patientSummaryActivityMetadata.recommendedForms = formController.getDownloadedFormsCount();
                patientSummaryActivityMetadata.completeForms = formController.getCompleteFormsCountForPatient(patientId);
                patientSummaryActivityMetadata.incompleteForms = formController.getIncompleteFormsCountForPatient(patientId);
            } catch (FormController.FormFetchException e) {
                Log.w(TAG, "FormFetchException occurred while fetching metadata in MainActivityBackgroundTask");
            }
            return patientSummaryActivityMetadata;
        }

        @Override
        protected void onPostExecute(PatientSummaryActivityMetadata patientSummaryActivityMetadata) {
            TextView formsCount = (TextView) findViewById(R.id.formsCount);
            formsCount.setText(Integer.toString(patientSummaryActivityMetadata.recommendedForms));

            TextView formsDescription = (TextView) findViewById(R.id.formDescription);
            formsDescription.setText(patientSummaryActivityMetadata.incompleteForms + " Incomplete, "
                    + patientSummaryActivityMetadata.completeForms + " Complete");
        }
    }

    private void executeBackgroundTask() {
        mBackgroundQueryTask = new BackgroundQueryTask();
        mBackgroundQueryTask.execute();
    }
}
