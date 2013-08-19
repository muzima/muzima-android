package com.muzima.view.patients;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.forms.FormsPagerAdapter;
import com.muzima.adapters.patients.CohortPatientsAdapter;

public class CohortPatientsActivity extends Activity {
    public static final String COHORT_ID = "cohortId";
    private CohortPatientsAdapter cohortPatientsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cohort_patient_list);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        ListView cohortPatientsList = (ListView) findViewById(R.id.cohort_patients_list);

        cohortPatientsAdapter = new CohortPatientsAdapter(this,
                R.layout.item_patients_list,
                ((MuzimaApplication)getApplicationContext()).getPatientController(),
                getIntent().getExtras().getString(COHORT_ID));

        cohortPatientsList.setAdapter(cohortPatientsAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cohortPatientsAdapter.reloadData();
    }
}
