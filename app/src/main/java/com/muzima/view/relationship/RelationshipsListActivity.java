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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
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
import com.muzima.controller.FormController;
import com.muzima.controller.PatientController;
import com.muzima.controller.RelationshipController;
import com.muzima.model.relationship.RelationshipTypeWrap;
import com.muzima.utils.Fonts;
import com.muzima.utils.RelationshipJsonMapper;
import com.muzima.utils.StringUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.patients.PatientSummaryActivity;
import es.dmoral.toasty.Toasty;
import org.json.JSONException;

import java.util.Objects;
import java.util.UUID;

import static com.muzima.utils.DateUtils.getFormattedDate;

public class RelationshipsListActivity extends BroadcastListenerActivity implements ListAdapter.BackgroundListQueryTaskListener {
    private Patient patient;
    private RelationshipsAdapter patientRelationshipsAdapter;
    private View noDataView;
    private View addRelationshipView;
    private final ThemeUtils themeUtils = new ThemeUtils();
    private ListView lvwPatientRelationships;
    private AutoCompleteTextView autoCompletePersonTextView;
    private AutoCompleteRelatedPersonAdapter autoCompleteRelatedPersonAdapterAdapter;
    private TextView textViewInfo;
    private Spinner relationshipType;
    private Person selectedPerson;

    private RelationshipController relationshipController;
    private PatientController patientController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeUtils.onCreate(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_relationships);
        patient = (Patient) getIntent().getSerializableExtra(PatientSummaryActivity.PATIENT);
        addRelationshipView = findViewById(R.id.add_relationship);
        autoCompletePersonTextView = findViewById(R.id.search_related_person);
        textViewInfo = findViewById(R.id.info);
        relationshipType = findViewById(R.id.relationshipType);

        relationshipController = ((MuzimaApplication) getApplicationContext()).getRelationshipController();
        patientController = ((MuzimaApplication) getApplicationContext()).getPatientController();

        autoCompleteRelatedPersonAdapterAdapter = new AutoCompleteRelatedPersonAdapter(this, R.layout.item_option_autocomplete, autoCompletePersonTextView, this);
        autoCompletePersonTextView.setAdapter(autoCompleteRelatedPersonAdapterAdapter);
        autoCompletePersonTextView.setOnItemClickListener(autoCompleteOnClickListener());
        autoCompletePersonTextView.addTextChangedListener(autoCompleteTextWatcher());

        setupPatientMetadata();
        setupStillLoadingView();
        setupPatientRelationships();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.relationship_list, menu);

        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_add_relationship) {
            createRelationshipView();
        }
        return super.onOptionsItemSelected(item);
    }

    private AdapterView.OnItemClickListener listOnClickListener() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                Relationship relationship = (Relationship) parent.getItemAtPosition(position);
                Patient relatedPerson;
                try {
                    if (StringUtils.equals(relationship.getPersonA().getUuid(), patient.getUuid()))
                        relatedPerson = patientController.getPatientByUuid(relationship.getPersonB().getUuid());
                    else
                        relatedPerson = patientController.getPatientByUuid(relationship.getPersonA().getUuid());

                    if (relatedPerson != null ){
                        Intent intent = new Intent(RelationshipsListActivity.this, PatientSummaryActivity.class);

                        intent.putExtra(PatientSummaryActivity.PATIENT, relatedPerson);
                        startActivity(intent);
                    } else {
                        System.out.println("Will show create");
                    }

                } catch (PatientController.PatientLoadException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private AdapterView.OnItemClickListener autoCompleteOnClickListener() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                selectedPerson = (Person) parent.getItemAtPosition(position);
            }
        };
    }

    private TextWatcher autoCompleteTextWatcher() {
        return new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                selectedPerson = null;

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {
//                if (autoCompletePersonTextView.enoughToFilter() && autoCompletePersonTextView.isPopupShowing()) {
//                    System.out.println("Sanugu lets create");
//                    System.out.println(autoCompleteRelatedPersonAdapterAdapter.getCount());
//                    System.out.println(autoCompletePersonTextView.getAdapter().getCount());
//                }
            }
        };
    }

    public void onFilterComplete() {
        System.out.println("Therere: " + autoCompleteRelatedPersonAdapterAdapter.getCount());
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

        TextView dob = findViewById(R.id.dob);
        dob.setText(String.format("DOB: %s", getFormattedDate(patient.getBirthdate())));

        TextView patientIdentifier = findViewById(R.id.patientIdentifier);
        patientIdentifier.setText(patient.getIdentifier());
    }

    private void setupPatientRelationships() {
        lvwPatientRelationships = findViewById(R.id.relationships_list);
        patientRelationshipsAdapter = new RelationshipsAdapter(this, R.layout.item_relationship, relationshipController,
                patient.getUuid(), patientController);
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

    public void cancelRelationshipAdd(View view) {
        closeNewRelationshipWindow();
    }

    private void closeNewRelationshipWindow(){
        // Show the existing relationships
        lvwPatientRelationships.setVisibility(View.VISIBLE);

        // Hide the add relationship view
        autoCompletePersonTextView.setText(StringUtils.EMPTY);
        addRelationshipView.setVisibility(View.GONE);

        // and the keyboard too if open
        try {
            InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null)
                imm.hideSoftInputFromWindow(Objects.requireNonNull(getCurrentFocus()).getWindowToken(), 0);
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), "Closing a closed keyboard");
        }
    }

    @Override
    public void onQueryTaskStarted() {}

    @Override
    public void onQueryTaskFinish() {
        if (patientRelationshipsAdapter.isEmpty())
            setupNoDataView();
    }

    @Override
    public void onQueryTaskCancelled() {}

    @Override
    public void onQueryTaskCancelled(Object errorDefinition) {}

    private void createRelationshipView() {

        RelationshipTypesAdapter relationshipTypesAdapter = new RelationshipTypesAdapter(this, R.layout.item_relationship,
                relationshipController, this);

        relationshipType.setAdapter(relationshipTypesAdapter);
        relationshipTypesAdapter.reloadData();

        // Hide the existing relationships
        lvwPatientRelationships.setVisibility(View.GONE);

        // Show the add relationship view
        addRelationshipView.setVisibility(View.VISIBLE);
        autoCompletePersonTextView.requestFocus();
        textViewInfo.setText(String.format("%s is a:", patient.getDisplayName()));
    }

    public void saveRelationship(View view) {

        if (StringUtils.isEmpty(((RelationshipTypeWrap) relationshipType.getSelectedItem()).getUuid())) {
            Toasty.error(RelationshipsListActivity.this, "Please choose a relationship type", Toast.LENGTH_LONG, true).show();
            return;
        }

        if (selectedPerson == null) {
            Toasty.error(RelationshipsListActivity.this, "Please choose a related person", Toast.LENGTH_LONG, true).show();
            return;
        }

        if (StringUtils.equals(selectedPerson.getUuid(), patient.getUuid())) {
            Log.e(getClass().getSimpleName(), "Self relation is invalid");
            Toasty.warning(RelationshipsListActivity.this, "Patient cannot relate to him/herself", Toast.LENGTH_SHORT, true).show();
            return;
        }

        RelationshipTypeWrap relationshipTypeWrap = ((RelationshipTypeWrap) relationshipType.getSelectedItem());

        Relationship newRelationship;
        if (StringUtils.equals(relationshipTypeWrap.getSide(), "A"))
            newRelationship = new Relationship(patient, selectedPerson, relationshipTypeWrap.getRelationshipType());
        else
            newRelationship = new Relationship(selectedPerson, patient, relationshipTypeWrap.getRelationshipType());

        newRelationship.setUuid(UUID.randomUUID().toString());

        if (((MuzimaApplication) getApplicationContext()).getRelationshipController().relationshipExists(newRelationship)){
            Log.e(getClass().getSimpleName(), "Relationship already exists");
            Toast.makeText(RelationshipsListActivity.this, "Patient already has " +
                    relationshipTypeWrap.getRelationshipType().getAIsToB() + ":" + relationshipTypeWrap.getRelationshipType().getBIsToA()
                    + " relationship with " + selectedPerson.getDisplayName(), Toast.LENGTH_LONG).show();
        } else {
            // we will create a formData payload and save the relationship ONLY on success

            try {
                RelationshipJsonMapper relationshipJsonMapper = new RelationshipJsonMapper(newRelationship, patient, patientController,
                        ((MuzimaApplication) getApplicationContext()).getAuthenticatedUser());
                ((MuzimaApplication) getApplicationContext()).getFormController().saveFormData(relationshipJsonMapper.createFormDataFromRelationship());
                relationshipController.saveRelationship(newRelationship);
                patientRelationshipsAdapter.reloadData();
                closeNewRelationshipWindow();
                Toast.makeText(RelationshipsListActivity.this, "Relationship created successfully", Toast.LENGTH_LONG).show();
            } catch (RelationshipController.SaveRelationshipException e) {
                Log.e(getClass().getSimpleName(), "Error saving new relationship");
            } catch (JSONException e) {
                Log.e("", "Error While Parsing data" + e);
            } catch (FormController.FormDataSaveException e) {
                Log.e("", "Error While Saving Form Data" + e);
            }
        }
    }
}
