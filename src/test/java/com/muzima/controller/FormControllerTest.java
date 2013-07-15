package com.muzima.controller;

import com.muzima.api.model.Form;
import com.muzima.api.service.FormService;
import com.muzima.search.api.util.StringUtil;

import org.apache.lucene.queryParser.ParseException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.muzima.controller.FormController.FormFetchException;
import static com.muzima.controller.FormController.FormSaveException;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FormControllerTest {
    private FormController formController;
    private FormService formService;

    @Before
    public void setup() {
        formService = mock(FormService.class);
        formController = new FormController(formService);
    }

    @Test
    public void getAllForms_shouldReturnAllAvailableForms() throws IOException, ParseException, FormFetchException {
        List<Form> forms = new ArrayList<Form>();
        when(formService.getAllForms()).thenReturn(forms);

        assertThat(formController.getAllForms(), is(forms));
    }

    @Test (expected = FormFetchException.class)
    public void getAllForms_shouldThrowFormFetchExceptionIfExceptionThrownByFormService() throws IOException, ParseException, FormFetchException {
        doThrow(new IOException()).when(formService).getAllForms();
        formController.getAllForms();

        doThrow(new ParseException()).when(formService).getAllForms();
        formController.getAllForms();
    }

    @Test
    public void downloadAllForms_shouldDownloadAllForms() throws IOException, ParseException, FormFetchException {
        List<Form> forms = new ArrayList<Form>();
        when(formService.downloadFormsByName(StringUtil.EMPTY)).thenReturn(forms);

        assertThat(formController.downloadAllForms(), is(forms));
    }

    @Test (expected = FormFetchException.class)
    public void downloadAllForms_shouldThrowFormFetchExceptionIfExceptionThrownByFormService() throws IOException, ParseException, FormFetchException {
        doThrow(new IOException()).when(formService).downloadFormsByName(StringUtil.EMPTY);
        formController.downloadAllForms();

        doThrow(new ParseException()).when(formService).downloadFormsByName(StringUtil.EMPTY);
        formController.downloadAllForms();
    }

    @Test
    public void saveForm_shouldSaveForm() throws FormSaveException, IOException {
        Form form = new Form();

        formController.saveForm(form);

        verify(formService).saveForm(form);
    }

    @Test(expected = FormSaveException.class)
    public void saveForm_shouldThrowFormSaveExceptionIfExceptionThrownByFormService() throws FormSaveException, IOException, ParseException {
        Form form = new Form();
        doThrow(new IOException()).when(formService).saveForm(form);

        formController.saveForm(form);

        verify(formService).saveForm(form);
    }
}
