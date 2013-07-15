package com.muzima.controller;

import com.muzima.api.model.Form;
import com.muzima.api.service.FormService;
import com.muzima.search.api.util.StringUtil;

import org.apache.lucene.queryParser.ParseException;

import java.io.IOException;
import java.util.List;

public class FormController {

    private FormService formService;

    public FormController(FormService formService) {
        this.formService = formService;
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

    public static class FormFetchException extends Throwable {
        public FormFetchException(Throwable throwable){
            super(throwable);
        }
    }

    public static class FormSaveException extends Throwable {
        public FormSaveException(Throwable throwable){
            super(throwable);
        }
    }
}
