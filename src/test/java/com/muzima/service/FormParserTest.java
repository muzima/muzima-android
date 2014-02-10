package com.muzima.service;

import com.muzima.api.model.*;
import com.muzima.controller.ConceptController;
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
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class FormParserTest {

    private FormParser formParser;

    @Mock
    private PatientController patientController;

    @Mock
    private ObservationController observationController;

    @Mock
    private ConceptController conceptController;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void shouldCreateObservations() throws IOException, XmlPullParserException, ParseException, PatientController.PatientLoadException, ConceptController.ConceptFetchException {
        String xml = readFile("xml/histo_xml_payload.xml");
        formParser = new FormParser(xml, new MXParser(), patientController, conceptController, observationController);
        Patient patient = new Patient();
        when(patientController.getPatientByUuid("dd7963a8-1691-11df-97a5-7038c432aabf")).thenReturn(patient);
        when(conceptController.getConceptByName("5096^RETURN VISIT DATE^99DCT")).thenReturn(null);
        String conceptName = "8265^BODY PART^99DCT";
        Concept concept = mock(Concept.class);
        when(concept.getName()).thenReturn(conceptName);
        when(concept.getUuid()).thenReturn("SpecialUUID");
        when(conceptController.getConceptByName(conceptName)).thenReturn(concept);


        List<Observation> observations = formParser.parseForm();
        assertThat(observations.size(), is(6));
        for (Observation observation : observations) {
            assertThat((Patient) observation.getPerson(), is(patient));
            assertThat(observation.getUuid(), notNullValue());
            assertThat(observation.getConcept(), notNullValue());
            assertThat(observation.getEncounter(), notNullValue());
        }
        boolean conceptPresent = false;
        for (Observation observation : observations) {
            if (conceptName.equals(observation.getConcept().getName()) && "SpecialUUID".equals(observation.getConcept().getUuid())) {
                conceptPresent = true;
            }
        }
        assertThat(conceptPresent, is(true));
    }

    @Test
    public void shouldAssociateCorrectConceptForObservation() throws IOException, XmlPullParserException, ParseException, PatientController.PatientLoadException, ConceptController.ConceptFetchException {
        String xml = readFile("xml/histo_xml_payload_one_observation.xml");
        formParser = new FormParser(xml, new MXParser(), patientController, conceptController, observationController);
        Concept aConcept = mock(Concept.class);
        String conceptName = "5096^RETURN VISIT DATE^99DCT";
        when(conceptController.getConceptByName(conceptName)).thenReturn(aConcept);

        List<Observation> observations = formParser.parseForm();
        verify(conceptController).getConceptByName(conceptName);
        assertThat(observations.get(0).getConcept(), is(aConcept));
    }

    @Test
    public void shouldAssociateCorrectEncounterForObservation() throws IOException, XmlPullParserException, ParseException, PatientController.PatientLoadException, ConceptController.ConceptFetchException {
        String xml = readFile("xml/histo_xml_payload_one_observation.xml");
        formParser = new FormParser(xml, new MXParser(), patientController, conceptController, observationController);

        List<Observation> observations = formParser.parseForm();
        assertThat(observations.get(0).getEncounter().getEncounterDatetime(), is(DateUtils.parse("2014-02-01")));
    }

    @Test
    public void shouldAssociateCorrectPatient() throws IOException, XmlPullParserException, ParseException, PatientController.PatientLoadException, ConceptController.ConceptFetchException {
        String xml = readFile("xml/histo_xml_payload.xml");
        formParser = new FormParser(xml, new MXParser(), patientController, conceptController, observationController);
        Patient patient = new Patient();
        when(patientController.getPatientByUuid("dd7963a8-1691-11df-97a5-7038c432aabf")).thenReturn(patient);

        List<Observation> observations = formParser.parseForm();
        assertThat(observations.get(0).getEncounter().getPatient(), is(patient));
        assertThat(observations.get(0).getPerson(), is((Person)patient));
    }


    public String readFile(String fileName) {
        InputStream fileStream = getClass().getClassLoader().getResourceAsStream(fileName);
        Scanner s = new Scanner(fileStream).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "{}";
    }
}
