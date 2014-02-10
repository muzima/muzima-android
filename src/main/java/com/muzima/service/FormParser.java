package com.muzima.service;

import com.muzima.api.model.Concept;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Patient;
import com.muzima.controller.ConceptController;
import com.muzima.controller.EncounterController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.PatientController;
import com.muzima.utils.DateUtils;
import com.muzima.utils.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.UUID;

import static android.util.Xml.newPullParser;

public class FormParser {

    private final PatientController patientController;
    private final ConceptController conceptController;
    private XmlPullParser parser;

    private Patient patient;
    private Encounter encounter;
    private List<Observation> observations;
    private String observationFromPhoneUuidPrefix = "observationFromPhoneUuid";

    public FormParser(PatientController patientController, ConceptController conceptController, EncounterController encounterController, ObservationController observationController) {
        this(newPullParser(), patientController, conceptController, encounterController, observationController);

    }

    public FormParser(XmlPullParser parser, PatientController patientController,
                      ConceptController conceptController, EncounterController encounterController, ObservationController observationController) {
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
    }

    public List<Observation> parseForm(String xml) throws XmlPullParserException, IOException,
        ParseException, PatientController.PatientLoadException, ConceptController.ConceptFetchException {
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
        encounter.setUuid("encounterUuid" + UUID.randomUUID());

        for (Observation observation : observations) {
            observation.setPerson(patient);
            observation.setEncounter(encounter);
            observation.setUuid(observationFromPhoneUuidPrefix + UUID.randomUUID());
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

    private List<Observation> createObservations(XmlPullParser parser) throws XmlPullParserException, IOException, ConceptController.ConceptFetchException {
        List<Observation> observationList = new ArrayList<Observation>();
        Stack<String> conceptNames = new Stack<String>();
        while (!isEndOf("obs")) {
            if (parser.getEventType() == XmlPullParser.START_TAG) {
                String conceptName = parser.getAttributeValue("", "concept");
                if (conceptName != null) {
                    conceptNames.push(conceptName);
                }
                if (isStartOf("value")) {
                    observationList.add(createObservation(conceptNames.peek(), parser.nextText()));
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

    private Observation createObservation(String conceptName, String conceptValue) throws XmlPullParserException, IOException, ConceptController.ConceptFetchException {
        if (conceptValue != null) {
            String myConceptName = getConceptName(conceptName);
            Concept concept = conceptController.getConceptByName(myConceptName);
            if (concept == null) {
                concept = createDummyConcept(myConceptName);
            }
            Observation observation = new Observation();
            observation.setConcept(concept);
            observation.setValueText(conceptValue);
            return observation;
        }
        return null;
    }

    private Concept createDummyConcept(String conceptName) {
        return new Concept();
    }

    private String checkForValueInConcept(XmlPullParser parser, String conceptName) throws XmlPullParserException, IOException {
        String initConceptTagName = parser.getName();
        System.out.println("Parser: " + parser.getName() + " Concept: " + conceptName);
        System.out.println(initConceptTagName);
        while (!isEndOf(initConceptTagName)) {
            if (isStartOf("value")) {
                return parser.nextText();
            }
            parser.next();
        }
        return null;
    }


    public class ParseFormException extends RuntimeException {

        public ParseFormException(Exception e) {
            super(e);
        }
    }

    private static String getConceptName(String peek) {
        if (!StringUtils.isEmpty(peek) && peek.split("\\^").length > 1) {
            return peek.split("\\^")[1];
        }
        return "";
    }
}
