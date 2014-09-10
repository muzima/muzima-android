/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;
import com.muzima.MuzimaApplication;
import com.muzima.api.model.FormData;
import com.muzima.controller.FormController;
import com.muzima.service.HTMLFormObservationCreator;
import com.muzima.utils.Constants;

public class HTMLFormDataStore {
    private static final String TAG = "FormDataStore";

    private HTMLFormWebViewActivity formWebViewActivity;
    private FormController formController;
    private FormData formData;

    public HTMLFormDataStore(HTMLFormWebViewActivity formWebViewActivity, FormController formController, FormData formData) {
        this.formWebViewActivity = formWebViewActivity;
        this.formController = formController;
        this.formData = formData;
    }

    @JavascriptInterface
    public void saveHTML(String jsonPayload, String status) {
        formData.setJsonPayload(jsonPayload);
        formData.setStatus(status);
        try {
            parseForm(jsonPayload, status);
            formController.saveFormData(formData);
            formWebViewActivity.setResult(FormsActivity.RESULT_OK);
            formWebViewActivity.finish();
        } catch (FormController.FormDataSaveException e) {
            Toast.makeText(formWebViewActivity, "An error occurred while saving the form", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Exception occurred while saving form data", e);
        } catch (Exception e) {
            Toast.makeText(formWebViewActivity, "An error occurred while saving the form", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Exception occurred while saving form data", e);
        }
    }

    private void parseForm(String jsonPayload, String status) {
        if (status.equals(Constants.STATUS_INCOMPLETE)) {
            return;
        }
        getFormParser().createAndPersistObservations(jsonPayload, formData.getUuid());
    }

    public HTMLFormObservationCreator getFormParser() {
        MuzimaApplication applicationContext = (MuzimaApplication) formWebViewActivity.getApplicationContext();
        return new HTMLFormObservationCreator(applicationContext.getPatientController(), applicationContext.getConceptController(),
                applicationContext.getEncounterController(), applicationContext.getObservationController());
    }
}
