package com.muzima.tasks.cohort;

import com.muzima.MuzimaApplication;
import com.muzima.api.context.Context;
import com.muzima.api.model.Cohort;
import com.muzima.api.model.Form;
import com.muzima.controller.CohortController;
import com.muzima.controller.FormController;
import com.muzima.listeners.DownloadListener;
import com.muzima.search.api.exception.ParseException;
import com.muzima.tasks.DownloadMuzimaTask;
import com.muzima.testSupport.CustomTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(CustomTestRunner.class)
public class DownloadCohortTaskTest {
    private DownloadCohortTask downloadCohortTask;
    private DownloadListener<Integer[]> taskListener;
    private MuzimaApplication applicationContext;
    private CohortController cohortController;
    private Context muzimaContext;

    String[] credentials = {"username", "password", "server"};

    @Before
    public void setup(){
        applicationContext = mock(MuzimaApplication.class);
        muzimaContext = mock(Context.class);
        cohortController = mock(CohortController.class);
        taskListener = mock(DownloadListener.class);
        downloadCohortTask = new DownloadCohortTask(applicationContext);
        downloadCohortTask.addDownloadListener(taskListener);

        when(applicationContext.getMuzimaContext()).thenReturn(muzimaContext);
        when(applicationContext.getCohortController()).thenReturn(cohortController);
    }

    @Test
    public void execute_shouldDownloadAndReplaceForms() throws IOException, CohortController.CohortDownloadException, CohortController.CohortDeleteException, CohortController.CohortSaveException {
        List<Cohort> downloadedCohorts = new ArrayList<Cohort>();
        downloadedCohorts.add(new Cohort());
        downloadedCohorts.add(new Cohort());

        Integer[] result = new Integer[2];
        result[0] = DownloadMuzimaTask.SUCCESS;
        result[1] = downloadedCohorts.size();

        when(muzimaContext.isAuthenticated()).thenReturn(true);
        when(cohortController.downloadAllCohorts()).thenReturn(downloadedCohorts);

        downloadCohortTask.execute(credentials);

        verify(cohortController).downloadAllCohorts();
        verify(cohortController).deleteAllCohorts();
        verify(cohortController).saveAllCohorts(downloadedCohorts);
        verify(taskListener).downloadTaskComplete(result);
    }

    @Test
    public void execute_shouldNotifyDownloadErrorIfUnableToDownloadForms() throws CohortController.CohortDownloadException, IOException {
        Integer[] result = new Integer[2];
        result[0] = DownloadMuzimaTask.DOWNLOAD_ERROR;
        when(muzimaContext.isAuthenticated()).thenReturn(false);
        doThrow(new CohortController.CohortDownloadException(new IOException())).when(cohortController).downloadAllCohorts();

        downloadCohortTask.execute(credentials);

        verify(taskListener).downloadTaskComplete(result);
    }

    @Test
    public void execute_shouldNotifyDeleteErrorIfUnableToDeleteOldForms() throws IOException, CohortController.CohortDeleteException {
        Integer[] result = new Integer[2];
        result[0] = DownloadMuzimaTask.DELETE_ERROR;
        when(muzimaContext.isAuthenticated()).thenReturn(false);
        doThrow(new CohortController.CohortDeleteException(new IOException())).when(cohortController).deleteAllCohorts();

        downloadCohortTask.execute(credentials);

        verify(taskListener).downloadTaskComplete(result);
    }

    @Test
    public void execute_shouldNotifySaveErrorIfUnableToSaveForms() throws IOException, CohortController.CohortDownloadException, CohortController.CohortSaveException {
        List<Cohort> downloadedCohorts = new ArrayList<Cohort>();
        downloadedCohorts.add(new Cohort());
        downloadedCohorts.add(new Cohort());

        Integer[] result = new Integer[2];
        result[0] = DownloadMuzimaTask.SAVE_ERROR;

        when(muzimaContext.isAuthenticated()).thenReturn(false);
        when(cohortController.downloadAllCohorts()).thenReturn(downloadedCohorts);
        doThrow(new CohortController.CohortSaveException(new IOException())).when(cohortController).saveAllCohorts(downloadedCohorts);

        downloadCohortTask.execute(credentials);

        verify(taskListener).downloadTaskComplete(result);
    }
}
