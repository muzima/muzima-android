/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */
package com.muzima.view.relationship;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import com.muzima.adapters.relationships.AutoCompleteRelatedPersonAdapter;
import com.muzima.adapters.relationships.RelationshipTypesAdapter;
import com.muzima.adapters.relationships.RelationshipsAdapter;
import com.muzima.api.model.FormData;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Person;
import com.muzima.api.model.Relationship;
import com.muzima.controller.FormController;
import com.muzima.controller.PatientController;
import com.muzima.controller.PersonController;
import com.muzima.controller.RelationshipController;
import com.muzima.model.relationship.RelationshipTypeWrap;
import com.muzima.utils.DateUtils;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.RelationshipJsonMapper;
import com.muzima.utils.StringUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.forms.PersonDemographicsUpdateFormsActivity;
import com.muzima.view.forms.RegistrationFormsActivity;
import com.muzima.view.patients.PatientSummaryActivity;

import androidx.appcompat.widget.Toolbar;
import es.dmoral.toasty.Toasty;
import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import static com.muzima.utils.DateUtils.getFormattedDate;

public class RelationshipsListActivity extends BroadcastListenerActivity implements ListAdapter.BackgroundListQueryTaskListener {
    private Patient patient;
    private RelationshipsAdapter patientRelationshipsAdapter;
    private RelationshipTypesAdapter relationshipTypesAdapter;
    private View noDataView;
    private View searchServerView;
    private View createPersonView;
    private View addRelationshipView;
    private View progressBarContainer;

    private TextView patientNameTextView;
    private ImageView patientGenderImageView;
    private TextView dobTextView;
    private TextView identifierTextView;
    private TextView ageTextView;

    private ListView lvwPatientRelationships;
    private AutoCompleteTextView autoCompletePersonTextView;
    private AutoCompleteRelatedPersonAdapter autoCompleteRelatedPersonAdapterAdapter;
    private TextView textViewInfo;
    private TextView textViewCreatePersonTip;
    private Spinner relationshipType;
    private Person selectedPerson;
    private Button saveButton;
    private RelationshipController relationshipController;
    private PatientController patientController;
    private Person selectedRelatedPerson;
    private boolean actionModeActive = false;
    private ActionMode actionMode;
    public static final String INDEX_PATIENT = "indexPatient";

    private boolean isSearching = false;
    private final LanguageUtil languageUtil = new LanguageUtil();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.getInstance().onCreate(this,true);
        languageUtil.onCreate(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_relationships);
        patient = (Patient) getIntent().getSerializableExtra(PatientSummaryActivity.PATIENT);
        addRelationshipView = findViewById(R.id.add_relationship);
        autoCompletePersonTextView = findViewById(R.id.search_related_person);
        textViewInfo = findViewById(R.id.info);
        textViewCreatePersonTip = findViewById(R.id.create_person_tip);
        relationshipType = findViewById(R.id.relationshipType);
        saveButton = findViewById(R.id.save);
        searchServerView = findViewById(R.id.search_server_layout);
        createPersonView = findViewById(R.id.create_person_layout);
        progressBarContainer = findViewById(R.id.progress_bar_container);

        relationshipController = ((MuzimaApplication) getApplicationContext()).getRelationshipController();
        patientController = ((MuzimaApplication) getApplicationContext()).getPatientController();

        autoCompleteRelatedPersonAdapterAdapter = new AutoCompleteRelatedPersonAdapter(this, R.layout.item_option_autocomplete, autoCompletePersonTextView);
        autoCompletePersonTextView.setAdapter(autoCompleteRelatedPersonAdapterAdapter);
        autoCompletePersonTextView.setOnItemClickListener(autoCompleteOnClickListener());
        autoCompletePersonTextView.addTextChangedListener(autoCompleteTextWatcher());

        loadPatientData();
        setupStillLoadingView();
        setupPatientRelationships();
        setTitle(R.string.general_relationships);

        if (actionModeActive) {
            actionMode = startActionMode(new DeleteRelationshipsActionModeCallback());
            actionMode.setTitle(String.valueOf(getSelectedRelationships().size()));
        }
    }

    private void loadPatientData() {
        patientNameTextView = findViewById(R.id.name);
        patientGenderImageView = findViewById(R.id.genderImg);
        dobTextView = findViewById(R.id.dateOfBirth);
        identifierTextView = findViewById(R.id.identifier);
        ageTextView = findViewById(R.id.age_text_label);
        if(patient != null) {
            patientNameTextView.setText(patient.getDisplayName());
        }else{
            patientNameTextView.setText("");
        }
        identifierTextView.setText(String.format(Locale.getDefault(), "ID:#%s", patient.getIdentifier()));
        dobTextView.setText(String.format("DOB: %s", new SimpleDateFormat("MM-dd-yyyy", Locale.getDefault()).format(patient.getBirthdate())));
        patientGenderImageView.setImageResource(getGenderImage(patient.getGender()));
        ageTextView.setText(String.format(Locale.getDefault(), "%d Yrs", DateUtils.calculateAge(patient.getBirthdate())));
    }

    private int getGenderImage(String gender) {
        return gender.equalsIgnoreCase("M") ? R.drawable.gender_male : R.drawable.gender_female;
    }

    private void setupPatientRelationships() {
        lvwPatientRelationships = findViewById(R.id.relationships_list);
        patientRelationshipsAdapter = new RelationshipsAdapter(this, R.layout.item_patients_list_multi_checkable, relationshipController,
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
    }

    private void setupStillLoadingView() {
        noDataView = findViewById(R.id.no_data_layout);
        TextView noDataMsgTextView = findViewById(R.id.no_data_msg);
        noDataMsgTextView.setText(R.string.general_loading_relationships);
    }

    @Override
    protected void onResume() {
        super.onResume();
        languageUtil.onResume(this);
        patientRelationshipsAdapter.reloadData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.relationship_list, menu);
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
                        TypedValue typedValue = new TypedValue();
                        Resources.Theme theme = getTheme();
                        theme.resolveAttribute(R.attr.primaryBackgroundColor, typedValue, true);

                        int selectedRelationshipsCount = getSelectedRelationships().size();
                        if (selectedRelationshipsCount == 0 && actionModeActive) {
                            actionMode.finish();
                            view.setBackgroundResource(typedValue.resourceId);
                        } else {
                            if(view.isActivated()){
                                view.setBackgroundResource(R.color.hint_blue_opaque);
                            } else {
                                view.setBackgroundResource(typedValue.resourceId);
                            }
                            actionMode.setTitle(String.valueOf(selectedRelationshipsCount));
                        }
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

                            intent.putExtra(PatientSummaryActivity.PATIENT_UUID, relatedPerson.getUuid());
                            startActivity(intent);
                        } else {
                            // We pick the right related person and create them as a patient
                            if (StringUtils.equalsIgnoreCase(patient.getUuid(), relationship.getPersonA().getUuid())) {
                                selectedRelatedPerson = relationship.getPersonB();
                            } else {
                                selectedRelatedPerson = relationship.getPersonA();
                            }
                            selectAction();
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
                Relationship relationship = (Relationship) parent.getItemAtPosition(position);
                if (!actionModeActive) {
                    if (!relationship.getSynced()) {
                        actionMode = startActionMode(new DeleteRelationshipsActionModeCallback());
                        actionModeActive = true;

                        lvwPatientRelationships.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                        lvwPatientRelationships.setItemChecked(position, true);
                        view.setBackgroundResource(R.color.hint_blue_opaque);
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
        intent.putExtra(INDEX_PATIENT, patient);
        startActivity(intent);
    }

    private void OpenUpdatePersonDemographicsForm() {
        Intent intent = new Intent(this, PersonDemographicsUpdateFormsActivity.class);
        intent.putExtra(PersonDemographicsUpdateFormsActivity.PERSON, selectedRelatedPerson);
        intent.putExtra(INDEX_PATIENT, patient);
        startActivity(intent);
    }

    private void selectAction(){
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(RelationshipsListActivity.this);
        builderSingle.setIcon(R.drawable.ic_accept);
        builderSingle.setTitle(R.string.hint_person_action_prompt);

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(RelationshipsListActivity.this, android.R.layout.simple_selectable_list_item);
        arrayAdapter.add(getString(R.string.info_convert_person_to_patient));
        arrayAdapter.add(getString(R.string.info_update_person_demographics));

        builderSingle.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String strName = arrayAdapter.getItem(which);
                if(getString(R.string.info_convert_person_to_patient).equals(strName)){
                    showAlertDialog();
                } else {
                    OpenUpdatePersonDemographicsForm();
                }
            }
        });
        builderSingle.show();
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
                closeSoftKeyboard();
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
                createPersonView.setVisibility(View.GONE);
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

    public void onFilterComplete(int count, boolean connectivityFailed) {
        progressBarContainer.setVisibility(View.GONE);
        isSearching = false;

        if (autoCompleteRelatedPersonAdapterAdapter.getSearchRemote()) {
            autoCompleteRelatedPersonAdapterAdapter.setSearchRemote(false);
            searchServerView.setVisibility(View.GONE);
            if (count < 1) {
                if (connectivityFailed)
                    textViewCreatePersonTip.setText(getString(R.string.autocomplete_server_not_found));
                else
                    textViewCreatePersonTip.setText(getString(R.string.info_client_remote_search_not_found));

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
        // close the keyboard if open
        closeSoftKeyboard();

        // Show the existing relationships
        lvwPatientRelationships.setVisibility(View.VISIBLE);

        // Hide the add relationship view
        autoCompletePersonTextView.setText(StringUtils.EMPTY);
        addRelationshipView.setVisibility(View.GONE);
        createPersonView.setVisibility(View.GONE);
        searchServerView.setVisibility(View.GONE);
        progressBarContainer.setVisibility(View.GONE);
    }

    private void closeSoftKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                View v = getCurrentFocus();
                if (v == null)
                    v = new View(this);
                imm.hideSoftInputFromWindow(Objects.requireNonNull(v).getWindowToken(), 0);
            }
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

        relationshipTypesAdapter = new RelationshipTypesAdapter(this, R.layout.item_simple_spinner,
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
                RelationshipJsonMapper relationshipJsonMapper = new RelationshipJsonMapper( (MuzimaApplication) getApplicationContext());
                ((MuzimaApplication) getApplicationContext()).getFormController()
                        .saveFormData(relationshipJsonMapper.createFormDataFromRelationship(patient, newRelationship));
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            // create person form

            if (resultCode == 0 && data != null) {
                if (data.hasExtra(PatientSummaryActivity.PATIENT)) {
                    Patient patient = (Patient) data.getSerializableExtra(PatientSummaryActivity.PATIENT);
                    PersonController personController = ((MuzimaApplication) getApplicationContext()).getPersonController();

                    try {
                        closeSoftKeyboard();
                        Person p = personController.getPersonByUuid(patient.getUuid());
                        if (p != null) {
                            autoCompletePersonTextView.setText(p.getDisplayName(), false);
                            createPersonView.setVisibility(View.GONE);
                            selectedPerson = p;
                            saveButton.setVisibility(View.VISIBLE);
                        }
                    } catch (PersonController.PersonLoadException e) {
                        e.printStackTrace();
                        closeNewRelationshipWindow();
                    }
                }
            } else {
                closeNewRelationshipWindow();
            }
        }
    }

    public void cancelRelationshipAdd(View view) {
        closeNewRelationshipWindow();
    }

    public void searchServer(View view) {
        if (isSearching) return;

        closeSoftKeyboard();
        searchServerView.setVisibility(View.GONE);
        progressBarContainer.setVisibility(View.VISIBLE);

        isSearching = true;
        autoCompleteRelatedPersonAdapterAdapter.setSearchRemote(true);
        String tmpText = autoCompletePersonTextView.getText().toString();
        autoCompletePersonTextView.setText(tmpText);
    }

    public void createPerson(View view) {
        Intent intent = new Intent(this, RelationshipFormsActivity.class);
        intent.putExtra(INDEX_PATIENT, patient);
        startActivityForResult(intent, 1);
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
            clearSelectedRelationships();
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

    private void clearSelectedRelationships() {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getTheme();
        theme.resolveAttribute(R.attr.primaryBackgroundColor, typedValue, true);

        SparseBooleanArray checkedItemPositions = lvwPatientRelationships.getCheckedItemPositions();
        for (int i = 0; i < checkedItemPositions.size(); i++) {
            if (checkedItemPositions.valueAt(i)) {
                lvwPatientRelationships.getChildAt(checkedItemPositions.keyAt(i)).setBackgroundResource(typedValue.resourceId);
            }
        }
    }


}
