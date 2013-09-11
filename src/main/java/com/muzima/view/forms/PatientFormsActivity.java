
package com.muzima.view.forms;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.MuzimaPagerAdapter;
import com.muzima.adapters.forms.PatientFormsPagerAdapter;
import com.muzima.api.model.Patient;
import com.muzima.controller.PatientController;

import static com.muzima.view.patients.PatientSummaryActivity.PATIENT_ID;


public class PatientFormsActivity extends FormsActivityBase {
    private static final String TAG = "FormsActivity";
    private String patientId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_with_pager);
        Intent intent = getIntent();
        patientId = intent.getStringExtra(PATIENT_ID);
        super.onCreate(savedInstanceState);
        try {
            setupActionbar();
        } catch (PatientController.PatientLoadException e) {
            Toast.makeText(this, "An error occurred while fetching patien", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupActionbar() throws PatientController.PatientLoadException {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        PatientController patientController = ((MuzimaApplication) getApplicationContext()).getPatientController();
        Patient patient = patientController.getPatientByUuid(patientId);
        getSupportActionBar().setTitle(patient.getFamilyName() + ", " + patient.getGivenName() + " " + patient.getMiddleName());
    }


    @Override
    protected MuzimaPagerAdapter createFormsPagerAdapter() {
        return new PatientFormsPagerAdapter(getApplicationContext(), getSupportFragmentManager(), patientId);
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
}
