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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
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
import com.muzima.api.model.FormData;
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
import com.muzima.view.forms.RegistrationFormsActivity;
import com.muzima.view.patients.PatientSummaryActivity;
import es.dmoral.toasty.Toasty;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.muzima.utils.DateUtils.getFormattedDate;

public class RelationshipsListActivity extends BroadcastListenerActivity implements ListAdapter.BackgroundListQueryTaskListener {
    private Patient patient;
    private RelationshipsAdapter patientRelationshipsAdapter;
    private View noDataView;
    private View searchServerView;
    private View createPersonView;
    private View addRelationshipView;
    private final ThemeUtils themeUtils = new ThemeUtils();
    private ListView lvwPatientRelationships;
    private AutoCompleteTextView autoCompletePersonTextView;
    private AutoCompleteRelatedPersonAdapter autoCompleteRelatedPersonAdapterAdapter;
    private TextView textViewInfo;
    private Spinner relationshipType;
    private Person selectedPerson;
    private Button saveButton;

    private RelationshipController relationshipController;
    private PatientController patientController;

    private Person selectedRelatedPerson;

    private boolean actionModeActive = false;
    private ActionMode actionMode;

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
        saveButton = findViewById(R.id.save);
        searchServerView = findViewById(R.id.search_server_layout);
        createPersonView = findViewById(R.id.create_person_layout);

        relationshipController = ((MuzimaApplication) getApplicationContext()).getRelationshipController();
        patientController = ((MuzimaApplication) getApplicationContext()).getPatientController();

        autoCompleteRelatedPersonAdapterAdapter = new AutoCompleteRelatedPersonAdapter(this, R.layout.item_option_autocomplete, autoCompletePersonTextView);
        autoCompletePersonTextView.setAdapter(autoCompleteRelatedPersonAdapterAdapter);
        autoCompletePersonTextView.setOnItemClickListener(autoCompleteOnClickListener());
        autoCompletePersonTextView.addTextChangedListener(autoCompleteTextWatcher());

        setupPatientMetadata();
        setupStillLoadingView();
        setupPatientRelationships();

        if (actionModeActive) {
            actionMode = startActionMode(new DeleteRelationshipsActionModeCallback());
            actionMode.setTitle(String.valueOf(getSelectedRelationships().size()));
        }
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

        lvwPatientRelationships.setAdapter(patientRelationshipsAdapter);
        lvwPatientRelationships.setClickable(true);
        lvwPatientRelationships.setLongClickable(true);
        lvwPatientRelationships.setEmptyView(noDataView);
        lvwPatientRelationships.setOnItemClickListener(listOnClickListener());
        lvwPatientRelationships.setOnItemLongClickListener(listOnLongClickListener());
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

    @Override
    protected void onResume() {
        super.onResume();
        themeUtils.onResume(this);
        patientRelationshipsAdapter.reloadData();
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

                if (actionModeActive) {
                    if (!relationship.getSynced()) {
                        int selectedRelationshipsCount = getSelectedRelationships().size();
                        if (selectedRelationshipsCount == 0 && actionModeActive)
                            actionMode.finish();
                        else
                            actionMode.setTitle(String.valueOf(selectedRelationshipsCount));
                    } else {
                        Toasty.warning(RelationshipsListActivity.this, getApplicationContext().getString(R.string.relationship_delete_fail), Toast.LENGTH_SHORT, true).show();
                        lvwPatientRelationships.setItemChecked(position, false);
                    }
                } else {

                    Patient relatedPerson;
                    try {
                        selectedRelatedPerson = null;
                        if (StringUtils.equals(relationship.getPersonA().getUuid(), patient.getUuid()))
                            relatedPerson = patientController.getPatientByUuid(relationship.getPersonB().getUuid());
                        else
                            relatedPerson = patientController.getPatientByUuid(relationship.getPersonA().getUuid());

                        if (relatedPerson != null) {
                            Intent intent = new Intent(RelationshipsListActivity.this, PatientSummaryActivity.class);

                            intent.putExtra(PatientSummaryActivity.PATIENT, relatedPerson);
                            startActivity(intent);
                        } else {
                            // We pick the right related person and create them as a patient
                            if (StringUtils.equalsIgnoreCase(patient.getUuid(), relationship.getPersonA().getUuid())) {
                                selectedRelatedPerson = relationship.getPersonB();
                            } else {
                                selectedRelatedPerson = relationship.getPersonA();
                            }
                            showAlertDialog();
                        }
                    } catch (PatientController.PatientLoadException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    private AdapterView.OnItemLongClickListener listOnLongClickListener() {
        return new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (!actionModeActive) {
                    Relationship relationship = (Relationship) parent.getItemAtPosition(position);

                    if (!relationship.getSynced()) {
                        actionMode = startActionMode(new DeleteRelationshipsActionModeCallback());
                        actionModeActive = true;

                        lvwPatientRelationships.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                        lvwPatientRelationships.setItemChecked(position, true);
                        actionMode.setTitle(String.valueOf(getSelectedRelationships().size()));
                    } else {
                        Toasty.warning(RelationshipsListActivity.this, getApplicationContext().getString(R.string.relationship_delete_fail), Toast.LENGTH_SHORT, true).show();
                        lvwPatientRelationships.setItemChecked(position, false);
                    }
                }
                return true;
            }
        };
    }

    private void createPatientFromRelatedPerson() {
        Intent intent = new Intent(this, RegistrationFormsActivity.class);
        Patient pat = new Patient();
        pat.setUuid(selectedRelatedPerson.getUuid());
        pat.setBirthdate(selectedRelatedPerson.getBirthdate());
        pat.setBirthdateEstimated(selectedRelatedPerson.getBirthdateEstimated());
        pat.setGender(selectedRelatedPerson.getGender());
        pat.setNames(selectedRelatedPerson.getNames());

        intent.putExtra(PatientSummaryActivity.PATIENT, pat);
        startActivity(intent);
    }

    private void showAlertDialog() {
        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setIcon(ThemeUtils.getIconWarning(this))
                .setTitle(getResources().getString(R.string.title_logout_confirm))
                .setMessage(getResources().getString(R.string.confirm_create_patient_from_person))
                .setPositiveButton(getString(R.string.general_yes), positiveClickListener())
                .setNegativeButton(getString(R.string.general_no), null)
                .create()
                .show();
    }

    private Dialog.OnClickListener positiveClickListener() {
        return new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                createPatientFromRelatedPerson();
            }
        };
    }

    private AdapterView.OnItemClickListener autoCompleteOnClickListener() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                selectedPerson = (Person) parent.getItemAtPosition(position);
                createPersonView.setVisibility(View.GONE);
                saveButton.setVisibility(View.VISIBLE);
            }
        };
    }

    private TextWatcher autoCompleteTextWatcher() {
        return new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                selectedPerson = null;
                saveButton.setVisibility(View.GONE);

                if (s.length() < 3)
                    searchServerView.setVisibility(View.GONE);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {}
        };
    }

    public void onFilterComplete(int count) {

        if (autoCompleteRelatedPersonAdapterAdapter.getSearchRemote()) {
            autoCompleteRelatedPersonAdapterAdapter.setSearchRemote(false);
            searchServerView.setVisibility(View.GONE);
            if (count < 1) {
                createPersonView.setVisibility(View.VISIBLE);
            }
        } else {
            // local search
            if (count < 1) {
                searchServerView.setVisibility(View.VISIBLE);
            } else {
                searchServerView.setVisibility(View.GONE);
            }
        }
    }

    private void closeNewRelationshipWindow(){
        // Show the existing relationships
        lvwPatientRelationships.setVisibility(View.VISIBLE);

        // Hide the add relationship view
        autoCompletePersonTextView.setText(StringUtils.EMPTY);
        addRelationshipView.setVisibility(View.GONE);
        createPersonView.setVisibility(View.GONE);
        searchServerView.setVisibility(View.GONE);

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
                relationshipController);

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
            Toasty.error(this, getString(R.string.warning_relationship_type_missing), Toast.LENGTH_LONG, true).show();
            return;
        }

        if (selectedPerson == null) {
            Toasty.error(this, getString(R.string.warning_related_person_missing), Toast.LENGTH_LONG, true).show();
            return;
        }

        if (StringUtils.equals(selectedPerson.getUuid(), patient.getUuid())) {
            Log.e(getClass().getSimpleName(), "Self relation is invalid");
            Toasty.error(this, getString(R.string.relationship_to_self_error), Toast.LENGTH_SHORT, true).show();
            return;
        }

        RelationshipTypeWrap relationshipTypeWrap = ((RelationshipTypeWrap) relationshipType.getSelectedItem());

        Relationship newRelationship;
        if (StringUtils.equals(relationshipTypeWrap.getSide(), "A"))
            newRelationship = new Relationship(patient, selectedPerson, relationshipTypeWrap.getRelationshipType(), false);
        else
            newRelationship = new Relationship(selectedPerson, patient, relationshipTypeWrap.getRelationshipType(), false);

        newRelationship.setUuid(UUID.randomUUID().toString());

        if (((MuzimaApplication) getApplicationContext()).getRelationshipController().relationshipExists(newRelationship)){
            Log.e(getClass().getSimpleName(), "Relationship already exists");
            Toasty.warning(this, getString(R.string.warning_relationship_exists,
                    relationshipTypeWrap.getRelationshipType().getAIsToB(),relationshipTypeWrap.getRelationshipType().getBIsToA(),
                     selectedPerson.getDisplayName()), Toast.LENGTH_LONG, true).show();
        } else {
            // we will create a formData payload and save the relationship ONLY on success
            try {
                RelationshipJsonMapper relationshipJsonMapper = new RelationshipJsonMapper(newRelationship, patient, patientController,
                        ((MuzimaApplication) getApplicationContext()).getAuthenticatedUser());
                ((MuzimaApplication) getApplicationContext()).getFormController().saveFormData(relationshipJsonMapper.createFormDataFromRelationship());
                newRelationship.setSynced(false);
                relationshipController.saveRelationship(newRelationship);
                patientRelationshipsAdapter.reloadData();
                closeNewRelationshipWindow();
                Toasty.success(this, getString(R.string.relationship_create_success), Toast.LENGTH_LONG, true).show();
            } catch (RelationshipController.SaveRelationshipException e) {
                Log.e(getClass().getSimpleName(), "Error saving new relationship");
            } catch (JSONException e) {
                Log.e("", "Error While Parsing data" + e);
            } catch (FormController.FormDataSaveException e) {
                Log.e("", "Error While Saving Form Data" + e);
            }
        }
    }

    public void cancelRelationshipAdd(View view) {
        closeNewRelationshipWindow();
    }

    public void searchServer(View view) {
        autoCompleteRelatedPersonAdapterAdapter.setSearchRemote(true);
        String tmpText = autoCompletePersonTextView.getText().toString();
        autoCompletePersonTextView.setText(tmpText);
    }

    public void createPerson(View view) {
        Intent intent = new Intent(this, RelationshipFormsActivity.class);
        intent.putExtra(RelationshipFormsActivity.PATIENT, patient);
        startActivity(intent);
    }

    // Methods to delete relationships
    final class DeleteRelationshipsActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            getMenuInflater().inflate(R.menu.actionmode_menu_delete, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            if (menuItem.getItemId() == R.id.menu_delete) {
                List<Relationship> selectedRelationships = getSelectedRelationships();
                patientRelationshipsAdapter.removeRelationshipsForPatient(patient.getUuid(), selectedRelationships);

                onCompleteOfFormDataDelete(selectedRelationships);
            }
            return false;
        }

        private void onCompleteOfRelationshipDelete(int numberOfDeletedRelationships) {
            endActionMode();
            patientRelationshipsAdapter.reloadData();
            Toasty.info(getApplicationContext(), getString(R.string.info_relationships_delete_success, numberOfDeletedRelationships), Toast.LENGTH_SHORT, true).show();
        }
        
        private void onCompleteOfFormDataDelete(List<Relationship> selectedRelationships) {
            FormController formController = ((MuzimaApplication) getApplicationContext()).getFormController();

            List<FormData> formDataList = new ArrayList<>();
            try {
                for (Relationship relationship : selectedRelationships) {
                    FormData formData = formController.getFormDataByUuid(relationship.getUuid());
                    if (formData != null)
                        formDataList.add(formData);
                }
                formController.deleteFormDataAndRelatedEncountersAndObs(formDataList);
            } catch (FormController.FormDataFetchException | FormController.FormDataDeleteException e) {
                e.printStackTrace();
            }

            onCompleteOfRelationshipDelete(selectedRelationships.size());
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            actionModeActive = false;
            lvwPatientRelationships.clearChoices();
            patientRelationshipsAdapter.notifyDataSetChanged();

            lvwPatientRelationships.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }
    }

    private void endActionMode() {
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    private List<Relationship> getSelectedRelationships() {
        List<Relationship> relationships = new ArrayList<>();
        SparseBooleanArray checkedItemPositions = lvwPatientRelationships.getCheckedItemPositions();
        for (int i = 0; i < checkedItemPositions.size(); i++) {
            if (checkedItemPositions.valueAt(i)) {
                relationships.add(((Relationship) lvwPatientRelationships.getItemAtPosition(checkedItemPositions.keyAt(i))));
            }
        }
        return relationships;
    }
}
