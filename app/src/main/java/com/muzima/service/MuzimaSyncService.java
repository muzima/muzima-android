/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.service;

import static android.content.Context.DOWNLOAD_SERVICE;

import android.app.DownloadManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.context.Context;
import com.muzima.api.exception.AuthenticationException;
import com.muzima.api.model.AppUsageLogs;
import com.muzima.api.model.Cohort;
import com.muzima.api.model.CohortData;
import com.muzima.api.model.CohortMember;
import com.muzima.api.model.Concept;
import com.muzima.api.model.DerivedConcept;
import com.muzima.api.model.DerivedObservation;
import com.muzima.api.model.Form;
import com.muzima.api.model.FormData;
import com.muzima.api.model.FormDataStatus;
import com.muzima.api.model.FormTemplate;
import com.muzima.api.model.Location;
import com.muzima.api.model.Media;
import com.muzima.api.model.MediaCategory;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PatientReport;
import com.muzima.api.model.PatientReportHeader;
import com.muzima.api.model.PatientTag;
import com.muzima.api.model.Person;
import com.muzima.api.model.PersonAddress;
import com.muzima.api.model.PersonTag;
import com.muzima.api.model.Provider;
import com.muzima.api.model.MuzimaSetting;
import com.muzima.api.model.Relationship;
import com.muzima.api.model.RelationshipType;
import com.muzima.api.model.ReportDataset;
import com.muzima.api.model.SetupConfiguration;
import com.muzima.api.model.SetupConfigurationTemplate;
import com.muzima.controller.AppUsageLogsController;
import com.muzima.controller.CohortController;
import com.muzima.controller.ConceptController;
import com.muzima.controller.DerivedConceptController;
import com.muzima.controller.DerivedObservationController;
import com.muzima.controller.FormController;
import com.muzima.controller.LocationController;
import com.muzima.controller.MediaCategoryController;
import com.muzima.controller.MediaController;
import com.muzima.controller.PatientReportController;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.PatientController;
import com.muzima.controller.PersonController;
import com.muzima.controller.ProviderController;
import com.muzima.controller.RelationshipController;
import com.muzima.controller.ReportDatasetController;
import com.muzima.controller.SetupConfigurationController;
import com.muzima.model.CompleteFormWithPatientData;
import com.muzima.model.IncompleteFormWithPatientData;
import com.muzima.model.collections.CompleteFormsWithPatientData;
import com.muzima.model.collections.IncompleteFormsWithPatientData;
import com.muzima.util.JsonUtils;
import com.muzima.util.MuzimaSettingUtils;
import com.muzima.utils.Constants;
import com.muzima.utils.MemoryUtil;
import com.muzima.utils.NetworkUtils;
import com.muzima.utils.RelationshipViewUtil;
import com.muzima.utils.StringUtils;
import com.muzima.view.forms.SyncFormTemplateIntent;
import com.muzima.view.patients.SyncPatientDataIntent;
import com.muzima.view.progressdialog.ProgressDialogUpdateIntentService;

import org.apache.lucene.queryParser.ParseException;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.DELETE_ERROR;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.DOWNLOAD_ERROR;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.LOAD_ERROR;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.SAVE_ERROR;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.UNKNOWN_ERROR;
import static com.muzima.utils.Constants.FGH.Concepts.NO_INTERVENTION_NEEDED_ANSWER_CONCEPT_ID;
import static com.muzima.utils.Constants.FGH.Concepts.NO_INTERVENTION_NEEDED_QUESTION_CONCEPT_ID;
import static com.muzima.utils.Constants.FGH.DerivedConcepts.CONTACTS_TESTED_DERIVED_CONCEPT_ID;
import static com.muzima.utils.Constants.FGH.TagsUuids.ALL_CONTACTS_VISITED_TAG_UUID;
import static com.muzima.utils.Constants.FGH.TagsUuids.NAO_TAG_UUID;
import static com.muzima.utils.Constants.FGH.TagsUuids.NOT_ALL_CONTACTS_VISITED_TAG_UUID;
import static com.muzima.utils.Constants.FGH.TagsUuids.NO_INTERVENTION_NEEDED_UUID;
import static com.muzima.utils.Constants.FGH.TagsUuids.SIM_TAG_UUID;
import static com.muzima.utils.Constants.LOCAL_PATIENT;
import static java.util.Collections.singleton;

import static com.muzima.utils.Constants.FGH.Concepts.HEALTHWORKER_ASSIGNMENT_CONCEPT_ID;
import static com.muzima.utils.Constants.FGH.TagsUuids.ALREADY_ASSIGNED_TAG_UUID;
import static com.muzima.utils.Constants.FGH.TagsUuids.AWAITING_ASSIGNMENT_TAG_UUID;
import static com.muzima.utils.Constants.FGH.TagsUuids.HAS_SEXUAL_PARTNER_TAG_UUID;
import static com.muzima.utils.Constants.STANDARD_DATE_TIMEZONE_FORMAT;
import static com.muzima.utils.DeviceDetailsUtil.generatePseudoDeviceId;

public class MuzimaSyncService {
    private static final String TAG = "MuzimaSyncService";
    private final MuzimaApplication muzimaApplication;
    private final FormController formController;
    private final ConceptController conceptController;
    private final CohortController cohortController;
    private final PatientController patientController;
    private final ObservationController observationController;
    private LocationController locationController;
    private ProviderController providerController;
    private SetupConfigurationController setupConfigurationController;
    private MuzimaSettingController settingsController;
    private PatientReportController patientReportController;
    private RelationshipController relationshipController;
    private PersonController personController;
    private ReportDatasetController reportDatasetController;
    private MediaController mediaController;
    private MediaCategoryController mediaCategoryController;
    private AppUsageLogsController appUsageLogsController;
    private DerivedConceptController derivedConceptController;
    private DerivedObservationController derivedObservationController;
    private Logger logger;
    private String pseudoDeviceId;

    public MuzimaSyncService(MuzimaApplication muzimaContext) {
        this.muzimaApplication = muzimaContext;
        formController = muzimaApplication.getFormController();
        conceptController = muzimaApplication.getConceptController();
        cohortController = muzimaApplication.getCohortController();
        patientController = muzimaApplication.getPatientController();
        observationController = muzimaApplication.getObservationController();
        locationController = muzimaApplication.getLocationController();
        providerController = muzimaApplication.getProviderController();
        setupConfigurationController = muzimaApplication.getSetupConfigurationController();
        settingsController = muzimaApplication.getMuzimaSettingController();
        patientReportController = muzimaApplication.getPatientReportController();
        relationshipController = muzimaApplication.getRelationshipController();
        personController = muzimaApplication.getPersonController();
        reportDatasetController = muzimaApplication.getReportDatasetController();
        mediaController = muzimaApplication.getMediaController();
        mediaCategoryController = muzimaApplication.getMediaCategoryController();
        appUsageLogsController = muzimaApplication.getAppUsageLogsController();
        derivedConceptController = muzimaApplication.getDerivedConceptController();
        derivedObservationController = muzimaApplication.getDerivedObservationController();
        pseudoDeviceId = generatePseudoDeviceId();
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
            Log.e(getClass().getSimpleName(),"Encounter an exception while fetching concepts",e);
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
            List<Patient> cohortPatients = new ArrayList<>();
            List<Patient> downloadedPatients = new ArrayList<>();
            for (CohortData cohortData : cohortDataList) {
                cohortController.addCohortMembers(cohortData.getCohortMembers());
                cohortPatients = cohortData.getPatients();
                getVoidedPatients(voidedPatients, cohortPatients);
                cohortPatients.removeAll(voidedPatients);
                patientController.replacePatients(cohortPatients);
                patientCount += cohortData.getPatients().size();
                if(cohortPatients.size() > 0) {
                    downloadedPatients.addAll(cohortPatients);
                }
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

            if(cohortUuids.length > 0) {
                downloadRelationshipsForPatientsByCohortUUIDs(cohortUuids);
            }
            MuzimaSettingController muzimaSettingController = muzimaApplication.getMuzimaSettingController();
            if(muzimaSettingController.isPatientTagGenerationEnabled()) {
                List<String> patientUuids = new ArrayList<>();
                if(downloadedPatients.size()>0) {
                    for (Patient patient : downloadedPatients) {
                        patientUuids.add(patient.getUuid());
                    }
                }
                if(patientUuids.size()>0){
                    updatePatientTags(patientUuids);
                }
            }

            //update memberships
            downloadRemovedCohortMembershipData(cohortUuids);

            cohortController.markAsUpToDate(cohortUuids);
            cohortController.setSyncStatus(cohortUuids,1);
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
            result[0] = SAVE_ERROR;
        }
        return result;
    }

    public int[] downloadObservationsForPatientsByCohortUUIDs(String[] cohortUuids, boolean replaceExistingObservation) {
        int[] result = new int[4];
        List<Patient> patients;
        try {
            patients = patientController.getPatientsForCohorts(cohortUuids);

            List<String> patientlist = new ArrayList();
            patientlist = getPatientUuids(patients);

            result = downloadObservationsForPatientsByPatientUUIDs(patientlist, replaceExistingObservation);
            if (result[0] != SUCCESS) {
                updateProgressDialog(muzimaApplication.getString(R.string.error_encounter_observation_download));

            }
            MuzimaSettingController muzimaSettingController = muzimaApplication.getMuzimaSettingController();
            if(muzimaSettingController.isPatientTagGenerationEnabled()) {
                if (patientlist.size() > 0) {
                    updatePatientTags(patientlist);
                }
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

    public int[] downloadDerivedObservationsForAllPersons(boolean replaceExistingObservation) {
        int[] result = new int[4];
        List<Person> persons;
        try {
            persons = personController.getAllPersons();

            List<String> personUuidList = new ArrayList();
            for (Person person: persons) {
                personUuidList.add(person.getUuid());
            }
            result = downloadDerivedObservationsForPatientsByPatientUUIDs(personUuidList, replaceExistingObservation);
            List<Patient> patients = patientController.getAllPatients();

            List<String> patientlist = new ArrayList();
            patientlist = getPatientUuids(patients);
            if(patientlist.size()>0) {
                updatePatientTags(patientlist);
            }

            if (result[0] != SUCCESS) {
                updateProgressDialog(muzimaApplication.getString(R.string.error_derived_observation_download));
            }
        } catch (PersonController.PersonLoadException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while loading persons.", e);
            result[0] = SyncStatusConstants.LOAD_ERROR;
        } catch (PatientController.PatientLoadException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while loading patients.", e);
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

            for (List<String> slicedPatientUuid : slicedPatientUuids) {
                for (List<String> slicedConceptUuid : slicedConceptUuids) {
                    long startDownloadObservations = System.currentTimeMillis();

                    List<Observation> allObservations = new ArrayList<>(observationController.downloadObservationsByPatientUuidsAndConceptUuids(
                            slicedPatientUuid, slicedConceptUuid, activeSetupConfigUuid));

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

    public int[] uploadAllCompletedForms() {
        int[] result = new int[1];
        try {
            result[0] = formController.uploadAllCompletedForms() ? SUCCESS : SyncStatusConstants.UPLOAD_ERROR;
            patientController.deletePatientsPendingDeletion();
            try {
                SimpleDateFormat simpleDateTimezoneFormat = new SimpleDateFormat(STANDARD_DATE_TIMEZONE_FORMAT);
                AppUsageLogs lastUploadLog = appUsageLogsController.getAppUsageLogByKeyAndUserName(com.muzima.util.Constants.AppUsageLogs.LAST_UPLOAD_TIME, muzimaApplication.getAuthenticatedUserId());
                if (lastUploadLog != null) {
                    lastUploadLog.setLogvalue(simpleDateTimezoneFormat.format(new Date()));
                    lastUploadLog.setUpdateDatetime(new Date());
                    lastUploadLog.setUserName(muzimaApplication.getAuthenticatedUserId());
                    lastUploadLog.setDeviceId(pseudoDeviceId);
                    lastUploadLog.setLogSynced(false);
                    appUsageLogsController.saveOrUpdateAppUsageLog(lastUploadLog);
                } else {
                    AppUsageLogs newUploadTime = new AppUsageLogs();
                    newUploadTime.setUuid(UUID.randomUUID().toString());
                    newUploadTime.setLogKey(com.muzima.util.Constants.AppUsageLogs.LAST_UPLOAD_TIME);
                    newUploadTime.setLogvalue(simpleDateTimezoneFormat.format(new Date()));
                    newUploadTime.setUpdateDatetime(new Date());
                    newUploadTime.setUserName(muzimaApplication.getAuthenticatedUserId());
                    newUploadTime.setDeviceId(pseudoDeviceId);
                    newUploadTime.setLogSynced(false);
                    appUsageLogsController.saveOrUpdateAppUsageLog(newUploadTime);
                }
            } catch (IOException e) {
                Log.e(getClass().getSimpleName(),"Encountered IO Exception ",e);
            } catch (ParseException e) {
                Log.e(getClass().getSimpleName(),"Encountered Parse Exception ",e);
            }
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
            Log.e(getClass().getSimpleName(),"Encounter an exception while fetching form data",e);
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
            result[0] = DOWNLOAD_ERROR;
        } catch (FormController.FormDataDeleteException e) {
            result[0] = DELETE_ERROR;
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
        List<Cohort> cohorts;
        cohorts = cohortController.downloadAllCohorts(getDefaultLocation());
        Log.e(TAG, "downloadCohortsList: downloaded cohorts " + cohorts.size());
        return cohorts;
    }

    List<String> getPatientUuids(List<Patient> patients) {
        List<String> patientUuids = new ArrayList<>();
        for (Patient patient : patients) {
            if(!patientUuids.contains(patient.getUuid())) {
                patientUuids.add(patient.getUuid());
            }
        }
        return patientUuids;
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
            Log.e(getClass().getSimpleName(), "Exception when trying to download setup config",e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
        } catch (SetupConfigurationController.SetupConfigurationSaveException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to save setup config",e);
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
            if (MuzimaSettingUtils.isOnlineOnlyModeSetting(muzimaSetting)) {
                new OnlineOnlyModePreferenceService(muzimaApplication).updateOnlineOnlyModePreferenceValue();
            }else if(MuzimaSettingUtils.isDuplicateFormCheckSetting(muzimaSetting)){
                new FormDuplicateCheckPreferenceService(muzimaApplication).updateFormDuplicateCheckPreferenceSettings();
            }else if(MuzimaSettingUtils.isConfidentialityNoticeDisplaySetting(muzimaSetting)){
                new ConfidentialityNoticeDisplayPreferenceService(muzimaApplication).updateConfidentialityNoticeDisplayPreferenceValue();
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

    public int[] downloadRelationshipsForPatientsByPatientUUIDs(List<String> patientUuids) {
        if (!settingsController.isRelationshipEnabled())
            return null;

        int[] result = new int[3];
        result[2] = patientUuids.size();
        try {
            String activeSetupConfigUuid = null;
            SetupConfigurationTemplate setupConfigurationTemplate = setupConfigurationController.getActiveSetupConfigurationTemplate();
            if (setupConfigurationTemplate != null) {
                activeSetupConfigUuid = setupConfigurationTemplate.getUuid();
            }
            Log.i(getClass().getSimpleName(), "Downloading relationships for " + patientUuids.size() + " patients");
            List<List<String>> slicedPatientUuids = split(patientUuids);
            for(List<String> slice : slicedPatientUuids){
                List<Relationship> patientRelationships = relationshipController.downloadRelationshipsForPatients(slice, activeSetupConfigUuid);
                relationshipController.saveRelationships(patientRelationships);
                result[1] += patientRelationships.size();
            }
            result[0] = SUCCESS;
        } catch (RelationshipController.RetrieveRelationshipException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while downloading relationships.", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
        } catch (RelationshipController.SaveRelationshipException | RelationshipController.SearchRelationshipException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while saving relationships.", e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
        } catch (SetupConfigurationController.SetupConfigurationFetchException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while fetching the active setup config.", e);
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
                if(!patientList.contains(patient.getUuid())) {
                    patientList.add(patient.getUuid());
                }
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

    public void updatePatientTags(List<String> patientUuidList){
        Log.e(getClass().getSimpleName(),"Generating Patient Tags");
        List<PatientTag> existingTags = new ArrayList<>();

        try {
            existingTags = patientController.getAllTags();
        } catch (PatientController.PatientLoadException e) {
            Log.e(getClass().getSimpleName(),"Encounter an exception loading patients",e);
        }

        for(String patientUuid:patientUuidList){
            try {
                Patient patient = patientController.getPatientByUuid(patientUuid);
                List<CohortMember> cohortMembers = muzimaApplication.getCohortController().getCohortMembershipByPatientUuid(patientUuid);
                if(patient != null) {
                    List<PatientTag> tags = new ArrayList<>();
                    if (patient.getTags() != null) {
                        tags = new ArrayList<>(Arrays.asList(patient.getTags()));
                    }

                    PatientTag addressTag = null;
                    PatientTag assignmentTag = null;
                    PatientTag noInterventionTag = null;
                    boolean hasSexualPartnerTag = false;
                    boolean hasAssignmentTag = false;
                    boolean hasAwaitingAssignmentTag = false;
                    boolean hasNoInterventionNeededTag = false;
                    boolean hasAllContactsVisitedTag = false;
                    boolean hasHomeVisitTags = false;
                    boolean hasNotAllContactsVisitedTag = false;
                    int homeVisitTagCount = 0;
                    for (PatientTag tag : patient.getTags()) {
                        if (StringUtils.equals(tag.getUuid(), HAS_SEXUAL_PARTNER_TAG_UUID)) {
                            hasSexualPartnerTag = true;
                        } else if (StringUtils.equals(tag.getUuid(), ALREADY_ASSIGNED_TAG_UUID)) {
                            hasAssignmentTag = true;
                            assignmentTag = tag;
                        } else if (StringUtils.equals(tag.getUuid(), AWAITING_ASSIGNMENT_TAG_UUID)) {
                            hasAwaitingAssignmentTag = true;
                        } else if (StringUtils.equals(tag.getUuid(), ALL_CONTACTS_VISITED_TAG_UUID)) {
                            hasAllContactsVisitedTag = true;
                        } else if(StringUtils.equals(tag.getUuid(), SIM_TAG_UUID) || StringUtils.equals(tag.getUuid(), NAO_TAG_UUID)){
                            hasHomeVisitTags = true;
                            homeVisitTagCount++;
                        }else if(StringUtils.equals(tag.getUuid(), NOT_ALL_CONTACTS_VISITED_TAG_UUID)){
                            hasNotAllContactsVisitedTag = true;
                        }else if(StringUtils.equals(tag.getUuid(), NO_INTERVENTION_NEEDED_UUID)){
                            hasNoInterventionNeededTag = true;
                        }
                    }

                    //Create tag if patient has a sexual partner
                    if (!hasSexualPartnerTag) {
                        List<Relationship> relationships = relationshipController.getRelationshipsForPerson(patientUuid);
                        for (Relationship relationship : relationships) {
                            if (StringUtils.equals(relationship.getRelationshipType().getUuid(), "2f7d5778-0c80-11eb-b335-9f16b42e3b00")) {
                                PatientTag sexualPartnerTag = new PatientTag();
                                sexualPartnerTag.setName("P");
                                sexualPartnerTag.setDescription(muzimaApplication.getString(R.string.general_has_sexual_partner));
                                sexualPartnerTag.setUuid(HAS_SEXUAL_PARTNER_TAG_UUID);
                                if (!hasSexualPartnerTag) {
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

                                    if (relatedPatient != null) {
                                        boolean relatedPatientHasSexualPartnerTag = false;
                                        for (PatientTag tag : relatedPatient.getTags()) {
                                            if (StringUtils.equals(tag.getUuid(), HAS_SEXUAL_PARTNER_TAG_UUID)) {
                                                relatedPatientHasSexualPartnerTag = true;
                                            }
                                        }
                                        if (!relatedPatientHasSexualPartnerTag) {
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
                                } catch (PatientController.PatientLoadException e) {
                                    Log.e(getClass().getSimpleName(), "Could not update related patient", e);
                                }
                            }
                        }
                    }

                    //Create tag if the patient has address field for Bairro.
                    List<String> tagNames = new ArrayList<>();

                    for (PatientTag tag : existingTags) {
                        tagNames.add(tag.getName());
                    }

                    PersonAddress personAddress = patient.getPreferredAddress();
                    String address5 = null;

                    if (personAddress != null) {
                        address5 = personAddress.getAddress5();
                    }

                    if (personAddress == null) {
                        for (PersonAddress address : patient.getAddresses()) {
                            if (!StringUtils.isEmpty(address.getAddress5())) {
                                address5 = address.getAddress5();
                                break;
                            }
                        }
                    }

                    if (!StringUtils.isEmpty(address5)) {
                        String addressTagName = null;
                        if (address5.length() > 3) {
                            addressTagName = address5.substring(0, 3);
                        } else {
                            addressTagName = address5;
                        }

                        for (PatientTag existingTag : existingTags) {
                            if (StringUtils.equals(existingTag.getName(), addressTagName)) {
                                addressTag = existingTag;
                            }
                        }

                        if (addressTag == null) {
                            addressTag = new PatientTag();
                            addressTag.setName(addressTagName);
                            addressTag.setDescription(address5);
                            addressTag.setUuid(UUID.randomUUID().toString());
                            existingTags.add(addressTag);
                            patientController.savePatientTags(addressTag);

                            tags.add(addressTag);
                        }
                    }

                    if (muzimaApplication.getMuzimaSettingController().isAllocationTagGenerationEnabled()) {
                        if (!hasAssignmentTag) {
                            List<Observation> assignmentObsList = observationController.getObservationsByPatientuuidAndConceptId(patientUuid, HEALTHWORKER_ASSIGNMENT_CONCEPT_ID);
                            if (assignmentObsList.size() > 0) {
                                for (Observation assignmentObs : assignmentObsList) {
                                    if(cohortMembers.size()>0) {
                                        CohortMember cohortMember = cohortMembers.get(0);
                                        Date membershipDate = cohortMember.getMembershipDate();
                                        if (assignmentObs.getObservationDatetime().after(membershipDate)) {
                                            assignmentTag = new PatientTag();
                                            assignmentTag.setName("AL");
                                            assignmentTag.setDescription(muzimaApplication.getString(R.string.general_already_assigned));
                                            assignmentTag.setUuid(ALREADY_ASSIGNED_TAG_UUID);
                                            tags.add(assignmentTag);
                                            patientController.savePatientTags(assignmentTag);

                                            //remove AA tag if available
                                            PatientTag AATag = null;
                                            for (PatientTag patientTag : tags) {
                                                if (patientTag.getName().equals("AA")) {
                                                    AATag = patientTag;
                                                }
                                            }

                                            if (AATag != null) {
                                                tags.remove(AATag);
                                            }

                                            hasAssignmentTag = true;
                                            break;
                                        }
                                    }
                                    if (assignmentTag != null) {
                                        hasAssignmentTag = true;
                                        break;
                                    }
                                }
                            }
                        }

                        if(!hasNoInterventionNeededTag){
                            List<Observation> obsList = observationController.getObservationsByPatientuuidAndConceptId(patientUuid, NO_INTERVENTION_NEEDED_QUESTION_CONCEPT_ID);
                            if (obsList.size() > 0) {
                                for (Observation obs : obsList) {
                                    if(cohortMembers.size()>0) {
                                        CohortMember cohortMember = cohortMembers.get(0);
                                        Date membershipDate = cohortMember.getMembershipDate();
                                        if (obs.getObservationDatetime().after(membershipDate)) {
                                            if (obs.getValueCoded().getId() == NO_INTERVENTION_NEEDED_ANSWER_CONCEPT_ID) {

                                                noInterventionTag = new PatientTag();
                                                noInterventionTag.setName("NA");
                                                noInterventionTag.setDescription(muzimaApplication.getString(R.string.general_no_intervention_needed));
                                                noInterventionTag.setUuid(NO_INTERVENTION_NEEDED_UUID);
                                                tags.add(noInterventionTag);
                                                patientController.savePatientTags(noInterventionTag);

                                                //remove AA tag if available
                                                PatientTag AATag = null;
                                                for (PatientTag patientTag : tags) {
                                                    if (patientTag.getName().equals("AA")) {
                                                        AATag = patientTag;
                                                    }
                                                }

                                                if (AATag != null) {
                                                    tags.remove(AATag);
                                                }

                                                hasNoInterventionNeededTag = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (noInterventionTag != null) {
                                        hasNoInterventionNeededTag = true;
                                        break;
                                    }
                                }
                            }
                        }

                        if (!hasAssignmentTag && !hasAwaitingAssignmentTag && assignmentTag == null && !hasNoInterventionNeededTag) {
                            assignmentTag = new PatientTag();
                            assignmentTag.setName("AA");
                            assignmentTag.setDescription(muzimaApplication.getString(R.string.general_awaiting_assignment));
                            assignmentTag.setUuid(AWAITING_ASSIGNMENT_TAG_UUID);
                            tags.add(assignmentTag);
                            patientController.savePatientTags(assignmentTag);
                        }
                    }

                    if (!hasAllContactsVisitedTag) {
                        List<Person> relatedPersons = RelationshipViewUtil.getDisplayableRelatedPersonsList(patientUuid, muzimaApplication);
                        if (relatedPersons != null) {
                            int contactsVisited = 0;
                            if(cohortMembers.size()>0) {
                                CohortMember cohortMember = cohortMembers.get(0);
                                Date membershipDate = cohortMember.getMembershipDate();
                                for (Person person : relatedPersons) {
                                    Boolean isDerivedConceptAfterMembershipDate = derivedObservationController.getDerivedObservationsByPatientUuidAndAfterIndexCaseMembershipDate(person.getUuid(), membershipDate, CONTACTS_TESTED_DERIVED_CONCEPT_ID);
                                    if(isDerivedConceptAfterMembershipDate){
                                        contactsVisited++;
                                    }
                                }
                            }

                            if (contactsVisited >= relatedPersons.size() && relatedPersons.size()>0) {
                                //Remove NV patient tag to be replaced by the V tag
                                PatientTag NVTag = null;
                                for (PatientTag patientTag : tags) {
                                    if (patientTag.getName().equals("NV")) {
                                        NVTag = patientTag;
                                    }
                                }

                                if (NVTag != null) {
                                    tags.remove(NVTag);
                                }

                                //Add V tag
                                PatientTag allContactsVisitedTag = new PatientTag();
                                allContactsVisitedTag.setName("V");
                                allContactsVisitedTag.setDescription(muzimaApplication.getString(R.string.general_all_contacts_visited));
                                allContactsVisitedTag.setUuid(ALL_CONTACTS_VISITED_TAG_UUID);
                                tags.add(allContactsVisitedTag);

                                patientController.savePatientTags(allContactsVisitedTag);
                            }else if(relatedPersons.size()>0 && !hasAllContactsVisitedTag && !hasNotAllContactsVisitedTag){
                                PatientTag notAllContactsVisitedTag = new PatientTag();
                                notAllContactsVisitedTag.setName("NV");
                                notAllContactsVisitedTag.setDescription(muzimaApplication.getString(R.string.general_not_all_contacts_visited));
                                notAllContactsVisitedTag.setUuid(NOT_ALL_CONTACTS_VISITED_TAG_UUID);
                                tags.add(notAllContactsVisitedTag);
                                patientController.savePatientTags(notAllContactsVisitedTag);
                            }
                        }
                    }

                    if(!hasHomeVisitTags || homeVisitTagCount<3){
                        if(cohortMembers.size()>0) {
                            CohortMember cohortMember = cohortMembers.get(0);
                            Date membershipDate = cohortMember.getMembershipDate();

                            List<Observation> firstAttemptObservations = observationController.getObservationsByPatientuuidAndConceptId(patientUuid,24008);
                            Collections.sort(firstAttemptObservations, observationDateTimeComparator);

                            List<Observation> secondAttemptObservations = observationController.getObservationsByPatientuuidAndConceptId(patientUuid,24009);
                            Collections.sort(secondAttemptObservations, observationDateTimeComparator);

                            List<Observation> thirdAttemptObservations = observationController.getObservationsByPatientuuidAndConceptId(patientUuid,24010);
                            Collections.sort(thirdAttemptObservations, observationDateTimeComparator);

                            if(firstAttemptObservations.size() > 0 && homeVisitTagCount==0){
                                if(firstAttemptObservations.get(0).getObservationDatetime().after(membershipDate)) {
                                    if (firstAttemptObservations.get(0).getValueCoded().getId() == 1065) {
                                        PatientTag firstAttemptTag = new PatientTag();
                                        firstAttemptTag.setName("SIM");
                                        firstAttemptTag.setUuid(SIM_TAG_UUID);
                                        tags.add(firstAttemptTag);
                                        patientController.savePatientTags(firstAttemptTag);
                                    } else {
                                        PatientTag firstAttemptTag = new PatientTag();
                                        firstAttemptTag.setName("NÃO");
                                        firstAttemptTag.setUuid(NAO_TAG_UUID);
                                        tags.add(firstAttemptTag);
                                        patientController.savePatientTags(firstAttemptTag);
                                    }
                                    homeVisitTagCount++;
                                }
                            }

                            if(secondAttemptObservations.size() > 0 && homeVisitTagCount==1){
                                if(secondAttemptObservations.get(0).getObservationDatetime().after(membershipDate)) {
                                    if (secondAttemptObservations.get(0).getValueCoded().getId() == 1065) {
                                        PatientTag secondAttemptTag = new PatientTag();
                                        secondAttemptTag.setName("SIM");
                                        secondAttemptTag.setUuid(SIM_TAG_UUID);
                                        tags.add(secondAttemptTag);
                                        patientController.savePatientTags(secondAttemptTag);
                                    } else {
                                        PatientTag secondAttemptTag = new PatientTag();
                                        secondAttemptTag.setName("NÃO");
                                        secondAttemptTag.setUuid(NAO_TAG_UUID);
                                        tags.add(secondAttemptTag);
                                        patientController.savePatientTags(secondAttemptTag);
                                    }
                                    homeVisitTagCount++;
                                }
                            }

                            if(thirdAttemptObservations.size() > 0 && homeVisitTagCount==2){
                                if(thirdAttemptObservations.get(0).getObservationDatetime().after(membershipDate)) {
                                    if (thirdAttemptObservations.get(0).getValueCoded().getId() == 1065) {
                                        PatientTag thirdAttemptTag = new PatientTag();
                                        thirdAttemptTag.setName("SIM");
                                        thirdAttemptTag.setUuid(SIM_TAG_UUID);
                                        tags.add(thirdAttemptTag);
                                        patientController.savePatientTags(thirdAttemptTag);
                                    } else {
                                        PatientTag thirdAttemptTag = new PatientTag();
                                        thirdAttemptTag.setName("NÃO");
                                        thirdAttemptTag.setUuid(NAO_TAG_UUID);
                                        tags.add(thirdAttemptTag);
                                        patientController.savePatientTags(thirdAttemptTag);
                                    }
                                    homeVisitTagCount++;
                                }
                            }
                        }
                    }

                    patient.setTags(tags.toArray(new PatientTag[tags.size()]));
                    patientController.updatePatient(patient);
                }
            } catch (RelationshipController.RetrieveRelationshipException e) {
                Log.e(getClass().getSimpleName(),"Error retrieving relationships", e);
            } catch (PatientController.PatientSaveException e) {
                Log.e(getClass().getSimpleName(), "Could not save patient with updated tags", e);
            } catch (IOException e) {
                Log.e(getClass().getSimpleName(), "Could not load records", e);
            } catch (PatientController.PatientLoadException e) {
                Log.e(getClass().getSimpleName(), "Could not load patient record to update update tags", e);
            } catch (ObservationController.LoadObservationException e) {
                Log.e(getClass().getSimpleName(), "Could not load observations to create tags", e);
            } catch (CohortController.CohortFetchException e) {
                Log.e(getClass().getSimpleName(), "Exception thrown while fetching cohorts.", e);
            }
        }
    }

    private final Comparator<Observation> observationDateTimeComparator = new Comparator<Observation>() {
        @Override
        public int compare(Observation lhs, Observation rhs) {
            return -lhs.getObservationDatetime().compareTo(rhs.getObservationDatetime());
        }
    };

    public int[] downloadReportDatasets(List<Integer> datasetDefinitionIds, boolean isDeltaSync){
        int[] result = new int[2];
        try {
            List<ReportDataset> reportDatasets = reportDatasetController.downloadReportDatasets(datasetDefinitionIds, isDeltaSync);
            reportDatasetController.saveReportDatasets(reportDatasets);
            result[0] = SUCCESS;
            result[1] = reportDatasets.size();

        } catch (ReportDatasetController.ReportDatasetDownloadException | ReportDatasetController.ReportDatasetSaveException e) {
            Log.e(TAG, "Encountered Load Exception while getting report datasets", e);
        }

        return result;
    }


    public int[] downloadReportDatasetsForDownloadedReports(boolean isDeltaSync){
        int[] result = new int[2];
        List<Integer> datasetDefinitionIds = new ArrayList<>();
        try {
            List<ReportDataset> reportDatasets = reportDatasetController.getReportDatasets();
            if(reportDatasets != null && reportDatasets.size()>0){
                for(ReportDataset reportDataset : reportDatasets){
                    datasetDefinitionIds.add(reportDataset.getDatasetDefinitionId());
                }
                downloadReportDatasets(datasetDefinitionIds, isDeltaSync);
            }
        } catch (ReportDatasetController.ReportDatasetFetchException e) {
            result[0] = LOAD_ERROR;
            Log.e(getClass().getSimpleName(), "Error while fetching report datasets",e);
        }

        return result;
    }

    public int[] downloadMediaCategories(List<String> mediaCategoryUuids){
        int[] result = new int[2];
        try {
            List<MediaCategory> mediaCategories = mediaCategoryController.downloadMediaCategory(mediaCategoryUuids, false);
            mediaCategoryController.saveMediaCategory(mediaCategories);
            result[0] = SUCCESS;
            result[1] = mediaCategories.size();

        } catch (MediaCategoryController.MediaCategoryDownloadException e) {
            result[0] = DOWNLOAD_ERROR;
            Log.e(TAG, "Encountered Load Exception while downloading media categories", e);

        } catch (MediaCategoryController.MediaCategorySaveException e) {
            result[0] = SAVE_ERROR;
            Log.e(TAG, "Encountered Load Exception while saving media categories", e);
        }
        return result;
    }

    public List<Media> downloadMedia(List<String> mediaCategoryUuids, boolean isDeltaSync){
        List<Media> media = new ArrayList<>();
        try {
            media = mediaController.downloadMedia(mediaCategoryUuids, isDeltaSync);
        } catch (MediaController.MediaDownloadException e) {
            Log.e(TAG, "Encountered Load Exception while getting media", e);
        }
        return media;
    }

    public int[] saveMedia(List<Media> media){
        int[] result = new int[2];
        try {
            mediaController.saveMedia(media);
            result[0] = SUCCESS;
            result[1] = media.size();
        } catch (MediaController.MediaSaveException e) {
            result[0] = SyncStatusConstants.SAVE_ERROR;
            Log.e(TAG, "Encountered Load Exception while downloading media", e);
        }
        return result;
    }

    public int[] SyncDatasets(SetupConfigurationTemplate configBeforeConfigUpdate){
        int[] result = new int[2];
        int reportDatasets = 0;
        try {
            //Get datasets in the config
            List<Integer> datasetIds = new ArrayList<>();
            List<Integer> datasetIdsBeforeConfigUpdate = new ArrayList<>();

            SetupConfigurationTemplate activeSetupConfig = setupConfigurationController.getActiveSetupConfigurationTemplate();
            String configJson = activeSetupConfig.getConfigJson();
            List<Object> datasets = JsonUtils.readAsObjectList(configJson, "$['config']['datasets']");
            for (Object dataset : datasets) {
                net.minidev.json.JSONObject dataset1 = (net.minidev.json.JSONObject) dataset;
                Integer datasetId = (Integer)dataset1.get("id");
                datasetIds.add(datasetId);
            }

            String configJsonBeforeConfigUpdate = configBeforeConfigUpdate.getConfigJson();
            List<Object> datasetsBeforeConfigUpdate = JsonUtils.readAsObjectList(configJsonBeforeConfigUpdate, "$['config']['datasets']");
            for (Object dataset : datasetsBeforeConfigUpdate) {
                net.minidev.json.JSONObject dataset1 = (net.minidev.json.JSONObject) dataset;
                Integer datasetId = (Integer)dataset1.get("id");
                datasetIdsBeforeConfigUpdate.add(datasetId);
            }

            List<Integer> datasetToDeleteIds = new ArrayList<>();
            List<Integer> datasetToDownload= new ArrayList<>();

            //Get datasets previously downloaded but not in the updated config
            for(Integer datasetId: datasetIdsBeforeConfigUpdate){
                if(!datasetIds.contains(datasetId)){
                    datasetToDeleteIds.add(datasetId);
                }
            }

            //sync the downloaded datasets with changes
            if(datasetIdsBeforeConfigUpdate.size() > 0){
                List<ReportDataset> reportDatasetList = reportDatasetController.downloadReportDatasets(datasetIdsBeforeConfigUpdate, true);
                reportDatasetController.updateReportDatasets(reportDatasetList);
                reportDatasets = reportDatasetList.size();
            }

            //Get Added datasets to updated config
            for(Integer datasetId : datasetIds){
                if(!datasetIdsBeforeConfigUpdate.contains(datasetId)){
                    datasetToDownload.add(datasetId);
                }
            }

            if(datasetToDeleteIds.size() > 0) {
                reportDatasetController.deleteReportDatasets(datasetToDeleteIds);
            }

            if(datasetToDownload.size()>0){
                //Download Added datasets
                List<ReportDataset> reportDatasetList = reportDatasetController.downloadReportDatasets(datasetToDownload, false);
                reportDatasetController.saveReportDatasets(reportDatasetList);
                reportDatasets = reportDatasets+reportDatasetList.size();
            }

            result[0] = SUCCESS;
            result[1] = reportDatasets;

        } catch (ReportDatasetController.ReportDatasetSaveException reportDatasetSaveException) {
            reportDatasetSaveException.printStackTrace();
            result[0] = SyncStatusConstants.SAVE_ERROR;
        } catch (SetupConfigurationController.SetupConfigurationFetchException setupConfigurationFetchException) {
            setupConfigurationFetchException.printStackTrace();
            result[0] = SyncStatusConstants.LOAD_ERROR;
        } catch (ReportDatasetController.ReportDatasetDownloadException reportDatasetDownloadException) {
            reportDatasetDownloadException.printStackTrace();
            result[0] = SyncStatusConstants.LOAD_ERROR;
        } catch (ReportDatasetController.ReportDatasetFetchException reportDatasetFetchException) {
            reportDatasetFetchException.printStackTrace();
            result[0] = SyncStatusConstants.LOAD_ERROR;
        }
        return result;
    }

    public void SyncFormTemplates(SetupConfigurationTemplate configBeforeConfigUpdate){
        try {
            android.content.Context context = muzimaApplication.getApplicationContext();
            //Get forms in the config
            List<String> formUuids = new ArrayList<>();
            List<String> formUuidsBeforeConfigUpdate = new ArrayList<>();

            SetupConfigurationTemplate activeSetupConfig = setupConfigurationController.getActiveSetupConfigurationTemplate();
            String configJson = activeSetupConfig.getConfigJson();
            List<Object> forms = JsonUtils.readAsObjectList(configJson, "$['config']['forms']");
            for (Object form : forms) {
                net.minidev.json.JSONObject form1 = (net.minidev.json.JSONObject) form;
                String formUuid = form1.get("uuid").toString();
                formUuids.add(formUuid);
            }

            String configJsonBeforeConfigUpdate = configBeforeConfigUpdate.getConfigJson();
            List<Object> formsBeforeConfigUpdate = JsonUtils.readAsObjectList(configJsonBeforeConfigUpdate, "$['config']['forms']");
            for (Object form : formsBeforeConfigUpdate) {
                net.minidev.json.JSONObject form1 = (net.minidev.json.JSONObject) form;
                String formUuid = form1.get("uuid").toString();
                formUuidsBeforeConfigUpdate.add(formUuid);
            }

            List<String> formTemplatesToDeleteUuids = new ArrayList<>();
            List<String>  formTemplateToDownload= new ArrayList<>();

            //Get forms previously downloaded but not in the updated config
            for(String formUuid: formUuidsBeforeConfigUpdate){
                if(!formUuids.contains(formUuid)){
                    formTemplatesToDeleteUuids.add(formUuid);
                }
            }

            //Get Added forms to updated config
            for(String formUuid : formUuids){
                if(!formUuidsBeforeConfigUpdate.contains(formUuid)){
                    formTemplateToDownload.add(formUuid);
                }
            }

            //Get Forms with Updates
            List<Form> allForms = formController.getAllAvailableForms();
            for (Form form : allForms) {
                if (form.isUpdateAvailable() && formUuids.contains(form.getUuid())) {
                    formTemplateToDownload.add(form.getUuid());
                }
            }

            boolean isFormWithPatientDataAvailable = formController.isFormWithPatientDataAvailable(context);

            if(!isFormWithPatientDataAvailable){
                String[] formsToDownload = formTemplateToDownload.stream().toArray(String[]::new);

                if(formTemplatesToDeleteUuids.size()>0)
                    formController.deleteFormTemplatesByUUID(formTemplatesToDeleteUuids);

                if(formTemplateToDownload.size()>0)
                    new SyncFormTemplateIntent(context, formsToDownload).start();
            }else{
                List<String> formsWithPatientData = new ArrayList<>();

                CompleteFormsWithPatientData completeFormsWithPatientData = formController.getAllCompleteFormsWithPatientData(context, StringUtils.EMPTY);
                IncompleteFormsWithPatientData incompleteFormsWithPatientData = formController.getAllIncompleteFormsWithPatientData(StringUtils.EMPTY);

                for(CompleteFormWithPatientData completeFormWithPatientData : completeFormsWithPatientData){
                    formsWithPatientData.add(completeFormWithPatientData.getFormUuid());
                }

                for(IncompleteFormWithPatientData inCompleteFormWithPatientData : incompleteFormsWithPatientData){
                    formsWithPatientData.add(inCompleteFormWithPatientData.getFormUuid());
                }

                for(String formTemplateToDeleteUuid : formTemplatesToDeleteUuids) {
                    if (!formsWithPatientData.contains(formTemplateToDeleteUuid)) {
                        //Delete form template
                        formController.deleteFormTemplatesByUUID(Collections.singletonList(formTemplateToDeleteUuid));
                    }
                }

                List<String> formsToDownloadUuids = new ArrayList<>();
                for(String formTemplateUuidToDownload : formTemplateToDownload) {
                    if (!formsWithPatientData.contains(formTemplateUuidToDownload)) {
                        formsToDownloadUuids.add(formTemplateUuidToDownload);

                    }
                }
                if(formsToDownloadUuids.size()>0){
                    //Download Templates
                    new SyncFormTemplateIntent(context, formsToDownloadUuids.stream().toArray(String[]::new)).start();
                }
            }
        } catch (FormController.FormFetchException e){
            Log.e(getClass().getSimpleName(),"Could not fetch downloaded forms ",e);
        } catch (SetupConfigurationController.SetupConfigurationFetchException e){
            Log.e(getClass().getSimpleName(),"Could not get the active config ",e);
        } catch (FormController.FormDeleteException e) {
            Log.e(getClass().getSimpleName(),"Could not delete form templates ",e);
        }
    }

    public int[] SyncMediaCategory(SetupConfigurationTemplate configBeforeConfigUpdate){
        int[] result = new int[2];
        int mediaCategories = 0;
        try {
            //Get media Categories in the config
            List<String> mediaCategoryUuids = new ArrayList<>();
            List<String> mediaCategoryUuidsBeforeConfigUpdate = new ArrayList<>();

            SetupConfigurationTemplate activeSetupConfig = setupConfigurationController.getActiveSetupConfigurationTemplate();
            String configJson = activeSetupConfig.getConfigJson();
            List<Object> mediaCategoryList = JsonUtils.readAsObjectList(configJson, "$['config']['mediaCategories']");
            for (Object mediaCategory : mediaCategoryList) {
                net.minidev.json.JSONObject mediaCategory1 = (net.minidev.json.JSONObject) mediaCategory;
                String mediaCategoryUuid = mediaCategory1.get("uuid").toString();
                mediaCategoryUuids.add(mediaCategoryUuid);
            }

            String configJsonBeforeConfigUpdate = configBeforeConfigUpdate.getConfigJson();
            List<Object> mediaCategoryBeforeConfigUpdate = JsonUtils.readAsObjectList(configJsonBeforeConfigUpdate, "$['config']['mediaCategories']");
            for (Object mediaCategory : mediaCategoryBeforeConfigUpdate) {
                net.minidev.json.JSONObject mediaCategory1 = (net.minidev.json.JSONObject) mediaCategory;
                String mediaCategoryUuid = mediaCategory1.get("uuid").toString();
                mediaCategoryUuidsBeforeConfigUpdate.add(mediaCategoryUuid);
            }

            List<String> mediaCategoryToBeDeleted = new ArrayList<>();
            List<String> mediaCategoryToDownload= new ArrayList<>();
            List<String> mediaCategoryToCheckForUpdates = new ArrayList<>();

            //Get mediaCategory previously downloaded but not in the updated config
            for(String mediaCategoryUuid: mediaCategoryUuidsBeforeConfigUpdate){
                if(!mediaCategoryUuids.contains(mediaCategoryUuid)){
                    mediaCategoryToBeDeleted.add(mediaCategoryUuid);
                }else{
                    mediaCategoryToCheckForUpdates.add(mediaCategoryUuid);
                }
            }

            //Get Added mediaCategory to updated config
            for(String mediaCategoryUuid : mediaCategoryUuids){
                if(!mediaCategoryUuidsBeforeConfigUpdate.contains(mediaCategoryUuid)){
                    mediaCategoryToDownload.add(mediaCategoryUuid);
                }
            }

            if(mediaCategoryToBeDeleted.size()>0) {
                mediaCategoryController.deleteMediaCategory(mediaCategoryToBeDeleted);
            }

            if(mediaCategoryToCheckForUpdates.size()>0){
                List<MediaCategory> mediaCategoryListToUpdate = mediaCategoryController.downloadMediaCategory(mediaCategoryToCheckForUpdates, true);
                mediaCategoryController.updateMediaCategory(mediaCategoryListToUpdate);
                mediaCategories = mediaCategories+mediaCategoryListToUpdate.size();
            }

            if(mediaCategoryToDownload.size()>0) {
                List<MediaCategory> downloadedMediaList = mediaCategoryController.downloadMediaCategory(mediaCategoryToDownload, false);
                mediaCategoryController.saveMediaCategory(downloadedMediaList);
                mediaCategories = mediaCategories+downloadedMediaList.size();
            }

            result[0] = SUCCESS;
            result[1] = mediaCategories;
        } catch (MediaCategoryController.MediaCategoryDownloadException e) {
            result[0] = DOWNLOAD_ERROR;
            Log.e(getClass().getSimpleName(), "Encountered an error while downloading media categories");
        } catch (MediaCategoryController.MediaCategorySaveException e) {
            result[0] = SyncStatusConstants.SAVE_ERROR;
            Log.e(getClass().getSimpleName(), "Encountered an error while saving media categories");
        } catch (SetupConfigurationController.SetupConfigurationFetchException e) {
            result[0] = SyncStatusConstants.LOAD_ERROR;
            Log.e(getClass().getSimpleName(), "Encountered an error while getting config");
        }
        return result;
    }

    public int[] SyncMedia(){
        int[] result = new int[2];
        try {
            android.content.Context context = muzimaApplication.getApplicationContext();
            //update memory space app usage log
            AppUsageLogs appUsageLogs = null;

            try{
                appUsageLogsController.getAppUsageLogByKey(com.muzima.util.Constants.AppUsageLogs.AVAILABLE_INTERNAL_SPACE);
            } catch (IOException e){
                Log.e(TAG,"Could not find availableInternalSpace log");
            }
            String availableMemory = MemoryUtil.getFormattedMemory(MemoryUtil.getAvailableInternalMemorySize());

            if(appUsageLogs != null) {
                if(!availableMemory.equals(appUsageLogs.getLogvalue())) {
                    appUsageLogs.setLogvalue(availableMemory);
                    appUsageLogs.setUpdateDatetime(new Date());
                    appUsageLogs.setUserName(muzimaApplication.getAuthenticatedUserId());
                    appUsageLogs.setDeviceId(pseudoDeviceId);
                    appUsageLogs.setLogSynced(false);
                    appUsageLogsController.saveOrUpdateAppUsageLog(appUsageLogs);
                }
            }else{
                AppUsageLogs availableSpace = new AppUsageLogs();
                availableSpace.setUuid(UUID.randomUUID().toString());
                availableSpace.setLogKey(com.muzima.util.Constants.AppUsageLogs.AVAILABLE_INTERNAL_SPACE);
                availableSpace.setLogvalue(availableMemory);
                availableSpace.setUpdateDatetime(new Date());
                availableSpace.setUserName(muzimaApplication.getAuthenticatedUserId());
                availableSpace.setDeviceId(pseudoDeviceId);
                availableSpace.setLogSynced(false);
                appUsageLogsController.saveOrUpdateAppUsageLog(availableSpace);
            }

            MuzimaSyncService muzimaSyncService = ((MuzimaApplication) context).getMuzimaSyncService();
            MediaCategoryController mediaCategoryController = ((MuzimaApplication) context).getMediaCategoryController();
            List<MediaCategory> mediaCategoryList= mediaCategoryController.getMediaCategories();
            List<String> mediaCategoryUuids = new ArrayList<>();
            if(mediaCategoryList.size()>0) {
                for (MediaCategory mediaCategory : mediaCategoryList) {
                    mediaCategoryUuids.add(mediaCategory.getUuid());
                }
                List<Media> mediaList = muzimaSyncService.downloadMedia(mediaCategoryUuids, true);
                long totalFileSize = MemoryUtil.getTotalMediaFileSize(mediaList);
                long availableSpace = MemoryUtil.getAvailableInternalMemorySize();
                if(availableSpace>totalFileSize) {
                    for (Media media : mediaList) {
                        Media media1 = mediaController.getMediaByUuid(media.getUuid());
                        if(media1 != null)
                            mediaController.updateMedia(Collections.singletonList(media));
                        else
                            mediaController.saveMedia(Collections.singletonList(media));

                        downloadFile(media,context);
                    }
                    result[0] =SUCCESS;
                    result[1] = mediaList.size();
                }else{
                    AppUsageLogs noEnoughSpaceLog = appUsageLogsController.getAppUsageLogByKey(com.muzima.util.Constants.AppUsageLogs.NO_ENOUGH_SPACE_DEVICES);
                    String requiredMemory = MemoryUtil.getFormattedMemory(MemoryUtil.getAvailableInternalMemorySize());
                    if(noEnoughSpaceLog != null) {
                        noEnoughSpaceLog.setLogvalue("Required: "+requiredMemory+ " Available: "+availableMemory);
                        noEnoughSpaceLog.setUpdateDatetime(new Date());
                        noEnoughSpaceLog.setUserName(muzimaApplication.getAuthenticatedUserId());
                        noEnoughSpaceLog.setDeviceId(pseudoDeviceId);
                        noEnoughSpaceLog.setLogSynced(false);
                        appUsageLogsController.saveOrUpdateAppUsageLog(noEnoughSpaceLog);
                    }else{
                        AppUsageLogs newNoEnoughSpaceLog = new AppUsageLogs();
                        newNoEnoughSpaceLog.setUuid(UUID.randomUUID().toString());
                        newNoEnoughSpaceLog.setLogKey(com.muzima.util.Constants.AppUsageLogs.NO_ENOUGH_SPACE_DEVICES);
                        newNoEnoughSpaceLog.setLogvalue("Required: "+requiredMemory+ " Available: "+availableMemory);
                        newNoEnoughSpaceLog.setUpdateDatetime(new Date());
                        newNoEnoughSpaceLog.setUserName(muzimaApplication.getAuthenticatedUserId());
                        newNoEnoughSpaceLog.setDeviceId(pseudoDeviceId);
                        newNoEnoughSpaceLog.setLogSynced(false);
                        appUsageLogsController.saveOrUpdateAppUsageLog(newNoEnoughSpaceLog);
                    }
                    MemoryUtil.showAlertDialog(availableSpace,totalFileSize, context.getApplicationContext());
                }
            }else{
                result[0] =SUCCESS;
                result[1] = 0;
            }
        }  catch (MediaCategoryController.MediaCategoryFetchException e) {
            Log.e(getClass().getSimpleName(), "Encountered an error while fetching media categories");
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(),"Encountered IOException ",e);
        } catch (ParseException e) {
            Log.e(getClass().getSimpleName(),"Encountered ParseException ",e);
        } catch (MediaController.MediaFetchException e) {
            Log.e(getClass().getSimpleName(),"Encountered Media fetch exception ",e);
        } catch (MediaController.MediaSaveException e) {
            Log.e(getClass().getSimpleName(),"Encountered exception while saving media ",e);
        }
        return result;
    }

    public void downloadFile(Media media, android.content.Context context){
        try {
            //Delete file if exists
            String mimeType = media.getMimeType();
            String PATH = Objects.requireNonNull(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)).getAbsolutePath();
            File file = new File(PATH + "/"+media.getName()+"."+mimeType.substring(mimeType.lastIndexOf("/") + 1));
            String mediaName = media.getName()+"."+mimeType.substring(mimeType.lastIndexOf("/") + 1);
            if(mimeType.substring(mimeType.lastIndexOf("/") + 1).equals("vnd.ms-excel")){
                file = new File(PATH + "/"+media.getName()+".xls");
                mediaName = media.getName()+".xls";
            }else if(mimeType.substring(mimeType.lastIndexOf("/") + 1).equals("vnd.openxmlformats-officedocument.spreadsheetml.sheet")){
                file = new File(PATH + "/"+media.getName()+".xlsx");
                mediaName = media.getName()+".xlsx";
            }else if(mimeType.substring(mimeType.lastIndexOf("/") + 1).equals("msword")){
                file = new File(PATH + "/"+media.getName()+".doc");
                mediaName = media.getName()+".doc";
            }else if(mimeType.substring(mimeType.lastIndexOf("/") + 1).equals("vnd.openxmlformats-officedocument.wordprocessingml.document")){
                file = new File(PATH + "/"+media.getName()+".docx");
                mediaName = media.getName()+".docx";
            }else if(mimeType.substring(mimeType.lastIndexOf("/") + 1).equals("vnd.ms-powerpoint")){
                file = new File(PATH + "/"+media.getName()+".ppt");
                mediaName = media.getName()+".ppt";
            }else if(mimeType.substring(mimeType.lastIndexOf("/") + 1).equals("vnd.openxmlformats-officedocument.presentationml.presentation")){
                file = new File(PATH + "/"+media.getName()+".pptx");
                mediaName = media.getName()+".pptx";
            }
            if(file.exists()) {
                file.delete();
                context.getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
            }

            if(!media.isRetired()) {
                //Enqueue the file for download
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(media.getUrl() + ""));
                request.setTitle(media.getName());
                request.setDescription(media.getDescription());
                request.allowScanningByMediaScanner();
                request.setAllowedOverMetered(true);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, mediaName);
                DownloadManager dm = (DownloadManager) context.getApplicationContext().getSystemService(DOWNLOAD_SERVICE);
                dm.enqueue(request);
            }
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "Error ", e);
        }
    }

    public void SyncPatientFullDataBasedOnCohortChangesInConfig(SetupConfigurationTemplate configBeforeConfigUpdate){
        try {
            android.content.Context context = muzimaApplication.getApplicationContext();
            //Get cohorts in the config
            List<String> cohortUuids = new ArrayList<>();
            List<String> cohortUuidsBeforeUpdate = new ArrayList<>();

            SetupConfigurationTemplate activeSetupConfig = setupConfigurationController.getActiveSetupConfigurationTemplate();
            String configJson = activeSetupConfig.getConfigJson();
            List<Object> cohorts = JsonUtils.readAsObjectList(configJson, "$['config']['cohorts']");
            for (Object cohort : cohorts) {
                net.minidev.json.JSONObject cohort1 = (net.minidev.json.JSONObject) cohort;
                String cohortUuid = cohort1.get("uuid").toString();
                cohortUuids.add(cohortUuid);
            }


            String configJsonBeforeConfigUpdate = configBeforeConfigUpdate.getConfigJson();
            List<Object> cohortsBeforeConfigUpdate = JsonUtils.readAsObjectList(configJsonBeforeConfigUpdate, "$['config']['cohorts']");
            for (Object cohort : cohortsBeforeConfigUpdate) {
                net.minidev.json.JSONObject cohort1 = (net.minidev.json.JSONObject) cohort;
                String cohortUuid = cohort1.get("uuid").toString();
                cohortUuidsBeforeUpdate.add(cohortUuid);
            }

            List<String> cohortsToSetAsUnsyncedUuids = new ArrayList<>();
            List<String> cohortsToDownload = new ArrayList<>();

            //Get cohorts previously in config but not in the updated config
            for (String cohortUuid : cohortUuidsBeforeUpdate) {
                if (!cohortUuids.contains(cohortUuid)) {
                    cohortsToSetAsUnsyncedUuids.add(cohortUuid);
                }
            }

            //Get Added cohorts to updated config
            for (String cohortUuid : cohortUuids) {
                if (!cohortUuidsBeforeUpdate.contains(cohortUuid)) {
                    cohortsToDownload.add(cohortUuid);
                }
            }

            if (cohortsToSetAsUnsyncedUuids.size() > 0) {
                cohortController.setSyncStatus(cohortsToSetAsUnsyncedUuids.stream().toArray(String[]::new), 0);
                cohortController.deletePatientsNotBelongingToAnotherCohortByCohortUuids(cohortsToSetAsUnsyncedUuids);
                cohortController.deleteAllCohortMembersByCohortUuids(cohortsToSetAsUnsyncedUuids);
            }

            if (cohortsToDownload.size() > 0)
                new SyncPatientDataIntent(context, cohortsToDownload.stream().toArray(String[]::new)).start();

        } catch(SetupConfigurationController.SetupConfigurationFetchException e) {
            Log.e(getClass().getSimpleName(), "Could not get the active config ", e);
        } catch(CohortController.CohortUpdateException e) {
            Log.e(getClass().getSimpleName(), "Could not able to update cohort ", e);
        } catch(CohortController.CohortReplaceException e) {
            Log.e(getClass().getSimpleName(), "Could not able to replace cohort ", e);
        }
    }

    public int[] DownloadAndDeleteLocationBasedOnConfigChanges(SetupConfigurationTemplate configBeforeConfigUpdate){
        int result[] = new int[3];
        try {
            //Get locations in the config
            List<String> locationUuids = new ArrayList<>();
            List<String> locationUuidsBeforeConfigUpdate = new ArrayList<>();

            SetupConfigurationTemplate activeSetupConfig = setupConfigurationController.getActiveSetupConfigurationTemplate();
            String configJson = activeSetupConfig.getConfigJson();
            List<Object> locations = JsonUtils.readAsObjectList(configJson, "$['config']['locations']");
            for (Object location : locations) {
                net.minidev.json.JSONObject location1 = (net.minidev.json.JSONObject) location;
                String locationUuid = location1.get("uuid").toString();
                locationUuids.add(locationUuid);
            }

            String configJsonBeforeConfigUpdate = configBeforeConfigUpdate.getConfigJson();
            List<Object> locationsBeforeConfigUpdate = JsonUtils.readAsObjectList(configJsonBeforeConfigUpdate, "$['config']['locations']");
            for (Object location : locationsBeforeConfigUpdate) {
                net.minidev.json.JSONObject location1 = (net.minidev.json.JSONObject) location;
                String locationUuid = location1.get("uuid").toString();
                locationUuidsBeforeConfigUpdate.add(locationUuid);
            }

            List<String> locationsToBeDeleted = new ArrayList<>();
            List<String> locationsToDownload= new ArrayList<>();

            //Get locations previously in config  but not in the updated config
            for(String locationUuid: locationUuidsBeforeConfigUpdate){
                if(!locationUuids.contains(locationUuid)){
                    locationsToBeDeleted.add(locationUuid);
                }
            }

            //Get Added locations to updated config
            for(String locationUuid : locationUuids){
                if(!locationUuidsBeforeConfigUpdate.contains(locationUuid)){
                    locationsToDownload.add(locationUuid);
                }
            }

            if(locationsToBeDeleted.size()>0) {
                locationController.deleteLocationsByUuids(locationsToBeDeleted);
            }

            if(locationsToDownload.size()>0) {
                List<Location> locationList = locationController.downloadLocationsFromServerByUuid(locationsToDownload.stream().toArray(String[]::new));
                locationController.saveLocations(locationList);
            }

            result[0] = SUCCESS;
            result[1] = locationsToDownload.size();
            result[2] = locationsToBeDeleted.size();

        } catch (SetupConfigurationController.SetupConfigurationFetchException e){
            result[0] = LOAD_ERROR;
            Log.e(getClass().getSimpleName(),"Could not get the active config ",e);
        }  catch (LocationController.LocationDeleteException e) {
            result[0] = DELETE_ERROR;
            Log.e(getClass().getSimpleName(),"Could not delete locations ",e);
        } catch (LocationController.LocationDownloadException e) {
            result[0] = DOWNLOAD_ERROR;
            Log.e(getClass().getSimpleName(),"Could not download locations ",e);
        } catch (LocationController.LocationSaveException e) {
            result[0] = SAVE_ERROR;
            Log.e(getClass().getSimpleName(),"Could not save locations ",e);
        } catch (IOException e) {
            result[0] = UNKNOWN_ERROR;
            Log.e(getClass().getSimpleName(),"Could not get locations ",e);
        }
        return result;
    }

    public int[] DownloadAndDeleteProvidersBasedOnConfigChanges(SetupConfigurationTemplate configBeforeConfigUpdate){
        int result[] = new int[3];
        try {
            //Get providers in the config
            List<String> providerUuids = new ArrayList<>();
            List<String> providerUuidsBeforeConfigUpdate = new ArrayList<>();

            SetupConfigurationTemplate activeSetupConfig = setupConfigurationController.getActiveSetupConfigurationTemplate();
            String configJson = activeSetupConfig.getConfigJson();
            List<Object> providers = JsonUtils.readAsObjectList(configJson, "$['config']['providers']");
            for (Object provider : providers) {
                net.minidev.json.JSONObject provider1 = (net.minidev.json.JSONObject) provider;
                String providerUuid = provider1.get("uuid").toString();
                providerUuids.add(providerUuid);
            }

            String configJsonBeforeConfigUpdate = configBeforeConfigUpdate.getConfigJson();
            List<Object> providersBeforeConfigUpdate = JsonUtils.readAsObjectList(configJsonBeforeConfigUpdate, "$['config']['providers']");
            for (Object provider : providersBeforeConfigUpdate) {
                net.minidev.json.JSONObject provider1 = (net.minidev.json.JSONObject) provider;
                String providerUuid = provider1.get("uuid").toString();
                providerUuidsBeforeConfigUpdate.add(providerUuid);
            }

            List<String> providersToBeDeleted = new ArrayList<>();
            List<String> providersToDownload= new ArrayList<>();

            //Get providers previously downloaded but not in the updated config
            for(String providerUuid: providerUuidsBeforeConfigUpdate){
                if(!providerUuids.contains(providerUuid)){
                    providersToBeDeleted.add(providerUuid);
                }
            }

            //Get Added providers to updated config
            for(String providerUuid : providerUuids){
                if(!providerUuidsBeforeConfigUpdate.contains(providerUuid)){
                    providersToDownload.add(providerUuid);
                }
            }

            if(providersToBeDeleted.size()>0) {
                providerController.deleteProvidersByUuids(providersToBeDeleted);
            }

            if(providersToDownload.size()>0) {
                List<Provider> providerList = providerController.downloadProvidersFromServerByUuid(providersToDownload.stream().toArray(String[]::new));
                providerController.saveProviders(providerList);
            }

            result[0] = SUCCESS;
            result[1] = providersToDownload.size();
            result[2] = providersToBeDeleted.size();

        } catch (SetupConfigurationController.SetupConfigurationFetchException e){
            result[0] = LOAD_ERROR;
            Log.e(getClass().getSimpleName(),"Could not get the active config ",e);
        } catch (ProviderController.ProviderDeleteException e) {
            result[0] = DELETE_ERROR;
            Log.e(getClass().getSimpleName(),"Could not delete providers ",e);
        } catch (ProviderController.ProviderDownloadException e) {
            result[0] = DOWNLOAD_ERROR;
            Log.e(getClass().getSimpleName(),"Could not download providers ",e);
        } catch (ProviderController.ProviderSaveException e) {
            result[0] = SAVE_ERROR;
            Log.e(getClass().getSimpleName(),"Could not save providers ",e);
        } catch (IOException e) {
            result[0] = UNKNOWN_ERROR;
            Log.e(getClass().getSimpleName(),"Could not load providers ",e);
        }
        return result;
    }

    public int[] DownloadAndDeleteConceptAndObservationBasedOnConfigChanges(SetupConfigurationTemplate configBeforeConfigUpdate){
        int result[] = new int[5];
        try {
            android.content.Context context = muzimaApplication.getApplicationContext();
            //Get concepts in the config
            List<String> conceptUuids = new ArrayList<>();
            List<String> conceptUuidsBeforeConfigUpdate = new ArrayList<>();

            SetupConfigurationTemplate activeSetupConfig = setupConfigurationController.getActiveSetupConfigurationTemplate();
            String configJson = activeSetupConfig.getConfigJson();

            // ToDo: This should parse concepts in groups as well
            List<Object> concepts = JsonUtils.readAsObjectList(configJson, "$['config']['concepts']");
            for (Object concept : concepts) {
                net.minidev.json.JSONObject concept1 = (net.minidev.json.JSONObject) concept;
                String conceptUuid = concept1.get("uuid").toString();
                conceptUuids.add(conceptUuid);
            }

            String configJsonBeforeConfigUpdate = configBeforeConfigUpdate.getConfigJson();
            List<Object> conceptsBeforeConfigUpdate = JsonUtils.readAsObjectList(configJsonBeforeConfigUpdate, "$['config']['concepts']");
            for (Object concept : conceptsBeforeConfigUpdate) {
                net.minidev.json.JSONObject concept1 = (net.minidev.json.JSONObject) concept;
                String conceptUuid = concept1.get("uuid").toString();
                conceptUuidsBeforeConfigUpdate.add(conceptUuid);
            }

            List<Concept> conceptsToBeDeleted = new ArrayList<>();
            List<String> conceptsToDownload= new ArrayList<>();

            //Get concepts previously downloaded but not in the updated config
            for(String conceptUuid: conceptUuidsBeforeConfigUpdate){
                if(!conceptUuids.contains(conceptUuid)){
                    Concept concept = conceptController.getConceptByUuid(conceptUuid);
                    if(concept != null){
                        conceptsToBeDeleted.add(concept);
                    }

                }
            }

            //Get Added concepts to updated config
            for(String conceptUuid : conceptUuids){
                if(!conceptUuidsBeforeConfigUpdate.contains(conceptUuid)){
                    conceptsToDownload.add(conceptUuid);
                }
            }

            if(conceptsToBeDeleted.size()>0) {
                conceptController.deleteConcepts(conceptsToBeDeleted);
                observationController.deleteAllObservations(conceptsToBeDeleted);
            }

            List<Patient> patients = new ArrayList<>();
            List<Observation> observations = new ArrayList<>();

            if(conceptsToDownload.size()>0) {
                patients = ((MuzimaApplication) context).getPatientController().getAllPatients();
                List<String> patientUuids = new ArrayList<>();
                for(Patient patient : patients){
                    patientUuids.add(patient.getUuid());
                }
                List<List<String>> slicedPatientUuids = split(patientUuids);
                List<List<String>> slicedConceptUuids = split(conceptsToDownload);

                List<Concept> conceptList = conceptController.downloadConceptsByUuid(conceptsToDownload.stream().toArray(String[]::new));
                if(conceptList.size()>0){
                    conceptController.saveConcepts(conceptList);
                    for (List<String> slicedPatientUuid : slicedPatientUuids) {
                        for (List<String> slicedConceptUuid : slicedConceptUuids) {
                            List<Observation> observationsDownloaded = observationController.downloadObservationsForAddedConceptsByPatientUuidsAndConceptUuids(slicedPatientUuid, slicedConceptUuid,activeSetupConfig.getUuid());

                            if(observationsDownloaded.size() > 0){
                                observations.addAll(observationsDownloaded);
                            }
                        }
                    }
                    if(observations.size() > 0){
                        observationController.saveObservations(observations);
                    }
                }
            }

            result[0] = SUCCESS;
            result[1] = conceptsToDownload.size();
            result[2] = conceptsToBeDeleted.size();
            result[3] = observations.size();
            result[4] = patients.size();

        } catch (SetupConfigurationController.SetupConfigurationFetchException e){
            Log.e(getClass().getSimpleName(),"Could not get the active config ",e);
        } catch (ConceptController.ConceptFetchException e) {
            Log.e(getClass().getSimpleName(),"Could not get concepts ",e);
        } catch (ConceptController.ConceptDeleteException e) {
            Log.e(getClass().getSimpleName(),"Could not delete concepts ",e);
        } catch (ObservationController.DeleteObservationException e) {
            Log.e(getClass().getSimpleName(),"Could not delete observations ",e);
        } catch (ConceptController.ConceptDownloadException e) {
            Log.e(getClass().getSimpleName(),"Could not download concepts ",e);
        } catch (PatientController.PatientLoadException e) {
            Log.e(getClass().getSimpleName(),"Could not load patients ",e);
        } catch (ObservationController.DownloadObservationException e) {
            Log.e(getClass().getSimpleName(),"Could not download observations ",e);
        } catch (ObservationController.SaveObservationException e) {
            Log.e(getClass().getSimpleName(),"Could not save observations ",e);
        } catch (ConceptController.ConceptSaveException e) {
            Log.e(getClass().getSimpleName(),"Could not save concepts ",e);
        }
        return result;
    }

    public int[] downloadDerivedConcepts(String[] conceptUuids) {
        int[] result = new int[4];

        try {
            List<DerivedConcept> derivedConcepts = derivedConceptController.downloadDerivedConceptsByUuid(conceptUuids);
            derivedConceptController.saveDerivedConcepts(derivedConcepts);
            Log.i(getClass().getSimpleName(), "Downloaded " + derivedConcepts.size() + " derived concepts");

            result[0] = SUCCESS;
            result[1] = derivedConcepts.size();
        } catch (DerivedConceptController.DerivedConceptDownloadException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to download derived concepts", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
            return result;
        } catch (DerivedConceptController.DerivedConceptSaveException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to save derived concepts", e);
            result[0] = SyncStatusConstants.SAVE_ERROR;
            return result;
        }
        return result;
    }

    public int[] downloadDerivedObservationsForPatientsByCohortUUIDs(String[] cohortUuids, boolean replaceExistingObservation) {
        int[] result = new int[4];
        List<Patient> patients;
        try {
            patients = patientController.getPatientsForCohorts(cohortUuids);

            List<String> patientlist = new ArrayList();
            patientlist = getPatientUuids(patients);

            result = downloadDerivedObservationsForPatientsByPatientUUIDs(patientlist, replaceExistingObservation);
            if (result[0] != SUCCESS) {
                updateProgressDialog(muzimaApplication.getString(R.string.error_derived_observation_download));
            }

            if (muzimaApplication.getMuzimaSettingController().isRelationshipEnabled()) {
                downloadDerivedObservationsForAllPersons(true);
            }

        } catch (PatientController.PatientLoadException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while loading patients.", e);
            result[0] = SyncStatusConstants.LOAD_ERROR;
        }
        return result;
    }

    public int[] downloadDerivedObservationsForPatientsByPatientUUIDs(List<String> patientUuids, boolean replaceExistingObservations) {
        int[] result = new int[4];
        try {
            List<String> conceptUuidsFromDerivedConcepts = getConceptUuidsFromDerivedConcepts(derivedConceptController.getDerivedConcepts());
            List<List<String>> slicedPatientUuids = split(patientUuids);
            List<List<String>> slicedDerivedConceptUuids = split(conceptUuidsFromDerivedConcepts);
            Set<String> patientUuidsForDownloadedObs = new HashSet<>();

            String activeSetupConfigUuid = null;
            try {
                SetupConfigurationTemplate setupConfigurationTemplate = setupConfigurationController.getActiveSetupConfigurationTemplate();
                if (setupConfigurationTemplate != null) {
                    activeSetupConfigUuid = setupConfigurationTemplate.getUuid();
                }
            } catch (SetupConfigurationController.SetupConfigurationFetchException e) {
                Log.e(getClass().getSimpleName(), "Could not obtain active setup config", e);
            }

            for (List<String> slicedPatientUuid : slicedPatientUuids) {
                for (List<String> slicedDerivedConceptUuid : slicedDerivedConceptUuids) {

                    List<DerivedObservation> derivedObservations = new ArrayList<>(derivedObservationController.downloadDerivedObservationsByPatientUuidsAndConceptUuids(
                            slicedPatientUuid, slicedDerivedConceptUuid, activeSetupConfigUuid));

                    for (DerivedObservation derivedObservation : derivedObservations) {
                        if(!patientUuidsForDownloadedObs.contains(derivedObservation.getPerson().getUuid())) {
                            patientUuidsForDownloadedObs.add(derivedObservation.getPerson().getUuid());
                        }
                    }

                    updateProgressDialog(muzimaApplication.getString(R.string.info_derived_observations_download_progress, patientUuidsForDownloadedObs.size(), patientUuids.size()));

                    List<DerivedObservation> voidedObservations = getVoidedDerivedObservations(derivedObservations);
                    derivedObservationController.deleteDerivedObservations(voidedObservations);
                    derivedObservations.removeAll(voidedObservations);

                    if (replaceExistingObservations) {
                        derivedObservationController.updateDerivedObservations(derivedObservations);
                    } else {
                        derivedObservationController.saveDerivedObservations(derivedObservations);
                    }

                    result[1] += derivedObservations.size();
                    result[2] += voidedObservations.size();
                }
            }

            result[3] = patientUuidsForDownloadedObs.size();
            result[0] = SUCCESS;
        } catch (DerivedObservationController.DerivedObservationDownloadException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while downloading observations.", e);
            result[0] = SyncStatusConstants.DOWNLOAD_ERROR;
        } catch (DerivedObservationController.DerivedObservationDeleteException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while downloading observations.", e);
            result[0] = DELETE_ERROR;
        } catch (DerivedObservationController.DerivedObservationSaveException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while downloading observations.", e);
            result[0] = SAVE_ERROR;
        } catch (DerivedConceptController.DerivedConceptFetchException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while downloading observations.", e);
            result[0] = LOAD_ERROR;
        }
        return result;
    }

    private List<String> getConceptUuidsFromDerivedConcepts(List<DerivedConcept> derivedConcepts) {
        List<String> conceptUuids = new ArrayList<>();
        for (DerivedConcept derivedConcept : derivedConcepts) {
            conceptUuids.add(derivedConcept.getUuid());
        }
        return conceptUuids;
    }

    private List<DerivedObservation> getVoidedDerivedObservations(List<DerivedObservation> derivedObservations) {
        List<DerivedObservation> voidedDerivedObservations = new ArrayList<>();
        for (DerivedObservation derivedObservation : derivedObservations) {
            if (derivedObservation.isRetired()) {
                voidedDerivedObservations.add(derivedObservation);
            }
        }
        return voidedDerivedObservations;
    }

    public int[] DownloadAndDeleteDerivedConceptAndObservationBasedOnConfigChanges(SetupConfigurationTemplate configBeforeConfigUpdate, boolean replaceExistingDerivedObservation){
        int result[] = new int[5];
        try {
            android.content.Context context = muzimaApplication.getApplicationContext();
            //Get derived concepts in the config
            List<String> derivedConceptUuids = new ArrayList<>();
            List<String> derivedConceptUuidsBeforeConfigUpdate = new ArrayList<>();

            SetupConfigurationTemplate activeSetupConfig = setupConfigurationController.getActiveSetupConfigurationTemplate();
            String configJson = activeSetupConfig.getConfigJson();
            List<Object> derivedConcepts = JsonUtils.readAsObjectList(configJson, "$['config']['derivedConcepts']");
            for (Object derivedConcept : derivedConcepts) {
                net.minidev.json.JSONObject derivedConcept1 = (net.minidev.json.JSONObject) derivedConcept;
                String derivedConceptUuid = derivedConcept1.get("uuid").toString();
                derivedConceptUuids.add(derivedConceptUuid);
            }

            String configJsonBeforeConfigUpdate = configBeforeConfigUpdate.getConfigJson();
            List<Object> derivedConceptsBeforeConfigUpdate = JsonUtils.readAsObjectList(configJsonBeforeConfigUpdate, "$['config']['derivedConcepts']");
            for (Object derivedConcept : derivedConceptsBeforeConfigUpdate) {
                net.minidev.json.JSONObject derivedConcept1 = (net.minidev.json.JSONObject) derivedConcept;
                String derivedConceptUuid = derivedConcept1.get("uuid").toString();
                derivedConceptUuidsBeforeConfigUpdate.add(derivedConceptUuid);
            }

            List<DerivedConcept> derivedConceptsToBeDeleted = new ArrayList<>();
            List<String> derivedConceptsToDownload= new ArrayList<>();
            List<String> derivedConceptsToUpdate= new ArrayList<>();
            List<String> allConceptUuids = new ArrayList<>();

            //Get derived concepts previously downloaded but not in the updated config
            for(String derivedConceptUuid: derivedConceptUuidsBeforeConfigUpdate){
                if(!derivedConceptUuids.contains(derivedConceptUuid)){
                    DerivedConcept derivedConcept = derivedConceptController.getDerivedConceptByUuid(derivedConceptUuid);
                    if(derivedConcept != null){
                        derivedConceptsToBeDeleted.add(derivedConcept);
                    }
                }else{
                    DerivedConcept derivedConcept = derivedConceptController.getDerivedConceptByUuid(derivedConceptUuid);
                    if(derivedConcept != null){
                        derivedConceptsToUpdate.add(derivedConceptUuid);
                    }
                }
            }

            //Get Added derived concepts to updated config
            for(String derivedConceptUuid : derivedConceptUuids){
                if(!derivedConceptUuidsBeforeConfigUpdate.contains(derivedConceptUuid)){
                    derivedConceptsToDownload.add(derivedConceptUuid);
                }
            }

            if(derivedConceptsToBeDeleted.size()>0) {
                derivedConceptController.deleteDerivedConcepts(derivedConceptsToBeDeleted);
                derivedObservationController.deleteDerivedObservationsForDerivedConcepts(derivedConceptsToBeDeleted);
            }

            List<Patient> patients = new ArrayList<>();
            List<DerivedObservation> derivedObservations = new ArrayList<>();

            patients = ((MuzimaApplication) context).getPatientController().getAllPatients();
            List<String> patientUuids = new ArrayList<>();
            for(Patient patient : patients){
                patientUuids.add(patient.getUuid());
            }

            if(derivedConceptsToDownload.size()>0) {
                List<DerivedConcept> derivedConceptList = derivedConceptController.downloadDerivedConceptsByUuid(derivedConceptsToDownload.stream().toArray(String[]::new));
                if(derivedConceptList.size()>0){
                    derivedConceptController.saveDerivedConcepts(derivedConceptList);
                }
            }

            allConceptUuids.addAll(derivedConceptsToUpdate);
            allConceptUuids.addAll(derivedConceptsToDownload);

            if (allConceptUuids.size() > 0 && patientUuids.size() > 0) {
                List<DerivedObservation> derivedObservationsDownloaded = derivedObservationController.downloadDerivedObservationsByPatientUuidsAndConceptUuids(patientUuids, allConceptUuids, activeSetupConfig.getUuid());

                if (derivedObservationsDownloaded.size() > 0) {
                    derivedObservations.addAll(derivedObservationsDownloaded);
                    if (replaceExistingDerivedObservation)
                        derivedObservationController.updateDerivedObservations(derivedObservationsDownloaded);
                    else
                        derivedObservationController.saveDerivedObservations(derivedObservationsDownloaded);
                }

                List<String> patientUuidsForDownloadedDerivedObs = new ArrayList<>();

                for (DerivedObservation derivedObservation : derivedObservations) {
                    patientUuidsForDownloadedDerivedObs.add(derivedObservation.getPerson().getUuid());
                }

                result[0] = SUCCESS;
                result[1] = derivedConceptsToDownload.size();
                result[2] = derivedConceptsToBeDeleted.size();
                result[3] = derivedObservations.size();
                result[4] = patientUuidsForDownloadedDerivedObs.size();
            } else {
                result[0] = SUCCESS;
                result[1] = 0;
                result[2] = 0;
                result[3] = 0;
                result[4] = 0;
            }
        } catch (SetupConfigurationController.SetupConfigurationFetchException e){
            Log.e(getClass().getSimpleName(),"Could not get the active config ",e);
        } catch (PatientController.PatientLoadException e) {
            Log.e(getClass().getSimpleName(),"Could not load patients ",e);
        } catch (DerivedConceptController.DerivedConceptFetchException e) {
            Log.e(getClass().getSimpleName(),"Could not get derived concepts ",e);
        } catch (DerivedConceptController.DerivedConceptDownloadException e) {
            Log.e(getClass().getSimpleName(),"Could not download derived concepts ",e);
        } catch (DerivedConceptController.DerivedConceptSaveException e) {
            Log.e(getClass().getSimpleName(),"Could not save derived concepts ",e);
        } catch (DerivedObservationController.DerivedObservationDownloadException e) {
            Log.e(getClass().getSimpleName(),"Could not download derived observations ",e);
        } catch (DerivedObservationController.DerivedObservationSaveException e) {
            Log.e(getClass().getSimpleName(),"Could not save derived obs ",e);
        } catch (DerivedConceptController.DerivedConceptDeleteException e) {
            Log.e(getClass().getSimpleName(),"Could not delete derived concepts ",e);
        } catch (DerivedObservationController.DerivedObservationDeleteException e) {
            Log.e(getClass().getSimpleName(),"Could not delete derived observations ",e);
        }
        return result;
    }

    public void updatePersonTags(List<String> patientUuidList){
        Log.e(getClass().getSimpleName(),"Generating Person Tags");
        for(String patientUuid:patientUuidList){
            try {
                Patient patient = patientController.getPatientByUuid(patientUuid);
                Person relatedPerson = null;
                Person person = null;
                List<CohortMember> cohortMembers = muzimaApplication.getCohortController().getCohortMembershipByPatientUuid(patientUuid);
                if(patient != null) {
                    List<Relationship> relationships = relationshipController.getRelationshipsForPerson(patientUuid);
                    for(Relationship relationship : relationships){
                        if(relationship.getPersonA().getUuid().equals(patientUuid)) {
                            relatedPerson = relationship.getPersonB();
                        }
                        else {
                            relatedPerson = relationship.getPersonA();
                        }

                        person = personController.getPersonByUuid(relatedPerson.getUuid());
                        if(person != null){
                            List<PersonTag> tags = new ArrayList<>();
                            if (person.getPersonTags() != null) {
                                tags = new ArrayList<>(Arrays.asList(person.getPersonTags()));
                            }

                            boolean hasVisitTags = false;
                            int homeVisitTagCount = 0;
                            for (PersonTag tag : person.getPersonTags()) {
                                if(StringUtils.equals(tag.getUuid(), SIM_TAG_UUID) || StringUtils.equals(tag.getUuid(), NAO_TAG_UUID)){
                                    hasVisitTags = true;
                                    homeVisitTagCount++;
                                }
                            }

                            if(!hasVisitTags || homeVisitTagCount<4){
                                if(cohortMembers.size()>0) {
                                    CohortMember cohortMember = cohortMembers.get(0);
                                    Date membershipDate = cohortMember.getMembershipDate();

                                    List<Observation> observations = observationController.getObservationsByPatientuuidAndConceptId(person.getUuid(),2003);
                                    Collections.sort(observations, observationDateTimeComparator);

                                    if(observations.size() > 0 && homeVisitTagCount==0){
                                        if(observations.get(0).getObservationDatetime().after(membershipDate)) {
                                            if (observations.get(0).getValueCoded().getId() == 1065) {
                                                PersonTag firstAttemptTag = new PersonTag();
                                                firstAttemptTag.setName("SIM");
                                                firstAttemptTag.setUuid(SIM_TAG_UUID);
                                                tags.add(firstAttemptTag);
                                                personController.savePersonTags(firstAttemptTag);
                                            } else {
                                                PersonTag firstAttemptTag = new PersonTag();
                                                firstAttemptTag.setName("NÃO");
                                                firstAttemptTag.setUuid(NAO_TAG_UUID);
                                                tags.add(firstAttemptTag);
                                                personController.savePersonTags(firstAttemptTag);
                                            }
                                            homeVisitTagCount++;
                                        }
                                        }

                                        if(observations.size() > 1 && homeVisitTagCount==1){
                                            if(observations.get(1).getObservationDatetime().after(membershipDate)) {
                                                if (observations.get(1).getValueCoded().getId() == 1065) {
                                                    PersonTag firstAttemptTag = new PersonTag();
                                                    firstAttemptTag.setName("SIM");
                                                    firstAttemptTag.setUuid(SIM_TAG_UUID);
                                                    tags.add(firstAttemptTag);
                                                    personController.savePersonTags(firstAttemptTag);
                                                } else {
                                                    PersonTag firstAttemptTag = new PersonTag();
                                                    firstAttemptTag.setName("NÃO");
                                                    firstAttemptTag.setUuid(NAO_TAG_UUID);
                                                    tags.add(firstAttemptTag);
                                                    personController.savePersonTags(firstAttemptTag);
                                                }
                                                homeVisitTagCount++;
                                            }
                                        }

                                        if(observations.size() > 2 && homeVisitTagCount==2){
                                            if(observations.get(2).getObservationDatetime().after(membershipDate)) {
                                                if (observations.get(2).getValueCoded().getId() == 1065) {
                                                    PersonTag attemptTag = new PersonTag();
                                                    attemptTag.setName("SIM");
                                                    attemptTag.setUuid(SIM_TAG_UUID);
                                                    tags.add(attemptTag);
                                                    personController.savePersonTags(attemptTag);
                                                } else {
                                                    PersonTag attemptTag = new PersonTag();
                                                    attemptTag.setName("NÃO");
                                                    attemptTag.setUuid(NAO_TAG_UUID);
                                                    tags.add(attemptTag);
                                                    personController.savePersonTags(attemptTag);
                                                }
                                                homeVisitTagCount++;
                                            }
                                        }
                                    }
                                }
                                person.setPersonTags(tags.toArray(new PersonTag[tags.size()]));
                                personController.updatePerson(person);
                            }
                    }
                }
            } catch (IOException e) {
                Log.e(getClass().getSimpleName(), "Could not load records", e);
            } catch (PatientController.PatientLoadException e) {
                Log.e(getClass().getSimpleName(), "Could not load patient record to update update tags", e);
            } catch (ObservationController.LoadObservationException e) {
                Log.e(getClass().getSimpleName(), "Could not load observations to create tags", e);
            } catch (CohortController.CohortFetchException e) {
                Log.e(getClass().getSimpleName(), "Exception thrown while fetching cohorts.", e);
            } catch (RelationshipController.RetrieveRelationshipException e) {
                Log.e(getClass().getSimpleName(), "Exception thrown while fetching Relationships.", e);
            } catch (PersonController.PersonLoadException e) {
                Log.e(getClass().getSimpleName(), "Exception thrown while loading persons.", e);
            }
        }
    }

    public void updatePersonTagsByCohortUuids(String[] cohortUuids){
        try {
            List<Patient> patients = patientController.getPatientsForCohorts(cohortUuids);
            List<String> patientlist = new ArrayList();
            patientlist = getPatientUuids(patients);
            updatePersonTags(patientlist);
        } catch (PatientController.PatientLoadException e) {
            Log.e(getClass().getSimpleName(), "Exception thrown while loading patients.", e);
        }
    }
}
