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
import com.muzima.api.model.Patient;
import com.muzima.controller.FormController;
import com.muzima.scheduler.RealTimeFormUploader;
import com.muzima.service.HTMLFormObservationCreator;
import com.muzima.utils.Constants;
import com.muzima.utils.StringUtils;

import java.util.Date;

import static com.muzima.utils.Constants.FORM_HTML_DISCRIMINATOR_REGISTRATION;

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
    public String getStatus() {
        return formData == null ? StringUtils.EMPTY : formData.getStatus();
    }

    @JavascriptInterface
    public void saveHTML(String jsonPayload, String status) {
        saveHTML(jsonPayload, status, false);
    }

    @JavascriptInterface
    public void loadJsonPayload(String jsonPayload){
        formWebViewActivity.jsonPayload = jsonPayload;
    }

    @JavascriptInterface
    public void saveHTML(String jsonPayload, String status, boolean keepFormOpen) {
        formData.setJsonPayload(jsonPayload);
        formData.setStatus(status);
        try {
            if (isRegistrationComplete(status)) {
                Patient newPatient = formController.createNewHTMLPatient(jsonPayload);
                formData.setPatientUuid(newPatient.getUuid());
                formWebViewActivity.startPatientSummaryView(newPatient);
            }
            parseForm(jsonPayload, status);
            Date encounterDate = getEncounterDateFromForm(jsonPayload);
            formData.setEncounterDate(encounterDate);
            formController.saveFormData(formData);
            formWebViewActivity.setResult(FormsActivity.RESULT_OK);
            Log.i(TAG, "Saving form data ...");
            if (!keepFormOpen) {
                formWebViewActivity.finish();
                if(status.equals("complete")) {
                    Toast.makeText(formWebViewActivity, "Completed form data is saved successfully.", Toast.LENGTH_SHORT).show();
                    RealTimeFormUploader.getInstance().uploadAllCompletedForms(formWebViewActivity.getApplicationContext());
                }
                if(status.equals("incomplete")) {
                    Toast.makeText(formWebViewActivity, "Draft form data is saved successfully.", Toast.LENGTH_SHORT).show();
                }
            }

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

    private Date getEncounterDateFromForm(String jsonPayload){
        return getFormParser().getEncounterDateFromFormDate(jsonPayload);
    }

    public HTMLFormObservationCreator getFormParser() {
        MuzimaApplication applicationContext = (MuzimaApplication) formWebViewActivity.getApplicationContext();
        return new HTMLFormObservationCreator(applicationContext.getPatientController(), applicationContext.getConceptController(),
                applicationContext.getEncounterController(), applicationContext.getObservationController());
    }
    private boolean isRegistrationComplete(String status) {
        return isRegistrationForm() && status.equals(Constants.STATUS_COMPLETE);
    }
    public boolean isRegistrationForm() {
        return (formData.getDiscriminator() != null) && formData.getDiscriminator().equals(FORM_HTML_DISCRIMINATOR_REGISTRATION);
    }
}
