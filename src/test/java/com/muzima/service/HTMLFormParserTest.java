package com.muzima.service;

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
        List<Observation> observations = htmlFormParser.parse(jsonResponse);

        assertThat(observations.size(), is(16));
        for (Observation observation : observations) {
            assertThat((Patient) observation.getPerson(), is(patient));
            assertThat(observation.getConcept(), notNullValue());
            assertThat(observation.getEncounter(), notNullValue());
        }
    }

    public String readFile() {
        InputStream fileStream = getClass().getClassLoader().getResourceAsStream("html/dispensary.json");
        Scanner s = new Scanner(fileStream).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "{}";
    }

}
