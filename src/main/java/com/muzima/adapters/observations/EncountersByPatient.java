package com.muzima.adapters.observations;

import com.muzima.controller.ObservationController;
import com.muzima.model.observation.Encounters;

public class EncountersByPatient extends EncounterAction {
    private String patientUuid;
    private ObservationController controller;

    public EncountersByPatient(ObservationController controller, String patientUuid) {
        this.controller = controller;
        this.patientUuid = patientUuid;
    }

    @Override
    Encounters get() throws ObservationController.LoadObservationException {
        return  controller.getEncountersWithObservations(patientUuid);
    }


    @Override
    public String toString() {
        return "EncountersByPatient{" +
                "patientUuid='" + patientUuid + '\'' +
                ", controller=" + controller +
                '}';
    }
}
