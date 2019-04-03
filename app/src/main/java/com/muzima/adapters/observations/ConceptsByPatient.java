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

import com.muzima.api.model.Concept;
import com.muzima.controller.ConceptController;
import com.muzima.controller.ObservationController;
import com.muzima.model.observation.Concepts;

import java.util.List;

class ConceptsByPatient extends ConceptAction {
    private final String patientUuid;
    private final ObservationController controller;
    private final ConceptController conceptController;

    public ConceptsByPatient(ConceptController conceptController, ObservationController controller, String patientUuid) {
        this.controller = controller;
        this.patientUuid = patientUuid;
        this.conceptController = conceptController;
    }

    @Override
    Concepts get() throws ObservationController.LoadObservationException {
        return controller.getConceptWithObservations(patientUuid);
    }

    @Override
    Concepts get(Concept concept) throws ObservationController.LoadObservationException {
        return controller.getConceptWithObservations(patientUuid,concept.getUuid());
    }

    @Override
    List<Concept> getConcepts() throws ObservationController.LoadObservationException{
        try {
            return conceptController.getConcepts();
        }catch (ConceptController.ConceptFetchException e){
            throw new ObservationController.LoadObservationException(e);
        }
    }


    @Override
    public String toString() {
        return "ConceptsByPatient{" +
                "patientUuid='" + patientUuid + '\'' +
                ", controller=" + controller +
                '}';
    }
}
