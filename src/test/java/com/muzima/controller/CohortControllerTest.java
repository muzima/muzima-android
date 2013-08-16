package com.muzima.controller;

import com.muzima.api.model.Cohort;
import com.muzima.api.service.CohortService;
import com.muzima.search.api.util.StringUtil;

import org.apache.lucene.queryParser.ParseException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class CohortControllerTest {
    private CohortController cohortController;
    private CohortService cohortService;

    @Before
    public void setup(){
        cohortService = mock(CohortService.class);
        cohortController = new CohortController(cohortService);
    }

    @Test
    public void getAllCohorts_shouldReturnAllAvailableCohorts() throws IOException, ParseException, CohortController.CohortFetchException {
        List<Cohort> cohorts = new ArrayList<Cohort>();
        when(cohortService.getAllCohorts()).thenReturn(cohorts);

        assertThat(cohortController.getAllCohorts(), is(cohorts));
    }

    @Test(expected = CohortController.CohortFetchException.class)
    public void getAllCohorts_shouldThrowCohortFetchExceptionIfExceptionThrownByCohortService() throws IOException, ParseException, CohortController.CohortFetchException {
        doThrow(new IOException()).when(cohortService).getAllCohorts();
        cohortController.getAllCohorts();

        doThrow(new ParseException()).when(cohortService).getAllCohorts();
        cohortController.getAllCohorts();
    }

    @Test
    public void downloadAllCohorts_shouldReturnDownloadedCohorts() throws CohortController.CohortDownloadException, IOException {
        List<Cohort> downloadedCohorts = new ArrayList<Cohort>();
        when(cohortService.downloadCohortsByName(StringUtil.EMPTY)).thenReturn(downloadedCohorts);

        cohortController.downloadAllCohorts();

        assertThat(cohortController.downloadAllCohorts(), is(downloadedCohorts));
    }

    @Test(expected = CohortController.CohortDownloadException.class)
    public void downloadAllCohorts_shouldThrowCohortDownloadExceptionIfExceptionIsThrownByCohortService() throws CohortController.CohortDownloadException, IOException {
        doThrow(new IOException()).when(cohortService).downloadCohortsByName(StringUtil.EMPTY);

        cohortController.downloadAllCohorts();
    }

    @Test
    public void saveAllCohorts_shouldSaveAllCohorts() throws CohortController.CohortSaveException, IOException {
        ArrayList<Cohort> cohorts = new ArrayList<Cohort>(){{
            add(new Cohort());
            add(new Cohort());
            add(new Cohort());
        }};

        cohortController.saveAllCohorts(cohorts);

        verify(cohortService, times(cohorts.size())).saveCohort(any(Cohort.class));
        verifyNoMoreInteractions(cohortService);
    }

    @Test(expected = CohortController.CohortSaveException.class)
    public void saveAllCohorts_shouldThrowCohortSaveExceptionIfExceptionThrownByCohortService() throws IOException, ParseException, CohortController.CohortSaveException {
        ArrayList<Cohort> cohorts = new ArrayList<Cohort>(){{
            add(new Cohort());
        }};
        doThrow(new IOException()).when(cohortService).saveCohort(cohorts.get(0));

        cohortController.saveAllCohorts(cohorts);
    }

    @Test
    public void deleteAllCohorts_shouldDeleteAllCohorts() throws IOException, ParseException, CohortController.CohortDeleteException {
        ArrayList<Cohort> cohorts = new ArrayList<Cohort>(){{
            add(new Cohort());
            add(new Cohort());
        }};
        when(cohortService.getAllCohorts()).thenReturn(cohorts);

        cohortController.deleteAllCohorts();

        verify(cohortService).getAllCohorts();
        verify(cohortService, times(2)).deleteCohort(any(Cohort.class));
        verifyNoMoreInteractions(cohortService);
    }

    @Test(expected = CohortController.CohortDeleteException.class)
    public void deleteAllCohorts_shouldThrowCohortSaveExceptionIfExceptionThrownByCohortService() throws IOException, ParseException, CohortController.CohortDeleteException {
        ArrayList<Cohort> cohorts = new ArrayList<Cohort>(){{
            add(new Cohort());
            add(new Cohort());
        }};
        when(cohortService.getAllCohorts()).thenReturn(cohorts);
        doThrow(new IOException()).when(cohortService).deleteCohort(cohorts.get(0));

        cohortController.deleteAllCohorts();
    }
}
