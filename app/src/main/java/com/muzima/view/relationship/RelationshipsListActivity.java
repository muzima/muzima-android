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

import static com.muzima.view.patients.PatientSummaryActivity.CALLING_ACTIVITY;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.adapters.RecyclerAdapter;
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
import com.muzima.tasks.MuzimaAsyncTask;
import com.muzima.utils.DateUtils;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.RelationshipJsonMapper;
import com.muzima.utils.StringUtils;
import com.muzima.utils.TagsUtil;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.patients.PatientSummaryActivity;
import com.muzima.view.patients.UpdatePatientTagsIntent;

import com.muzima.utils.RelationshipViewUtil;
import es.dmoral.toasty.Toasty;

import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;


public class RelationshipsListActivity extends BroadcastListenerActivity implements ListAdapter.BackgroundListQueryTaskListener, RecyclerAdapter.BackgroundListQueryTaskListener, RelationshipsAdapter.RelationshipListClickListener {
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

    private RecyclerView lvwPatientRelationships;
    private AutocompleteRelatedPersonTextView autoCompletePersonTextView;
    private AutoCompleteRelatedPersonAdapter autoCompleteRelatedPersonAdapterAdapter;
    private TextView textViewInfo;
    private TextView textViewCreatePersonTip;
    private Spinner relationshipType;
    private Person selectedPerson;
    private Button saveButton;
    private Button createPersonButton;
    private Button searchServerButton;
    private RelationshipController relationshipController;
    private PatientController patientController;
    private boolean actionModeActive = false;
    private ActionMode actionMode;
    public static final String INDEX_PATIENT = "indexPatient";

    private boolean isSearching = false;
    private boolean wasServerSearch = false;
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
        createPersonButton = findViewById(R.id.create_person_button);
        searchServerButton = findViewById(R.id.search_server_button);

        if(((MuzimaApplication) getApplicationContext()).getMuzimaSettingController().isFGHCustomClientSummaryEnabled()){
           createPersonButton.setText(R.string.general_create_contact);
        }else{
            createPersonButton.setText(R.string.general_person_create);
        }

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

        LinearLayout tagsLayout = findViewById(R.id.menu_tags);
        TagsUtil.loadTags(patient, tagsLayout, getApplicationContext());
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
        if(patient.getBirthdate() != null) {
            dobTextView.setText(getString(R.string.general_date_of_birth ,String.format(" %s", new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(patient.getBirthdate()))));
            ageTextView.setText(getString(R.string.general_years ,String.format(Locale.getDefault(), "%d ", DateUtils.calculateAge(patient.getBirthdate()))));
        }
        patientGenderImageView.setImageResource(getGenderImage(patient.getGender()));
    }

    private int getGenderImage(String gender) {
        return gender.equalsIgnoreCase("M") ? R.drawable.gender_male : R.drawable.gender_female;
    }

    private void setupPatientRelationships() {
        lvwPatientRelationships = findViewById(R.id.relationships_list);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        lvwPatientRelationships.setLayoutManager(linearLayoutManager);
        patientRelationshipsAdapter = new RelationshipsAdapter(this, R.layout.item_patients_list_multi_checkable, relationshipController,
                patient.getUuid(), patientController);
        patientRelationshipsAdapter.setBackgroundListQueryTaskListener(this);

        lvwPatientRelationships.setAdapter(patientRelationshipsAdapter);
        lvwPatientRelationships.setClickable(true);
        lvwPatientRelationships.setLongClickable(true);
        patientRelationshipsAdapter.setRelationshipListClickListener(this);
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
        else if (item.getItemId() == android.R.id.home) {
                Intent intent = new Intent(this.getApplicationContext(), PatientSummaryActivity.class);
                intent.putExtra(CALLING_ACTIVITY, RelationshipsListActivity.class.getSimpleName());
                if(patient != null)
                    intent.putExtra(PatientSummaryActivity.PATIENT_UUID, patient.getUuid());
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private AdapterView.OnItemClickListener autoCompleteOnClickListener() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                closeSoftKeyboard();
                selectedPerson = (Person) parent.getItemAtPosition(position);

                if(wasServerSearch) {
                    new MuzimaAsyncTask<Void, Void, Void>() {
                        @Override
                        protected void onPreExecute() {}

                        @Override
                        protected Void doInBackground(Void... voids) {
                            ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService()
                                    .downloadObservationsForPatientsByPatientUUIDs(new ArrayList<String>() {{
                                        add(selectedPerson.getUuid());
                                    }}, true);
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void unused) {}

                        @Override
                        protected void onBackgroundError(Exception e) {}
                    }.execute();
                }

                createPersonView.setVisibility(View.GONE);
                createPersonButton.setVisibility(View.GONE);
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
                createPersonButton.setVisibility(View.GONE);

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
            wasServerSearch = true;
            autoCompleteRelatedPersonAdapterAdapter.setSearchRemote(false);
            searchServerView.setVisibility(View.GONE);
            searchServerButton.setVisibility(View.GONE);
            if (count < 1) {
                textViewCreatePersonTip.setVisibility(View.VISIBLE);
                if (connectivityFailed)
                    textViewCreatePersonTip.setText(getString(R.string.autocomplete_server_not_found));
                else
                    textViewCreatePersonTip.setText(getString(R.string.info_client_remote_search_not_found));
            } else {
                textViewCreatePersonTip.setVisibility(View.GONE);
            }
            createPersonView.setVisibility(View.VISIBLE);
            createPersonButton.setVisibility(View.VISIBLE);
        } else {
            // local search
            wasServerSearch = false;
            searchServerButton.setVisibility(View.VISIBLE);
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
        createPersonButton.setVisibility(View.GONE);
        textViewCreatePersonTip.setVisibility(View.GONE);
        searchServerView.setVisibility(View.GONE);
        searchServerButton.setVisibility(View.GONE);
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
        textViewInfo.setText(getString(R.string.general_is_a ,String.format("%s", patient.getDisplayName())));
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

                List<String> relatedPatientsuuids = new ArrayList<>();
                relatedPatientsuuids.add(patient.getUuid());
                initiatePatientTagsUpdate(relatedPatientsuuids);

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

    private void initiatePatientTagsUpdate(List<String> patientUuidList){
        UpdatePatientTagsIntent updatePatientTagsIntent = new UpdatePatientTagsIntent(getApplicationContext(),patientUuidList);
        updatePatientTagsIntent.start();
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
                            createPersonButton.setVisibility(View.GONE);
                            selectedPerson = p;
                            saveButton.setVisibility(View.VISIBLE);
                        }
                    } catch (PersonController.PersonLoadException e) {
                        Log.e(getClass().getSimpleName(),"Encountered an exception",e);
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
        searchServerButton.setVisibility(View.GONE);
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
                Log.e(getClass().getSimpleName(),"Encountered an exception",e);
            }

            onCompleteOfRelationshipDelete(selectedRelationships.size());
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            actionModeActive = false;
            clearSelectedRelationships();
            patientRelationshipsAdapter.resetSelectedRelationships();
            patientRelationshipsAdapter.notifyDataSetChanged();
        }
    }

    private void endActionMode() {
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    private List<Relationship> getSelectedRelationships() {
        List<Relationship> relationships = patientRelationshipsAdapter.getSelectedRelationships();
        return relationships;
    }

    private void clearSelectedRelationships() {
        patientRelationshipsAdapter.resetSelectedRelationships();
    }

    @Override
    public void onItemLongClick(View view, int position) {
        Relationship relationship = patientRelationshipsAdapter.getRelationship(position);
        if (!actionModeActive) {
            if (!relationship.getSynced()) {
                actionMode = startActionMode(new DeleteRelationshipsActionModeCallback());
                actionModeActive = true;
                patientRelationshipsAdapter.toggleSelection(view, position);
                lvwPatientRelationships.setSelected(true);
                view.setBackgroundResource(R.color.hint_blue_opaque);
                actionMode.setTitle(String.valueOf(getSelectedRelationships().size()));
            } else {
                Toasty.warning(RelationshipsListActivity.this, getApplicationContext().getString(R.string.relationship_delete_fail), Toast.LENGTH_SHORT, true).show();
                lvwPatientRelationships.setSelected(false);
            }
        }
    }

    @Override
    public void onItemClick(View view, int position) {
        Relationship relationship = patientRelationshipsAdapter.getRelationship(position);
        RelationshipViewUtil.listOnClickListeners(this,((MuzimaApplication) getApplicationContext()), patient, false,lvwPatientRelationships, view, relationship, patientRelationshipsAdapter);
    }
}
