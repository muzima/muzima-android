package com.muzima.tasks.forms;

import com.muzima.MuzimaApplication;
import com.muzima.api.context.Context;
import com.muzima.api.model.FormTemplate;
import com.muzima.controller.FormController;
import com.muzima.listeners.DownloadListener;
import com.muzima.tasks.DownloadMuzimaTask;
import com.muzima.testSupport.CustomTestRunner;

import org.apache.lucene.queryParser.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(CustomTestRunner.class)
public class DownloadFormTemplateTaskTest {
    private DownloadFormTemplateTask downloadFormTemplateTask;
    private DownloadListener<Integer[]> taskListener;
    private MuzimaApplication applicationContext;
    private FormController formController;
    private Context muzimaContext;

    String[] credentials = {"username", "password", "server"};
    String[] formsToDownload = {"uuid1", "uuid2"};

    @Before
    public void setup(){
        applicationContext = mock(MuzimaApplication.class);
        muzimaContext = mock(Context.class);
        formController = mock(FormController.class);
        taskListener = mock(DownloadListener.class);
        downloadFormTemplateTask = new DownloadFormTemplateTask(applicationContext);
        downloadFormTemplateTask.addDownloadListener(taskListener);

        when(applicationContext.getMuzimaContext()).thenReturn(muzimaContext);
        when(applicationContext.getFormController()).thenReturn(formController);
    }

    @Test
    public void execute_shouldPerformTaskIfAuthenticationIsAlreadyDone() throws IOException, FormController.FormFetchException {
        when(muzimaContext.isAuthenticated()).thenReturn(true);

        downloadFormTemplateTask.execute(credentials, formsToDownload);

        verify(formController).downloadFormTemplates(formsToDownload);
    }

    @Test
    public void execute_shouldPerformTaskIfAuthenticationIsSuccessful() throws IOException, FormController.FormFetchException, ParseException {
        when(muzimaContext.isAuthenticated()).thenReturn(false);

        downloadFormTemplateTask.execute(credentials, formsToDownload);

        verify(muzimaContext).authenticate(credentials[0],credentials[1],credentials[2]);
        verify(formController).downloadFormTemplates(formsToDownload);
    }

    @Test
    public void execute_shouldNotPerformTaskIfAuthenticationFailed() throws IOException, ParseException {
        when(muzimaContext.isAuthenticated()).thenReturn(false);
        doThrow(new ConnectException()).when(muzimaContext).authenticate(credentials[0], credentials[1], credentials[2]);

        downloadFormTemplateTask.execute(credentials, formsToDownload);

        verifyZeroInteractions(formController);
    }

    @Test
    public void execute_shouldDownloadAndReplaceFormsTemplate() throws IOException, FormController.FormFetchException, FormController.FormSaveException {
        List<FormTemplate> downloadedForms = new ArrayList<FormTemplate>();
        downloadedForms.add(new FormTemplate());
        downloadedForms.add(new FormTemplate());

        Integer[] result = new Integer[2];
        result[0] = DownloadMuzimaTask.SUCCESS;
        result[1] = downloadedForms.size();

        when(muzimaContext.isAuthenticated()).thenReturn(true);
        when(formController.downloadFormTemplates(formsToDownload)).thenReturn(downloadedForms);

        downloadFormTemplateTask.execute(credentials, formsToDownload);

        verify(formController).downloadFormTemplates(formsToDownload);
        verify(formController).replaceFormTemplates(downloadedForms);
        verify(taskListener).downloadTaskComplete(result);
    }

    @Test
    public void execute_shouldNotifyConnectionErrorIfUnableToConnectDuringAuthentication() throws IOException, ParseException {
        Integer[] result = new Integer[2];
        result[0] = DownloadMuzimaTask.CONNECTION_ERROR;
        when(muzimaContext.isAuthenticated()).thenReturn(false);
        doThrow(new ConnectException()).when(muzimaContext).authenticate(credentials[0], credentials[1], credentials[2]);

        downloadFormTemplateTask.execute(credentials, formsToDownload);

        verify(taskListener).downloadTaskComplete(result);
    }

    @Test
    public void execute_shouldNotifyParsingErrorIfUnableToParseDuringAuthentication() throws IOException, ParseException {
        Integer[] result = new Integer[2];
        result[0] = DownloadMuzimaTask.PARSING_ERROR;
        when(muzimaContext.isAuthenticated()).thenReturn(false);
        doThrow(new ParseException()).when(muzimaContext).authenticate(credentials[0], credentials[1], credentials[2]);

        downloadFormTemplateTask.execute(credentials, formsToDownload);

        verify(taskListener).downloadTaskComplete(result);
    }

    @Test
    public void execute_shouldNotifyAuthenticationErrorIfUnableToAuthenticate() throws IOException, ParseException {
        Integer[] result = new Integer[2];
        result[0] = DownloadMuzimaTask.AUTHENTICATION_ERROR;
        when(muzimaContext.isAuthenticated()).thenReturn(false);
        doThrow(new IOException()).when(muzimaContext).authenticate(credentials[0], credentials[1], credentials[2]);

        downloadFormTemplateTask.execute(credentials, formsToDownload);

        verify(taskListener).downloadTaskComplete(result);
    }

    @Test
    public void execute_shouldNotifyDownloadErrorIfUnableToDownloadForms() throws IOException, ParseException, FormController.FormFetchException {
        Integer[] result = new Integer[2];
        result[0] = DownloadMuzimaTask.DOWNLOAD_ERROR;
        when(muzimaContext.isAuthenticated()).thenReturn(false);
        doThrow(new FormController.FormFetchException(new IOException())).when(formController).downloadFormTemplates(formsToDownload);

        downloadFormTemplateTask.execute(credentials, formsToDownload);

        verify(taskListener).downloadTaskComplete(result);
    }

    @Test
    public void execute_shouldNotifySaveErrorIfUnableToSaveForms() throws IOException, FormController.FormFetchException, FormController.FormSaveException {
        Integer[] result = new Integer[2];
        result[0] = DownloadMuzimaTask.SAVE_ERROR;

        List<FormTemplate> downloadedForms = new ArrayList<FormTemplate>();
        downloadedForms.add(new FormTemplate());
        downloadedForms.add(new FormTemplate());

        when(muzimaContext.isAuthenticated()).thenReturn(false);
        when(formController.downloadFormTemplates(formsToDownload)).thenReturn(downloadedForms);
        doThrow(new FormController.FormSaveException(new IOException())).when(formController).replaceFormTemplates(downloadedForms);

        downloadFormTemplateTask.execute(credentials, formsToDownload);

        verify(taskListener).downloadTaskComplete(result);
    }
}
