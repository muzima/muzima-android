package com.muzima.controller;

import com.muzima.api.model.Encounter;
import com.muzima.api.service.EncounterService;

import java.io.IOException;
import java.util.List;

public class EncounterController {

    private EncounterService encounterService;

    public EncounterController(EncounterService encounterService) {
        this.encounterService = encounterService;
    }

    public void replaceEncounters(List<String> patientUuids, List<Encounter> allEncounters) throws ReplaceEncounterException {
        try {
            for (String patientUuid : patientUuids) {
                List<Encounter> encountersByPatient = encounterService.getEncountersByPatientUuid(patientUuid);
                encounterService.deleteEncounters(encountersByPatient);
            }
            encounterService.saveEncounters(allEncounters);
        } catch (IOException e) {
            throw new ReplaceEncounterException(e);
        }
    }

    public List<Encounter> downloadEncountersByPatientUuids(List<String> patientUuids) throws DownloadEncounterException {
        try {
            return encounterService.downloadEncountersByPatientUuids(patientUuids);
        } catch (IOException e) {
            throw new DownloadEncounterException(e);
        }
    }

    public class DownloadEncounterException extends Throwable {
        public DownloadEncounterException(IOException e) {
            super(e);
        }


    }

    public class ReplaceEncounterException extends Throwable {
        public ReplaceEncounterException(IOException e) {
            super(e);
        }
    }
}
