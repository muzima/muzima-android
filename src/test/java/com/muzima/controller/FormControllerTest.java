package com.muzima.controller;

import com.muzima.api.model.Form;
import com.muzima.api.model.FormData;
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

import static com.muzima.controller.FormController.*;
import static com.muzima.utils.Constants.*;
import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

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
    public void getTotalFormCount_shouldReturnTotalAvailableForms() throws IOException, ParseException, FormFetchException {
        when(formService.countAllForms()).thenReturn(2);

        assertThat(formController.getTotalFormCount(), is(2));
    }

    @Test
    public void getAllFormByTags_shouldFetchAllFormsWithGivenTags() throws IOException, ParseException, FormFetchException {
        List<Form> forms = buildForms();
        when(formService.getAllForms()).thenReturn(forms);

        List<Form> formByTags = formController.getAllFormByTags(asList("tag2"));
        assertThat(formByTags.size(), is(2));
        assertThat(formByTags, hasItem(forms.get(0)));
        assertThat(formByTags, hasItem(forms.get(2)));

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
        assertThat(formByTags.size(), is(5));
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

        verify(formService).saveForms(forms);
        verifyNoMoreInteractions(formService);
    }

    @Test(expected = FormSaveException.class)
    public void saveAllForms_shouldThrowFormSaveExceptionIfExceptionThrownByFormService() throws FormSaveException, IOException {
        List<Form> forms = buildForms();
        doThrow(new IOException()).when(formService).saveForms(forms);

        formController.saveAllForms(forms);
    }

    @Test
    public void getAllTags_shouldFetchAllUsedTags() throws FormFetchException, IOException, ParseException {
        when(formService.getAllForms()).thenReturn(buildForms());

        List<Tag> allTags = formController.getAllTags();

        assertThat(allTags.size(), is(5));
        assertThat(allTags.get(0).getUuid(), is("tag1"));
        assertThat(allTags.get(1).getUuid(), is("tag2"));
        assertThat(allTags.get(2).getUuid(), is("tag3"));
        assertThat(allTags.get(3).getUuid(), is("tag4"));
        assertThat(allTags.get(4).getUuid(), is("tag5"));
    }

    @Test
    public void deleteAllForms_shouldDeleteAllForms() throws FormDeleteException, IOException, ParseException, FormFetchException {
        List<Form> forms = buildForms();
        when(formService.getAllForms()).thenReturn(forms);

        formController.deleteAllForms();

        verify(formService).getAllForms();
        verify(formService).deleteForms(forms);
        verifyNoMoreInteractions(formService);
    }

    @Test(expected = FormDeleteException.class)
    public void deleteAllForms_shouldThrowFormSaveExceptionIfExceptionThrownByFormService() throws IOException, FormDeleteException, ParseException {
        List<Form> forms = buildForms();
        when(formService.getAllForms()).thenReturn(forms);
        doThrow(new IOException()).when(formService).deleteForms(forms);

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

    @Test
    public void getAllDownloadedForms_shouldReturnOnlyDownloadedForms() throws IOException, ParseException, FormFetchException {
        List<Form> forms = buildForms();
        List<FormTemplate> formTemplates = buildFormTemplates();

        when(formService.getAllForms()).thenReturn(forms);
        when(formService.isFormTemplateDownloaded(anyString())).thenReturn(true);

        List<Form> allDownloadedForms = formController.getAllDownloadedFormsByTags(null);

        assertThat(allDownloadedForms.size(), is(5));
    }

    @Test
    public void getAllDownloadedForms_shouldReturnNoFormsIfNoTemplateIsDownloaded() throws IOException, ParseException, FormFetchException {
        List<Form> forms = buildForms();
        List<FormTemplate> formTemplates = buildFormTemplates();

        when(formService.getAllForms()).thenReturn(forms);
        when(formService.isFormTemplateDownloaded(anyString())).thenReturn(false);

        List<Form> allDownloadedForms = formController.getAllDownloadedFormsByTags(null);

        assertThat(allDownloadedForms.size(), is(0));
    }

    @Test
    public void isFormDownloaded_shouldReturnTrueIfFromIsDownloaded() throws IOException, ParseException, FormFetchException {
        List<Form> forms = buildForms();
        List<FormTemplate> formTemplates = buildFormTemplates();

        when(formService.isFormTemplateDownloaded(anyString())).thenReturn(true);

        assertThat(formController.isFormDownloaded(forms.get(0)), is(true));
    }

    @Test
    public void isFormDownloaded_shouldReturnFalseIfFromIsNotDownloaded() throws IOException, ParseException, FormFetchException {
        List<Form> forms = buildForms();
        List<FormTemplate> formTemplates = buildFormTemplates();

        when(formService.isFormTemplateDownloaded(anyString())).thenReturn(false);

        assertThat(formController.isFormDownloaded(forms.get(0)), is(false));
    }

    @Test
    public void getFormTemplateByUuid_shouldReturnForm() throws IOException, FormFetchException {
        List<FormTemplate> formTemplates = buildFormTemplates();
        String uuid = formTemplates.get(0).getUuid();
        when(formService.getFormTemplateByUuid(uuid)).thenReturn(formTemplates.get(0));

        assertThat(formController.getFormTemplateByUuid(uuid), is(formTemplates.get(0)));
    }

    @Test
    public void getFormByUuid_shouldReturnForm() throws IOException, FormFetchException {
        List<Form> forms = buildForms();
        String uuid = forms.get(0).getUuid();
        when(formService.getFormByUuid(uuid)).thenReturn(forms.get(0));

        assertThat(formController.getFormByUuid(uuid), is(forms.get(0)));
    }

    @Test
    public void getFormDataByUuid_shouldReturnFormDataForAGivenId() throws Exception, FormDataFetchException {
        FormData formData = new FormData();
        String uuid = "uuid";

        when(formService.getFormDataByUuid(uuid)).thenReturn(formData);

        assertThat(formController.getFormDataByUuid(uuid), is(formData));
    }

    @Test(expected = FormController.FormDataFetchException.class)
    public void getFormDataByUuid_shouldThrowFormDataFetchExceptionIfFormServiceThrowAnException() throws Exception, FormDataFetchException {
        String uuid = "uuid";
        doThrow(new IOException()).when(formService).getFormDataByUuid(uuid);

        formController.getFormDataByUuid(uuid);
    }

    @Test
    public void saveFormData_shouldSaveFormData() throws Exception, FormDataSaveException {
        FormData formData = new FormData();

        formController.saveFormData(formData);

        verify(formService).saveFormData(formData);
    }

    @Test(expected = FormDataSaveException.class)
    public void saveFormData_shouldThrowFormDataSaveExceptionIfExceptionThrownByFormService() throws Exception, FormDataSaveException {
        FormData formData = new FormData();

        doThrow(new IOException()).when(formService).saveFormData(formData);
        formController.saveFormData(formData);
    }

    @Test
    public void getAllFormData_shouldReturnListOfAllFormDatas() throws Exception, FormDataFetchException {
        FormData formData = new FormData();
        String status = "draft";

        when(formService.getAllFormData(status)).thenReturn(asList(formData));

        assertThat(formController.getAllFormData(status).size(), is(1));
        assertThat(formController.getAllFormData(status), hasItem(formData));
    }

    @Test
    public void getAllFormDataByPatientUuid_shouldReturnAllFormDataForPatientAndGivenStatus() throws Exception, FormDataFetchException {
        List<FormData> formDataList = new ArrayList<FormData>();
        String patientUuid = "patientUuid";
        String status = "status";

        when(formService.getFormDataByPatient(patientUuid, status)).thenReturn(formDataList);

        assertThat(formController.getAllFormDataByPatientUuid(patientUuid, status), is(formDataList));
    }

    @Test (expected = FormDataFetchException.class)
    public void getAllFormDataByPatientUuid_shouldThrowFormDataFetchExpetionIfExceptionThrownByService() throws Exception, FormDataFetchException {
        doThrow(new IOException()).when(formService).getFormDataByPatient(anyString(), anyString());
        formController.getAllFormDataByPatientUuid("", "");
    }

    @Test
    public void getAllIncompleteForms_shouldReturnAllIncompleteForms() throws Exception, FormFetchException {
        final Form form1 = new Form();
        final Form form2 = new Form();
        List<Form> forms = new ArrayList<Form>(){{
            add(form1);
            add(form2);
        }};

        final FormData formData1 = new FormData();
        formData1.setTemplateUuid("form1Uuid");
        final FormData formData2 = new FormData();
        formData2.setTemplateUuid("form2Uuid");

        List<FormData> formDataList = new ArrayList<FormData>(){{
            add(formData1);
            add(formData2);
        }};

        when(formService.getAllFormData(STATUS_INCOMPLETE)).thenReturn(formDataList);
        when(formService.getFormByUuid(formData1.getTemplateUuid())).thenReturn(form1);
        when(formService.getFormByUuid(formData2.getTemplateUuid())).thenReturn(form2);

        assertThat(formController.getAllIncompleteForms(), is(forms));
    }

    @Test (expected = FormFetchException.class)
    public void getAllIncompleteForms_shouldThrowFormFetchExceptionIfExceptionThrownByService() throws Exception, FormFetchException {
        doThrow(new IOException()).when(formService).getAllFormData(anyString());

        formController.getAllIncompleteForms();
    }

    @Test
    public void getAllIncompleteFormsForPatientUuid_shouldReturnAllIncompleteFormsForGivenPatient() throws Exception, FormFetchException {
        final Form form1 = new Form();
        final Form form2 = new Form();
        List<Form> forms = new ArrayList<Form>(){{
            add(form1);
            add(form2);
        }};

        final FormData formData1 = new FormData();
        formData1.setTemplateUuid("form1Uuid");
        final FormData formData2 = new FormData();
        formData2.setTemplateUuid("form2Uuid");

        List<FormData> formDataList = new ArrayList<FormData>(){{
            add(formData1);
            add(formData2);
        }};

        String patientUuid = "patientUuid";

        when(formService.getFormDataByPatient(patientUuid, STATUS_INCOMPLETE)).thenReturn(formDataList);
        when(formService.getFormByUuid(formData1.getTemplateUuid())).thenReturn(form1);
        when(formService.getFormByUuid(formData2.getTemplateUuid())).thenReturn(form2);

        assertThat(formController.getAllIncompleteFormsForPatientUuid(patientUuid), is(forms));
    }

    @Test (expected = FormFetchException.class)
    public void getAllCompleteFormsForPatientUuid_shouldThrowFormFetchExceptionIfExceptionThrownByService() throws Exception, FormFetchException {
        doThrow(new IOException()).when(formService).getFormDataByPatient(anyString(),anyString());

        formController.getAllCompleteFormsForPatientUuid("patientUuid");
    }

    @Test
    public void getAllCompleteFormsForPatientUuid_shouldReturnAllCompleteFormsForGivenPatient() throws Exception, FormFetchException {
        final Form form1 = new Form();
        final Form form2 = new Form();
        List<Form> forms = new ArrayList<Form>(){{
            add(form1);
            add(form2);
        }};

        final FormData formData1 = new FormData();
        formData1.setTemplateUuid("form1Uuid");
        final FormData formData2 = new FormData();
        formData2.setTemplateUuid("form2Uuid");

        List<FormData> formDataList = new ArrayList<FormData>(){{
            add(formData1);
            add(formData2);
        }};

        String patientUuid = "patientUuid";

        when(formService.getFormDataByPatient(patientUuid, STATUS_COMPLETE)).thenReturn(formDataList);
        when(formService.getFormByUuid(formData1.getTemplateUuid())).thenReturn(form1);
        when(formService.getFormByUuid(formData2.getTemplateUuid())).thenReturn(form2);

        assertThat(formController.getAllCompleteFormsForPatientUuid(patientUuid), is(forms));
    }

    @Test (expected = FormFetchException.class)
    public void getAllIncompleteFormsForPatientUuid_shouldThrowFormFetchExceptionIfExceptionThrownByService() throws Exception, FormFetchException {
        doThrow(new IOException()).when(formService).getFormDataByPatient(anyString(),anyString());

        formController.getAllIncompleteFormsForPatientUuid("patientUuid");
    }

    private List<Form> buildForms() {
        List<Form> forms = new ArrayList<Form>();
        Tag tag1 = TagBuilder.tag().withName("Patient").withUuid("tag1").build();
        Tag tag2 = TagBuilder.tag().withName("PMTCT").withUuid("tag2").build();
        Tag tag3 = TagBuilder.tag().withName("Observation").withUuid("tag3").build();
        Tag tag4 = TagBuilder.tag().withName("AMPATH").withUuid("tag4").build();
        Tag tag5 = TagBuilder.tag().withName("Encounter").withUuid("tag5").build();

        Tag[] tags1 = {tag1, tag2, tag3};
        Tag[] tags2 = {tag1, tag3};
        Tag[] tags3 = {tag2, tag4};
        Tag[] tags4 = {tag4, tag5, tag3};
        Tag[] tags5 = {};

        Form form1 = FormBuilder.form().withName("Patient Registration").withDescription("Form for patient registration").withUuid("uuid1").withTags(tags1).build();
        Form form2 = FormBuilder.form().withName("PMTCT").withDescription("Form for pmtct registration").withUuid("uuid2").withTags(tags2).build();
        Form form3 = FormBuilder.form().withName("Ampath").withDescription("Form for pmtct registration").withUuid("uuid3").withTags(tags3).build();
        Form form4 = FormBuilder.form().withName("Patient Observation").withDescription("Form for pmtct registration").withUuid("uuid4").withTags(tags4).build();
        Form form5 = FormBuilder.form().withName("Encounter Form").withDescription("Form for pmtct registration").withUuid("uuid5").withTags(tags5).build();

        forms.add(form1);
        forms.add(form2);
        forms.add(form3);
        forms.add(form4);
        forms.add(form5);

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
