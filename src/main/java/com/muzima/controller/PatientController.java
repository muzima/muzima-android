/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.controller;

import android.util.Log;
import com.muzima.api.model.CohortMember;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PatientIdentifier;
import com.muzima.api.service.CohortService;
import com.muzima.api.service.PatientService;
import com.muzima.utils.StringUtils;
import org.apache.lucene.queryParser.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.muzima.utils.Constants.LOCAL_PATIENT;

public class PatientController {

    public static final String TAG = "PatientController";
    private PatientService patientService;
    private CohortService cohortService;

    public PatientController(PatientService patientService, CohortService cohortService) {
        this.patientService = patientService;
        this.cohortService = cohortService;
    }

    public void replacePatients(List<Patient> patients) throws PatientSaveException {
        try {
            patientService.updatePatients(patients);
        } catch (IOException e) {
            throw new PatientSaveException(e);
        }
    }

    public List<Patient> getPatients(String cohortId) throws PatientLoadException {
        try {
            List<CohortMember> cohortMembers = cohortService.getCohortMembers(cohortId);
            return patientService.getPatientsFromCohortMembers(cohortMembers);
        } catch (IOException e) {
            throw new PatientLoadException(e);
        }
    }

    public List<Patient> getAllPatients() throws PatientLoadException {
        try {
            return patientService.getAllPatients();
        } catch (IOException e) {
            throw new PatientLoadException(e);
        }
    }

    public int getTotalPatientsCount() throws PatientLoadException {
        try {
            return patientService.countAllPatients();
        } catch (IOException e) {
            throw new PatientLoadException(e);
        }
    }

    public Patient getPatientByUuid(String uuid) throws PatientLoadException {
        try {
            return patientService.getPatientByUuid(uuid);
        } catch (IOException e) {
            throw new PatientLoadException(e);
        }
    }

    public List<Patient> searchPatientLocally(String term, String cohortUuid) throws PatientLoadException {
        try {
            return StringUtils.isEmpty(cohortUuid)
                    ? patientService.searchPatients(term)
                    : patientService.searchPatients(term, cohortUuid);
        } catch (IOException e) {
            throw new PatientLoadException(e);
        } catch (ParseException e) {
            throw new PatientLoadException(e);
        }
    }


    public List<Patient> getPatientsForCohorts(String[] cohortUuids) throws PatientLoadException {
        List<Patient> allPatients = new ArrayList<Patient>();
        for (String cohortUuid : cohortUuids) {
            try {
                List<Patient> patients = getPatients(cohortUuid);
                allPatients.addAll(patients);
            } catch (PatientLoadException e) {
                throw new PatientLoadException(e);
            }
        }
        return allPatients;
    }

    public List<Patient> searchPatientOnServer(String name) {
        try {
            return patientService.downloadPatientsByName(name);
        } catch (IOException e) {
            Log.e(TAG, "Error while searching for patients in the server", e);
        }
        return new ArrayList<Patient>();
    }

    public List<Patient> getAllPatientsCreatedLocallyAndNotSynced() {
        //TODO: Try to replace this google guava
        try {
            List<Patient> localPatients = new ArrayList<Patient>();
            List<Patient> allPatients = getAllPatients();
            for (Patient patient : allPatients) {
                PatientIdentifier localPatientIdentifier = patient.getIdentifier(LOCAL_PATIENT);
                if (localPatientIdentifier != null && localPatientIdentifier.getIdentifier().equals(patient.getUuid())) {
                    localPatients.add(patient);
                }
            }
            return localPatients;
        } catch (PatientLoadException e) {
            Log.e(TAG, "Error while loading local patients", e);
        }
        return new ArrayList<Patient>();
    }

    public Patient consolidateTemporaryPatient(Patient patient) {
        try {
            return patientService.consolidateTemporaryPatient(patient);
        } catch (IOException e) {
            Log.e(TAG, "Error while consolidating the temporary patient.", e);
        }
        return null;
    }

    public void savePatient(Patient patient) throws PatientSaveException {
        try {
            patientService.savePatient(patient);
        } catch (IOException e) {
            Log.e(TAG, "Error while saving the patient : " + patient.getUuid(), e);
            throw new PatientSaveException(e);
        }
    }

    public void updatePatient(Patient patient) throws PatientSaveException {
        try {
            patientService.updatePatient(patient);
        } catch (IOException e) {
            Log.e(TAG, "Error while updating the patient : " + patient.getUuid(), e);
            throw new PatientSaveException(e);
        }
    }

    public void savePatients(List<Patient> patients) throws PatientSaveException {
        try {
            patientService.savePatients(patients);
        } catch (IOException e) {
            Log.e(TAG, "Error while saving the patient list", e);
            throw new PatientSaveException(e);
        }
    }

    public void deletePatient(Patient localPatient) {
        try {
            patientService.deletePatient(localPatient);
        } catch (IOException e) {
            Log.e(TAG, "Error while deleting local patient : " + localPatient.getUuid(), e);
        }
    }

    public void deletePatient(List<Patient> localPatients) throws PatientDeleteException {
        try {
            patientService.deletePatients(localPatients);
        } catch (IOException e) {
            Log.e(TAG, "Error while deleting local patients ", e);
            throw new PatientDeleteException(e);
        }
    }

    public List<Patient> getPatientsNotInCohorts() {
        try {
            return patientService.getPatientsNotInCohorts();
        } catch (IOException e) {
            Log.e(TAG, "Error while getting patients that are not in Cohorts", e);
        }
        return new ArrayList<Patient>();
    }

    public Patient downloadPatientByUUID(String uuid) throws PatientDownloadException {
        try {
            return patientService.downloadPatientByUuid(uuid);
        } catch (IOException e) {
            Log.e(TAG, "Error while downloading patient with UUID : " + uuid + " from server", e);
            throw new PatientDownloadException(e);
        }
    }

    public void deleteAllPatients() throws PatientDeleteException,IOException {
        List<Patient> allPatients = patientService.getAllPatients();
        patientService.deletePatients(allPatients);
    }


    public static class PatientSaveException extends Throwable {
        public PatientSaveException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class PatientDownloadException extends Throwable {
        public PatientDownloadException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class PatientLoadException extends Throwable {
        public PatientLoadException(Throwable e) {
            super(e);
        }
    }

    public static class PatientDeleteException extends Throwable {
        public PatientDeleteException(Throwable e) {
            super(e);
        }
    }
}
