package com.muzima.view.patients;

import android.content.Intent;
import android.os.Bundle;
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
import com.muzima.controller.PatientController;
import com.muzima.view.forms.PatientFormsActivity;

import static com.muzima.utils.DateUtils.getFormattedDate;

public class PatientSummaryActivity extends SherlockActivity {
    public static final String PATIENT_ID = "patientId";
    public static final String PATIENT_SUMMARY = "patientSummary";

    private String patientId;
    private String patientSummary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_summary);

        Bundle intentExtras = getIntent().getExtras();
        if (intentExtras != null) {
            patientId = intentExtras.getString(PATIENT_ID);
            patientSummary = intentExtras.getString(PATIENT_SUMMARY);
        }

		setupActionBar();
        try {
            setupPatientMetadata();
        } catch (PatientController.PatientLoadException e) {
            Toast.makeText(this, "An error occurred while fetching patien", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setTitle(patientSummary);
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
                overridePendingTransition(R.anim.push_in_from_left, R.anim.push_out_to_right);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void showForms(View v) {
        Intent intent = new Intent(this, PatientFormsActivity.class);
        intent.putExtra(PatientSummaryActivity.PATIENT_ID, patientId);
        startActivity(intent);
        overridePendingTransition(R.anim.push_in_from_right, R.anim.push_out_to_left);
    }

    public void showObservations(View v) {
        Intent intent = new Intent(PatientSummaryActivity.this, PatientObservationsActivity.class);
        intent.putExtra(PATIENT_ID, patientId);
        intent.putExtra(PATIENT_SUMMARY, patientSummary);
        startActivity(intent);
        overridePendingTransition(R.anim.push_in_from_right, R.anim.push_out_to_left);
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
}
