/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Concept;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.FormData;
import com.muzima.api.model.Location;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Provider;
import com.muzima.controller.ConceptController;
import com.muzima.controller.FormController;
import com.muzima.controller.LocationController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.controller.ProviderController;
import com.muzima.model.location.MuzimaGPSLocation;
import com.muzima.scheduler.RealTimeFormUploader;
import com.muzima.service.GPSFeaturePreferenceService;
import com.muzima.service.HTMLFormObservationCreator;
import com.muzima.service.MuzimaLocationService;
import com.muzima.utils.Constants;
import com.muzima.utils.StringUtils;

import net.minidev.json.JSONValue;

import org.json.JSONException;

import com.muzima.controller.EncounterController;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONObject;

import static com.muzima.utils.Constants.STANDARD_DATE_FORMAT;


class HTMLFormDataStore {

    private final HTMLFormWebViewActivity formWebViewActivity;
    private final FormController formController;
    private final LocationController locationController;
    private final ConceptController conceptController;
    private final ObservationController observationController;
    private final FormData formData;
    private final ProviderController providerController;
    private final EncounterController encounterController;
    private final MuzimaApplication application;
    private final MuzimaSettingController settingController;

    public HTMLFormDataStore(HTMLFormWebViewActivity formWebViewActivity, FormData formData, MuzimaApplication application) {
        this.formWebViewActivity = formWebViewActivity;
        this.formData = formData;

        this.formController = application.getFormController();
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
        jsonPayload = injectUserSystemIdToEncounterPayload(jsonPayload);
        jsonPayload = injectTimeZoneToEncounterPayload(jsonPayload);
        formData.setJsonPayload(jsonPayload);
        formData.setStatus(status);
        boolean encounterDetailsValidityStatus = true;
        try {
            if (status.equals("complete")) {
                encounterDetailsValidityStatus = areMandatoryEncounterDetailsInForm(jsonPayload);
            }

            if (encounterDetailsValidityStatus) {

                if (isRegistrationComplete(status)) {
                    Patient newPatient = formController.createNewPatient(application, formData);
                    formData.setPatientUuid(newPatient.getUuid());
                    formWebViewActivity.startPatientSummaryView(newPatient);
                }

                parseForm(jsonPayload, status);

                Date encounterDate = getEncounterDateFromForm(jsonPayload);
                formData.setEncounterDate(encounterDate);
                formController.saveFormData(formData);
                formWebViewActivity.setResult(FormsActivity.RESULT_OK);
                if (!keepFormOpen) {
                    formWebViewActivity.finish();
                    if (status.equals("complete")) {
                        Toast.makeText(formWebViewActivity, formWebViewActivity.getString(R.string.info_form_data_save_success), Toast.LENGTH_SHORT).show();
                        RealTimeFormUploader.getInstance().uploadAllCompletedForms(formWebViewActivity.getApplicationContext(),false);
                    }
                    if (status.equals("incomplete")) {
                        Toast.makeText(formWebViewActivity, formWebViewActivity.getString(R.string.info_draft_form_save_success), Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                String missingMandatoryEncounterDetailsMessage = checkMisssingMandatoryEncounterDetails(jsonPayload);
                String message = missingMandatoryEncounterDetailsMessage.concat(" ");
                message = message.concat(formWebViewActivity.getString(R.string.message_missing_form_encounter_details_error));

                formWebViewActivity.showMissingEncounterDetailsDialog(message);
            }
        } catch (FormController.FormDataSaveException e) {
            Toast.makeText(formWebViewActivity, formWebViewActivity.getString(R.string.error_form_save), Toast.LENGTH_SHORT).show();
            Log.e(getClass().getSimpleName(), "Exception occurred while saving form data", e);
            // } catch (Exception e) {
            Toast.makeText(formWebViewActivity, formWebViewActivity.getString(R.string.error_form_save), Toast.LENGTH_SHORT).show();
            Log.e(getClass().getSimpleName(), "Exception occurred while saving form data", e);
        }
    }

    @JavascriptInterface
    public String getLocationNamesFromDevice() {
        List<Location> locationsOnDevice = new ArrayList<>();
        try {
            locationsOnDevice = locationController.getAllLocations();
        } catch (LocationController.LocationLoadException e) {
            Toast.makeText(formWebViewActivity, formWebViewActivity.getString(R.string.error_form_location_load), Toast.LENGTH_SHORT).show();
            Log.e(getClass().getSimpleName(), "Exception occurred while loading locations", e);
        }
        return JSONValue.toJSONString(locationsOnDevice);
    }

    @JavascriptInterface
    public String getProviderNamesFromDevice() {
        List<Provider> providersOnDevice = new ArrayList<>();
        try {
            providersOnDevice = providerController.getAllProviders();
        } catch (ProviderController.ProviderLoadException e) {
            Toast.makeText(formWebViewActivity, formWebViewActivity.getString(R.string.error_form_provider_load), Toast.LENGTH_SHORT).show();
            Log.e(getClass().getSimpleName(), "Exception occurred while loading providers", e);
            e.printStackTrace();
        }
        return JSONValue.toJSONString(providersOnDevice);
    }

    @JavascriptInterface
    public String getDefaultEncounterProvider() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(formWebViewActivity.getApplicationContext());
        boolean encounterProviderPreference = preferences.getBoolean("encounterProviderPreference", false);
        List<Provider> providers = new ArrayList<>();

        if (encounterProviderPreference) {
            MuzimaApplication applicationContext = (MuzimaApplication) formWebViewActivity.getApplicationContext();
            providers.add(providerController.getProviderBySystemId(applicationContext.getAuthenticatedUser().getSystemId()));
            return JSONValue.toJSONString(providers);
        }
        return JSONValue.toJSONString(providers);
    }

    @JavascriptInterface
    public String getFontSizePreference() {
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

    public Date getEncounterDateFromForm(String jsonPayload) {
        return getFormParser().getEncounterDateFromFormDate(jsonPayload);
    }

    HTMLFormObservationCreator getFormParser() {
        MuzimaApplication applicationContext = (MuzimaApplication) formWebViewActivity.getApplicationContext();
        return new HTMLFormObservationCreator(applicationContext, false);
    }

    private boolean isRegistrationComplete(String status) {
        return formController.isRegistrationFormData(formData) && status.equals(Constants.STATUS_COMPLETE);
    }

    @JavascriptInterface
    public String getStringResource(String stringResourceName) {
        Context context = formWebViewActivity.getBaseContext();
        return context.getString(context.getResources().getIdentifier(stringResourceName, "string", context.getPackageName()));
    }

    @JavascriptInterface
    public String getConcepts() {
        List<Concept> concepts = new ArrayList<>();
        try {
            concepts = conceptController.getConcepts();
        } catch (ConceptController.ConceptFetchException | Exception e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while loading concepts", e);
        }
        return JSONValue.toJSONString(concepts);
    }

    @JavascriptInterface
    public String getEncountersByPatientUuid(String patientuuid) {
        List<Encounter> encounters = new ArrayList<>();
        try {
            encounters = encounterController.getEncountersByPatientUuid(patientuuid);
        } catch (EncounterController.DownloadEncounterException | Exception e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while loading encounters", e);
        }
        return JSONValue.toJSONString(encounters);
    }

    @JavascriptInterface
    public String getEncounterTypes(String patientuuid) {
        List<Encounter> encounters = new ArrayList<>();
        List<Encounter> encountertypes = new ArrayList<>();
        List<String> encounterTypeArray = new ArrayList<>();
        try {
            encounters = encounterController.getEncountersByPatientUuid(patientuuid);
            for (Encounter encounter : encounters) {
                if (!(encounterTypeArray.contains(encounter.getEncounterType().getName()))) {
                    encounterTypeArray.add(encounter.getEncounterType().getName());
                    encountertypes.add(encounter);
                }
            }
        } catch (EncounterController.DownloadEncounterException | Exception e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while loading encounterTypes", e);
        }
        return JSONValue.toJSONString(encountertypes);
    }

    @JavascriptInterface
    public String getObsByConceptId(String patientUuid, int conceptId) throws JSONException, ConceptController.ConceptFetchException {
        List<Observation> observations = new ArrayList<>();
        try {
            observations = observationController.getObservationsByPatientuuidAndConceptId(patientUuid, conceptId);
        } catch (ObservationController.LoadObservationException | Exception e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while loading observations", e);
        }
        return createObsJsonArray(observations);
    }

    @JavascriptInterface
    public String getObsByEncounterId(int encounterid) throws JSONException, ConceptController.ConceptFetchException {
        List<Observation> observations = new ArrayList<>();
        try {
            observations = observationController.getObservationsByEncounterId(encounterid);
        } catch (ObservationController.LoadObservationException | Exception e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while loading observations", e);
        }
        return createObsJsonArray(observations);
    }

    @JavascriptInterface
    public String getObsByEncounterType(String patientUuid, String encounterType) throws JSONException, ConceptController.ConceptFetchException {
        List<Observation> observations = new ArrayList<>();
        List<Encounter> encounters = new ArrayList<>();
        try {
            encounters = encounterController.getEncountersByPatientUuid(patientUuid);
            for (Encounter enc : encounters) {
                if (enc.getEncounterType().getName().equals(encounterType)) {
                    observations.addAll(observationController.getObservationsByEncounterId(enc.getId()));
                }
            }
        } catch (ObservationController.LoadObservationException | Exception e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while loading observations", e);
        } catch (EncounterController.DownloadEncounterException e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while loading encounters", e);
        }
        return createObsJsonArray(observations);
    }

    private String createObsJsonArray(List<Observation> observations) throws JSONException, ConceptController.ConceptFetchException {
        int i = 0;
        JSONArray arr = new JSONArray();
        HashMap<String, JSONObject> map = new HashMap<>();
        List<Concept> concepts = new ArrayList<>();
        concepts = conceptController.getConcepts();
        for (Observation obs : observations) {
            String conceptName = "";
            String conceptUuid = obs.getConcept().getUuid();
            for (Concept concept : concepts) {
                if (concept.getUuid().equals(conceptUuid)) {
                    conceptName = concept.getName();
                }
            }
            final String dateFormat = STANDARD_DATE_FORMAT;
            SimpleDateFormat newDateFormat = new SimpleDateFormat("dd-MM-yy HH:mm:ss");
            Date d = null;
            try {
                d = newDateFormat.parse(newDateFormat.format(obs.getObservationDatetime()));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            newDateFormat.applyPattern(dateFormat);
            String convertedEncounterDate = newDateFormat.format(d);

            JSONObject json = new JSONObject();
            if (!conceptName.isEmpty()) {
                json.put("conceptName", conceptName);
            } else {
                json.put("conceptName", "Concept Created On Phone");
            }
            json.put("obsDate", convertedEncounterDate);
            json.put("valueCoded", obs.getValueCoded().getName());
            json.put("valueNumeric", obs.getValueNumeric());
            json.put("valueText", obs.getValueText());
            map.put("json" + i, json);
            arr.put(map.get("json" + i));
            i++;
        }
        return arr.toString();
    }

    @JavascriptInterface
    public boolean isMedicalRecordNumberRequired() {
        return settingController.isMedicalRecordNumberRequiredDuringRegistration();
    }

    @JavascriptInterface
    public void checkForPossibleFormDuplicate(String formUuid, String encounterDateTime, String patientUuid, String encounterPayLoad) throws FormController.FormDataFetchException, JSONException {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(formWebViewActivity.getApplicationContext());
        boolean isDuplicateFormDataWarningPreferenceSet = preferences.getBoolean(formWebViewActivity.getResources().getString(R.string.preference_duplicate_form_data_key), HTMLFormWebViewActivity.IS_ALLOWED_FORM_DATA_DUPLICATION);
        if (isDuplicateFormDataWarningPreferenceSet) {
            JSONObject mainObject = new JSONObject(encounterPayLoad);
            JSONObject encounterObject = mainObject.getJSONObject("encounter");
            if (!(encounterObject.has("encounter.encounter_datetime"))) {
                List<FormData> allFormData = new ArrayList<>();
                List<FormData> incompleteForms = new ArrayList<>();
                List<FormData> completeForms = new ArrayList<>();
                incompleteForms = formController.getAllFormDataByPatientUuid(patientUuid, Constants.STATUS_INCOMPLETE);
                completeForms = formController.getAllFormDataByPatientUuid(patientUuid, Constants.STATUS_COMPLETE);
                allFormData.addAll(incompleteForms);
                allFormData.addAll(completeForms);
                for (FormData formData : allFormData) {
                    Date encounterDate = formData.getEncounterDate();
                    String formDataUuid = formData.getTemplateUuid();

                    final String dateFormat = "dd-MM-yyyy";

                    SimpleDateFormat newDateFormat = new SimpleDateFormat("dd-MM-yy HH:mm:ss");
                    Date d = null;
                    try {
                        d = newDateFormat.parse(newDateFormat.format(encounterDate));
                    } catch (ParseException e) {
                        Log.e(getClass().getSimpleName(), e.getMessage());
                    }
                    newDateFormat.applyPattern(dateFormat);
                    String convertedEncounterDate = newDateFormat.format(d);
                    if (convertedEncounterDate.equals(encounterDateTime.substring(0, 10)) && formDataUuid.equals(formUuid)) {
                        formWebViewActivity.showWarningDialog();
                        break;
                    }
                }
            }
        }
    }

    @JavascriptInterface
    public boolean getDefaultEncounterLocationSetting() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(formWebViewActivity.getApplicationContext());
        String defaultLocationName = preferences.getString("defaultEncounterLocation", getStringResource("no_default_encounter_location"));
        String defaultValue = getStringResource("no_default_encounter_location");
        return !defaultLocationName.equals(defaultValue);
    }

    @JavascriptInterface
    public String getDefaultEncounterLocationPreference() throws LocationController.LocationLoadException {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(formWebViewActivity.getApplicationContext());
        String defaultLocationName = preferences.getString("defaultEncounterLocation", getStringResource("no_default_encounter_location"));
        String defaultValue = getStringResource("no_default_encounter_location");
        List<Location> defaultLocation = new ArrayList<>();
        List<Location> locations = new ArrayList<>();

        locations = locationController.getAllLocations();
        if (!defaultLocationName.equals(defaultValue)) {
            for (Location loc : locations) {
                if (Integer.toString(loc.getId()).equals(defaultLocationName)) {
                    defaultLocation.add(loc);
                }
            }
            return JSONValue.toJSONString(defaultLocation);
        }
        return JSONValue.toJSONString(locations);
    }

    private boolean areMandatoryEncounterDetailsInForm(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONObject jsonObjectInner = jsonObject.getJSONObject("encounter");
            return jsonObjectInner.has("encounter.provider_id") && jsonObjectInner.has("encounter.encounter_datetime") && jsonObjectInner.has("encounter.location_id");
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "Error while parsing response JSON", e);
        }
        return false;
    }

    private String injectUserSystemIdToEncounterPayload(String jsonPayload) {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            JSONObject jsonObjectInner = jsonObject.getJSONObject("encounter");
            if (!(jsonObjectInner.has("encounter.user_system_id"))) {
                String user_system_id = ((MuzimaApplication) formWebViewActivity.getApplicationContext()).getAuthenticatedUser().getSystemId();
                jsonObjectInner.put("encounter.user_system_id", user_system_id);
                jsonObject.put("encounter", jsonObjectInner);
                jsonPayload = jsonObject.toString();
            }
            return jsonPayload;
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "Error while parsing response JSON", e);
        }

        return jsonPayload;
    }

    private String checkMisssingMandatoryEncounterDetails(String jsonPayLoad) {
        String message = "";
        try {
            JSONObject jsonObject = new JSONObject(jsonPayLoad);
            JSONObject jsonObjectInner = null;
            if (!(jsonObject.has("encounter"))) {
                Log.e(getClass().getSimpleName(), "No encounter details section in the form");
            } else {
                jsonObjectInner = jsonObject.getJSONObject("encounter");
                if (!(jsonObjectInner.has("encounter.encounter_datetime"))) {
                    message = formWebViewActivity.getString(R.string.form_encounter_date);
                }
                if (!(jsonObjectInner.has("encounter.provider_id"))) {
                    if (!(message.isEmpty())) {
                        message = message.concat(", ");
                        message = message.concat(formWebViewActivity.getString(R.string.form_encounter_provider));
                    } else {
                        message = formWebViewActivity.getString(R.string.form_encounter_provider);
                    }
                }
                if (!(jsonObjectInner.has("encounter.location_id"))) {
                    if (!(message.isEmpty())) {
                        message = message.concat(", ");
                        message = message.concat(formWebViewActivity.getString(R.string.form_encounter_location));
                    } else {
                        message = formWebViewActivity.getString(R.string.form_encounter_location);
                    }
                }
                return message;
            }
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "Error while parsing response JSON. Unparsable jsonPayLoad", e);
        }
        return null;
    }

    @JavascriptInterface
    public String getLastKnowGPSLocation(String jsonReturnType) {
        String gps_location_string = "Unknown Error Occured!";
        Boolean isGpsFeatureEnabled = false;
        isGpsFeatureEnabled = new GPSFeaturePreferenceService(application).isGPSDataCollectionSettingEnabled();
        if (isGpsFeatureEnabled) {

            if (isLocationPermissionsGranted()) {

                if(isLocationServicesEnabled()){
                    MuzimaLocationService muzimaLocationService = new MuzimaLocationService(application);
                    HashMap<String, String> locationDataHashMap = new HashMap<>(); //empty hashmap prevent NullPointerException
                    try {
                        locationDataHashMap = muzimaLocationService.getLastKnownGPS(jsonReturnType);
                        gps_location_string = locationDataHashMap.get("gps_location_string");
                        return gps_location_string;
                    } catch (Exception e) {
                        Log.e(getClass().getSimpleName(), "Unable to process gps data, unknow Error Occurred" + e.getMessage());
                        return gps_location_string;
                    }
                }else {
                    return "Location service disabled by user";
                }

            } else {
                return "Location Permissions Denied By User.";
            }
        } else {
            return "GPS Feature is Disabled by User";
        }

    }

    public boolean isLocationPermissionsGranted() {
        int permissionStatus = ActivityCompat.checkSelfPermission(application, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionStatus == PackageManager.PERMISSION_GRANTED)
            return true;
        else if (permissionStatus == PackageManager.PERMISSION_DENIED)
            return false;
        else
            return false;
    }

    public Boolean isLocationServicesEnabled(){
        LocationManager locationManager = (LocationManager)application.getSystemService(Context.LOCATION_SERVICE);

        boolean isGPSProviderEnabled = false;
        boolean isNetworkEnabled = false;

        if(locationManager != null){
            isGPSProviderEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }

        return (isGPSProviderEnabled || isNetworkEnabled);
    }

    public void showLocationDisabledDialog(){
        Boolean isGPSDataCollectionSettingEnabled = new GPSFeaturePreferenceService(application).isGPSDataCollectionSettingEnabled();
        if(isGPSDataCollectionSettingEnabled) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(application);
            alertDialog.setTitle(formWebViewActivity.getString(R.string.title_enable_gps_location));
            alertDialog.setMessage(formWebViewActivity.getString(R.string.hint_gps_location_off));
            alertDialog.setPositiveButton(formWebViewActivity.getString(R.string.general_location_setting), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    application.startActivity(intent);
                }
            });

            alertDialog.show();
        }
    }

    private String injectTimeZoneToEncounterPayload(String jsonPayload) {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            JSONObject jsonObjectInner = jsonObject.getJSONObject("encounter");
            if (!(jsonObjectInner.has("encounter.device_time_zone"))) {
                String device_time_zone = TimeZone.getDefault().getID();
                jsonObjectInner.put("encounter.device_time_zone", device_time_zone);
                jsonObject.put("encounter", jsonObjectInner);
                jsonPayload = jsonObject.toString();
            }
            return jsonPayload;
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "Error while parsing response JSON", e);
        }

        return jsonPayload;
    }

}