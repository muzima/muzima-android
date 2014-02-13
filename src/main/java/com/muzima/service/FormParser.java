package com.muzima.service;

import com.muzima.api.model.Encounter;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Patient;
import com.muzima.controller.ConceptController;
import com.muzima.controller.EncounterController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.PatientController;
import com.muzima.utils.DateUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import static android.util.Xml.newPullParser;

public class FormParser {

    private final PatientController patientController;
    private final ConceptController conceptController;
    private final EncounterController encounterController;
    private final ObservationController observationController;
    private final ObservationParserUtility observationParserUtility;

    private XmlPullParser parser;

    private Patient patient;
    private Encounter encounter;
    private List<Observation> observations;

    public FormParser(PatientController patientController, ConceptController conceptController, EncounterController encounterController, ObservationController observationController) {
        this(newPullParser(), patientController, conceptController, encounterController, observationController);

    }

    public FormParser(XmlPullParser parser, PatientController patientController,
                      ConceptController conceptController, EncounterController encounterController, ObservationController observationController) {
        this.encounterController = encounterController;
        this.observationController = observationController;
        try {
            if (parser != null) {
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            }
        } catch (XmlPullParserException e) {
            throw new ParseFormException(e);
        }
        this.parser = parser;
        this.patientController = patientController;
        this.conceptController = conceptController;
        this.observationParserUtility = ObservationParserUtility.getInstance();
    }

    public List<Observation> parseAndSaveObservations(String xml) throws XmlPullParserException, IOException,
            ParseException, PatientController.PatientLoadException, ConceptController.ConceptFetchException, ConceptController.ConceptSaveException {
        parser.setInput(new ByteArrayInputStream(xml.getBytes()), null);
        parser.nextTag();
        while (!isEndOf("form")) {
            if (isStartOf("patient")) {
                patient = getPatient(parser);
            }
            if (isStartOf("encounter")) {
                encounter = createEncounter(parser);
            }
            if (isStartOf("obs")) {
                observations = createObservations(parser);
            }
            parser.next();
        }
        associatePatientsWithEncountersAndObservations();
        return observations;
    }

    private void associatePatientsWithEncountersAndObservations() {
        encounter.setPatient(patient);
        encounter.setUuid(observationParserUtility.getEncounterUUID());

        for (Observation observation : observations) {
            observation.setPerson(patient);
            observation.setEncounter(encounter);
            observation.setObservationDatetime(encounter.getEncounterDatetime());
            observation.setUuid(observationParserUtility.getObservationUuid());
        }

        try {
            encounterController.saveEncounter(encounter);
            observationController.saveObservations(observations);
        } catch (EncounterController.SaveEncounterException e) {
            e.printStackTrace();
        } catch (ObservationController.SaveObservationException e) {
            e.printStackTrace();
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

    private Encounter createEncounter(XmlPullParser parser) throws XmlPullParserException, IOException, ParseException {
        Encounter encounter = new Encounter();
        while (!isEndOf("encounter")) {
            if (isStartOf("encounter.encounter_datetime")) {
                encounter.setEncounterDatetime(DateUtils.parse(parser.nextText()));
                encounter.setProvider(observationParserUtility.getDummyProvider());
                encounter.setLocation(observationParserUtility.getDummyLocation());
                encounter.setEncounterType(observationParserUtility.getDummyEncounterType());
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

    private List<Observation> createObservations(XmlPullParser parser) throws XmlPullParserException, IOException, ConceptController.ConceptFetchException, ParseException, ConceptController.ConceptSaveException {
        List<Observation> observationList = new ArrayList<Observation>();
        Stack<String> conceptNames = new Stack<String>();
        while (!isEndOf("obs")) {
            if (parser.getEventType() == XmlPullParser.START_TAG) {
                String conceptName = parser.getAttributeValue("", "concept");
                if (conceptName != null) {
                    conceptNames.push(conceptName);
                }
                String multipleSelect = parser.getAttributeValue("", "multipleSelect");
                boolean isMultipleSelect = false;
                if(multipleSelect != null){
                    isMultipleSelect = multipleSelect.equalsIgnoreCase("true");
                    if(isMultipleSelect){
                        String questionConceptname = parser.getName();
                        while (!isEndOf(questionConceptname)){
                            parser.next();
                            if (parser.getEventType() == XmlPullParser.START_TAG) {
                                String codedObservationName = parser.getAttributeValue("", "concept");
                                if (codedObservationName != null) {
                                    Observation codeObservation = observationParserUtility.createObservation(conceptNames.peek(), codedObservationName, conceptController);
                                    observationList.add(codeObservation);
                                    }
                                }
                        }
                    }
                } else if (isStartOf("value")) {
                    Observation newObservation = observationParserUtility.createObservation(conceptNames.peek(), parser.nextText(), conceptController);
                    observationList.add(newObservation);
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

    public class ParseFormException extends RuntimeException {
        public ParseFormException(Exception e) {
            super(e);
        }
    }
}