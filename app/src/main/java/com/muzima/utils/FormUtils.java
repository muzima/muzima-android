package com.muzima.utils;

import com.muzima.controller.FormController;
import com.muzima.model.collections.AvailableForms;

public class FormUtils {

    public static  AvailableForms getRegistrationForms(FormController formController) {
        AvailableForms availableForms = null;
        try {
            availableForms = formController.getDownloadedRegistrationForms();
        } catch (FormController.FormFetchException e) {
            e.printStackTrace();
        }
        return availableForms;
    }
}
