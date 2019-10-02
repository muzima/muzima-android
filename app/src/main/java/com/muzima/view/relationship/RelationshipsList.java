/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */
package com.muzima.view.relationship;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.adapters.patients.PatientAdapterHelper;
import com.muzima.adapters.relationships.PatientRelationshipsAdapter;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Relationship;
import com.muzima.utils.Fonts;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.patients.PatientSummaryActivity;

public class RelationshipsList extends BroadcastListenerActivity implements AdapterView.OnItemClickListener, ListAdapter.BackgroundListQueryTaskListener {
    private Patient patient;
    private PatientRelationshipsAdapter patientRelationshipsAdapter;
    private View noDataView;
    private final ThemeUtils themeUtils = new ThemeUtils();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeUtils.onCreate(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_relationships);
        patient = (Patient) getIntent().getSerializableExtra(PatientSummaryActivity.PATIENT);
        setupPatientMetadata();
        setupStillLoadingView();
        setupPatientEncounters();
    }

    @Override
    protected void onResume() {
        super.onResume();
        themeUtils.onResume(this);
    }

    private void setupPatientMetadata() {

        TextView patientName = findViewById(R.id.patientName);

        patientName.setText(PatientAdapterHelper.getPatientFormattedName(patient));

        ImageView genderIcon = findViewById(R.id.genderImg);
        int genderDrawable = patient.getGender().equalsIgnoreCase("M") ? R.drawable.ic_male : R.drawable.ic_female;
        genderIcon.setImageDrawable(getResources().getDrawable(genderDrawable));
    }

    private void setupPatientEncounters(){
        ListView  relationshipsLayout = findViewById(R.id.relationships_list);
        patientRelationshipsAdapter = new PatientRelationshipsAdapter(this,
                R.layout.item_relationship,
                ((MuzimaApplication) getApplicationContext()).getRelationshipController(), patient);
        patientRelationshipsAdapter.setBackgroundListQueryTaskListener(this);
        relationshipsLayout.setEmptyView(noDataView);
        relationshipsLayout.setAdapter(patientRelationshipsAdapter);
        relationshipsLayout.setOnItemClickListener(this);
        patientRelationshipsAdapter.reloadData();

    }

    private void setupNoDataView() {

        noDataView = findViewById(R.id.no_data_layout);
        TextView noDataMsgTextView = findViewById(R.id.no_data_msg);
        noDataMsgTextView.setText(getResources().getText(R.string.info_relationships_unavailable));
        noDataMsgTextView.setTypeface(Fonts.roboto_bold_condensed(this));
    }

    private void setupStillLoadingView() {

        noDataView = findViewById(R.id.no_data_layout);
        TextView noDataMsgTextView = findViewById(R.id.no_data_msg);
        noDataMsgTextView.setText(R.string.general_loading_encounters);
        noDataMsgTextView.setTypeface(Fonts.roboto_bold_condensed(this));
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        Relationship relationship = patientRelationshipsAdapter.getItem(position);
//        Intent intent = new Intent(this,EncounterSummaryActivity.class);
//        intent.putExtra(EncounterSummaryActivity.ENCOUNTER,encounter);
//        intent.putExtra(PatientSummaryActivity.PATIENT,patient);
//        startActivity(intent);
    }

    @Override
    public void onQueryTaskStarted() {}

    @Override
    public void onQueryTaskFinish() {
        if(patientRelationshipsAdapter.isEmpty()) {
            setupNoDataView();
        }
    }

    @Override
    public void onQueryTaskCancelled(){}

    @Override
    public void onQueryTaskCancelled(Object errorDefinition){}

}
