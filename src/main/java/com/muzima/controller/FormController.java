package com.muzima.controller;

import com.muzima.api.model.Form;
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
        } catch (ParseException e) {
            throw new FormFetchException(e);
        }
    }

    //TODO Do this at lucene level
    public List<Form> getAllFormByTags(List<String> tagsUuid) throws FormFetchException{
        try {
            List<Form> allForms = formService.getAllForms();
            if(tagsUuid.isEmpty()){
                return allForms;
            }

            List<Form> filteredForms = new ArrayList<Form>();
            for (Form form : allForms) {
                Tag[] formTags = form.getTags();
                for (Tag formTag : formTags) {
                    if(tagsUuid.contains(formTag.getUuid())){
                        filteredForms.add(form);
                        break;
                    }
                }
            }
            return filteredForms;
        } catch (IOException e) {
            throw new FormFetchException(e);
        } catch (ParseException e) {
            throw new FormFetchException(e);
        }
    }

    public List<Form> downloadAllForms() throws FormFetchException {
        try {
            return formService.downloadFormsByName(StringUtil.EMPTY);
        } catch (IOException e) {
            throw new FormFetchException(e);
        } catch (ParseException e) {
            throw new FormFetchException(e);
        }
    }

    public void saveForm(Form form) throws FormSaveException {
        try {
            formService.saveForm(form);
        } catch (IOException e) {
            throw new FormSaveException(e);
        }
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

    public List<Tag> getSelectedTags() {
        return selectedTags;
    }

    public void setSelectedTags(List<Tag> selectedTags) {
        this.selectedTags = selectedTags;
    }

    public void downloadFormsTemplate(List<Form> selectedForms) {
        
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
}
