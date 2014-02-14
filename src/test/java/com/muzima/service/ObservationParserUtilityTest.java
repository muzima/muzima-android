package com.muzima.service;

import com.muzima.api.model.Concept;
import com.muzima.api.model.Observation;
import com.muzima.controller.ConceptController;
import com.muzima.testSupport.CustomTestRunner;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(CustomTestRunner.class)
public class ObservationParserUtilityTest {
    private ObservationParserUtility observationParserUtility;

    @Mock
    private ConceptController conceptController;

    @Before
    public void setUp() {
        initMocks(this);
        observationParserUtility = new ObservationParserUtility(conceptController);

    }

    @Test
    @Ignore
    public void shouldCreateAndSaveUnknownConceptForObservation() throws ConceptController.ConceptFetchException, ConceptController.ConceptSaveException {
        ConceptController conceptController = mock(ConceptController.class);
        when(conceptController.getConceptByName("ConceptName")).thenReturn(null);

        Observation observation = observationParserUtility.getObservationEntity(null, null);
        verify(conceptController).saveConcepts(anyList());
        assertThat(observation.getConcept().getUuid(), notNullValue());
    }

    @Test
    @Ignore
    public void shouldCreateAndNotSaveUnknownConceptForCodedObservation() throws ConceptController.ConceptFetchException, ConceptController.ConceptSaveException {
        ConceptController conceptController = mock(ConceptController.class);
        Concept aConcept = mock(Concept.class);
        when(conceptController.getConceptByName("ConceptName")).thenReturn(aConcept);
        when(aConcept.isCoded()).thenReturn(true);

//        observationParserUtility.getObservationEntity("id^ConceptName^mm", "id^observation^kk", conceptController);
        verify(conceptController, times(0)).saveConcepts(anyList());
    }
}
