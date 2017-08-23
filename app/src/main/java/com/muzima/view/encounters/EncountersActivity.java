/*
 * Copyright (c) 2014 - 2017. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */
package com.muzima.view.encounters;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Menu;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.adapters.encounters.EncountersByPatientAdapter;
import com.muzima.adapters.patients.PatientAdapterHelper;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.Patient;
import com.muzima.controller.PatientController;
import com.muzima.utils.Fonts;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.patients.PatientSummaryActivity;

import static com.muzima.utils.DateUtils.getFormattedDate;

public class EncountersActivity extends BroadcastListenerActivity implements AdapterView.OnItemClickListener, ListAdapter.BackgroundListQueryTaskListener {
    private Patient patient;
    private EncountersByPatientAdapter encountersByPatientAdapter;
    private View noDataView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_encounters);
        patient = (Patient) getIntent().getSerializableExtra(PatientSummaryActivity.PATIENT);
        try {
            setupPatientMetadata();
            setupStillLoadingView();
            setupPatientEncounters();
        } catch (PatientController.PatientLoadException e) {
            Toast.makeText(this, getString(R.string.error_patient_fetch), Toast.LENGTH_SHORT).show();
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

    private void setupPatientEncounters(){
        ListView  encountersLayout = (ListView)findViewById(R.id.encounter_list);
        encountersByPatientAdapter = new EncountersByPatientAdapter(EncountersActivity.this,
                R.layout.item_encounter,
                ((MuzimaApplication) getApplicationContext()).getEncounterController(),patient);
        encountersByPatientAdapter.setBackgroundListQueryTaskListener(this);
        encountersLayout.setEmptyView(noDataView);
        encountersLayout.setAdapter(encountersByPatientAdapter);
        encountersLayout.setOnItemClickListener(this);
        encountersByPatientAdapter.reloadData();

    }

    private void setupNoDataView() {

        noDataView = findViewById(R.id.no_data_layout);
        TextView noDataMsgTextView = (TextView) findViewById(R.id.no_data_msg);
        noDataMsgTextView.setText(getResources().getText(R.string.info_encounter_unavailable));
        noDataMsgTextView.setTypeface(Fonts.roboto_bold_condensed(this));
    }

    private void setupStillLoadingView() {

        noDataView = findViewById(R.id.no_data_layout);
        TextView noDataMsgTextView = (TextView) findViewById(R.id.no_data_msg);
        noDataMsgTextView.setText("Encounters are still loading");
        noDataMsgTextView.setTypeface(Fonts.roboto_bold_condensed(this));
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        Encounter encounter = encountersByPatientAdapter.getItem(position);
        Intent intent = new Intent(this,EncounterSummaryActivity.class);
        intent.putExtra(EncounterSummaryActivity.ENCOUNTER,encounter);
        intent.putExtra(PatientSummaryActivity.PATIENT,patient);
        startActivity(intent);
    }

    @Override
    public void onQueryTaskStarted() {}

    @Override
    public void onQueryTaskFinish() {
        if(encountersByPatientAdapter.isEmpty()) {
            setupNoDataView();
        }
    }

    @Override
    public void onQueryTaskCancelled(){}

    @Override
    public void onQueryTaskCancelled(Object errorDefinition){}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.client_summary, menu);
        super.onCreateOptionsMenu(menu);
        return true;
    }
}
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        