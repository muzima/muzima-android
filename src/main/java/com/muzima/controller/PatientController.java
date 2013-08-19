package com.muzima.controller;

import com.muzima.api.model.CohortMember;
import com.muzima.api.model.Patient;
import com.muzima.api.service.CohortService;
import com.muzima.api.service.PatientService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PatientController {

    private PatientService patientService;
    private CohortService cohortService;

    public PatientController(PatientService patientService, CohortService cohortService) {
        this.patientService = patientService;
        this.cohortService = cohortService;
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

    public List<Patient> getPatients(String cohortId) throws LoadPatientException {
        try {
            List<CohortMember> cohortMembers = cohortService.getCohortMembers(cohortId);
            ArrayList<Patient> patients = new ArrayList<Patient>();
            for (CohortMember member : cohortMembers) {
                patients.add(patientService.getPatientByUuid(member.getPatientUuid()));
            }
            return patients;
        } catch (IOException e) {
            throw new LoadPatientException(e);
        }
    }

    public static class PatientReplaceException extends Throwable {
        public PatientReplaceException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class LoadPatientException extends Throwable {
        public LoadPatientException(IOException e) {
            super(e);
        }
    }
}
