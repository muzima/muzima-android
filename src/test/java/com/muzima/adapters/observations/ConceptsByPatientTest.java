/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

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
