package com.muzima.controller;

import android.util.Log;
import com.muzima.api.model.*;
import com.muzima.api.service.FormService;
import com.muzima.api.service.PatientService;
import com.muzima.model.AvailableForm;
import com.muzima.model.CompleteFormWithPatientData;
import com.muzima.model.builders.*;
import com.muzima.model.collections.*;
import com.muzima.search.api.util.StringUtil;
import com.muzima.utils.CustomColor;
import com.muzima.view.forms.PatientJSONMapper;
import org.apache.lucene.queryParser.ParseException;
import org.json.JSONException;

import java.io.IOException;
import java.util.*;

import static com.muzima.utils.Constants.STATUS_COMPLETE;
import static com.muzima.utils.Constants.STATUS_INCOMPLETE;
import static com.muzima.utils.Constants.STATUS_UPLOADED;

public class FormController {

    private static final String TAG = "FormController";
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
            AvailableForms availableForms = new AvailableForms();
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
        if (tagsUuid == null || tagsUuid.isEmpty()) {
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

    public DownloadedForms getAllDownloadedForms() throws FormFetchException {
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
            return formService.downloadFormsByName(StringUtil.EMPTY);
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

    public IncompleteFormsWithPatientData getAllIncompleteForms() throws FormFetchException {
        IncompleteFormsWithPatientData incompleteForms = new IncompleteFormsWithPatientData();

        try {
            List<FormData> allFormData = formService.getAllFormData(STATUS_INCOMPLETE);
            for (FormData formData : allFormData) {
                String patientUuid = formData.getPatientUuid();
                Patient patient = null;
                if (patientUuid != null) {
                    patient = patientService.getPatientByUuid(patientUuid);
                }
                incompleteForms.add(new IncompleteFormWithPatientDataBuilder()
                    .withForm(formService.getFormByUuid(formData.getTemplateUuid()))
                    .withFormDataUuid(formData.getUuid())
                    .withPatient(patient)
                    .build());
            }
        } catch (IOException e) {
            throw new FormFetchException(e);
        }
        return incompleteForms;
    }

    public CompleteFormsWithPatientData getAllCompleteForms() throws FormFetchException {
        CompleteFormsWithPatientData completeForms = new CompleteFormsWithPatientData();

        try {
            List<FormData> allFormData = formService.getAllFormData(STATUS_COMPLETE);
            for (FormData formData : allFormData) {
                Patient patient = patientService.getPatientByUuid(formData.getPatientUuid());
                CompleteFormWithPatientData completeForm = new CompleteFormWithPatientDataBuilder()
                        .withForm(formService.getFormByUuid(formData.getTemplateUuid()))
                        .withFormDataUuid(formData.getUuid())
                        .withPatient(patient)
                        .build();
                completeForms.add(completeForm);
            }
        } catch (IOException e) {
            throw new FormFetchException(e);
        }
        return completeForms;
    }

    public IncompleteForms getAllIncompleteFormsForPatientUuid(String patientUuid) throws FormFetchException {
        IncompleteForms incompleteForms = new IncompleteForms();
        try {
            List<FormData> allFormData = formService.getFormDataByPatient(patientUuid, STATUS_INCOMPLETE);
            for (FormData formData : allFormData) {
                incompleteForms.add(new IncompleteFormBuilder().withForm(formService.getFormByUuid(formData.getTemplateUuid()))
                        .withFormDataUuid(formData.getUuid())
                        .build());
            }
        } catch (IOException e) {
            throw new FormFetchException(e);
        }
        return incompleteForms;
    }

    public CompleteForms getAllCompleteFormsForPatientUuid(String patientUuid) throws FormFetchException {
        CompleteForms completePatientForms = new CompleteForms();
        try {
            List<FormData> allFormData = formService.getFormDataByPatient(patientUuid, STATUS_COMPLETE);
            for (FormData formData : allFormData) {
                Form form = formService.getFormByUuid(formData.getTemplateUuid());
                completePatientForms.add(new CompleteFormBuilder()
                        .withForm(form)
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

    public int getCompleteFormsCountForPatient(String patientId) throws FormFetchException {
        return getAllCompleteFormsForPatientUuid(patientId).size();
    }

    public int getIncompleteFormsCountForPatient(String patientId) throws FormFetchException {
        return getAllIncompleteFormsForPatientUuid(patientId).size();
    }

    public AvailableForms getDownloadedRegistrationForms() throws FormFetchException {
        AvailableForms result = new AvailableForms();

        for (AvailableForm form : getAvailableFormByTags(null)) {
            if (form.isDownloaded() && form.isRegistrationForm()) {
                result.add(form);
            }
        }
        return result;
    }

    public Patient createNewPatient(String data) {
            try {
                Patient patient = new PatientJSONMapper(data).getPatient();
                patientService.savePatient(patient);
                return patient;
            } catch (JSONException e) {
                Log.e(TAG, e.toString());
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            } catch (ParseException e) {
                Log.e(TAG, e.toString());
            }
        return null;
    }

    public boolean uploadAllCompletedForms() throws UploadFormDataException {
        try {
            boolean result = true;
            List<FormData> allFormData = formService.getAllFormData(STATUS_COMPLETE);
            for (FormData formData : allFormData) {
                if (formService.syncFormData(formData)) {
                    formData.setStatus(STATUS_UPLOADED);
                    formService.saveFormData(formData);
                } else {
                    result = false;
                }
            }
            return result;
        } catch (IOException e) {
            throw new UploadFormDataException(e);
        }
    }

    public AvailableForms getRecommendedForms() throws FormFetchException {
        AvailableForms result = new AvailableForms();

        for (AvailableForm form : getAvailableFormByTags(null)) {
            if (form.isDownloaded() && !form.isRegistrationForm()) {
                result.add(form);
            }
        }
        return result;
    }

    public int getRecommendedFormsCount() throws FormFetchException {
            return getRecommendedForms().size();
    }

    public static class UploadFormDataException extends Throwable{
        public UploadFormDataException(Throwable throwable) {
            super(throwable);
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
