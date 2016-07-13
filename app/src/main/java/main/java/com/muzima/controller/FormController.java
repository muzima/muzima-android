/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.controller;

import android.util.Log;
import com.muzima.api.model.APIName;
import com.muzima.api.model.Form;
import com.muzima.api.model.FormData;
import com.muzima.api.model.FormTemplate;
import com.muzima.api.model.LastSyncTime;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Tag;
import com.muzima.api.service.FormService;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.api.service.ObservationService;
import com.muzima.api.service.PatientService;
import com.muzima.model.AvailableForm;
import com.muzima.model.CompleteFormWithPatientData;
import com.muzima.model.builders.AvailableFormBuilder;
import com.muzima.model.builders.CompleteFormBuilder;
import com.muzima.model.builders.CompleteFormWithPatientDataBuilder;
import com.muzima.model.builders.DownloadedFormBuilder;
import com.muzima.model.builders.IncompleteFormBuilder;
import com.muzima.model.builders.IncompleteFormWithPatientDataBuilder;
import com.muzima.model.collections.AvailableForms;
import com.muzima.model.collections.CompleteForms;
import com.muzima.model.collections.CompleteFormsWithPatientData;
import com.muzima.model.collections.DownloadedForms;
import com.muzima.model.collections.IncompleteForms;
import com.muzima.model.collections.IncompleteFormsWithPatientData;
import com.muzima.search.api.util.StringUtil;
import com.muzima.service.SntpService;
import com.muzima.util.JsonUtils;
import com.muzima.utils.Constants;
import com.muzima.utils.CustomColor;
import com.muzima.utils.EnDeCrypt;
import com.muzima.utils.MediaUtils;
import com.muzima.utils.StringUtils;
import com.muzima.view.forms.HTMLPatientJSONMapper;
import com.muzima.view.forms.PatientJSONMapper;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.muzima.utils.Constants.FORM_JSON_DISCRIMINATOR_CONSULTATION;
import static com.muzima.utils.Constants.STATUS_UPLOADED;

public class FormController {

    private static final String TAG = "FormController";
    private final String REGISTRATION = "registration";
    private FormService formService;
    private PatientService patientService;
    private LastSyncTimeService lastSyncTimeService;
    private SntpService sntpService;
    private ObservationService observationService;
    private Map<String, Integer> tagColors;
    private List<Tag> selectedTags;
    private String jsonPayload;

    public FormController(FormService formService, PatientService patientService, LastSyncTimeService lastSyncTimeService, SntpService sntpService,
                          ObservationService observationService) {
        this.formService = formService;
        this.patientService = patientService;
        this.lastSyncTimeService = lastSyncTimeService;
        this.sntpService = sntpService;
        this.observationService = observationService;
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
        return getAvailableFormByTags(tagsUuid, false);
    }

    public List<Form> getAllAvailableForms() throws FormFetchException {
        try {
            return formService.getAllForms();
        } catch (IOException e) {
            throw new FormFetchException(e);
        }
    }

    public AvailableForms getAvailableFormByTags(List<String> tagsUuid, boolean alwaysIncludeRegistrationForms) throws FormFetchException {
        try {
            List<Form> allForms = formService.getAllForms();
            List<Form> filteredForms = filterFormsByTags(allForms, tagsUuid, alwaysIncludeRegistrationForms);
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

    private List<Form> filterFormsByTags(List<Form> forms, List<String> tagsUuid, boolean alwaysIncludeRegistrationForms) {
        if (tagsUuid == null || tagsUuid.isEmpty()) {
            return forms;
        }
        List<Form> filteredForms = new ArrayList<Form>();
        for (Form form : forms) {
            Tag[] formTags = form.getTags();
            for (Tag formTag : formTags) {
                if (tagsUuid.contains(formTag.getUuid()) ||
                        (alwaysIncludeRegistrationForms && isRegistrationTag(formTag))) {
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

    public List<Tag> getAllTagsExcludingRegistrationTag() throws FormFetchException {
        List<Tag> allTags = new ArrayList<Tag>();
        for (Tag tag : getAllTags()) {
            if (!isRegistrationTag(tag)) {
                allTags.add(tag);
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

    public List<Form> downloadAllForms() throws FormFetchException {
        try {
            Date lastSyncDate = lastSyncTimeService.getLastSyncTimeFor(APIName.DOWNLOAD_FORMS);
            List<Form> forms = formService.downloadFormsByName(StringUtil.EMPTY, lastSyncDate);
            LastSyncTime lastSyncTime = new LastSyncTime(APIName.DOWNLOAD_FORMS, sntpService.getLocalTime());
            lastSyncTimeService.saveLastSyncTime(lastSyncTime);
            return forms;
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

    public void updateAllForms(List<Form> forms) throws FormSaveException {
        try {
            formService.updateForms(forms);
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

    public void deleteAllFormTemplates() throws FormDeleteException {
        try {
            formService.deleteFormTemplates(formService.getAllFormTemplates());
        } catch (IOException e) {
            throw new FormDeleteException(e);
        }
    }

    public void deleteForms(List<Form> forms) throws FormDeleteException {
        try {
            formService.deleteForms(forms);
        } catch (IOException e) {
            throw new FormDeleteException(e);
        }
    }

    public void deleteFormTemplatesByUUID(List<String> formTemplateUUIDs) throws FormDeleteException {
        try {
            formService.deleteFormTemplateByUUIDs(formTemplateUUIDs);
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

    public void saveFormTemplates(List<FormTemplate> formTemplates) throws FormSaveException {

        try {
            formService.saveFormTemplates(formTemplates);
        } catch (IOException e) {
            throw new FormSaveException(e);
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

    public FormData getFormDataByUuid(String formDataUuid) throws FormDataFetchException {
        try {
            return formService.getFormDataByUuid(formDataUuid);
        } catch (IOException e) {
            throw new FormDataFetchException(e);
        }
    }

    public CompleteFormWithPatientData getCompleteFormDataByUuid(String formDataUuid) throws FormDataFetchException, FormFetchException {
        CompleteFormWithPatientData completeForm = null;

        try {
            FormData formData = formService.getFormDataByUuid(formDataUuid);
            if (formData != null && (StringUtil.equals(formData.getStatus(), Constants.STATUS_UPLOADED)) ||
                    StringUtil.equals(formData.getStatus(), Constants.STATUS_COMPLETE)){
                Patient patient = patientService.getPatientByUuid(formData.getPatientUuid());
                completeForm = new CompleteFormWithPatientDataBuilder()
                        .withForm(formService.getFormByUuid(formData.getTemplateUuid()))
                        .withFormDataUuid(formData.getUuid())
                        .withPatient(patient)
                        .withLastModifiedDate(formData.getSaveTime())
                        .withEncounterDate(formData.getEncounterDate())
                        .build();
            }

        } catch (IOException e) {
            throw new FormFetchException(e);
        }
        return completeForm;
    }

    public void saveFormData(FormData formData) throws FormDataSaveException {
        try {
            formData.setSaveTime(new Date());
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

    public IncompleteFormsWithPatientData getAllIncompleteFormsWithPatientData() throws FormFetchException {
        IncompleteFormsWithPatientData incompleteForms = new IncompleteFormsWithPatientData();

        try {
            List<FormData> allFormData = formService.getAllFormData(Constants.STATUS_INCOMPLETE);
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
                        .withLastModifiedDate(formData.getSaveTime())
                        .withEncounterDate(formData.getEncounterDate())
                        .build());
            }
        } catch (IOException e) {
            throw new FormFetchException(e);
        }
        return incompleteForms;
    }

    public CompleteFormsWithPatientData getAllCompleteFormsWithPatientData() throws FormFetchException {
        CompleteFormsWithPatientData completeForms = new CompleteFormsWithPatientData();

        try {
            List<FormData> allFormData = formService.getAllFormData(Constants.STATUS_COMPLETE);
            for (FormData formData : allFormData) {
                Patient patient = patientService.getPatientByUuid(formData.getPatientUuid());
                CompleteFormWithPatientData completeForm = new CompleteFormWithPatientDataBuilder()
                        .withForm(formService.getFormByUuid(formData.getTemplateUuid()))
                        .withFormDataUuid(formData.getUuid())
                        .withPatient(patient)
                        .withLastModifiedDate(formData.getSaveTime())
                        .withEncounterDate(formData.getEncounterDate())
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
            List<FormData> allFormData = formService.getFormDataByPatient(patientUuid, Constants.STATUS_INCOMPLETE);
            for (FormData formData : allFormData) {
                incompleteForms.add(new IncompleteFormBuilder().withForm(formService.getFormByUuid(formData.getTemplateUuid()))
                        .withFormDataUuid(formData.getUuid())
                        .withLastModifiedDate(formData.getSaveTime())
                        .withEncounterDate(formData.getEncounterDate())
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
            List<FormData> allFormData = formService.getFormDataByPatient(patientUuid, Constants.STATUS_COMPLETE);
            for (FormData formData : allFormData) {
                Form form = formService.getFormByUuid(formData.getTemplateUuid());
                completePatientForms.add(new CompleteFormBuilder()
                        .withForm(form)
                        .withFormDataUuid(formData.getUuid())
                        .withLastModifiedDate(formData.getSaveTime())
                        .withEncounterDate(formData.getEncounterDate())
                        .build());
            }
        } catch (IOException e) {
            throw new FormFetchException(e);
        }
        return completePatientForms;
    }

    public int getAllIncompleteFormsSize() throws FormFetchException {
        return getAllIncompleteFormsWithPatientData().size();
    }

    public int getAllCompleteFormsSize() throws FormFetchException {
        return getAllCompleteFormsWithPatientData().size();
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
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    public Patient createNewHTMLPatient(String data) {
        try {
            Patient patient = new HTMLPatientJSONMapper().getPatient(data);
            patientService.savePatient(patient);
            return patient;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    public boolean uploadAllCompletedForms() throws UploadFormDataException {
        try {
            boolean result = true;
            List<FormData> allFormData = formService.getAllFormData(Constants.STATUS_COMPLETE);

            result = uploadFormDataToServer(getFormsWithDiscriminator(allFormData, Constants.FORM_DISCRIMINATOR_REGISTRATION), result);
            result = uploadFormDataToServer(getFormsWithDiscriminator(allFormData, Constants.FORM_JSON_DISCRIMINATOR_REGISTRATION), result);
            result = uploadFormDataToServer(getFormsWithDiscriminator(allFormData, Constants.FORM_JSON_DISCRIMINATOR_DEMOGRAPHICS_UPDATE), result);
            result = uploadFormDataToServer(getFormsWithDiscriminator(allFormData, Constants.FORM_JSON_DISCRIMINATOR_CONSULTATION), result);
            result = uploadFormDataToServer(getFormsWithDiscriminator(allFormData, Constants.FORM_XML_DISCRIMINATOR_ENCOUNTER), result);
            return uploadFormDataToServer(getFormsWithDiscriminator(allFormData, Constants.FORM_JSON_DISCRIMINATOR_ENCOUNTER), result);
        } catch (IOException e) {
            throw new UploadFormDataException(e);
        } catch (JSONException e) {
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

    private boolean isRegistrationTag(Tag formTag) {
        return REGISTRATION.equalsIgnoreCase(formTag.getName());
    }

    public void deleteCompleteAndIncompleteForms(List<String> selectedIncompleteFormsUuids) throws FormDeleteException{
        try {
            List<FormData> selectedFormsData = new ArrayList<FormData>();
            for (String selectedIncompleteFormsUuid : selectedIncompleteFormsUuids) {
                selectedFormsData.add(getFormDataByUuid(selectedIncompleteFormsUuid));
            }
            formService.deleteFormData(selectedFormsData);
        } catch (IOException e) {
            throw new FormDeleteException(e);
        } catch (FormDataFetchException e) {
            Log.e(TAG, "Fetching form data throwing exception", e);
        }
    }

    public static class UploadFormDataException extends Throwable {
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

    public List<FormData> getUnUploadedFormData(String templateUUID) throws FormDataFetchException {
        List<FormData> incompleteFormData = new ArrayList<FormData>();
        try {
            List<FormData> formDataByTemplateUUID = formService.getFormDataByTemplateUUID(templateUUID);
            for (FormData formData : formDataByTemplateUUID) {
                if (!formData.getStatus().equals(Constants.STATUS_UPLOADED)) {
                    incompleteFormData.add(formData);
                }
            }
            return incompleteFormData;
        } catch (IOException e) {
            throw new FormDataFetchException(e);
        }
    }

    private List<FormData> getFormsWithDiscriminator(List<FormData> allFormData, String discriminator) {
        List<FormData> requiredForms = new ArrayList<FormData>();
        for (FormData formData : allFormData) {
            if (formData.getDiscriminator().equals(discriminator)) {
                requiredForms.add(formData);
            }
        }
        return requiredForms;
    }

    boolean uploadFormDataToServer(List<FormData> allFormData, boolean result) throws IOException, JSONException {
        for (FormData formData : allFormData) {
            String rawPayload = formData.getJsonPayload();
            // inject consultation.sourceUuid
            formData = injectUuidToPayload(formData);
            // replace media paths with base64 string
            formData = replaceMediaPathWithBase64String(formData);
            if (formService.syncFormData(formData)) {
                formData.setStatus(STATUS_UPLOADED);

                //DO NOT save base64 string in DB
                formData.setJsonPayload(rawPayload);
                formService.saveFormData(formData);
                observationService.deleteObservationsByFormData(formData.getUuid());
            } else {
                result = false;
            }
        }
        return result;
    }

    private static FormData injectUuidToPayload(FormData formData) {

        if (StringUtil.equals(formData.getDiscriminator(), FORM_JSON_DISCRIMINATOR_CONSULTATION)) {
            try {
                String base = "consultation";
                JSONParser jp = new JSONParser(JSONParser.MODE_PERMISSIVE);
                JSONObject obj = (JSONObject) jp.parse(formData.getJsonPayload());
                JsonUtils.replaceAsString(obj, base, "consultation.sourceUuid", formData.getUuid());
                formData.setJsonPayload(obj.toJSONString());
            } catch (ParseException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        return formData;
    }

    private void traverseJson(JSONObject json) {
        Iterator<String> keys = json.keySet().iterator();
        while(keys.hasNext()){
            String key = keys.next();
            String val = null;
            try{
                Object obj =  JsonUtils.readAsObject(json.toJSONString(), "$['" + key + "']");
                if (obj instanceof JSONArray){
                    JSONArray arr = (JSONArray) obj;
                    for (Object object : arr) {
                        traverseJson((JSONObject) object);
                    }
                } else {
                    traverseJson((JSONObject)obj);
                }
            } catch(Exception e){
                val = json.get(key).toString();
            }

            if(val != null) {
                replaceMediaPathWithMedia(key, val);
            }
        }
    }

    private void replaceMediaPathWithMedia(String key, String value){
        if (value.contains("/muzima/media/")) {
            String newKeyValPair = "\"" + key + "\":\"" + getStringMedia(value) + "\"";
            if (!jsonPayload.contains(value)) {
                value = value.replace("/", "\\/");
            }
            String keyValPair = "\"" + key + "\":\"" + value + "\"";

            jsonPayload = jsonPayload.replace(keyValPair, newKeyValPair);
        }
    }

    private FormData replaceMediaPathWithBase64String(FormData formData) {
        try {
            jsonPayload = formData.getJsonPayload();
            JSONParser jp = new JSONParser(JSONParser.MODE_PERMISSIVE);
            traverseJson((JSONObject) jp.parse(jsonPayload));
            formData.setJsonPayload(jsonPayload);
        } catch (ParseException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return formData;
    }

    private static String getStringMedia(String mediaUri) {
        String mediaString = null;
        if (!StringUtils.isEmpty(mediaUri))  {
            //fetch the media and convert it to @Base64 encoded string.
            File f = new File(mediaUri) ;
            if (f.exists()){

                // here the media file is encrypted so we decrypt it
                EnDeCrypt.decrypt(f, "this-is-supposed-to-be-a-secure-key");

                try {
                    FileInputStream fis = new FileInputStream(f);
                    byte[] fileBytes = new byte[(int) f.length()];
                    int read = fis.read(fileBytes);
                    if (read != f.length()) {
                        Log.e(TAG, "File read is not equal to the length of the file itself.");
                    }

                    //convert the decrypted media to Base64 string
                    mediaString = MediaUtils.toBase64(fileBytes);
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                }

                // and encrypt again
                EnDeCrypt.encrypt(f, "this-is-supposed-to-be-a-secure-key");
            }
        }
        return mediaString != null? mediaString : mediaUri;
    }
    public boolean isFormAlreadyExist(String jsonPayload, FormData formData) throws IOException, JSONException {
        org.json.JSONObject temp = new org.json.JSONObject(jsonPayload);
        String checkEncounterDate = ((org.json.JSONObject)temp.get("encounter")).get("encounter.encounter_datetime").toString();
        String checkPatientUuid = ((org.json.JSONObject)temp.get("patient")).get("patient.uuid").toString();
        String checkFormUuid = ((org.json.JSONObject)temp.get("encounter")).get("encounter.form_uuid").toString();

        List<FormData> allFormData = formService.getAllFormData(Constants.STATUS_INCOMPLETE);
        allFormData.addAll(formService.getAllFormData(Constants.STATUS_COMPLETE));
        for (FormData formData1 : allFormData){
            if(!isRegistrationFormData(formData1) && !formData1.getUuid().equals(formData.getUuid())) {
                org.json.JSONObject object = new org.json.JSONObject(formData1.getJsonPayload());
                String encounterDate = ((org.json.JSONObject) object.get("encounter")).get("encounter.encounter_datetime").toString();
                String patientUuid = ((org.json.JSONObject) object.get("patient")).get("patient.uuid").toString();
                String formUuid = ((org.json.JSONObject) object.get("encounter")).get("encounter.form_uuid").toString();
                if (encounterDate.equals(checkEncounterDate)
                        && patientUuid.equals(checkPatientUuid)
                        && formUuid.equals(checkFormUuid)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isRegistrationFormData(FormData formData){
        return formData.getDiscriminator().equals(Constants.FORM_DISCRIMINATOR_REGISTRATION)
                || formData.getDiscriminator().equals(Constants.FORM_JSON_DISCRIMINATOR_REGISTRATION);
    }
}
