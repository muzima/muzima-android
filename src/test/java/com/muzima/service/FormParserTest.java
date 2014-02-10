package com.muzima.service;

import com.muzima.api.model.*;
import com.muzima.controller.ConceptController;
import com.muzima.controller.EncounterController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.PatientController;
import com.muzima.utils.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.List;
import java.util.Scanner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class FormParserTest {

    private FormParser formParser;

    @Mock
    private PatientController patientController;
    @Mock
    private ConceptController conceptController;
    @Mock
    private EncounterController encounterController;
    @Mock
    private ObservationController observationController;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void shouldCreateMultipleObservations() throws IOException, XmlPullParserException, ParseException, PatientController.PatientLoadException, ConceptController.ConceptFetchException {
        String xml = readFile("xml/histo_xml_payload.xml");
        formParser = new FormParser(new MXParser(), patientController, conceptController, encounterController, observationController);

        List<Observation> observations = formParser.parseAndSaveObservations(xml);
        assertThat(observations.size(), is(6));
    }

    @Test
    public void shouldAssociateCorrectConceptForObservation() throws IOException, XmlPullParserException, ParseException, PatientController.PatientLoadException, ConceptController.ConceptFetchException {
        String xml = readFile("xml/histo_xml_payload_one_observation.xml");
        formParser = new FormParser(new MXParser(), patientController, conceptController, encounterController, observationController);
        Concept aConcept = mock(Concept.class);
        String conceptName = "RETURN VISIT DATE";
        when(conceptController.getConceptByName(conceptName)).thenReturn(aConcept);

        List<Observation> observations = formParser.parseAndSaveObservations(xml);
        verify(conceptController).getConceptByName(conceptName);
        assertThat(observations.get(0).getConcept(), is(aConcept));
    }

    @Test
    public void shouldPrefixCreatedObservationsUuidWithCustomPrefix() throws IOException, XmlPullParserException, ParseException, PatientController.PatientLoadException, ConceptController.ConceptFetchException {
        String xml = readFile("xml/histo_xml_payload_one_observation.xml");
        formParser = new FormParser(new MXParser(), patientController, conceptController, encounterController, observationController);

        List<Observation> observations = formParser.parseAndSaveObservations(xml);
        assertThat(observations.get(0).getUuid(), containsString("observationFromPhoneUuid"));
    }

    @Test
    public void shouldAssociateCorrectEncounterForObservation() throws IOException, XmlPullParserException, ParseException, PatientController.PatientLoadException, ConceptController.ConceptFetchException {
        String xml = readFile("xml/histo_xml_payload_one_observation.xml");
        formParser = new FormParser(new MXParser(), patientController, conceptController, encounterController, observationController);

        List<Observation> observations = formParser.parseAndSaveObservations(xml);
        assertThat(observations.get(0).getEncounter().getEncounterDatetime(), is(DateUtils.parse("2014-02-01")));
    }

    @Test
    public void shouldAssociateCorrectPatient() throws IOException, XmlPullParserException, ParseException, PatientController.PatientLoadException, ConceptController.ConceptFetchException {
        String xml = readFile("xml/histo_xml_payload.xml");
        formParser = new FormParser(new MXParser(), patientController, conceptController, encounterController, observationController);
        Patient patient = new Patient();
        String patientUuid = "dd7963a8-1691-11df-97a5-7038c432aabf";
        when(patientController.getPatientByUuid(patientUuid)).thenReturn(patient);

        List<Observation> observations = formParser.parseAndSaveObservations(xml);
        verify(patientController).getPatientByUuid(patientUuid);
        assertThat(observations.get(0).getEncounter().getPatient(), is(patient));
        assertThat(observations.get(0).getPerson(), is((Person)patient));
    }


    public String readFile(String fileName) {
        InputStream fileStream = getClass().getClassLoader().getResourceAsStream(fileName);
        Scanner s = new Scanner(fileStream).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "{}";
    }
}
