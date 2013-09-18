package com.muzima.adapters.observations;

import com.muzima.controller.ObservationController;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ConceptsBySearchTest {
    @Test
    public void shouldSearchObservationsByUuidAndTerm() throws Exception, ObservationController.LoadObservationException {
        ObservationController controller = mock(ObservationController.class);
        ConceptsBySearch conceptsBySearch = new ConceptsBySearch(controller, "uuid", "term");
        conceptsBySearch.get();
        verify(controller).searchObservations("term", "uuid");
    }
}
