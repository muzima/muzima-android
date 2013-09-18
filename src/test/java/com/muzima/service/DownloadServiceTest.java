package com.muzima.service;

import com.muzima.MuzimaApplication;
import com.muzima.api.context.Context;
import com.muzima.api.model.Form;
import com.muzima.api.model.FormTemplate;
import com.muzima.controller.FormController;
import com.muzima.testSupport.CustomTestRunner;

import org.apache.lucene.queryParser.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.AUTHENTICATION_ERROR;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.AUTHENTICATION_SUCCESS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.CONNECTION_ERROR;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.DELETE_ERROR;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.DOWNLOAD_ERROR;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.PARSING_ERROR;
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
import static org.mockito.Mockito.when;

@RunWith(CustomTestRunner.class)
public class DownloadServiceTest {

    private DownloadService downloadService;
    private MuzimaApplication muzimaApplication;
    private Context muzimaContext;
    private FormController formContorller;

    @Before
    public void setUp() throws Exception {
        muzimaApplication = mock(MuzimaApplication.class);
        muzimaContext = mock(Context.class);
        formContorller = mock(FormController.class);
        downloadService = new DownloadService(muzimaApplication);
        when(muzimaApplication.getMuzimaContext()).thenReturn(muzimaContext);
        when(muzimaApplication.getFormController()).thenReturn(formContorller);
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
}
