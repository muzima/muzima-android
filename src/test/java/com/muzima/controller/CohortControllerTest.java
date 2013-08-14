package com.muzima.controller;

import com.muzima.api.model.Cohort;
import com.muzima.api.service.CohortService;
import com.muzima.search.api.util.StringUtil;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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
}
