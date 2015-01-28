/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.forms;

import com.muzima.api.model.FormData;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PersonName;
import com.muzima.api.model.User;
import com.muzima.api.model.PatientIdentifier;
import com.muzima.api.model.PatientIdentifierType;
import com.muzima.utils.Constants;
import com.muzima.utils.DateUtils;
import com.muzima.utils.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.muzima.utils.DateUtils.parse;
import static java.util.Arrays.asList;

public class HTMLPatientJSONMapper {


    public String map(Patient patient, FormData formData, User loggedInUser, boolean isLoggedInUserIsDefaultProvider) {
        JSONObject prepopulateJSON = new JSONObject();
        JSONObject patientDetails = new JSONObject();
        JSONObject encounterDetails = new JSONObject();

        try {
            patientDetails.put("patient.medical_record_number", StringUtils.defaultString(patient.getIdentifier()));
            patientDetails.put("patient.given_name", StringUtils.defaultString(patient.getGivenName()));
            patientDetails.put("patient.middle_name", StringUtils.defaultString(patient.getMiddleName()));
            patientDetails.put("patient.family_name", StringUtils.defaultString(patient.getFamilyName()));
            patientDetails.put("patient.sex", StringUtils.defaultString(patient.getGender()));
            patientDetails.put("patient.uuid", StringUtils.defaultString(patient.getUuid()));
            if (patient.getBirthdate() != null) {
                patientDetails.put("patient.birthdate", DateUtils.getFormattedDate(patient.getBirthdate()));
            }
            encounterDetails.put("encounter.form_uuid", StringUtils.defaultString(formData.getTemplateUuid()));

            if(isLoggedInUserIsDefaultProvider) {
                encounterDetails.put("encounter.provider_id_select", loggedInUser.getSystemId());
                encounterDetails.put("encounter.provider_id", loggedInUser.getSystemId());
            }
            prepopulateJSON.put("patient",patientDetails);
            prepopulateJSON.put("encounter",encounterDetails);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return prepopulateJSON.toString();
    }
    public Patient getPatient(String jsonPayload) throws JSONException {
        JSONObject responseJSON = new JSONObject(jsonPayload);
        return createPatient(responseJSON.getJSONObject("patient"));

    }
    private Patient createPatient(JSONObject patientJSON)  throws JSONException {
        Patient patient = new Patient();
        patient.setUuid(patientJSON.getString("patient.uuid"));
        patient.setGender(patientJSON.getString("patient.sex"));

        List<PersonName> names = new ArrayList<PersonName>();
        names.add(createPersonName(patientJSON));
        patient.setNames(names);

        patient.setBirthdate(getBirthDate(patientJSON));
        patient.setIdentifiers(asList(createPatientIdentifier(patient.getUuid()), createPreferredIdentifier(patientJSON)));

        return patient;
    }
    private PatientIdentifier createPreferredIdentifier(JSONObject patientJSON) throws JSONException  {
        PatientIdentifier patientIdentifier = createPatientIdentifier(patientJSON.getString("patient.medical_record_number"));
        patientIdentifier.setPreferred(true);
        return patientIdentifier;
    }
    private PatientIdentifier createPatientIdentifier(String uuid) {
        PatientIdentifier patientIdentifier = new PatientIdentifier();
        PatientIdentifierType identifierType = new PatientIdentifierType();
        identifierType.setName(Constants.LOCAL_PATIENT);
        patientIdentifier.setIdentifierType(identifierType);
        patientIdentifier.setIdentifier(uuid);
        return patientIdentifier;
    }
    private Date getBirthDate(JSONObject patientJSON) throws JSONException  {
        String birth_date = patientJSON.getString("patient.birth_date");
        try {
            return birth_date == null ? null : parse(birth_date);
        } catch (ParseException e) {
            return null;
        }
    }
    private PersonName createPersonName(JSONObject patientJSON) throws JSONException {
        PersonName personName = new PersonName();
        personName.setFamilyName(patientJSON.getString("patient.family_name"));
        personName.setGivenName(patientJSON.getString("patient.given_name"));

        try{
            String middleName = patientJSON.getString("patient.middle_name");
            personName.setMiddleName(middleName);
        }catch(NullPointerException e){

        }
        return personName;
    }
}
