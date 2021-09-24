/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.utils;

import android.util.Log;

import com.muzima.api.model.Concept;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.FormData;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Patient;
import com.muzima.api.model.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

import com.muzima.controller.FormController;
import com.muzima.MuzimaApplication;
import com.muzima.view.forms.HTMLPatientJSONMapper;

import static com.muzima.utils.Constants.FORM_JSON_DISCRIMINATOR_INDIVIDUAL_OBS;
import static com.muzima.utils.Constants.STATUS_COMPLETE;

public class IndividualObsJsonMapper {

    private User currentUser;
    private String jsonPayload;
    private String patientEncounterPayload;
    private Patient patient;
    private Encounter encounter;
    private FormData formData;
    private Observation observation;
    private MuzimaApplication muzimaApplication;
    private FormController formController;

    public IndividualObsJsonMapper(Observation observation, Patient patient, MuzimaApplication muzimaApplication){
        this.observation = observation;
        this.muzimaApplication = muzimaApplication;
        this.formController = muzimaApplication.getFormController();
        this.patient =patient;
    }


    public FormData createFormDataFromObservation() throws JSONException {
        formData = new FormData();
        encounter = observation.getEncounter();
        currentUser = muzimaApplication.getAuthenticatedUser();
        formData.setDiscriminator(FORM_JSON_DISCRIMINATOR_INDIVIDUAL_OBS);
        formData.setEncounterDate(encounter.getEncounterDatetime());
        formData.setPatientUuid(patient.getUuid());
        formData.setStatus(STATUS_COMPLETE);
        formData.setUserUuid(currentUser.getUuid());
        formData.setTemplateUuid(UUID.randomUUID().toString());
        formData.setUuid(UUID.randomUUID().toString());
        formData.setUserSystemId((muzimaApplication).getAuthenticatedUser( ).getSystemId());
        formData.setSaveTime(observation.getObservationDatetime());
        formData.setXmlPayload(null);
        patientEncounterPayload = createJsonPayloadFromPatientEncounter(patient,formData);
        jsonPayload = createJsonPayloadFromObs(observation);

        formData.setJsonPayload(jsonPayload);

        return formData;
    }

    private String createJsonPayloadFromPatientEncounter(Patient patient, FormData formData){
        String payload = new HTMLPatientJSONMapper().map(muzimaApplication, patient, formData, true);
        try {
            JSONObject jsonObject = new JSONObject(payload);
            JSONObject discriminatorObject = new JSONObject();
            JSONObject jsonObjectInner = jsonObject.getJSONObject("encounter");
            if(!(jsonObjectInner.has("encounter.encounter_datetime"))) {
                jsonObjectInner.put("encounter.encounter_datetime", DateUtils.getFormattedDateTime(formData.getEncounterDate()));
                jsonObject.put("encounter.encounter_uuid",encounter.getUuid());
                jsonObject.put("encounter", jsonObjectInner);
            }
            if(!(jsonObjectInner.has("encounter.encounter_uuid"))){
                jsonObjectInner.put("encounter.encounter_uuid",encounter.getUuid());
                jsonObject.put("encounter", jsonObjectInner);
            }
            discriminatorObject.put("discriminator", Constants.FORM_JSON_DISCRIMINATOR_INDIVIDUAL_OBS);
            jsonObject.put("discriminator",discriminatorObject);
            payload = jsonObject.toString();

            return  payload;
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "Error while parsing response JSON", e);
        }

        return payload;
    }

    private String createJsonPayloadFromObs(Observation observation) throws JSONException {
        Concept concept = observation.getConcept();
        String conceptName = concept.getName();
        String conceptId = Integer.toString(concept.getId());


        JSONObject obsNodeJSonJsonObject = new JSONObject();
        obsNodeJSonJsonObject.put(conceptId+"^"+conceptName+"^99DCT",observation.getValueAsString());

        JSONObject jsonMainObject = new JSONObject(patientEncounterPayload);
        jsonMainObject.put("observation",obsNodeJSonJsonObject);

        String obsJsonString = jsonMainObject.toString();
        return obsJsonString;
    }

    public FormData getFormData(){
        return formData;
    }

}
