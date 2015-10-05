/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.FormData;
import com.muzima.api.model.Location;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Provider;
import com.muzima.controller.FormController;
import com.muzima.controller.LocationController;
import com.muzima.controller.ProviderController;
import com.muzima.scheduler.RealTimeFormUploader;
import com.muzima.service.HTMLFormObservationCreator;
import com.muzima.utils.Constants;
import com.muzima.utils.StringUtils;
import net.minidev.json.JSONValue;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.muzima.utils.Constants.FORM_JSON_DISCRIMINATOR_REGISTRATION;

public class HTMLFormDataStore {
    private static final String TAG = "FormDataStore";

    private HTMLFormWebViewActivity formWebViewActivity;
    private FormController formController;
    private LocationController locationController;
    private FormData formData;
    private ProviderController providerController;

    public HTMLFormDataStore(HTMLFormWebViewActivity formWebViewActivity, FormController formController, LocationController locationController, FormData formData, ProviderController providerController) {
        this.formWebViewActivity = formWebViewActivity;
        this.formController = formController;
        this.formData = formData;
        this.providerController = providerController;
        this.locationController = locationController;
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
                if (status.equals("complete")) {
                    Toast.makeText(formWebViewActivity, "Completed form data is saved successfully.", Toast.LENGTH_SHORT).show();
                    RealTimeFormUploader.getInstance().uploadAllCompletedForms(formWebViewActivity.getApplicationContext());
                }
                if (status.equals("incomplete")) {
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

    @JavascriptInterface
    public String getLocationNamesFromDevice() throws JSONException {
        List<Location> locationsOnDevice = new ArrayList<Location>();
        try {
            locationsOnDevice = locationController.getAllLocations();
        } catch (LocationController.LocationLoadException e) {
            Toast.makeText(formWebViewActivity, "An error occurred while loading locations for the form", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Exception occurred while loading locations", e);
        }
        return JSONValue.toJSONString(locationsOnDevice);
    }

    @JavascriptInterface
    public String getProviderNamesFromDevice() throws JSONException {
        List<Provider> providersOnDevice = new ArrayList<Provider>();
        try {
            providersOnDevice = providerController.getAllProviders();
        } catch (ProviderController.ProviderLoadException e) {
            Toast.makeText(formWebViewActivity, "An error occurred while loading provider for the form", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Exception occurred while loading providers", e);
            e.printStackTrace();
        }
        return JSONValue.toJSONString(providersOnDevice);
    }

    @JavascriptInterface
    public String getDefaultEncounterProvider()
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(formWebViewActivity.getApplicationContext());
        boolean encounterProviderPreference = preferences.getBoolean("encounterProviderPreference", false);
        List<Provider> providers = new ArrayList<Provider>();

        if(encounterProviderPreference){
            MuzimaApplication applicationContext = (MuzimaApplication) formWebViewActivity.getApplicationContext();
            providers.add(providerController.getProviderBySystemId(applicationContext.getAuthenticatedUser().getSystemId()));
            return JSONValue.toJSONString(providers);
        }
        return JSONValue.toJSONString(providers);
    }

    @JavascriptInterface
    public String getFontSizePreference()
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(formWebViewActivity.getApplicationContext());
        return preferences.getString(formWebViewActivity.getResources().getString(R.string.preference_font_size),
                HTMLFormWebViewActivity.DEFAULT_FONT_SIZE).toLowerCase();
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
        return (formData.getDiscriminator() != null) && formData.getDiscriminator().equals(FORM_JSON_DISCRIMINATOR_REGISTRATION);
    }
}
