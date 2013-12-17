package com.muzima.view.forms;

import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;
import com.muzima.api.model.FormData;
import com.muzima.api.model.Patient;
import com.muzima.controller.FormController;

import static com.muzima.utils.Constants.FORM_DISCRIMINATOR_REGISTRATION;
import static com.muzima.utils.Constants.STATUS_COMPLETE;

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
    public void save(String jsonData, String xmlData, String status) {
        Patient newPatient = null;
        if (isRegistrationComplete(status)) {
            newPatient = formController.createNewPatient(jsonData);
            formData.setPatientUuid(newPatient.getUuid());
            formData.setDiscriminator(FORM_DISCRIMINATOR_REGISTRATION);
            formWebViewActivity.startPatientSummaryView(newPatient);
        }
        Log.d(TAG,"xml data is:"+xmlData);
        formData.setXmlPayload(xmlData);
        Log.d(TAG,"json data is:"+xmlData);
        formData.setJsonPayload(jsonData);
        formData.setStatus(status);
        try {
            formController.saveFormData(formData);
            formWebViewActivity.setResult(FormsActivity.RESULT_OK);
            formWebViewActivity.finish();
        } catch (FormController.FormDataSaveException e) {
            Toast.makeText(formWebViewActivity, "An error occurred while saving the form", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Exception occurred while saving form data" + e);
        }
    }

    private boolean isRegistrationComplete(String status) {
        return isRegisterPatient() && status.equals(STATUS_COMPLETE);
    }

    @JavascriptInterface
    public String getFormPayload() {
        return formData.getJsonPayload();
    }

    @JavascriptInterface
    public String getFormStatus() {
        return formData.getStatus();
    }

    @JavascriptInterface
     public void showSaveProgressBar() {
        formWebViewActivity.showProgressBar("Saving...");
    }

    public boolean isRegisterPatient() {
        return formData.getPatientUuid() == null;
    }
}
