/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.service;

import android.content.SharedPreferences;
import com.muzima.MuzimaApplication;
import com.muzima.api.context.Context;
import com.muzima.api.model.Cohort;
import com.muzima.api.model.CohortData;
import com.muzima.api.model.CohortMember;
import com.muzima.api.model.Concept;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.Form;
import com.muzima.api.model.FormTemplate;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Patient;
import com.muzima.controller.CohortController;
import com.muzima.controller.ConceptController;
import com.muzima.controller.EncounterController;
import com.muzima.controller.FormController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.PatientController;
import com.muzima.testSupport.CustomTestRunner;
import com.muzima.utils.Constants;
import org.apache.lucene.queryParser.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.muzima.controller.ObservationController.ReplaceObservationException;
import static com.muzima.utils.Constants.COHORT_PREFIX_PREF;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants;
import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(CustomTestRunner.class)
public class MuzimaSyncServiceTest {

    private MuzimaSyncService muzimaSyncService;
    private MuzimaApplication muzimaApplication;
    private Context muzimaContext;
    private FormController formContorller;
    private CohortController cohortController;
    private SharedPreferences sharedPref;
    private PatientController patientController;
    private ObservationController observationController;
    private ConceptController conceptController;
    private EncounterController encounterController;
    private CohortPrefixPreferenceService prefixesPreferenceService;

    @Before
    public void setUp() throws Exception {
        muzimaApplication = mock(MuzimaApplication.class);
        muzimaContext = mock(Context.class);
        formContorller = mock(FormController.class);
        cohortController = mock(CohortController.class);
        patientController = mock(PatientController.class);
        observationController = mock(ObservationController.class);
        sharedPref = mock(SharedPreferences.class);
        conceptController = mock(ConceptController.class);
        encounterController = mock(EncounterController.class);
        prefixesPreferenceService = mock(CohortPrefixPreferenceService.class);

        when(muzimaApplication.getMuzimaContext()).thenReturn(muzimaContext);
        when(muzimaApplication.getFormController()).thenReturn(formContorller);
        when(muzimaApplication.getCohortController()).thenReturn(cohortController);
        when(muzimaApplication.getPatientController()).thenReturn(patientController);
        when(muzimaApplication.getObservationController()).thenReturn(observationController);
        when(muzimaApplication.getConceptController()).thenReturn(conceptController);
        when(muzimaApplication.getEncounterController()).thenReturn(encounterController);
        when(muzimaApplication.getCohortPrefixesPreferenceService()).thenReturn(prefixesPreferenceService);
        when(muzimaApplication.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPref);
        muzimaSyncService = new MuzimaSyncService(muzimaApplication);
    }

    @Test
    public void authenticate_shouldReturnSuccessStatusIfAuthenticated() throws Exception {
        String[] credentials = new String[]{"username", "password", "url"};

        assertThat(muzimaSyncService.authenticate(credentials), is(SyncStatusConstants.AUTHENTICATION_SUCCESS));
    }

    @Test
    public void authenticate_shouldAuthenticateIfNotAlreadyAuthenticated() throws Exception {
        String[] credentials = new String[]{"username", "password", "url"};

        when(muzimaContext.isAuthenticated()).thenReturn(true);

        verify(muzimaContext, times(0)).authenticate(anyString(), anyString(), anyString(), anyBoolean(), anyBoolean());
        assertThat(muzimaSyncService.authenticate(credentials), is(SyncStatusConstants.AUTHENTICATION_SUCCESS));
    }

    @Test
    public void authenticate_shouldCallCloseSessionIfAuthenticationSucceed() throws Exception {
        String[] credentials = new String[]{"username", "password", "url"};

        muzimaSyncService.authenticate(credentials);

        verify(muzimaContext).closeSession();
    }


    @Test
    public void authenticate_shouldCallCloseSessionIfExceptionOccurred() throws Exception {
        String[] credentials = new String[]{"username", "password", "url"};

        doThrow(new ParseException()).when(muzimaContext).authenticate(credentials[0], credentials[1], credentials[2], true, false);
        muzimaSyncService.authenticate(credentials);

        verify(muzimaContext).closeSession();
    }

    @Test
    public void authenticate_shouldReturnParsingErrorIfParsingExceptionOccurs() throws Exception {
        String[] credentials = new String[]{"username", "password", "url"};

        doThrow(new ParseException()).when(muzimaContext).authenticate(credentials[0], credentials[1], credentials[2], true, false);

        assertThat(muzimaSyncService.authenticate(credentials), is(SyncStatusConstants.PARSING_ERROR));
    }

    @Test
    public void authenticate_shouldReturnConnectionErrorIfConnectionErrorOccurs() throws Exception {
        String[] credentials = new String[]{"username", "password", "url"};

        doThrow(new ConnectException()).when(muzimaContext).authenticate(credentials[0], credentials[1], credentials[2], true, false);

        assertThat(muzimaSyncService.authenticate(credentials), is(SyncStatusConstants.CONNECTION_ERROR));
    }

    @Test
    public void authenticate_shouldReturnAuthenticationErrorIfAuthenticationErrorOccurs() throws Exception {
        String[] credentials = new String[]{"username", "password", "url"};

        doThrow(new IOException()).when(muzimaContext).authenticate(credentials[0], credentials[1], credentials[2], true, false);

        assertThat(muzimaSyncService.authenticate(credentials), is(SyncStatusConstants.AUTHENTICATION_ERROR));
    }

    @Test
    public void downloadForms_shouldReplaceOldForms() throws Exception, FormController.FormFetchException, FormController.FormDeleteException, FormController.FormSaveException {
        List<Form> forms = new ArrayList<Form>();
        when(formContorller.downloadAllForms()).thenReturn(forms);

        muzimaSyncService.downloadForms();

        verify(formContorller).downloadAllForms();
        verify(formContorller).updateAllForms(forms);
    }

    @Test
    public void downloadForms_shouldReturnSuccessStatusAndDownloadCountIfSuccessful() throws Exception, FormController.FormFetchException {
        int[] result = new int[]{SyncStatusConstants.SUCCESS, 2, 0};

        List<Form> forms = new ArrayList<Form>() {{
            add(new Form());
            add(new Form());
        }};
        when(formContorller.downloadAllForms()).thenReturn(forms);

        assertThat(muzimaSyncService.downloadForms(), is(result));
    }

    @Test
    public void downloadForms_shouldReturnDeletedFormCount() throws FormController.FormFetchException {
        int[] result = new int[]{SyncStatusConstants.SUCCESS, 2, 1};

        List<Form> downloadedForms = new ArrayList<Form>();
        Form formToDelete = new Form();
        formToDelete.setRetired(true);
        formToDelete.setUuid("123");
        Form newForm = new Form();
        newForm.setRetired(false);
        newForm.setUuid("456");
        downloadedForms.add(formToDelete);
        downloadedForms.add(new Form());
        downloadedForms.add(newForm);
        List<Form> allAvailableForms = new ArrayList<Form>();
        Form formA = new Form();
        formA.setUuid("789");
        allAvailableForms.add(formToDelete);
        allAvailableForms.add(formA);
        when(formContorller.downloadAllForms()).thenReturn(downloadedForms);
        when(formContorller.getAllAvailableForms()).thenReturn(allAvailableForms);
        assertThat(muzimaSyncService.downloadForms(), is(result));
    }

    @Test
    public void downloadForms_shouldReturnDownloadErrorIfDownloadExceptionOccur() throws Exception, FormController.FormFetchException {
        doThrow(new FormController.FormFetchException(null)).when(formContorller).downloadAllForms();
        assertThat(muzimaSyncService.downloadForms()[0], is(SyncStatusConstants.DOWNLOAD_ERROR));
    }

    @Test
    public void downloadForms_shouldReturnSaveErrorIfSaveExceptionOccur() throws Exception, FormController.FormSaveException {
        doThrow(new FormController.FormSaveException(null)).when(formContorller).updateAllForms(anyList());
        assertThat(muzimaSyncService.downloadForms()[0], is(SyncStatusConstants.SAVE_ERROR));
    }

    @Test
    public void downloadFormTemplates_shouldReplaceDownloadedTemplates() throws FormController.FormFetchException, FormController.FormSaveException {
        String[] formTemplateUuids = new String[]{};
        List<FormTemplate> formTemplates = new ArrayList<FormTemplate>();
        when(formContorller.downloadFormTemplates(formTemplateUuids)).thenReturn(formTemplates);
        SharedPreferences.Editor editor = mock(SharedPreferences.Editor.class);
        when(sharedPref.edit()).thenReturn(editor);

        muzimaSyncService.downloadFormTemplates(formTemplateUuids,true);

        verify(formContorller).downloadFormTemplates(formTemplateUuids);
        verify(formContorller).replaceFormTemplates(formTemplates);
    }

    @Test
    public void downloadFormTemplates_shouldReturnSuccessStatusAndDownloadCountIfSuccessful() throws FormController.FormFetchException {
        int[] result = new int[]{SyncStatusConstants.SUCCESS, 2, 0};

        List<FormTemplate> formTemplates = new ArrayList<FormTemplate>() {{
            FormTemplate formTemplate = new FormTemplate();
            formTemplate.setHtml("<html></html>");
            add(formTemplate);
            add(formTemplate);
        }};

        String[] formIds = {};
        when(formContorller.downloadFormTemplates(formIds)).thenReturn(formTemplates);
        SharedPreferences.Editor editor = mock(SharedPreferences.Editor.class);
        when(sharedPref.edit()).thenReturn(editor);

        assertThat(muzimaSyncService.downloadFormTemplates(formIds,true), is(result));
    }

    @Test
    public void downloadFormTemplates_shouldReturnDownloadErrorIfDownloadExceptionOccur() throws FormController.FormFetchException {
        String[] formUuids = {};
        doThrow(new FormController.FormFetchException(null)).when(formContorller).downloadFormTemplates(formUuids);
        assertThat(muzimaSyncService.downloadFormTemplates(formUuids,true)[0], is(SyncStatusConstants.DOWNLOAD_ERROR));
    }

    @Test
    public void downloadFormTemplates_shouldReturnSaveErrorIfSaveExceptionOccur() throws FormController.FormSaveException, FormController.FormFetchException {
        String[] formUuids = {};
        doThrow(new FormController.FormSaveException(null)).when(formContorller).replaceFormTemplates(anyList());
        assertThat(muzimaSyncService.downloadFormTemplates(formUuids,true)[0], is(SyncStatusConstants.SAVE_ERROR));
    }


    @Test
    public void downloadCohort_shouldDownloadAllCohortsWhenNoPrefixesAreAvailableAndReplaceOldCohorts() throws Exception, CohortController.CohortDownloadException, CohortController.CohortDeleteException, CohortController.CohortSaveException {
        List<Cohort> cohorts = new ArrayList<Cohort>();

        when(cohortController.downloadAllCohorts()).thenReturn(cohorts);
        when(muzimaApplication.getSharedPreferences(COHORT_PREFIX_PREF, android.content.Context.MODE_PRIVATE)).thenReturn(sharedPref);
        when(sharedPref.getStringSet(Constants.COHORT_PREFIX_PREF_KEY, new HashSet<String>())).thenReturn(new HashSet<String>());

        muzimaSyncService.downloadCohorts();

        verify(cohortController).downloadAllCohorts();
        verify(cohortController).saveAllCohorts(cohorts);
        verify(cohortController).deleteCohorts(new ArrayList<Cohort>());
        verifyNoMoreInteractions(cohortController);
    }

    @Test
    public void shouldDeleteVoidedCohortsWhenDownloading() throws CohortController.CohortDownloadException, CohortController.CohortSaveException, CohortController.CohortDeleteException {
        List<Cohort> cohorts = new ArrayList<Cohort>();
        Cohort aCohort = mock(Cohort.class);
        Cohort voidedCohort = mock(Cohort.class);
        when(voidedCohort.isVoided()).thenReturn(true);
        when(aCohort.isVoided()).thenReturn(false);
        cohorts.add(aCohort);
        cohorts.add(voidedCohort);

        when(cohortController.downloadAllCohorts()).thenReturn(cohorts);
        when(muzimaApplication.getSharedPreferences(COHORT_PREFIX_PREF, android.content.Context.MODE_PRIVATE)).thenReturn(sharedPref);
        when(sharedPref.getStringSet(Constants.COHORT_PREFIX_PREF_KEY, new HashSet<String>())).thenReturn(new HashSet<String>());

        muzimaSyncService.downloadCohorts();
        verify(cohortController).deleteCohorts(asList(voidedCohort));
        verify(cohortController).saveAllCohorts(asList(aCohort));
    }

    @Test
    public void downloadCohort_shouldDownloadOnlyPrefixedCohortsWhenPrefixesAreAvailableAndReplaceOldCohorts() throws Exception, CohortController.CohortDownloadException, CohortController.CohortDeleteException, CohortController.CohortSaveException {
        List<Cohort> cohorts = new ArrayList<Cohort>();
        List<String> cohortPrefixes = new ArrayList<String>() {{
            add("Pref1");
            add("Pref2");
        }};

        when(cohortController.downloadAllCohorts()).thenReturn(cohorts);
        when(muzimaApplication.getSharedPreferences(COHORT_PREFIX_PREF, android.content.Context.MODE_PRIVATE)).thenReturn(sharedPref);
        when(prefixesPreferenceService.getCohortPrefixes()).thenReturn(cohortPrefixes);

        muzimaSyncService.downloadCohorts();

        verify(cohortController).downloadCohortsByPrefix(cohortPrefixes);
        verify(cohortController).saveAllCohorts(cohorts);
        verify(cohortController).deleteCohorts(new ArrayList<Cohort>());
        verifyNoMoreInteractions(cohortController);
    }

    @Test
    public void downloadCohort_shouldReturnSuccessStatusAndDownloadCountIfSuccessful() throws Exception, CohortController.CohortDownloadException {
        List<Cohort> cohorts = new ArrayList<Cohort>() {{
            add(new Cohort());
            add(new Cohort());
        }};
        int[] result = new int[]{SyncStatusConstants.SUCCESS, 2, 0};

        when(muzimaApplication.getSharedPreferences(COHORT_PREFIX_PREF, android.content.Context.MODE_PRIVATE)).thenReturn(sharedPref);
        when(sharedPref.getStringSet(Constants.COHORT_PREFIX_PREF_KEY, new HashSet<String>())).thenReturn(new HashSet<String>());
        when(cohortController.downloadAllCohorts()).thenReturn(cohorts);

        assertThat(muzimaSyncService.downloadCohorts(), is(result));
    }

    @Test
    public void downloadCohort_shouldReturnDownloadErrorIfDownloadExceptionOccurs() throws Exception, CohortController.CohortDownloadException {
        when(muzimaApplication.getSharedPreferences(COHORT_PREFIX_PREF, android.content.Context.MODE_PRIVATE)).thenReturn(sharedPref);
        when(sharedPref.getStringSet(Constants.COHORT_PREFIX_PREF_KEY, new HashSet<String>())).thenReturn(new HashSet<String>());
        doThrow(new CohortController.CohortDownloadException(null)).when(cohortController).downloadAllCohorts();

        assertThat(muzimaSyncService.downloadCohorts()[0], is(SyncStatusConstants.DOWNLOAD_ERROR));
    }

    @Test
    public void downloadCohort_shouldReturnSaveErrorIfSaveExceptionOccurs() throws Exception, CohortController.CohortSaveException {
        when(muzimaApplication.getSharedPreferences(COHORT_PREFIX_PREF, android.content.Context.MODE_PRIVATE)).thenReturn(sharedPref);
        when(sharedPref.getStringSet(Constants.COHORT_PREFIX_PREF_KEY, new HashSet<String>())).thenReturn(new HashSet<String>());
        doThrow(new CohortController.CohortSaveException(null)).when(cohortController).saveAllCohorts(new ArrayList<Cohort>());

        assertThat(muzimaSyncService.downloadCohorts()[0], is(SyncStatusConstants.SAVE_ERROR));
    }

    @Test
    public void downloadPatientsForCohorts_shouldDownloadAndReplaceCohortMembersAndPatients() throws Exception, CohortController.CohortDownloadException, CohortController.CohortReplaceException, PatientController.PatientSaveException {
        String[] cohortUuids = new String[]{"uuid1", "uuid2"};
        List<CohortData> cohortDataList = new ArrayList<CohortData>() {{
            add(new CohortData() {{
                addCohortMember(new CohortMember());
                addPatient(new Patient());
            }});
            add(new CohortData() {{
                addCohortMember(new CohortMember());
                addPatient(new Patient());
            }});
        }};

        when(cohortController.downloadCohortData(cohortUuids)).thenReturn(cohortDataList);

        muzimaSyncService.downloadPatientsForCohorts(cohortUuids);

        verify(cohortController).downloadCohortData(cohortUuids);
        verify(cohortController).addCohortMembers(cohortDataList.get(0).getCohortMembers());
        verify(cohortController).addCohortMembers(cohortDataList.get(1).getCohortMembers());
        verify(patientController).replacePatients(cohortDataList.get(0).getPatients());
        verify(patientController).replacePatients(cohortDataList.get(1).getPatients());
        verifyNoMoreInteractions(cohortController);
    }

    @Test
    public void shouldDeleteVoidedPatientsDuringPatientDownload() throws Exception, CohortController.CohortDownloadException, PatientController.PatientDeleteException {
        String[] cohortUuids = new String[]{"uuid1", "uuid2"};
        final Patient voidedPatient = mock(Patient.class);
        when(voidedPatient.isVoided()).thenReturn(true);
        List<CohortData> cohortDataList = new ArrayList<CohortData>() {{
            add(new CohortData() {{
                addCohortMember(new CohortMember());
                addPatient(new Patient());
                addPatient(voidedPatient);
            }});
            add(new CohortData() {{
                addCohortMember(new CohortMember());
                addPatient(new Patient());
            }});
        }};

        when(cohortController.downloadCohortData(cohortUuids)).thenReturn(cohortDataList);

        muzimaSyncService.downloadPatientsForCohorts(cohortUuids);

        verify(patientController).deletePatient(asList(voidedPatient));
    }

    @Test
    public void downloadPatientsForCohorts_shouldReturnSuccessStatusAndCohortAndPatinetCountIfDownloadIsSuccessful() throws Exception, CohortController.CohortDownloadException {
        String[] cohortUuids = new String[]{"uuid1", "uuid2"};
        List<CohortData> cohortDataList = new ArrayList<CohortData>() {{
            add(new CohortData() {{
                addCohortMember(new CohortMember());
                addPatient(new Patient());
            }});
            add(new CohortData() {{
                addCohortMember(new CohortMember());
                addPatient(new Patient());
                addPatient(new Patient());
            }});
        }};

        when(cohortController.downloadCohortData(cohortUuids)).thenReturn(cohortDataList);

        int[] result = muzimaSyncService.downloadPatientsForCohorts(cohortUuids);
        assertThat(result[0], is(SyncStatusConstants.SUCCESS));
        assertThat(result[1], is(3));
        assertThat(result[2], is(2));
    }

    @Test
    public void downloadPatientsForCohorts_shouldReturnDownloadErrorIfDownloadExceptionIsThrown() throws CohortController.CohortDownloadException {
        String[] cohortUuids = new String[]{"uuid1", "uuid2"};

        doThrow(new CohortController.CohortDownloadException(null)).when(cohortController).downloadCohortData(cohortUuids);

        assertThat(muzimaSyncService.downloadPatientsForCohorts(cohortUuids)[0], is(SyncStatusConstants.DOWNLOAD_ERROR));
    }

    @Test
    public void downloadPatientsForCohorts_shouldReturnReplaceErrorIfReplaceExceptionIsThrownForCohorts() throws CohortController.CohortReplaceException, CohortController.CohortDownloadException {
        String[] cohortUuids = new String[]{"uuid1", "uuid2"};
        List<CohortData> cohortDataList = new ArrayList<CohortData>() {{
            add(new CohortData() {{
                addCohortMember(new CohortMember());
                addPatient(new Patient());
            }});
            add(new CohortData() {{
                addCohortMember(new CohortMember());
                addPatient(new Patient());
                addPatient(new Patient());
            }});
        }};

        when(cohortController.downloadCohortData(cohortUuids)).thenReturn(cohortDataList);
        doThrow(new CohortController.CohortReplaceException(null)).when(cohortController).addCohortMembers(anyList());

        assertThat(muzimaSyncService.downloadPatientsForCohorts(cohortUuids)[0], is(SyncStatusConstants.REPLACE_ERROR));
    }

    @Test
    public void downloadPatientsForCohorts_shouldReturnReplaceErrorIfReplaceExceptionIsThrownForPatients() throws CohortController.CohortDownloadException, PatientController.PatientSaveException {
        String[] cohortUuids = new String[]{"uuid1", "uuid2"};
        List<CohortData> cohortDataList = new ArrayList<CohortData>() {{
            add(new CohortData() {{
                addCohortMember(new CohortMember());
                addPatient(new Patient());
            }});
            add(new CohortData() {{
                addCohortMember(new CohortMember());
                addPatient(new Patient());
                addPatient(new Patient());
            }});
        }};

        when(cohortController.downloadCohortData(cohortUuids)).thenReturn(cohortDataList);
        doThrow(new PatientController.PatientSaveException(null)).when(patientController).replacePatients(anyList());

        assertThat(muzimaSyncService.downloadPatientsForCohorts(cohortUuids)[0], is(SyncStatusConstants.REPLACE_ERROR));
    }

    @Test
    public void downloadObservationsForPatients_shouldDownloadObservationsForGiveCohortIdsAndSavedConcepts() throws Exception, PatientController.PatientLoadException, ObservationController.DownloadObservationException, ObservationController.ReplaceObservationException, ConceptController.ConceptFetchException, ObservationController.DeleteObservationException {
        String[] cohortUuids = new String[]{"uuid1", "uuid2"};
        List<Patient> patients = new ArrayList<Patient>() {{
            add(new Patient() {{
                setUuid("patient1");
            }});
            add(new Patient() {{
                setUuid("patient2");
            }});
        }};

        List<Observation> allObservations = new ArrayList<Observation>() {{
            add(new Observation());
            add(new Observation());
        }};

        when(patientController.getPatientsForCohorts(cohortUuids)).thenReturn(patients);
        when(muzimaApplication.getSharedPreferences(Constants.CONCEPT_PREF, android.content.Context.MODE_PRIVATE)).thenReturn(sharedPref);
        List<String> patientUuids = asList(new String[]{"patient1", "patient2"});
        List<String> conceptUuids = asList(new String[]{"weight", "temp"});
        when(observationController.downloadObservationsByPatientUuidsAndConceptUuids(patientUuids, conceptUuids))
                .thenReturn(allObservations);
        Concept conceptWeight = new Concept();
        conceptWeight.setUuid("weight");
        Concept conceptTemp = new Concept();
        conceptTemp.setUuid("temp");
        when(conceptController.getConcepts()).thenReturn(asList(conceptWeight, conceptTemp));

        muzimaSyncService.downloadObservationsForPatientsByCohortUUIDs(cohortUuids,true);

        verify(observationController).downloadObservationsByPatientUuidsAndConceptUuids(patientUuids, conceptUuids);
        verify(observationController).deleteObservations(new ArrayList<Observation>());
        verify(observationController).replaceObservations(allObservations);
        verifyNoMoreInteractions(observationController);
    }

    @Test
    public void downloadObservationsForPatients_shouldReturnSuccessAndCountWhenDownloadingObservationsForPatient() throws Exception, PatientController.PatientLoadException, ObservationController.DownloadObservationException, ConceptController.ConceptFetchException {
        String[] cohortUuids = new String[]{"uuid1"};
        List<Patient> patients = new ArrayList<Patient>() {{
            add(new Patient() {{
                setUuid("patient1");
            }});
        }};

        List<Observation> allObservations = new ArrayList<Observation>() {{
            add(new Observation());
            add(new Observation());
        }};

        when(patientController.getPatientsForCohorts(cohortUuids)).thenReturn(patients);
        List<String> conceptUuids = asList("weight");
        Concept conceptWeight = new Concept();
        conceptWeight.setUuid("weight");
        when(conceptController.getConcepts()).thenReturn(asList(conceptWeight));
        when(observationController.downloadObservationsByPatientUuidsAndConceptUuids(asList("patient1"), conceptUuids))
                .thenReturn(allObservations);

        int[] result = muzimaSyncService.downloadObservationsForPatientsByCohortUUIDs(cohortUuids,true);

        assertThat(result[0], is(SyncStatusConstants.SUCCESS));
        assertThat(result[1], is(2));
    }

    @Test
    public void downloadObservationsForPatients_shouldReturnLoadErrorWhenLoadExceptionIsThrownForObservations() throws Exception, PatientController.PatientLoadException {
        String[] cohortUuids = new String[]{};

        doThrow(new PatientController.PatientLoadException(null)).when(patientController).getPatientsForCohorts(cohortUuids);

        int[] result = muzimaSyncService.downloadObservationsForPatientsByCohortUUIDs(cohortUuids,true);
        assertThat(result[0], is(SyncStatusConstants.LOAD_ERROR));
    }

    @Test
    public void downloadObservationsForPatients_shouldReturnDownloadErrorWhenDownloadExceptionIsThrownForObservations() throws Exception, ObservationController.DownloadObservationException, PatientController.PatientLoadException, ConceptController.ConceptFetchException {
        String[] cohortUuids = new String[]{"uuid1"};
        List<Patient> patients = new ArrayList<Patient>() {{
            add(new Patient() {{
                setUuid("patient1");
            }});
        }};

        List<Concept> conceptList = new ArrayList<Concept>() {{
            add(new Concept() {{
                setUuid("concept1");
            }});
        }};
        Set<String> concepts = new HashSet<String>() {{
            add("weight");
        }};

        when(patientController.getPatientsForCohorts(cohortUuids)).thenReturn(patients);
        when(conceptController.getConcepts()).thenReturn(conceptList);
        when(muzimaApplication.getSharedPreferences(Constants.CONCEPT_PREF, android.content.Context.MODE_PRIVATE)).thenReturn(sharedPref);
        when(sharedPref.getStringSet(Constants.CONCEPT_PREF_KEY, new HashSet<String>())).thenReturn(concepts);
        doThrow(new ObservationController.DownloadObservationException(null)).when(observationController).downloadObservationsByPatientUuidsAndConceptUuids(anyList(), anyList());

        int[] result = muzimaSyncService.downloadObservationsForPatientsByCohortUUIDs(cohortUuids,true);
        assertThat(result[0], is(SyncStatusConstants.DOWNLOAD_ERROR));
    }

    @Test
    public void downloadObservationsForPatients_shouldReturnReplaceErrorWhenReplaceExceptionIsThrownForObservations() throws Exception, ReplaceObservationException {
        String[] cohortUuids = new String[]{};

        doThrow(new ObservationController.ReplaceObservationException(null)).when(observationController).replaceObservations(anyList());

        int[] result = muzimaSyncService.downloadObservationsForPatientsByCohortUUIDs(cohortUuids,true);
        assertThat(result[0], is(SyncStatusConstants.REPLACE_ERROR));
    }

    @Test
    public void downloadEncountersForPatients_shouldDownloadInBatch() throws Exception, ObservationController.DownloadObservationException, PatientController.PatientLoadException, EncounterController.ReplaceEncounterException, EncounterController.DownloadEncounterException {
        String[] cohortUuids = new String[]{"uuid1"};
        List<Patient> patients = new ArrayList<Patient>() {{
            add(new Patient() {{
                setUuid("patient1");
            }});
        }};

        List<Encounter> encounters = new ArrayList<Encounter>() {{
            add(new Encounter());
        }};

        when(patientController.getPatientsForCohorts(cohortUuids)).thenReturn(patients);
        List<String> patientUuids = asList(new String[]{"patient1"});
        when(encounterController.downloadEncountersByPatientUuids(patientUuids)).thenReturn(encounters);

        muzimaSyncService.downloadEncountersForPatientsByCohortUUIDs(cohortUuids, true);

        verify(encounterController).downloadEncountersByPatientUuids(patientUuids);
        verify(encounterController).replaceEncounters(encounters);
        verifyNoMoreInteractions(observationController);
    }

    @Test
    public void shouldDeleteVoidedEncountersWhenDownloadingEncounters() throws EncounterController.DeleteEncounterException, EncounterController.DownloadEncounterException {
        String[] patientUuids = new String[]{"patientUuid1", "patientUuid2"};
        List<Encounter> encounters = new ArrayList<Encounter>();
        encounters.add(new Encounter());
        Encounter voidedEncounter = mock(Encounter.class);
        when(voidedEncounter.isVoided()).thenReturn(true);
        encounters.add(voidedEncounter);
        when(encounterController.downloadEncountersByPatientUuids(asList(patientUuids))).thenReturn(encounters);
        muzimaSyncService.downloadEncountersForPatientsByPatientUUIDs(asList(patientUuids),true);
        verify(encounterController).deleteEncounters(asList(voidedEncounter));
    }

    @Test
    public void consolidatePatients_shouldGetAllPatientsConsolidateSavePatientFromServerAndDeleteLocalPatient() throws Exception, PatientController.PatientSaveException {
        Patient localPatient = mock(Patient.class);
        Patient remotePatient = mock(Patient.class);

        when(patientController.consolidateTemporaryPatient(localPatient)).thenReturn(remotePatient);
        when(patientController.getAllPatientsCreatedLocallyAndNotSynced()).thenReturn(asList(localPatient));

        muzimaSyncService.consolidatePatients();

        verify(patientController).getAllPatientsCreatedLocallyAndNotSynced();
        verify(patientController).consolidateTemporaryPatient(localPatient);
        verify(patientController).savePatient(remotePatient);
        verify(patientController).deletePatient(localPatient);
    }

    @Test
    public void shouldUpdatePatientsThatAreNotInCohorts() throws Exception, PatientController.PatientSaveException, PatientController.PatientDownloadException {
        Patient localPatient1 = patient("patientUUID1");
        Patient localPatient2 = patient("patientUUID2");
        Patient serverPatient1 = patient("patientUUID3");
        when(patientController.getPatientsNotInCohorts()).thenReturn(asList(localPatient1, localPatient2));
        when(patientController.downloadPatientByUUID("patientUUID1")).thenReturn(serverPatient1);
        when(patientController.downloadPatientByUUID("patientUUID2")).thenReturn(null);

        muzimaSyncService.updatePatientsNotPartOfCohorts();

        verify(patientController).downloadPatientByUUID("patientUUID1");
        verify(patientController).downloadPatientByUUID("patientUUID2");
        verify(patientController).replacePatients(asList(serverPatient1));
    }

    @Test
    public void shouldDownloadAndSavePatientsGivenByUUID() throws Exception, PatientController.PatientDownloadException, PatientController.PatientSaveException {
        Patient patient1 = patient("patientUUID1");
        Patient patient2 = patient("patientUUID2");

        String[] patientUUIDs = new String[]{"patientUUID1", "patientUUID2"};

        when(patientController.downloadPatientByUUID("patientUUID1")).thenReturn(patient1);
        when(patientController.downloadPatientByUUID("patientUUID2")).thenReturn(patient2);

        int[] result = muzimaSyncService.downloadPatients(patientUUIDs);

        assertThat(result[0], is(SyncStatusConstants.SUCCESS));
        assertThat(result[1], is(2));

        verify(patientController).savePatients(asList(patient1, patient2));
    }

    @Test
    public void shouldReturnFailureIfDownloadFails() throws Exception, PatientController.PatientDownloadException, PatientController.PatientSaveException {
        Patient patient1 = patient("patientUUID1");

        String[] patientUUIDs = new String[]{"patientUUID1"};

        when(patientController.downloadPatientByUUID("patientUUID1")).thenReturn(patient1);
        doThrow(new PatientController.PatientSaveException(new Throwable())).when(patientController).savePatients(asList(patient1));

        int[] result = muzimaSyncService.downloadPatients(patientUUIDs);

        assertThat(result[0], is(SyncStatusConstants.DOWNLOAD_ERROR));
        assertThat(result[1], is(0));

        verify(patientController).savePatients(asList(patient1));
    }

    @Test
    public void shouldDeleteVoidedObservationsWhenDownloadingObservations() throws ObservationController.DeleteObservationException, ObservationController.DownloadObservationException, ReplaceObservationException, ConceptController.ConceptFetchException {
        List<String> patientUuids = asList(new String[]{"patientUuid"});
        List<Observation> observations = new ArrayList<Observation>();
        Observation anObservation = mock(Observation.class);
        when(anObservation.isVoided()).thenReturn(false);
        observations.add(anObservation);
        Observation voidedObservation = mock(Observation.class);
        when(voidedObservation.isVoided()).thenReturn(true);
        observations.add(voidedObservation);
        List<Concept> conceptList = new ArrayList<Concept>() {{
            add(new Concept() {{
                setUuid("concept1");
            }});
        }};
        when(conceptController.getConcepts()).thenReturn(conceptList);
        when(observationController.downloadObservationsByPatientUuidsAndConceptUuids
                (eq(patientUuids), eq(asList("concept1")))).thenReturn(observations);

        muzimaSyncService.downloadObservationsForPatientsByPatientUUIDs(patientUuids,true);

        verify(observationController).deleteObservations(asList(voidedObservation));
        verify(observationController).replaceObservations(asList(anObservation));
    }

    private Patient patient(String patientUUID) {
        Patient patient1 = new Patient();
        patient1.setUuid(patientUUID);
        return patient1;
    }

}
