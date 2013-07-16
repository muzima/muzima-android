package com.muzima.controller;

import com.muzima.api.model.Form;
import com.muzima.api.model.Tag;
import com.muzima.api.service.FormService;
import com.muzima.builder.FormBuilder;
import com.muzima.builder.TagBuilder;
import com.muzima.search.api.util.StringUtil;

import org.apache.lucene.queryParser.ParseException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.muzima.controller.FormController.FormFetchException;
import static com.muzima.controller.FormController.FormSaveException;
import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;
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
    public void addSelectedTag_shouldAddTagToSelectedList(){
        assertThat(formController.getSelectedTags().size(), is(0));

        Tag tag = new Tag();
        formController.addSelectedTags(tag);

        assertThat(formController.getSelectedTags().size(), is(1));
        assertThat(formController.getSelectedTags().get(0), is(tag));
    }

    @Test
    public void addSelectedTag_shouldNotAddTagIfItIsNull(){
        assertThat(formController.getSelectedTags().size(), is(0));

        formController.addSelectedTags(null);

        assertThat(formController.getSelectedTags().size(), is(0));
    }

    @Test
    public void addSelectedTag_shouldNotAddTagIfItIsAlreadyAdded(){
        assertThat(formController.getSelectedTags().size(), is(0));

        Tag tag = new Tag();
        formController.addSelectedTags(tag);

        assertThat(formController.getSelectedTags().size(), is(1));

        formController.addSelectedTags(tag);

        assertThat(formController.getSelectedTags().size(), is(1));
    }

    @Test
    public void removeSelectedTag_shouldRemoveATag(){
        Tag tag = new Tag();
        formController.addSelectedTags(tag);

        assertThat(formController.getSelectedTags().size(), is(1));

        formController.removeSelectedTags(tag);

        assertThat(formController.getSelectedTags().size(), is(0));
    }

    @Test
    public void clearAllSelectedTags_shouldRemoveAllTags(){
        Tag tag = new Tag();
        formController.addSelectedTags(tag);

        assertThat(formController.getSelectedTags().size(), is(1));

        formController.clearAllSelectedTags();

        assertThat(formController.getSelectedTags().size(), is(0));
    }

    @Test
    public void isTagSelected_shouldReturnTrueIfATagIsSelected(){
        Tag tag1 = new Tag();
        tag1.setUuid("tag1");
        Tag tag2 = new Tag();
        tag2.setUuid("tag2");
        formController.addSelectedTags(tag1);

        assertThat(formController.isTagSelected(tag1), is(true));
        assertThat(formController.isTagSelected(tag2), is(false));
    }

    @Test
    public void hasSelectedTags_shouldReturnTrueIfNoTagsArePresent(){
        assertThat(formController.hasSelectedTags(), is(false));

        formController.addSelectedTags(new Tag());

        assertThat(formController.hasSelectedTags(), is(true));
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
}
