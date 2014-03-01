package com.muzima.service;

import android.util.Log;
import com.muzima.MuzimaApplication;
import com.muzima.api.context.Context;
import com.muzima.api.exception.AuthenticationException;
import com.muzima.api.model.*;
import com.muzima.controller.*;
import com.muzima.utils.Constants;
import org.apache.lucene.queryParser.ParseException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.*;
import static com.muzima.utils.Constants.LOCAL_PATIENT;
import static java.util.Collections.singleton;

public class MuzimaSyncService {
    private static final String TAG = "MuzimaSyncService";

    private MuzimaApplication muzimaApplication;
    private FormController formController;
    private ConceptController conceptController;
    private CohortController cohortController;
    private PatientController patientController;
    private ObservationController observationController;
    private final CohortPrefixPreferenceService cohortPrefixPreferenceService;
    private EncounterController encounterController;

    public MuzimaSyncService(MuzimaApplication muzimaContext) {
        this.muzimaApplication = muzimaContext;
        cohortPrefixPreferenceService = muzimaApplication.getCohortPrefixesPreferenceService();
        formController = muzimaApplication.getFormController();
        conceptController = muzimaApplication.getConceptController();
        cohortController = muzimaApplication.getCohortController();
        patientController = muzimaApplication.getPatientController();
        observationController = muzimaApplication.getObservationController();
        encounterController = muzimaApplication.getEncounterController();
    }

    public int authenticate(String[] credentials) {
        String username = credentials[0];
        String password = credentials[1];
        String server = credentials[2];

        Context muzimaContext = muzimaApplication.getMuzimaContext();
        try {
            muzimaContext.openSession();
            if (!muzimaContext.isAuthenticated()) {
                muzimaContext.authenticate(username, password, server);
            }
        } catch (ConnectException e) {
            Log.e(TAG, "ConnectException Exception thrown while authentication.", e);
            return CONNECTION_ERROR;
        } catch (ParseException e) {
            Log.e(TAG, "ParseException Exception thrown while authentication.", e);
            return PARSING_ERROR;
        } catch (MalformedURLException e) {
            Log.e(TAG, "IOException Exception thrown while authentication.", e);
            return MALFORMED_URL_ERROR;
        } catch (IOException e) {
            Log.e(TAG, "IOException Exception thrown while authentication.", e);
            return AUTHENTICATION_ERROR;
        } catch (AuthenticationException e) {
            Log.e(TAG, "Exception thrown while authentication.", e);
            return INVALID_CREDENTIALS_ERROR;
        } finally {
            if (muzimaContext != null)
                muzimaContext.closeSession();
        }
        return AUTHENTICATION_SUCCESS;
    }

    public int[] downloadForms() {
        int[] result = new int[2];

        try {
            List<Form> forms;
            forms = formController.downloadAllForms();
            Log.i(TAG, "Form download successful");
            formController.saveAllForms(forms);
            Log.i(TAG, "New forms are saved");

            result[0] = Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS;
            result[1] = forms.size();

        } catch (FormController.FormFetchException e) {
            Log.e(TAG, "Exception when trying to download forms", e);
            result[0] = DOWNLOAD_ERROR;
            return result;
        } catch (FormController.FormSaveException e) {
            Log.e(TAG, "Exception when trying to save forms", e);
            result[0] = SAVE_ERROR;
            return result;
        }
        return result;
    }

    public int[] downloadFormTemplates(String[] formIds) {
        int[] result = new int[3];

        try {
            List<FormTemplate> formTemplates = formController.downloadFormTemplates(formIds);
            Log.i(TAG, formTemplates.size() + " form template download successful");

            formController.replaceFormTemplates(formTemplates);
            List<Concept> concepts = getRelatedConcepts(formTemplates);
            conceptController.saveConcepts(concepts);

            Log.i(TAG, "Form templates replaced");

            result[0] = SUCCESS;
            result[1] = formTemplates.size();
            result[2] = concepts.size();
        } catch (FormController.FormSaveException e) {
            Log.e(TAG, "Exception when trying to save forms", e);
            result[0] = SAVE_ERROR;
            return result;
        } catch (FormController.FormFetchException e) {
            Log.e(TAG, "Exception when trying to download forms", e);
            result[0] = DOWNLOAD_ERROR;
            return result;
        } catch (ConceptController.ConceptDownloadException e) {
            Log.e(TAG, "Exception while parsing Concepts", e);
            result[0] = DOWNLOAD_ERROR;
            return result;
        } catch (ConceptController.ConceptSaveException e) {
            Log.e(TAG, "Exception when trying to download forms", e);
            result[0] = DOWNLOAD_ERROR;
            return result;
        }
        return result;
    }

    public int[] downloadCohorts() {
        int[] result = new int[3];
        try {
            List<Cohort> cohorts = downloadCohortsList();
            ArrayList<Cohort> voidedCohorts = deleteVoidedCohorts(cohorts);
            cohorts.removeAll(voidedCohorts);

            cohortController.saveAllCohorts(cohorts);
            Log.i(TAG, "New cohorts are saved");
            result[0] = SUCCESS;
            result[1] = cohorts.size();
            result[2] = voidedCohorts.size();
        } catch (CohortController.CohortDownloadException e) {
            Log.e(TAG, "Exception when trying to download cohorts", e);
            result[0] = DOWNLOAD_ERROR;
            return result;
        } catch (CohortController.CohortSaveException e) {
            Log.e(TAG, "Exception when trying to save cohorts", e);
            result[0] = SAVE_ERROR;
            return result;
        } catch (CohortController.CohortDeleteException e) {
            Log.e(TAG, "Exception occurred while deleting voided cohorts", e);
            result[0] = DELETE_ERROR;
            return result;
        }
        return result;
    }

    private ArrayList<Cohort> deleteVoidedCohorts(List<Cohort> cohorts) throws CohortController.CohortDeleteException {
        Log.i(TAG, "Voided cohorts are deleted");
        ArrayList<Cohort> voidedCohorts = new ArrayList<Cohort>();
        for( Cohort cohort : cohorts){
            if(cohort.isVoided()){
                voidedCohorts.add(cohort);
            }
        }
        cohortController.deleteCohorts(voidedCohorts);
        return voidedCohorts;
    }

    private List<Concept> getRelatedConcepts(List<FormTemplate> formTemplates) throws ConceptController.ConceptDownloadException {
        HashSet<Concept> concepts = new HashSet<Concept>();
        ConceptParser xmlParserUtils = new ConceptParser();
        HTMLConceptParser htmlParserUtils = new HTMLConceptParser();
        for (FormTemplate formTemplate : formTemplates) {
            List<String> names = new ArrayList<String>();
            if (formTemplate.isHTMLForm()) {
                names = htmlParserUtils.parse(formTemplate.getHtml());
            } else {
                names = xmlParserUtils.parse(formTemplate.getModel());
            }
            concepts.addAll(conceptController.downloadConceptsByNames(names));
        }
        return new ArrayList<Concept>(concepts);
    }

    public int[] downloadPatientsForCohorts(String[] cohortUuids) {
        int[] result = new int[4];

        int patientCount = 0;
        try {
            long startDownloadCohortData = System.currentTimeMillis();

            List<CohortData> cohortDataList = cohortController.downloadCohortData(cohortUuids);

            long endDownloadCohortData = System.currentTimeMillis();
            Log.i(TAG, "Cohort data download successful with " + cohortDataList.size() + " cohorts");
            ArrayList<Patient> voidedPatients = new ArrayList<Patient>();
            List<Patient> cohortPatients;
            for (CohortData cohortData : cohortDataList) {
                cohortController.addCohortMembers(cohortData.getCohortMembers());
                cohortPatients = cohortData.getPatients();
                getVoidedPatients(voidedPatients, cohortPatients);
                cohortPatients.removeAll(voidedPatients);
                patientController.replacePatients(cohortPatients);
                patientCount += cohortData.getPatients().size();
            }
            patientController.deletePatient(voidedPatients);
            long cohortMemberAndPatientReplaceTime = System.currentTimeMillis();

            Log.i(TAG, "Cohort data replaced");
            Log.i(TAG, "Patients downloaded " + patientCount);
            Log.d(TAG, "Time Taken:\n " +
                    "In Downloading cohort data: " + (endDownloadCohortData - startDownloadCohortData) / 1000 + " sec\n" +
                    "In Replacing cohort members and patients: " + (cohortMemberAndPatientReplaceTime - endDownloadCohortData) / 1000 + " sec");

            result[0] = SUCCESS;
            result[1] = patientCount;
            result[2] = cohortDataList.size();
            result[3] = voidedPatients.size();
        } catch (CohortController.CohortDownloadException e) {
            Log.e(TAG, "Exception thrown while downloading cohort data.", e);
            result[0] = DOWNLOAD_ERROR;
        } catch (CohortController.CohortReplaceException e) {
            Log.e(TAG, "Exception thrown while replacing cohort data.", e);
            result[0] = REPLACE_ERROR;
        } catch (PatientController.PatientSaveException e) {
            Log.e(TAG, "Exception thrown while replacing patients.", e);
            result[0] = REPLACE_ERROR;
        } catch (PatientController.PatientDeleteException e) {
            Log.e(TAG, "Exception thrown while deleting patients.", e);
            result[0] = DELETE_ERROR;
        }
        return result;
    }

    private void getVoidedPatients(ArrayList<Patient> voidedPatients, List<Patient> cohortPatients) {
        for(Patient patient : cohortPatients){
            if(patient.isVoided()){
                voidedPatients.add(patient);
            }
        }
    }

    public int[] downloadPatients(String[] patientUUIDs) {
        int[] result = new int[2];
        List<Patient> downloadedPatients;
        try {
            downloadedPatients = downloadPatientsByUUID(patientUUIDs);
            patientController.savePatients(downloadedPatients);
            result[0] = SUCCESS;
            result[1] = downloadedPatients.size();
        } catch (PatientController.PatientDownloadException e) {
            Log.e(TAG, "Error while downloading patients.", e);
            result[0] = DOWNLOAD_ERROR;
        } catch (PatientController.PatientSaveException e) {
            Log.e(TAG, "Error while saving patients.", e);
            result[0] = DOWNLOAD_ERROR;
        }
        return result;
    }

    public int[] downloadObservationsForPatientsByCohortUUIDs(String[] cohortUuids) {
        int[] result = new int[2];
        List<Patient> patients;
        try {
            patients = patientController.getPatientsForCohorts(cohortUuids);
            result = downloadObservationsForPatientsByPatientUUIDs(getPatientUuids(patients));
        } catch (PatientController.PatientLoadException e) {
            Log.e(TAG, "Exception thrown while loading patients.", e);
            result[0] = LOAD_ERROR;
        }
        return result;
    }

    public int[] downloadObservationsForPatientsByPatientUUIDs(List<String> patientUuids) {
        int[] result = new int[3];
        try {
            long startDownloadObservations = System.currentTimeMillis();
            List<Observation> allObservations = observationController.downloadObservationsByPatientUuidsAndConceptUuids
                    (patientUuids, getConceptUuidsFromConcepts(conceptController.getConcepts()));
            long endDownloadObservations = System.currentTimeMillis();
            Log.i(TAG, "Observations download successful with " + allObservations.size() + " observations");
            List<Observation> voidedObservations = getVoidedObservations(allObservations);
            observationController.deleteObservations(voidedObservations);
            allObservations.removeAll(voidedObservations);
            Log.i(TAG, "Voided observations delete successful with " + voidedObservations.size() + " observations");
            observationController.replaceObservations(allObservations);
            long replacedObservations = System.currentTimeMillis();

            Log.d(TAG, "In Downloading observations : " + (endDownloadObservations - startDownloadObservations) / 1000 + " sec\n" +
                    "In Replacing observations for patients: " + (replacedObservations - endDownloadObservations) / 1000 + " sec");

            result[0] = SUCCESS;
            result[1] = allObservations.size();
            result[2] = voidedObservations.size();
        } catch (ObservationController.DownloadObservationException e) {
            Log.e(TAG, "Exception thrown while downloading observations.", e);
            result[0] = DOWNLOAD_ERROR;
        } catch (ObservationController.ReplaceObservationException e) {
            Log.e(TAG, "Exception thrown while replacing observations.", e);
            result[0] = REPLACE_ERROR;
        } catch (ConceptController.ConceptFetchException e) {
            Log.e(TAG, "Exception thrown while loading concepts.", e);
            result[0] = LOAD_ERROR;
        } catch (ObservationController.DeleteObservationException e) {
            Log.e(TAG, "Exception thrown while deleting observations.", e);
            result[0] = DELETE_ERROR;
        }

        return result;
    }

    private List<Observation> getVoidedObservations(List<Observation> allObservations) {
        List<Observation> voidedObservations = new ArrayList<Observation>();
        for(Observation observation : allObservations){
            if(observation.isVoided()){
                voidedObservations.add(observation);
            }
        }
        return voidedObservations;
    }

    public int[] downloadEncountersForPatientsByCohortUUIDs(String[] cohortUuids) {
        int[] result = new int[2];
        List<Patient> patients;
        try {
            patients = patientController.getPatientsForCohorts(cohortUuids);
            result = downloadEncountersForPatientsByPatientUUIDs(getPatientUuids(patients));
        } catch (PatientController.PatientLoadException e) {
            Log.e(TAG, "Exception thrown while loading patients.", e);
            result[0] = LOAD_ERROR;
        }
        return result;
    }

    public int[] downloadEncountersForPatientsByPatientUUIDs(List<String> patientUuids) {
        int[] result = new int[3];
        try {
            long startDownloadEncounters = System.currentTimeMillis();
            List<Encounter> allEncounters = encounterController.downloadEncountersByPatientUuids(patientUuids);
            long endDownloadObservations = System.currentTimeMillis();
            Log.i(TAG, "Encounters download successful with " + allEncounters.size() + " encounters");
            ArrayList<Encounter> voidedEncounters = getVoidedEncounters(allEncounters);
            allEncounters.removeAll(voidedEncounters);
            encounterController.deleteEncounters(voidedEncounters);
            Log.i(TAG, "Voided encounters delete successful with " + allEncounters.size() + " encounters");

            encounterController.replaceEncounters(allEncounters);
            long replacedEncounters = System.currentTimeMillis();

            Log.d(TAG, "In Downloading encounters : " + (endDownloadObservations - startDownloadEncounters) / 1000 + " sec\n" +
                    "In Replacing encounters for patients: " + (replacedEncounters - endDownloadObservations) / 1000 + " sec");

            result[0] = SUCCESS;
            result[1] = allEncounters.size();
            result[2] = voidedEncounters.size();
        } catch (EncounterController.DownloadEncounterException e) {
            Log.e(TAG, "Exception thrown while downloading encounters.", e);
            result[0] = DOWNLOAD_ERROR;
        } catch (EncounterController.ReplaceEncounterException e) {
            Log.e(TAG, "Exception thrown while replacing encounters.", e);
            result[0] = REPLACE_ERROR;
        } catch (EncounterController.DeleteEncounterException e) {
            Log.e(TAG, "Exception thrown while deleting encounters.", e);
            result[0] = DELETE_ERROR;
        }
        return result;
    }

    private ArrayList<Encounter> getVoidedEncounters(List<Encounter> allEncounters) {
        ArrayList<Encounter> voidedEncounters = new ArrayList<Encounter>();
        for(Encounter encounter : allEncounters){
            if(encounter.isVoided()){
                voidedEncounters.add(encounter);
            }
        }
        return voidedEncounters;
    }

    public int[] uploadAllCompletedForms() {
        int[] result = new int[1];
        try {
            result[0] = formController.uploadAllCompletedForms() ? SUCCESS : UPLOAD_ERROR;
        } catch (FormController.UploadFormDataException e) {
            Log.e(TAG, "Exception thrown while uploading forms.", e);
            result[0] = UPLOAD_ERROR;
        }
        return result;
    }

    public void consolidatePatients() {
        List<Patient> allLocalPatients = patientController.getAllPatientsCreatedLocallyAndNotSynced();
        for (Patient localPatient : allLocalPatients) {
            Patient patientFromServer = patientController.consolidateTemporaryPatient(localPatient);
            if (patientFromServer != null) {
                checkChangeInPatientId(localPatient, patientFromServer);
                patientFromServer.addIdentifier(localPatient.getIdentifier(LOCAL_PATIENT));
                patientController.deletePatient(localPatient);
                try {
                    patientController.savePatient(patientFromServer);
                } catch (PatientController.PatientSaveException e) {
                    Log.e(TAG, "Error while saving patients.", e);
                }
            }
        }
    }

    private void checkChangeInPatientId(Patient localPatient, Patient patientFromServer) {
        String patientIdentifier = patientFromServer.getIdentifier();
        String localPatientIdentifier = localPatient.getIdentifier();
        if (patientFromServer == null || localPatientIdentifier == null) {
            return;
        }
        if (!patientIdentifier.equals(localPatientIdentifier)) {
            JSONInputOutputToDisk jsonInputOutputToDisk = new JSONInputOutputToDisk(muzimaApplication);
            try {
                jsonInputOutputToDisk.add(patientIdentifier);
            } catch (IOException e) {
                Log.e(TAG, "Exception thrown when writing to phone disk.", e);
            }
        }
    }

    public List<Patient> updatePatientsNotPartOfCohorts() {
        List<Patient> patientsNotInCohorts = patientController.getPatientsNotInCohorts();
        List<Patient> downloadedPatients = new ArrayList<Patient>();
        try {
            for (Patient patient : patientsNotInCohorts) {
                downloadedPatients.add(patientController.downloadPatientByUUID(patient.getUuid()));
            }
            downloadedPatients.removeAll(singleton(null));
            patientController.replacePatients(downloadedPatients);
        } catch (PatientController.PatientSaveException e) {
            Log.e(TAG, "Exception thrown while updating patients from server.", e);
        } catch (PatientController.PatientDownloadException e) {
            Log.e(TAG, "Exception thrown while downloading patients from server.", e);
        }
        return downloadedPatients;
    }

    private List<String> getConceptUuidsFromConcepts(List<Concept> concepts) {
        List<String> conceptUuids = new ArrayList<String>();
        for (Concept concept : concepts) {
            conceptUuids.add(concept.getUuid());
        }
        return conceptUuids;
    }

    private List<Patient> downloadPatientsByUUID(String[] patientUUIDs) throws PatientController.PatientDownloadException {
        List<Patient> downloadedPatients = new ArrayList<Patient>();
        for (String patientUUID : patientUUIDs) {
            Log.i(TAG, "Downloading patient with UUID: " + patientUUID);
            downloadedPatients.add(patientController.downloadPatientByUUID(patientUUID));
        }
        downloadedPatients.removeAll(singleton(null));
        return downloadedPatients;
    }

    private List<Cohort> downloadCohortsList() throws CohortController.CohortDownloadException {
        List<String> cohortPrefixes = cohortPrefixPreferenceService.getCohortPrefixes();
        List<Cohort> cohorts;
        if (cohortPrefixes.isEmpty()) {
            cohorts = cohortController.downloadAllCohorts();
        } else {
            cohorts = cohortController.downloadCohortsByPrefix(cohortPrefixes);
        }
        return cohorts;
    }

    List<String> getPatientUuids(List<Patient> patients) {
        List<String> patientUuids = new ArrayList<String>();
        for (Patient patient : patients) {
            patientUuids.add(patient.getUuid());
        }
        return patientUuids;
    }
}
