/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.service;

import android.util.Log;
import com.muzima.MuzimaApplication;
import com.muzima.api.model.Concept;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Patient;
import com.muzima.controller.ConceptController;
import com.muzima.controller.EncounterController;
import com.muzima.controller.LocationController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.PatientController;
import com.muzima.controller.ProviderController;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import static com.muzima.utils.DateUtils.parse;

public class FormParser {

    private final ConceptController conceptController;
    private final EncounterController encounterController;
    private final ObservationController observationController;
    private final PatientController patientController;
    private final ObservationParserUtility observationParserUtility;

    private XmlPullParser parser;

    private Patient patient;
    private Encounter encounter;
    private List<Observation> observations;

    public FormParser(MuzimaApplication muzimaApplication) {

        LocationController locationController = muzimaApplication.getLocationController();
        ProviderController providerController = muzimaApplication.getProviderController();
        this.conceptController = muzimaApplication.getConceptController();
        this.encounterController = muzimaApplication.getEncounterController();
        this.observationController = muzimaApplication.getObservationController();
        this.patientController = muzimaApplication.getPatientController();
        try {
            if (parser != null) {
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            }
        } catch (XmlPullParserException e) {
            throw new ParseFormException(e);
        }
        this.parser = parser;
        this.observationParserUtility = new ObservationParserUtility(muzimaApplication);
    }

    public void parseAndSaveObservations(String xml, String formDataUuid)
            throws XmlPullParserException, IOException, ParseException, PatientController.PatientLoadException,
            ConceptController.ConceptFetchException, ConceptController.ConceptSaveException,
            ConceptController.ConceptParseException, ObservationController.ParseObservationException {
        parser.setInput(new ByteArrayInputStream(xml.getBytes()), null);
        parser.nextTag();
        while (!isEndOf("form")) {
            if (isStartOf("patient")) {
                patient = getPatient(parser);
            }
            if (isStartOf("encounter")) {
                encounter = createEncounter(parser,formDataUuid);
            }
            if (isStartOf("obs")) {
                observations = createObservations(parser);
            }
            parser.next();
        }
        associatePatientsWithEncountersAndObservations();
    }

    private void associatePatientsWithEncountersAndObservations() {
        encounter.setPatient(patient);

        for (Observation observation : observations) {
            observation.setPerson(patient);
            observation.setEncounter(encounter);
            observation.setObservationDatetime(encounter.getEncounterDatetime());
            observation.setUuid(observationParserUtility.getObservationUuid());
        }

        try {
            encounterController.saveEncounter(encounter);
            conceptController.saveConcepts(observationParserUtility.getNewConceptList());
            observationController.saveObservations(observations);
        } catch (EncounterController.SaveEncounterException | ConceptController.ConceptSaveException | ObservationController.SaveObservationException e) {
            Log.e(this.getClass().getSimpleName(), "Saving encounter throwing exception!", e);
        }
    }

    private Patient getPatient(XmlPullParser parser) throws XmlPullParserException, IOException, PatientController.PatientLoadException {
        while (!isEndOf("patient")) {
            if (isStartOf("patient.uuid")) {
                return patientController.getPatientByUuid(parser.nextText());
            }
            parser.next();
        }
        return null;

    }

    private Encounter createEncounter(XmlPullParser parser, String formDataUuid) throws XmlPullParserException, IOException, ParseException {
        Encounter encounter = null;
        String formUuid = "";
        String providerId="";
        int locationId=0;
        String userSystemId="";
        while (!isEndOf("encounter")) {
            if (isStartOf("encounter.form_uuid")) {
                 formUuid = String.valueOf(parser.nextText());
            }
            if (isStartOf("encounter.provider_id")) {
                 providerId = String.valueOf(parser.nextText());
            }
            if (isStartOf("encounter.location_id")) {
                 locationId = Integer.parseInt(String.valueOf(parser.nextText()));
            }
            if (isStartOf("encounter.user_system_id")) {
                userSystemId = String.valueOf(parser.nextText());
            }
            if (isStartOf("encounter.encounter_datetime")) {
                encounter = observationParserUtility.getEncounterEntity(parse(parser.nextText()), formUuid, providerId, locationId, userSystemId, patient, formDataUuid);
            }
            parser.next();
        }
        return encounter;
    }

    private boolean isEndOf(String tagName) throws XmlPullParserException {
        return parser.getEventType() == XmlPullParser.END_TAG && tagName.equals(parser.getName());
    }

    private boolean isStartOf(String tagName) throws XmlPullParserException {
        return parser.getEventType() == XmlPullParser.START_TAG && tagName.equals(parser.getName());
    }

    private List<Observation> createObservations(XmlPullParser parser) throws XmlPullParserException,
            IOException, ConceptController.ConceptFetchException,
            ConceptController.ConceptParseException, ObservationController.ParseObservationException{
        List<Observation> observationList = new ArrayList<>();
        Stack<String> conceptNames = new Stack<>();
        while (!isEndOf("obs")) {
            if (parser.getEventType() == XmlPullParser.START_TAG) {
                String conceptName = parser.getAttributeValue("", "concept");
                if (conceptName != null) {
                    conceptNames.push(conceptName);
                }
                String multipleSelect = parser.getAttributeValue("", "multipleSelect");
                boolean isMultipleSelect = false;
                if (multipleSelect != null) {
                    isMultipleSelect = multipleSelect.equalsIgnoreCase("true");
                    if (isMultipleSelect) {
                        String questionConceptName = parser.getName();
                        while (!isEndOf(questionConceptName)) {
                            parser.next();
                            if (parser.getEventType() == XmlPullParser.START_TAG) {
                                String codedObservationName = parser.getAttributeValue("", "concept");
                                if (codedObservationName != null) {
                                    observationList.add(getObservation(conceptNames, codedObservationName));
                                }
                            }
                        }
                    }
                } else if (isStartOf("value")) {
                    observationList.add(getObservation(conceptNames, parser.nextText()));
                }
            }
            if (parser.getEventType() == XmlPullParser.END_TAG && !conceptNames.empty()) {
                if (!(parser.getName().equals("value") || parser.getName().equals("date") || parser.getName().equals("time"))) {
                    conceptNames.pop();
                }
            }
            parser.next();
        }
        observationList.removeAll(Collections.singleton(null));
        return observationList;
    }

    private Observation getObservation(Stack<String> conceptNames, String codedObservationName)
            throws ConceptController.ConceptFetchException, ConceptController.ConceptParseException,
            ObservationController.ParseObservationException {
        Concept conceptEntity = observationParserUtility.getConceptEntity(conceptNames.peek(),false);
        return observationParserUtility.getObservationEntity(conceptEntity, codedObservationName);
    }

    class ParseFormException extends RuntimeException {
        ParseFormException(Exception e) {
            super(e);
        }
    }
}