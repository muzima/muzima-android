package com.muzima.view.forms;

import android.util.Log;
import android.widget.Toast;

import com.muzima.api.model.FormData;
import com.muzima.controller.FormController;

import android.webkit.JavascriptInterface;

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
    public void save(String data, String status) {
        Log.d(TAG, "saving form data: " + data);
        formData.setPayload(data);
        formData.setStatus(status);
        try {
            formController.saveFormData(formData);
            formWebViewActivity.finish();
        } catch (FormController.FormDataSaveException e) {
            Toast.makeText(formWebViewActivity, "An error occurred while saving the form", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Exception occurred while saving form data" + e);
        }
    }

    @JavascriptInterface
    public String getFormPayload() {
        return formData.getPayload();
    }

}
