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
