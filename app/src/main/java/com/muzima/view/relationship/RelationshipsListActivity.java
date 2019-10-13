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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.adapters.patients.PatientAdapterHelper;
import com.muzima.adapters.relationships.AutoCompleteRelatedPersonAdapter;
import com.muzima.adapters.relationships.RelationshipTypesAdapter;
import com.muzima.adapters.relationships.RelationshipsAdapter;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Person;
import com.muzima.api.model.Relationship;
import com.muzima.api.model.RelationshipType;
import com.muzima.controller.RelationshipController;
import com.muzima.utils.Fonts;
import com.muzima.utils.StringUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.patients.PatientSummaryActivity;

import java.util.Objects;

public class RelationshipsListActivity extends BroadcastListenerActivity implements ListAdapter.BackgroundListQueryTaskListener {
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

    private RelationshipType selectedNewRelationshipType;
    private String selectedSide;

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
        AutoCompleteRelatedPersonAdapter autoCompleteRelatedPersonAdapterAdapter = new AutoCompleteRelatedPersonAdapter(this, R.layout.item_option_autocomplete, autoCompletePersonTextView);
        autoCompletePersonTextView.setAdapter(autoCompleteRelatedPersonAdapterAdapter);
        autoCompletePersonTextView.setOnItemClickListener(autoCompleteOnClickListener());

        setupPatientMetadata();
        setupStillLoadingView();
        setupPatientRelationships();
    }

    private AdapterView.OnItemClickListener listOnClickListener() {
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
                System.out.println("narudikimbia home");
            }
        };
    }

    private AdapterView.OnItemClickListener autoCompleteOnClickListener() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                Person selectedPerson = (Person) parent.getItemAtPosition(position);
                if (StringUtils.equals(selectedPerson.getUuid(), patient.getUuid())) {
                    Log.e(getClass().getSimpleName(), "Self relation is invalid");
                    Toast.makeText(RelationshipsListActivity.this, "Patient cannot relate to him/herself", Toast.LENGTH_SHORT).show();
                } else {
                    Relationship newRelationship;
                    if (StringUtils.equals(selectedSide, "A"))
                        newRelationship = new Relationship(patient, selectedPerson, selectedNewRelationshipType);
                    else
                        newRelationship = new Relationship(selectedPerson, patient, selectedNewRelationshipType);

                    if (patientRelationshipsAdapter.relationshipWithPersonExist(newRelationship)){
                        Log.e(getClass().getSimpleName(), "Relationship already exists");
                        Toast.makeText(RelationshipsListActivity.this, "Patient already has " +
                                selectedNewRelationshipType.getAIsToB() + ":" + selectedNewRelationshipType.getBIsToA()
                                + " relationship with " + selectedPerson.getDisplayName(), Toast.LENGTH_LONG).show();
                    } else {
                        try {
                            ((MuzimaApplication) getApplicationContext()).getRelationshipController().saveRelationship(newRelationship);
                            patientRelationshipsAdapter.reloadData();
                            closeNewRelationshipWindow();
                            Toast.makeText(RelationshipsListActivity.this, "Relationship created successfully", Toast.LENGTH_LONG).show();
                        } catch (RelationshipController.SaveRelationshipException e) {
                            Log.e(getClass().getSimpleName(), "Error saving new relationship");
                        }
                    }
                }
                autoCompletePersonTextView.setText(StringUtils.EMPTY);
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
        lvwPatientRelationships.setOnItemClickListener(listOnClickListener());
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
        closeNewRelationshipWindow();
    }

    private void closeNewRelationshipWindow(){
        // Show the existing relationships
        lvwPatientRelationships.setVisibility(View.VISIBLE);
        fabNewRelationship.setVisibility(View.VISIBLE);

        // Hide the add relationship view
        autoCompletePersonTextView.setText(StringUtils.EMPTY);
        addRelationshipView.setVisibility(View.GONE);

        // and the keyboard too if open
        try {
            InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(Objects.requireNonNull(getCurrentFocus()).getWindowToken(), 0);
            }
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), "Closing a closed keyboard");
        }
    }

    public void saveRelationship(View view) {
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

        selectedNewRelationshipType = relationshipType;
        selectedSide = side;

        // Hide the existing relationships
        lvwPatientRelationships.setVisibility(View.GONE);
        fabNewRelationship.setVisibility(View.GONE);
        textViewInfo.setText(String.format("%s's %s is:", patient.getDisplayName(), StringUtils.equals(side, "A") ? relationshipType.getAIsToB() : relationshipType.getBIsToA()));

        // Show the add relationship view
        addRelationshipView.setVisibility(View.VISIBLE);
        autoCompletePersonTextView.requestFocus();
    }
}
