/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.adapters.observations;

import com.muzima.controller.ObservationController;
import com.muzima.model.observation.Concepts;

public class ConceptsBySearch extends ConceptAction {
    private String patientUuid;
    private String term;
    private ObservationController controller;

    public ConceptsBySearch(ObservationController controller, String patientUuid, String term) {
        this.controller = controller;
        this.patientUuid = patientUuid;
        this.term = term;
    }

    @Override
    Concepts get() throws ObservationController.LoadObservationException {
        return controller.searchObservationsGroupedByConcepts(term, patientUuid);
    }

    @Override
    public String toString() {
        return "ConceptsBySearch{" +
                "patientUuid='" + patientUuid + '\'' +
                ", term='" + term + '\'' +
                ", controller=" + controller +
                '}';
    }
}
