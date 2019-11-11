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
import com.muzima.api.model.CohortMember;
import com.muzima.api.model.Concept;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.FormData;
import com.muzima.api.model.Location;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Provider;
import com.muzima.api.model.Tag;
import com.muzima.controller.CohortController;
import com.muzima.controller.ConceptController;
import com.muzima.controller.FormController;
import com.muzima.controller.LocationController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.controller.PatientController;
import com.muzima.controller.ProviderController;
import com.muzima.model.location.MuzimaGPSLocation;
import com.muzima.scheduler.RealTimeFormUploader;
import com.muzima.service.GPSFeaturePreferenceService;
import com.muzima.service.HTMLFormObservationCreator;
import com.muzima.service.MuzimaGPSLocationService;
import com.muzima.service.MuzimaLoggerService;
import com.muzima.utils.Constants;
import com.muzima.utils.StringUtils;

import net.minidev.json.JSONValue;

import org.json.JSONException;

import com.muzima.controller.EncounterController;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONObject;

import static com.muzima.utils.Constants.STANDARD_DATE_FORMAT;
import static com.muzima.utils.Constants.STATUS_COMPLETE;
import static com.muzima.utils.Constants.STATUS_INCOMPLETE;


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
    private final CohortController cohortController;
    private final PatientController patientController;

    public HTMLFormDataStore(HTMLFormWebViewActivity formWebViewActivity, FormData formData, boolean isFormReload, MuzimaApplication application) {
        this.formWebViewActivity = formWebViewActivity;
        this.formData = formData;

        this.formController = application.getFormController();
        this.providerController = application.getProviderController();
        this.locationController = application.getLocationController();
        this.settingController = application.getMuzimaSettingController();
        this.conceptController = application.getConceptController();
        this.encounterController = application.getEncounterController();
        this.observationController = application.getObservationController();
        this.cohortController = application.getCohortController();
        this.patientController = application.getPatientController();
        this.application = application;
        logFormStartEvent(isFormReload);
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
        String patientUuid = formData.getPatientUuid();
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
                if (status.equals("complete")) {
                    JSONObject jsonObject = new JSONObject(jsonPayload);
                    JSONObject jsonObjectInner = jsonObject.getJSONObject("patient");
                    Log.e(getClass().getSimpleName(),jsonObjectInner.toString());
                    if(jsonObjectInner.has("patient.tagName") && jsonObjectInner.has("patient.tagUuid")) {
                        Log.e(getClass().getSimpleName(),"Form Has both tag fields");
                        List<Tag> tags = new ArrayList<Tag>();
                        Patient patient = patientController.getPatientByUuid(patientUuid);
                        for (Tag tag : patient.getTags()) {
                            tags.add(tag);
                        }

                        Tag tag = new Tag();
                        tag.setName(jsonObjectInner.getString("patient.tagName"));
                        tag.setUuid(jsonObjectInner.getString("patient.tagUuid"));
                        tags.add(tag);
                        patient.setTags(tags.toArray(new Tag[tags.size()]));
                        patientController.updatePatient(patient);
                    }
                }
                if (!keepFormOpen) {
                    formWebViewActivity.finish();
                    if (status.equals("complete")) {
                        Toast.makeText(formWebViewActivity, formWebViewActivity.getString(R.string.info_form_data_save_success), Toast.LENGTH_SHORT).show();
                        RealTimeFormUploader.getInstance().uploadAllCompletedForms(formWebViewActivity.getApplicationContext(),false);
                    }
                    if (status.equals("incomplete")) {
                        Toast.makeText(formWebViewActivity, formWebViewActivity.getString(R.string.info_draft_form_save_success), Toast.LENGTH_SHORT).show();
                    }
                    logFormSaveEvent(status);
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
        }
        catch (PatientController.PatientLoadException e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while fetching patient", e);
        }
        catch (PatientController.PatientSaveException e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while saving patient", e);
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while parsing object", e);
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

    private boolean isRegistrationForm() {
        return formController.isRegistrationFormData(formData);
    }

    private boolean isEncounterForm() {
        return formController.isEncounterFormData(formData);
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
        } catch (EncounterController.DownloadEncounterException e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while loading encounters", e);
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "ExceptioJSONValuen occurred while loading encounters", e);
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
            Collections.sort(observations, observationDateTimeComparator);
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
            Collections.sort(observations, observationDateTimeComparator);
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
            Collections.sort(observations, observationDateTimeComparator);
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
            Date obsDateTime = null;
            Date valueDateTime = null;
            try {
                obsDateTime = newDateFormat.parse(newDateFormat.format(obs.getObservationDatetime()));
                if(obs.getValueDatetime() != null) {
                    valueDateTime = newDateFormat.parse(newDateFormat.format(obs.getValueDatetime()));
                }
            } catch (ParseException e) {
                Log.e(getClass().getSimpleName(), "Exception occurred while parsing date", e);
            }
            newDateFormat.applyPattern(dateFormat);
            String convertedEncounterDate = newDateFormat.format(obsDateTime);
            String convertedvalueDateTime = "";
            if(valueDateTime != null){
                 convertedvalueDateTime = newDateFormat.format(valueDateTime);
            }

            JSONObject json = new JSONObject();
            JSONObject codedConcept = new JSONObject();
            if (!conceptName.isEmpty()) {
                json.put("conceptName", conceptName);
            } else {
                json.put("conceptName", "Concept Created On Phone");
            }
            json.put("obsDate", convertedEncounterDate);
            if(obs.getValueCoded() != null) {
                codedConcept.put("uuid",obs.getValueCoded().getUuid());
                codedConcept.put("id",obs.getValueCoded().getId());
                codedConcept.put("name",obs.getValueCoded().getName());
                json.put("valueCoded",codedConcept);
            }else{
                json.put("valueCoded", obs.getValueCoded());
            }
            json.put("valueNumeric", obs.getValueNumeric());
            json.put("valueText", obs.getValueText());
            json.put("valueDatetime",convertedvalueDateTime);
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
            MuzimaGPSLocationService muzimaLocationService = application.getMuzimaGPSLocationService();

            if (muzimaLocationService.isGPSLocationPermissionsGranted()) {
                if(muzimaLocationService.isLocationServicesSwitchedOn()){
                    HashMap<String, Object> locationDataHashMap;
                    try {
                        locationDataHashMap = muzimaLocationService.getLastKnownGPS();
                        if(locationDataHashMap.containsKey("gps_location")) {
                            if (jsonReturnType.equals("json-object")){
                                gps_location_string = ((MuzimaGPSLocation)locationDataHashMap.get("gps_location")).toJsonObject().toString();
                            } else {
                                gps_location_string = ((MuzimaGPSLocation)locationDataHashMap.get("gps_location")).toJsonArray().toString();
                            }
                        } else {
                            gps_location_string = (String)locationDataHashMap.get("gps_location_status");
                        }
                        return gps_location_string;
                    } catch (Exception e) {
                        Log.e(getClass().getSimpleName(), "Unable to process gps data, unknow Error Occurred", e);
                        return gps_location_string;
                    }
                } else {
                    return "Location service disabled by user";
                }

            } else {
                return "Location Permissions Denied By User.";
            }
        } else {
            return "GPS Feature is Disabled by User";
        }

    }

    @JavascriptInterface
    public void logEvent(String tag, String details){
        MuzimaLoggerService.log(formWebViewActivity,tag,details);
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

    @JavascriptInterface
    public String getCohortMembershipByPatientUuid(String patientUuid){
        List<CohortMember> cohortMembers = new ArrayList<>();
        JSONArray jsonArray = new JSONArray();
        HashMap<String, JSONObject> map = new HashMap<>();
        int i = 0;
        try {
            cohortMembers = cohortController.getCohortMembershipByPatientUuid(patientUuid);
            for(CohortMember cohortMember:cohortMembers){
                JSONObject json = new JSONObject();
                json.put("cohortUuid", cohortMember.getCohort().getUuid());
                json.put("cohortName", cohortMember.getCohort().getName());
                map.put("json" + i, json);
                jsonArray.put(map.get("json" + i));
                i++;
            }
        } catch (CohortController.CohortFetchException e) {
            Log.e(getClass().getSimpleName(), "Exception occurred while loading cohort membership", e);
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "JSONException encountered while process cohort membership", e);
        }
        return jsonArray.toString();
    }

    private void logFormStartEvent(boolean isFormReload){
        try {
            JSONObject eventDetails = new JSONObject();
            eventDetails.put("patientuuid", formData.getPatientUuid());
            eventDetails.put("formDataUuid", formData.getUuid());

            MuzimaGPSLocationService muzimaLocationService = application.getMuzimaGPSLocationService();

            HashMap<String, Object> locationDataHashMap = muzimaLocationService.getLastKnownGPS();
            if(locationDataHashMap.containsKey("gps_location")) {
                MuzimaGPSLocation muzimaGPSLocation = ((MuzimaGPSLocation)locationDataHashMap.get("gps_location"));
                eventDetails.put("location", muzimaGPSLocation.toJsonObject());
            }

            if (isEncounterForm()) {
                if(isFormReload) {
                    logEvent("RESUME_ENCOUNTER_FORM", eventDetails.toString());
                } else {
                    logEvent("OPEN_ENCOUNTER_FORM", eventDetails.toString());
                }
            } else {
                if(isFormReload) {
                    logEvent("RESUME_REGISTRATION_FORM", eventDetails.toString());
                } else {
                    logEvent("OPEN_REGISTRATION_FORM", eventDetails.toString());
                }
            }
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(),"Cannot create log",e);
        }
    }

    private void logFormSaveEvent(String status){
        try {
            JSONObject eventDetails = new JSONObject();
            eventDetails.put("patientuuid", formData.getPatientUuid());
            eventDetails.put("formDataUuid", formData.getUuid());

            switch(status) {
                case STATUS_COMPLETE :
                    if(isEncounterForm()){
                        logEvent( "SAVE_COMPLETE_ENCOUNTER_FORM", eventDetails.toString());
                    } else {
                        logEvent( "SAVE_COMPLETE_REGISTRATION_FORM", eventDetails.toString());
                    }
                    break;
                case STATUS_INCOMPLETE :
                    if(isEncounterForm()){
                        logEvent( "SAVE_DRAFT_ENCOUNTER_FORM", eventDetails.toString());
                    } else {
                        logEvent( "SAVE_DRAFT_REGISTRATION_FORM", eventDetails.toString());
                    }
                    break;
            }
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(),"Cannot create log",e);
        }
    }

    private final Comparator<Observation> observationDateTimeComparator = new Comparator<Observation>() {
        @Override
        public int compare(Observation lhs, Observation rhs) {
            return -lhs.getObservationDatetime().compareTo(rhs.getObservationDatetime());
        }
    };

}