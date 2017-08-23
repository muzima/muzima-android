/*
 * Copyright (c) 2014 - 2017. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
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
import com.muzima.controller.NotificationController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.PatientController;
import com.muzima.controller.ProviderController;
import com.muzima.controller.SetupConfigurationController;
import com.muzima.utils.Constants;
import com.muzima.utils.Constants.SERVER_CONNECTIVITY_STATUS;
import com.muzima.utils.NetworkUtils;
import com.muzima.utils.StringUtils;
import com.muzima.view.progressdialog.ProgressDialogUpdateIntentService;
import org.apache.lucene.queryParser.ParseException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants;
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
    private NotificationController notificationController;
    private LocationController locationController;
    private ProviderController providerController;
    private SetupConfigurationController setupConfigurationController;

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
            Log.e(TAG, "ConnectException Exception thrown while authentication.", e);
            return SyncStatusConstants.SERVER_CONNECTION_ERROR;
        } catch (ParseException e) {
            Log.e(TAG, "ParseException Exception thrown while authentication.", e);
            return SyncStatusConstants.PARSING_ERROR;
        } catch (MalformedURLException e) {
            Log.e(TAG, "IOException Exception thrown while authentication.", e);
            return SyncStatusConstants.MALFORMED_URL_ERROR;
        } catch (IOException e) {
            Log.e(TAG, "IOException Exception thrown while authentication.", e);
            return SyncStatusConstants.AUTHENTICATION_ERROR;
        } catch (AuthenticationException e) {
            Log.e(TAG, "Exception thrown while authentication.", e);
            return SyncStatusConstants.INVALID_CREDENTIALS_ERROR;
        } finally {
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
            Log.d(TAG, "In downloading forms: " + (endDownloadForms - startDownloadForms) / 1000 + " sec\n" +
                    "In replacing forms: " + (endDownloadForms - endSaveForms) / 1000 + " sec");

            result[0] = SyncStatusConstants.SUCCESS;
            result[1] = allDownloadedForms.size();
            result[2] = deletedFormCount;

        } catch (FormController.FormFetchException e) {
            Log.e(TAG, "Exception when trying to download forms", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
            return result;
        } catch (FormController.FormSaveException e) {
            Log.e(TAG, "Exception when trying to save forms", e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
            return result;
        } catch (FormController.FormDeleteException e) {
            Log.e(TAG, "Exception when trying to delete forms", e);
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
        Log.i(TAG, "Voided forms are deleted");
        List<Form> voidedForms = new ArrayList<Form>();
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
            Log.i(TAG, formTemplates.size() + " form template download successful");

            List<Concept> concepts = conceptController.getRelatedConcepts(formTemplates);
            List<Provider> providers = new ArrayList<Provider>();
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

            Log.i(TAG, "Form templates replaced");

            result[0] = SyncStatusConstants.SUCCESS;
            result[1] = formTemplates.size();
            result[2] = concepts.size();
        } catch (FormController.FormSaveException e) {
            Log.e(TAG, "Exception when trying to save forms", e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
            return result;
        } catch (FormController.FormFetchException e) {
            Log.e(TAG, "Exception when trying to download forms", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
            return result;
        } catch (ConceptController.ConceptDownloadException e) {
            Log.e(TAG, "Exception while parsing Concepts", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
            return result;
        } catch (ConceptController.ConceptSaveException e) {
            Log.e(TAG, "Exception when trying to download forms", e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
            return result;
        } catch (ConceptController.ConceptFetchException e) {
            e.printStackTrace();
        } catch (ProviderController.ProviderLoadException e) {
            Log.e(TAG, "Exception while loading Providers", e);
            result[0] = SyncStatusConstants.LOAD_ERROR;
            return result;
        } catch (ProviderController.ProviderSaveException e) {
            Log.e(TAG, "Exception while saving Provider", e);
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
            Log.i(TAG, formTemplates.size() + " form template download successful");

            if (replaceExistingTemplates) {
                formController.replaceFormTemplates(formTemplates);
            }
            else {
                formController.saveFormTemplates(formTemplates);
            }

            Log.i(TAG, "Form templates replaced");

            result[0] = SyncStatusConstants.SUCCESS;
            result[1] = formTemplates.size();
        } catch (FormController.FormSaveException e) {
            Log.e(TAG, "Exception when trying to save forms", e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
            return result;
        } catch (FormController.FormFetchException e) {
            Log.e(TAG, "Exception when trying to download forms", e);
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
            Log.i(TAG, "Downloaded "+locations.size()+" locations");

            result[0] = SyncStatusConstants.SUCCESS;
            result[1] = locations.size();
        } catch (LocationController.LocationSaveException e) {
            Log.e(TAG, "Exception when trying to save locations", e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
            return result;
        } catch (LocationController.LocationDownloadException e) {
            Log.e(TAG, "Exception when trying to download locations", e);
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
            Log.i(TAG, "Downloaded "+providers.size()+" providers");

            result[0] = SyncStatusConstants.SUCCESS;
            result[1] = providers.size();
        } catch (ProviderController.ProviderSaveException e) {
            Log.e(TAG, "Exception when trying to save providers", e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
            return result;
        } catch (ProviderController.ProviderDownloadException e) {
            Log.e(TAG, "Exception when trying to download providers", e);
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
            Log.i(TAG, "Downloaded "+concepts.size()+" concepts");

            result[0] = SyncStatusConstants.SUCCESS;
            result[1] = concepts.size();
        } catch (ConceptController.ConceptSaveException e) {
            Log.e(TAG, "Exception when trying to save concepts", e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
            return result;
        } catch (ConceptController.ConceptDownloadException e) {
            Log.e(TAG, "Exception when trying to download concepts", e);
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

            cohortController.saveAllCohorts(cohorts);
            Log.i(TAG, "New cohorts are saved");
            result[0] = SyncStatusConstants.SUCCESS;
            result[1] = cohorts.size();
            result[2] = voidedCohorts.size();
        } catch (CohortController.CohortDownloadException e) {
            Log.e(TAG, "Exception when trying to download cohorts", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
            return result;
        } catch (CohortController.CohortSaveException e) {
            Log.e(TAG, "Exception when trying to save cohorts", e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
            return result;
        } catch (CohortController.CohortDeleteException e) {
            Log.e(TAG, "Exception occurred while deleting voided cohorts", e);
            result[0] = SyncStatusConstants.DELETE_ERROR;
            return result;
        }
        return result;
    }

    private List<Cohort> deleteVoidedCohorts(List<Cohort> cohorts) throws CohortController.CohortDeleteException {
        Log.i(TAG, "Voided cohorts are deleted");
        List<Cohort> voidedCohorts = new ArrayList<Cohort>();
        for (Cohort cohort : cohorts) {
            if (cohort.isVoided()) {
                voidedCohorts.add(cohort);
            }
        }
        cohortController.deleteCohorts(voidedCohorts);
        return voidedCohorts;
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
            Log.d(TAG, "In Downloading cohort data: " + (endDownloadCohortData - startDownloadCohortData) / 1000 + " sec\n" +
                    "In Replacing cohort members and patients: " + (cohortMemberAndPatientReplaceTime - endDownloadCohortData) / 1000 + " sec");

            result[0] = SyncStatusConstants.SUCCESS;
            result[1] = patientCount;
            result[2] = cohortDataList.size();
            result[3] = voidedPatients.size();
        } catch (CohortController.CohortDownloadException e) {
            Log.e(TAG, "Exception thrown while downloading cohort data.", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
        } catch (CohortController.CohortReplaceException e) {
            Log.e(TAG, "Exception thrown while replacing cohort data.", e);
            result[0] = SyncStatusConstants.REPLACE_ERROR;
        } catch (PatientController.PatientSaveException e) {
            Log.e(TAG, "Exception thrown while replacing patients.", e);
            result[0] = SyncStatusConstants.REPLACE_ERROR;
        } catch (PatientController.PatientDeleteException e) {
            Log.e(TAG, "Exception thrown while deleting patients.", e);
            result[0] = SyncStatusConstants.DELETE_ERROR;
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
            result[0] = SyncStatusConstants.SUCCESS;
            result[1] = downloadedPatients.size();
        } catch (PatientController.PatientDownloadException e) {
            Log.e(TAG, "Error while downloading patients.", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
        } catch (PatientController.PatientSaveException e) {
            Log.e(TAG, "Error while saving patients.", e);
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
            for(Patient patient : patients){
                count++;
                Log.i(TAG, "Downloading Obs for patient " + count + " of "+ patientsTotal);
                updateProgressDialog(muzimaApplication.getString(R.string.info_observations_download_pogress, count, patientsTotal));
                List<String> patientlist = new ArrayList();
                patientlist.add(patient.getUuid());
                result = downloadObservationsForPatientsByPatientUUIDs(patientlist,replaceExistingObservation);
                if(result[0] != SyncStatusConstants.SUCCESS){
                    Log.e(TAG, "Obs for patient " + count + " of "+ patientsTotal + " not downloaded");
                    updateProgressDialog(muzimaApplication.getString(R.string.info_observations_not_downloaded_progress, count, patientsTotal));

                } else if(result[1] > 0) {
                    patientsObsDownloaded++;
                    totalObsDownloaded += result[1];
                }
            }
            result[1]=totalObsDownloaded;
            result[2]=patientsObsDownloaded;
        } catch (PatientController.PatientLoadException e) {
            Log.e(TAG, "Exception thrown while loading patients.", e);
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
        List<List<String>> lists = new ArrayList<List<String>>();

        int count = 0;
        boolean hasElements = !strings.isEmpty();
        while (hasElements) {
            int startElement = count * 50;
            int endElement = ++count * 50;
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
            long startDownloadObservations = System.currentTimeMillis();
            List<String> conceptUuidsFromConcepts = getConceptUuidsFromConcepts(conceptController.getConcepts());
            List<List<String>> slicedPatientUuids = split(patientUuids);
            List<List<String>> slicedConceptUuids = split(conceptUuidsFromConcepts);

            List<Observation> allObservations = new ArrayList<Observation>();
            for (List<String> slicedPatientUuid : slicedPatientUuids) {
                for (List<String> slicedConceptUuid : slicedConceptUuids) {
                    allObservations.addAll(
                            observationController.downloadObservationsByPatientUuidsAndConceptUuids(
                                    slicedPatientUuid, slicedConceptUuid)
                    );
                }
            }
            long endDownloadObservations = System.currentTimeMillis();
            Log.i(TAG, "Observations download successful with " + allObservations.size() + " observations");
            List<Observation> voidedObservations = getVoidedObservations(allObservations);
            observationController.deleteObservations(voidedObservations);
            allObservations.removeAll(voidedObservations);
            Log.i(TAG, "Voided observations delete successful with " + voidedObservations.size() + " observations");

            if(replaceExistingObservations) {
                observationController.replaceObservations(allObservations);
                long replacedObservations = System.currentTimeMillis();
                Log.d(TAG, "In Downloading observations : " + (endDownloadObservations - startDownloadObservations) / 1000 + " sec\n" +
                        "In Replacing observations for patients: " + (replacedObservations - endDownloadObservations) / 1000 + " sec");
            }
            else {
                observationController.saveObservations(allObservations);
                Log.d(TAG, "In Saving observations : " + (endDownloadObservations - startDownloadObservations) / 1000 + " sec\n");
            }

            result[0] = SyncStatusConstants.SUCCESS;
            result[1] = allObservations.size();
            result[2] = voidedObservations.size();
        } catch (ObservationController.DownloadObservationException e) {
            Log.e(TAG, "Exception thrown while downloading observations.", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
        } catch (ObservationController.ReplaceObservationException e) {
            Log.e(TAG, "Exception thrown while replacing observations.", e);
            result[0] = SyncStatusConstants.REPLACE_ERROR;
        } catch (ConceptController.ConceptFetchException e) {
            Log.e(TAG, "Exception thrown while loading concepts.", e);
            result[0] = SyncStatusConstants.LOAD_ERROR;
        } catch (ObservationController.DeleteObservationException e) {
            Log.e(TAG, "Exception thrown while deleting observations.", e);
            result[0] = SyncStatusConstants.DELETE_ERROR;
        } catch (ObservationController.SaveObservationException e) {
            Log.e(TAG, "Exception thrown while saving observations.", e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
        }

        return result;
    }

    private List<Observation> getVoidedObservations(List<Observation> allObservations) {
        List<Observation> voidedObservations = new ArrayList<Observation>();
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


            for(Patient patient : patients){
                count++;
                Log.i(TAG, "Downloading Encounters for patient " + count + " of "+ patientsTotal);
                updateProgressDialog(muzimaApplication.getString(R.string.info_encounter_download_progress, count, patientsTotal));
                List<String> patientlist = new ArrayList();
                patientlist.add(patient.getUuid());
                result = downloadEncountersForPatientsByPatientUUIDs(patientlist,replaceExistingEncounters);
                if(result[0] != SyncStatusConstants.SUCCESS){
                    Log.e(TAG, "Encounters for patient " + count + " of "+ patientsTotal + " not downloaded");
                    updateProgressDialog(muzimaApplication.getString(R.string.info_encounter_not_downloaded_progress, count, patientsTotal));
                } else if(result[1] > 0) {
                    patientsEncountersDownloaded++;
                    totalEncountersDownloaded += result[1];
                }
            }
            result[1]=totalEncountersDownloaded;
            result[2]=patientsEncountersDownloaded;



        } catch (PatientController.PatientLoadException e) {
            Log.e(TAG, "Exception thrown while loading patients.", e);
            result[0] = SyncStatusConstants.LOAD_ERROR;
        }
        return result;
    }

    public int[] downloadEncountersForPatientsByPatientUUIDs(List<String> patientUuids, boolean replaceExistingEncounters) {
        int[] result = new int[3];
        try {
            long startDownloadEncounters = System.currentTimeMillis();
            List<Encounter> allEncounters = new ArrayList<Encounter>();
            List<List<String>> slicedPatientUuids = split(patientUuids);
            for (List<String> slicedPatientUuid : slicedPatientUuids) {
                allEncounters.addAll(encounterController.downloadEncountersByPatientUuids(slicedPatientUuid));
            }
            long endDownloadObservations = System.currentTimeMillis();
            Log.i(TAG, "Encounters download successful with " + allEncounters.size() + " encounters");
            ArrayList<Encounter> voidedEncounters = getVoidedEncounters(allEncounters);
            allEncounters.removeAll(voidedEncounters);
            encounterController.deleteEncounters(voidedEncounters);
            Log.i(TAG, "Voided encounters delete successful with " + allEncounters.size() + " encounters");

            if(replaceExistingEncounters) {
                encounterController.replaceEncounters(allEncounters);
                long replacedEncounters = System.currentTimeMillis();
                Log.d(TAG, "In Downloading encounters : " + (endDownloadObservations - startDownloadEncounters) / 1000 + " sec\n" +
                        "In Replacing encounters for patients: " + (replacedEncounters - endDownloadObservations) / 1000 + " sec");
            }else {
                encounterController.saveEncounters(allEncounters);
                Log.d(TAG, "In Saving encounters : " + (endDownloadObservations - startDownloadEncounters) / 1000 + " sec\n" );
            }

            result[0] = SyncStatusConstants.SUCCESS;
            result[1] = allEncounters.size();
            result[2] = voidedEncounters.size();
        } catch (EncounterController.DownloadEncounterException e) {
            Log.e(TAG, "Exception thrown while downloading encounters.", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
        } catch (EncounterController.ReplaceEncounterException e) {
            Log.e(TAG, "Exception thrown while replacing encounters.", e);
            result[0] = SyncStatusConstants.REPLACE_ERROR;
        } catch (EncounterController.DeleteEncounterException e) {
            Log.e(TAG, "Exception thrown while deleting encounters.", e);
            result[0] = SyncStatusConstants.DELETE_ERROR;
        } catch (EncounterController.SaveEncounterException e) {
            Log.e(TAG, "Exception thrown while saving encounters.", e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
        }
        return result;
    }

    private ArrayList<Encounter> getVoidedEncounters(List<Encounter> allEncounters) {
        ArrayList<Encounter> voidedEncounters = new ArrayList<Encounter>();
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
            result[0] = formController.uploadAllCompletedForms() ? SyncStatusConstants.SUCCESS : SyncStatusConstants.UPLOAD_ERROR;
        } catch (FormController.UploadFormDataException e) {
            Log.e(TAG, "Exception thrown while uploading forms.", e);
            result[0] = SyncStatusConstants.UPLOAD_ERROR;
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


    public int[] downloadNotifications(String receiverUuid) {
        int[] result = new int[2];

        try {
            List<Notification> notifications;
            notifications = notificationController.downloadNotificationByReceiver(receiverUuid);
            Log.i(TAG, "Notifications download successful");
            notificationController.deleteAllNotifications(receiverUuid);
            Log.i(TAG, "Old notifications are deleted");
            notificationController.saveNotifications(notifications);
            Log.i(TAG, "New notifications are saved");

            result[0] = SyncStatusConstants.SUCCESS;
            result[1] = notifications.size();

        } catch (NotificationController.NotificationDownloadException e) {
            Log.e(TAG, "Exception when trying to download notifications", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
            return result;
        } catch (NotificationController.NotificationDeleteException e) {
            Log.e(TAG, "Exception occurred while deleting existing notifications", e);
            result[0] = SyncStatusConstants.DELETE_ERROR;
            return result;
        } catch (NotificationController.NotificationFetchException e) {
            Log.e(TAG, "Exception occurred while fetching existing notifications", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
            return result;
        } catch (NotificationController.NotificationSaveException e) {
            Log.e(TAG, "Exception when trying to save notifications", e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
            return result;
        } catch (ParseException e) {
            Log.e(TAG, "Exception when trying to download notifications", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
            return result;
        }
        return result;
    }

    public int[] downloadSetupConfigurations(){
        int[] result = new int[2];
        try {
            List<SetupConfiguration> setupConfigurations = setupConfigurationController.downloadAllSetupConfigurations();
            result[0] = SyncStatusConstants.SUCCESS;
            result[1] = setupConfigurations.size();
            Log.i(TAG, "Setup Configs downloaded: "+setupConfigurations.size());
            //ToDo: Remove all retired
            setupConfigurationController.saveSetupConfigurations(setupConfigurations);
        } catch (SetupConfigurationController.SetupConfigurationDownloadException e){
            Log.e(TAG, "Exception when trying to download setup configs");
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
        } catch (SetupConfigurationController.SetupConfigurationSaveException e){
            Log.e(TAG, "Exception when trying to save setup configs");
            result[0] = SyncStatusConstants.SAVE_ERROR;
        }
        return result;
    }

    public int[] downloadSetupConfigurationTemplate(String uuid){
        int[] result = new int[2];
        try {
            SetupConfigurationTemplate setupConfigurationTemplate =
                    setupConfigurationController.downloadSetupConfigurationTemplate(uuid);
            result[0] = SyncStatusConstants.SUCCESS;
            if(setupConfigurationTemplate != null) {
                result[1] = 1;
            }
            setupConfigurationController.saveSetupConfigurationTemplate(setupConfigurationTemplate);
        } catch (SetupConfigurationController.SetupConfigurationDownloadException e){
            Log.e(TAG, "Exception when trying to download setup configs");
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
        } catch (SetupConfigurationController.SetupConfigurationSaveException e){
            Log.e(TAG, "Exception when trying to save setup configs");
            result[0] = SyncStatusConstants.SAVE_ERROR;
        }
        return result;
    }
}