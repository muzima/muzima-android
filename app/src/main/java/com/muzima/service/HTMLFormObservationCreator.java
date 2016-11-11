/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.service;

import android.util.Log;
import com.muzima.api.model.Concept;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Patient;
import com.muzima.controller.ConceptController;
import com.muzima.controller.EncounterController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.PatientController;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static com.muzima.utils.DateUtils.parse;
import static java.util.Arrays.asList;

public class HTMLFormObservationCreator {

    private PatientController patientController;
    private ConceptController conceptController;
    private EncounterController encounterController;
    private ObservationController observationController;
    private ObservationParserUtility observationParserUtility;

    private Patient patient;
    private Encounter encounter;
    private List<Observation> observations;
    private String TAG = "HTMLFormObservationCreator";

    public HTMLFormObservationCreator(PatientController patientController, ConceptController conceptController,
                                      EncounterController encounterController, ObservationController observationController) {
        this.patientController = patientController;
        this.conceptController = conceptController;
        this.encounterController = encounterController;
        this.observationController = observationController;
        this.observationParserUtility = new ObservationParserUtility(conceptController);
    }

    public void createAndPersistObservations(String jsonResponse,String formDataUuid) {
        parseJSONResponse(jsonResponse,formDataUuid);
        try {
            saveObservationsAndRelatedEntities();
        } catch (ConceptController.ConceptSaveException e) {
            Log.e(TAG, "Error while saving concept", e);
        } catch (EncounterController.SaveEncounterException e) {
            Log.e(TAG, "Error while saving Encounter", e);
        } catch (ObservationController.SaveObservationException e) {
            Log.e(TAG, "Error while saving Observation", e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected Exception occurred", e);
        }
    }

    public List<Observation> getObservations() {
        return observations;
    }

    private void parseJSONResponse(String jsonResponse, String formDataUuid) {
        try {
            JSONObject responseJSON = new JSONObject(jsonResponse);
            patient = getPatient(responseJSON.getJSONObject("patient"));
            encounter = createEncounter(responseJSON.getJSONObject("encounter"),formDataUuid);
            if (responseJSON.has("observation")) {
                observations = extractObservationFromJSONObject(responseJSON.getJSONObject("observation"));
            }
        } catch (PatientController.PatientLoadException e) {
            Log.e(TAG, "Error while fetching Patient", e);
        } catch (ConceptController.ConceptFetchException e) {
            Log.e(TAG, "Error while fetching Concept", e);
        }catch (JSONException e) {
            Log.e(TAG, "Error while parsing response JSON", e);
        } catch (ParseException e) {
            Log.e(TAG, "Error while parsing response JSON", e);
        } catch (ConceptController.ConceptSaveException e) {
            Log.e(TAG, "Error while saving newly created concept", e);
        }
    }

    private void saveObservationsAndRelatedEntities() throws EncounterController.SaveEncounterException,
            ObservationController.SaveObservationException, ConceptController.ConceptSaveException {
        try {
            encounterController.saveEncounters(asList(encounter));
            conceptController.saveConcepts(observationParserUtility.getNewConceptList());
            if(observations != null && !observations.isEmpty()){
                observationController.saveObservations(observations);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while parsing and storing Observations.", e);
        }
    }

    private List<Observation> extractObservationFromJSONObject(JSONObject jsonObject) throws JSONException,
            ConceptController.ConceptFetchException,ConceptController.ConceptSaveException{
        List<Observation> observations = new ArrayList<Observation>();
        Iterator keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            observations.addAll(extractBasedOnType(jsonObject, key));
        }
        observations.removeAll(Collections.singleton(null));
        return observations;
    }

    private List<Observation> extractBasedOnType(JSONObject jsonObject, String key) throws JSONException,
            ConceptController.ConceptFetchException, ConceptController.ConceptSaveException{
        if (jsonObject.get(key) instanceof JSONArray) {
            return createMultipleObservation(key, jsonObject.getJSONArray(key));
        } else if (jsonObject.get(key) instanceof JSONObject) {
            return extractObservationFromJSONObject(jsonObject.getJSONObject(key));
        }
        ArrayList<Observation> observations = new ArrayList<Observation>();
        observations.add(createObservation(key, jsonObject.getString(key)));
        return observations;
    }

    private List<Observation> createMultipleObservation(String conceptName, JSONArray jsonArray) throws JSONException,
            ConceptController.ConceptFetchException, ConceptController.ConceptSaveException{
        List<Observation> observations = new ArrayList<Observation>();
        for (int i = 0; i < jsonArray.length(); i++) {
            if (jsonArray.get(i) instanceof JSONObject) {
                observations.addAll(extractObservationFromJSONObject(jsonArray.getJSONObject(i)));
            } else {
                observations.add(createObservation(conceptName, jsonArray.getString(i)));
            }
        }
        return observations;
    }

    private Observation createObservation(String conceptName, String value) throws JSONException,
            ConceptController.ConceptFetchException, ConceptController.ConceptSaveException{
        try {
            Concept concept = observationParserUtility.getConceptEntity(conceptName);
            Observation observation = observationParserUtility.getObservationEntity(concept, value);
            observation.setEncounter(encounter);
            observation.setPerson(patient);
            observation.setObservationDatetime(encounter.getEncounterDatetime());
            return observation;
        } catch (ConceptController.ConceptParseException e) {
            Log.e(TAG, "Error while parsing Concept", e);
            return null;
        } catch (ObservationController.ParseObservationException e) {
            Log.e(TAG, "Error while parsing Observation", e);
            return null;
        }
    }

    private Encounter createEncounter(JSONObject encounterJSON, String formDataUuid) throws JSONException, ParseException {
        return observationParserUtility.getEncounterEntity(parse(encounterJSON.getString("encounter.encounter_datetime")), patient,formDataUuid);
    }

    public Date getEncounterDateFromFormDate(String jsonResponse){
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONObject jsonObjectInner = jsonObject.getJSONObject("encounter");
            return parse(jsonObjectInner.getString("encounter.encounter_datetime"));
        } catch (JSONException e) {
            Log.e(TAG, "Error while parsing response JSON", e);
        } catch (ParseException e) {
            Log.e(TAG, "Error while parsing response JSON", e);
        }
        return null;
    }

    private Patient getPatient(JSONObject patient) throws JSONException, PatientController.PatientLoadException {
        String uuid = patient.getString("patient.uuid");
        return patientController.getPatientByUuid(uuid);
    }
}
