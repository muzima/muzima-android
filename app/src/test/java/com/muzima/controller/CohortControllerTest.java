/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.controller;

import com.muzima.api.model.Cohort;
import com.muzima.api.model.CohortData;
import com.muzima.api.model.LastSyncTime;
import com.muzima.api.service.CohortService;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.service.SntpService;
import com.muzima.utils.StringUtils;
import org.apache.lucene.queryParser.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.muzima.api.model.APIName.DOWNLOAD_COHORTS;
import static com.muzima.api.model.APIName.DOWNLOAD_COHORTS_DATA;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class CohortControllerTest {
    private CohortController controller;
    private CohortService cohortService;
    private LastSyncTimeService lastSyncTimeService;
    private Date anotherMockDate;
    private SntpService sntpService;
    private Date mockDate;

    @Before
    public void setup() {
        cohortService = mock(CohortService.class);
        lastSyncTimeService = mock(LastSyncTimeService.class);
        sntpService = mock(SntpService.class);
        controller = new CohortController(cohortService, lastSyncTimeService, sntpService);
        LastSyncTime lastSyncTime = mock(LastSyncTime.class);
        anotherMockDate = mock(Date.class);
        mockDate = mock(Date.class);
    }

    @Test
    public void getAllCohorts_shouldReturnAllAvailableCohorts() throws IOException, CohortController.CohortFetchException {
        List<Cohort> cohorts = new ArrayList<>();
        when(cohortService.getAllCohorts()).thenReturn(cohorts);

        assertThat(controller.getAllCohorts(), is(cohorts));
    }

    @Test(expected = CohortController.CohortFetchException.class)
    public void getAllCohorts_shouldThrowCohortFetchExceptionIfExceptionThrownByCohortService() throws IOException, CohortController.CohortFetchException {
        doThrow(new IOException()).when(cohortService).getAllCohorts();
        controller.getAllCohorts();

        doThrow(new ParseException()).when(cohortService).getAllCohorts();
        controller.getAllCohorts();
    }

    @Test
    public void downloadAllCohorts_shouldReturnDownloadedCohorts() throws CohortController.CohortDownloadException, IOException {
        List<Cohort> downloadedCohorts = new ArrayList<>();
        when(cohortService.downloadCohortsByName(StringUtils.EMPTY)).thenReturn(downloadedCohorts);
        when(lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_COHORTS)).thenReturn(null);
        controller.downloadAllCohorts();

        assertThat(controller.downloadAllCohorts(), is(downloadedCohorts));
    }

    @Test(expected = CohortController.CohortDownloadException.class)
    public void downloadAllCohorts_shouldThrowCohortDownloadExceptionIfExceptionIsThrownByCohortService() throws CohortController.CohortDownloadException, IOException {
        doThrow(new IOException()).when(cohortService).downloadCohortsByNameAndSyncDate(StringUtils.EMPTY, null);
        when(lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_COHORTS)).thenReturn(null);

        controller.downloadAllCohorts();
    }

    @Test
    public void shouldSaveLastSyncTimeAfterDownloadingAllCohorts() throws Exception, CohortController.CohortDownloadException {
        ArgumentCaptor<LastSyncTime> lastSyncCaptor = ArgumentCaptor.forClass(LastSyncTime.class);
        when(lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_COHORTS)).thenReturn(anotherMockDate);
        when(sntpService.getLocalTime()).thenReturn(mockDate);

        controller.downloadAllCohorts();
        verify(lastSyncTimeService).saveLastSyncTime(lastSyncCaptor.capture());
        verify(lastSyncTimeService).getLastSyncTimeFor(DOWNLOAD_COHORTS);


        LastSyncTime setLastSyncTime = lastSyncCaptor.getValue();
        assertThat(setLastSyncTime.getApiName(), is(DOWNLOAD_COHORTS));
        assertThat(setLastSyncTime.getLastSyncDate(), is(mockDate));
        assertThat(setLastSyncTime.getParamSignature(), nullValue());
    }

    @Test
    public void shouldSaveLastSyncTimeAfterDownloadingAllCohortsWithPrefix() throws Exception, CohortController.CohortDownloadException {
        ArgumentCaptor<LastSyncTime> lastSyncCaptor = ArgumentCaptor.forClass(LastSyncTime.class);
        when(lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_COHORTS, "prefix1")).thenReturn(anotherMockDate);
        when(lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_COHORTS, "prefix2")).thenReturn(anotherMockDate);

        when(lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_COHORTS)).thenReturn(anotherMockDate);
        when(sntpService.getLocalTime()).thenReturn(mockDate);

        controller.downloadCohortsByPrefix(asList("prefix1", "prefix2"));
        verify(lastSyncTimeService, times(2)).saveLastSyncTime(lastSyncCaptor.capture());
        verify(lastSyncTimeService).getLastSyncTimeFor(DOWNLOAD_COHORTS, "prefix1");
        verify(lastSyncTimeService).getLastSyncTimeFor(DOWNLOAD_COHORTS, "prefix2");

        LastSyncTime firstSetLastSyncTime = lastSyncCaptor.getAllValues().get(0);
        LastSyncTime secondSetLastSyncTime = lastSyncCaptor.getAllValues().get(1);
        assertThat(firstSetLastSyncTime.getApiName(), is(DOWNLOAD_COHORTS));
        assertThat(firstSetLastSyncTime.getLastSyncDate(), is(mockDate));
        assertThat(firstSetLastSyncTime.getParamSignature(), is("prefix1"));
        assertThat(secondSetLastSyncTime.getApiName(), is(DOWNLOAD_COHORTS));
        assertThat(secondSetLastSyncTime.getLastSyncDate(), is(mockDate));
        assertThat(secondSetLastSyncTime.getParamSignature(), is("prefix2"));
    }

    @Test
    public void downloadCohortDataByUuid_shouldDownloadCohortByUuid() throws IOException, CohortController.CohortDownloadException {
        CohortData cohortData = new CohortData();
        String uuid = "uuid";
        when(cohortService.downloadCohortDataAndSyncDate(uuid, false, null)).thenReturn(cohortData);
        when(lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_COHORTS_DATA, uuid)).thenReturn(null);

        assertThat(controller.downloadCohortDataByUuid(uuid), is(cohortData));
    }

    @Test
    public void shouldGetLastSynchDateAndUseItWhenDownloadingData() throws IOException, CohortController.CohortDownloadException {
        CohortData cohortData = mock(CohortData.class);
        String uuid = "uuid";
        when(cohortService.downloadCohortData(uuid, false)).thenReturn(cohortData);
        when(lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_COHORTS_DATA, uuid)).thenReturn(mockDate);
        when(cohortService.downloadCohortDataAndSyncDate(uuid, false, mockDate)).thenReturn(cohortData);

        controller.downloadCohortDataByUuid(uuid);

        verify(lastSyncTimeService).getLastSyncTimeFor(DOWNLOAD_COHORTS_DATA, uuid);
        verify(cohortService,never()).downloadCohortData(uuid, false);
    }

    @Test
    public void shouldSaveLastSyncTimeOfCohortData() throws Exception, CohortController.CohortDownloadException {
        String uuid = "uuid";
        when(lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_COHORTS_DATA, uuid)).thenReturn(mockDate);
        when(sntpService.getLocalTime()).thenReturn(anotherMockDate);

        controller.downloadCohortDataByUuid(uuid);

        ArgumentCaptor<LastSyncTime> captor = ArgumentCaptor.forClass(LastSyncTime.class);
        verify(lastSyncTimeService).saveLastSyncTime(captor.capture());
        LastSyncTime savedLastSyncTime = captor.getValue();
        assertThat(savedLastSyncTime.getApiName(), is(DOWNLOAD_COHORTS_DATA));
        assertThat(savedLastSyncTime.getLastSyncDate(), is(anotherMockDate));
        assertThat(savedLastSyncTime.getParamSignature(), is(uuid));
    }

    @Test(expected = CohortController.CohortDownloadException.class)
    public void downloadCohortDataByUuid_shouldThrowCohortDownloadExceptionIfExceptionThrownByCohortService() throws IOException, CohortController.CohortDownloadException {
        String uuid = "uuid";
        doThrow(new IOException()).when(cohortService).downloadCohortDataAndSyncDate(uuid, false, null);
        when(lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_COHORTS_DATA, uuid)).thenReturn(null);

        controller.downloadCohortDataByUuid(uuid);
    }

    @Test
    public void downloadFormTemplates_shouldDownloadAllFormTemplates() throws IOException, CohortController.CohortDownloadException {
        String[] uuids = new String[]{"uuid1", "uuid2"};
        CohortData cohortData1 = new CohortData();
        CohortData cohortData2 = new CohortData();
        when(cohortService.downloadCohortDataAndSyncDate(uuids[0], false, null)).thenReturn(cohortData1);
        when(cohortService.downloadCohortDataAndSyncDate(uuids[1], false, null)).thenReturn(cohortData2);
        when(lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_COHORTS_DATA, "uuid1")).thenReturn(null);
        when(lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_COHORTS_DATA, "uuid2")).thenReturn(null);

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


        ArrayList<Cohort> agePrefixedCohortList1 = new ArrayList<>();
        agePrefixedCohortList1.add(cohort11);
        agePrefixedCohortList1.add(cohort2);

        ArrayList<Cohort> agePrefixedCohortList2 = new ArrayList<>();
        agePrefixedCohortList2.add(cohort12);
        agePrefixedCohortList2.add(cohort2);

        ArrayList<Cohort> encounterPerfixedCohortList = new ArrayList<>();
        encounterPerfixedCohortList.add(cohort3);
        encounterPerfixedCohortList.add(cohort4);

        when(cohortService.downloadCohortsByNameAndSyncDate(cohortPrefixes.get(0), mockDate)).thenReturn(agePrefixedCohortList1);
        when(cohortService.downloadCohortsByNameAndSyncDate(cohortPrefixes.get(1), anotherMockDate)).thenReturn(agePrefixedCohortList2);
        when(cohortService.downloadCohortsByNameAndSyncDate(cohortPrefixes.get(2), anotherMockDate)).thenReturn(encounterPerfixedCohortList);
        when(lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_COHORTS, cohortPrefixes.get(0))).thenReturn(anotherMockDate);
        when(lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_COHORTS, cohortPrefixes.get(1))).thenReturn(anotherMockDate);
        when(lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_COHORTS, cohortPrefixes.get(2))).thenReturn(anotherMockDate);
        when(lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_COHORTS)).thenReturn(anotherMockDate);
        when(sntpService.getLocalTime()).thenReturn(mockDate);

        List<Cohort> downloadedCohorts = controller.downloadCohortsByPrefix(cohortPrefixes);
        assertThat(downloadedCohorts.size(), is(3));
        assertTrue(downloadedCohorts.contains(cohort11));
        assertTrue(downloadedCohorts.contains(cohort3));
        assertTrue(downloadedCohorts.contains(cohort4));
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
    public void saveAllCohorts_shouldThrowCohortSaveExceptionIfExceptionThrownByCohortService() throws IOException, CohortController.CohortSaveException {
        ArrayList<Cohort> cohorts = new ArrayList<Cohort>() {{
            add(new Cohort());
        }};
        doThrow(new IOException()).when(cohortService).saveCohorts(cohorts);

        controller.saveAllCohorts(cohorts);
    }

    @Test
    public void deleteAllCohorts_shouldDeleteAllCohorts() throws IOException, CohortController.CohortDeleteException {
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
    public void deleteAllCohorts_shouldThrowCohortSaveExceptionIfExceptionThrownByCohortService() throws IOException, CohortController.CohortDeleteException {
        ArrayList<Cohort> cohorts = new ArrayList<Cohort>() {{
            add(new Cohort());
            add(new Cohort());
        }};
        when(cohortService.getAllCohorts()).thenReturn(cohorts);
        doThrow(new IOException()).when(cohortService).deleteCohorts(cohorts);

        controller.deleteAllCohorts();
    }

    @Test
    public void getTotalCohortsCount_shouldReturnEmptyListOfNoCohortsHaveBeenSynced() throws IOException, CohortController.CohortFetchException {
        when(cohortService.countAllCohorts()).thenReturn(2);
        assertThat(controller.countAllCohorts(), is(2));
    }


    @Test
    public void getSyncedCohortsCount_shouldReturnTotalNumberOfSyncedCohorts() throws IOException, CohortController.CohortFetchException {
        List<Cohort> cohorts = new ArrayList<>();
        cohorts.add(new Cohort());

        when(cohortService.getAllCohorts()).thenReturn(cohorts);
        when(cohortService.countCohortMembers(anyString())).thenReturn(2);
        assertThat(controller.countSyncedCohorts(), is(1));
    }

    @Test
    public void getSyncedCohortsCount_shouldReturnZeroIfNoCohortIsSynced() throws IOException, CohortController.CohortFetchException {
        List<Cohort> cohorts = new ArrayList<>();

        when(cohortService.getAllCohorts()).thenReturn(cohorts);

        assertThat(controller.countSyncedCohorts(), is(0));
    }

    @Test
    public void getSyncedCohorts_shouldReturnTheCohortsWhichHaveMoreThanOneMember() throws IOException, CohortController.CohortFetchException {
        Cohort cohort = new Cohort();
        when(cohortService.getAllCohorts()).thenReturn(Collections.singletonList(cohort));
        when(cohortService.countCohortMembers(anyString())).thenReturn(1);
        assertThat(controller.getSyncedCohorts(), hasItem(cohort));
    }

    @Test
    public void getSyncedCohorts_shouldNotReturnCohortsIfTheyHaveNoMembers() throws IOException, CohortController.CohortFetchException {
        Cohort cohort = new Cohort();
        when(cohortService.getAllCohorts()).thenReturn(Collections.singletonList(cohort));
        when(cohortService.countCohortMembers(anyString())).thenReturn(0);
        assertThat(controller.getSyncedCohorts().size(), is(0));
    }
}
