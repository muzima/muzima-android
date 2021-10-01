/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.controller;

import android.util.Log;

import com.muzima.api.model.CohortMember;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PatientIdentifier;
import com.muzima.api.model.PatientIdentifierType;
import com.muzima.api.model.PatientTag;
import com.muzima.api.model.PersonAttributeType;
import com.muzima.api.service.CohortService;
import com.muzima.api.service.FormService;
import com.muzima.api.service.PatientService;
import com.muzima.api.service.PatientTagService;
import com.muzima.utils.CustomColor;
import com.muzima.utils.StringUtils;
import org.apache.lucene.queryParser.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.muzima.util.Constants.PATIENT_DELETION_PENDING_STATUS;
import static com.muzima.utils.Constants.LOCAL_PATIENT;
import static com.muzima.utils.Constants.STATUS_COMPLETE;
import static com.muzima.utils.Constants.STATUS_INCOMPLETE;

public class PatientController {

    private final PatientService patientService;
    private final CohortService cohortService;
    private final Map<String, Integer> tagColors;
    private List<PatientTag> selectedTags;
    private FormService formService;
    private PatientTagService patientTagService;

    public PatientController(PatientService patientService, CohortService cohortService, FormService formService,PatientTagService patientTagService) {
        this.patientService = patientService;
        this.cohortService = cohortService;
        tagColors = new HashMap<>();
        selectedTags = new ArrayList<>();
        this.formService = formService;
        this.patientTagService= patientTagService;
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

    public List<Patient> getPatients(String cohortId,int page, int pageSize) throws PatientLoadException {
        try {
            return patientService.getPatients(cohortId, page,pageSize);
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

    public List<Patient> getPatients(int page, int pageSize) throws PatientLoadException {
        try {
            return patientService.getPatients(page,pageSize);
        } catch (IOException e) {
            throw new PatientLoadException(e);
        }
    }

    public int countAllPatients() throws PatientLoadException {
        try {
            return patientService.countAllPatients();
        } catch (IOException e) {
            throw new PatientLoadException(e);
        }
    }

    public int countPatients(String cohortId) throws PatientLoadException {
        try {
            return patientService.countPatients(cohortId);
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
        } catch (IOException | ParseException e) {
            throw new PatientLoadException(e);
        }
    }

    public List<Patient> searchPatientLocally(String term,  int page, int pageSize)
            throws PatientLoadException {
        try {
            return patientService.searchPatients(term,page,pageSize);
        } catch (IOException | ParseException e) {
            throw new PatientLoadException(e);
        }
    }


    public List<Patient> getPatientsForCohorts(String[] cohortUuids) throws PatientLoadException {
        List<Patient> allPatients = new ArrayList<>();
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
            Log.e(getClass().getSimpleName(), "Error while searching for patients in the server", e);
        }
        return new ArrayList<>();
    }

    public List<Patient> getAllPatientsCreatedLocallyAndNotSynced() {
        //TODO: Try to replace this google guava
        try {
            List<Patient> localPatients = new ArrayList<>();
            List<Patient> allPatients = getAllPatients();
            for (Patient patient : allPatients) {
                PatientIdentifier localPatientIdentifier = patient.getIdentifier(LOCAL_PATIENT);
                if (localPatientIdentifier != null && localPatientIdentifier.getIdentifier().equals(patient.getUuid())) {
                    localPatients.add(patient);
                }
            }
            return localPatients;
        } catch (PatientLoadException e) {
            Log.e(getClass().getSimpleName(), "Error while loading local patients", e);
        }
        return new ArrayList<>();
    }

    public Patient consolidateTemporaryPatient(Patient patient) {
        try {
            return patientService.consolidateTemporaryPatient(patient);
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error while consolidating the temporary patient.", e);
        }
        return null;
    }

    public void savePatient(Patient patient) throws PatientSaveException {
        try {
            patientService.savePatient(patient);
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error while saving the patient : " + patient.getUuid(), e);
            throw new PatientSaveException(e);
        }
    }

    public void updatePatient(Patient patient) throws PatientSaveException {
        try {
            patientService.updatePatient(patient);
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error while updating the patient : " + patient.getUuid(), e);
            throw new PatientSaveException(e);
        }
    }

    public  void savePatientTags(PatientTag tag) throws IOException {
        patientTagService.savePatientTag(tag);
    }

    public void savePatients(List<Patient> patients) throws PatientSaveException {
        try {
            patientService.savePatients(patients);
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error while saving the patient list", e);
            throw new PatientSaveException(e);
        }
    }

    public void deletePatient(Patient localPatient) {
        try {
            deleteOrMarkAsPendingDeletion(localPatient);
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error while deleting local patient : " + localPatient.getUuid(), e);
        }
    }

    public void deletePatient(List<Patient> localPatients) throws PatientDeleteException {
        try {
            for(Patient patient:localPatients){
                deleteOrMarkAsPendingDeletion(patient);
            }
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error while deleting local patients ", e);
            throw new PatientDeleteException(e);
        }
    }

    public List<Patient> getPatientsNotInCohorts() {
        try {
            return patientService.getPatientsNotInCohorts();
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error while getting patients that are not in Cohorts", e);
        }
        return new ArrayList<>();
    }

    public Patient downloadPatientByUUID(String uuid) throws PatientDownloadException {
        try {
            return patientService.downloadPatientByUuid(uuid);
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "Error while downloading patient with UUID : " + uuid + " from server", e);
            throw new PatientDownloadException(e);
        }
    }

    public void deleteAllPatients() throws IOException {
        List<Patient> allPatients = patientService.getAllPatients();
        patientService.deletePatients(allPatients);
    }

    public PatientIdentifierType getPatientIdentifierTypeByUuid(String uuid){
        try {
            return patientService.getPatientIdentifierTypeByUuid(uuid);
        } catch (IOException e){
            Log.e(getClass().getSimpleName(), "Error retrieving patient identifier type by uuid : " + uuid, e);
        }
        return null;
    }

    public List<PatientIdentifierType> getPatientIdentifierTypeByName(String name){
        try {
            return patientService.getPatientIdentifierTypeByName(name);
        } catch (IOException e){
            Log.e(getClass().getSimpleName(), "Error retrieving patient identifier type by name : " + name, e);
        }
        return null;
    }

    public PersonAttributeType getPersonAttributeTypeByUuid(String uuid){
        try {
            return patientService.getPersonAttributeTypeByUuid(uuid);
        } catch (IOException e){
            Log.e(getClass().getSimpleName(), "Error retrieving person attribute type by uuid : " + uuid, e);
        }
        return null;
    }

    public List<PersonAttributeType> getPersonAttributeTypeByName(String name){
        try {
            return patientService.getPersonAttributeTypeByName(name);
        } catch (IOException e){
            Log.e(getClass().getSimpleName(), "Error retrieving person attribute type by name : " + name, e);
        }
        return null;
    }


    public static class PatientSaveException extends Throwable {
        public PatientSaveException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class PatientDownloadException extends Throwable {
        PatientDownloadException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class PatientLoadException extends Throwable {
        public PatientLoadException(Throwable e) {
            super(e);
        }
        public PatientLoadException(String message) {
            super(message);
        }
    }

    public static class PatientDeleteException extends Throwable {
        PatientDeleteException(Throwable e) {
            super(e);
        }
    }

    public int getTagColor(String uuid) {
        if (!tagColors.containsKey(uuid)) {
            tagColors.put(uuid, CustomColor.getRandomColor());
        }
        return tagColors.get(uuid);
    }

    public List<PatientTag> getSelectedTags() {
        return selectedTags;
    }

    public void setSelectedTags(List<PatientTag> selectedTags) {
        this.selectedTags = selectedTags;
    }

    public List<PatientTag> getAllTags() throws PatientLoadException {
        List<PatientTag> allTags = new ArrayList<>();
        try {
            allTags = patientTagService.getAllPatientTags();
        } catch (IOException e) {
            throw new PatientLoadException(e);
        }
        return allTags;
    }

    public List<String> getSelectedTagUuids() {
        List<PatientTag> selectedTags = getSelectedTags();
        List<String> tags = new ArrayList<String>();
        for (PatientTag selectedTag : selectedTags) {
            tags.add(selectedTag.getUuid());
        }
        return tags;
    }

    public List<Patient> filterPatientByTags(List<Patient> patients, List<String> tagsUuid) {
        if (tagsUuid == null || tagsUuid.isEmpty()) {
            return patients;
        }
        List<Patient> filteredPatients = new ArrayList<>();
        for (Patient patient : patients) {
            PatientTag[] patientTags = patient.getTags();
            for (PatientTag patientTag : patientTags) {
                if (tagsUuid.contains(patientTag.getUuid())) {
                    filteredPatients.add(patient);
                    break;
                }
            }
        }
        return filteredPatients;
    }

    public void deletePatientByCohortMembership(List<CohortMember> cohortMembers){
        for(CohortMember cohortMember:cohortMembers){
            Patient patient = cohortMember.getPatient();
            try {
                deleteOrMarkAsPendingDeletion(patient);
            } catch (IOException e) {
                Log.e(getClass().getSimpleName(), "An IOException was encountered : ", e);
            }
        }
    }

    public void deleteOrMarkAsPendingDeletion(Patient patient) throws IOException {
        int formCount = getFormDataCount(patient.getUuid());
        if(formCount == 0 ){
            patientService.deletePatient(patient);
        }else{
            Patient pat = patientService.getPatientByUuid(patient.getUuid());
            pat.setDeletionStatus(PATIENT_DELETION_PENDING_STATUS);
            patientService.updatePatient(pat);
        }
    }

    public void deletePatientsPendingDeletion(){
        try {
            List<Patient> patients = patientService.getPatientsPendingDeletion();
            for(Patient patient:patients){
                int formCount = getFormDataCount(patient.getUuid());
                if(formCount == 0){
                    patientService.deletePatient(patient);
                }
            }
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "An IOException was encountered : ", e);
        } catch (ParseException e) {
            Log.e(getClass().getSimpleName(), "A ParseException was encountered : ", e);
        }
    }

    public int getFormDataCount(String patientUuid) throws IOException {
        int incompleteFormCount = formService.countFormDataByPatient(patientUuid,STATUS_INCOMPLETE);
        int completeFormCount = formService.countFormDataByPatient(patientUuid,STATUS_COMPLETE);
        int formCount = incompleteFormCount + completeFormCount;
        return formCount;
    }
}
