/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
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
import com.muzima.api.model.EncounterType;
import com.muzima.api.model.Form;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Provider;
import com.muzima.controller.ConceptController;
import com.muzima.controller.FormController;
import com.muzima.controller.LocationController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.ProviderController;
import com.muzima.testSupport.CustomTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(CustomTestRunner.class)
@Config(manifest = Config.NONE)
public class ObservationParserUtilityTest {
    private ObservationParserUtility observationParserUtility;

    @Mock
    private
    MuzimaApplication muzimaApplication;

    @Mock
    private ConceptController conceptController;

    @Mock
    private Patient patient;

    private String formDataUuid;
    @Mock
    private ProviderController providerController;
    @Mock
    private FormController formController;
    @Mock
    private LocationController locationController;


    @Before
    public void setUp() {
        initMocks(this);
        observationParserUtility = new ObservationParserUtility(muzimaApplication);
        formDataUuid = "formDataUuid";
        when(muzimaApplication.getConceptController()).thenReturn(conceptController);
        when(muzimaApplication.getProviderController()).thenReturn(providerController);
        when(muzimaApplication.getLocationController()).thenReturn(locationController);
        when(muzimaApplication.getFormController()).thenReturn(formController);
    }

    @Test
    public void shouldCreateEncounterEntityWithAppropriateValues() throws ProviderController.ProviderLoadException, FormController.FormFetchException {
        Date encounterDateTime = new Date();
        final String formUuid = "formUuid";
        String providerId = "providerId";
        int locationId = 1;
        String userSystemId = "userSystemId";

        final EncounterType encounterType = new EncounterType(){{
            setUuid("encounterTypeForObservationsCreatedOnPhone");
        }};
        Form form = new Form(){{
            setUuid(formUuid);
            setEncounterType(encounterType);
        }};

        List<Provider> providers = new ArrayList<Provider>() {{
            add(new Provider() {{
                setUuid("provider1");
            }});
        }};


        when(providerController.getAllProviders()).thenReturn(providers);
        when(formController.getFormByUuid(formUuid)).thenReturn(form);
        Encounter encounter = observationParserUtility.getEncounterEntity(encounterDateTime, formUuid,providerId, locationId, userSystemId, patient, formDataUuid);
        assertTrue(encounter.getUuid().startsWith("encounterUuid"));
        assertThat(encounter.getEncounterType().getUuid(), is(form.getEncounterType().getUuid()));
        assertThat(encounter.getProvider().getUuid(), is("providerForObservationsCreatedOnPhone"));
        assertThat(encounter.getEncounterDatetime(), is(encounterDateTime));
    }

    @Test
    public void shouldCreateNewConceptEntityAndAddItToListIfNotInDB() throws
            ConceptController.ConceptFetchException, ConceptController.ConceptParseException {
        observationParserUtility = new ObservationParserUtility(muzimaApplication);
        when(conceptController.getConceptByName("ConceptName")).thenReturn(null);
        Concept concept = observationParserUtility.getConceptEntity("1^ConceptName^mm",false);
        assertThat(concept.getName(), is("ConceptName"));
        assertThat(concept.getConceptType().getName(), is("ConceptTypeCreatedOnThePhone"));
        assertThat(concept.isCreatedOnDevice(), is(true));
        assertThat(observationParserUtility.getNewConceptList().size(), is(1));
    }

    @Test
    public void shouldNotCreateNewConceptOrObservationForInvalidConceptName() {
        observationParserUtility = new ObservationParserUtility(muzimaApplication);
        assertThat(observationParserUtility.getNewConceptList().size(), is(0));
    }

    @Test
    public void shouldNotCreateConceptIfAlreadyExistsInDB() throws ConceptController.ConceptFetchException,
            ConceptController.ConceptParseException{
        observationParserUtility = new ObservationParserUtility(muzimaApplication);
        Concept mockConcept = mock(Concept.class);
        when(conceptController.getConceptByName("ConceptName")).thenReturn(mockConcept);

        Concept concept = observationParserUtility.getConceptEntity("1^ConceptName^mm", false);

        assertThat(concept, is(mockConcept));
        assertThat(concept.isCreatedOnDevice(), is(false));
        assertThat(observationParserUtility.getNewConceptList().isEmpty(), is(true));
    }

    @Test
    public void shouldCreateOnlyOneConceptForRepeatedConceptNames() throws ConceptController.ConceptFetchException,
            ConceptController.ConceptParseException{
        observationParserUtility = new ObservationParserUtility(muzimaApplication);
        when(conceptController.getConceptByName("ConceptName")).thenReturn(null);

        Concept concept1 = observationParserUtility.getConceptEntity("1^ConceptName^mm",false);
        Concept concept2 = observationParserUtility.getConceptEntity("1^ConceptName^mm",false);

        assertThat(concept1, is(concept2));
        assertThat(concept1.isCreatedOnDevice(), is(true));
        assertThat(concept2.isCreatedOnDevice(), is(true));
        assertThat(observationParserUtility.getNewConceptList().size(), is(1));
    }

    @Test
    public void shouldCreateNumericObservation() throws
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
    public void shouldCreateValueCodedObsAndShouldAddItToNewConceptList() throws
            ConceptController.ConceptFetchException, ConceptController.ConceptParseException,
            ObservationController.ParseObservationException{
        observationParserUtility = new ObservationParserUtility(muzimaApplication);
        Concept concept = mock(Concept.class);
        when(concept.isNumeric()).thenReturn(false);
        when(concept.isCoded()).thenReturn(true);
        Observation observation = observationParserUtility.getObservationEntity(concept, "1^obs_value^mm");

        assertThat(observation.getValueCoded(), is(notNullValue()));
        assertThat(observationParserUtility.getNewConceptList().size(), is(1));
    }

    @Test
    public void shouldCreateObsWithStringForNonNumericNonCodedConcept() throws
            ConceptController.ConceptFetchException, ConceptController.ConceptParseException,
            ObservationController.ParseObservationException{
        observationParserUtility = new ObservationParserUtility(muzimaApplication);
        Concept concept = mock(Concept.class);
        when(concept.getName()).thenReturn("SomeConcept");
        when(concept.isNumeric()).thenReturn(false);
        when(concept.isCoded()).thenReturn(false);
        Observation observation = observationParserUtility.getObservationEntity(concept, "someString");
        assertThat(observation.getValueAsString(), is("someString"));
    }
}
