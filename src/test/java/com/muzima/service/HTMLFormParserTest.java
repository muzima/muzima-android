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
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class HTMLFormParserTest {
    private HTMLFormParser htmlFormParser;

    @Mock
    private PatientController patientController;

    @Mock
    private ConceptController conceptController;

    @Mock
    private Patient patient;

    private String mockConceptName;
    private String mockConceptUUID;


    @Before
    public void setUp() throws PatientController.PatientLoadException, ConceptController.ConceptFetchException {
        initMocks(this);
        htmlFormParser = new HTMLFormParser(patientController, conceptController);

        when(patientController.getPatientByUuid("9090900-asdsa-asdsannidj-qwnkika")).thenReturn(patient);

        Concept concept = mock(Concept.class);
        mockConceptName = "HEIGHT (CM)";
        mockConceptUUID = "MyUUID";
        when(concept.getName()).thenReturn(mockConceptName);
        when(concept.getUuid()).thenReturn(mockConceptUUID);

        when(conceptController.getConceptByName(mockConceptName)).thenReturn(concept);
    }

    @Test
    public void shouldParseJSONResponseAndCreateObservation() throws PatientController.PatientLoadException,
            JSONException, ParseException, ConceptController.ConceptFetchException {

        List<Observation> observations = htmlFormParser.parse(readFile());

        assertThat(observations.size(), is(18));
    }

    @Test
    public void shouldCheckIfAllObservationsHaveEncounterAndPatient() throws Exception, PatientController.PatientLoadException, ConceptController.ConceptFetchException {
        List<Observation> observations = htmlFormParser.parse(readFile());

        for (Observation observation : observations) {
            assertThat((Patient) observation.getPerson(), is(patient));
            assertThat(observation.getEncounter(), notNullValue());
        }
    }

    @Test
    public void shouldCheckIfAllObservationsHasEitherAFetchedConceptOrNewConcept() throws Exception, PatientController.PatientLoadException, ConceptController.ConceptFetchException {
        List<Observation> observations = htmlFormParser.parse(readFile());

        boolean conceptUuidAsserted = false;
        for (Observation observation : observations) {
            assertThat(observation.getConcept(), notNullValue());
            conceptUuidAsserted = isMockConceptPresent(mockConceptName, mockConceptUUID, conceptUuidAsserted, observation);
        }
        assertTrue("Expected Concept name is not present", conceptUuidAsserted);
    }

    @Test
    public void shouldCheckIfMultipleObservationsAreCreatedForMultiValuedConcepts() throws Exception, PatientController.PatientLoadException,
            ConceptController.ConceptFetchException {
        List<Observation> observations = htmlFormParser.parse(readFile());
        List<Observation> multiValuedObservations = new ArrayList<Observation>();
        for (Observation observation : observations) {
            if (observation.getConcept().getName().equals("REFERRALS ORDERED")) {
                multiValuedObservations.add(observation);
            }
        }
        List<String> expectedValues = asList("7056^REFERRED TO CLINIC^99DCT", "6436^DISPENSARY^99DCT");
        assertThat(multiValuedObservations.size(), is(2));
        assertThat(expectedValues, hasItem(multiValuedObservations.get(0).getValueText()));
        assertThat(expectedValues, hasItem(multiValuedObservations.get(1).getValueText()));

    }

    private boolean isMockConceptPresent(String mockConceptName, String mockConceptUUID,
                                         boolean conceptUuidAsserted, Observation observation) {
        if (mockConceptName.equals(observation.getConcept().getName())) {
            assertThat(observation.getConcept().getUuid(), is(mockConceptUUID));
            conceptUuidAsserted = true;
        }
        return conceptUuidAsserted;
    }

    public String readFile() {
        InputStream fileStream = getClass().getClassLoader().getResourceAsStream("html/dispensary.json");
        Scanner s = new Scanner(fileStream).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "{}";
    }

}
