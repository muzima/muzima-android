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

import android.util.Log;
import com.muzima.MuzimaApplication;
import com.muzima.api.model.FormData;
import com.muzima.api.model.Location;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PatientIdentifier;
import com.muzima.api.model.PatientIdentifierType;
import com.muzima.api.model.PersonAddress;
import com.muzima.api.model.PersonAttribute;
import com.muzima.api.model.PersonName;
import com.muzima.api.model.User;
import com.muzima.controller.LocationController;
import com.muzima.controller.PatientController;
import com.muzima.utils.Constants;
import com.muzima.utils.DateUtils;
import com.muzima.utils.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.muzima.utils.DateUtils.parse;

public class HTMLPatientJSONMapper {

    private JSONObject patientJSON;
    private JSONObject observationJSON;
    private JSONObject encounterJSON;
    private Patient patient;
    private MuzimaApplication muzimaApplication;

    public String map(MuzimaApplication muzimaApplication,Patient patient, FormData formData, boolean isLoggedInUserIsDefaultProvider, String selectedPatientUUids) {
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
            encounterDetails.put("encounter.user_system_id", StringUtils.defaultString(formData.getUserSystemId()));

            if (isLoggedInUserIsDefaultProvider) {
                User loggedInUser = muzimaApplication.getAuthenticatedUser();
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

            if(!patient.getAtributes().isEmpty()){
                List<PersonAttribute> attributes = patient.getAtributes();

                JSONArray attributesJSONArray = new JSONArray();

                for(PersonAttribute attribute : attributes){
                    JSONObject attributeJSONObject = new JSONObject();
                    attributeJSONObject.put("attribute_type_uuid",attribute.getAttributeType().getUuid());
                    attributeJSONObject.put("attribute_type_name",attribute.getAttributeType().getName());
                    attributeJSONObject.put("attribute_value",attribute.getAttribute());
                    attributesJSONArray.put(attributeJSONObject);
                }
                patientDetails.put("patient.personattribute",attributesJSONArray);
            }

            if(!patient.getAddresses().isEmpty()){
                List<PersonAddress> addresses = patient.getAddresses();
                JSONArray addressesJSONArray = new JSONArray();
                for(PersonAddress address : addresses){
                    JSONObject addressJSONObject = new JSONObject();
                    addressJSONObject.put("address1",address.getAddress1());
                    addressJSONObject.put("address2",address.getAddress2());
                    addressJSONObject.put("address3",address.getAddress3());
                    addressJSONObject.put("address4",address.getAddress4());
                    addressJSONObject.put("address5",address.getAddress5());
                    addressJSONObject.put("address6",address.getAddress6());
                    addressJSONObject.put("cityVillage",address.getCityVillage());
                    addressJSONObject.put("stateProvince",address.getStateProvince());
                    addressJSONObject.put("country",address.getCountry());
                    addressJSONObject.put("postalCode",address.getPostalCode());
                    addressJSONObject.put("countyDistrict",address.getCountyDistrict());
                    addressJSONObject.put("latitude",address.getLatitude());
                    addressJSONObject.put("longitude",address.getLongitude());
                    addressJSONObject.put("startDate",address.getStartDate());
                    addressJSONObject.put("endDate",address.getEndDate());
                    addressJSONObject.put("preferred",address.getPreferred());
                    addressJSONObject.put("uuid",address.getUuid());
                    addressesJSONArray.put(addressJSONObject);
                }
                patientDetails.put("patient.personaddress",addressesJSONArray);
            }

            if(!selectedPatientUUids.isEmpty() && !selectedPatientUUids.equals("[]")) {
                List<Patient> patients = new ArrayList<>();
                JSONArray selectedPatientJSONArray = new JSONArray();
                selectedPatientUUids = selectedPatientUUids.replace("[", "");
                selectedPatientUUids = selectedPatientUUids.replace("]", "");
                selectedPatientUUids = selectedPatientUUids.replaceAll("\"", "");
                List<String> patientUuidList = Arrays.asList(selectedPatientUUids.split(","));
                for (String patientUuid : patientUuidList) {
                    PatientController patientController = muzimaApplication.getPatientController();
                    Patient patient1 = patientController.getPatientByUuid(patientUuid);

                    JSONObject selectedPatientJSONObject = new JSONObject();
                    selectedPatientJSONObject.put("names", patient1.getDisplayName()+" ("+patient1.getIdentifier()+")");
                    selectedPatientJSONObject.put("uuid", patient1.getUuid());
                    selectedPatientJSONObject.put("gender", patient1.getGender());
                    selectedPatientJSONObject.put("dob", patient1.getBirthdate());
                    selectedPatientJSONObject.put("nid", patient1.getIdentifier());
                    selectedPatientJSONArray.put(selectedPatientJSONObject);
                }
                patientDetails.put("patient.selectedPatients", selectedPatientJSONArray);
            }

            prepopulateJSON.put("patient", patientDetails);
            prepopulateJSON.put("encounter", encounterDetails);
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "Could not populate patient registration data to JSON", e);
        } catch (PatientController.PatientLoadException e) {
            e.printStackTrace();
        }
        return prepopulateJSON.toString();
    }

    public Patient getPatient(MuzimaApplication muzimaApplication, String jsonPayload) throws JSONException {
        this.muzimaApplication = muzimaApplication;
        setJSONObjects(jsonPayload);
        createPatient();
        return patient;
    }

    private void setJSONObjects(String jsonPayload) throws JSONException {
        JSONObject responseJSON = new JSONObject(jsonPayload);
        patientJSON = responseJSON.getJSONObject("patient");
        encounterJSON = responseJSON.getJSONObject("encounter");
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
        List<PersonName> names = new ArrayList<>();
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
        Location location = getEncounterLocation();
        List<PatientIdentifier> patientIdentifiers = new ArrayList<>();

        patientIdentifiers.add(getPreferredPatientIdentifier(location));
        patientIdentifiers.add(getPatientUuidAsIdentifier(location));

        List<PatientIdentifier> otherIdentifiers = getOtherPatientIdentifiers(location);
        if (!otherIdentifiers.isEmpty())
            patientIdentifiers.addAll(otherIdentifiers);
        return patientIdentifiers;
    }

    private Location getEncounterLocation() throws JSONException {
        String locationId = encounterJSON.getString("encounter.location_id");
        LocationController locationController = muzimaApplication.getLocationController();
        try {
            Location location = locationController.getLocationById(Integer.parseInt(locationId));
            if(location == null){
                throw new JSONException("Could not find location in local repo");
            }
            return location;
        } catch (LocationController.LocationLoadException e) {
            throw new JSONException("Could not find location in local repo");
        }
    }

    private PatientIdentifier getPreferredPatientIdentifier(Location location) throws JSONException {
        String identifierValue = patientJSON.getString("patient.medical_record_number");
        String identifierTypeName = Constants.LOCAL_PATIENT;

        PatientIdentifier preferredPatientIdentifier = createPatientIdentifier(identifierTypeName, identifierValue);
        preferredPatientIdentifier.setPreferred(true);
        preferredPatientIdentifier.setLocation(location);

        return preferredPatientIdentifier;
    }

    private List<PatientIdentifier> getOtherPatientIdentifiers(Location location) throws JSONException {
        List<PatientIdentifier> otherIdentifiers = new ArrayList<>();
        if (observationJSON != null && observationJSON.has("other_identifier_type") && observationJSON.has("other_identifier_value")) {
            Object identifierTypeNameObject = observationJSON.get("other_identifier_type");
            Object identifierValueObject = observationJSON.get("other_identifier_value");

            if (identifierTypeNameObject instanceof JSONArray) {
                JSONArray identifierTypeName = (JSONArray) identifierTypeNameObject;
                JSONArray identifierValue = (JSONArray) identifierValueObject;
                for (int i = 0; i < identifierTypeName.length(); i++) {
                    PatientIdentifier identifier = createPatientIdentifier(identifierTypeName.getString(i), identifierValue.getString(i));
                    identifier.setLocation(location);
                    otherIdentifiers.add(identifier);
                }
            } else if (identifierTypeNameObject instanceof String) {
                String identifierTypeName = (String) identifierTypeNameObject;
                String identifierValue = (String) identifierValueObject;
                PatientIdentifier identifier = createPatientIdentifier(identifierTypeName, identifierValue);
                identifier.setLocation(location);
                otherIdentifiers.add(identifier);
            }
        }
        return otherIdentifiers;
    }

    private PatientIdentifier getPatientUuidAsIdentifier(Location location) {
        PatientIdentifier identifier =  createPatientIdentifier(Constants.LOCAL_PATIENT, patient.getUuid());
        identifier.setLocation(location);
        return identifier;
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
            Log.e(getClass().getSimpleName(), "Could not parse birth_date", e);
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
