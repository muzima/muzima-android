package com.muzima.service;

import com.muzima.api.model.Concept;
import com.muzima.controller.ConceptController;
import com.muzima.testSupport.CustomTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

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

        observationParserUtility.createObservation("id^ConceptName^mm", "observation", conceptController);
        verify(conceptController).saveConcepts(anyList());
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
