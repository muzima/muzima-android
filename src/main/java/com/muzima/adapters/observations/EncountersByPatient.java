/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.adapters.observations;

import com.muzima.api.model.Encounter;
import com.muzima.controller.EncounterController;
import com.muzima.controller.ObservationController;
import com.muzima.model.observation.Encounters;

import java.util.List;

public class EncountersByPatient extends EncounterAction {
    private String patientUuid;
    private ObservationController controller;
    private EncounterController encounterController;

    public EncountersByPatient(EncounterController encounterController,ObservationController controller, String patientUuid) {
        this.controller = controller;
        this.patientUuid = patientUuid;
        this.encounterController = encounterController;
    }

    @Override
    Encounters get() throws ObservationController.LoadObservationException {
        return  controller.getEncountersWithObservations(patientUuid);
    }

    @Override
    Encounters get(Encounter encounter) throws ObservationController.LoadObservationException {
        return controller.getObservationsByPatientAndEncounter(patientUuid,encounter.getUuid());
    }

    @Override
    List<Encounter> getEncounters() throws ObservationController.LoadObservationException {
        try {
            return encounterController.getEncountersByPatientUuid(patientUuid);
        }catch (EncounterController.DownloadEncounterException e){
            throw new ObservationController.LoadObservationException(e);
        }
    }


    @Override
    public String toString() {
        return "EncountersByPatient{" +
                "patientUuid='" + patientUuid + '\'' +
                ", controller=" + controller +
                '}';
    }
}
