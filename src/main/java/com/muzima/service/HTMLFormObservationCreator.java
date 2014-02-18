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

    public void createAndPersistObservations(String jsonResponse) {
        parseJSONResponse(jsonResponse);
        try {
            saveObservationsAndRelatedEntities();
        } catch (ConceptController.ConceptSaveException e) {
            Log.e(TAG, "Error while saving concept");
        } catch (EncounterController.SaveEncounterException e) {
            Log.e(TAG, "Error while saving Encounter");
        } catch (ObservationController.SaveObservationException e) {
            Log.e(TAG, "Error while saving Observation");
        }
    }

    public List<Observation> getObservations() {
        return observations;
    }

    private void parseJSONResponse(String jsonResponse) {
        try {
            JSONObject responseJSON = new JSONObject(jsonResponse);
            patient = getPatient(responseJSON.getJSONObject("patient"));
            encounter = createEncounter(responseJSON.getJSONObject("encounter"));
            observations = extractObservationFromJSONObject(responseJSON.getJSONObject("observation"));
        } catch (PatientController.PatientLoadException e) {
            Log.e(TAG, "Error while fetching Patient");
        } catch (ConceptController.ConceptFetchException e) {
            Log.e(TAG, "Error while fetching Concept");
        } catch (JSONException e) {
            Log.e(TAG, "Error while parsing response JSON");
        } catch (ParseException e) {
            Log.e(TAG, "Error while parsing response JSON");
        } catch (ConceptController.ConceptSaveException e) {
            Log.e(TAG, "Error while saving newly created concept");
        }
    }

    private void saveObservationsAndRelatedEntities() throws EncounterController.SaveEncounterException,
            ObservationController.SaveObservationException, ConceptController.ConceptSaveException {
        encounterController.saveEncounters(asList(encounter));
        conceptController.saveConcepts(observationParserUtility.getNewConceptList());
        observationController.saveObservations(observations);
    }

    private List<Observation> extractObservationFromJSONObject(JSONObject jsonObject) throws JSONException, ConceptController.ConceptFetchException, ConceptController.ConceptSaveException {
        List<Observation> observations = new ArrayList<Observation>();
        Iterator keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            observations.addAll(extractBasedOnType(jsonObject, key));
        }
        return observations;
    }

    private List<Observation> extractBasedOnType(JSONObject jsonObject, String key) throws JSONException, ConceptController.ConceptFetchException, ConceptController.ConceptSaveException {
        if (jsonObject.get(key) instanceof JSONArray) {
            return createMultipleObservation(key, jsonObject.getJSONArray(key));
        } else if (jsonObject.get(key) instanceof JSONObject) {
            return extractObservationFromJSONObject(jsonObject.getJSONObject(key));
        }
        return asList(createObservation(key, jsonObject.getString(key)));
    }

    private List<Observation> createMultipleObservation(String conceptName, JSONArray jsonArray) throws JSONException,
            ConceptController.ConceptFetchException, ConceptController.ConceptSaveException {
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

    private Observation createObservation(String conceptName, String value) throws JSONException, ConceptController.ConceptFetchException, ConceptController.ConceptSaveException {
        Concept concept = observationParserUtility.getConceptEntity(conceptName);
        Observation observation = observationParserUtility.getObservationEntity(concept, value);
        observation.setEncounter(encounter);
        observation.setPerson(patient);
        observation.setObservationDatetime(encounter.getEncounterDatetime());
        return observation;
    }

    private Encounter createEncounter(JSONObject encounterJSON) throws JSONException, ParseException {
        return observationParserUtility.getEncounterEntity(parse(encounterJSON.getString("encounter.encounter_datetime")), patient);
    }

    private Patient getPatient(JSONObject patient) throws JSONException, PatientController.PatientLoadException {
        String uuid = patient.getString("patient.uuid");
        return patientController.getPatientByUuid(uuid);
    }
}
