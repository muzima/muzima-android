/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.util.Log;
import com.muzima.api.model.FormData;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PatientIdentifier;
import com.muzima.api.model.PatientIdentifierType;
import com.muzima.api.model.PersonName;
import com.muzima.api.model.User;
import com.muzima.utils.Constants;
import com.muzima.utils.DateUtils;
import com.muzima.utils.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.muzima.utils.DateUtils.parse;

public class HTMLPatientJSONMapper {
    public static String TAG = HTMLPatientJSONMapper.class.getSimpleName();

    private JSONObject patientJSON;
    private JSONObject observationJSON;
    private Patient patient;

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
                patientDetails.put("patient.birth_date", DateUtils.getFormattedDate(patient.getBirthdate()));
            }
            encounterDetails.put("encounter.form_uuid", StringUtils.defaultString(formData.getTemplateUuid()));

            if (isLoggedInUserIsDefaultProvider) {
                encounterDetails.put("encounter.provider_id_select", loggedInUser.getSystemId());
                encounterDetails.put("encounter.provider_id", loggedInUser.getSystemId());
            }

            if (!patient.getIdentifiers().isEmpty()) {
                List<PatientIdentifier> patientIdentifiers = patient.getIdentifiers();

                JSONArray identifierTypeName = new JSONArray();
                JSONArray identifierValue = new JSONArray();

                for (PatientIdentifier identifier : patientIdentifiers) {
                    if (identifier.getIdentifier() != null && !(identifier.getIdentifier().equals(patient.getIdentifier()) || identifier.getIdentifier().equals(patient.getUuid()))) {
                        identifierTypeName.put(StringUtils.defaultString(identifier.getIdentifierType().getName()));
                        identifierValue.put(StringUtils.defaultString(identifier.getIdentifier()));
                    }
                }
                prepopulateJSON.put("other_identifier_type", identifierTypeName);
                prepopulateJSON.put("other_identifier_value", identifierValue);
            }
            prepopulateJSON.put("patient", patientDetails);
            prepopulateJSON.put("encounter", encounterDetails);
        } catch (JSONException e) {
            Log.e(TAG, "Could not populate patient registration data to JSON", e);
        }
        return prepopulateJSON.toString();
    }

    public Patient getPatient(String jsonPayload) throws JSONException {
        setJSONObjects(jsonPayload);
        createPatient();
        return patient;
    }

    private void setJSONObjects(String jsonPayload) throws JSONException {
        JSONObject responseJSON = new JSONObject(jsonPayload);
        patientJSON = responseJSON.getJSONObject("patient");
        if (responseJSON.has("observation")) {
            observationJSON = responseJSON.getJSONObject("observation");
        }
    }

    private void createPatient() throws JSONException {
        initializePatient();
        setPatientIdentifiers();
        setPatientNames();
        setPatientGender();
        setPatientBirthDate();
    }

    private void initializePatient() throws JSONException {
        patient = new Patient();
        patient.setUuid(patientJSON.getString("patient.uuid"));
    }

    private void setPatientIdentifiers() throws JSONException {
        List<PatientIdentifier> identifiers = getPatientIdentifiers();
        patient.setIdentifiers(identifiers);
    }

    private void setPatientNames() throws JSONException {
        List<PersonName> names = new ArrayList<PersonName>();
        names.add(getPersonName());
        patient.setNames(names);
    }

    private void setPatientGender() throws JSONException {
        String gender = patientJSON.getString("patient.sex");
        patient.setGender(gender);
    }

    private void setPatientBirthDate() throws JSONException {
        Date date = getBirthDate();
        patient.setBirthdate(date);
    }

    private List<PatientIdentifier> getPatientIdentifiers() throws JSONException {
        List<PatientIdentifier> patientIdentifiers = new ArrayList<PatientIdentifier>();

        patientIdentifiers.add(getPreferredPatientIdentifier());
        patientIdentifiers.add(getPatientUuidAsIdentifier());

        List<PatientIdentifier> otherIdentifiers = getOtherPatientIdentifiers();
        if (!otherIdentifiers.isEmpty())
            patientIdentifiers.addAll(otherIdentifiers);
        return patientIdentifiers;
    }

    private PatientIdentifier getPreferredPatientIdentifier() throws JSONException {
        String identifierValue = patientJSON.getString("patient.medical_record_number");
        String identifierTypeName = Constants.LOCAL_PATIENT;

        PatientIdentifier preferredPatientIdentifier = createPatientIdentifier(identifierTypeName, identifierValue);
        preferredPatientIdentifier.setPreferred(true);

        return preferredPatientIdentifier;
    }

    private List<PatientIdentifier> getOtherPatientIdentifiers() throws JSONException {
        List<PatientIdentifier> otherIdentifiers = new ArrayList<PatientIdentifier>();
        if (observationJSON != null && observationJSON.has("other_identifier_type") && observationJSON.has("other_identifier_value")) {
            Object identifierTypeNameObject = observationJSON.get("other_identifier_type");
            Object identifierValueObject = observationJSON.get("other_identifier_value");

            if (identifierTypeNameObject instanceof JSONArray) {
                JSONArray identifierTypeName = (JSONArray) identifierTypeNameObject;
                JSONArray identifierValue = (JSONArray) identifierValueObject;
                for (int i = 0; i < identifierTypeName.length(); i++) {
                    PatientIdentifier identifier = createPatientIdentifier(identifierTypeName.getString(i), identifierValue.getString(i));
                    otherIdentifiers.add(identifier);
                }
            } else if (identifierTypeNameObject instanceof String) {
                String identifierTypeName = (String) identifierTypeNameObject;
                String identifierValue = (String) identifierValueObject;
                PatientIdentifier identifier = createPatientIdentifier(identifierTypeName, identifierValue);
                otherIdentifiers.add(identifier);
            }
        }
        return otherIdentifiers;
    }

    private PatientIdentifier getPatientUuidAsIdentifier() {
        return createPatientIdentifier(Constants.LOCAL_PATIENT, patient.getUuid());
    }

    private PatientIdentifier createPatientIdentifier(String identifierTypeName, String identifierValue) {
        PatientIdentifier patientIdentifier = new PatientIdentifier();
        PatientIdentifierType identifierType = new PatientIdentifierType();
        identifierType.setName(identifierTypeName);
        patientIdentifier.setIdentifierType(identifierType);
        patientIdentifier.setIdentifier(identifierValue);
        return patientIdentifier;
    }

    private Date getBirthDate() throws JSONException {
        String birthDateAsString = patientJSON.getString("patient.birth_date");
        Date birthDate = null;
        try {
            if (birthDateAsString != null)
                birthDate = parse(birthDateAsString);
        } catch (ParseException e) {
            Log.e(TAG, "Could not parse birth_date", e);
        }
        return birthDate;
    }

    private PersonName getPersonName() throws JSONException {
        PersonName personName = new PersonName();
        personName.setFamilyName(patientJSON.getString("patient.family_name"));
        personName.setGivenName(patientJSON.getString("patient.given_name"));

        String middleNameJSONString = "patient.middle_name";
        String middleName = "";
        if (patientJSON.has(middleNameJSONString))
            middleName = patientJSON.getString(middleNameJSONString);
        personName.setMiddleName(middleName);

        return personName;
    }
}
