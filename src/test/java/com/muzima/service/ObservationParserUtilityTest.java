package com.muzima.service;

import com.muzima.api.model.Concept;
import com.muzima.api.model.Observation;
import com.muzima.controller.ConceptController;
import com.muzima.testSupport.CustomTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.*;

@RunWith(CustomTestRunner.class)
public class ObservationParserUtilityTest {
    private ObservationParserUtility observationParserUtility;
    @Test
    public void shouldCreateAndSaveUnknownConceptForObservation() throws ConceptController.ConceptFetchException, ConceptController.ConceptSaveException {
        observationParserUtility = ObservationParserUtility.getInstance();
        ConceptController conceptController = mock(ConceptController.class);
        when(conceptController.getConceptByName("ConceptName")).thenReturn(null);

        Observation observation = observationParserUtility.createObservation("id^ConceptName^mm", "observation", conceptController);
        verify(conceptController).saveConcepts(anyList());
        assertThat(observation.getConcept().getUuid(), notNullValue());
    }

    @Test
    public void shouldCreateAndNotSaveUnknownConceptForCodedObservation() throws ConceptController.ConceptFetchException, ConceptController.ConceptSaveException {
        observationParserUtility = ObservationParserUtility.getInstance();
        ConceptController conceptController = mock(ConceptController.class);
        Concept aConcept = mock(Concept.class);
        when(conceptController.getConceptByName("ConceptName")).thenReturn(aConcept);
        when(aConcept.isCoded()).thenReturn(true);

        observationParserUtility.createObservation("id^ConceptName^mm", "id^observation^kk", conceptController);
        verify(conceptController, times(0)).saveConcepts(anyList());
    }
}
