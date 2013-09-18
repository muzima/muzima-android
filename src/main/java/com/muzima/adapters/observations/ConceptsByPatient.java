package com.muzima.adapters.observations;

import com.muzima.controller.ObservationController;
import com.muzima.model.observation.Concepts;

public class ConceptsByPatient extends ConceptAction {
    private String patientUuid;
    private ObservationController controller;

    public ConceptsByPatient(ObservationController controller, String patientUuid) {
        this.controller = controller;
        this.patientUuid = patientUuid;
    }

    @Override
    Concepts get() throws ObservationController.LoadObservationException {
        return controller.getConceptWithObservations(patientUuid);
    }

    @Override
    public String toString() {
        return "ConceptsByPatient{" +
                "patientUuid='" + patientUuid + '\'' +
                ", controller=" + controller +
                '}';
    }
}
