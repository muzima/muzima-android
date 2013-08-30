package com.muzima.view.forms;

import android.util.Log;

import com.muzima.api.model.FormData;
import com.muzima.controller.FormController;

public class FormDataRepository {
    private static final String TAG = "FormDataRepository";

    private FormController formController;
    private FormData formData;

    public FormDataRepository(FormController formController, FormData formData) {
        this.formController = formController;
        this.formData = formData;
    }

    //    @JavascriptInterface
    public boolean saveFormSubmission(String paramsJSON, String data) {
        Log.d(TAG, "saving form data: " + data);
        formData.setPayload(data);
        try {
            formController.saveFormData(formData);
        } catch (FormController.FormDataSaveException e) {
            Log.e(TAG, "Exception occurred while saving form data" + e);
            return false;
        }
        return true;
    }

}
