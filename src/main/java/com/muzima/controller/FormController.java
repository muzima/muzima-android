package com.muzima.controller;

import com.muzima.api.model.Form;
import com.muzima.api.model.FormData;
import com.muzima.api.model.FormTemplate;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Tag;
import com.muzima.api.service.FormService;
import com.muzima.api.service.PatientService;
import com.muzima.model.AvailableForm;
import com.muzima.model.CompleteForm;
import com.muzima.model.builders.CompletePatientFormBuilder;
import com.muzima.model.collections.AvailableForms;
import com.muzima.model.collections.CompleteForms;
import com.muzima.model.collections.CompletePatientForms;
import com.muzima.model.collections.DownloadedForms;
import com.muzima.model.builders.AvailableFormBuilder;
import com.muzima.model.builders.CompleteFormBuilder;
import com.muzima.model.builders.DownloadedFormBuilder;
import com.muzima.search.api.util.StringUtil;
import com.muzima.utils.CustomColor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.muzima.utils.Constants.*;

public class FormController {

    private FormService formService;
    private PatientService patientService;
    private Map<String, Integer> tagColors;
    private List<Tag> selectedTags;

    public FormController(FormService formService, PatientService patientService) {
        this.formService = formService;
        this.patientService = patientService;
        tagColors = new HashMap<String, Integer>();
        selectedTags = new ArrayList<Tag>();
    }

    public int getTotalFormCount() throws FormFetchException {
        try {
            return formService.countAllForms();
        } catch (IOException e) {
            throw new FormFetchException(e);
        }
    }

    public Form getFormByUuid(String formId) throws FormFetchException {
        try {
            return formService.getFormByUuid(formId);
        } catch (IOException e) {
            throw new FormFetchException(e);
        }
    }

    public FormTemplate getFormTemplateByUuid(String formId) throws FormFetchException {
        try {
            return formService.getFormTemplateByUuid(formId);
        } catch (IOException e) {
            throw new FormFetchException(e);
        }
    }

    public AvailableForms getAvailableFormByTags(List<String> tagsUuid) throws FormFetchException {
        try {
            List<Form> allForms = formService.getAllForms();
            List<Form> filteredForms = filterFormsByTags(allForms, tagsUuid);
            AvailableForms availableForms  = new AvailableForms();
            for (Form filteredForm : filteredForms) {
                boolean downloadStatus = formService.isFormTemplateDownloaded(filteredForm.getUuid());
                AvailableForm availableForm = new AvailableFormBuilder()
                        .withAvailableForm(filteredForm)
                        .withDownloadStatus(downloadStatus).build();
                availableForms.add(availableForm);
            }
            return availableForms;
        } catch (IOException e) {
            throw new FormFetchException(e);
        }
    }

    private List<Form> filterFormsByTags(List<Form> forms, List<String> tagsUuid) {
        if (tagsUuid==null || tagsUuid.isEmpty()) {
            return forms;
        }
        List<Form> filteredForms = new ArrayList<Form>();
        for (Form form : forms) {
            Tag[] formTags = form.getTags();
            for (Tag formTag : formTags) {
                if (tagsUuid.contains(formTag.getUuid())) {
                    filteredForms.add(form);
                    break;
                }
            }
        }
        return filteredForms;
    }

    public List<Tag> getAllTags() throws FormFetchException {
        List<Tag> allTags = new ArrayList<Tag>();
        List<Form> allForms = null;
        try {
            allForms = formService.getAllForms();
        } catch (IOException e) {
                throw new FormFetchException(e);
        }
        for (Form form : allForms) {
            for (Tag tag : form.getTags()) {
                if (!allTags.contains(tag)) {
                    allTags.add(tag);
                }
            }
        }
        return allTags;
    }

    public DownloadedForms getAllDownloadedFormsByTags() throws FormFetchException {
        DownloadedForms downloadedFormsByTags = new DownloadedForms();
        try {
            List<Form> allForms = formService.getAllForms();
            for (Form form : allForms) {
                if (formService.isFormTemplateDownloaded(form.getUuid())) {
                    downloadedFormsByTags.add(new DownloadedFormBuilder().withDownloadedForm(form).build());
                }
            }
        } catch (IOException e) {
            throw new FormFetchException(e);
        }
        return downloadedFormsByTags;
    }

    public int getDownloadedFormsCount() throws FormFetchException {
        try {
            return formService.countAllFormTemplates();
        } catch (IOException e) {
            throw new FormFetchException(e);
        }
    }

    public List<Form> downloadAllForms() throws FormFetchException {
        try {
            return formService.downloadFormsByName(StringUtil.EMPTY);
        } catch (IOException e) {
            throw new FormFetchException(e);
        }
    }

    public List<FormTemplate> downloadFormTemplates(String[] formUuids) throws FormFetchException {
        ArrayList<FormTemplate> formTemplates = new ArrayList<FormTemplate>();
        for (String uuid : formUuids) {
            formTemplates.add(downloadFormTemplateByUuid(uuid));
        }
        return formTemplates;
    }

    public FormTemplate downloadFormTemplateByUuid(String uuid) throws FormFetchException {
        try {
            return formService.downloadFormTemplateByUuid(uuid);
        } catch (IOException e) {
            throw new FormFetchException(e);
        }
    }

    public void saveAllForms(List<Form> forms) throws FormSaveException {
        try {
            formService.saveForms(forms);
        } catch (IOException e) {
            throw new FormSaveException(e);
        }
    }

    public void deleteAllForms() throws FormDeleteException {
        try {
            formService.deleteForms(formService.getAllForms());
        } catch (IOException e) {
            throw new FormDeleteException(e);
        }
    }

    public void replaceFormTemplates(List<FormTemplate> formTemplates) throws FormSaveException {
        for (FormTemplate formTemplate : formTemplates) {
            FormTemplate existingFormTemplate = null;
            try {
                existingFormTemplate = formService.getFormTemplateByUuid(formTemplate.getUuid());
                if (existingFormTemplate != null) {
                    formService.deleteFormTemplate(existingFormTemplate);
                }
                formService.saveFormTemplate(formTemplate);
            } catch (IOException e) {
                throw new FormSaveException(e);
            }
        }
    }

    public boolean isFormDownloaded(Form form) throws FormFetchException {
        boolean downloaded;
        try {
            downloaded = formService.isFormTemplateDownloaded(form.getUuid());
        } catch (IOException e) {
            throw new FormFetchException(e);
        }
        return downloaded;
    }

    public int getTagColor(String uuid) {
        if (!tagColors.containsKey(uuid)) {
            tagColors.put(uuid, CustomColor.getRandomColor());
        }
        return tagColors.get(uuid);
    }

    public void resetTagColors() {
        tagColors.clear();
    }

    public List<Tag> getSelectedTags() {
        return selectedTags;
    }

    public void setSelectedTags(List<Tag> selectedTags) {
        this.selectedTags = selectedTags;
    }
    public List<Form> downloadFormsByTags(List<String> tags) throws FormFetchException {
        try {
            //TODO replace with downloadFormsByTags later
            return formService.downloadFormsByName(StringUtil.EMPTY) ;
        } catch (IOException e) {
            throw new FormFetchException(e);
        }
    }

    public FormData getFormDataByUuid(String formDataUuid) throws FormDataFetchException {
        try {
            return formService.getFormDataByUuid(formDataUuid);
        } catch (IOException e) {
            throw new FormDataFetchException(e);
        }
    }

    public void saveFormData(FormData formData) throws FormDataSaveException {
        try {
            formService.saveFormData(formData);
        } catch (IOException e) {
            throw new FormDataSaveException(e);
        }
    }

    public List<FormData> getAllFormData(String status) throws FormDataFetchException {
        try {
            return formService.getAllFormData(status);
        } catch (Exception e) {
            throw new FormDataFetchException(e);
        }
    }

    public List<FormData> getAllFormDataByPatientUuid(String patientUuid, String status) throws FormDataFetchException {
        try {
            return formService.getFormDataByPatient(patientUuid, status);
        } catch (IOException e) {
            throw new FormDataFetchException(e);
        }
    }

    public List<Form> getAllIncompleteForms() throws FormFetchException {
        List<Form> incompleteForms = new ArrayList<Form>();

        try {
            List<FormData> allFormData = formService.getAllFormData(STATUS_INCOMPLETE);
            for (FormData formData : allFormData) {
                incompleteForms.add(formService.getFormByUuid(formData.getTemplateUuid()));
            }
        } catch (IOException e) {
            throw new FormFetchException(e);
        }
        return incompleteForms;
    }

    public CompleteForms getAllCompleteForms() throws FormFetchException {
        CompleteForms completeForms = new CompleteForms();

        try {
            List<FormData> allFormData = formService.getAllFormData(STATUS_COMPLETE);
            for (FormData formData : allFormData) {
                Patient patient = patientService.getPatientByUuid(formData.getPatientUuid());
                CompleteForm completeForm = new CompleteFormBuilder()
                        .withCompleteForm(formService.getFormByUuid(formData.getTemplateUuid()))
                        .withPatientInfo(patient.getFamilyName(),patient.getGivenName(),patient.getMiddleName(),patient.getIdentifier())
                        .build();
                completeForms.add(completeForm);
            }
        } catch (IOException e) {
            throw new FormFetchException(e);
        }
        return completeForms;
    }

    public List<Form> getAllIncompleteFormsForPatientUuid(String patientUuid) throws FormFetchException {
        List<Form> incompleteForms = new ArrayList<Form>();
        try {
            List<FormData> allFormData = formService.getFormDataByPatient(patientUuid, STATUS_INCOMPLETE);
            for (FormData formData : allFormData) {
                incompleteForms.add(formService.getFormByUuid(formData.getTemplateUuid()));
            }
        } catch (IOException e) {
            throw new FormFetchException(e);
        }
        return incompleteForms;
    }

    public CompletePatientForms getAllCompleteFormsForPatientUuid(String patientUuid) throws FormFetchException {
        CompletePatientForms completePatientForms = new CompletePatientForms();
        try {
            List<FormData> allFormData = formService.getFormDataByPatient(patientUuid, STATUS_COMPLETE);
            for (FormData formData : allFormData) {
                Form form = formService.getFormByUuid(formData.getTemplateUuid());
                completePatientForms.add(new CompletePatientFormBuilder()
                        .withCompleteForm(form)
                        .withFormDataUuid(formData.getUuid())
                        .build());
            }
        } catch (IOException e) {
            throw new FormFetchException(e);
        }
        return completePatientForms;
    }

    public int getAllIncompleteFormsSize() throws FormFetchException {
        return getAllIncompleteForms().size();
    }

    public int getAllCompleteFormsSize() throws FormFetchException {
        return getAllCompleteForms().size();
    }

    public static class FormFetchException extends Throwable {
        public FormFetchException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class FormSaveException extends Throwable {
        public FormSaveException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class FormDeleteException extends Throwable {
        public FormDeleteException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class FormDataFetchException extends Throwable {
        public FormDataFetchException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class FormDataSaveException extends Throwable {
        public FormDataSaveException(Throwable throwable) {
            super(throwable);
        }
    }
}
