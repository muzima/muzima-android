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

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.adapters.concept.SelectedProviderAdapter;
import com.muzima.adapters.patients.PatientAdapterHelper;
import com.muzima.adapters.relationships.AutoCompleteRelatedPersonAdapter;
import com.muzima.adapters.relationships.RelationshipTypesAdapter;
import com.muzima.adapters.relationships.RelationshipsAdapter;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Relationship;
import com.muzima.api.model.RelationshipType;
import com.muzima.utils.Fonts;
import com.muzima.utils.StringUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.patients.PatientSummaryActivity;

public class RelationshipsListActivity extends BroadcastListenerActivity implements AdapterView.OnItemClickListener, ListAdapter.BackgroundListQueryTaskListener {
    private Patient patient;
    private RelationshipsAdapter patientRelationshipsAdapter;
    private View noDataView;
    private View addRelationshipView;
    private final ThemeUtils themeUtils = new ThemeUtils();
    private AlertDialog relationshipTypeDialog;
    private ListView lvwPatientRelationships;
    private FloatingActionButton fabNewRelationship;
    private AutoCompleteTextView autoCompletePersonTextView;
    private TextView textViewInfo;

    private SelectedProviderAdapter selectedProviderAdapter;
    private ListView selectedProviderListView;
    private boolean actionModeActive = false;
    private ActionMode actionMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeUtils.onCreate(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_relationships);
        patient = (Patient) getIntent().getSerializableExtra(PatientSummaryActivity.PATIENT);
        addRelationshipView = findViewById(R.id.add_relationship);
        fabNewRelationship = findViewById(R.id.fab_add_relationship);
        autoCompletePersonTextView = findViewById(R.id.search_related_person);
        textViewInfo = findViewById(R.id.info);
        AutoCompleteRelatedPersonAdapter autoCompleteProvidersTextView = new AutoCompleteRelatedPersonAdapter(this, R.layout.item_option_autocomplete, autoCompletePersonTextView);
        autoCompletePersonTextView.setAdapter(autoCompleteProvidersTextView);
        autoCompletePersonTextView.setOnItemClickListener(autoCompleteOnClickListener());

        setupPatientMetadata();
        setupStillLoadingView();
        setupPatientRelationships();
    }

    private AdapterView.OnItemClickListener autoCompleteOnClickListener() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
//                Provider selectedProvider = (Provider) parent.getItemAtPosition(position);
//                if (selectedProviderAdapter.doesProviderAlreadyExist(selectedProvider)) {
//                    Log.e(getClass().getSimpleName(), "Providers Already exists");
//                    Toast.makeText(ProviderPreferenceActivity.this, "Provider " + selectedProvider.getName() + " already exists", Toast.LENGTH_SHORT).show();
//                } else {
//                    selectedProviderAdapter.addProvider(selectedProvider);
//                    selectedProviderAdapter.notifyDataSetChanged();
//                }
//                autoCompleteProvidersTextView.setText(StringUtils.EMPTY);
            }
        };
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

    private void setupPatientRelationships() {
        lvwPatientRelationships = findViewById(R.id.relationships_list);
        patientRelationshipsAdapter = new RelationshipsAdapter(this, R.layout.item_relationship,
                ((MuzimaApplication) getApplicationContext()).getRelationshipController(), patient.getUuid());
        patientRelationshipsAdapter.setBackgroundListQueryTaskListener(this);
        lvwPatientRelationships.setEmptyView(noDataView);
        lvwPatientRelationships.setAdapter(patientRelationshipsAdapter);
        lvwPatientRelationships.setOnItemClickListener(this);
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
        noDataMsgTextView.setText(R.string.general_loading_relationships);
        noDataMsgTextView.setTypeface(Fonts.roboto_bold_condensed(this));
    }

    public void createRelationship(View view) {
        displayChooseRelationshipTypeDialog();
    }

    public void cancelRelationshipAdd(View view) {
        // Show the existing relationships
        lvwPatientRelationships.setVisibility(View.VISIBLE);
        fabNewRelationship.setVisibility(View.VISIBLE);

        // Hide the add relationship view
        addRelationshipView.setVisibility(View.GONE);
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
    public void onQueryTaskStarted() {
    }

    @Override
    public void onQueryTaskFinish() {
        if (patientRelationshipsAdapter.isEmpty()) {
            setupNoDataView();
        }
    }

    @Override
    public void onQueryTaskCancelled() {
    }

    @Override
    public void onQueryTaskCancelled(Object errorDefinition) {
    }

    private void displayChooseRelationshipTypeDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(RelationshipsListActivity.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.activity_relationship_type_layout, null);

        ListView ll = view.findViewById(R.id.relationship_type_list);
        RelationshipTypesAdapter relationshipTypesAdapter = new RelationshipTypesAdapter(this, R.layout.item_relationship,
                ((MuzimaApplication) getApplicationContext()).getRelationshipController(), this);

        ll.setAdapter(relationshipTypesAdapter);
        relationshipTypesAdapter.reloadData();

        builder.setNegativeButton(getString(R.string.general_cancel), null);

        builder.setView(view);

        relationshipTypeDialog = builder.create();
        relationshipTypeDialog.show();
    }

    public void relationshipTypeSelected(RelationshipType relationshipType, String side) {
        relationshipTypeDialog.dismiss();

        // Hide the existing relationships
        lvwPatientRelationships.setVisibility(View.GONE);
        fabNewRelationship.setVisibility(View.GONE);
        textViewInfo.setText(patient.getDisplayName() + "'s " + (StringUtils.equals(side,"A") ? relationshipType.getAIsToB() : relationshipType.getBIsToA()) + " is:");

        // Show the add relationship view
        addRelationshipView.setVisibility(View.VISIBLE);
        autoCompletePersonTextView.requestFocus();
    }


}
