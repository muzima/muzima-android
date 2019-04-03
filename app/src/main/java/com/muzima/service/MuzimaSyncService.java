/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.service;

import android.content.Intent;
import android.util.Log;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.context.Context;
import com.muzima.api.exception.AuthenticationException;
import com.muzima.api.model.Cohort;
import com.muzima.api.model.CohortData;
import com.muzima.api.model.Concept;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.Form;
import com.muzima.api.model.FormTemplate;
import com.muzima.api.model.Location;
import com.muzima.api.model.MuzimaSetting;
import com.muzima.api.model.Notification;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Provider;
import com.muzima.api.model.SetupConfiguration;
import com.muzima.api.model.SetupConfigurationTemplate;
import com.muzima.controller.CohortController;
import com.muzima.controller.ConceptController;
import com.muzima.controller.EncounterController;
import com.muzima.controller.FormController;
import com.muzima.controller.LocationController;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.controller.NotificationController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.PatientController;
import com.muzima.controller.ProviderController;
import com.muzima.controller.SetupConfigurationController;
import com.muzima.utils.Constants;
import com.muzima.utils.NetworkUtils;
import com.muzima.view.progressdialog.ProgressDialogUpdateIntentService;

import org.apache.lucene.queryParser.ParseException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS;
import static com.muzima.utils.Constants.LOCAL_PATIENT;
import static java.util.Collections.singleton;

public class MuzimaSyncService {

    private final MuzimaApplication muzimaApplication;
    private final FormController formController;
    private final ConceptController conceptController;
    private final CohortController cohortController;
    private final PatientController patientController;
    private final ObservationController observationController;
    private final CohortPrefixPreferenceService cohortPrefixPreferenceService;
    private final EncounterController encounterController;
    private final NotificationController notificationController;
    private final LocationController locationController;
    private final ProviderController providerController;
    private final SetupConfigurationController setupConfigurationController;
    private final MuzimaSettingController settingsController;

    public MuzimaSyncService(MuzimaApplication muzimaContext) {
        this.muzimaApplication = muzimaContext;
        cohortPrefixPreferenceService = muzimaApplication.getCohortPrefixesPreferenceService();
        formController = muzimaApplication.getFormController();
        conceptController = muzimaApplication.getConceptController();
        cohortController = muzimaApplication.getCohortController();
        patientController = muzimaApplication.getPatientController();
        observationController = muzimaApplication.getObservationController();
        encounterController = muzimaApplication.getEncounterController();
        notificationController = muzimaApplication.getNotificationController();
        locationController = muzimaApplication.getLocationController();
        providerController = muzimaApplication.getProviderController();
        setupConfigurationController = muzimaApplication.getSetupConfigurationController();
        settingsController = muzimaApplication.getMuzimaSettingController();
    }

    public int authenticate(String[] credentials){
        return authenticate(credentials, false);
    }

    public int authenticate(String[] credentials, boolean isUpdatePasswordRequired) {
        String username = credentials[0].trim();
        String password = credentials[1];
        String server = credentials[2];

        Context muzimaContext = muzimaApplication.getMuzimaContext();
        try {
//            if(hasInvalidSpecialCharacter(username)){
//                return SyncStatusConstants.INVALID_CHARACTER_IN_USERNAME;
//            }

            muzimaContext.openSession();
            if (!muzimaContext.isAuthenticated()){
                if(isUpdatePasswordRequired && !NetworkUtils.isConnectedToNetwork(muzimaApplication)) {
                    return SyncStatusConstants.LOCAL_CONNECTION_ERROR;
                } else {
                    muzimaContext.authenticate(username, password, server, isUpdatePasswordRequired);
                }
            }
        } catch (ConnectException e) {
            Log.e(getClass().getSimpleName(), "ConnectException Exception thrown while authentication.", e);
            return SyncStatusConstants.SERVER_CONNECTION_ERROR;
        } catch (ParseException e) {
            Log.e(getClass().getSimpleName(), "ParseException Exception thrown while authentication.", e);
            return SyncStatusConstants.PARSING_ERROR;
        } catch (MalformedURLException e) {
            Log.e(getClass().getSimpleName(), "IOException Exception thrown while authentication.", e);
            return SyncStatusConstants.MALFORMED_URL_ERROR;
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "IOException Exception thrown while authentication.", e);
            return SyncStatusConstants.AUTHENTICATION_ERROR;
        } catch (AuthenticationException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while authentication.", e);
            return SyncStatusConstants.INVALID_CREDENTIALS_ERROR;
        } catch (IllegalArgumentException e) {
            Log.e(getClass().getSimpleName(),"IllegalArgumentException Exception thrown while authenticating.",e);
            return SyncStatusConstants.UNKNOWN_ERROR;
        }finally {
            if (muzimaContext != null)
                muzimaContext.closeSession();
        }
        return SyncStatusConstants.AUTHENTICATION_SUCCESS;
    }

    private boolean hasInvalidSpecialCharacter(String username) {
        String invalidCharacters = SyncStatusConstants.INVALID_CHARACTER_FOR_USERNAME;
        for (int i = 0; i < invalidCharacters.length(); i++) {
            String substring = invalidCharacters.substring(i, i + 1);
            if (username.contains(substring)) {
                return true;
            }
        }
        return false;
    }

    public int[] downloadForms() {
        int[] result = new int[3];

        try {
            long startDownloadForms = System.currentTimeMillis();
            List<Form> allDownloadedForms = formController.downloadAllForms();
            List<Form> allForms = formController.getAllAvailableForms();
            int deletedFormCount = getDeletedFormCount(allDownloadedForms, allForms);
            long endDownloadForms = System.currentTimeMillis();
            List<Form> voidedForms = deleteVoidedForms(allDownloadedForms);
            allDownloadedForms.removeAll(voidedForms);
            formController.updateAllForms(allDownloadedForms);
            long endSaveForms = System.currentTimeMillis();
            Log.d(getClass().getSimpleName(), "In downloading forms: " + (endDownloadForms - startDownloadForms) / 1000 + " sec\n" +
                    "In replacing forms: " + (endDownloadForms - endSaveForms) / 1000 + " sec");

            result[0] = SUCCESS;
            result[1] = allDownloadedForms.size();
            result[2] = deletedFormCount;

        } catch (FormController.FormFetchException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to download forms", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
            return result;
        } catch (FormController.FormSaveException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to save forms", e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
            return result;
        } catch (FormController.FormDeleteException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to delete forms", e);
            result[0] = SyncStatusConstants.DELETE_ERROR;
            return result;
        }
        return result;
    }

    private int getDeletedFormCount(List<Form> allDownloadedForms, List<Form> allForms) {
        int deletedFormCount = 0;
        for (Form form : allForms) {
            for (Form downloadedForm : allDownloadedForms) {
                if (form.getUuid().equals(downloadedForm.getUuid()) && downloadedForm.isRetired()) {
                    deletedFormCount++;
                }
            }
        }
        return deletedFormCount;
    }


    private List<Form> deleteVoidedForms(List<Form> forms) throws FormController.FormDeleteException {
        Log.i(getClass().getSimpleName(), "Voided forms are deleted");
        List<Form> voidedForms = new ArrayList<>();
        for (Form form : forms) {
            if (form.isRetired()) {
                voidedForms.add(form);
            }
        }
        formController.deleteForms(voidedForms);
        return voidedForms;
    }

    public int[] downloadFormTemplatesAndRelatedMetadata(String[] formIds, boolean replaceExistingTemplates) {
        int[] result = new int[4];

        try {
            List<FormTemplate> formTemplates = formController.downloadFormTemplates(formIds);
            Log.i(getClass().getSimpleName(), formTemplates.size() + " form template download successful");

            List<Concept> concepts = conceptController.getRelatedConcepts(formTemplates);
            List<Provider> providers = new ArrayList<>();
            Provider loggedInProvider = providerController.getLoggedInProvider(
                    muzimaApplication.getAuthenticatedUser().getSystemId());
            if(loggedInProvider != null) providers.add(loggedInProvider);

            if (replaceExistingTemplates) {
                formController.replaceFormTemplates(formTemplates);
                conceptController.newConcepts(concepts);
                providerController.newProviders(providers);
            }
            else {
                formController.saveFormTemplates(formTemplates);
            }

            conceptController.saveConcepts(concepts);
            providerController.saveProviders(providers);

            Log.i(getClass().getSimpleName(), "Form templates replaced");

            result[0] = SUCCESS;
            result[1] = formTemplates.size();
            result[2] = concepts.size();
        } catch (FormController.FormSaveException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to save forms", e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
            return result;
        } catch (FormController.FormFetchException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to download forms", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
            return result;
        } catch (ConceptController.ConceptDownloadException e) {
            Log.e(getClass().getSimpleName(), "Exception while parsing Concepts", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
            return result;
        } catch (ConceptController.ConceptSaveException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to download forms", e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
            return result;
        } catch (ConceptController.ConceptFetchException e) {
            e.printStackTrace();
        } catch (ProviderController.ProviderLoadException e) {
            Log.e(getClass().getSimpleName(), "Exception while loading Providers", e);
            result[0] = SyncStatusConstants.LOAD_ERROR;
            return result;
        } catch (ProviderController.ProviderSaveException e) {
            Log.e(getClass().getSimpleName(), "Exception while saving Provider", e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
            return result;
        }
        return result;
    }

    public int[] downloadFormTemplates(String[] formIds, boolean replaceExistingTemplates) {
        int[] result = new int[4];

        try {
            List<FormTemplate> formTemplates = formController.downloadFormTemplates(formIds);
            formTemplates.removeAll(Collections.singleton(null));
            Log.i(getClass().getSimpleName(), formTemplates.size() + " form template download successful");

            if (replaceExistingTemplates) {
                formController.replaceFormTemplates(formTemplates);
            }
            else {
                formController.saveFormTemplates(formTemplates);
            }

            Log.i(getClass().getSimpleName(), "Form templates replaced");

            result[0] = SUCCESS;
            result[1] = formTemplates.size();
        } catch (FormController.FormSaveException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to save forms", e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
            return result;
        } catch (FormController.FormFetchException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to download forms", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
            return result;
        }
        return result;
    }

    public int[] downloadLocations(String[] locationIds) {
        int[] result = new int[4];

        try {
            List<Location> locations = locationController.downloadLocationsFromServerByUuid(locationIds);
            locationController.saveLocations(locations);
            Log.i(getClass().getSimpleName(), "Downloaded "+locations.size()+" locations");

            result[0] = SUCCESS;
            result[1] = locations.size();
        } catch (LocationController.LocationSaveException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to save locations", e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
            return result;
        } catch (LocationController.LocationDownloadException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to download locations", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
            return result;
        }
        return result;
    }

    public int[] downloadProviders(String[] providerIds) {
        int[] result = new int[4];

        try {
            List<Provider> providers = providerController.downloadProvidersFromServerByUuid(providerIds);
            providerController.saveProviders(providers);
            Log.i(getClass().getSimpleName(), "Downloaded "+providers.size()+" providers");

            result[0] = SUCCESS;
            result[1] = providers.size();
        } catch (ProviderController.ProviderSaveException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to save providers", e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
            return result;
        } catch (ProviderController.ProviderDownloadException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to download providers", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
            return result;
        }
        return result;
    }

    public int[] downloadConcepts(String[] conceptIds) {
        int[] result = new int[4];

        try {
            List<Concept> concepts = conceptController.downloadConceptsByUuid(conceptIds);
            conceptController.saveConcepts(concepts);
            Log.i(getClass().getSimpleName(), "Downloaded "+concepts.size()+" concepts");

            result[0] = SUCCESS;
            result[1] = concepts.size();
        } catch (ConceptController.ConceptSaveException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to save concepts", e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
            return result;
        } catch (ConceptController.ConceptDownloadException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to download concepts", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
            return result;
        }
        return result;
    }

    public int[] downloadCohorts() {
        int[] result = new int[3];
        try {
            List<Cohort> cohorts = downloadCohortsList();
            List<Cohort> voidedCohorts = deleteVoidedCohorts(cohorts);
            cohorts.removeAll(voidedCohorts);
            cohortController.saveOrUpdateCohorts(cohorts);
            Log.i(getClass().getSimpleName(), "New cohorts are saved");
            result[0] = SUCCESS;
            result[1] = cohorts.size();
            result[2] = voidedCohorts.size();
        } catch (CohortController.CohortDownloadException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to download cohorts", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
            return result;
        } catch (CohortController.CohortSaveException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to save cohorts", e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
            return result;
        } catch (CohortController.CohortDeleteException e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while deleting voided cohorts", e);
            result[0] = SyncStatusConstants.DELETE_ERROR;
            return result;
        }
        return result;
    }

    public int[] downloadCohorts(String[] cohortUuids) {
        int[] result = new int[3];
        try {
            List<Cohort> cohorts = cohortController.downloadCohortsByUuidList(cohortUuids);
            List<Cohort> voidedCohorts = deleteVoidedCohorts(cohorts);
            cohorts.removeAll(voidedCohorts);

            cohortController.saveOrUpdateCohorts(cohorts);
            Log.i(getClass().getSimpleName(), "New cohorts are saved");
            result[0] = SUCCESS;
            result[1] = cohorts.size();
            result[2] = voidedCohorts.size();
        } catch (CohortController.CohortDownloadException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to download cohorts", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
            return result;
        } catch (CohortController.CohortSaveException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to save cohorts", e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
            return result;
        } catch (CohortController.CohortDeleteException e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while deleting voided cohorts", e);
            result[0] = SyncStatusConstants.DELETE_ERROR;
            return result;
        }
        return result;
    }

    public int[] updateCohortsWithUpdatesAvailable(){
        int[] result = new int[3];
        try {
            List<Cohort> cohortList = cohortController.getCohortsWithPendingUpdates();
            if(cohortList.size() > 0) {
                String[] cohortUuids = new String[cohortList.size()];
                int index = 0;
                for(Cohort cohort:cohortList){
                    cohortUuids[index] = cohort.getUuid();
                    index++;
                }
                return downloadPatientsForCohorts(cohortUuids);
            } else {
                result[0] = SyncStatusConstants.CANCELLED;
            }
        } catch (CohortController.CohortFetchException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while downloading cohort data.", e);
            result[0] = SyncStatusConstants.LOAD_ERROR;
        }
        return result;
    }

    private List<Cohort> deleteVoidedCohorts(List<Cohort> cohorts) throws CohortController.CohortDeleteException {
        Log.i(getClass().getSimpleName(), "Voided cohorts are deleted");
        List<Cohort> voidedCohorts = new ArrayList<>();
        for (Cohort cohort : cohorts) {
            if (cohort != null && cohort.isVoided()) {
                voidedCohorts.add(cohort);
            }
        }
        cohortController.deleteCohorts(voidedCohorts);
        return voidedCohorts;
    }

    public int[] downloadRemovedCohortMembershipData(String[] cohortUuids){
        int[] result = new int[4];
        try {
            List<CohortData> cohortDataList = cohortController.downloadRemovedCohortData(cohortUuids);

            for (CohortData cohortData : cohortDataList) {
                cohortController.deleteCohortMembers(cohortData.getCohortMembers());
            }

            result[0] = SUCCESS;
            result[1] = cohortDataList.size();
        } catch (CohortController.CohortDownloadException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while downloading cohort data.", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
        } catch (CohortController.CohortDeleteException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while replacing cohort data.", e);
            result[0] = SyncStatusConstants.REPLACE_ERROR;
        }
        return result;
    }

    public int[] downloadPatientsForCohorts(String[] cohortUuids) {
        int[] result = new int[4];

        int patientCount = 0;
        try {
            long startDownloadCohortData = System.currentTimeMillis();

            List<CohortData> cohortDataList = cohortController.downloadCohortData(cohortUuids);

            long endDownloadCohortData = System.currentTimeMillis();
            Log.i(getClass().getSimpleName(), "Cohort data download successful with " + cohortDataList.size() + " cohorts");
            ArrayList<Patient> voidedPatients = new ArrayList<>();
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

            Log.i(getClass().getSimpleName(), "Cohort data replaced");
            Log.i(getClass().getSimpleName(), "Patients downloaded " + patientCount);
            Log.d(getClass().getSimpleName(), "In Downloading cohort data: " + (endDownloadCohortData - startDownloadCohortData) / 1000 + " sec\n" +
                    "In Replacing cohort members and patients: " + (cohortMemberAndPatientReplaceTime - endDownloadCohortData) / 1000 + " sec");

            result[0] = SUCCESS;
            result[1] = patientCount;
            result[2] = cohortDataList.size();
            result[3] = voidedPatients.size();

            //update memberships
            downloadRemovedCohortMembershipData(cohortUuids);

            cohortController.markAsUpToDate(cohortUuids);
        } catch (CohortController.CohortDownloadException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while downloading cohort data.", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
        } catch (CohortController.CohortReplaceException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while replacing cohort data.", e);
            result[0] = SyncStatusConstants.REPLACE_ERROR;
        } catch (PatientController.PatientSaveException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while replacing patients.", e);
            result[0] = SyncStatusConstants.REPLACE_ERROR;
        } catch (PatientController.PatientDeleteException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while deleting patients.", e);
            result[0] = SyncStatusConstants.DELETE_ERROR;
        } catch (CohortController.CohortUpdateException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while marking cohorts as updated.", e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
        }
        return result;
    }

    private void getVoidedPatients(ArrayList<Patient> voidedPatients, List<Patient> cohortPatients) {
        for (Patient patient : cohortPatients) {
            if (patient.isVoided()) {
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
            Log.e(getClass().getSimpleName(), "DOWNLOADED PATIENTS.");
            result[0] = SUCCESS;
            result[1] = downloadedPatients.size();
        } catch (PatientController.PatientDownloadException e) {
            Log.e(getClass().getSimpleName(), "Error while downloading patients.", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
        } catch (PatientController.PatientSaveException e) {
            Log.e(getClass().getSimpleName(), "Error while saving patients.", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
        }
        return result;
    }
    public int[] downloadObservationsForPatientsByCohortUUIDs(String[] cohortUuids, boolean replaceExistingObservation) {
        int[] result = new int[3];
        List<Patient> patients;
        try {
            patients = patientController.getPatientsForCohorts(cohortUuids);
            int patientsTotal = patients.size();
            int patientsObsDownloaded = 0;
            int totalObsDownloaded = 0;

            int count = 0;
            List<String> patientlist = new ArrayList();
            for(Patient patient : patients){
                count++;
                Log.i(getClass().getSimpleName(), "Downloading Obs for patient " + count + " of "+ patientsTotal);
                updateProgressDialog(muzimaApplication.getString(R.string.info_observations_download_pogress, count, patientsTotal));
                patientlist.add(patient.getUuid());
            }
            result = downloadObservationsForPatientsByPatientUUIDs(patientlist,replaceExistingObservation);
            if(result[0] != SUCCESS){
                Log.e(getClass().getSimpleName(), "Obs for patient " + count + " of "+ patientsTotal + " not downloaded");
                updateProgressDialog(muzimaApplication.getString(R.string.info_observations_not_downloaded_progress, count, patientsTotal));

            }
        } catch (PatientController.PatientLoadException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while loading patients.", e);
            result[0] = SyncStatusConstants.LOAD_ERROR;
        }
        return result;
    }
    private void updateProgressDialog(String message){
        Intent progressUpdateIntent = new Intent(muzimaApplication.getApplicationContext(),ProgressDialogUpdateIntentService.class);
        progressUpdateIntent.putExtra(Constants.ProgressDialogConstants.PROGRESS_UPDATE_MESSAGE, message);
        muzimaApplication.getApplicationContext().startService(progressUpdateIntent);
    }

    private List<List<String>> split(final List<String> strings) {
        List<List<String>> lists = new ArrayList<>();

        int count = 0;
        boolean hasElements = !strings.isEmpty();
        while (hasElements) {
            int startElement = count * 100;
            int endElement = ++count * 100;
            hasElements = strings.size() > endElement;
            if (hasElements) {
                lists.add(strings.subList(startElement, endElement));
            } else {
                lists.add(strings.subList(startElement, strings.size()));
            }
        }

        return lists;
    }

    public int[] downloadObservationsForPatientsByPatientUUIDs(List<String> patientUuids, boolean replaceExistingObservations) {
        int[] result = new int[3];
        try {

            List<String> conceptUuidsFromConcepts = getConceptUuidsFromConcepts(conceptController.getConcepts());
            List<List<String>> slicedPatientUuids = split(patientUuids);
            List<List<String>> slicedConceptUuids = split(conceptUuidsFromConcepts);
            long totalTimeDownloading = 0, totalTimeReplacing = 0,totalTimeSaving = 0;
            int i=0;
            for (List<String> slicedPatientUuid : slicedPatientUuids) {
                for (List<String> slicedConceptUuid : slicedConceptUuids) {
                    long startDownloadObservations = System.currentTimeMillis();
                    List<Observation> allObservations = new ArrayList<>(observationController.downloadObservationsByPatientUuidsAndConceptUuids(
                            slicedPatientUuid, slicedConceptUuid));
                    Log.i(getClass().getSimpleName(), "Downloading observations for " + slicedPatientUuid.size() + " patients and " +slicedConceptUuid.size() +" concepts");
                    long endDownloadObservations = System.currentTimeMillis();
                    Log.i(getClass().getSimpleName(), "Observations download successful with " + allObservations.size() + " observations");
                    Log.d(getClass().getSimpleName(), "In Downloading observations : " + (endDownloadObservations - startDownloadObservations) / 1000 + " sec");
                    totalTimeDownloading += endDownloadObservations - startDownloadObservations;
                    List<Observation> voidedObservations = getVoidedObservations(allObservations);
                    observationController.deleteObservations(voidedObservations);
                    allObservations.removeAll(voidedObservations);
                    Log.i(getClass().getSimpleName(), "Voided observations delete successful with " + voidedObservations.size() + " observations");

                    if (replaceExistingObservations) {
                        observationController.replaceObservations(allObservations);
                        long replacedObservations = System.currentTimeMillis();
                        Log.d(getClass().getSimpleName(), "In Replacing observations for patients: " + (replacedObservations - endDownloadObservations) / 1000 + " sec");
                        totalTimeReplacing += replacedObservations - endDownloadObservations;
                    } else {
                        observationController.saveObservations(allObservations);
                        long savedObservations = System.currentTimeMillis();
                        Log.d(getClass().getSimpleName(), "In saving observations : " + (savedObservations - endDownloadObservations) / 1000 + " sec\n");
                        totalTimeSaving += savedObservations - endDownloadObservations;
                    }

                    result[1] += allObservations.size();
                    result[2] += slicedPatientUuid.size();

                }
            }
            result[0] = SUCCESS;
            Log.d(getClass().getSimpleName(), "Total Downloading observations : " + totalTimeDownloading/1000 + " sec\n");
            Log.d(getClass().getSimpleName(), "Total Replacing observations : " + totalTimeReplacing/1000 + " sec\n");
            Log.d(getClass().getSimpleName(), "Total Saving observations : " + totalTimeSaving/1000 + " sec\n");
        } catch (ObservationController.DownloadObservationException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while downloading observations.", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
        } catch (ObservationController.ReplaceObservationException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while replacing observations.", e);
            result[0] = SyncStatusConstants.REPLACE_ERROR;
        } catch (ConceptController.ConceptFetchException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while loading concepts.", e);
            result[0] = SyncStatusConstants.LOAD_ERROR;
        } catch (ObservationController.DeleteObservationException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while deleting observations.", e);
            result[0] = SyncStatusConstants.DELETE_ERROR;
        } catch (ObservationController.SaveObservationException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while saving observations.", e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
        }

        return result;
    }

    private List<Observation> getVoidedObservations(List<Observation> allObservations) {
        List<Observation> voidedObservations = new ArrayList<>();
        for (Observation observation : allObservations) {
            if (observation.isVoided()) {
                voidedObservations.add(observation);
            }
        }
        return voidedObservations;
    }

    public int[] downloadEncountersForPatientsByCohortUUIDs(String[] cohortUuids, boolean replaceExistingEncounters) {
        int[] result = new int[3];
        List<Patient> patients;
        try {
            patients = patientController.getPatientsForCohorts(cohortUuids);

            int patientsTotal = patients.size();
            int count = 0;
            int patientsEncountersDownloaded=0;
            int totalEncountersDownloaded=0;

            List<String> patientlist = new ArrayList();
            for(Patient patient : patients){
                count++;
                //Log.i(getClass().getSimpleName(), "Downloading Encounters for patient " + count + " of "+ patientsTotal);
                //updateProgressDialog(muzimaApplication.getString(R.string.info_encounter_download_progress, count, patientsTotal));

                patientlist.add(patient.getUuid());
            }
            result = downloadEncountersForPatientsByPatientUUIDs(patientlist,replaceExistingEncounters);
            if(result[0] != SUCCESS){
                Log.e(getClass().getSimpleName(), "Encounters for patient " + count + " of "+ patientsTotal + " not downloaded");
                updateProgressDialog(muzimaApplication.getString(R.string.info_encounter_not_downloaded_progress, count, patientsTotal));
            }

        } catch (PatientController.PatientLoadException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while loading patients.", e);
            result[0] = SyncStatusConstants.LOAD_ERROR;
        }
        return result;
    }

    public int[] downloadEncountersForPatientsByPatientUUIDs(List<String> patientUuids, boolean replaceExistingEncounters) {
        int[] result = new int[3];
        try {
            List<List<String>> slicedPatientUuids = split(patientUuids);
            long totalTimeDownloading = 0, totalTimeReplacing = 0,totalTimeSaving = 0;
            int i =0;
            for (List<String> slicedPatientUuid : slicedPatientUuids) {
                Log.i(getClass().getSimpleName(), "Downloading encounters for "+ slicedPatientUuid.size() + " patients");
                long startDownloadEncounters = System.currentTimeMillis();
                List<Encounter> allEncounters = new ArrayList<>(encounterController.downloadEncountersByPatientUuids(slicedPatientUuid));

                long endDownloadObservations = System.currentTimeMillis();
                Log.d(getClass().getSimpleName(), "In Downloading encounters : " + (endDownloadObservations - startDownloadEncounters) / 1000 + " sec\n");
                totalTimeDownloading += endDownloadObservations - startDownloadEncounters;

                Log.i(getClass().getSimpleName(), "Encounters download successful with " + allEncounters.size() + " encounters");
                ArrayList<Encounter> voidedEncounters = getVoidedEncounters(allEncounters);
                allEncounters.removeAll(voidedEncounters);
                encounterController.deleteEncounters(voidedEncounters);
                Log.i(getClass().getSimpleName(), "Voided encounters delete successful with " + voidedEncounters.size() + " encounters");

                if (replaceExistingEncounters) {
                    encounterController.replaceEncounters(allEncounters);
                    long replacedEncounters = System.currentTimeMillis();
                    Log.d(getClass().getSimpleName(), "In Replacing encounters for patients: " + (replacedEncounters - endDownloadObservations) / 1000 + " sec");
                    totalTimeReplacing += replacedEncounters - endDownloadObservations;
                } else {
                    encounterController.saveEncounters(allEncounters);
                    long savedEncounters = System.currentTimeMillis();
                    Log.d(getClass().getSimpleName(), "In Saving encounters : " + (savedEncounters - endDownloadObservations) / 1000 + " sec\n");
                    totalTimeSaving += savedEncounters - endDownloadObservations;
                }
                result[1] += allEncounters.size();
                result[2] += slicedPatientUuid.size();
            }
            Log.d(getClass().getSimpleName(), "Total Downloading encounters : " + totalTimeDownloading/1000 + " sec\n");
            Log.d(getClass().getSimpleName(), "Total Replacing encounters : " + totalTimeReplacing/1000 + " sec\n");
            Log.d(getClass().getSimpleName(), "Total Saving encounters : " + totalTimeSaving/1000 + " sec\n");
            result[0] = SUCCESS;
        } catch (EncounterController.DownloadEncounterException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while downloading encounters.", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
        } catch (EncounterController.ReplaceEncounterException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while replacing encounters.", e);
            result[0] = SyncStatusConstants.REPLACE_ERROR;
        } catch (EncounterController.DeleteEncounterException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while deleting encounters.", e);
            result[0] = SyncStatusConstants.DELETE_ERROR;
        } catch (EncounterController.SaveEncounterException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while saving encounters.", e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
        }
        return result;
    }

    private ArrayList<Encounter> getVoidedEncounters(List<Encounter> allEncounters) {
        ArrayList<Encounter> voidedEncounters = new ArrayList<>();
        for (Encounter encounter : allEncounters) {
            if (encounter.isVoided()) {
                voidedEncounters.add(encounter);
            }
        }
        return voidedEncounters;
    }

    public int[] uploadAllCompletedForms() {
        int[] result = new int[1];

        try {
             result[0] = formController.uploadAllCompletedForms() ? SUCCESS : SyncStatusConstants.UPLOAD_ERROR;
        } catch (FormController.UploadFormDataException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while uploading forms.", e);
            String exceptionError = e.getMessage();
            String []  exceptionErrorArray= exceptionError.split(":", 0);
            String uploadError = "";
            int i=0;
            for (String error : exceptionErrorArray) {
                if(i==0) {
                    uploadError = error.trim();
                }
                i++;
            }
            if(uploadError.equals("java.net.ConnectException")){
                result[0] = SyncStatusConstants.SERVER_CONNECTION_ERROR;
            }
            else {
                result[0] = SyncStatusConstants.UPLOAD_ERROR;
            }
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
                    Log.e(getClass().getSimpleName(), "Error while saving patients.", e);
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
                Log.e(getClass().getSimpleName(), "Exception thrown when writing to phone disk.", e);
            }
        }
    }

    public List<Patient> updatePatientsNotPartOfCohorts() {
        List<Patient> patientsNotInCohorts = patientController.getPatientsNotInCohorts();
        List<Patient> downloadedPatients = new ArrayList<>();
        try {
            for (Patient patient : patientsNotInCohorts) {
                downloadedPatients.add(patientController.downloadPatientByUUID(patient.getUuid()));
            }
            downloadedPatients.removeAll(singleton(null));
            patientController.replacePatients(downloadedPatients);
        } catch (PatientController.PatientSaveException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while updating patients from server.", e);
        } catch (PatientController.PatientDownloadException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while downloading patients from server.", e);
        }
        return downloadedPatients;
    }

    private List<String> getConceptUuidsFromConcepts(List<Concept> concepts) {
        List<String> conceptUuids = new ArrayList<>();
        for (Concept concept : concepts) {
            conceptUuids.add(concept.getUuid());
        }
        return conceptUuids;
    }

    private List<Patient> downloadPatientsByUUID(String[] patientUUIDs) throws PatientController.PatientDownloadException {
        List<Patient> downloadedPatients = new ArrayList<>();
        for (String patientUUID : patientUUIDs) {
            Log.i(getClass().getSimpleName(), "Downloading patient with UUID: " + patientUUID);
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
        List<String> patientUuids = new ArrayList<>();
        for (Patient patient : patients) {
            patientUuids.add(patient.getUuid());
        }
        return patientUuids;
    }


    public int[] downloadNotifications(String receiverUuid) {
        Log.e(getClass().getSimpleName(),"Downloading messages in MuzimaSyncService");
        int[] result = new int[2];

        try {
            List<Notification> notifications;
            notifications = notificationController.downloadNotificationByReceiver(receiverUuid);
            Log.i(getClass().getSimpleName(), "Notifications download successful");
            notificationController.saveNotifications(notifications);
            Log.i(getClass().getSimpleName(), "New notifications are saved");

            List<Notification> senderNotifications;
            senderNotifications = notificationController.downloadNotificationBySender(receiverUuid);
            notificationController.saveNotifications(senderNotifications);

            result[0] = SUCCESS;
            result[1] = notifications.size();

        } catch (NotificationController.NotificationDownloadException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to download notifications", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
            return result;
        } catch (NotificationController.NotificationSaveException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to save notifications", e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
            return result;
        }
        return result;
    }

    public void downloadSetupConfigurations(){
        int[] result = new int[2];
        try {
            List<SetupConfiguration> setupConfigurations = setupConfigurationController.downloadAllSetupConfigurations();
            result[0] = SUCCESS;
            result[1] = setupConfigurations.size();
            Log.i(getClass().getSimpleName(), "Setup Configs downloaded: "+setupConfigurations.size());
            //ToDo: Remove all retired
            setupConfigurationController.saveSetupConfigurations(setupConfigurations);
        } catch (SetupConfigurationController.SetupConfigurationDownloadException e){
            Log.e(getClass().getSimpleName(), "Exception when trying to download setup configs");
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
        } catch (SetupConfigurationController.SetupConfigurationSaveException e){
            Log.e(getClass().getSimpleName(), "Exception when trying to save setup configs");
            result[0] = SyncStatusConstants.SAVE_ERROR;
        }
    }

    public int[] downloadSetupConfigurationTemplate(String uuid){
        int[] result = new int[2];
        try {
            SetupConfigurationTemplate setupConfigurationTemplate =
                    setupConfigurationController.downloadSetupConfigurationTemplate(uuid);
            result[0] = SUCCESS;
            if(setupConfigurationTemplate != null) {
                result[1] = 1;
            }
            setupConfigurationController.saveSetupConfigurationTemplate(setupConfigurationTemplate);
        } catch (SetupConfigurationController.SetupConfigurationDownloadException e){
            Log.e(getClass().getSimpleName(), "Exception when trying to download setup configs");
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
        } catch (SetupConfigurationController.SetupConfigurationSaveException e){
            Log.e(getClass().getSimpleName(), "Exception when trying to save setup configs");
            result[0] = SyncStatusConstants.SAVE_ERROR;
        }
        return result;
    }

    public int[] downloadSetting(String property){
        int[] result = new int[2];
        try {
            MuzimaSetting setting = settingsController.downloadSettingByProperty(property);
            result[0] = SUCCESS;
            if(setting != null) {
                result[1] = 1;
                settingsController.saveOrUpdateSetting(setting);
            }
        } catch (MuzimaSettingController.MuzimaSettingDownloadException e){
            Log.e(getClass().getSimpleName(), "Exception when trying to download setting.",e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
        } catch (MuzimaSettingController.MuzimaSettingSaveException e){
            Log.e(getClass().getSimpleName(), "Exception when trying to save setting.",e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
        }
        return result;
    }

    public int[] downloadMissingMandatorySettings(){

        int[] result = new int[2];
        try {
            List<String> properties = settingsController.getNonDownloadedMandatorySettingsProperties();
            for(String property:properties) {
                MuzimaSetting setting = settingsController.downloadSettingByProperty(property);
                result[0] = SUCCESS;

                if (setting != null) {
                    result[1]++;
                    settingsController.saveSetting(setting);
                }
            }
        } catch (MuzimaSettingController.MuzimaSettingDownloadException e){
            Log.e(getClass().getSimpleName(), "Exception when trying to download setting",e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
        } catch (MuzimaSettingController.MuzimaSettingSaveException e){
            Log.e(getClass().getSimpleName(), "Exception when trying to save setting",e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
        } catch (MuzimaSettingController.MuzimaSettingFetchException e){
            Log.e(getClass().getSimpleName(), "Exception when trying to read setting",e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
        }
        return result;
    }
}