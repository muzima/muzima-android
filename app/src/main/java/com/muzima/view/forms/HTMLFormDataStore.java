/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.CohortMember;
import com.muzima.api.model.Concept;
import com.muzima.api.model.DerivedConcept;
import com.muzima.api.model.DerivedObservation;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.FormData;
import com.muzima.api.model.Location;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PatientIdentifier;
import com.muzima.api.model.PatientTag;
import com.muzima.api.model.Person;
import com.muzima.api.model.PersonAttribute;
import com.muzima.api.model.PersonTag;
import com.muzima.api.model.Provider;
import com.muzima.api.model.Relationship;
import com.muzima.api.model.RelationshipType;
import com.muzima.api.model.SetupConfigurationTemplate;
import com.muzima.controller.CohortController;
import com.muzima.controller.ConceptController;
import com.muzima.controller.DerivedConceptController;
import com.muzima.controller.DerivedObservationController;
import com.muzima.controller.FormController;
import com.muzima.controller.LocationController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.controller.PatientController;
import com.muzima.controller.PersonController;
import com.muzima.controller.ProviderController;
import com.muzima.controller.RelationshipController;
import com.muzima.controller.SetupConfigurationController;
import com.muzima.domain.Credentials;
import com.muzima.model.location.MuzimaGPSLocation;
import com.muzima.scheduler.RealTimeFormUploader;
import com.muzima.service.HTMLFormObservationCreator;
import com.muzima.service.MuzimaGPSLocationService;
import com.muzima.service.MuzimaLoggerService;
import com.muzima.utils.Constants;
import com.muzima.utils.DateUtils;
import com.muzima.utils.NetworkUtils;
import com.muzima.utils.RelationshipJsonMapper;
import com.muzima.utils.StringUtils;

import net.minidev.json.JSONValue;

import org.json.JSONException;

import com.muzima.controller.EncounterController;
import com.muzima.view.MainDashboardActivity;
import com.muzima.view.patients.UpdatePatientTagsIntent;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

import com.muzima.utils.ConceptUtils;
import com.muzima.view.relationship.RelationshipsListActivity;


class HTMLFormDataStore {

    private final HTMLFormWebViewActivity formWebViewActivity;
    private final FormController formController;
    private final LocationController locationController;
    private final ConceptController conceptController;
    private final ObservationController observationController;
    private final FormData formData;
    private final ProviderController providerController;
    private final EncounterController encounterController;
    private final MuzimaApplication application;
    private final MuzimaSettingController settingController;
    private final CohortController cohortController;
    private final PatientController patientController;
    private final PersonController personController;
    private final DerivedConceptController derivedConceptController;
    private final DerivedObservationController derivedObservationController;

    private String selectedPatientsUuids;

    public HTMLFormDataStore(HTMLFormWebViewActivity formWebViewActivity, FormData formData, boolean isFormReload, MuzimaApplication application) {
        this.formWebViewActivity = formWebViewActivity;
        this.formData = formData;

        this.formController = application.getFormController();
        this.providerController = application.getProviderController();
        this.locationController = application.getLocationController();
        this.settingController = application.getMuzimaSettingController();
        this.conceptController = application.getConceptController();
        this.encounterController = application.getEncounterController();
        this.observationController = application.getObservationController();
        this.cohortController = application.getCohortController();
        this.patientController = application.getPatientController();
        this.personController = application.getPersonController();
        this.derivedConceptController = application.getDerivedConceptController();
        this.derivedObservationController = application.getDerivedObservationController();
        this.application = application;
        logFormStartEvent(isFormReload);
    }

    @JavascriptInterface
    public String getStatus() {
        return formData == null ? StringUtils.EMPTY : formData.getStatus();
    }

    @JavascriptInterface
    public void saveHTML(String jsonPayload, String status) {
        saveHTML(jsonPayload, status, false);
    }

    @JavascriptInterface
    public void saveHTML(String jsonPayload, final String status, boolean keepFormOpen) {String selectedPatients = getSelectedPatientsUuids();
        final MuzimaApplication applicationContext = (MuzimaApplication) formWebViewActivity.getApplicationContext();
        if (selectedPatients.equals("[]") || selectedPatients.equals("")) {
            processForm(jsonPayload, status, keepFormOpen, formData);
        } else {
            List<Patient> patients = getPatientsFromCommaSeparatedString(selectedPatients);
            for (int i = 0; i < patients.size(); i++) {
                Patient patient = patients.get(i);
                String separatePatientJsonPayload = setPatientInfoToThePayload(patient, jsonPayload);
                final String patientUuid = patient.getUuid();
                FormData formDatas = new FormData() {{
                    setUuid(UUID.randomUUID().toString());
                    setPatientUuid(patientUuid);
                    setUserSystemId(applicationContext.getAuthenticatedUser().getSystemId());
                    setUserUuid(applicationContext.getAuthenticatedUser().getUuid());
                    setStatus(status);
                    setTemplateUuid(formData.getTemplateUuid());
                    setDiscriminator(formData.getDiscriminator());
                }};

                if (i == (patients.size() - 1))
                    processForm(separatePatientJsonPayload, Constants.STATUS_COMPLETE, false, formDatas);
                else
                    processForm(separatePatientJsonPayload, Constants.STATUS_COMPLETE, true, formDatas);
            }

            Intent intent = new Intent(applicationContext, MainDashboardActivity.class);
            applicationContext.getApplicationContext().startActivity(intent);
        }
    }

    public void processForm(String jsonPayload, String status, boolean keepFormOpen, FormData formData) {
        jsonPayload = injectUserSystemIdToEncounterPayload(jsonPayload);
        jsonPayload = injectTimeZoneToEncounterPayload(jsonPayload);
        jsonPayload = injectActiveSetupConfigUuidToEncounterPayload(jsonPayload);
        formData.setJsonPayload(jsonPayload);
        formData.setStatus(status);

        final String patientUuid = formData.getPatientUuid();
        boolean encounterDetailsValidityStatus = true;

        String errorMsg = isValidForm(jsonPayload, status, true, formData);
        if (!StringUtils.isEmpty(errorMsg)) {
            Toast.makeText(formWebViewActivity, errorMsg, Toast.LENGTH_LONG).show();
            Log.e(getClass().getSimpleName(), errorMsg);
            return;
        }

        try {
            if (status.equals("complete")) {
                encounterDetailsValidityStatus = areMandatoryEncounterDetailsInForm(jsonPayload);
            }

            if (encounterDetailsValidityStatus) {
                if (isRegistrationComplete(status)) {
                    Patient newPatient = formController.createNewPatient(application, formData);
                    formData.setPatientUuid(newPatient.getUuid());
                    formWebViewActivity.startPatientSummaryView(newPatient);
                    initiatePatientTagsUpdate(new ArrayList<String>() {{
                        add(patientUuid);
                    }});
                }
                if (formData.getDiscriminator() != null && (formData.getDiscriminator().equals(Constants.FORM_JSON_DISCRIMINATOR_RELATIONSHIP))) {
                    formData.setDiscriminator(Constants.FORM_JSON_DISCRIMINATOR_INDIVIDUAL_OBS);
                    parseObsFromCompletedForm(jsonPayload, status, true);
                } else if (formData.getDiscriminator() != null &&
                        (formData.getDiscriminator().equals(Constants.FORM_JSON_DISCRIMINATOR_INDIVIDUAL_OBS))) {

                    if (personController.getPersonByUuid(patientUuid) != null) {
                        parseObsFromCompletedForm(jsonPayload, status, true);
                    } else {
                        parseObsFromCompletedForm(jsonPayload, status, false);
                    }
                } else if (formData.getDiscriminator() != null &&
                        (formData.getDiscriminator().equals(Constants.FORM_JSON_DISCRIMINATOR_PERSON_UPDATE))) {

                    Person updatePerson = personController.getPersonByUuid(patientUuid);
                    if (updatePerson != null) {
                        formController.updatePerson(application, formData);
                        parseObsFromCompletedForm(jsonPayload, status, true);
                    } else {
                        formController.updatePatient(application, formData);
                        parseObsFromCompletedForm(jsonPayload, status, false);

                        initiatePatientTagsUpdate(new ArrayList<String>() {{
                            add(patientUuid);
                        }});
                    }
                } else if (status.equals("complete") && formData.getDiscriminator() != null &&
                        formData.getDiscriminator().equals(Constants.FORM_JSON_DISCRIMINATOR_DEMOGRAPHICS_UPDATE)) {
                    Patient updatedPatient = formController.updatePatient(application, formData);
                    if (updatedPatient != null) {
                        parseObsFromCompletedForm(jsonPayload, status, false);
                        formWebViewActivity.startPatientSummaryView(updatedPatient);
                        initiatePatientTagsUpdate(new ArrayList<String>() {{
                            add(patientUuid);
                        }});
                    }
                } else {
                    parseObsFromCompletedForm(jsonPayload, status, false);
                }

                Date encounterDate = getEncounterDateFromForm(jsonPayload);
                formData.setEncounterDate(encounterDate);
                formController.saveFormData(formData);
                formWebViewActivity.setResult(FormsWithDataActivity.RESULT_OK);
                if (status.equals("complete")) {
                    JSONObject jsonObject = new JSONObject(jsonPayload);
                    JSONObject jsonObjectInner = jsonObject.getJSONObject("patient");
                    Log.e(getClass().getSimpleName(), jsonObjectInner.toString());
                    if (jsonObjectInner.has("patient.tagName") && jsonObjectInner.has("patient.tagUuid")) {
                        Log.e(getClass().getSimpleName(), "Form Has both tag fields");
                        Person person = personController.getPersonByUuid(patientUuid);

                        Patient patientWithUuid = patientController.getPatientByUuid(patientUuid);
                        if(patientWithUuid != null){
                            List<PatientTag> existingTags = new ArrayList<>();

                            try {
                                existingTags = patientController.getAllTags();
                            } catch (PatientController.PatientLoadException e) {
                                Log.e(getClass().getSimpleName(), "Encountered an exception", e);
                            }

                            List<PatientTag> tags = new ArrayList<PatientTag>();
                            Patient patient = patientController.getPatientByUuid(patientUuid);

                            if (patient.getTags() != null) {
                                tags = new ArrayList<>(Arrays.asList(patient.getTags()));
                            }

                            //Remove AA patient tag to be replaced by the AL/NA tags
                            PatientTag AATag = null;
                            for (PatientTag patientTag : tags) {
                                if (patientTag.getName().equals("AA")) {
                                    AATag = patientTag;
                                }
                            }

                            if (AATag != null) {
                                tags.remove(AATag);
                            }


                            String tagName = jsonObjectInner.getString("patient.tagName");
                            PatientTag tag = null;
                            for (PatientTag existingTag : existingTags) {
                                if (StringUtils.equals(existingTag.getName(), tagName)) {
                                    tag = existingTag;
                                }
                            }

                            if (tag == null) {
                                tag = new PatientTag();
                                tag.setName(tagName);
                                tag.setUuid(jsonObjectInner.getString("patient.tagUuid"));
                                if (jsonObjectInner.has("patient.tagDescription")) {
                                    tag.setDescription(jsonObjectInner.getString("patient.tagDescription"));
                                }
                                existingTags.add(tag);
                                patientController.savePatientTags(tag);
                            }

                            tags.add(tag);

                            patient.setTags(tags.toArray(new PatientTag[tags.size()]));
                            patientController.updatePatient(patient);
                        } else {
                            List<PersonTag> existingTags = new ArrayList<>();
                            try {
                                existingTags = personController.getAllPersonTags();
                            }catch (PersonController.PersonLoadException e){
                                Log.e(getClass().getSimpleName(), "Encountered an exception", e);
                            }catch (IOException e){
                                Log.e(getClass().getSimpleName(), "Encountered an exception", e);
                            }


                            List<PersonTag> tags = new ArrayList<>();

                            if (person.getPersonTags() != null) {
                                tags = new ArrayList<>(Arrays.asList(person.getPersonTags()));
                            }


                            String tagName = jsonObjectInner.getString("patient.tagName");
                            PersonTag tag = null;
                            for (PersonTag existingTag : existingTags) {
                                if (StringUtils.equals(existingTag.getName(), tagName)) {
                                    tag = existingTag;
                                }
                            }

                            if (tag == null) {
                                tag = new PersonTag();
                                tag.setName(tagName);
                                tag.setUuid(jsonObjectInner.getString("patient.tagUuid"));
                                if (jsonObjectInner.has("patient.tagDescription")) {
                                    tag.setDescription(jsonObjectInner.getString("patient.tagDescription"));
                                }
                                existingTags.add(tag);
                                personController.savePersonTags(tag);
                            }

                            tags.add(tag);

                            person.setPersonTags(tags.toArray(new PersonTag[tags.size()]));
                            personController.updatePerson(person);
                        }
                    }

                    if (formData.getDiscriminator() != null &&
                            (formData.getDiscriminator().equals(Constants.FORM_JSON_DISCRIMINATOR_PERSON_UPDATE))) {
                        Patient indexPatient = (Patient) formWebViewActivity.getIntent().getSerializableExtra(RelationshipsListActivity.INDEX_PATIENT);
                        if (indexPatient != null) {
                            initiatePatientTagsUpdate(new ArrayList<String>() {{
                                add(indexPatient.getUuid());
                            }});
                        }
                    }
                }
                if (!keepFormOpen) {
                    formWebViewActivity.finish();
                    if (status.equals("complete")) {
                        Toast.makeText(formWebViewActivity, formWebViewActivity.getString(R.string.info_form_data_save_success), Toast.LENGTH_SHORT).show();
                        RealTimeFormUploader.getInstance().uploadAllCompletedForms(formWebViewActivity.getApplicationContext(), false);
                    }
                    if (status.equals("incomplete")) {
                        Toast.makeText(formWebViewActivity, formWebViewActivity.getString(R.string.info_draft_form_save_success), Toast.LENGTH_SHORT).show();
                    }
                    logFormSaveEvent(status);
                }
            } else {
                String missingMandatoryEncounterDetailsMessage = checkMissingMandatoryEncounterDetails(jsonPayload);
                String message = missingMandatoryEncounterDetailsMessage.concat(" ");
                message = message.concat(formWebViewActivity.getString(R.string.message_missing_form_encounter_details_error));

                formWebViewActivity.showMissingEncounterDetailsDialog(message);
            }
        } catch (FormController.FormDataSaveException e) {
            Toast.makeText(formWebViewActivity, formWebViewActivity.getString(R.string.error_form_save), Toast.LENGTH_SHORT).show();
            Log.e(getClass().getSimpleName(), "Exception occurred while saving form data", e);
        } catch (PatientController.PatientLoadException e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while fetching patient", e);
        } catch (PersonController.PersonLoadException e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while fetching person", e);
        } catch (PatientController.PatientSaveException e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while saving patient", e);
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while parsing object", e);
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while saving tags", e);
        } catch (FormController.FormDataProcessException e) {
            Toast.makeText(formWebViewActivity, formWebViewActivity.getString(R.string.error_form_data_processing), Toast.LENGTH_SHORT).show();
            Log.e(getClass().getSimpleName(), "Exception occurred while processing form data", e);
        }
    }

    private void initiatePatientTagsUpdate(List<String> patientUuidList) {
        UpdatePatientTagsIntent updatePatientTagsIntent = new UpdatePatientTagsIntent(application, patientUuidList);
        updatePatientTagsIntent.start();
    }

    @JavascriptInterface
    public String getLocationNamesFromDevice() {
        List<Location> locationsOnDevice = new ArrayList<>();
        try {
            locationsOnDevice = locationController.getAllLocations();
        } catch (LocationController.LocationLoadException e) {
            Toast.makeText(formWebViewActivity, formWebViewActivity.getString(R.string.error_form_location_load), Toast.LENGTH_SHORT).show();
            Log.e(getClass().getSimpleName(), "Exception occurred while loading locations", e);
        }
        return JSONValue.toJSONString(locationsOnDevice);
    }

    @JavascriptInterface
    public String getRelationships(String patientUuid) {
        JSONArray relationshipsJsonArray = new JSONArray();
        RelationshipController relationshipController = ((MuzimaApplication) formWebViewActivity.getApplicationContext()).getRelationshipController();
        try {
            List<Relationship> relationships = relationshipController.getRelationshipsForPerson(patientUuid);

            for (Relationship relationship : relationships) {
                JSONObject relationshipJsonObject = new JSONObject();
                relationshipJsonObject.put("personA", relationship.getPersonA().getUuid());
                relationshipJsonObject.put("personB", relationship.getPersonB().getUuid());
                relationshipJsonObject.put("relationshipType", relationship.getRelationshipType().getUuid());

                relationshipsJsonArray.put(relationshipJsonObject);
            }
        } catch (RelationshipController.RetrieveRelationshipException e) {
            Log.e(getClass().getSimpleName(), "Could not retrieve relationships", e);
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "Could not build relationships JSON", e);
        }
        return relationshipsJsonArray.toString();
    }

    @JavascriptInterface
    public String getRelationshipForPersons(String person1Uuid, String person2Uuid) {
        JSONArray relationshipsJsonArray = new JSONArray();
        RelationshipController relationshipController = ((MuzimaApplication) formWebViewActivity.getApplicationContext()).getRelationshipController();
        try {
            List<Relationship> relationships = relationshipController.getRelationshipsForPerson(person1Uuid);

            for (Relationship relationship : relationships) {
                if (StringUtils.equals(relationship.getPersonA().getUuid(), person2Uuid) ||
                        StringUtils.equals(relationship.getPersonB().getUuid(), person2Uuid)) {
                    JSONObject relationshipJsonObject = new JSONObject();
                    relationshipJsonObject.put("personA", relationship.getPersonA().getUuid());
                    relationshipJsonObject.put("personB", relationship.getPersonB().getUuid());
                    relationshipJsonObject.put("relationshipType", relationship.getRelationshipType().getUuid());
                    relationshipJsonObject.put("relationshipUuid", relationship.getUuid());

                    relationshipsJsonArray.put(relationshipJsonObject);
                }
            }
        } catch (RelationshipController.RetrieveRelationshipException e) {
            Log.e(getClass().getSimpleName(), "Could not retrieve relationships", e);
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "Could not build relationships JSON", e);
        }
        return relationshipsJsonArray.toString();
    }

    @JavascriptInterface
    public String getRelationshipTypesFromDevice() {
        JSONArray relationshipsJsonArray = new JSONArray();
        try {
            List<RelationshipType> relationshipTypeList = application.getRelationshipController().getAllRelationshipTypes();
            for (RelationshipType relationshipType : relationshipTypeList) {
                try {
                    JSONObject relationshipJsonObject = new JSONObject();
                    relationshipJsonObject.put("uuid", relationshipType.getUuid());
                    relationshipJsonObject.put("AIsToB", relationshipType.getAIsToB());
                    relationshipJsonObject.put("BIsToA", relationshipType.getBIsToA());
                    relationshipsJsonArray.put(relationshipJsonObject);
                } catch (JSONException e) {
                    Log.e(getClass().getSimpleName(), "Exception occurred while populating relationship", e);
                }
            }
        } catch (RelationshipController.RetrieveRelationshipTypeException e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while loading relationships", e);
        }
        return relationshipsJsonArray.toString();

    }

    @JavascriptInterface
    public String getPersonAttribute(String patientUuid, String attributeTypeNameOrUuid) {
        JSONObject attributeJSONObject = new JSONObject();
        try {
            Person person = patientController.getPatientByUuid(patientUuid);
            if (person == null)
                person = personController.getPersonByUuid(patientUuid);

            if (person != null) {
                PersonAttribute attribute = person.getAttribute(attributeTypeNameOrUuid);
                if (attribute != null) {
                    attributeJSONObject.put("attribute_type_uuid", attribute.getAttributeType().getUuid());
                    attributeJSONObject.put("attribute_type_name", attribute.getAttributeType().getName());
                    attributeJSONObject.put("attribute_value", attribute.getAttribute());
                }
            }
        } catch (PersonController.PersonLoadException | PatientController.PatientLoadException | JSONException e) {
            Log.e(getClass().getSimpleName(), "Could not retrieve patient record", e);
        }
        return attributeJSONObject.toString();
    }

    @JavascriptInterface
    public String getPatientIdentifier(String patientUuid, String identifierType) {
        JSONObject identifierJSONObject = new JSONObject();
        try {
            Patient patient = patientController.getPatientByUuid(patientUuid);
            if (patient != null) {
                PatientIdentifier identifier = patient.getIdentifier(identifierType);
                if (identifier != null) {
                    identifierJSONObject.put("identifier_type_uuid", identifier.getIdentifierType().getUuid());
                    identifierJSONObject.put("identifier_type_name", identifier.getIdentifierType().getName());
                    identifierJSONObject.put("identifier_value", identifier.getIdentifier());
                }
            }
        } catch (PatientController.PatientLoadException | JSONException e) {
            Log.e(getClass().getSimpleName(), "Could not retrieve patient record", e);
        }
        return identifierJSONObject.toString();
    }

    @JavascriptInterface
    public String getPatientDetailsFromServerByUuid(String uuid) {
        JSONObject patientJsonObject = new JSONObject();
        try {
            Patient patient = patientController.downloadPatientByUUID(uuid);
            if (patient != null) {
                patientController.savePatient(patient);

                application.getMuzimaSyncService().downloadObservationsForPatientsByPatientUUIDs(new ArrayList<String>() {{
                    add(uuid);
                }}, true);

                patientJsonObject.put("uuid", patient.getUuid());
                patientJsonObject.put("name", patient.getDisplayName());
                patientJsonObject.put("family_name", patient.getFamilyName());
                patientJsonObject.put("middle_name", patient.getMiddleName());
                patientJsonObject.put("given_name", patient.getGivenName());
                patientJsonObject.put("identifier", patient.getIdentifier());
                patientJsonObject.put("birth_date", DateUtils.getFormattedDate(patient.getBirthdate()));
                patientJsonObject.put("birthdate_estimated", Boolean.toString(patient.getBirthdateEstimated()));
                patientJsonObject.put("sex", patient.getGender());
                patientJsonObject.put("attributes", patient.getAtributes());
                patientJsonObject.put("addresses", patient.getAddresses());
            }
        } catch (PatientController.PatientDownloadException | JSONException |
                 PatientController.PatientSaveException e) {
            Log.e(getClass().getSimpleName(), "Could not download patient record", e);
        }
        return patientJsonObject.toString();
    }

    @JavascriptInterface
    public String getPersonDetailsFromDeviceByUuid(String uuid) {
        JSONObject personJsonObject = new JSONObject();

        try {
            Person person = personController.getPersonByUuid(uuid);
            if (person == null) {
                person = patientController.getPatientByUuid(uuid);
            }

            if (person != null) {
                personJsonObject.put("uuid", person.getUuid());
                personJsonObject.put("name", person.getDisplayName());
                personJsonObject.put("family_name", person.getFamilyName());
                personJsonObject.put("middle_name", person.getMiddleName());
                personJsonObject.put("given_name", person.getGivenName());
                personJsonObject.put("birth_date", DateUtils.getFormattedDate(person.getBirthdate()));
                personJsonObject.put("sex", person.getGender());
                personJsonObject.put("attributes", person.getAtributes());
                personJsonObject.put("addresses", person.getAddresses());
            }
        } catch (PersonController.PersonLoadException | PatientController.PatientLoadException | JSONException e) {
            Log.e(getClass().getSimpleName(), "Could not retrieve person record", e);
        }
        return personJsonObject.toString();
    }

    @JavascriptInterface
    public String searchPersons(String searchTerm, boolean searchServer) {
        if (searchServer) {
            return searchPatientOnServer(searchTerm);
        } else {
            return searchPersonsLocally(searchTerm);
        }
    }

    @JavascriptInterface
    public String searchPersonsLocally(String searchTerm) {
        JSONArray personsJsonArray = new JSONArray();
        try {
            List<Patient> patientsOnDevice = patientController.searchPatientLocally(searchTerm, null);
            for (Patient patient : patientsOnDevice) {
                if (personController.getPersonByUuid(patient.getUuid()) == null)
                    personsJsonArray.put(createPatientJsonObject(patient));
            }

            List<Person> personsOnDevice = personController.searchPersonLocally(searchTerm);
            for (Person person : personsOnDevice) {
                personsJsonArray.put(createPersonJsonObject(person));
            }
        } catch (PersonController.PersonLoadException | PatientController.PatientLoadException e) {
            Toast.makeText(formWebViewActivity, formWebViewActivity.getString(R.string.error_form_provider_load), Toast.LENGTH_SHORT).show();
            Log.e(getClass().getSimpleName(), "Exception occurred while loading persons", e);
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "Could not add person object into persons array", e);
        }
        return personsJsonArray.toString();
    }

    @JavascriptInterface
    public String searchPatientOnServer(String searchTerm) {
        JSONArray patientsJsonArray = new JSONArray();

        if (searchTerm != null && searchTerm.length() >= 3) {
            Credentials credentials = new Credentials(formWebViewActivity);
            Constants.SERVER_CONNECTIVITY_STATUS serverStatus = NetworkUtils.getServerStatus(formWebViewActivity, credentials.getServerUrl());

            List<Patient> patientList = new ArrayList<>();
            if (serverStatus == Constants.SERVER_CONNECTIVITY_STATUS.SERVER_ONLINE) {
                patientList = patientController.searchPatientOnServer(searchTerm);
            }
            for (Patient patient : patientList) {
                try {
                    patientsJsonArray.put(createPatientJsonObject(patient));
                } catch (JSONException e) {
                    Log.e(getClass().getSimpleName(), "Could not add person object into persons array", e);
                }
            }
        }
        return patientsJsonArray.toString();
    }

    private JSONObject createPatientJsonObject(Patient patient) throws JSONException {
        JSONObject patientJsonObject = new JSONObject();
        patientJsonObject.put("uuid", patient.getUuid());
        patientJsonObject.put("name", patient.getDisplayName());
        patientJsonObject.put("birth_date", DateUtils.getFormattedDate(patient.getBirthdate()));
        patientJsonObject.put("birthdate_estimated", Boolean.toString(patient.getBirthdateEstimated()));
        patientJsonObject.put("sex", patient.getGender());
        patientJsonObject.put("identifier", patient.getIdentifier());
        return patientJsonObject;
    }

    private JSONObject createPersonJsonObject(Person person) throws JSONException {
        JSONObject personJsonObject = new JSONObject();
        personJsonObject.put("uuid", person.getUuid());
        personJsonObject.put("name", person.getDisplayName());
        personJsonObject.put("birth_date", DateUtils.getFormattedDate(person.getBirthdate()));
        personJsonObject.put("birthdate_estimated", Boolean.toString(person.getBirthdateEstimated()));
        personJsonObject.put("sex", person.getGender());
        return personJsonObject;
    }

    @JavascriptInterface
    public String getProviderNamesFromDevice() {
        List<Provider> providersOnDevice = new ArrayList<>();
        try {
            providersOnDevice = providerController.getAllProviders();
        } catch (ProviderController.ProviderLoadException e) {
            Toast.makeText(formWebViewActivity, formWebViewActivity.getString(R.string.error_form_provider_load), Toast.LENGTH_SHORT).show();
            Log.e(getClass().getSimpleName(), "Exception occurred while loading providers", e);
        }
        return JSONValue.toJSONString(providersOnDevice);
    }

    @JavascriptInterface
    public String getDefaultEncounterProvider() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(formWebViewActivity.getApplicationContext());
        boolean encounterProviderPreference = preferences.getBoolean("encounterProviderPreference", false);
        List<Provider> providers = new ArrayList<>();

        if (encounterProviderPreference) {
            MuzimaApplication applicationContext = (MuzimaApplication) formWebViewActivity.getApplicationContext();
            Provider provider = providerController.getProviderBySystemId(applicationContext.getAuthenticatedUser().getSystemId());
            if (provider != null) {
                providers.add(provider);
            }
        }
        return JSONValue.toJSONString(providers);
    }

    @JavascriptInterface
    public String getFontSizePreference() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(formWebViewActivity.getApplicationContext());
        return preferences.getString(formWebViewActivity.getResources().getString(R.string.preference_font_size),
                HTMLFormWebViewActivity.DEFAULT_FONT_SIZE).toLowerCase();
    }

    private String isValidForm(String jsonPayload, String status, boolean parseForPerson, FormData formData) {
        String homeVisitFormUuid = "fdd67221-5d1a-49e9-97e2-2f69aa5e26bc";
        if(homeVisitFormUuid.equalsIgnoreCase(formData.getTemplateUuid()) && Constants.STATUS_COMPLETE.equalsIgnoreCase(status)){
            HTMLFormObservationCreator observationCreator = getFormParser(parseForPerson);
            observationCreator.parseObservationsJSONResponse(jsonPayload, this.formData.getUuid());
            List<Observation> observations = observationCreator.getObservations();

            return validateRequiredHomeVisitFormFields(observations, jsonPayload);
        }

        return "";
    }
    private String validateRequiredHomeVisitFormFields(final List<Observation> observations, String jsonPayload) {

        String errorMessage = validateVisitTypeObs(observations, jsonPayload);
        if(!StringUtils.isEmpty(errorMessage)){
           return errorMessage;
        }

        errorMessage = validateAttemptNumberObs(observations);
        if(!StringUtils.isEmpty(errorMessage)){
            return errorMessage;
        }

        errorMessage = validatePatientFoundAttemptNumberObs(observations);
        if(!StringUtils.isEmpty(errorMessage)){
            return errorMessage;
        }

        errorMessage = validatePatientFoundObs(observations);
        if(!StringUtils.isEmpty(errorMessage)){
            return errorMessage;
        }

        Observation visitTypeObs = getVisitType(observations);
        Concept visitTypeConcept = visitTypeObs.getValueCoded();

        Observation patientFoundObs = getObs(observations, 2003);
        Concept patientFoundConcept = patientFoundObs.getValueCoded();
        if(2161 == visitTypeConcept.getId()
                || 23914 == visitTypeConcept.getId()) {
            if(1066 == patientFoundConcept.getId()){

                String informationGivenByErrorMessage = validateObsForInformationGivenBy(observations, jsonPayload);
                if(!StringUtils.isEmpty(informationGivenByErrorMessage)){
                    return informationGivenByErrorMessage;
                }
            }
            else if(1065 == patientFoundConcept.getId()){
                Observation visitReportObs = getObs(observations, 2158);
                Concept visitReportObsValueCoded = visitReportObs.getValueCoded();
                List<Integer> conceptsIds = getObsConcepts(observations);
                if(1383 == visitReportObsValueCoded.getId()){
                        // Ok
                }
                else if(2157 == visitReportObsValueCoded.getId()) {
                    if (!jsonPayload.contains("2157")) {
                        return "Relatório da visita é de preenchimento obrigatório";
                    }
                }
                if (!jsonPayload.contains("23947")) {
                    return "Paciente ou cuidador encaminhado para US é de preenchimento obrigatório!";
                }
                return validateReturnDate(observations, conceptsIds);
            }
        }
        else if(2160 == visitTypeConcept.getId()) {
            if(1066 == patientFoundConcept.getId()){

                String informationGivenByErrorMessage = validateObsForInformationGivenBy(observations, jsonPayload);
                if(!StringUtils.isEmpty(informationGivenByErrorMessage)){
                    return informationGivenByErrorMessage;
                }

            }
            else if(1065 == patientFoundConcept.getId()){
                List<Integer> conceptsIds = getObsConcepts(observations);
                if (!conceptsIds.contains(23947)) {
                    return "Paciente ou cuidador encaminhado para US é de preenchimento obrigatório!";
                }
                Observation observation = getObs(observations, 165268);
                if(observation == null) {
                    return validateReturnDate(observations, conceptsIds);
                }
            }
        }
        return "";
    }

    private String validateReturnDate(final List<Observation> observations, final List<Integer> conceptsIds) {
        Observation patientReferredToUsObs = getObs(observations, 23947);
        Concept patientReferredToUsObsValueCoded = patientReferredToUsObs.getValueCoded();
        if(conceptsIds.contains(24008) && 1065 == patientReferredToUsObsValueCoded.getId()) {
            if (!conceptsIds.contains(23933)) {
                return "Data combinada para retorno a US é de preenchimento obrigatório!";
            }
        }
        else if(conceptsIds.contains(24009) && 1065 == patientReferredToUsObsValueCoded.getId()) {
            if (!conceptsIds.contains(23934)) {
                return "Data combinada para retorno a US é de preenchimento obrigatório!";
            }
        }
        else if(conceptsIds.contains(24010) && 1065 == patientReferredToUsObsValueCoded.getId()) {
            if (!conceptsIds.contains(23935)) {
                return "Data combinada para retorno a US é de preenchimento obrigatório!";
            }
        }
        return "";
    }

    private String validateObsForInformationGivenBy(final List<Observation> observations, String jsonpayload) {
        List<Integer> conceptsIds = getObsConcepts(observations);
        if (jsonpayload.contains("2037")) {
            return "";
        }
        return "Dados do Informante é de preenchimento obrigatório!";
    }
    private Observation getObs(final List<Observation> observations, final Integer conceptId) {
        for (Observation observation :observations) {
            if(conceptId == observation.getConcept().getId()){
                return observation;
            }
        }
        return null;
    }

    private List<Integer> getObsConcepts (final List<Observation> observations) {
        List<Integer> conceptsIds = new ArrayList<>();
        for (Observation observation :observations) {
            conceptsIds.add(observation.getConcept().getId());
        }
        return conceptsIds;
    }
    private String validateVisitTypeObs(final List<Observation> observations, String jsonPayload){
        Integer conceptId = 1981;
        for (Observation observation :observations) {
            if(conceptId == observation.getConcept().getId()
                    && (2160 == observation.getValueCoded().getId() || 2161 == observation.getValueCoded().getId() || 23914 == observation.getValueCoded().getId())){
                return  "";
            }
        }
        return "Tipo de visita é de preenchimento obrigatório!";
    }
    private String validateAttemptNumberObs(final List<Observation> observations) {
        Integer conceptId = 23842;
        for (Observation observation :observations) {
            if(conceptId == observation.getConcept().getId()
                    && (6440 == observation.getValueCoded().getId() || 6254 == observation.getValueCoded().getId() || 6255 == observation.getValueCoded().getId())){
                return  "";
            }
        }
        return "Número de Tentativa é de preenchimento obrigatório!";
    }
    private String validatePatientFoundAttemptNumberObs(final List<Observation> observations) {
        for (Observation observation :observations) {
            if(24008 == observation.getConcept().getId() || 24009 == observation.getConcept().getId() || 24010 == observation.getConcept().getId()){
                return  "";
            }
        }
        return "Número da tentiva em que paciente foi encontrado é de preenchimento obrigatório!";
    }
    private Observation getVisitType(final List<Observation> observations){
        Integer conceptId = 1981;
        for (Observation observation :observations) {
            if(conceptId == observation.getConcept().getId()
                    && (2160 == observation.getValueCoded().getId() || 2161 == observation.getValueCoded().getId() || 23914 == observation.getValueCoded().getId())){
                return  observation;
            }
        }
        return null;
    }
    private String validatePatientFoundObs(final List<Observation> observations) {
        Integer conceptId = 2003;
        for (Observation observation :observations) {
            if(conceptId == observation.getConcept().getId()
                    && (1065 == observation.getValueCoded().getId() || 1066 == observation.getValueCoded().getId())){
                return  "";
            }
        }
        return "Encontrou o Paciente é de preenchimento obrigatório!";
    }
    private void parseObsFromCompletedForm(String jsonPayload, String status, boolean parseForPerson) {
        if (status.equals(Constants.STATUS_INCOMPLETE)) {
            return;
        }

        getFormParser(parseForPerson).createAndPersistObservations(jsonPayload, formData.getUuid());
    }

    public Date getEncounterDateFromForm(String jsonPayload) {
        return getFormParser().getEncounterDateFromFormDate(jsonPayload);
    }

    HTMLFormObservationCreator getFormParser(boolean parseAsPersonObs) {
        MuzimaApplication applicationContext = (MuzimaApplication) formWebViewActivity.getApplicationContext();
        return new HTMLFormObservationCreator(applicationContext, false, parseAsPersonObs);
    }

    HTMLFormObservationCreator getFormParser() {
        return getFormParser(false);
    }

    private boolean isRegistrationComplete(String status) {
        return formController.isRegistrationFormData(formData) && status.equals(Constants.STATUS_COMPLETE);
    }

    private boolean isRegistrationForm() {
        return formController.isRegistrationFormData(formData);
    }

    private boolean isEncounterForm() {
        return formController.isEncounterFormData(formData);
    }

    @JavascriptInterface
    public String getStringResource(String stringResourceName) {
        Context context = formWebViewActivity.getBaseContext();
        return context.getString(context.getResources().getIdentifier(stringResourceName, "string", context.getPackageName()));
    }

    @JavascriptInterface
    public String getConcepts() {
        List<Concept> concepts = new ArrayList<>();
        try {
            concepts = conceptController.getConcepts();
        } catch (ConceptController.ConceptFetchException | Exception e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while loading concepts", e);
        }
        return JSONValue.toJSONString(concepts);
    }

    @JavascriptInterface
    public String getEncountersByPatientUuid(String patientuuid) {
        List<Encounter> encounters = new ArrayList<>();
        try {
            encounters = encounterController.getEncountersByPatientUuid(patientuuid);
        } catch (EncounterController.FetchEncounterException | Exception e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while loading encounters", e);
        }
        return JSONValue.toJSONString(encounters);
    }

    @JavascriptInterface
    public String getObservationsByPatientUuid(String patientuuid) throws JSONException, ConceptController.ConceptFetchException {
        List<Observation> observations = new ArrayList<>();
        try {
            observations = observationController.getObservationsByPatient(patientuuid);
        } catch (Exception | ObservationController.LoadObservationException e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while loading observations", e);
        }
        return createObsJsonArray(observations);
    }

    @JavascriptInterface
    public String getEncounterTypes(String patientuuid) {
        List<Encounter> encounters = new ArrayList<>();
        List<Encounter> encountertypes = new ArrayList<>();
        List<String> encounterTypeArray = new ArrayList<>();
        try {
            encounters = encounterController.getEncountersByPatientUuid(patientuuid);
            for (Encounter encounter : encounters) {
                if (!(encounterTypeArray.contains(encounter.getEncounterType().getName()))) {
                    encounterTypeArray.add(encounter.getEncounterType().getName());
                    encountertypes.add(encounter);
                }
            }
        } catch (EncounterController.FetchEncounterException | Exception e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while loading encounterTypes", e);
        }
        return JSONValue.toJSONString(encountertypes);
    }

    @JavascriptInterface
    public String getObsByConceptId(String patientUuid, int conceptId) throws JSONException, ConceptController.ConceptFetchException {
        List<Observation> observations = new ArrayList<>();
        try {
            observations = observationController.getObservationsByPatientuuidAndConceptId(patientUuid, conceptId);
            Collections.sort(observations, observationDateTimeComparator);
        } catch (ObservationController.LoadObservationException | Exception e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while loading observations", e);
        }
        return createObsJsonArray(observations);
    }

    @JavascriptInterface
    public String getObsByEncounterId(int encounterid) throws JSONException, ConceptController.ConceptFetchException {
        List<Observation> observations = new ArrayList<>();
        try {
            observations = observationController.getObservationsByEncounterId(encounterid);
            Collections.sort(observations, observationDateTimeComparator);
        } catch (ObservationController.LoadObservationException | Exception e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while loading observations", e);
        }
        return createObsJsonArray(observations);
    }

    @JavascriptInterface
    public String getObsByEncounterType(String patientUuid, String encounterType) throws JSONException, ConceptController.ConceptFetchException {
        List<Observation> observations = new ArrayList<>();
        List<Encounter> encounters = new ArrayList<>();
        try {
            encounters = encounterController.getEncountersByPatientUuid(patientUuid);
            for (Encounter enc : encounters) {
                if (enc.getEncounterType().getName().equals(encounterType)) {
                    observations.addAll(observationController.getObservationsByEncounterId(enc.getId()));
                }
            }
            Collections.sort(observations, observationDateTimeComparator);
        } catch (ObservationController.LoadObservationException | Exception e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while loading observations", e);
        } catch (EncounterController.FetchEncounterException e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while loading encounters", e);
        }
        return createObsJsonArray(observations);
    }

    private String createObsJsonArray(List<Observation> observations) throws JSONException, ConceptController.ConceptFetchException {
        int i = 0;
        JSONArray arr = new JSONArray();
        HashMap<String, JSONObject> map = new HashMap<>();
        List<Concept> concepts = conceptController.getConcepts();
        for (Observation obs : observations) {
            String conceptName = "";
            String conceptUuid = obs.getConcept().getUuid();
            for (Concept concept : concepts) {
                if (concept.getUuid().equals(conceptUuid)) {
                    conceptName = ConceptUtils.getConceptNameFromConceptNamesByLocale(concept.getConceptNames(), getApplicationLanguage());
                }
            }
            final String dateFormat = Constants.STANDARD_DATE_FORMAT;
            SimpleDateFormat newDateFormat = new SimpleDateFormat("dd-MM-yy HH:mm:ss");
            Date obsDateTime = null;
            Date valueDateTime = null;
            try {
                obsDateTime = newDateFormat.parse(newDateFormat.format(obs.getObservationDatetime()));
                if (obs.getValueDatetime() != null) {
                    valueDateTime = newDateFormat.parse(newDateFormat.format(obs.getValueDatetime()));
                }
            } catch (ParseException e) {
                Log.e(getClass().getSimpleName(), "Exception occurred while parsing date", e);
            }
            newDateFormat.applyPattern(dateFormat);
            String convertedEncounterDate = newDateFormat.format(obsDateTime);
            String convertedvalueDateTime = "";
            if (valueDateTime != null) {
                convertedvalueDateTime = newDateFormat.format(valueDateTime);
            }

            JSONObject json = new JSONObject();
            JSONObject codedConcept = new JSONObject();
            if (!conceptName.isEmpty()) {
                json.put("conceptName", conceptName);
            } else {
                json.put("conceptName", "Concept Created On Phone");
            }

            json.put("conceptId", obs.getConcept().getId());
            json.put("conceptUuid", obs.getConcept().getUuid());

            json.put("obsDate", convertedEncounterDate);
            if (obs.getValueCoded() != null) {
                codedConcept.put("uuid", obs.getValueCoded().getUuid());
                codedConcept.put("id", obs.getValueCoded().getId());
                codedConcept.put("name", ConceptUtils.getConceptNameFromConceptNamesByLocale(obs.getValueCoded().getConceptNames(), getApplicationLanguage()));
                ;
                json.put("valueCoded", codedConcept);
            } else {
                json.put("valueCoded", obs.getValueCoded());
            }
            json.put("valueNumeric", obs.getValueNumeric());
            json.put("valueText", obs.getValueText());
            json.put("encounterId", obs.getEncounter().getId());
            json.put("uuid", obs.getUuid());
            json.put("valueComplex", obs.getValueComplex());
            json.put("valueDatetime", convertedvalueDateTime);
            json.put("obs_comment", obs.getComment());
            json.put("obs_group_id", obs.getObsGroupId());
            map.put("json" + i, json);
            arr.put(map.get("json" + i));
            i++;
        }
        return arr.toString();
    }

    @JavascriptInterface
    public boolean isMedicalRecordNumberRequired() {
        return settingController.isMedicalRecordNumberRequiredDuringRegistration();
    }

    @JavascriptInterface
    public void checkForPossibleFormDuplicate(String formUuid, String encounterDateTime, String patientUuid, String encounterPayLoad) throws FormController.FormDataFetchException, JSONException {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(formWebViewActivity.getApplicationContext());
        boolean isDuplicateFormDataWarningPreferenceSet = preferences.getBoolean(formWebViewActivity.getResources().getString(R.string.preference_duplicate_form_data_key), HTMLFormWebViewActivity.IS_ALLOWED_FORM_DATA_DUPLICATION);
        if (isDuplicateFormDataWarningPreferenceSet) {
            JSONObject mainObject = new JSONObject(encounterPayLoad);
            JSONObject encounterObject = mainObject.getJSONObject("encounter");
            if (!(encounterObject.has("encounter.encounter_datetime"))) {
                List<FormData> allFormData = new ArrayList<>();
                List<FormData> incompleteForms = new ArrayList<>();
                List<FormData> completeForms = new ArrayList<>();
                incompleteForms = formController.getAllFormDataByPatientUuid(patientUuid, Constants.STATUS_INCOMPLETE);
                completeForms = formController.getAllFormDataByPatientUuid(patientUuid, Constants.STATUS_COMPLETE);
                allFormData.addAll(incompleteForms);
                allFormData.addAll(completeForms);
                for (FormData formData : allFormData) {
                    Date encounterDate = formData.getEncounterDate();
                    String formDataUuid = formData.getTemplateUuid();

                    final String dateFormat = "dd-MM-yyyy";

                    SimpleDateFormat newDateFormat = new SimpleDateFormat("dd-MM-yy HH:mm:ss");
                    Date d = null;
                    try {
                        d = newDateFormat.parse(newDateFormat.format(encounterDate));
                    } catch (ParseException e) {
                        Log.e(getClass().getSimpleName(), e.getMessage());
                    }
                    newDateFormat.applyPattern(dateFormat);
                    String convertedEncounterDate = newDateFormat.format(d);
                    if (convertedEncounterDate.equals(encounterDateTime.substring(0, 10)) && formDataUuid.equals(formUuid)) {
                        formWebViewActivity.showWarningDialog();
                        break;
                    }
                }
            }
        }
    }

    @JavascriptInterface
    public boolean getDefaultEncounterLocationSetting() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(formWebViewActivity.getApplicationContext());
        String defaultLocationName = preferences.getString("defaultEncounterLocation", getStringResource("no_default_encounter_location"));
        String defaultValue = getStringResource("no_default_encounter_location");
        return !defaultLocationName.equals(defaultValue);
    }

    @JavascriptInterface
    public String getDefaultEncounterLocationPreference() throws LocationController.LocationLoadException {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(formWebViewActivity.getApplicationContext());
        String defaultLocationName = preferences.getString("defaultEncounterLocation", getStringResource("no_default_encounter_location"));
        String defaultValue = getStringResource("no_default_encounter_location");
        List<Location> defaultLocation = new ArrayList<>();
        List<Location> locations = new ArrayList<>();

        locations = locationController.getAllLocations();
        if (!defaultLocationName.equals(defaultValue)) {
            for (Location loc : locations) {
                if (Integer.toString(loc.getId()).equals(defaultLocationName)) {
                    defaultLocation.add(loc);
                }
            }
            return JSONValue.toJSONString(defaultLocation);
        }
        return JSONValue.toJSONString(locations);
    }

    private boolean areMandatoryEncounterDetailsInForm(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONObject jsonObjectInner = jsonObject.getJSONObject("encounter");
            return jsonObjectInner.has("encounter.provider_id") && jsonObjectInner.has("encounter.encounter_datetime") && jsonObjectInner.has("encounter.location_id");
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "Error while parsing response JSON", e);
        }
        return false;
    }

    private String injectUserSystemIdToEncounterPayload(String jsonPayload) {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            JSONObject jsonObjectInner = jsonObject.getJSONObject("encounter");
            if (!(jsonObjectInner.has("encounter.user_system_id"))) {
                String user_system_id = ((MuzimaApplication) formWebViewActivity.getApplicationContext()).getAuthenticatedUser().getSystemId();
                jsonObjectInner.put("encounter.user_system_id", user_system_id);
                jsonObject.put("encounter", jsonObjectInner);
                jsonPayload = jsonObject.toString();
            }
            return jsonPayload;
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "Error while parsing response JSON", e);
        }

        return jsonPayload;
    }

    private String injectActiveSetupConfigUuidToEncounterPayload(String jsonPayload) {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            JSONObject jsonObjectInner = jsonObject.getJSONObject("encounter");
            if (!(jsonObjectInner.has("encounter.setup_config_uuid"))) {
                SetupConfigurationTemplate activeSetupConfig = application.getSetupConfigurationController().getActiveSetupConfigurationTemplate();
                jsonObjectInner.put("encounter.setup_config_uuid", activeSetupConfig.getUuid());
                jsonObject.put("encounter", jsonObjectInner);
                jsonPayload = jsonObject.toString();
            }
            return jsonPayload;
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "Error while parsing response JSON", e);
        } catch (SetupConfigurationController.SetupConfigurationFetchException e) {
            Log.e(getClass().getSimpleName(), "Could not add active setup config UUID", e);
        }

        return jsonPayload;
    }

    private String checkMissingMandatoryEncounterDetails(String jsonPayLoad) {
        String message = "";
        try {
            JSONObject jsonObject = new JSONObject(jsonPayLoad);
            JSONObject jsonObjectInner = null;
            if (!(jsonObject.has("encounter"))) {
                Log.e(getClass().getSimpleName(), "No encounter details section in the form");
            } else {
                jsonObjectInner = jsonObject.getJSONObject("encounter");
                if (!(jsonObjectInner.has("encounter.encounter_datetime"))) {
                    message = formWebViewActivity.getString(R.string.form_encounter_date);
                }
                if (!(jsonObjectInner.has("encounter.provider_id"))) {
                    if (!(message.isEmpty())) {
                        message = message.concat(", ");
                        message = message.concat(formWebViewActivity.getString(R.string.form_encounter_provider));
                    } else {
                        message = formWebViewActivity.getString(R.string.form_encounter_provider);
                    }
                }
                if (!(jsonObjectInner.has("encounter.location_id"))) {
                    if (!(message.isEmpty())) {
                        message = message.concat(", ");
                        message = message.concat(formWebViewActivity.getString(R.string.form_encounter_location));
                    } else {
                        message = formWebViewActivity.getString(R.string.form_encounter_location);
                    }
                }
                return message;
            }
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "Error while parsing response JSON. Unparsable jsonPayLoad", e);
        }
        return null;
    }

    @JavascriptInterface
    public String getLastKnowGPSLocation(String jsonReturnType) {
        String gps_location_string = "Unknown Error Occured!";
        MuzimaGPSLocationService muzimaLocationService = application.getMuzimaGPSLocationService();
        if (muzimaLocationService.isGPSLocationFeatureEnabled()) {
            if (muzimaLocationService.isGPSLocationPermissionsGranted()) {
                if (muzimaLocationService.isLocationServicesSwitchedOn()) {
                    HashMap<String, Object> locationDataHashMap;
                    try {
                        locationDataHashMap = muzimaLocationService.getLastKnownGPSLocationAndSettingDetails();
                        if (locationDataHashMap.containsKey("gps_location")) {
                            if (jsonReturnType.equals("json-object")) {
                                gps_location_string = ((MuzimaGPSLocation) locationDataHashMap.get("gps_location")).toJsonObject().toString();
                            } else {
                                gps_location_string = ((MuzimaGPSLocation) locationDataHashMap.get("gps_location")).toJsonArray().toString();
                            }
                        } else if (locationDataHashMap.containsKey("gps_location_status")) {
                            gps_location_string = (String) locationDataHashMap.get("gps_location_status");
                        }
                        return gps_location_string;
                    } catch (Exception e) {
                        Log.e(getClass().getSimpleName(), "Unable to process gps data, unknow Error Occurred", e);
                        return gps_location_string;
                    }
                } else {
                    return "Location service disabled by user";
                }
            } else {
                return "Location Permissions Denied By User.";
            }
        } else {
            return "GPS Feature is Disabled by User";
        }
    }

    @JavascriptInterface
    public void logEvent(String tag, String details) {
        MuzimaLoggerService.log((MuzimaApplication) formWebViewActivity.getApplicationContext(), tag, details);
    }

    private String injectTimeZoneToEncounterPayload(String jsonPayload) {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            JSONObject jsonObjectInner = jsonObject.getJSONObject("encounter");
            if (!(jsonObjectInner.has("encounter.device_time_zone"))) {
                String device_time_zone = TimeZone.getDefault().getID();
                jsonObjectInner.put("encounter.device_time_zone", device_time_zone);
                jsonObject.put("encounter", jsonObjectInner);
                jsonPayload = jsonObject.toString();
            }
            return jsonPayload;
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "Error while parsing response JSON", e);
        }

        return jsonPayload;
    }

    @JavascriptInterface
    public String getCohortMembershipByPatientUuid(String patientUuid) {
        List<CohortMember> cohortMembers = new ArrayList<>();
        JSONArray jsonArray = new JSONArray();
        HashMap<String, JSONObject> map = new HashMap<>();
        int i = 0;
        try {
            cohortMembers = cohortController.getCohortMembershipByPatientUuid(patientUuid);
            for (CohortMember cohortMember : cohortMembers) {
                JSONObject json = new JSONObject();
                json.put("cohortUuid", cohortMember.getCohort().getUuid());
                json.put("cohortName", cohortMember.getCohort().getName());
                map.put("json" + i, json);
                jsonArray.put(map.get("json" + i));
                i++;
            }
        } catch (CohortController.CohortFetchException e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while loading cohort membership", e);
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "JSONException encountered while process cohort membership", e);
        }
        return jsonArray.toString();
    }

    private void logFormStartEvent(boolean isFormReload) {
        try {
            JSONObject eventDetails = new JSONObject();
            eventDetails.put("patientuuid", formData.getPatientUuid());
            eventDetails.put("formDataUuid", formData.getUuid());
            eventDetails.put("formDiscriminator", formData.getDiscriminator());

            if (isFormReload) {
                logEvent("RESUME_FORM", eventDetails.toString());
            } else {
                logEvent("OPEN_FORM", eventDetails.toString());
            }
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "Cannot create event log", e);
        }
    }

    private void logFormSaveEvent(String status) {
        try {
            JSONObject eventDetails = new JSONObject();
            eventDetails.put("patientuuid", formData.getPatientUuid());
            eventDetails.put("formDataUuid", formData.getUuid());
            eventDetails.put("formDiscriminator", formData.getDiscriminator());
            eventDetails.put("formUuid", formData.getTemplateUuid());

            switch (status) {
                case Constants.STATUS_COMPLETE:
                    logEvent("SAVE_COMPLETE_FORM", eventDetails.toString());
                    break;
                case Constants.STATUS_INCOMPLETE:
                    logEvent("SAVE_DRAFT_FORM", eventDetails.toString());
                    break;
            }
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "Cannot create log", e);
        }
    }

    private final Comparator<Observation> observationDateTimeComparator = new Comparator<Observation>() {
        @Override
        public int compare(Observation lhs, Observation rhs) {
            return -lhs.getObservationDatetime().compareTo(rhs.getObservationDatetime());
        }
    };

    private final Comparator<Observation> observationValueCodedComparator = new Comparator<Observation>() {
        @Override
        public int compare(Observation lhs, Observation rhs) {
            return Integer.compare(lhs.getValueCoded().getId(), rhs.getValueCoded().getId());
        }
    };

    @JavascriptInterface
    public void createPersonAndDiscardHTML(String jsonPayload) {
        try {
            RelationshipJsonMapper mapper = new RelationshipJsonMapper((MuzimaApplication) formWebViewActivity.getApplicationContext());
            Person person = mapper.createNewPerson(jsonPayload, formData.getPatientUuid());
            personController.savePerson(person);
            JSONObject jsonObject = new JSONObject(jsonPayload);
            if (jsonObject.has("observation")) {
                saveHTML(jsonPayload, "complete", false);
            }
            formWebViewActivity.finish();
        } catch (Exception | PersonController.PersonSaveException e) {
            Toast.makeText(formWebViewActivity, R.string.info_person_creation_failure, Toast.LENGTH_LONG).show();
            Log.e(getClass().getSimpleName(), "Exception occurred while parsing object", e);
        }

    }

    @JavascriptInterface
    public String getApplicationLanguage() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(formWebViewActivity.getApplicationContext());
        String applicationLanguage = preferences.getString(formWebViewActivity.getResources().getString(R.string.preference_app_language), formWebViewActivity.getResources().getString(R.string.language_portuguese));
        return applicationLanguage;
    }

    public void setSelectedPatientsUuids(String selectedPatientsUuids) {
        this.selectedPatientsUuids = selectedPatientsUuids;
    }

    @JavascriptInterface
    public String getSelectedPatientsUuids() {
        return selectedPatientsUuids;
    }

    public List<Patient> getPatientsFromCommaSeparatedString(String patientUUids) {
        List<Patient> patients = new ArrayList<>();
        patientUUids = patientUUids.replace("[", "");
        patientUUids = patientUUids.replace("]", "");
        patientUUids = patientUUids.replaceAll("\"", "");
        List<String> patientUuidList = Arrays.asList(patientUUids.split(","));
        for (String patientUuid : patientUuidList) {
            try {
                Patient patient = patientController.getPatientByUuid(patientUuid);
                patients.add(patient);
            } catch (PatientController.PatientLoadException e) {
                Log.e(getClass().getSimpleName(), "Encountered an exception", e);
            }
        }
        return patients;
    }

    private String setPatientInfoToThePayload(Patient patient, String jsonPayload) {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            JSONObject jsonObjectInner = jsonObject.getJSONObject("patient");
            if ((jsonObjectInner.has("patient.uuid"))) {
                jsonObjectInner.remove("patient.uuid");
                jsonObjectInner.put("patient.uuid", patient.getUuid());
                jsonObject.put("patient", jsonObjectInner);
                jsonPayload = jsonObject.toString();
            }
            if ((jsonObjectInner.has("patient.names"))) {
                jsonObjectInner.remove("patient.names");
                jsonObjectInner.put("patient.names", patient.getDisplayName());
                jsonObject.put("patient", jsonObjectInner);
                jsonPayload = jsonObject.toString();
            }
            if ((jsonObjectInner.has("patient.birth_date"))) {
                jsonObjectInner.remove("patient.birth_date");
                jsonObjectInner.put("patient.birth_date", patient.getDisplayName());
                jsonObject.put("patient", jsonObjectInner);
                jsonPayload = jsonObject.toString();
            }
            if (!(jsonObjectInner.has("patient.sex"))) {
                jsonObjectInner.remove("patient.sex");
                jsonObjectInner.put("patient.sex", patient.getGender());
                jsonObject.put("patient", jsonObjectInner);
                jsonPayload = jsonObject.toString();
            }
            if ((jsonObjectInner.has("patient.medical_record_number"))) {
                jsonObjectInner.remove("patient.medical_record_number");
                jsonObjectInner.put("patient.medical_record_number", patient.getIdentifier());
                jsonObject.put("patient", jsonObjectInner);
                jsonPayload = jsonObject.toString();
            }

            return jsonPayload;
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "Error while parsing response JSON", e);
        }

        return jsonPayload;
    }

    @JavascriptInterface
    public String getDerivedObservationByPatientUuid(String patientUuid) throws JSONException, DerivedConceptController.DerivedConceptFetchException {
        List<DerivedObservation> derivedObservations = new ArrayList<>();
        try {
            derivedObservations = derivedObservationController.getDerivedObservationByPatientUuid(patientUuid);
            Collections.sort(derivedObservations, derivedObservationDateTimeComparator);
        } catch (DerivedObservationController.DerivedObservationFetchException e) {
            Log.e(getClass().getSimpleName(), "Encountered and exception while fetching derived observations", e);
        }
        return createDerivedObsJsonArray(derivedObservations);
    }

    @JavascriptInterface
    public String getDerivedObservationByPatientUuidAndDerivedConceptUuid(String patientUuid, String derivedConceptUuid) throws JSONException, DerivedConceptController.DerivedConceptFetchException {
        List<DerivedObservation> derivedObservations = new ArrayList<>();
        try {
            derivedObservations = derivedObservationController.getDerivedObservationByPatientUuidAndDerivedConceptUuid(patientUuid, derivedConceptUuid);
            Collections.sort(derivedObservations, derivedObservationDateTimeComparator);
        } catch (DerivedObservationController.DerivedObservationFetchException e) {
            Log.e(getClass().getSimpleName(), "Encountered and exception while fetching derived observations", e);
        }
        return createDerivedObsJsonArray(derivedObservations);
    }

    @JavascriptInterface
    public String getDerivedObservationByPatientUuidAndDerivedConceptId(String patientUuid, int derivedConceptId) throws JSONException, DerivedConceptController.DerivedConceptFetchException {
        List<DerivedObservation> derivedObservations = new ArrayList<>();
        try {
            derivedObservations = derivedObservationController.getDerivedObservationByPatientUuidAndDerivedConceptId(patientUuid, derivedConceptId);
            Collections.sort(derivedObservations, derivedObservationDateTimeComparator);
        } catch (DerivedObservationController.DerivedObservationFetchException e) {
            Log.e(getClass().getSimpleName(), "Encountered and exception while fetching derived observations", e);
        }
        return createDerivedObsJsonArray(derivedObservations);
    }

    @JavascriptInterface
    public String getInterventionsDerivedObservationByPatientUuid(String patientUuid) throws JSONException, DerivedConceptController.DerivedConceptFetchException {
        List<DerivedObservation> derivedObservationsList = new ArrayList<>();
        try {
            List<DerivedObservation> derivedObservations = new ArrayList<>();
            derivedObservations.addAll(getInterventionsDerivedObs(patientUuid));
            Collections.sort(derivedObservations, derivedObservationDateTimeComparator);

            List<CohortMember> cohortMembers = cohortController.getCohortMembershipByPatientUuid(patientUuid);
            Collections.sort(cohortMembers, cohortMemberDateTimeComparator);
            CohortMember cohortMember = cohortMembers.get(0);
            Set<String> derivedObs = new HashSet<String>(0);
            for (DerivedObservation derivedObservation : derivedObservations) {
                int value = derivedObservation.getDateCreated().compareTo(cohortMember.getMembershipDate());
                if (value == 0 || value == 1) {
                    if (derivedObs.isEmpty() && derivedObservationsList.isEmpty()) {
                        if (notElementOfTheList(derivedObservationsList, derivedObservation)) {
                            derivedObservationsList.add(derivedObservation);
                            derivedObs.add(derivedObservation.getValueText());
                            if (isPreventiveObs(derivedObservation)) {
                                break;
                            }
                        }
                    } else if (!derivedObs.isEmpty() && !derivedObs.contains(derivedObservation.getValueText())) {
                        if(isPatientFromList2(derivedObservationsList) && !isPreventiveObs(derivedObservation)){
                            if (notElementOfTheList(derivedObservationsList, derivedObservation)) {
                                derivedObservationsList.add(derivedObservation);
                                derivedObs.add(derivedObservation.getValueText());
                            }
                        }
                    }
                }
            }
            Collections.sort(derivedObservationsList, derivedObservationDateTimeComparator);
        } catch (DerivedObservationController.DerivedObservationFetchException |
                 CohortController.CohortFetchException e) {
            Log.e(getClass().getSimpleName(), "Encountered and exception while fetching derived observations", e);
        }
        return createDerivedObsJsonArray(derivedObservationsList);
    }

    private boolean notElementOfTheList(List<DerivedObservation> derivedObservationsList, DerivedObservation derivedObservation) {
        for (DerivedObservation derivedObservation1 : derivedObservationsList) {
            if (derivedObservation1.getValueText().equals(derivedObservation.getValueText())) return false;
        }
        return true;
    }

    private boolean isPreventiveObs(DerivedObservation derivedObservation) {
        List<String> derivedConceptsUuids = new ArrayList<String>();
        derivedConceptsUuids.add("379e2aa5-b750-4b08-af13-cd0b9795eca7");
        return  derivedConceptsUuids.contains(derivedObservation.getDerivedConcept().getUuid());
    }

    private boolean isPatientFromList2(List<DerivedObservation> derivedObservations) {
        List<String> derivedConceptsUuids = new ArrayList<String>();
        derivedConceptsUuids.add("4b479a6c-4276-45a1-b785-ecbc7dc59ff1");
        derivedConceptsUuids.add("1bd47ba9-b6ff-4b4c-ba26-f5b86498d738");
        derivedConceptsUuids.add("9e928864-b7d2-445d-9856-cb7c9a0632dd");
        derivedConceptsUuids.add("46e6c352-bddb-4191-8d1e-40380aa1a346");
        for (DerivedObservation derivedObservation : derivedObservations) {
            if (derivedConceptsUuids.contains(derivedObservation.getDerivedConcept().getUuid())) {
                return true;
            }
        }
        return false;
    }

    private List<DerivedObservation> getInterventionsDerivedObs(String patientUuid) throws DerivedObservationController.DerivedObservationFetchException {
        List<DerivedObservation> derivedObservations = derivedObservationController.getDerivedObservationByPatientUuid(patientUuid);
        List<DerivedObservation> derivedObservationList = new ArrayList<DerivedObservation>();
        List<String> derivedConceptsUuids = new ArrayList<String>();
        derivedConceptsUuids.add("4b479a6c-4276-45a1-b785-ecbc7dc59ff1");
        derivedConceptsUuids.add("1bd47ba9-b6ff-4b4c-ba26-f5b86498d738");
        derivedConceptsUuids.add("9e928864-b7d2-445d-9856-cb7c9a0632dd");
        derivedConceptsUuids.add("46e6c352-bddb-4191-8d1e-40380aa1a346");
        derivedConceptsUuids.add("379e2aa5-b750-4b08-af13-cd0b9795eca7");
        for (DerivedObservation derivedObservation : derivedObservations) {
            if (derivedConceptsUuids.contains(derivedObservation.getDerivedConcept().getUuid())) {
                derivedObservationList.add(derivedObservation);
            }
        }
        Collections.sort(derivedObservationList, derivedObservationDateTimeComparator);
        return derivedObservationList;
    }

    private final Comparator<DerivedObservation> derivedObservationDateTimeComparator = new Comparator<DerivedObservation>() {
        @Override
        public int compare(DerivedObservation lhs, DerivedObservation rhs) {
            return -lhs.getDateCreated().compareTo(rhs.getDateCreated());
        }
    };

    private final Comparator<CohortMember> cohortMemberDateTimeComparator = new Comparator<CohortMember>() {
        @Override
        public int compare(CohortMember lhs, CohortMember rhs) {
            return -lhs.getMembershipDate().compareTo(rhs.getMembershipDate());
        }
    };

    @JavascriptInterface
    public String getLastVisitAttemptNumberAfterLastTriangulation(String patientUuid, int conceptId) throws ConceptController.ConceptFetchException, JSONException {
        List<Observation> observations = new ArrayList<Observation>();
        try {
            List<Observation> lastTriangulations = observationController.getObservationsByPatientuuidAndConceptId(patientUuid, 1912);
            Collections.sort(lastTriangulations, observationDateTimeComparator);
            Observation lastTriangulation = null;
            for (Observation observation : lastTriangulations) {
                if ("ALLOCATION_PARENT_OBS_V3.3".equalsIgnoreCase(observation.getComment())) {
                    lastTriangulation = observation;
                    break;
                }
            }

            if (lastTriangulation != null) {
                /*List<Observation> lastAttempts = observationController.getObservationsByPatientuuidAndConceptId(patientUuid, conceptId);
                Collections.sort(lastAttempts, observationDateTimeComparator);*/
                Observation lastAttempt = getLastVisitAttemptNumber(patientUuid, conceptId);
                if (lastAttempt != null && lastAttempt.getObservationDatetime().after(lastTriangulation.getObservationDatetime())) {
                    observations.add(lastAttempt);
                }
            }
        } catch (ObservationController.LoadObservationException | RuntimeException e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while loading observations", e);
        }
        return createObsJsonArray(observations);
    }

    private String createDerivedObsJsonArray(List<DerivedObservation> derivedObservations) throws JSONException, DerivedConceptController.DerivedConceptFetchException {
        int i = 0;
        JSONArray arr = new JSONArray();
        HashMap<String, JSONObject> map = new HashMap<>();
        List<DerivedConcept> derivedConcepts = derivedConceptController.getDerivedConcepts();
        for (DerivedObservation derivedObservation : derivedObservations) {
            String derivedConceptName = "";
            String conceptUuid = derivedObservation.getDerivedConcept().getUuid();
            for (DerivedConcept derivedConcept : derivedConcepts) {
                if (derivedConcept.getUuid().equals(conceptUuid)) {
                    derivedConceptName = ConceptUtils.getDerivedConceptNameFromConceptNamesByLocale(derivedConcept.getDerivedConceptName(), getApplicationLanguage());
                }
            }
            final String dateFormat = Constants.STANDARD_DATE_FORMAT;
            SimpleDateFormat newDateFormat = new SimpleDateFormat("dd-MM-yy HH:mm:ss");
            Date dateCreated = null;
            Date valueDateTime = null;
            try {
                dateCreated = newDateFormat.parse(newDateFormat.format(derivedObservation.getDateCreated()));
                if (derivedObservation.getValueDatetime() != null) {
                    valueDateTime = newDateFormat.parse(newDateFormat.format(derivedObservation.getValueDatetime()));
                }
            } catch (ParseException e) {
                Log.e(getClass().getSimpleName(), "Exception occurred while parsing date", e);
            }
            newDateFormat.applyPattern(dateFormat);
            String convertedCreationDate = newDateFormat.format(dateCreated);
            String convertedValueDateTime = "";
            if (valueDateTime != null) {
                convertedValueDateTime = newDateFormat.format(valueDateTime);
            }

            JSONObject json = new JSONObject();
            JSONObject derivedCodedConcept = new JSONObject();
            if (!derivedConceptName.isEmpty()) {
                json.put("derivedConceptName", derivedConceptName);
            } else {
                json.put("derivedConceptName", "NULL");
            }

            json.put("derivedConceptId", derivedObservation.getDerivedConcept().getId());
            json.put("derivedConceptUuid", derivedObservation.getDerivedConcept().getUuid());

            json.put("dateCreated", convertedCreationDate);
            if (derivedObservation.getValueCoded() != null) {
                derivedCodedConcept.put("uuid", derivedObservation.getValueCoded().getUuid());
                derivedCodedConcept.put("id", derivedObservation.getValueCoded().getId());
                derivedCodedConcept.put("name", ConceptUtils.getConceptNameFromConceptNamesByLocale(derivedObservation.getValueCoded().getConceptNames(), getApplicationLanguage()));
                ;
                json.put("valueCoded", derivedCodedConcept);
            } else {
                json.put("valueCoded", derivedObservation.getValueCoded());
            }
            json.put("valueNumeric", derivedObservation.getValueNumeric());
            json.put("valueText", derivedObservation.getValueText());
            json.put("valueBoolean", derivedObservation.isValueBoolean());
            json.put("valueDatetime", convertedValueDateTime);
            json.put("uuid", derivedObservation.getUuid());
            map.put("json" + i, json);
            arr.put(map.get("json" + i));
            i++;
        }
        return arr.toString();
    }

    @JavascriptInterface
    public String getObsByObsGroupId(int obsGroupId) throws JSONException, ConceptController.ConceptFetchException {
        List<Observation> observations = new ArrayList<>();
        try {
            observations = observationController.getObsByObsGroupId(obsGroupId);
            Collections.sort(observations, observationDateTimeComparator);
        } catch (ObservationController.LoadObservationException | Exception e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while loading observations", e);
        }

        return createObsJsonArray(observations);
    }

    public Observation getLastTriangulation(String patientUuid) throws ConceptController.ConceptFetchException, JSONException {
        try {
            List<Observation> lastTriangulations = observationController.getObservationsByPatientuuidAndConceptId(patientUuid, 1912);
            Collections.sort(lastTriangulations, observationDateTimeComparator);
            for (Observation observation: lastTriangulations) {
                if (!StringUtils.isEmpty(observation.getComment())) return observation;
            }
            return null;
        } catch (ObservationController.LoadObservationException | RuntimeException e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while loading observations", e);
        }
        return null;
    }

    public Observation getLastVisitAttemptNumber(String patientUuid, int conceptId) throws ConceptController.ConceptFetchException, JSONException {
        try {
                List<Observation> lastAttempts = observationController.getObservationsByPatientuuidAndConceptId(patientUuid, conceptId);
                if (lastAttempts == null || lastAttempts.size() <= 0) return null;

                List<Observation> lastAttemptsAfterTriangulation = new ArrayList<>();

                Observation lastTriangulation = getLastTriangulation(patientUuid);
                for (Observation observation : lastAttempts) {
                    if(observation.getObservationDatetime().compareTo(lastTriangulation.getObservationDatetime())==1) {
                        lastAttemptsAfterTriangulation.add(observation);
                    }
                }
                if (lastAttemptsAfterTriangulation.size() <= 0) return null;

                if (listContains(lastAttemptsAfterTriangulation, 6255)) {
                    return getAttemptFromList(lastAttemptsAfterTriangulation,6255);
                } else if (listContains(lastAttemptsAfterTriangulation, 6254)) {
                    return getAttemptFromList(lastAttemptsAfterTriangulation,6254);
                } else if (listContains(lastAttemptsAfterTriangulation, 6440)) {
                    return getAttemptFromList(lastAttemptsAfterTriangulation,6440);
                }
                return null;
        } catch (ObservationController.LoadObservationException | RuntimeException e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while loading observations", e);
        }
        return null;
    }

    private Observation getAttemptFromList(List<Observation> lastAttemptsAfterTriangulation, int valueCodedId) {
        for (Observation observation : lastAttemptsAfterTriangulation) {
            if (observation.getValueCoded().getId() == valueCodedId) return observation;
        }
        return null;
    }

    private boolean listContains(List<Observation> lastAttemptsAfterTriangulation, int valueCodedId) {
        for (Observation observation : lastAttemptsAfterTriangulation) {
            if (observation.getValueCoded().getId() == valueCodedId) return true;
        }
        return false;
    }


    public boolean isLastAttemptReached(String patientUuid, int conceptId) throws ConceptController.ConceptFetchException, JSONException {
        Observation lastAttempt = getLastVisitAttemptNumber(patientUuid, conceptId);
        if (lastAttempt == null) return false;

        Concept concept = conceptController.getConceptById(6255);
        return  concept.getId() == lastAttempt.getValueCoded().getId();
    }

    public boolean wasLastVisitMadeSuccessfully(String patientUuid) throws ObservationController.LoadObservationException, ConceptController.ConceptFetchException, JSONException {
        try {
            Observation lastTriangulation = getLastTriangulation(patientUuid);
            List<Observation> patientFoundAtFirstAttempt = observationController.getObservationsByPatientuuidAndConceptId(patientUuid, 24008);
            Collections.sort(patientFoundAtFirstAttempt, observationDateTimeComparator);
            if(patientFoundAtFirstAttempt.get(0).getObservationDatetime().compareTo(lastTriangulation.getObservationDatetime())==1){
               return true;
            }
            List<Observation> patientFoundAtSecondAttempt = observationController.getObservationsByPatientuuidAndConceptId(patientUuid, 24009);
            Collections.sort(patientFoundAtSecondAttempt, observationDateTimeComparator);
            if(patientFoundAtSecondAttempt.get(0).getObservationDatetime().compareTo(lastTriangulation.getObservationDatetime())==1){
                return true;
            }

            } catch (ObservationController.LoadObservationException | RuntimeException e) {
               Log.e(getClass().getSimpleName(), "Exception occurred while loading observations", e);
             }
        return false;
    }
}
