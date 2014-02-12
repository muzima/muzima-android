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
import com.muzima.utils.DateUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

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
    private List<Concept> newConcepts;
    private String TAG = "HTMLFormObservationCreator";

    public HTMLFormObservationCreator(PatientController patientController, ConceptController conceptController,
                                      EncounterController encounterController, ObservationController observationController) {
        this.patientController = patientController;
        this.conceptController = conceptController;
        this.encounterController = encounterController;
        this.observationController = observationController;
        this.newConcepts = new ArrayList<Concept>();
        this.observationParserUtility = ObservationParserUtility.getInstance();
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
            observations = createObservations(responseJSON.getJSONObject("observation"));
        } catch (PatientController.PatientLoadException e) {
            Log.e(TAG, "Error while fetching Patient");
        } catch (ConceptController.ConceptFetchException e) {
            Log.e(TAG, "Error while fetching Concept");
        } catch (JSONException e) {
            Log.e(TAG, "Error while parsing response JSON");
        } catch (ParseException e) {
            Log.e(TAG, "Error while parsing response JSON");
        }
    }

    private void saveObservationsAndRelatedEntities() throws EncounterController.SaveEncounterException,
            ObservationController.SaveObservationException, ConceptController.ConceptSaveException {
        encounterController.saveEncounters(asList(encounter));
        conceptController.saveConcepts(newConcepts);
        observationController.saveObservations(observations);
    }

    private List<Observation> createObservations(JSONObject observationJSON) throws ConceptController.ConceptFetchException, JSONException {
        List<Observation> observations = new ArrayList<Observation>();
        Iterator keys = observationJSON.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            if (observationJSON.get(key) instanceof JSONArray) {
                observations.addAll(createMultipleObservation(key, observationJSON.getJSONArray(key)));
            } else {
                observations.add(createObservation(key, observationJSON.getString(key)));
            }
        }
        return observations;
    }

    private List<Observation> createMultipleObservation(String conceptName, JSONArray jsonArray) throws JSONException,
            ConceptController.ConceptFetchException {
        List<Observation> observations = new ArrayList<Observation>();
        for (int i = 0; i < jsonArray.length(); i++) {
            observations.add(createObservation(conceptName, jsonArray.getString(i)));
        }
        return observations;
    }

    private Observation createObservation(String conceptName, String value) throws JSONException, ConceptController.ConceptFetchException {
        Observation observation = observationParserUtility.createObservation(conceptName, value, conceptController);

        observation.setEncounter(encounter);
        observation.setPerson(patient);
        observation.setObservationDatetime(encounter.getEncounterDatetime());
        observation.setUuid(observationParserUtility.OBSERVATION_ON_PHONE_UUID_PREFIX + UUID.randomUUID());
        return observation;
    }

    private Encounter createEncounter(JSONObject encounterJSON) throws JSONException, ParseException {
        Encounter encounterEntity = observationParserUtility.getEncounterEntity(DateUtils.parse(encounterJSON.getString("encounter.encounter_datetime")));
        encounterEntity.setPatient(patient);
        return encounterEntity;
    }


    private Patient getPatient(JSONObject patient) throws JSONException, PatientController.PatientLoadException {
        String uuid = patient.getString("patient.uuid");
        return patientController.getPatientByUuid(uuid);
    }
}
