package com.muzima.adapters.observations;

import com.muzima.controller.ObservationController;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ConceptsByPatientTest {
    @Test
    public void shouldGetObservationsByPatientUUID() throws Exception, ObservationController.LoadObservationException {
        ObservationController controller = mock(ObservationController.class);
        ConceptsByPatient byPatient = new ConceptsByPatient(controller, "uuid");
        byPatient.get();
        verify(controller).getConceptWithObservations("uuid");
    }
}
