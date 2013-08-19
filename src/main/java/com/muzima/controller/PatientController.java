package com.muzima.controller;

import com.muzima.api.model.Patient;
import com.muzima.api.service.PatientService;

import java.io.IOException;
import java.util.List;

public class PatientController {

    private PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    public void replacePatients(List<Patient> patients) throws PatientReplaceException {
        try {
            for (Patient patient : patients) {
                Patient existingPatient = patientService.getPatientByUuid(patient.getUuid());
                if(existingPatient != null){
                    patientService.deletePatient(patient);
                }
                patientService.savePatient(patient);
            }
        } catch (IOException e) {
            throw new PatientReplaceException(e);
        }
    }

    public static class PatientReplaceException extends Throwable {
        public PatientReplaceException(Throwable throwable) {
            super(throwable);
        }
    }
}
