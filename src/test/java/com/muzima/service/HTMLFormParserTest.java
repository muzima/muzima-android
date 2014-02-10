package com.muzima.service;

import com.muzima.api.model.Concept;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Patient;
import com.muzima.controller.ConceptController;
import com.muzima.controller.PatientController;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.InputStream;
import java.text.ParseException;
import java.util.List;
import java.util.Scanner;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class HTMLFormParserTest {
    private HTMLFormParser htmlFormParser;

    @Mock
    private PatientController patientController;

    @Mock
    private ConceptController conceptController;

    @Before
    public void setUp() {
        initMocks(this);
        htmlFormParser = new HTMLFormParser(patientController, conceptController);
    }

    @Test
    public void shouldParseJSONResponseAndCreateObservation() throws PatientController.PatientLoadException,
            JSONException, ParseException, ConceptController.ConceptFetchException {
        String jsonResponse = readFile();
        Patient patient = new Patient();
        when(patientController.getPatientByUuid("9090900-asdsa-asdsannidj-qwnkika")).thenReturn(patient);

        Concept concept = mock(Concept.class);
        String mockConceptName = "HEIGHT (CM)";
        String mockConceptUUID = "MyUUID";
        when(concept.getName()).thenReturn(mockConceptName);
        when(concept.getUuid()).thenReturn(mockConceptUUID);

        when(conceptController.getConceptByName(mockConceptName)).thenReturn(concept);

        List<Observation> observations = htmlFormParser.parse(jsonResponse);
        boolean conceptUuidAsserted = false;
        assertThat(observations.size(), is(16));
        for (Observation observation : observations) {
            assertThat(observation.getConcept(), notNullValue());
            if (mockConceptName.equals(observation.getConcept().getName())) {
                assertThat(observation.getConcept().getUuid(), is(mockConceptUUID));
                conceptUuidAsserted = true;
            }
            assertThat((Patient) observation.getPerson(), is(patient));
            assertThat(observation.getEncounter(), notNullValue());
        }
        assertTrue("Expected Concept name is not present",conceptUuidAsserted);
    }

    public String readFile() {
        InputStream fileStream = getClass().getClassLoader().getResourceAsStream("html/dispensary.json");
        Scanner s = new Scanner(fileStream).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "{}";
    }

}
