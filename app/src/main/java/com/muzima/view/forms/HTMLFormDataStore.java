/*
 * Copyright (c) 2014 - 2017. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Concept;
import com.muzima.api.model.Form;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.EncounterType;
import com.muzima.api.model.FormData;
import com.muzima.api.model.Location;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Provider;
import com.muzima.api.model.SmartCardRecord;
import com.muzima.controller.ConceptController;
import com.muzima.controller.FormController;
import com.muzima.controller.LocationController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.controller.PatientController;
import com.muzima.controller.ProviderController;
import com.muzima.controller.SmartCardController;
import com.muzima.model.observation.Concepts;
import com.muzima.model.observation.EncounterWithObservations;
import com.muzima.model.observation.Encounters;
import com.muzima.model.shr.kenyaemr.KenyaEmrShrModel;
import com.muzima.scheduler.RealTimeFormUploader;
import com.muzima.service.HTMLFormObservationCreator;
import com.muzima.utils.Constants;
import com.muzima.utils.StringUtils;
import com.muzima.utils.smartcard.KenyaEmrShrMapper;
import net.minidev.json.JSONValue;
import org.json.JSONException;
import com.muzima.controller.EncounterController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.json.JSONArray;


public class HTMLFormDataStore {
    private static final String TAG = "FormDataStore";

    private HTMLFormWebViewActivity formWebViewActivity;
    private FormController formController;
    private LocationController locationController;
    private ConceptController conceptController;
    private ObservationController observationController;
    private FormData formData;
    private ProviderController providerController;
    private EncounterController encounterController;
    private MuzimaApplication application;
    private MuzimaSettingController settingController;

    public HTMLFormDataStore(HTMLFormWebViewActivity formWebViewActivity,FormData formData, MuzimaApplication application) {
        this.formWebViewActivity = formWebViewActivity;
        this.formController = application.getFormController();
        this.formData = formData;

        this.providerController = application.getProviderController();
        this.locationController = application.getLocationController();
        this.settingController = application.getMuzimaSettingController();
        this.conceptController = application.getConceptController();
        this.encounterController = application.getEncounterController();
        this.observationController = application.getObservationController();
        this.application = application;
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
                Patient newPatient = formController.createNewPatient(formData);
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

                    KenyaEmrShrMapper.updateSHRSmartCardRecordForPatient(application,formData.getPatientUuid());

                    Toast.makeText(formWebViewActivity, formWebViewActivity.getString(R.string.info_form_data_save_success), Toast.LENGTH_SHORT).show();
                    RealTimeFormUploader.getInstance().uploadAllCompletedForms(formWebViewActivity.getApplicationContext());
                }
                if (status.equals("incomplete")) {
                    Toast.makeText(formWebViewActivity, formWebViewActivity.getString(R.string.info_draft_form_save_success), Toast.LENGTH_SHORT).show();
                }
            }

        } catch (FormController.FormDataSaveException e) {
            Toast.makeText(formWebViewActivity, formWebViewActivity.getString(R.string.error_form_save), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Exception occurred while saving form data", e);
        } catch (Exception e) {
            Toast.makeText(formWebViewActivity, formWebViewActivity.getString(R.string.error_form_save), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Exception occurred while saving form data", e);
        }
    }

    @JavascriptInterface
    public String getLocationNamesFromDevice() throws JSONException {
        List<Location> locationsOnDevice = new ArrayList<Location>();
        try {
            locationsOnDevice = locationController.getAllLocations();
        } catch (LocationController.LocationLoadException e) {
            Toast.makeText(formWebViewActivity, formWebViewActivity.getString(R.string.error_form_location_load), Toast.LENGTH_SHORT).show();
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
            Toast.makeText(formWebViewActivity, formWebViewActivity.getString(R.string.error_form_provider_load), Toast.LENGTH_SHORT).show();
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
        return new HTMLFormObservationCreator(applicationContext);
    }
    private boolean isRegistrationComplete(String status) {
        return formController.isRegistrationFormData(formData) && status.equals(Constants.STATUS_COMPLETE);
    }

    @JavascriptInterface
    public String getStringResource(String stringResourceName){
        Context context = formWebViewActivity.getBaseContext();
        return context.getString(context.getResources().getIdentifier(stringResourceName, "string",context.getPackageName()));
    }

    @JavascriptInterface
    public String getConcepts() throws JSONException {
        List<Concept> concepts = new ArrayList<Concept>();
        try {
               concepts = conceptController.getConcepts();
       } catch (ConceptController.ConceptFetchException e) {
            Log.e(TAG, "Exception occurred while loading concepts", e);
        }
        catch (Exception e){
            Log.e(TAG, "Exception occurred while loading concepts", e);
        }
        return JSONValue.toJSONString(concepts);
    }

    @JavascriptInterface
    public String getEncountersByPatientUuid(String patientuuid) throws JSONException {
        List<Encounter> encounters = new ArrayList<Encounter>();
        try {
            encounters = encounterController.getEncountersByPatientUuid(patientuuid);
        } catch (EncounterController.DownloadEncounterException e) {
            Log.e(TAG, "Exception occurred while loading encounters", e);
        }
        catch (Exception e){
            Log.e(TAG, "ExceptioJSONValuen occurred while loading encounters", e);
        }
        return JSONValue.toJSONString(encounters);
    }

    @JavascriptInterface
    public String getEncounterTypes(String patientuuid) throws JSONException{
        List<Encounter> encounters = new ArrayList<Encounter>();
        List<Encounter> encountertypes = new ArrayList<Encounter>();
        List<String> encounterTypeArray = new ArrayList<String>();
        try {
            encounters = encounterController.getEncountersByPatientUuid(patientuuid);
            for(Encounter encounter:encounters){
                if(!(encounterTypeArray.contains(encounter.getEncounterType().getName()))){
                    encounterTypeArray.add(encounter.getEncounterType().getName());
                    encountertypes.add(encounter);
                }
            }
        } catch (EncounterController.DownloadEncounterException e) {
            Log.e(TAG, "Exception occurred while loading encounterTypes", e);
        }
        catch (Exception e){
            Log.e(TAG, "Exception occurred while loading encounterTypes", e);
        }
        return JSONValue.toJSONString(encountertypes);
    }

    @JavascriptInterface
    public String getObsByConceptId(String patientUuid,int conceptId) throws JSONException{
        List<Observation> observations = new ArrayList<Observation>();
        try {
            observations = observationController.getObservationsByPatientuuidAndConceptId(patientUuid,conceptId);
        } catch (ObservationController.LoadObservationException e) {
            Log.e(TAG, "Exception occurred while loading observations", e);
        }
        catch (Exception e){
            Log.e(TAG, "Exception occurred while loading observations", e);
        }
        return JSONValue.toJSONString(observations);
    }

    @JavascriptInterface
    public String getObsByEncounterId(int encounterid){
        List<Observation> observations = new ArrayList<Observation>();
        try {
            observations = observationController.getObservationsByEncounterId(encounterid);
        } catch (ObservationController.LoadObservationException e) {
            Log.e(TAG, "Exception occurred while loading observations", e);
        }
        catch (Exception e){
            Log.e(TAG, "Exception occurred while loading observations", e);
        }
        return JSONValue.toJSONString(observations);
    }

    @JavascriptInterface
    public String getObsByEncounterType(String patientUuid,String encounterType){
        List<Observation> observations = new ArrayList<Observation>();
        List<Encounter> encounters=new ArrayList<Encounter>();
        try {
            encounters = encounterController.getEncountersByPatientUuid(patientUuid);
            for(Encounter enc:encounters){
                if(enc.getEncounterType().getName().equals(encounterType)) {
                    observations.addAll(observationController.getObservationsByEncounterId(enc.getId()));
                }
            }
        } catch (ObservationController.LoadObservationException e) {
            Log.e(TAG, "Exception occurred while loading observations", e);
        } catch (EncounterController.DownloadEncounterException e) {
            Log.e(TAG, "Exception occurred while loading encounters", e);
        } catch (Exception e){
            Log.e(TAG, "Exception occurred while loading observations", e);
        }
        return JSONValue.toJSONString(observations);
    }

    public boolean isMedicalRecordNumberRequired(){
        return settingController.isMedicalRecordNumberRequiredDuringRegistration();
    }
}
