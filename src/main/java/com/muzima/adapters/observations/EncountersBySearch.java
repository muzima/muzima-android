package com.muzima.adapters.observations;

import com.muzima.controller.ObservationController;
import com.muzima.model.observation.Encounters;

public class EncountersBySearch extends EncounterAction {
    private String patientUuid;
    private String term;
    private ObservationController controller;

    public EncountersBySearch(ObservationController controller, String patientUuid, String term) {
        this.controller = controller;
        this.patientUuid = patientUuid;
        this.term = term;
    }

    @Override
    Encounters get() throws ObservationController.LoadObservationException {
        return controller.searchObservationsGroupedByEncounter(term, patientUuid);
    }

    @Override
    public String toString() {
        return "EncountersBySearch{" +
                "patientUuid='" + patientUuid + '\'' +
                ", term='" + term + '\'' +
                ", controller=" + controller +
                '}';
    }
}
