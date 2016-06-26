/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.service;

import com.muzima.api.model.Concept;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Patient;
import com.muzima.controller.ConceptController;
import com.muzima.controller.ObservationController;
import com.muzima.testSupport.CustomTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Date;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(CustomTestRunner.class)
public class ObservationParserUtilityTest {
    private ObservationParserUtility observationParserUtility;

    @Mock
    private ConceptController conceptController;

    @Mock
    private Patient patient;

    private String formDataUuid;

    @Before
    public void setUp() {
        initMocks(this);
        observationParserUtility = new ObservationParserUtility(conceptController);
        formDataUuid = "formDataUuid";
    }

    @Test
    public void shouldCreateEncounterEntityWithAppropriateValues() throws Exception {
        Date encounterDateTime = new Date();
        Encounter encounter = observationParserUtility.getEncounterEntity(encounterDateTime, patient,formDataUuid);
        assertTrue(encounter.getUuid().startsWith("encounterUuid"));
        assertThat(encounter.getEncounterType().getUuid(), is("encounterTypeForObservationsCreatedOnPhone"));
        assertThat(encounter.getProvider().getUuid(), is("providerForObservationsCreatedOnPhone"));
        assertThat(encounter.getEncounterDatetime(), is(encounterDateTime));
    }

    @Test
    public void shouldCreateNewConceptEntityAndAddItToListIfNotInDB() throws Exception,
            ConceptController.ConceptFetchException, ConceptController.ConceptParseException {
        observationParserUtility = new ObservationParserUtility(conceptController);
        when(conceptController.getConceptByName("ConceptName")).thenReturn(null);
        Concept concept = observationParserUtility.getConceptEntity("id^ConceptName^mm");
        assertThat(concept.getName(), is("ConceptName"));
        assertThat(concept.getConceptType().getName(), is("ConceptTypeCreatedOnThePhone"));
        assertThat(concept.isCreatedOnDevice(), is(true));
        assertThat(observationParserUtility.getNewConceptList().size(), is(1));
    }

    @Test
    public void shouldNotCreateNewConceptOrObservationForInvalidConceptName() throws Exception,
            ConceptController.ConceptFetchException {
        observationParserUtility = new ObservationParserUtility(conceptController);
        assertThat(observationParserUtility.getNewConceptList().size(), is(0));
    }

    @Test
    public void shouldNotCreateConceptIfAlreadyExistsInDB() throws Exception, ConceptController.ConceptFetchException,
            ConceptController.ConceptParseException{
        observationParserUtility = new ObservationParserUtility(conceptController);
        Concept mockConcept = mock(Concept.class);
        when(conceptController.getConceptByName("ConceptName")).thenReturn(mockConcept);

        Concept concept = observationParserUtility.getConceptEntity("id^ConceptName^mm");

        assertThat(concept, is(mockConcept));
        assertThat(concept.isCreatedOnDevice(), is(false));
        assertThat(observationParserUtility.getNewConceptList().isEmpty(), is(true));
    }

    @Test
    public void shouldCreateOnlyOneConceptForRepeatedConceptNames() throws Exception, ConceptController.ConceptFetchException,
            ConceptController.ConceptParseException{
        observationParserUtility = new ObservationParserUtility(conceptController);
        when(conceptController.getConceptByName("ConceptName")).thenReturn(null);

        Concept concept1 = observationParserUtility.getConceptEntity("id^ConceptName^mm");
        Concept concept2 = observationParserUtility.getConceptEntity("id^ConceptName^mm");

        assertThat(concept1, is(concept2));
        assertThat(concept1.isCreatedOnDevice(), is(true));
        assertThat(concept2.isCreatedOnDevice(), is(true));
        assertThat(observationParserUtility.getNewConceptList().size(), is(1));
    }

    @Test
    public void shouldCreateNumericObservation() throws Exception,
            ConceptController.ConceptFetchException, ConceptController.ConceptParseException,
            ObservationController.ParseObservationException{
        Concept concept = mock(Concept.class);
        when(concept.isNumeric()).thenReturn(true);
        when(concept.isCoded()).thenReturn(false);
        Observation observation = observationParserUtility.getObservationEntity(concept, "20.0");
        assertThat(observation.getValueNumeric(), is(20.0));
        assertTrue(observation.getUuid().startsWith("observationFromPhoneUuid"));
    }

    @Test
    public void shouldCreateValueCodedObsAndShouldAddItToNewConceptList() throws Exception,
            ConceptController.ConceptFetchException, ConceptController.ConceptParseException,
            ObservationController.ParseObservationException{
        observationParserUtility = new ObservationParserUtility(conceptController);
        Concept concept = mock(Concept.class);
        when(concept.isNumeric()).thenReturn(false);
        when(concept.isCoded()).thenReturn(true);
        Observation observation = observationParserUtility.getObservationEntity(concept, "id^obs_value^mm");

        assertThat(observation.getValueCoded(), is(notNullValue()));
        assertThat(observationParserUtility.getNewConceptList().size(), is(1));
    }

    @Test
    public void shouldCreateObsWithStringForNonNumericNonCodedConcept() throws Exception,
            ConceptController.ConceptFetchException, ConceptController.ConceptParseException,
            ObservationController.ParseObservationException{
        observationParserUtility = new ObservationParserUtility(conceptController);
        Concept concept = mock(Concept.class);
        when(concept.getName()).thenReturn("SomeConcept");
        when(concept.isNumeric()).thenReturn(false);
        when(concept.isCoded()).thenReturn(false);
        Observation observation = observationParserUtility.getObservationEntity(concept, "someString");
        assertThat(observation.getValueAsString(), is("someString"));
    }
}
