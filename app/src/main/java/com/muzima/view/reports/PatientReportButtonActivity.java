/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */
package com.muzima.view.reports;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.muzima.R;
import com.muzima.adapters.encounters.EncountersByPatientAdapter;
import com.muzima.adapters.patients.PatientAdapterHelper;
import com.muzima.api.model.Patient;
import com.muzima.controller.PatientController;
import com.muzima.utils.Constants;
import com.muzima.view.BroadcastListenerActivity;

import static com.muzima.utils.DateUtils.getFormattedDate;
import static com.muzima.view.patients.PatientSummaryActivity.PATIENT;

public class PatientReportButtonActivity extends BroadcastListenerActivity {
    private Patient patient;
    private EncountersByPatientAdapter encountersByPatientAdapter;
    private View noDataView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_report_test_button);
        patient = (Patient) getIntent().getSerializableExtra(PATIENT);
        try {
            setupPatientMetadata();
    
        }
        catch (PatientController.PatientLoadException e) {
            finish();
        }
    }

    private void setupPatientMetadata() throws PatientController.PatientLoadException {

        TextView patientName = (TextView) findViewById(R.id.patientName);

        patientName.setText(PatientAdapterHelper.getPatientFormattedName(patient));

        ImageView genderIcon = (ImageView) findViewById(R.id.genderImg);
        int genderDrawable = patient.getGender().equalsIgnoreCase("M") ? R.drawable.ic_male : R.drawable.ic_female;
        genderIcon.setImageDrawable(getResources().getDrawable(genderDrawable));

        TextView dob = (TextView) findViewById(R.id.dob);
        dob.setText("DOB: " + getFormattedDate(patient.getBirthdate()));

        TextView patientIdentifier = (TextView) findViewById(R.id.patientIdentifier);
        patientIdentifier.setText(patient.getIdentifier());
    }
    
    public void downloadReport(View v) {
    
        new SyncMuzimaGeneratedReportIntent(this, patient.getUuid()).start();
    }
    
    @Override
    protected void onReceive(Context context, Intent intent){
      super.onReceive(context, intent);
        
        int syncType = intent.getIntExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, -1);
        
        if (syncType == Constants.DataSyncServiceConstants.SYNC_MUZIMA_GENERATED_REPORTS) {
            Intent i = new Intent(getApplicationContext(), PatientReportWebActivity.class);
            intent.putExtra(PATIENT, patient);
            
            startActivity(i);
        }
    }
}
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        
