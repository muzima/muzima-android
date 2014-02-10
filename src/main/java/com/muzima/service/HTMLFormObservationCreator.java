package com.muzima.service;

import android.util.Log;
import com.muzima.api.model.*;
import com.muzima.controller.ConceptController;
import com.muzima.controller.EncounterController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.PatientController;
import com.muzima.utils.DateUtils;
import com.muzima.utils.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static java.util.Arrays.asList;

public class HTMLFormObservationCreator {

    private PatientController patientController;
    private ConceptController conceptController;
    private EncounterController encounterController;
    private ObservationController observationController;

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
                observations.addAll(createMultipleObservation(getConceptName(key), observationJSON.getJSONArray(key)));
            } else {
                observations.add(createObservation(getConceptName(key), observationJSON.getString(key)));
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
        Concept concept = conceptController.getConceptByName(conceptName);
        if (concept == null) {
            concept = findConceptInNewConcepts(conceptName);
            if (concept == null) {
                concept = createNewConcept(conceptName);
                newConcepts.add(concept);
            }
        }
        Observation observation = new Observation();
        observation.setConcept(concept);
        observation.setEncounter(encounter);
        observation.setPerson(patient);
        observation.setValueText(value);
        return observation;
    }

    private Concept findConceptInNewConcepts(String conceptName) {
        for (Concept newConcept : newConcepts) {
            if (newConcept.getName().equals(conceptName))
                return newConcept;
        }
        return null;
    }

    private Concept createNewConcept(String conceptNameString) {
        Concept concept = new Concept();
        ConceptType conceptType = new ConceptType();
        conceptType.setName("ConceptCreatedOnDevice");
        concept.setConceptType(conceptType);
        ConceptName conceptName = new ConceptName();
        conceptName.setName(conceptNameString);
        conceptName.setPreferred(true);
        concept.setConceptNames(asList(conceptName));
        return concept;
    }

    private Encounter createEncounter(JSONObject encounterJSON) throws JSONException, ParseException {
        Encounter encounter = new Encounter();
        EncounterType encounterType = new EncounterType();
        encounterType.setName("EncounterCreatedOnDevice");
        encounter.setEncounterType(encounterType);
        encounter.setEncounterDatetime(DateUtils.parse(encounterJSON.getString("encounter.encounter_datetime")));
        return encounter;
    }

    private Patient getPatient(JSONObject patient) throws JSONException, PatientController.PatientLoadException {
        String uuid = patient.getString("patient.uuid");
        return patientController.getPatientByUuid(uuid);
    }

    private static String getConceptName(String conceptName) {
        if (!StringUtils.isEmpty(conceptName) && conceptName.split("\\^").length > 1) {
            return conceptName.split("\\^")[1].trim();
        }
        return "";
    }
}
