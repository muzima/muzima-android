/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.adapters.observations;

import com.muzima.controller.ConceptController;
import com.muzima.controller.ObservationController;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ConceptsByPatientTest {
    @Test
    public void shouldGetObservationsByPatientUUID() throws ObservationController.LoadObservationException {
        ObservationController controller = mock(ObservationController.class);
        ConceptController conceptController = mock(ConceptController.class);
        ConceptsByPatient byPatient = new ConceptsByPatient(conceptController,controller, "uuid");
        byPatient.get();
        verify(controller).getConceptWithObservations("uuid");
    }
}
