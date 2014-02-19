package com.muzima.controller;

import com.muzima.api.model.APIName;
import com.muzima.api.model.Cohort;
import com.muzima.api.model.CohortData;
import com.muzima.api.model.LastSyncTime;
import com.muzima.api.service.CohortService;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.search.api.util.StringUtil;
import org.apache.lucene.queryParser.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class CohortControllerTest {
    private CohortController controller;
    private CohortService cohortService;
    private LastSyncTimeService lastSyncTimeService;
    private LastSyncTime lastSyncTime;
    private Date aDate;

    @Before
    public void setup() throws IOException {
        cohortService = mock(CohortService.class);
        lastSyncTimeService = mock(LastSyncTimeService.class);
        controller = new CohortController(cohortService, lastSyncTimeService);

        lastSyncTime = mock(LastSyncTime.class);
        when(lastSyncTimeService.getLastSyncTimeFor(APIName.DOWNLOAD_COHORTS)).thenReturn(lastSyncTime);
        aDate = mock(Date.class);
        when(lastSyncTime.getLastSyncDate()).thenReturn(aDate);
    }

    @Test
    public void getAllCohorts_shouldReturnAllAvailableCohorts() throws IOException, ParseException, CohortController.CohortFetchException {
        List<Cohort> cohorts = new ArrayList<Cohort>();
        when(cohortService.getAllCohorts()).thenReturn(cohorts);

        assertThat(controller.getAllCohorts(), is(cohorts));
    }

    @Test(expected = CohortController.CohortFetchException.class)
    public void getAllCohorts_shouldThrowCohortFetchExceptionIfExceptionThrownByCohortService() throws IOException, ParseException, CohortController.CohortFetchException {
        doThrow(new IOException()).when(cohortService).getAllCohorts();
        controller.getAllCohorts();

        doThrow(new ParseException()).when(cohortService).getAllCohorts();
        controller.getAllCohorts();
    }

    @Test
    public void downloadAllCohorts_shouldReturnDownloadedCohorts() throws CohortController.CohortDownloadException, IOException {
        List<Cohort> downloadedCohorts = new ArrayList<Cohort>();
        when(cohortService.downloadCohortsByName(StringUtil.EMPTY)).thenReturn(downloadedCohorts);

        controller.downloadAllCohorts();

        assertThat(controller.downloadAllCohorts(), is(downloadedCohorts));
    }

    @Test(expected = CohortController.CohortDownloadException.class)
    public void downloadAllCohorts_shouldThrowCohortDownloadExceptionIfExceptionIsThrownByCohortService() throws CohortController.CohortDownloadException, IOException {
        doThrow(new IOException()).when(cohortService).downloadCohortsByNameAndSyncDate(StringUtil.EMPTY, aDate);

        controller.downloadAllCohorts();
    }

    @Test
    public void shouldSaveLastSyncTimeAfterDownloadingAllCohorts() throws Exception, CohortController.CohortDownloadException {
        ArgumentCaptor<LastSyncTime> lastSyncCaptor = ArgumentCaptor.forClass(LastSyncTime.class);

        controller.downloadAllCohorts();
        verify(lastSyncTimeService).saveLastSyncTime(lastSyncCaptor.capture());
        verify(lastSyncTimeService).getLastSyncTimeFor(APIName.DOWNLOAD_COHORTS);

        LastSyncTime setLastSyncTime = lastSyncCaptor.getValue();
        assertThat(setLastSyncTime.getApiName(), is(APIName.DOWNLOAD_COHORTS));
        assertThat(setLastSyncTime.getLastSyncDate(), notNullValue());
        assertThat(setLastSyncTime.getParamSignature(), nullValue());
    }

    @Test
    public void shouldSaveLastSyncTimeAfterDownloadingAllCohortsWithPrefix() throws Exception, CohortController.CohortDownloadException {
        String prefixesAsString = "prefix1|prefix2";
        ArgumentCaptor<LastSyncTime> lastSyncCaptor = ArgumentCaptor.forClass(LastSyncTime.class);
        when(lastSyncTimeService.getLastSyncTimeFor(APIName.DOWNLOAD_COHORTS, "prefix1")).thenReturn(lastSyncTime);
        when(lastSyncTimeService.getLastSyncTimeFor(APIName.DOWNLOAD_COHORTS, "prefix2")).thenReturn(lastSyncTime);

        controller.downloadCohortsByPrefix(asList(new String[]{"prefix1", "prefix2"}));
        verify(lastSyncTimeService).saveLastSyncTime(lastSyncCaptor.capture());
        verify(lastSyncTimeService).getLastSyncTimeFor(APIName.DOWNLOAD_COHORTS, "prefix1");
        verify(lastSyncTimeService).getLastSyncTimeFor(APIName.DOWNLOAD_COHORTS, "prefix2");

        LastSyncTime setLastSyncTime = lastSyncCaptor.getValue();
        assertThat(setLastSyncTime.getApiName(), is(APIName.DOWNLOAD_COHORTS));
        assertThat(setLastSyncTime.getLastSyncDate(), notNullValue());
        assertThat(setLastSyncTime.getParamSignature(), is(prefixesAsString));
    }

    @Test
    public void downloadCohortDataByUuid_shouldDownloadCohortByUuid() throws IOException, CohortController.CohortDownloadException {
        CohortData cohortData = new CohortData();
        String uuid = "uuid";
        when(cohortService.downloadCohortData(uuid, false)).thenReturn(cohortData);

        assertThat(controller.downloadCohortDataByUuid(uuid), is(cohortData));
    }

    @Test(expected = CohortController.CohortDownloadException.class)
    public void downloadFormTemplateByUuid_shouldThrowFormFetchExceptionIfExceptionThrownByFormService() throws IOException, CohortController.CohortDownloadException {
        String uuid = "uuid";
        doThrow(new IOException()).when(cohortService).downloadCohortData(uuid, false);
        controller.downloadCohortDataByUuid(uuid);
    }

    @Test
    public void downloadFormTemplates_shouldDownloadAllFormTemplates() throws IOException, CohortController.CohortDownloadException {
        String[] uuids = new String[]{"uuid1", "uuid2"};
        CohortData cohortData1 = new CohortData();
        CohortData cohortData2 = new CohortData();
        when(cohortService.downloadCohortData(uuids[0], false)).thenReturn(cohortData1);
        when(cohortService.downloadCohortData(uuids[1], false)).thenReturn(cohortData2);

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

        when(cohortService.downloadCohortsByNameAndSyncDate(cohortPrefixes.get(0), aDate)).thenReturn(agePrefixedCohortList1);
        when(cohortService.downloadCohortsByNameAndSyncDate(cohortPrefixes.get(1), aDate)).thenReturn(agePrefixedCohortList2);
        when(cohortService.downloadCohortsByNameAndSyncDate(cohortPrefixes.get(2), aDate)).thenReturn(encounterPerfixedCohortList);
        when(lastSyncTimeService.getLastSyncTimeFor(APIName.DOWNLOAD_COHORTS, cohortPrefixes.get(0))).thenReturn(lastSyncTime);
        when(lastSyncTimeService.getLastSyncTimeFor(APIName.DOWNLOAD_COHORTS, cohortPrefixes.get(1))).thenReturn(lastSyncTime);
        when(lastSyncTimeService.getLastSyncTimeFor(APIName.DOWNLOAD_COHORTS, cohortPrefixes.get(2))).thenReturn(lastSyncTime);

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

        verify(cohortService).saveCohorts(cohorts);
        verifyNoMoreInteractions(cohortService);
    }

    @Test(expected = CohortController.CohortSaveException.class)
    public void saveAllCohorts_shouldThrowCohortSaveExceptionIfExceptionThrownByCohortService() throws IOException, ParseException, CohortController.CohortSaveException {
        ArrayList<Cohort> cohorts = new ArrayList<Cohort>() {{
            add(new Cohort());
        }};
        doThrow(new IOException()).when(cohortService).saveCohorts(cohorts);

        controller.saveAllCohorts(cohorts);
    }

    @Test
    public void deleteAllCohorts_shouldDeleteAllCohorts() throws IOException, ParseException, CohortController.CohortDeleteException {
        ArrayList<Cohort> cohorts = new ArrayList<Cohort>() {{
            add(new Cohort());
            add(new Cohort());
        }};
        when(cohortService.getAllCohorts()).thenReturn(cohorts);

        controller.deleteAllCohorts();

        verify(cohortService).getAllCohorts();
        verify(cohortService).deleteCohorts(cohorts);
        verifyNoMoreInteractions(cohortService);
    }

    @Test(expected = CohortController.CohortDeleteException.class)
    public void deleteAllCohorts_shouldThrowCohortSaveExceptionIfExceptionThrownByCohortService() throws IOException, ParseException, CohortController.CohortDeleteException {
        ArrayList<Cohort> cohorts = new ArrayList<Cohort>() {{
            add(new Cohort());
            add(new Cohort());
        }};
        when(cohortService.getAllCohorts()).thenReturn(cohorts);
        doThrow(new IOException()).when(cohortService).deleteCohorts(cohorts);

        controller.deleteAllCohorts();
    }

    @Test
    public void getTotalCohortsCount_shouldReturnEmptyListOfNoCohortsHaveBeenSynced() throws IOException, ParseException, CohortController.CohortFetchException {
        when(cohortService.countAllCohorts()).thenReturn(2);
        assertThat(controller.getTotalCohortsCount(), is(2));
    }


    @Test
    public void getSyncedCohortsCount_shouldReturnTotalNumberOfSyncedCohorts() throws IOException, ParseException, CohortController.CohortFetchException {
        List<Cohort> cohorts = new ArrayList<Cohort>();
        cohorts.add(new Cohort());

        when(cohortService.getAllCohorts()).thenReturn(cohorts);
        when(cohortService.countCohortMembers(anyString())).thenReturn(2);
        assertThat(controller.getSyncedCohortsCount(), is(1));
    }

    @Test
    public void getSyncedCohortsCount_shouldReturnZeroIfNoCohortIsSynced() throws IOException, ParseException, CohortController.CohortFetchException {
        List<Cohort> cohorts = new ArrayList<Cohort>();

        when(cohortService.getAllCohorts()).thenReturn(cohorts);

        assertThat(controller.getSyncedCohortsCount(), is(0));
    }

    @Test
    public void getSyncedCohorts_shouldReturnTheCohortsWhichHaveMoreThanOneMember() throws CohortController.CohortReplaceException, IOException, ParseException, CohortController.CohortFetchException {
        Cohort cohort = new Cohort();
        when(cohortService.getAllCohorts()).thenReturn(asList(cohort));
        when(cohortService.countCohortMembers(anyString())).thenReturn(1);
        assertThat(controller.getSyncedCohorts(), hasItem(cohort));
    }

    @Test
    public void getSyncedCohorts_shouldNotReturnCohortsIfTheyHaveNoMembers() throws CohortController.CohortReplaceException, IOException, ParseException, CohortController.CohortFetchException {
        Cohort cohort = new Cohort();
        when(cohortService.getAllCohorts()).thenReturn(asList(cohort));
        when(cohortService.countCohortMembers(anyString())).thenReturn(0);
        assertThat(controller.getSyncedCohorts().size(), is(0));
    }
}
