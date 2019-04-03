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

import com.muzima.api.model.FormData;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PatientIdentifier;
import com.muzima.api.model.PatientIdentifierType;
import com.muzima.api.model.PersonName;
import com.muzima.utils.Constants;
import com.muzima.utils.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.muzima.utils.DateUtils.getFormattedDate;
import static com.muzima.utils.DateUtils.parse;
import static java.util.Arrays.asList;

public class PatientJSONMapper {

    private final JSONObject model;

    public PatientJSONMapper(String modelJSON) throws JSONException {
        model = new JSONObject(modelJSON);
    }

    public String map(Patient patient, FormData formData) throws JSONException {
        Map<String, String> valueMap = convert(patient,formData);
        JSONObject form = model.getJSONObject("form");
        JSONArray fields = form.getJSONArray("fields");
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.getJSONObject(i);
            String name = field.getString("name");
            if (valueMap.containsKey(name)) {
                field.put("value", valueMap.get(name));
            }
        }
        return model.toString();
    }

    public Patient getPatient() throws JSONException {
        JSONObject form = model.getJSONObject("form");
        JSONArray fields = form.getJSONArray("fields");
        Map<String, String> paramsMap = convertJSONToPatientAttr(fields);
        return patient(paramsMap);
    }

    private Patient patient(Map<String, String> paramsMap) {
        Patient patient = new Patient();
        patient.setUuid(paramsMap.get("patient.uuid"));
        patient.setIdentifiers(asList(patientIdentifier(patient.getUuid()), preferredIdentifier(paramsMap)));
        patient.setNames(Collections.singletonList(personName(paramsMap)));
        patient.setGender(paramsMap.get("patient.sex"));
        patient.setBirthdate(getDate(paramsMap));
        return patient;
    }

    private PatientIdentifier preferredIdentifier(Map<String, String> paramsMap) {
        PatientIdentifier patientIdentifier = patientIdentifier(paramsMap.get("patient.medical_record_number"));
        patientIdentifier.setPreferred(true);
        return patientIdentifier;
    }

    private PatientIdentifier patientIdentifier(String uuid) {
        PatientIdentifier patientIdentifier = new PatientIdentifier();
        PatientIdentifierType identifierType = new PatientIdentifierType();
        identifierType.setName(Constants.LOCAL_PATIENT);
        patientIdentifier.setIdentifierType(identifierType);
        patientIdentifier.setIdentifier(uuid);
        return patientIdentifier;
    }

    private Date getDate(Map<String, String> paramsMap) {
        String dateAsString = paramsMap.get("patient.birthdate");
        try {
            return dateAsString == null ? null : parse(dateAsString);
        } catch (ParseException e) {
            return null;
        }
    }

    private PersonName personName(Map<String, String> paramsMap) {
        PersonName personName = new PersonName();
        personName.setFamilyName(paramsMap.get("patient.family_name"));
        personName.setMiddleName(paramsMap.get("patient.middle_name"));
        personName.setGivenName(paramsMap.get("patient.given_name"));
        return personName;
    }

    private Map<String, String> convertJSONToPatientAttr(JSONArray fields) throws JSONException {
        Map<String, String> patientParamsMap = new HashMap<>();
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.getJSONObject(i);
            try {
                patientParamsMap.put(field.getString("name"), field.getString("value"));
            } catch (JSONException e) {
                //Ignore
            }
        }
        return patientParamsMap;
    }

    private Map<String, String> convert(Patient patient, FormData formData) {
        Map<String, String> patientValueMap = new HashMap<>();
        patientValueMap.put("patient.medical_record_number", StringUtils.defaultString(patient.getIdentifier()));
        patientValueMap.put("patient.family_name", StringUtils.defaultString(patient.getFamilyName()));
        patientValueMap.put("patient.given_name", StringUtils.defaultString(patient.getGivenName()));
        patientValueMap.put("patient.middle_name", StringUtils.defaultString(patient.getMiddleName()));
        patientValueMap.put("patient.sex", StringUtils.defaultString(patient.getGender()));
        patientValueMap.put("patient.uuid", StringUtils.defaultString(patient.getUuid()));
        patientValueMap.put("encounter.form_uuid", StringUtils.defaultString(formData.getTemplateUuid()));
        if (patient.getBirthdate() != null) {
            patientValueMap.put("patient.birthdate", getFormattedDate(patient.getBirthdate()));
        }
        return patientValueMap;
    }
}
