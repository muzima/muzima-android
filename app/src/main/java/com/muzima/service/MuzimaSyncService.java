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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
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
import com.muzima.api.model.FormData;
import com.muzima.api.model.FormDataStatus;
import com.muzima.api.model.FormTemplate;
import com.muzima.api.model.Location;
import com.muzima.api.model.Notification;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PatientReport;
import com.muzima.api.model.PatientReportHeader;
import com.muzima.api.model.PatientTag;
import com.muzima.api.model.Person;
import com.muzima.api.model.PersonAddress;
import com.muzima.api.model.Provider;
import com.muzima.api.model.MuzimaSetting;
import com.muzima.api.model.Relationship;
import com.muzima.api.model.RelationshipType;
import com.muzima.api.model.ReportDataset;
import com.muzima.api.model.SetupConfiguration;
import com.muzima.api.model.SetupConfigurationTemplate;
import com.muzima.api.model.Tag;
import com.muzima.controller.CohortController;
import com.muzima.controller.ConceptController;
import com.muzima.controller.EncounterController;
import com.muzima.controller.FormController;
import com.muzima.controller.LocationController;
import com.muzima.controller.PatientReportController;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.controller.NotificationController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.PatientController;
import com.muzima.controller.PersonController;
import com.muzima.controller.ProviderController;
import com.muzima.controller.RelationshipController;
import com.muzima.controller.ReportDatasetController;
import com.muzima.controller.SetupConfigurationController;
import com.muzima.util.MuzimaSettingUtils;
import com.muzima.utils.Constants;
import com.muzima.utils.NetworkUtils;
import com.muzima.utils.StringUtils;
import com.muzima.view.progressdialog.ProgressDialogUpdateIntentService;

import org.apache.lucene.queryParser.ParseException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.DELETE_ERROR;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.DOWNLOAD_ERROR;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS;
import static com.muzima.utils.Constants.FGH.Concepts.HEALTHWORKER_ASSIGNMENT_CONCEPT_ID;
import static com.muzima.utils.Constants.FGH.Concepts.INDEX_CASE_TESTING_CONSENT_CONCEPT_ID;
import static com.muzima.utils.Constants.FGH.TagsUuids.ALREADY_ASSIGNED_TAG_UUID;
import static com.muzima.utils.Constants.FGH.TagsUuids.AWAITING_ASSIGNMENT_TAG_UUID;
import static com.muzima.utils.Constants.FGH.TagsUuids.HAS_SEXUAL_PARTNER_TAG_UUID;
import static com.muzima.utils.Constants.LOCAL_PATIENT;
import static java.util.Collections.singleton;

public class MuzimaSyncService {
    private static final String TAG = "MuzimaSyncService";

    private final MuzimaApplication muzimaApplication;
    private final FormController formController;
    private final ConceptController conceptController;
    private final CohortController cohortController;
    private final PatientController patientController;
    private final PersonController personController;
    private final ObservationController observationController;
    private final CohortPrefixPreferenceService cohortPrefixPreferenceService;
    private EncounterController encounterController;
    private NotificationController notificationController;
    private LocationController locationController;
    private ProviderController providerController;
    private SetupConfigurationController setupConfigurationController;
    private MuzimaSettingController settingsController;
    private PatientReportController patientReportController;
    private RelationshipController relationshipController;
    private ReportDatasetController reportDatasetController;
    private Logger logger;

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
        patientReportController = muzimaApplication.getPatientReportController();
        relationshipController = muzimaApplication.getRelationshipController();
        personController = muzimaApplication.getPersonController();
        reportDatasetController = muzimaApplication.getReportDatasetController();
    }

    public int authenticate(String[] credentials) {
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
            if (!muzimaContext.isAuthenticated()) {
                if (isUpdatePasswordRequired && !NetworkUtils.isConnectedToNetwork(muzimaApplication)) {
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
            Log.e(getClass().getSimpleName(), "IllegalArgumentException Exception thrown while authenticating.", e);
            return SyncStatusConstants.UNKNOWN_ERROR;
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
            markUpdatedFormsAsUpdatePendingDownload(allDownloadedForms,allForms);
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

    private void markUpdatedFormsAsUpdatePendingDownload(List<Form> downloadedForms, List<Form> availableForms) {
        for (Form form : availableForms) {
            for (Form downloadedForm : downloadedForms) {
                if (StringUtils.equals(form.getUuid(),downloadedForm.getUuid()) &&

                downloadedForm.getDateChanged() != null &&
                        (form.getDateChanged() != null && downloadedForm.getDateChanged().compareTo(form.getDateChanged()) > 0
                || form.getDateCreated() != null && downloadedForm.getDateChanged().compareTo(form.getDateCreated()) > 0
                || form.getDateCreated() == null)){
                    downloadedForm.setUpdateAvailable(true);
                }
            }
        }
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
            if (loggedInProvider != null) providers.add(loggedInProvider);

            if (replaceExistingTemplates) {
                formController.replaceFormTemplates(formTemplates);
                conceptController.newConcepts(concepts);
                providerController.newProviders(providers);
                markFormTemplatesAsUpToDate(formTemplates);
            } else {
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

    private void markFormTemplatesAsUpToDate(List<FormTemplate> formTemplates)
            throws FormController.FormSaveException, FormController.FormFetchException {
        List forms = new ArrayList();
        for(FormTemplate formTemplate:formTemplates){
            Form form = formController.getFormByUuid(formTemplate.getUuid());
            if(form != null) {
                form.setUpdateAvailable(false);
                forms.add(form);
            }
        }
        if(forms.size()>0){
            formController.updateAllForms(forms);
        }
    }

    public int[] downloadFormTemplates(String[] formIds, boolean replaceExistingTemplates) {
        int[] result = new int[4];

        try {
            List<FormTemplate> formTemplates = formController.downloadFormTemplates(formIds);
            formTemplates.removeAll(Collections.singleton(null));
            Log.i(getClass().getSimpleName(), formTemplates.size() + " form template download successful");

            if (replaceExistingTemplates) {
                formController.replaceFormTemplates(formTemplates);
            } else {
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
            Log.i(getClass().getSimpleName(), "Downloaded " + locations.size() + " locations");

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
            Log.i(getClass().getSimpleName(), "Downloaded " + providers.size() + " providers");

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
            Log.i(getClass().getSimpleName(), "Downloaded " + concepts.size() + " concepts");

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

    public int[] downloadPatientsForCohortsWithUpdatesAvailable() {
        int[] result = new int[3];
        try {
            List<Cohort> cohortList = cohortController.getCohortsWithPendingUpdates();
            if (cohortList.size() > 0) {
                String[] cohortUuids = new String[cohortList.size()];
                int index = 0;
                for (Cohort cohort : cohortList) {
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

    public int[] downloadRemovedCohortMembershipData(String[] cohortUuids) {
        int[] result = new int[4];
        try {
            List<CohortData> cohortDataList = cohortController.downloadRemovedCohortData(cohortUuids);

            for (CohortData cohortData : cohortDataList) {
                cohortController.deleteCohortMembers(cohortData.getCohortMembers());
                patientController.deletePatientByCohortMembership(cohortData.getCohortMembers());
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

            List<CohortData> cohortDataList = cohortController.downloadCohortData(cohortUuids, getDefaultLocation());

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
            cohortController.setSyncStatus(cohortUuids, 1);
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
        int[] result = new int[4];
        List<Patient> patients;
        try {
            patients = patientController.getPatientsForCohorts(cohortUuids);

            List<String> patientlist = new ArrayList();
            for (Patient patient : patients) {
                patientlist.add(patient.getUuid());
            }
            result = downloadObservationsForPatientsByPatientUUIDs(patientlist, replaceExistingObservation);
            if (result[0] != SUCCESS) {
                updateProgressDialog(muzimaApplication.getString(R.string.error_encounter_observation_download));

            }
        } catch (PatientController.PatientLoadException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while loading patients.", e);
            result[0] = SyncStatusConstants.LOAD_ERROR;
        }
        return result;
    }

    public int[] downloadObservationsForAllPersons(boolean replaceExistingObservation) {
        int[] result = new int[4];
        List<Person> persons;
        try {
            persons = personController.getAllPersons();

            List<String> personUuidList = new ArrayList();
            for (Person person: persons) {
                personUuidList.add(person.getUuid());
            }
            result = downloadObservationsForPatientsByPatientUUIDs(personUuidList, replaceExistingObservation);
            if (result[0] != SUCCESS) {
                updateProgressDialog(muzimaApplication.getString(R.string.error_encounter_observation_download));

            }
        } catch (PersonController.PersonLoadException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while loading persons.", e);
            result[0] = SyncStatusConstants.LOAD_ERROR;
        }
        return result;
    }

    private void updateProgressDialog(String message) {
        Intent progressUpdateIntent = new Intent(muzimaApplication.getApplicationContext(), ProgressDialogUpdateIntentService.class);
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
        int[] result = new int[4];
        try {
            List<String> conceptUuidsFromConcepts = getConceptUuidsFromConcepts(conceptController.getConcepts());
            List<List<String>> slicedPatientUuids = split(patientUuids);
            List<List<String>> slicedConceptUuids = split(conceptUuidsFromConcepts);
            Set<String> patientUuidsForDownloadedObs = new HashSet<>();
            long totalTimeDownloading = 0, totalTimeReplacing = 0, totalTimeSaving = 0;

            String activeSetupConfigUuid = null;
            try {
                SetupConfigurationTemplate setupConfigurationTemplate = setupConfigurationController.getActiveSetupConfigurationTemplate();
                if (setupConfigurationTemplate != null) {
                    activeSetupConfigUuid = setupConfigurationTemplate.getUuid();
                }
            } catch (SetupConfigurationController.SetupConfigurationFetchException e) {
                Log.e(getClass().getSimpleName(), "Could not obtain active setup config", e);
            }

            int i = 0;
            for (List<String> slicedPatientUuid : slicedPatientUuids) {
                for (List<String> slicedConceptUuid : slicedConceptUuids) {
                    long startDownloadObservations = System.currentTimeMillis();

                    List<Observation> allObservations = new ArrayList<>(observationController.downloadObservationsByPatientUuidsAndConceptUuids(
                            slicedPatientUuid, slicedConceptUuid,activeSetupConfigUuid));

                    for (Observation observation : allObservations) {
                        patientUuidsForDownloadedObs.add(observation.getPerson().getUuid());
                    }

                    updateProgressDialog(muzimaApplication.getString(R.string.info_observations_download_progress, patientUuidsForDownloadedObs.size(), patientUuids.size()));

                    Log.i(getClass().getSimpleName(), "Downloading observations for " + slicedPatientUuid.size() + " patients and " + slicedConceptUuid.size() + " concepts");
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
                    result[2] += voidedObservations.size();

                }
            }
            result[3] = patientUuidsForDownloadedObs.size();
            result[0] = SUCCESS;
            Log.d(getClass().getSimpleName(), "Total Downloading observations : " + totalTimeDownloading / 1000 + " sec\n");
            Log.d(getClass().getSimpleName(), "Total Replacing observations : " + totalTimeReplacing / 1000 + " sec\n");
            Log.d(getClass().getSimpleName(), "Total Saving observations : " + totalTimeSaving / 1000 + " sec\n");
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
        int[] result = new int[4];
        List<Patient> patients;
        try {
            patients = patientController.getPatientsForCohorts(cohortUuids);

            List<String> patientlist = new ArrayList();
            for (Patient patient : patients) {
                patientlist.add(patient.getUuid());
            }
            result = downloadEncountersForPatientsByPatientUUIDs(patientlist, replaceExistingEncounters);
            if (result[0] != SUCCESS) {
                Log.e(getClass().getSimpleName(), "Could not download encounters");
                updateProgressDialog(muzimaApplication.getString(R.string.error_encounter_download));
            }

        } catch (PatientController.PatientLoadException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while loading patients.", e);
            result[0] = SyncStatusConstants.LOAD_ERROR;
        }
        return result;
    }

    public int[] downloadEncountersForPatientsByPatientUUIDs(List<String> patientUuids, boolean replaceExistingEncounters) {
        int[] result = new int[4];
        try {
            String activeSetupConfigUuid = null;
            try {
                SetupConfigurationTemplate setupConfigurationTemplate = setupConfigurationController.getActiveSetupConfigurationTemplate();
                if (setupConfigurationTemplate != null) {
                    activeSetupConfigUuid = setupConfigurationTemplate.getUuid();
                }
            } catch (SetupConfigurationController.SetupConfigurationFetchException e) {
                Log.e(getClass().getSimpleName(), "Could not obtain active setup config", e);
            }
            List<List<String>> slicedPatientUuids = split(patientUuids);
            Set<String> patientsForDownloadedEncounters = new HashSet();
            long totalTimeDownloading = 0, totalTimeReplacing = 0, totalTimeSaving = 0;
            int i = 0;
            for (List<String> slicedPatientUuid : slicedPatientUuids) {
                Log.i(getClass().getSimpleName(), "Downloading encounters for " + slicedPatientUuid.size() + " patients");
                long startDownloadEncounters = System.currentTimeMillis();
                List<Encounter> allEncounters = new ArrayList<>(encounterController.downloadEncountersByPatientUuids(slicedPatientUuid, activeSetupConfigUuid));

                long endDownloadObservations = System.currentTimeMillis();
                Log.d(getClass().getSimpleName(), "In Downloading encounters : " + (endDownloadObservations - startDownloadEncounters) / 1000 + " sec\n");
                totalTimeDownloading += endDownloadObservations - startDownloadEncounters;

                Log.i(getClass().getSimpleName(), "Encounters download successful with " + allEncounters.size() + " encounters");
                for (Encounter encounter : allEncounters) {
                    patientsForDownloadedEncounters.add(encounter.getPatient().getUuid());
                }
                Log.i(getClass().getSimpleName(), "Downloaded Encounters for patient " + patientsForDownloadedEncounters.size() + " of " + patientUuids.size());
                updateProgressDialog(muzimaApplication.getString(R.string.info_encounter_download_progress, patientsForDownloadedEncounters.size(), patientUuids.size()));

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
                result[2] += voidedEncounters.size();
            }
            Log.d(getClass().getSimpleName(), "Total Downloading encounters : " + totalTimeDownloading / 1000 + " sec\n");
            Log.d(getClass().getSimpleName(), "Total Replacing encounters : " + totalTimeReplacing / 1000 + " sec\n");
            Log.d(getClass().getSimpleName(), "Total Saving encounters : " + totalTimeSaving / 1000 + " sec\n");
            result[0] = SUCCESS;
            result[3] = patientsForDownloadedEncounters.size();
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
            patientController.deletePatientsPendingDeletion();
        } catch (FormController.UploadFormDataException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while uploading forms.", e);
            String exceptionError = e.getMessage();
            String[] exceptionErrorArray = exceptionError.split(":", 0);
            String uploadError = "";
            int i = 0;
            for (String error : exceptionErrorArray) {
                if (i == 0) {
                    uploadError = error.trim();
                }
                i++;
            }
            if (uploadError.equals("java.net.ConnectException")) {
                result[0] = SyncStatusConstants.SERVER_CONNECTION_ERROR;
            } else {
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

    public List<FormData> getArchivedFormData() {
        try {
            return formController.getArchivedFormData();
        } catch (FormController.FormDataFetchException e) {
            Log.e(TAG, "Could not fetch archived form data", e);
        }
        return new ArrayList<>();
    }

    public int[] checkAndDeleteTemporaryDataForProcessedFormData(List<FormData> archivedFormData) {
        int result[] = new int[5];
        try {
            List<FormData> successfullyProcessedFormData = new ArrayList<>();
            List<FormData> processedWithErrorFormData = new ArrayList<>();
            List<FormData> pendingProcessingFormData = new ArrayList<>();
            List<FormData> unknownStatusFormData = new ArrayList<>();

            for (FormData formData : archivedFormData) {
                FormDataStatus formDataStatus = formController.downloadFormDataStatus(formData);
                if (formDataStatus.isFormDataProcessedSuccessfully()) {
                    successfullyProcessedFormData.add(formData);
                    MuzimaLoggerService.log(muzimaApplication, "FORM_DATA_PROCESSED_SUCCESSFULLY",
                            "{\"formDataUuid\":\"" + formData.getUuid() + "\"}");
                } else if (formDataStatus.isFormDataProcessedWithError()) {
                    processedWithErrorFormData.add(formData);
                } else if (formDataStatus.isFormDataPendingProcessing()) {
                    pendingProcessingFormData.add(formData);
                } else {
                    unknownStatusFormData.add(formData);
                }
            }
            if (successfullyProcessedFormData.size() > 0) {
                formController.deleteFormDataAndRelatedEncountersAndObs(successfullyProcessedFormData);
            }
            result[0] = SUCCESS;
            result[1] = successfullyProcessedFormData.size();
            result[2] = processedWithErrorFormData.size();
            result[3] = pendingProcessingFormData.size();
            result[4] = unknownStatusFormData.size();

        } catch (FormController.FormDataStatusDownloadException e) {
            Log.e(TAG, "Could not download form data status", e);
            result[0] = DOWNLOAD_ERROR;
        } catch (FormController.FormDataDeleteException e) {
            result[0] = DELETE_ERROR;
            Log.e(TAG, "Could not delete archived form data", e);
        }
        return result;
    }

    public List<String> getUuidsForAllPatientsFromLocalStorage() {
        List<String> uuids = new ArrayList<>();
        try {
            List<Patient> patients = patientController.getAllPatients();
            uuids = getPatientUuids(patients);
        } catch (PatientController.PatientLoadException e) {
            Log.e(TAG, "Cannot retrieve patients from local storage", e);
        }
        return uuids;
    }

    public List<String> getUuidsForPatientsInCohorts(String[] savedCohortIds) {
        List<String> uuids = new ArrayList<>();
        try {
            List<Patient> patients = patientController.getPatientsForCohorts(savedCohortIds);
            uuids = getPatientUuids(patients);
        } catch (PatientController.PatientLoadException e) {
            Log.e(TAG, "Cannot retrieve patients from local storage", e);
        }
        return uuids;
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
        ArrayList<Patient> voidedPatients = new ArrayList<>();
        try {
            for (Patient patient : patientsNotInCohorts) {
                downloadedPatients.add(patientController.downloadPatientByUUID(patient.getUuid()));
            }
            downloadedPatients.removeAll(singleton(null));

            getVoidedPatients(voidedPatients, downloadedPatients);
            downloadedPatients.removeAll(voidedPatients);

            patientController.replacePatients(downloadedPatients);
            patientController.deletePatient(voidedPatients);
        } catch (PatientController.PatientSaveException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while updating patients from server.", e);
        } catch (PatientController.PatientDownloadException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while downloading patients from server.", e);
        } catch (PatientController.PatientDeleteException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while deleting voided patients.", e);
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
            cohorts = cohortController.downloadAllCohorts(getDefaultLocation());
        } else {
            cohorts = cohortController.downloadCohortsByPrefix(cohortPrefixes, getDefaultLocation());
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
        Log.e(getClass().getSimpleName(), "Downloading messages in MuzimaSyncService");
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

    public int[] downloadPatientReportHeaders(String patientUuid) {
        int[] result = new int[2];

        try {
            List<PatientReportHeader> patientReportHeaders;
            patientReportHeaders = patientReportController.downloadPatientReportHeadersByPatientUuid(patientUuid);
            patientReportController.savePatientReportHeaders(patientReportHeaders);

            result[0] = SUCCESS;
            result[1] = patientReportHeaders.size();

        } catch (PatientReportController.PatientReportDownloadException e) {
            Log.e(TAG, "Exception when trying to download patient reports headers", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
            return result;
        } catch (PatientReportController.PatientReportSaveException e) {
            Log.e(TAG, "Exception when trying to save patient reports headers", e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
            return result;
        }
        return result;
    }

    public int[] downloadAllPatientReportHeadersAndReports() {
        int[] result = new int[2];
        try {
            List<PatientReportHeader> patientReportHeaders = new ArrayList<>();
            List<Patient> patients = patientController.getAllPatients();
            List<String> patientlist = new ArrayList();
            List<PatientReport> downloadedPatientReports = new ArrayList<>();
            List<PatientReport> totalDownloadedPatientReports = new ArrayList<>();
            for (Patient patient : patients) {
                patientlist.add(patient.getUuid());
            }
            List<List<String>> slicedPatientUuids = split(patientlist);
            for (List<String> slicedPatientUuid : slicedPatientUuids) {
                patientReportHeaders = patientReportController.downloadPatientReportHeadersByPatientUuid(slicedPatientUuid);
                if (patientReportHeaders.size() > 0) {
                    patientReportController.savePatientReportHeaders(patientReportHeaders);
                    downloadedPatientReports = patientReportController.downloadPatientReportByUuid(patientReportHeaders);
                    totalDownloadedPatientReports.addAll(downloadedPatientReports);
                    patientReportController.saveOrUpdatePatientReports(downloadedPatientReports);
                }
            }
            result[0] = SUCCESS;
            result[1] = totalDownloadedPatientReports.size();

        } catch (PatientController.PatientLoadException e) {
            Log.e(TAG, "Encountered Patient Load Exception while getting patients", e);
        } catch (PatientReportController.PatientReportDownloadException e) {
            Log.e(TAG, "Encountered PatientReportDownloadException while downloading patient reports", e);
        } catch (PatientReportController.PatientReportSaveException e) {
            Log.e(TAG, "Encountered PatientReportSaveException while saving patient reports", e);
        }
        return result;
    }

    public int[] downloadReportDatasets(List<Integer> datasetDefinitionIds){
        int[] result = new int[2];
        try {
            List<ReportDataset> reportDatasets = reportDatasetController.downloadReportDatasets(datasetDefinitionIds);
            reportDatasetController.saveReportDatasets(reportDatasets);
            result[0] = SUCCESS;
            result[1] = reportDatasets.size();

        } catch (ReportDatasetController.ReportDatasetDownloadException | ReportDatasetController.ReportDatasetSaveException e) {
            Log.e(TAG, "Encountered Load Exception while getting report datasets", e);
        }

        return result;
    }


    public int[] downloadReportDatasetsForDownloadedReports(){
        int[] result = new int[2];
        List<Integer> datasetDefinitionIds = new ArrayList<>();
        try {
            List<ReportDataset> reportDatasets = reportDatasetController.getReportDatasets();
            if(reportDatasets != null && reportDatasets.size()>0){
                for(ReportDataset reportDataset : reportDatasets){
                    datasetDefinitionIds.add(reportDataset.getDatasetDefinitionId());
                }
                downloadReportDatasets(datasetDefinitionIds);
            }
        } catch (ReportDatasetController.ReportDatasetFetchException e) {
            Log.e(getClass().getSimpleName(), "Error while fetching report datasets"+e);
        }

        return result;
    }

        

    public int[] downloadPatientReportsByUuid(String[] reportUuids) {
        int[] result = new int[2];
        List<PatientReport> downloadedPatientReports = new ArrayList<>();

        try {
            for (String uuid : reportUuids) {
                downloadedPatientReports.add(patientReportController.downloadPatientReportByUuid(uuid));
            }

            patientReportController.saveOrUpdatePatientReports(downloadedPatientReports);
            result[0] = SUCCESS;
            result[1] = downloadedPatientReports.size();

        } catch (PatientReportController.PatientReportDownloadException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to download patient report");
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
        } catch (PatientReportController.PatientReportSaveException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to save patient reports");
            result[0] = SyncStatusConstants.SAVE_ERROR;
        }
        return result;
    }

    public void downloadSetupConfigurations() {
        int[] result = new int[2];
        try {
            List<SetupConfiguration> setupConfigurations = setupConfigurationController.downloadAllSetupConfigurations();
            result[0] = SUCCESS;
            result[1] = setupConfigurations.size();
            Log.i(getClass().getSimpleName(), "Setup Configs downloaded: " + setupConfigurations.size());
            //ToDo: Remove all retired
            setupConfigurationController.saveSetupConfigurations(setupConfigurations);
        } catch (SetupConfigurationController.SetupConfigurationDownloadException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to download setup configs");
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
        } catch (SetupConfigurationController.SetupConfigurationSaveException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to save setup configs");
            result[0] = SyncStatusConstants.SAVE_ERROR;
        }
    }

    public int[] downloadSetupConfigurationTemplate(String uuid) {
        int[] result = new int[2];
        try {
            SetupConfigurationTemplate setupConfigurationTemplate =
                    setupConfigurationController.downloadSetupConfigurationTemplate(uuid);
            result[0] = SUCCESS;
            if (setupConfigurationTemplate != null) {
                result[1] = 1;
                setupConfigurationController.saveSetupConfigurationTemplate(setupConfigurationTemplate);
            }
        } catch (SetupConfigurationController.SetupConfigurationDownloadException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to download setup configs");
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
        } catch (SetupConfigurationController.SetupConfigurationSaveException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to save setup configs");
            result[0] = SyncStatusConstants.SAVE_ERROR;
        }
        return result;
    }

    public int[] downloadAndSaveUpdatedSetupConfigurationTemplate(String uuid) {
        int[] result = new int[2];
        try {
            SetupConfigurationTemplate setupConfigurationTemplate =
                    setupConfigurationController.downloadUpdatedSetupConfigurationTemplate(uuid);
            result[0] = SUCCESS;
            if (setupConfigurationTemplate != null) {
                result[1] = 1;
                setupConfigurationController.updateSetupConfigurationTemplate(setupConfigurationTemplate);
            }

        } catch (SetupConfigurationController.SetupConfigurationDownloadException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to download setup configs");
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
        } catch (SetupConfigurationController.SetupConfigurationSaveException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to save setup configs");
            result[0] = SyncStatusConstants.SAVE_ERROR;
        }
        return result;
    }

    public int[] updateSetupConfigurationTemplates() {
        int[] result = new int[2];
        try {
            for (SetupConfigurationTemplate template : setupConfigurationController.getSetupConfigurationTemplates()) {
                int[] templateResult = downloadAndSaveUpdatedSetupConfigurationTemplate(template.getUuid());
                if (templateResult[0] == SUCCESS) {
                    result[1] += templateResult[1];
                    result[0] = SUCCESS;

                    List<MuzimaSetting> settings = settingsController.getSettingsFromSetupConfigurationTemplate(template.getUuid());

                    updateSettingsPreferences(settings);
                } else {
                    result[0] = templateResult[0];
                }
            }
        } catch (SetupConfigurationController.SetupConfigurationFetchException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to save setup configs");
            result[0] = SyncStatusConstants.LOAD_ERROR;
        } catch (MuzimaSettingController.MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to update setup settings preferences");
            result[0] = SyncStatusConstants.LOAD_ERROR;
        }
        return result;
    }

    public int[] downloadSetting(String property) {
        int[] result = new int[2];
        try {
            MuzimaSetting setting = settingsController.downloadSettingByProperty(property);
            result[0] = SUCCESS;
            if (setting != null) {
                result[1] = 1;
                settingsController.saveOrUpdateSetting(setting);
            }
        } catch (MuzimaSettingController.MuzimaSettingDownloadException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to download setting.", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
        } catch (MuzimaSettingController.MuzimaSettingSaveException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to save setting.", e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
        }
        return result;
    }

    public int[] downloadNewSettings() {

        int[] result = new int[2];
        try {
            List<MuzimaSetting> settings = settingsController.downloadChangedSettingsSinceLastSync();
            if (settings.size() > 0) {
                settingsController.saveOrUpdateSetting(settings);
                updateSettingsPreferences(settings);
            }
            result[0] = SUCCESS;
            result[1] = settings.size();
        } catch (MuzimaSettingController.MuzimaSettingDownloadException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to download setting", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
        } catch (MuzimaSettingController.MuzimaSettingSaveException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to save setting", e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
        }
        return result;
    }

    public void updateSettingsPreferences(List<MuzimaSetting> muzimaSettings) {
        for (MuzimaSetting muzimaSetting : muzimaSettings) {
            if (MuzimaSettingUtils.isGpsFeatureEnabledSetting(muzimaSetting)) {
                muzimaApplication.getGPSFeaturePreferenceService().updateGPSDataPreferenceSettings();
            } else if (MuzimaSettingUtils.isSHRFeatureEnabledSetting(muzimaSetting)) {
                new SHRStatusPreferenceService(muzimaApplication).updateSHRStatusPreference();
            } else if (MuzimaSettingUtils.isPatientIdentifierAutogenerationSetting(muzimaSetting)) {
                new RequireMedicalRecordNumberPreferenceService(muzimaApplication).updateRequireMedicalRecordNumberPreference();
            }
        }
    }

    public int[] downloadRelationshipsTypes() {
        if (!settingsController.isRelationshipEnabled())
            return null;

        int[] result = new int[3];
        try {
            Log.i(getClass().getSimpleName(), "Downloading relationships Types");
            List<RelationshipType> relationshipTypes = new ArrayList<>(relationshipController.downloadAllRelationshipTypes());
            Log.i(getClass().getSimpleName(), "Relationship Types download successful with " + relationshipTypes.size() + " types");
            result[1] += relationshipTypes.size();

            relationshipController.saveRelationshipTypes(relationshipTypes);
            result[0] = SUCCESS;
        } catch (RelationshipController.RetrieveRelationshipTypeException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while downloading relationship types.", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
        } catch (RelationshipController.SaveRelationshipTypeException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while saving relationship types.", e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
        }
        return result;
    }

    public void updatePatientTags(List<String> patientUuidList){
        List<PatientTag> existingTags = new ArrayList<>();

        try {
            existingTags = patientController.getAllTags();
        } catch (PatientController.PatientLoadException e) {
            e.printStackTrace();
        }

        for(String patientUuid:patientUuidList){
            try {
                Patient patient = patientController.getPatientByUuid(patientUuid);
                List<PatientTag> tags = new ArrayList<>();
                if(patient.getTags() != null) {
                    tags = new ArrayList<>(Arrays.asList(patient.getTags()));
                }

                PatientTag addressTag = null;
                PatientTag assignmentTag = null;
                PatientTag awaitingAssignmentTag = null;
                boolean hasSexualPartnerTag = false;
                boolean hasAssignmentTag = false;
                boolean hasAwaitingAssignmentTag = false;
                for (PatientTag tag : patient.getTags()) {
                    if(StringUtils.equals(tag.getUuid(),HAS_SEXUAL_PARTNER_TAG_UUID)) {
                        hasSexualPartnerTag = true;
                    } else if(StringUtils.equals(tag.getUuid(),ALREADY_ASSIGNED_TAG_UUID)) {
                        hasAssignmentTag = true;
                        assignmentTag = tag;
                    } else if(StringUtils.equals(tag.getUuid(),AWAITING_ASSIGNMENT_TAG_UUID)) {
                        hasAwaitingAssignmentTag = true;
                    }
                }

                //Create tag if patient has a sexual partner
                if(!hasSexualPartnerTag) {
                    List<Relationship> relationships = relationshipController.getRelationshipsForPerson(patientUuid);
                    for (Relationship relationship : relationships) {
                        if (StringUtils.equals(relationship.getRelationshipType().getUuid(), "2f7d5778-0c80-11eb-b335-9f16b42e3b00")) {
                            PatientTag sexualPartnerTag = new PatientTag();
                            sexualPartnerTag.setName("P");
                            sexualPartnerTag.setDescription(muzimaApplication.getString(R.string.general_has_sexual_partner));
                            sexualPartnerTag.setUuid(HAS_SEXUAL_PARTNER_TAG_UUID);
                            if(!hasSexualPartnerTag) {
                                hasSexualPartnerTag = true;
                                tags.add(sexualPartnerTag);
                                patientController.savePatientTags(sexualPartnerTag);
                            }

                            //update for the related patient as well
                            try {
                                Patient relatedPatient = null;
                                if (relationship.getPersonA() != null && !StringUtils.equals(patientUuid, relationship.getPersonA().getUuid())) {
                                    relatedPatient = patientController.getPatientByUuid(relationship.getPersonA().getUuid());
                                } else if (relationship.getPersonB() != null) {
                                    relatedPatient = patientController.getPatientByUuid(relationship.getPersonB().getUuid());
                                }

                                if(relatedPatient != null) {
                                    boolean relatedPatientHasSexualPartnerTag = false;
                                    for (PatientTag tag : relatedPatient.getTags()) {
                                        if(StringUtils.equals(tag.getUuid(),HAS_SEXUAL_PARTNER_TAG_UUID)) {
                                            relatedPatientHasSexualPartnerTag = true;
                                        }
                                    }
                                    if(!relatedPatientHasSexualPartnerTag){
                                        PatientTag relatedSexualPartnerTag = new PatientTag();
                                        relatedSexualPartnerTag.setName("P");
                                        relatedSexualPartnerTag.setDescription(muzimaApplication.getString(R.string.general_has_sexual_partner));
                                        relatedSexualPartnerTag.setUuid(HAS_SEXUAL_PARTNER_TAG_UUID);
                                        List<PatientTag> relatedPatientTags = new ArrayList<>(Arrays.asList(relatedPatient.getTags()));


                                        relatedPatientTags.add(relatedSexualPartnerTag);
                                        patientController.savePatientTags(relatedSexualPartnerTag);

                                        relatedPatient.setTags(relatedPatientTags.toArray(new PatientTag[relatedPatientTags.size()]));
                                        patientController.updatePatient(relatedPatient);
                                    }
                                }
                            } catch (PatientController.PatientLoadException e){
                                Log.e(getClass().getSimpleName(),"Could not update related patient",e);
                            }
                        }
                    }
                }

                //Create tag if the patient has address field for Bairro.
                List<String> tagNames = new ArrayList<>();

                for(PatientTag tag:existingTags){
                    tagNames.add(tag.getName());
                }

                PersonAddress personAddress = patient.getPreferredAddress();
                String address5 = null;

                if(personAddress != null){
                    address5 = personAddress.getAddress5();
                }

                if(personAddress == null){
                    for(PersonAddress address:patient.getAddresses()){
                        if(!StringUtils.isEmpty(address.getAddress5())) {
                            address5 = address.getAddress5();
                            break;
                        }
                    }
                }

                if(!StringUtils.isEmpty(address5)){
                    String addressTagName = null;
                    if(address5.length() > 3) {
                        addressTagName = address5.substring(0, 3);
                    } else {
                        addressTagName = address5;
                    }

                    for(PatientTag existingTag : existingTags){
                        if(StringUtils.equals(existingTag.getName(),addressTagName)){
                            addressTag = existingTag;
                        }
                    }

                    if(addressTag == null) {
                        addressTag = new PatientTag();
                        addressTag.setName(addressTagName);
                        addressTag.setDescription(address5);
                        addressTag.setUuid(UUID.randomUUID().toString());
                        existingTags.add(addressTag);
                        patientController.savePatientTags(addressTag);
                    }

                    tags.add(addressTag);
                }

                if(!hasAssignmentTag) {
                    List<Observation> assignmentObsList = observationController.getObservationsByPatientuuidAndConceptId(patientUuid, HEALTHWORKER_ASSIGNMENT_CONCEPT_ID);
                    List<Observation> consentObsList = observationController.getObservationsByPatientuuidAndConceptId(patientUuid, INDEX_CASE_TESTING_CONSENT_CONCEPT_ID);

                    if (consentObsList.size() > 0 && assignmentObsList.size() > 0) {
                        for (Observation consentObs : consentObsList) {
                            Date now = new Date();
                            long consentDaysPassed = (now.getTime() - consentObs.getObservationDatetime().getTime()) / (24 * 60 * 60 * 1000);
                            if (consentDaysPassed >= 0 && consentDaysPassed <= 30) {
                                for (Observation assignmentObs : assignmentObsList) {
                                    if (assignmentObs.getObservationDatetime().after(consentObs.getObservationDatetime())) {
                                        assignmentTag = new PatientTag();
                                        assignmentTag.setName("AL");
                                        assignmentTag.setDescription(muzimaApplication.getString(R.string.general_already_assigned));
                                        assignmentTag.setUuid(ALREADY_ASSIGNED_TAG_UUID);
                                        tags.add(assignmentTag);
                                        patientController.savePatientTags(assignmentTag);
                                        break;

                                    }
                                }
                            }
                            if (assignmentTag != null) {
                                break;
                            }
                        }
                    }
                }

                if(!hasAwaitingAssignmentTag && assignmentTag == null){
                    assignmentTag = new PatientTag();
                    assignmentTag.setName("AA");
                    assignmentTag.setDescription(muzimaApplication.getString(R.string.general_awaiting_assignment));
                    assignmentTag.setUuid(AWAITING_ASSIGNMENT_TAG_UUID);
                    tags.add(assignmentTag);
                    patientController.savePatientTags(assignmentTag);
                }

                patient.setTags(tags.toArray(new PatientTag[tags.size()]));
                patientController.updatePatient(patient);
            } catch (RelationshipController.RetrieveRelationshipException e) {
                Log.e(getClass().getSimpleName(),"Error retrieving relationships", e);
            } catch (PatientController.PatientSaveException e) {
                Log.e(getClass().getSimpleName(), "Could not save patient with updated tags", e);
            } catch (IOException e) {
                Log.e(getClass().getSimpleName(), "Could not load recordqs", e);
            } catch (PatientController.PatientLoadException e) {
                Log.e(getClass().getSimpleName(), "Could not load patient record to update update tags", e);
            } catch (ObservationController.LoadObservationException e) {
                Log.e(getClass().getSimpleName(), "Could not load observations to create tags tags", e);
            }
        }
    }

    public int[] downloadRelationshipsForPatientsByPatientUUIDs(List<String> patientUuids) {
        if (!settingsController.isRelationshipEnabled())
            return null;

        int[] result = new int[3];
        result[2] = patientUuids.size();
        try {
            Log.i(getClass().getSimpleName(), "Downloading relationships for " + patientUuids.size() + " patients");
            for (String patientUuid : patientUuids) {
                Log.i(getClass().getSimpleName(), "Downloading relationships for " + patientUuid);
                long startDownloadRelationships = System.currentTimeMillis();
                List<Relationship> patientRelationships = new ArrayList<>(relationshipController.downloadRelationshipsForPerson(patientUuid));

                long endDownloadRelationships = System.currentTimeMillis();
                Log.d(getClass().getSimpleName(), "In Downloading relationships : " + (endDownloadRelationships - startDownloadRelationships) / 1000 + " sec\n");

                Log.i(getClass().getSimpleName(), "Relationships download successful with " + patientRelationships.size() + " relationships");
                result[1] += patientRelationships.size();

                relationshipController.saveRelationships(patientRelationships, patientUuid);
            }
            result[0] = SUCCESS;
        } catch (RelationshipController.RetrieveRelationshipException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while downloading relationships.", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
        } catch (RelationshipController.SaveRelationshipException | RelationshipController.SearchRelationshipException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while saving relationships.", e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
        }
        return result;
    }

    public int[] downloadRelationshipsForPatientsByCohortUUIDs(String[] cohortUuids) {
        if (!settingsController.isRelationshipEnabled())
            return null;

        int[] result = new int[3];
        List<Patient> patients;
        try {
            patients = patientController.getPatientsForCohorts(cohortUuids);
            int patientsTotal = patients.size();

            int count = 0;
            List<String> patientList = new ArrayList();
            for (Patient patient : patients) {
                count++;
                Log.i(getClass().getSimpleName(), "Downloading relationships for patient " + count + " of " + patientsTotal);
                updateProgressDialog(muzimaApplication.getString(R.string.info_relationships_download_progress, count, patientsTotal));
                patientList.add(patient.getUuid());
            }
            result = downloadRelationshipsForPatientsByPatientUUIDs(patientList);
            if (result[0] != SUCCESS) {
                Log.e(getClass().getSimpleName(), "Relationships for patient " + count + " of " + patientsTotal + " not downloaded");
                updateProgressDialog(muzimaApplication.getString(R.string.info_relationships_not_downloaded_progress, count, patientsTotal));

            }
        } catch (PatientController.PatientLoadException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while downloading relationships for patients.", e);
            result[0] = SyncStatusConstants.LOAD_ERROR;
        }
        return result;
    }

    public String getDefaultLocation() {
        android.content.Context context = muzimaApplication.getApplicationContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString("defaultEncounterLocation", null);
    }
}
