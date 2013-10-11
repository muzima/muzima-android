
package com.muzima.view.forms;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.muzima.R;
import com.muzima.adapters.MuzimaPagerAdapter;
import com.muzima.adapters.forms.PatientFormsPagerAdapter;
import com.muzima.api.model.Patient;
import com.muzima.controller.PatientController;
import com.muzima.view.patients.PatientSummaryActivity;


public class PatientFormsActivity extends FormsActivityBase {
    private static final String TAG = "FormsActivity";
    private Patient patient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_with_pager);
        Intent intent = getIntent();
        patient = (Patient)intent.getSerializableExtra(PatientSummaryActivity.PATIENT);
        super.onCreate(savedInstanceState);
        try {
            setupActionbar();
        } catch (PatientController.PatientLoadException e) {
            Toast.makeText(this, "An error occurred while fetching patient", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupActionbar() throws PatientController.PatientLoadException {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(patient.getFamilyName() + ", " + patient.getGivenName() + " " + patient.getMiddleName());
    }


    @Override
    protected MuzimaPagerAdapter createFormsPagerAdapter() {
        return new PatientFormsPagerAdapter(getApplicationContext(), getSupportFragmentManager(), patient.getUuid());
    }
}
