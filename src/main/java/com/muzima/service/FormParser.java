package com.muzima.service;

import com.muzima.api.model.Concept;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Patient;
import com.muzima.controller.ConceptController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.PatientController;
import com.muzima.utils.DateUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.util.Xml.newPullParser;

public class FormParser {

    private final String xml;
    private final PatientController patientController;
    private final ConceptController conceptController;
    private final ObservationController observationController;
    private XmlPullParser parser;

    private Patient patient;
    private Encounter encounter;
    private List<Observation> observations;

    public FormParser(String xml, PatientController patientController, ConceptController conceptController, ObservationController observationController) {
        this(xml, newPullParser(), patientController, conceptController, observationController);

    }

    public FormParser(String xml, XmlPullParser parser, PatientController patientController,
                      ConceptController conceptController, ObservationController observationController) {
        try {
            if (parser != null) {
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            }
        } catch (XmlPullParserException e) {
            throw new ParseFormException(e);
        }
        this.parser = parser;
        this.xml = xml;
        this.patientController = patientController;
        this.conceptController = conceptController;
        this.observationController = observationController;
    }

    public List<Observation> parseForm() throws XmlPullParserException, IOException,
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

        for (Observation observation : observations) {
            observation.setPerson(patient);
            observation.setEncounter(encounter);
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
        while (!isEndOf("obs")) {
            if (parser.getEventType() == XmlPullParser.START_TAG) {
                String conceptName = parser.getAttributeValue("", "concept");
                if (conceptName != null) {
                    observationList.add(createObservation(parser, conceptName));
                }
            }
            parser.next();
        }
        observationList.removeAll(Collections.singleton(null));
        return observationList;
    }

    private Observation createObservation(XmlPullParser parser, String conceptName) throws XmlPullParserException, IOException, ConceptController.ConceptFetchException {
        String conceptValue = checkForValueInConcept(parser);
        if (conceptValue != null) {
            Concept concept = conceptController.getConceptByName(conceptName);
            if (concept == null) {
                concept = createDummyConcept(conceptName);
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

    private String checkForValueInConcept(XmlPullParser parser) throws XmlPullParserException, IOException {
        String initConceptTagName = parser.getName();
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
}
