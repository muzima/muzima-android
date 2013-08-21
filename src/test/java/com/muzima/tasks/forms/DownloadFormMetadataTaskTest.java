package com.muzima.tasks.forms;

import com.muzima.MuzimaApplication;
import com.muzima.api.context.Context;
import com.muzima.api.model.Form;
import com.muzima.controller.FormController;
import com.muzima.listeners.DownloadListener;
import com.muzima.tasks.DownloadMuzimaTask;
import com.muzima.testSupport.CustomTestRunner;

import org.apache.lucene.queryParser.ParseException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(CustomTestRunner.class)
@Ignore
public class DownloadFormMetadataTaskTest {
    private DownloadFormMetadataTask downloadFormMetadataTask;
    private DownloadListener<Integer[]> taskListener;
    private MuzimaApplication applicationContext;
    private FormController formController;
    private Context muzimaContext;

    String[] credentials = {"username", "password", "server"};

    @Before
    public void setup(){
        applicationContext = mock(MuzimaApplication.class);
        muzimaContext = mock(Context.class);
        formController = mock(FormController.class);
        taskListener = mock(DownloadListener.class);
        downloadFormMetadataTask = new DownloadFormMetadataTask(applicationContext);
        downloadFormMetadataTask.addDownloadListener(taskListener);

        when(applicationContext.getMuzimaContext()).thenReturn(muzimaContext);
        when(applicationContext.getFormController()).thenReturn(formController);
    }

    @Test
    public void execute_shouldDownloadAndReplaceForms() throws IOException, FormController.FormFetchException, FormController.FormSaveException, FormController.FormDeleteException {
        List<Form> downloadedForms = new ArrayList<Form>();
        downloadedForms.add(new Form());
        downloadedForms.add(new Form());

        Integer[] result = new Integer[2];
        result[0] = DownloadMuzimaTask.SUCCESS;
        result[1] = downloadedForms.size();

        when(muzimaContext.isAuthenticated()).thenReturn(true);
        when(formController.downloadAllForms()).thenReturn(downloadedForms);

        downloadFormMetadataTask.execute(credentials);

        verify(formController).downloadAllForms();
        verify(formController).deleteAllForms();
        verify(formController).saveAllForms(downloadedForms);
        verify(taskListener).downloadTaskComplete(result);
    }

    @Test
    public void execute_shouldNotifyDownloadErrorIfUnableToDownloadForms() throws IOException, ParseException, FormController.FormFetchException {
        Integer[] result = new Integer[2];
        result[0] = DownloadMuzimaTask.DOWNLOAD_ERROR;
        when(muzimaContext.isAuthenticated()).thenReturn(false);
        doThrow(new FormController.FormFetchException(new IOException())).when(formController).downloadAllForms();

        downloadFormMetadataTask.execute(credentials);

        verify(taskListener).downloadTaskComplete(result);
    }

    @Test
    public void execute_shouldNotifyDeleteErrorIfUnableToDeleteOldForms() throws IOException, ParseException, FormController.FormFetchException, FormController.FormDeleteException {
        Integer[] result = new Integer[2];
        result[0] = DownloadMuzimaTask.DELETE_ERROR;
        when(muzimaContext.isAuthenticated()).thenReturn(false);
        doThrow(new FormController.FormDeleteException(new IOException())).when(formController).deleteAllForms();

        downloadFormMetadataTask.execute(credentials);

        verify(taskListener).downloadTaskComplete(result);
    }

    @Test
    public void execute_shouldNotifySaveErrorIfUnableToSaveForms() throws IOException, FormController.FormFetchException, FormController.FormSaveException {
        List<Form> downloadedForms = new ArrayList<Form>();
        downloadedForms.add(new Form());
        downloadedForms.add(new Form());

        Integer[] result = new Integer[2];
        result[0] = DownloadMuzimaTask.SAVE_ERROR;

        when(muzimaContext.isAuthenticated()).thenReturn(false);
        when(formController.downloadAllForms()).thenReturn(downloadedForms);
        doThrow(new FormController.FormSaveException(new IOException())).when(formController).saveAllForms(downloadedForms);

        downloadFormMetadataTask.execute(credentials);

        verify(taskListener).downloadTaskComplete(result);
    }
}
