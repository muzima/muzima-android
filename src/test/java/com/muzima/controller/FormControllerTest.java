package com.muzima.controller;

import com.muzima.api.model.Form;
import com.muzima.api.model.FormTemplate;
import com.muzima.api.model.Tag;
import com.muzima.api.service.FormService;
import com.muzima.builder.FormBuilder;
import com.muzima.builder.FormTemplateBuilder;
import com.muzima.builder.TagBuilder;
import com.muzima.search.api.util.StringUtil;

import org.apache.lucene.queryParser.ParseException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.muzima.controller.FormController.FormDeleteException;
import static com.muzima.controller.FormController.FormFetchException;
import static com.muzima.controller.FormController.FormSaveException;
import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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

    @Test(expected = FormFetchException.class)
    public void getAllForms_shouldThrowFormFetchExceptionIfExceptionThrownByFormService() throws IOException, ParseException, FormFetchException {
        doThrow(new IOException()).when(formService).getAllForms();
        formController.getAllForms();

        doThrow(new ParseException()).when(formService).getAllForms();
        formController.getAllForms();
    }

    @Test
    public void getAllFormByTags_shouldFetchAllFormsWithGivenTags() throws IOException, ParseException, FormFetchException {
        List<Form> forms = buildForms();
        when(formService.getAllForms()).thenReturn(forms);

        List<Form> formByTags = formController.getAllFormByTags(asList("tag2"));
        assertThat(formByTags.size(), is(1));
        assertThat(formByTags, hasItem(forms.get(0)));

        formByTags = formController.getAllFormByTags(asList("tag1"));
        assertThat(formByTags.size(), is(2));
        assertThat(formByTags, hasItem(forms.get(0)));
        assertThat(formByTags, hasItem(forms.get(1)));
    }

    @Test
    public void getAllFormByTags_shouldFetchAllFormsIfNoTagsAreProvided() throws IOException, ParseException, FormFetchException {
        List<Form> forms = buildForms();
        when(formService.getAllForms()).thenReturn(forms);

        List<Form> formByTags = formController.getAllFormByTags(new ArrayList<String>());
        assertThat(formByTags.size(), is(2));
    }

    @Test
    public void downloadAllForms_shouldDownloadAllForms() throws IOException, ParseException, FormFetchException {
        List<Form> forms = new ArrayList<Form>();
        when(formService.downloadFormsByName(StringUtil.EMPTY)).thenReturn(forms);

        assertThat(formController.downloadAllForms(), is(forms));
    }

    @Test(expected = FormFetchException.class)
    public void downloadAllForms_shouldThrowFormFetchExceptionIfExceptionThrownByFormService() throws IOException, ParseException, FormFetchException {
        doThrow(new IOException()).when(formService).downloadFormsByName(StringUtil.EMPTY);
        formController.downloadAllForms();

        doThrow(new ParseException()).when(formService).downloadFormsByName(StringUtil.EMPTY);
        formController.downloadAllForms();
    }

    @Test
    public void downloadFormTemplateByUuid_shouldDownloadFormByUuid() throws IOException, FormFetchException {
        FormTemplate formTemplate = new FormTemplate();
        String uuid = "uuid";
        when(formService.downloadFormTemplateByUuid(uuid)).thenReturn(formTemplate);

        assertThat(formController.downloadFormTemplateByUuid(uuid), is(formTemplate));
    }

    @Test(expected = FormFetchException.class)
    public void downloadFormTemplateByUuid_shouldThrowFormFetchExceptionIfExceptionThrownByFormService() throws IOException, FormFetchException {
        String uuid = "uuid";
        doThrow(new IOException()).when(formService).downloadFormTemplateByUuid(uuid);
        formController.downloadFormTemplateByUuid(uuid);
    }

    @Test
    public void downloadFormTemplates_shouldDownloadAllFormTemplates() throws IOException, FormFetchException {
        List<Form> forms = buildForms();
        FormTemplate formTemplate1 = new FormTemplate();
        FormTemplate formTemplate2 = new FormTemplate();
        when(formService.downloadFormTemplateByUuid(forms.get(0).getUuid())).thenReturn(formTemplate1);
        when(formService.downloadFormTemplateByUuid(forms.get(1).getUuid())).thenReturn(formTemplate2);

        List<FormTemplate> formTemplates = formController.downloadFormTemplates(new String[]{forms.get(0).getUuid(), forms.get(1).getUuid()});
        assertThat(formTemplates.size(), is(2));
        assertThat(formTemplates, hasItem(formTemplate1));
        assertThat(formTemplates, hasItem(formTemplate2));
    }

    @Test
    public void saveAllForms_shouldSaveAllForm() throws FormSaveException, IOException {
        List<Form> forms = buildForms();

        formController.saveAllForms(forms);

        verify(formService).saveForm(forms.get(0));
        verify(formService).saveForm(forms.get(1));
        verifyNoMoreInteractions(formService);
    }

    @Test(expected = FormSaveException.class)
    public void saveForm_shouldThrowFormSaveExceptionIfExceptionThrownByFormService() throws FormSaveException, IOException, ParseException {
        List<Form> forms = buildForms();
        doThrow(new IOException()).when(formService).saveForm(forms.get(0));

        formController.saveAllForms(forms);
    }

    @Test
    public void getAllTags_shouldFetchAllUsedTags() throws FormFetchException, IOException, ParseException {
        when(formService.getAllForms()).thenReturn(buildForms());

        List<Tag> allTags = formController.getAllTags();

        assertThat(allTags.size(), is(4));
        assertThat(allTags.get(0).getUuid(), is("tag1"));
        assertThat(allTags.get(1).getUuid(), is("tag2"));
        assertThat(allTags.get(2).getUuid(), is("tag3"));
        assertThat(allTags.get(3).getUuid(), is("tag4"));
    }

    @Test
    public void deleteAllForms_shouldDeleteAllForms() throws FormDeleteException, IOException, ParseException, FormFetchException {
        List<Form> forms = buildForms();
        when(formService.getAllForms()).thenReturn(forms);

        formController.deleteAllForms();

        verify(formService).getAllForms();
        verify(formService).deleteForm(forms.get(0));
        verify(formService).deleteForm(forms.get(1));
        verifyNoMoreInteractions(formService);
    }

    @Test(expected = FormDeleteException.class)
    public void deleteAllForms_shouldThrowFormSaveExceptionIfExceptionThrownByFormService() throws IOException, FormDeleteException, ParseException {
        List<Form> forms = buildForms();
        when(formService.getAllForms()).thenReturn(forms);
        doThrow(new IOException()).when(formService).deleteForm(forms.get(0));

        formController.deleteAllForms();
    }

    @Test
    public void replaceFormTemplates_shouldReplaceAnyExistingFormTemplateWithSameId() throws IOException, FormFetchException, FormSaveException {
        List<FormTemplate> newFormTemplates = buildFormTemplates();

        FormTemplate existingFormTemplate1 = FormTemplateBuilder.formTemplate().withUuid("uuid1").build();
        FormTemplate existingFormTemplate2 = FormTemplateBuilder.formTemplate().withUuid("uuid2").build();

        when(formService.getFormTemplateByUuid(newFormTemplates.get(0).getUuid())).thenReturn(existingFormTemplate1);
        when(formService.getFormTemplateByUuid(newFormTemplates.get(1).getUuid())).thenReturn(existingFormTemplate2);
        when(formService.getFormTemplateByUuid(newFormTemplates.get(2).getUuid())).thenReturn(null);

        formController.replaceFormTemplates(newFormTemplates);

        verify(formService).deleteFormTemplate(existingFormTemplate1);
        verify(formService).deleteFormTemplate(existingFormTemplate2);

        verify(formService).saveFormTemplate(newFormTemplates.get(0));
        verify(formService).saveFormTemplate(newFormTemplates.get(1));
        verify(formService).saveFormTemplate(newFormTemplates.get(2));
    }

    private List<Form> buildForms() {
        List<Form> forms = new ArrayList<Form>();
        Tag[] tags1 = new Tag[3];
        tags1[0] = TagBuilder.tag().withName("Patient").withUuid("tag1").build();
        tags1[1] = TagBuilder.tag().withName("PMTCT").withUuid("tag2").build();
        tags1[2] = TagBuilder.tag().withName("Observation").withUuid("tag3").build();

        Tag[] tags2 = new Tag[2];
        tags2[0] = tags1[0];
        tags2[1] = TagBuilder.tag().withName("AMPATH").withUuid("tag4").build();

        Form form1 = FormBuilder.form().withName("Patient Registration").withDescription("Form for patient registration").withUuid("uuid1").withTags(tags1).build();
        Form form2 = FormBuilder.form().withName("PMTCT").withDescription("Form for pmtct registration").withUuid("uuid2").withTags(tags2).build();

        forms.add(form1);
        forms.add(form2);

        return forms;
    }

    private List<FormTemplate> buildFormTemplates() {
        List<FormTemplate> formTemplates = new ArrayList<FormTemplate>();

        FormTemplate formTemplate1 = FormTemplateBuilder.formTemplate().withUuid("uuid1").withHtml("html1").withModel("{model1}").withModelJson("{modelJson1}").build();
        FormTemplate formTemplate2 = FormTemplateBuilder.formTemplate().withUuid("uuid2").withHtml("html2").withModel("{model2}").withModelJson("{modelJson2}").build();
        FormTemplate formTemplate3 = FormTemplateBuilder.formTemplate().withUuid("uuid3").withHtml("html3").withModel("{model3}").withModelJson("{modelJson3}").build();

        formTemplates.add(formTemplate1);
        formTemplates.add(formTemplate2);
        formTemplates.add(formTemplate3);

        return formTemplates;
    }
}
