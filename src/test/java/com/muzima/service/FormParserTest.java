package com.muzima.service;

import com.muzima.api.model.Concept;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Patient;
import com.muzima.controller.ConceptController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.PatientController;
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
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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
        String xml = readFile();
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


    public String readFile() {
        InputStream fileStream = getClass().getClassLoader().getResourceAsStream("xml/histo_xml_payload.xml");
        Scanner s = new Scanner(fileStream).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "{}";
    }
}
