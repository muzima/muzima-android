package com.muzima.service;

import android.content.SharedPreferences;

import com.muzima.MuzimaApplication;
import com.muzima.api.context.Context;
import com.muzima.api.model.Cohort;
import com.muzima.api.model.CohortData;
import com.muzima.api.model.CohortMember;
import com.muzima.api.model.Form;
import com.muzima.api.model.FormTemplate;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Patient;
import com.muzima.controller.CohortController;
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

import static com.muzima.controller.ObservationController.*;
import static com.muzima.utils.Constants.*;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.AUTHENTICATION_ERROR;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.AUTHENTICATION_SUCCESS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.CONNECTION_ERROR;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.DELETE_ERROR;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.DOWNLOAD_ERROR;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.LOAD_ERROR;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.PARSING_ERROR;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.REPLACE_ERROR;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.SAVE_ERROR;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(CustomTestRunner.class)
public class DownloadServiceTest {

    private DownloadService downloadService;
    private MuzimaApplication muzimaApplication;
    private Context muzimaContext;
    private FormController formContorller;
    private CohortController cohortController;
    private SharedPreferences sharedPref;
    private PatientController patientController;
    private ObservationController observationController;

    @Before
    public void setUp() throws Exception {
        muzimaApplication = mock(MuzimaApplication.class);
        muzimaContext = mock(Context.class);
        formContorller = mock(FormController.class);
        cohortController = mock(CohortController.class);
        patientController = mock(PatientController.class);
        observationController = mock(ObservationController.class);
        sharedPref = mock(SharedPreferences.class);

        downloadService = new DownloadService(muzimaApplication);
        when(muzimaApplication.getMuzimaContext()).thenReturn(muzimaContext);
        when(muzimaApplication.getFormController()).thenReturn(formContorller);
        when(muzimaApplication.getCohortController()).thenReturn(cohortController);
        when(muzimaApplication.getPatientController()).thenReturn(patientController);
        when(muzimaApplication.getObservationController()).thenReturn(observationController);
    }

    @Test
    public void authenticate_shouldReturnSuccessStatusIfAuthenticated() throws Exception {
        String[] credentials = new String[]{"username", "password", "url"};

        assertThat(downloadService.authenticate(credentials), is(AUTHENTICATION_SUCCESS));
    }

    @Test
    public void authenticate_shouldAuthenticateIfNotAlreadyAuthenticated() throws Exception {
        String[] credentials = new String[]{"username", "password", "url"};

        when(muzimaContext.isAuthenticated()).thenReturn(true);

        verify(muzimaContext, times(0)).authenticate(anyString(), anyString(), anyString());
        assertThat(downloadService.authenticate(credentials), is(AUTHENTICATION_SUCCESS));
    }

    @Test
    public void authenticate_shouldCallCloseSessionIfAuthenticationSucceed() throws Exception {
        String[] credentials = new String[]{"username", "password", "url"};

        downloadService.authenticate(credentials);

        verify(muzimaContext).closeSession();
    }

    @Test
    public void authenticate_shouldCallCloseSessionIfExceptionOccurred() throws Exception {
        String[] credentials = new String[]{"username", "password", "url"};

        doThrow(new ParseException()).when(muzimaContext).authenticate(credentials[0], credentials[1], credentials[2]);
        downloadService.authenticate(credentials);

        verify(muzimaContext).closeSession();
    }

    @Test
    public void authenticate_shouldReturnParsingErrorIfParsingExceptionOccurs() throws Exception {
        String[] credentials = new String[]{"username", "password", "url"};

        doThrow(new ParseException()).when(muzimaContext).authenticate(credentials[0], credentials[1], credentials[2]);

        assertThat(downloadService.authenticate(credentials), is(PARSING_ERROR));
    }

    @Test
    public void authenticate_shouldReturnConnectionErrorIfConnectionErrorOccurs() throws Exception {
        String[] credentials = new String[]{"username", "password", "url"};

        doThrow(new ConnectException()).when(muzimaContext).authenticate(credentials[0], credentials[1], credentials[2]);

        assertThat(downloadService.authenticate(credentials), is(CONNECTION_ERROR));
    }

    @Test
    public void authenticate_shouldReturnAuthenticationErrorIfAuthenticationErrorOccurs() throws Exception {
        String[] credentials = new String[]{"username", "password", "url"};

        doThrow(new IOException()).when(muzimaContext).authenticate(credentials[0], credentials[1], credentials[2]);

        assertThat(downloadService.authenticate(credentials), is(AUTHENTICATION_ERROR));
    }

    @Test
    public void downloadForms_shouldReplaceOldForms() throws Exception, FormController.FormFetchException, FormController.FormDeleteException, FormController.FormSaveException {
        List<Form> forms = new ArrayList<Form>();
        when(formContorller.downloadAllForms()).thenReturn(forms);

        downloadService.downloadForms();

        verify(formContorller).downloadAllForms();
        verify(formContorller).deleteAllForms();
        verify(formContorller).saveAllForms(forms);
    }

    @Test
    public void downloadForms_shouldReturnSuccessStatusAndDownloadCountIfSuccessful() throws Exception, FormController.FormFetchException {
        int[] result = new int[]{SUCCESS, 2};

        List<Form> forms = new ArrayList<Form>(){{
            add(new Form());
            add(new Form());
        }};
        when(formContorller.downloadAllForms()).thenReturn(forms);

        assertThat(downloadService.downloadForms(), is(result));
    }

    @Test
    public void downloadForms_shouldReturnDownloadErrorIfDownloadExceptionOccur() throws Exception, FormController.FormFetchException {
        doThrow(new FormController.FormFetchException(null)).when(formContorller).downloadAllForms();
        assertThat(downloadService.downloadForms()[0], is(DOWNLOAD_ERROR));
    }

    @Test
    public void downloadForms_shouldReturnSaveErrorIfSaveExceptionOccur() throws Exception, FormController.FormSaveException {
        doThrow(new FormController.FormSaveException(null)).when(formContorller).saveAllForms(anyList());
        assertThat(downloadService.downloadForms()[0], is(SAVE_ERROR));
    }

    @Test
    public void downloadForms_shouldReturnDeleteErrorIfDeleteExceptionOccur() throws Exception, FormController.FormDeleteException {
        doThrow(new FormController.FormDeleteException(null)).when(formContorller).deleteAllForms();
        assertThat(downloadService.downloadForms()[0], is(DELETE_ERROR));
    }

    @Test
    public void downloadFormTemplates_shouldReplaceDownloadedTemplates() throws FormController.FormFetchException, FormController.FormSaveException {
        String[] formTemplateUuids = new String[]{};
        List<FormTemplate> formTemplates = new ArrayList<FormTemplate>();
        when(formContorller.downloadFormTemplates(formTemplateUuids)).thenReturn(formTemplates);

        downloadService.downloadFormTemplates(formTemplateUuids);

        verify(formContorller).downloadFormTemplates(formTemplateUuids);
        verify(formContorller).replaceFormTemplates(formTemplates);
    }

    @Test
    public void downloadFormTemplates_shouldReturnSuccessStatusAndDownloadCountIfSuccessful() throws FormController.FormFetchException {
        int[] result = new int[]{SUCCESS, 2};

        List<FormTemplate> formTemplates = new ArrayList<FormTemplate>(){{
            add(new FormTemplate());
            add(new FormTemplate());
        }};

        String[] formIds = {};
        when(formContorller.downloadFormTemplates(formIds)).thenReturn(formTemplates);

        assertThat(downloadService.downloadFormTemplates(formIds), is(result));
    }

    @Test
    public void downloadFormTemplates_shouldReturnDownloadErrorIfDownloadExceptionOccur() throws FormController.FormFetchException {
        String[] formUuids = {};
        doThrow(new FormController.FormFetchException(null)).when(formContorller).downloadFormTemplates(formUuids);
        assertThat(downloadService.downloadFormTemplates(formUuids)[0], is(DOWNLOAD_ERROR));
    }

    @Test
    public void downloadFormTemplates_shouldReturnSaveErrorIfSaveExceptionOccur() throws FormController.FormSaveException, FormController.FormFetchException {
        String[] formUuids = {};
        doThrow(new FormController.FormSaveException(null)).when(formContorller).replaceFormTemplates(anyList());
        assertThat(downloadService.downloadFormTemplates(formUuids)[0], is(SAVE_ERROR));
    }


    @Test
    public void downloadCohort_shouldDownloadAllCohortsWhenNoPrefixesAreAvailableAndReplaceOldCohorts() throws Exception, CohortController.CohortDownloadException, CohortController.CohortDeleteException, CohortController.CohortSaveException {
        List<Cohort> cohorts = new ArrayList<Cohort>();

        when(cohortController.downloadAllCohorts()).thenReturn(cohorts);
        when(muzimaApplication.getSharedPreferences(COHORT_PREFIX_PREF, android.content.Context.MODE_PRIVATE)).thenReturn(sharedPref);
        when(sharedPref.getStringSet(COHORT_PREFIX_PREF_KEY, new HashSet<String>())).thenReturn(new HashSet<String>());

        downloadService.downloadCohorts();

        verify(cohortController).downloadAllCohorts();
        verify(cohortController).deleteAllCohorts();
        verify(cohortController).saveAllCohorts(cohorts);
        verifyNoMoreInteractions(cohortController);
    }

    @Test
    public void downloadCohort_shouldDownloadOnlyPrefixedCohortsWhenPrefixesAreAvailableAndReplaceOldCohorts() throws Exception, CohortController.CohortDownloadException, CohortController.CohortDeleteException, CohortController.CohortSaveException {
        List<Cohort> cohorts = new ArrayList<Cohort>();
        Set<String> cohortPrefixes = new HashSet<String>(){{
            add("Pre1");
        }};

        when(cohortController.downloadAllCohorts()).thenReturn(cohorts);
        when(muzimaApplication.getSharedPreferences(COHORT_PREFIX_PREF, android.content.Context.MODE_PRIVATE)).thenReturn(sharedPref);
        when(sharedPref.getStringSet(COHORT_PREFIX_PREF_KEY, new HashSet<String>())).thenReturn(cohortPrefixes);

        downloadService.downloadCohorts();

        verify(cohortController).downloadCohortsByPrefix(new ArrayList<String>(cohortPrefixes));
        verify(cohortController).deleteAllCohorts();
        verify(cohortController).saveAllCohorts(cohorts);
        verifyNoMoreInteractions(cohortController);
    }

    @Test
    public void downloadCohort_shouldReturnSuccessStatusAndDownloadCountIfSuccessful() throws Exception, CohortController.CohortDownloadException {
        List<Cohort> cohorts = new ArrayList<Cohort>() {{
            add(new Cohort());
            add(new Cohort());
        }};
        int[] result = new int[]{SUCCESS, 2};

        when(muzimaApplication.getSharedPreferences(COHORT_PREFIX_PREF, android.content.Context.MODE_PRIVATE)).thenReturn(sharedPref);
        when(sharedPref.getStringSet(COHORT_PREFIX_PREF_KEY, new HashSet<String>())).thenReturn(new HashSet<String>());
        when(cohortController.downloadAllCohorts()).thenReturn(cohorts);

        assertThat(downloadService.downloadCohorts(), is(result));
    }

    @Test
    public void downloadCohort_shouldReturnDownloadErrorIfDownloadExceptionOccurs() throws Exception, CohortController.CohortDownloadException {
        when(muzimaApplication.getSharedPreferences(COHORT_PREFIX_PREF, android.content.Context.MODE_PRIVATE)).thenReturn(sharedPref);
        when(sharedPref.getStringSet(COHORT_PREFIX_PREF_KEY, new HashSet<String>())).thenReturn(new HashSet<String>());
        doThrow(new CohortController.CohortDownloadException(null)).when(cohortController).downloadAllCohorts();

        assertThat(downloadService.downloadCohorts()[0], is(DOWNLOAD_ERROR));
    }

    @Test
    public void downloadCohort_shouldReturnSaveErrorIfSaveExceptionOccurs() throws Exception, CohortController.CohortSaveException {
        when(muzimaApplication.getSharedPreferences(COHORT_PREFIX_PREF, android.content.Context.MODE_PRIVATE)).thenReturn(sharedPref);
        when(sharedPref.getStringSet(COHORT_PREFIX_PREF_KEY, new HashSet<String>())).thenReturn(new HashSet<String>());
        doThrow(new CohortController.CohortSaveException(null)).when(cohortController).saveAllCohorts(new ArrayList<Cohort>());

        assertThat(downloadService.downloadCohorts()[0], is(SAVE_ERROR));
    }

    @Test
    public void downloadCohort_shouldReturnDeleteErrorIfDeleteExceptionOccurs() throws CohortController.CohortDeleteException {
        when(muzimaApplication.getSharedPreferences(COHORT_PREFIX_PREF, android.content.Context.MODE_PRIVATE)).thenReturn(sharedPref);
        when(sharedPref.getStringSet(COHORT_PREFIX_PREF_KEY, new HashSet<String>())).thenReturn(new HashSet<String>());
        doThrow(new CohortController.CohortDeleteException(null)).when(cohortController).deleteAllCohorts();

        assertThat(downloadService.downloadCohorts()[0], is(DELETE_ERROR));
    }

    @Test
    public void downloadPatientsForCohorts_shouldDownloadAndReplaceCohortMembersAndPatients() throws Exception, CohortController.CohortDownloadException, CohortController.CohortReplaceException, PatientController.PatientReplaceException {
        String[] cohortUuids = new String[]{"uuid1","uuid2"};
        List<CohortData> cohortDataList = new ArrayList<CohortData>(){{
            add(new CohortData(){{
                addCohortMember(new CohortMember());
                addPatient(new Patient());
            }});
            add(new CohortData(){{
                addCohortMember(new CohortMember());
                addPatient(new Patient());
            }});
        }};

        when(cohortController.downloadCohortData(cohortUuids)).thenReturn(cohortDataList);

        downloadService.downloadPatientsForCohorts(cohortUuids);

        verify(cohortController).downloadCohortData(cohortUuids);
        verify(cohortController).deleteCohortMembers(cohortUuids[0]);
        verify(cohortController).deleteCohortMembers(cohortUuids[1]);
        verify(cohortController).addCohortMembers(cohortDataList.get(0).getCohortMembers());
        verify(cohortController).addCohortMembers(cohortDataList.get(1).getCohortMembers());
        verify(patientController).replacePatients(cohortDataList.get(0).getPatients());
        verify(patientController).replacePatients(cohortDataList.get(1).getPatients());
        verifyNoMoreInteractions(cohortController);
    }

    @Test
    public void downloadPatientsForCohorts_shouldReturnSuccessStatusAndCohortAndPatinetCountIfDownloadIsSuccessful() throws Exception, CohortController.CohortDownloadException {
        String[] cohortUuids = new String[]{"uuid1","uuid2"};
        List<CohortData> cohortDataList = new ArrayList<CohortData>(){{
            add(new CohortData(){{
                addCohortMember(new CohortMember());
                addPatient(new Patient());
            }});
            add(new CohortData(){{
                addCohortMember(new CohortMember());
                addPatient(new Patient());
                addPatient(new Patient());
            }});
        }};

        when(cohortController.downloadCohortData(cohortUuids)).thenReturn(cohortDataList);

        int[] result = downloadService.downloadPatientsForCohorts(cohortUuids);
        assertThat(result[0], is(SUCCESS));
        assertThat(result[1], is(3));
        assertThat(result[2], is(2));
    }

    @Test
    public void downloadPatientsForCohorts_shouldReturnDownloadErrorIfDownloadExceptionIsThrown() throws CohortController.CohortDownloadException {
        String[] cohortUuids = new String[]{"uuid1","uuid2"};

        doThrow(new CohortController.CohortDownloadException(null)).when(cohortController).downloadCohortData(cohortUuids);

        assertThat(downloadService.downloadPatientsForCohorts(cohortUuids)[0], is(DOWNLOAD_ERROR));
    }

    @Test
    public void downloadPatientsForCohorts_shouldReturnReplaceErrorIfReplaceExceptionIsThrownForCohorts() throws CohortController.CohortReplaceException, CohortController.CohortDownloadException {
        String[] cohortUuids = new String[]{"uuid1","uuid2"};
        List<CohortData> cohortDataList = new ArrayList<CohortData>(){{
            add(new CohortData(){{
                addCohortMember(new CohortMember());
                addPatient(new Patient());
            }});
            add(new CohortData(){{
                addCohortMember(new CohortMember());
                addPatient(new Patient());
                addPatient(new Patient());
            }});
        }};

        when(cohortController.downloadCohortData(cohortUuids)).thenReturn(cohortDataList);
        doThrow(new CohortController.CohortReplaceException(null)).when(cohortController).addCohortMembers(anyList());

        assertThat(downloadService.downloadPatientsForCohorts(cohortUuids)[0], is(REPLACE_ERROR));
    }

    @Test
    public void downloadPatientsForCohorts_shouldReturnReplaceErrorIfReplaceExceptionIsThrownForPatients() throws CohortController.CohortDownloadException, PatientController.PatientReplaceException {
        String[] cohortUuids = new String[]{"uuid1","uuid2"};
        List<CohortData> cohortDataList = new ArrayList<CohortData>(){{
            add(new CohortData(){{
                addCohortMember(new CohortMember());
                addPatient(new Patient());
            }});
            add(new CohortData(){{
                addCohortMember(new CohortMember());
                addPatient(new Patient());
                addPatient(new Patient());
            }});
        }};

        when(cohortController.downloadCohortData(cohortUuids)).thenReturn(cohortDataList);
        doThrow(new PatientController.PatientReplaceException(null)).when(patientController).replacePatients(anyList());

        assertThat(downloadService.downloadPatientsForCohorts(cohortUuids)[0], is(REPLACE_ERROR));
    }

    @Test
    public void downloadObservationsForPatients_shouldDownloadObservationsForGiveCohortIdsAndSavedConcepts() throws Exception, PatientController.PatientLoadException, ObservationController.DownloadObservationException, ObservationController.ReplaceObservationException {
        String[] cohortUuids = new String[]{"uuid1","uuid2"};
        List<Patient> patients = new ArrayList<Patient>(){{
            add(new Patient(){{
                setUuid("patient1");
            }});
            add(new Patient(){{
                setUuid("patient2");
            }});
        }};
        Set<String> concepts = new HashSet<String>(){{
            add("weight");
            add("temp");
        }};
        final List<Observation> observationList1ForWeight = new ArrayList<Observation>();
        final List<Observation> observationList2ForWeight = new ArrayList<Observation>();
        final List<Observation> observationList1ForTemp = new ArrayList<Observation>();
        final List<Observation> observationList2ForTemp = new ArrayList<Observation>();

        List<Observation> allObservations = new ArrayList<Observation>(){{
            addAll(observationList1ForWeight);
            addAll(observationList2ForWeight);
            addAll(observationList1ForTemp);
            addAll(observationList2ForTemp);
        }};

        when(patientController.getPatientsForCohorts(cohortUuids)).thenReturn(patients);
        when(muzimaApplication.getSharedPreferences(Constants.CONCEPT_PREF, android.content.Context.MODE_PRIVATE)).thenReturn(sharedPref);
        when(sharedPref.getStringSet(Constants.CONCEPT_PREF_KEY, new HashSet<String>())).thenReturn(concepts);
        when(observationController.downloadObservations("patient1", "weight")).thenReturn(observationList1ForWeight);
        when(observationController.downloadObservations("patient1", "temp")).thenReturn(observationList1ForTemp);
        when(observationController.downloadObservations("patient2", "weight")).thenReturn(observationList2ForWeight);
        when(observationController.downloadObservations("patient2", "temp")).thenReturn(observationList2ForTemp);

        downloadService.downloadObservationsForPatients(cohortUuids);

        verify(observationController).downloadObservations("patient1", "weight");
        verify(observationController).downloadObservations("patient1", "temp");
        verify(observationController).downloadObservations("patient2", "weight");
        verify(observationController).downloadObservations("patient2", "temp");
        verify(observationController).replaceObservations(downloadService.getPatientUuids(patients), allObservations);
        verifyNoMoreInteractions(observationController);
    }

    @Test
    public void downloadObservationsForPatients_shouldReturnSuccessAndCountWhenDownloadingObservationsForPatient() throws Exception, PatientController.PatientLoadException, ObservationController.DownloadObservationException {
        String[] cohortUuids = new String[]{"uuid1"};
        List<Patient> patients = new ArrayList<Patient>(){{
            add(new Patient(){{
                setUuid("patient1");
            }});
        }};
        Set<String> concepts = new HashSet<String>(){{
            add("weight");
        }};

        List<Observation> allObservations = new ArrayList<Observation>(){{
            add(new Observation());
            add(new Observation());
        }};

        when(patientController.getPatientsForCohorts(cohortUuids)).thenReturn(patients);
        when(muzimaApplication.getSharedPreferences(Constants.CONCEPT_PREF, android.content.Context.MODE_PRIVATE)).thenReturn(sharedPref);
        when(sharedPref.getStringSet(Constants.CONCEPT_PREF_KEY, new HashSet<String>())).thenReturn(concepts);
        when(observationController.downloadObservations("patient1", "weight")).thenReturn(allObservations);

        int[] result = downloadService.downloadObservationsForPatients(cohortUuids);

        assertThat(result[0], is(SUCCESS));
        assertThat(result[1], is(2));
    }

    @Test
    public void downloadObservationsForPatients_shouldReturnLoadErrorWhenLoadExceptionIsThrownForObservations() throws Exception, PatientController.PatientLoadException {
        String[] cohortUuids = new String[]{};

        doThrow(new PatientController.PatientLoadException(null)).when(patientController).getPatientsForCohorts(cohortUuids);

        int[] result = downloadService.downloadObservationsForPatients(cohortUuids);
        assertThat(result[0], is(LOAD_ERROR));
    }

    @Test
    public void downloadObservationsForPatients_shouldReturnDownloadErrorWhenDownloadExceptionIsThrownForObservations() throws Exception, ObservationController.DownloadObservationException, PatientController.PatientLoadException {
        String[] cohortUuids = new String[]{"uuid1"};
        List<Patient> patients = new ArrayList<Patient>(){{
            add(new Patient(){{
                setUuid("patient1");
            }});
        }};
        Set<String> concepts = new HashSet<String>(){{
            add("weight");
        }};

        when(patientController.getPatientsForCohorts(cohortUuids)).thenReturn(patients);
        when(muzimaApplication.getSharedPreferences(Constants.CONCEPT_PREF, android.content.Context.MODE_PRIVATE)).thenReturn(sharedPref);
        when(sharedPref.getStringSet(Constants.CONCEPT_PREF_KEY, new HashSet<String>())).thenReturn(concepts);
        doThrow(new ObservationController.DownloadObservationException(null)).when(observationController).downloadObservations(anyString(),anyString());

        int[] result = downloadService.downloadObservationsForPatients(cohortUuids);
        assertThat(result[0], is(DOWNLOAD_ERROR));
    }

    @Test
    public void downloadObservationsForPatients_shouldReturnReplaceErrorWhenReplaceExceptionIsThrownForObservations() throws Exception, ReplaceObservationException {
        String[] cohortUuids = new String[]{};

        doThrow(new ObservationController.ReplaceObservationException(null)).when(observationController).replaceObservations(anyList(), anyList());

        int[] result = downloadService.downloadObservationsForPatients(cohortUuids);
        assertThat(result[0], is(REPLACE_ERROR));
    }

}
