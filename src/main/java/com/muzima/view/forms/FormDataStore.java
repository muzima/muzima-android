package com.muzima.view.forms;

import android.util.Log;
import android.webkit.JavascriptInterface;

import com.muzima.api.model.FormData;
import com.muzima.controller.FormController;

public class FormDataStore {
    private static final String TAG = "FormDataStore";

    private FormWebViewActivity formWebViewActivity;
    private FormController formController;
    private FormData formData;

    public FormDataStore(FormWebViewActivity formWebViewActivity, FormController formController, FormData formData) {
        this.formWebViewActivity = formWebViewActivity;
        this.formController = formController;
        this.formData = formData;
    }

    @JavascriptInterface
    public boolean saveFormSubmission(String data) {
        Log.d(TAG, "saving form data: " + data);
        formData.setPayload(data);
        try {
            formController.saveFormData(formData);
            formWebViewActivity.finish();
        } catch (FormController.FormDataSaveException e) {
            Log.e(TAG, "Exception occurred while saving form data" + e);
            return false;
        }
        return true;
    }

    @JavascriptInterface
    public String getFormPayload() {
        return formData.getPayload();
    }

}
