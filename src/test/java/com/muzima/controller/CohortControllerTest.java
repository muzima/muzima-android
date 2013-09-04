package com.muzima.controller;

import com.muzima.api.model.Cohort;
import com.muzima.api.model.CohortData;
import com.muzima.api.model.CohortMember;
import com.muzima.api.service.CohortService;
import com.muzima.search.api.util.StringUtil;

import org.apache.lucene.queryParser.ParseException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class CohortControllerTest {
    private CohortController controller;
    private CohortService service;

    @Before
    public void setup() {
        service = mock(CohortService.class);
        controller = new CohortController(service);
    }

    @Test
    public void getAllCohorts_shouldReturnAllAvailableCohorts() throws IOException, ParseException, CohortController.CohortFetchException {
        List<Cohort> cohorts = new ArrayList<Cohort>();
        when(service.getAllCohorts()).thenReturn(cohorts);

        assertThat(controller.getAllCohorts(), is(cohorts));
    }

    @Test(expected = CohortController.CohortFetchException.class)
    public void getAllCohorts_shouldThrowCohortFetchExceptionIfExceptionThrownByCohortService() throws IOException, ParseException, CohortController.CohortFetchException {
        doThrow(new IOException()).when(service).getAllCohorts();
        controller.getAllCohorts();

        doThrow(new ParseException()).when(service).getAllCohorts();
        controller.getAllCohorts();
    }

    @Test
    public void downloadAllCohorts_shouldReturnDownloadedCohorts() throws CohortController.CohortDownloadException, IOException {
        List<Cohort> downloadedCohorts = new ArrayList<Cohort>();
        when(service.downloadCohortsByName(StringUtil.EMPTY)).thenReturn(downloadedCohorts);

        controller.downloadAllCohorts();

        assertThat(controller.downloadAllCohorts(), is(downloadedCohorts));
    }

    @Test(expected = CohortController.CohortDownloadException.class)
    public void downloadAllCohorts_shouldThrowCohortDownloadExceptionIfExceptionIsThrownByCohortService() throws CohortController.CohortDownloadException, IOException {
        doThrow(new IOException()).when(service).downloadCohortsByName(StringUtil.EMPTY);

        controller.downloadAllCohorts();
    }

    @Test
    public void downloadCohortDataByUuid_shouldDownloadCohortByUuid() throws IOException, CohortController.CohortDownloadException {
        CohortData cohortData = new CohortData();
        String uuid = "uuid";
        when(service.downloadCohortData(uuid, false)).thenReturn(cohortData);

        assertThat(controller.downloadCohortDataByUuid(uuid), is(cohortData));
    }

    @Test(expected = CohortController.CohortDownloadException.class)
    public void downloadFormTemplateByUuid_shouldThrowFormFetchExceptionIfExceptionThrownByFormService() throws IOException, CohortController.CohortDownloadException {
        String uuid = "uuid";
        doThrow(new IOException()).when(service).downloadCohortData(uuid, false);
        controller.downloadCohortDataByUuid(uuid);
    }

    @Test
    public void downloadFormTemplates_shouldDownloadAllFormTemplates() throws IOException, CohortController.CohortDownloadException {
        String[] uuids = new String[]{"uuid1", "uuid2"};
        CohortData cohortData1 = new CohortData();
        CohortData cohortData2 = new CohortData();
        when(service.downloadCohortData(uuids[0], false)).thenReturn(cohortData1);
        when(service.downloadCohortData(uuids[1], false)).thenReturn(cohortData2);

        List<CohortData> allCohortData = controller.downloadCohortData(uuids);
        assertThat(allCohortData.size(), is(2));
        assertThat(allCohortData, hasItem(cohortData1));
        assertThat(allCohortData, hasItem(cohortData2));
    }

    @Test
    public void downloadCohortsByPrefix_shouldDownloadAllCohortsForTheGivenPrefixes() throws IOException, CohortController.CohortDownloadException {
        List<String> cohortPrefixes = new ArrayList<String>() {{
            add("Age");
            add("age");
            add("Encounter");
        }};

        Cohort cohort11 = new Cohort() {{
            setUuid("uuid1");
            setName("Age between 20 and 30");
        }};
        Cohort cohort12 = new Cohort() {{
            setUuid("uuid1");
            setName("Age between 20 and 30");
        }};

        Cohort cohort2 = new Cohort() {{
            setUuid("uuid2");
            setName("Patients with age over 65");
        }};
        Cohort cohort3 = new Cohort() {{
            setUuid("uuid3");
            setName("Encounter 1");
        }};
        Cohort cohort4 = new Cohort() {{
            setUuid("uuid4");
            setName("Encounter 2");
        }};


        ArrayList<Cohort> agePrefixedCohortList1 = new ArrayList<Cohort>();
        agePrefixedCohortList1.add(cohort11);
        agePrefixedCohortList1.add(cohort2);

        ArrayList<Cohort> agePrefixedCohortList2 = new ArrayList<Cohort>();
        agePrefixedCohortList2.add(cohort12);
        agePrefixedCohortList2.add(cohort2);

        ArrayList<Cohort> encounterPerfixedCohortList = new ArrayList<Cohort>();
        encounterPerfixedCohortList.add(cohort3);
        encounterPerfixedCohortList.add(cohort4);

        when(service.downloadCohortsByName(cohortPrefixes.get(0))).thenReturn(agePrefixedCohortList1);
        when(service.downloadCohortsByName(cohortPrefixes.get(1))).thenReturn(agePrefixedCohortList2);
        when(service.downloadCohortsByName(cohortPrefixes.get(2))).thenReturn(encounterPerfixedCohortList);

        assertThat(controller.downloadCohortsByPrefix(cohortPrefixes).size(), is(3));
        assertTrue(controller.downloadCohortsByPrefix(cohortPrefixes).contains(cohort11));
        assertTrue(controller.downloadCohortsByPrefix(cohortPrefixes).contains(cohort3));
        assertTrue(controller.downloadCohortsByPrefix(cohortPrefixes).contains(cohort4));
    }

    @Test
    public void saveAllCohorts_shouldSaveAllCohorts() throws CohortController.CohortSaveException, IOException {
        ArrayList<Cohort> cohorts = new ArrayList<Cohort>() {{
            add(new Cohort());
            add(new Cohort());
            add(new Cohort());
        }};

        controller.saveAllCohorts(cohorts);

        verify(service).saveCohorts(cohorts);
        verifyNoMoreInteractions(service);
    }

    @Test(expected = CohortController.CohortSaveException.class)
    public void saveAllCohorts_shouldThrowCohortSaveExceptionIfExceptionThrownByCohortService() throws IOException, ParseException, CohortController.CohortSaveException {
        ArrayList<Cohort> cohorts = new ArrayList<Cohort>() {{
            add(new Cohort());
        }};
        doThrow(new IOException()).when(service).saveCohorts(cohorts);

        controller.saveAllCohorts(cohorts);
    }

    @Test
    public void deleteAllCohorts_shouldDeleteAllCohorts() throws IOException, ParseException, CohortController.CohortDeleteException {
        ArrayList<Cohort> cohorts = new ArrayList<Cohort>() {{
            add(new Cohort());
            add(new Cohort());
        }};
        when(service.getAllCohorts()).thenReturn(cohorts);

        controller.deleteAllCohorts();

        verify(service).getAllCohorts();
        verify(service).deleteCohorts(cohorts);
        verifyNoMoreInteractions(service);
    }

    @Test(expected = CohortController.CohortDeleteException.class)
    public void deleteAllCohorts_shouldThrowCohortSaveExceptionIfExceptionThrownByCohortService() throws IOException, ParseException, CohortController.CohortDeleteException {
        ArrayList<Cohort> cohorts = new ArrayList<Cohort>() {{
            add(new Cohort());
            add(new Cohort());
        }};
        when(service.getAllCohorts()).thenReturn(cohorts);
        doThrow(new IOException()).when(service).deleteCohorts(cohorts);

        controller.deleteAllCohorts();
    }

    @Test
    public void getTotalCohortsCount_shouldReturnEmptyListOfNoCohortsHaveBeenSynced() throws IOException, ParseException, CohortController.CohortFetchException {
        when(service.countAllCohorts()).thenReturn(2);
        assertThat(controller.getTotalCohortsCount(), is(2));
    }


    @Test
    public void getSyncedCohortsCount_shouldReturnTotalNumberOfSyncedCohorts() throws IOException, ParseException, CohortController.CohortFetchException {
        List<Cohort> cohorts = new ArrayList<Cohort>();
        cohorts.add(new Cohort());

        when(service.getAllCohorts()).thenReturn(cohorts);
        when(service.countCohortMembers(anyString())).thenReturn(2);
        assertThat(controller.getSyncedCohortsCount(), is(1));
    }

    @Test
    public void getSyncedCohortsCount_shouldReturnZeroIfNoCohortIsSynced() throws IOException, ParseException, CohortController.CohortFetchException {
        List<Cohort> cohorts = new ArrayList<Cohort>();

        when(service.getAllCohorts()).thenReturn(cohorts);

        assertThat(controller.getSyncedCohortsCount(), is(0));
    }

    @Test
    public void getSyncedCohorts_shouldReturnTheCohortsWhichHaveMoreThanOneMember() throws CohortController.CohortReplaceException, IOException, ParseException, CohortController.CohortFetchException {
        Cohort cohort = new Cohort();
        when(service.getAllCohorts()).thenReturn(asList(cohort));
        when(service.countCohortMembers(anyString())).thenReturn(1);
        assertThat(controller.getSyncedCohorts(), hasItem(cohort));
    }

    @Test
    public void getSyncedCohorts_shouldNotReturnCohortsIfTheyHaveNoMembers() throws CohortController.CohortReplaceException, IOException, ParseException, CohortController.CohortFetchException {
        Cohort cohort = new Cohort();
        when(service.getAllCohorts()).thenReturn(asList(cohort));
        when(service.countCohortMembers(anyString())).thenReturn(0);
        assertThat(controller.getSyncedCohorts().size(), is(0));
    }
}
