package com.muzima.controller;

import com.muzima.api.model.Form;
import com.muzima.api.model.FormData;
import com.muzima.api.model.FormTemplate;
import com.muzima.api.model.Tag;
import com.muzima.api.service.FormService;
import com.muzima.search.api.util.StringUtil;
import com.muzima.utils.CustomColor;

import org.apache.lucene.queryParser.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FormController {

    private FormService formService;
    private Map<String, Integer> tagColors;
    private List<Tag> selectedTags;

    public FormController(FormService formService) {
        this.formService = formService;
        tagColors = new HashMap<String, Integer>();
        selectedTags = new ArrayList<Tag>();
    }

    public List<Form> getAllForms() throws FormFetchException {
        try {
            return formService.getAllForms();
        } catch (IOException e) {
            throw new FormFetchException(e);
        }
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

    //TODO Do this at lucene level
    public List<Form> getAllFormByTags(List<String> tagsUuid) throws FormFetchException {
        try {
            List<Form> allForms = formService.getAllForms();
            if (tagsUuid.isEmpty()) {
                return allForms;
            }

            List<Form> filteredForms = new ArrayList<Form>();
            for (Form form : allForms) {
                Tag[] formTags = form.getTags();
                for (Tag formTag : formTags) {
                    if (tagsUuid.contains(formTag.getUuid())) {
                        filteredForms.add(form);
                        break;
                    }
                }
            }
            return filteredForms;
        } catch (IOException e) {
            throw new FormFetchException(e);
        }
    }

    public List<Tag> getAllTags() throws FormFetchException {
        List<Tag> allTags = new ArrayList<Tag>();
        List<Form> allForms = getAllForms();
        for (Form form : allForms) {
            for (Tag tag : form.getTags()) {
                if (!allTags.contains(tag)) {
                    allTags.add(tag);
                }
            }
        }
        return allTags;
    }

    public List<Form> getAllDownloadedForms() throws FormFetchException {
        ArrayList<Form> result = new ArrayList<Form>();
        try {
            List<Form> allForms = formService.getAllForms();
            for (Form form : allForms) {
                if (formService.isFormTemplateDownloaded(form.getUuid())) {
                    result.add(form);
                }
            }
        } catch (IOException e) {
            throw new FormFetchException(e);
        }
        return result;
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
