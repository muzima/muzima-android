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

import com.muzima.MuzimaApplication;
import com.muzima.api.model.Concept;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Patient;
import com.muzima.controller.ConceptController;
import com.muzima.controller.EncounterController;
import com.muzima.controller.FormController;
import com.muzima.controller.LocationController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.PatientController;
import com.muzima.controller.ProviderController;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class HTMLFormObservationCreatorTest {

    private HTMLFormObservationCreator htmlFormObservationCreator;

    @Mock
    private
    MuzimaApplication muzimaApplication;

    @Mock
    private PatientController patientController;

    @Mock
    private ConceptController conceptController;

    @Mock
    private EncounterController encounterController;

    @Mock
    private ObservationController observationController;

    @Mock
    private LocationController locationController;

    @Mock
    private ProviderController providerController;

    @Mock
    private FormController formController;

    @Mock
    private Patient patient;

    @Captor
    private
    ArgumentCaptor<List<Encounter>> encounterArgumentCaptor;

    @Captor
    private
    ArgumentCaptor<List<Observation>> observationArgumentCaptor;

    @Captor
    private
    ArgumentCaptor<List<Concept>> conceptArgumentCaptor;

    private String mockConceptName;
    private String mockConceptUUID;
    private String formDataUuid;


    @Before
    public void setUp() throws PatientController.PatientLoadException, ConceptController.ConceptFetchException {
        initMocks(this);
        when(muzimaApplication.getPatientController()).thenReturn(patientController);
        when(muzimaApplication.getConceptController()).thenReturn(conceptController);
        when(muzimaApplication.getEncounterController()).thenReturn(encounterController);
        when(muzimaApplication.getFormController()).thenReturn(formController);
        when(muzimaApplication.getObservationController()).thenReturn(observationController);
        when(muzimaApplication.getProviderController()).thenReturn(providerController);
        when(muzimaApplication.getLocationController()).thenReturn(locationController);
        htmlFormObservationCreator = new HTMLFormObservationCreator(muzimaApplication);

        when(patientController.getPatientByUuid("9090900-asdsa-asdsannidj-qwnkika")).thenReturn(patient);

        Concept concept = mock(Concept.class);
        mockConceptName = "HEIGHT (CM)";
        mockConceptUUID = "MyUUID";
        when(concept.getName()).thenReturn(mockConceptName);
        when(concept.getUuid()).thenReturn(mockConceptUUID);

        when(conceptController.getConceptByName(mockConceptName)).thenReturn(concept);
        formDataUuid = "formDataUuid";
    }

    @Test
    public void shouldParseJSONResponseAndCreateObservation() {

        htmlFormObservationCreator.createAndPersistObservations(readFile(),formDataUuid);
        List<Observation> observations = htmlFormObservationCreator.getObservations();
        assertTrue("Number of processed observations should be 30. Currently " + observations.size() + " On file : ",
                observations.size() == 30);
    }

    @Test
    public void shouldCheckIfAllObservationsHaveEncounterObservationTimeAndPatient() {
        htmlFormObservationCreator.createAndPersistObservations(readFile(),formDataUuid);
        List<Observation> observations = htmlFormObservationCreator.getObservations();

        for (Observation observation : observations) {
            assertThat((Patient) observation.getPerson(), is(patient));
            assertThat(observation.getEncounter(), notNullValue());
            assertThat(observation.getObservationDatetime(), notNullValue());
            assertThat(observation.getValueCoded(), notNullValue());
            assertThat(observation.getValueCoded().getConceptType(), notNullValue());
        }
    }

    @Test
    public void shouldCheckIfEncounterHasMinimumAttributes() {
        htmlFormObservationCreator.createAndPersistObservations(readFile(),formDataUuid);
        List<Observation> observations = htmlFormObservationCreator.getObservations();
        assertThat(observations, notNullValue());
        assertTrue(observations.size() > 0);
        Encounter encounter = observations.get(0).getEncounter();
        assertThat(encounter.getEncounterDatetime(), notNullValue());
        assertThat(encounter.getPatient(), notNullValue());
        assertThat(encounter.getProvider(), notNullValue());
        assertThat(encounter.getLocation(), notNullValue());
        assertThat(encounter.getUserSystemId(), notNullValue());
    }

    @Test
    public void shouldCheckIfAllObservationsHasEitherAFetchedConceptOrNewConcept() {
        htmlFormObservationCreator.createAndPersistObservations(readFile(),formDataUuid);
        List<Observation> observations = htmlFormObservationCreator.getObservations();

        boolean conceptUuidAsserted = false;
        for (Observation observation : observations) {
            assertThat(observation.getConcept(), notNullValue());
            conceptUuidAsserted = isMockConceptPresent(mockConceptName, mockConceptUUID, conceptUuidAsserted, observation);
        }
        assertTrue("Expected Concept name is not present", conceptUuidAsserted);
    }

    @Test
    public void shouldCheckIfMultipleObservationsAreCreatedForMultiValuedConcepts() {
        htmlFormObservationCreator.createAndPersistObservations(readFile(),formDataUuid);
        List<Observation> observations = htmlFormObservationCreator.getObservations();
        List<Observation> multiValuedObservations = new ArrayList<>();
        for (Observation observation : observations) {
            if (observation.getConcept().getName().equals("REFERRALS ORDERED")) {
                multiValuedObservations.add(observation);
            }
        }
        List<String> expectedValues = asList("REFERRED TO CLINIC", "DISPENSARY");
        assertThat(multiValuedObservations.size(), is(2));
        assertThat(multiValuedObservations.get(0).getValueCoded(), notNullValue());
        assertThat(expectedValues, hasItem(multiValuedObservations.get(0).getValueCoded().getName()));
        assertThat(multiValuedObservations.get(1).getValueCoded(), notNullValue());
        assertThat(expectedValues, hasItem(multiValuedObservations.get(1).getValueCoded().getName()));
    }

    @Test
    public void shouldVerifyAllObservationsAndRelatedEntitiesAreSaved() throws EncounterController.SaveEncounterException, ConceptController.ConceptSaveException, ObservationController.SaveObservationException {
        htmlFormObservationCreator.createAndPersistObservations(readFile(),formDataUuid);

        verify(encounterController).saveEncounters(encounterArgumentCaptor.capture());
        assertThat(encounterArgumentCaptor.getValue().size(), is(1));

        verify(conceptController).saveConcepts(conceptArgumentCaptor.capture());

        List<Concept> value = conceptArgumentCaptor.getValue();
        assertThat(value.size(), is(33));

        verify(observationController).saveObservations(observationArgumentCaptor.capture());
        assertThat(observationArgumentCaptor.getValue().size(), is(30));
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
