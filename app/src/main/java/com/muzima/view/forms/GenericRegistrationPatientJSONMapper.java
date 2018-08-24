/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.util.Log;
import com.muzima.api.exception.InvalidPatientIdentifierException;
import com.muzima.api.exception.InvalidPersonAddressException;
import com.muzima.api.exception.InvalidPersonAttributeException;
import com.muzima.api.model.FormData;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PatientIdentifier;
import com.muzima.api.model.PatientIdentifierType;
import com.muzima.api.model.PersonAddress;
import com.muzima.api.model.PersonAttribute;
import com.muzima.api.model.PersonAttributeType;
import com.muzima.api.model.PersonName;
import com.muzima.api.model.User;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.controller.PatientController;
import com.muzima.utils.Constants;
import com.muzima.utils.DateUtils;
import com.muzima.utils.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static com.muzima.utils.DateUtils.parse;

public class GenericRegistrationPatientJSONMapper {

    private JSONObject patientJSON;
    private Patient patient;
    private MuzimaSettingController settingController;
    private PatientController patientController;

    public String map(Patient patient, FormData formData, User loggedInUser, boolean isLoggedInUserIsDefaultProvider) {
        JSONObject prepopulateJSON = new JSONObject();
        JSONObject patientDetails = new JSONObject();
        JSONObject encounterDetails = new JSONObject();

        try {
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
                encounterDetails.put("encounter.provider_id_select", loggedInUser.getSystemId());
                encounterDetails.put("encounter.provider_id", loggedInUser.getSystemId());
            }

            if (!patient.getIdentifiers().isEmpty()) {
                List<PatientIdentifier> patientIdentifiers = patient.getIdentifiers();

                JSONArray identifierJSONArray = new JSONArray();

                for (PatientIdentifier identifier : patientIdentifiers) {
                    if (identifier.getIdentifier() != null){
                        if(identifier.getIdentifier().equals(patient.getIdentifier())){
                            JSONObject preferredIdentifierJSONObject = new JSONObject();
                            preferredIdentifierJSONObject.put("identifier_value", identifier.getIdentifier());
                            preferredIdentifierJSONObject.put("identifier_type_uuid", identifier.getIdentifierType().getUuid());
                            preferredIdentifierJSONObject.put("identifier_type_name", identifier.getIdentifierType().getName());
                            patientDetails.put("patient.medical_record_number", StringUtils.defaultString(patient.getIdentifier()));
                        } else if (!identifier.getIdentifier().equals(patient.getUuid())) {
                            JSONObject identifierJSONObject = new JSONObject();
                            identifierJSONObject.put("identifier_value", identifier.getIdentifier());
                            identifierJSONObject.put("identifier_type_uuid", identifier.getIdentifierType().getUuid());
                            identifierJSONObject.put("identifier_type_name", identifier.getIdentifierType().getName());
                            identifierJSONArray.put(identifierJSONObject);
                        }
                    }
                }
                patientDetails.put("patient.otheridentifier", identifierJSONArray);
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
                prepopulateJSON.put("patient.personattribute",attributesJSONArray);
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
                prepopulateJSON.put("patient.personaddress",addressesJSONArray);
            }

            prepopulateJSON.put("patient", patientDetails);
            prepopulateJSON.put("encounter", encounterDetails);
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "Could not populate patient registration data to JSON", e);
        }
        return prepopulateJSON.toString();
    }

    public Patient getPatient(String jsonPayload, PatientController patientController, MuzimaSettingController settingController) throws JSONException {
        setPatientController(patientController);
        setSettingController(settingController);
        setJSONObjects(jsonPayload);
        createPatient();
        return patient;
    }

    private void setPatientController(PatientController patientController){
        this.patientController = patientController;
    }

    private void setSettingController(MuzimaSettingController settingController){
        this.settingController = settingController;
    }

    private void setJSONObjects(String jsonPayload) throws JSONException {
        JSONObject responseJSON = new JSONObject(jsonPayload);
        patientJSON = responseJSON.getJSONObject("patient");
    }

    private void createPatient() throws JSONException {
        initializePatient();
        setPatientIdentifiers();
        setPatientNames();
        setPatientGender();
        setPatientBirthDate();
        setPersonAddresses();
        setPersonAttributes();
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
        List<PatientIdentifier> patientIdentifiers = new ArrayList<>();

        patientIdentifiers.add(getPatientUuidAsIdentifier());
        boolean requireMedicalRecordNumber = settingController.isMedicalRecordNumberRequiredDuringRegistration();
        if(requireMedicalRecordNumber || patientJSON.has("patient.medical_record_number")) {
            PatientIdentifier medicalRecordIdentifier = getMedicalRecordNumberIdentifier();

            if(medicalRecordIdentifier != null) {
                if(requireMedicalRecordNumber){
                    medicalRecordIdentifier.setPreferred(true);
                }
                patientIdentifiers.add(medicalRecordIdentifier);
            }
        }

        List<PatientIdentifier> otherIdentifiers = getOtherPatientIdentifiers();
        if (!otherIdentifiers.isEmpty())
            patientIdentifiers.addAll(otherIdentifiers);
        return patientIdentifiers;
    }

    private PatientIdentifier getMedicalRecordNumberIdentifier() throws JSONException {
        PatientIdentifier preferredPatientIdentifier = null;
        if(patientJSON.has("patient.medical_record_number")) {
            JSONObject identifierObject = patientJSON.getJSONObject("patient.medical_record_number");
            try {
                preferredPatientIdentifier = createPatientIdentifier(identifierObject);
            } catch (InvalidPatientIdentifierException e){
                throw new RuntimeException("Invalid patient medical record number", e);
            }
        } else {
            throw new RuntimeException("Cannot find patient medical record number");
        }

        return preferredPatientIdentifier;
    }

    private List<PatientIdentifier> getOtherPatientIdentifiers() throws JSONException {
        List<PatientIdentifier> otherIdentifiers = new ArrayList<>();
        if (patientJSON != null && patientJSON.has("patient.otheridentifier")) {
            Object otherIdentifierObject = patientJSON.get("patient.otheridentifier");

            if (otherIdentifierObject instanceof JSONArray) {
                JSONArray identifiers = (JSONArray) otherIdentifierObject;
                for (int i = 0; i < identifiers.length(); i++) {
                    try {
                        PatientIdentifier identifier = createPatientIdentifier(identifiers.getJSONObject(i));
                        if (identifier != null) {
                            otherIdentifiers.add(identifier);
                        }
                    }catch (InvalidPatientIdentifierException e){
                        Log.e(getClass().getSimpleName(), "Error while creating identifier.", e);
                    }
                }
            } else if (otherIdentifierObject instanceof JSONObject) {
                try {
                    PatientIdentifier identifier = createPatientIdentifier((JSONObject) otherIdentifierObject);
                    if (identifier != null) {
                        otherIdentifiers.add(identifier);
                    }
                } catch (InvalidPatientIdentifierException e){
                    Log.e(getClass().getSimpleName(), "Error while creating identifier.", e);
                }
            }
        }

        Iterator<String> keys = patientJSON.keys();
        while(keys.hasNext()){
            String key = keys.next();
            if(key.startsWith("patient.otheridentifier^")){
                try {
                    otherIdentifiers.add(createPatientIdentifier(patientJSON.getJSONObject(key)));
                } catch (InvalidPatientIdentifierException e){
                    Log.e(getClass().getSimpleName(), "Error while creating identifier.", e);
                }
            }
        }

        return otherIdentifiers;
    }

    private PatientIdentifier getPatientUuidAsIdentifier() {
        try {
            return createPatientIdentifier(Constants.LOCAL_PATIENT, null, patient.getUuid());
        } catch (InvalidPatientIdentifierException e){
            throw new RuntimeException("Invalid patient medical record number", e);
        }
    }

    private PatientIdentifier createPatientIdentifier(JSONObject identifierJSONObject) throws JSONException,InvalidPatientIdentifierException{
        if((identifierJSONObject.has("identifier_type_uuid") || identifierJSONObject.has("identifier_type_name"))
                && identifierJSONObject.has("identifier_value")) {
            String identifierTypeName = (String) getFromJsonObject(identifierJSONObject, "identifier_type_name");
            String identifierTypeUuid = (String) getFromJsonObject(identifierJSONObject, "identifier_type_uuid");
            String identifierValue = (String) getFromJsonObject(identifierJSONObject, "identifier_value");
            return createPatientIdentifier(identifierTypeName, identifierTypeUuid, identifierValue);
        } else {
            throw new InvalidPatientIdentifierException("Cannot create patient identifier due to missing identifier_type or identifier_value");
        }
    }

    private PatientIdentifier createPatientIdentifier(String identifierTypeName, String identifierTypeUuid, String identifierValue)
            throws InvalidPatientIdentifierException{
        if(StringUtils.isEmpty(identifierValue) || StringUtils.isEmpty(identifierTypeName)
                && StringUtils.isEmpty(identifierTypeUuid)){
            throw new InvalidPatientIdentifierException("Cannot create Identifier. Missing identifier value and identifier type name or" +
                    " identifier type uuid");
        }

        PatientIdentifier patientIdentifier = new PatientIdentifier();
        PatientIdentifierType identifierType = null;

        if(!StringUtils.isEmpty(identifierTypeUuid)){
            identifierType = patientController.getPatientIdentifierTypeByUuid(identifierTypeUuid);
        } else if(!StringUtils.isEmpty(identifierTypeName)){
            List<PatientIdentifierType> tmpIdentifierTypes = patientController.getPatientIdentifierTypeByName(identifierTypeName);
            if(tmpIdentifierTypes.size() == 1){
                identifierType = tmpIdentifierTypes.get(0);
            }
        }

        if(identifierType == null){
            identifierType = new PatientIdentifierType();
            if(!StringUtils.isEmpty(identifierTypeUuid)){
                identifierType.setUuid(identifierTypeUuid);
            }
            if(!StringUtils.isEmpty(identifierTypeName)){
                identifierType.setName(identifierTypeName);
            }
        }
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

    private void setPersonAddresses() throws JSONException {
        List<PersonAddress> addresses = new ArrayList<>();
        if(patientJSON.has("patient.personaddress")){
            Object personAddress = patientJSON.get("patient.personaddress");
            if(personAddress instanceof JSONObject){
                try {
                    addresses.add(createPersonAddress((JSONObject) personAddress));
                } catch (InvalidPersonAddressException e){
                    Log.e(getClass().getSimpleName(),"Error while creating person address.",e);
                }
            } else if(personAddress instanceof JSONArray){
                JSONArray address = (JSONArray)personAddress;
                for(int i=0; i<address.length(); i++){
                    try {
                        addresses.add(createPersonAddress(address.getJSONObject(i)));
                    } catch (InvalidPersonAddressException e){
                        Log.e(getClass().getSimpleName(),"Error while creating person address.",e);
                    }
                }
            }
        }

        Iterator<String> keys = patientJSON.keys();
        while(keys.hasNext()){
            String key = keys.next();
            if(key.startsWith("patient.personaddress^")){
                try {
                    addresses.add(createPersonAddress(patientJSON.getJSONObject(key)));
                } catch (InvalidPersonAddressException e){
                    Log.e(getClass().getSimpleName(),"Error while creating person address.",e);
                }
            }
        }

        if(!addresses.isEmpty()){
            patient.setAddresses(addresses);
        }
    }

    private PersonAddress createPersonAddress(JSONObject addressObject) throws JSONException,InvalidPersonAddressException {
        PersonAddress personAddress = new PersonAddress();
        personAddress.setAddress1((String)getFromJsonObject(addressObject,"address1"));
        personAddress.setAddress2((String)getFromJsonObject(addressObject,"address2"));
        personAddress.setAddress3((String)getFromJsonObject(addressObject,"address3"));
        personAddress.setAddress4((String)getFromJsonObject(addressObject,"address4"));
        personAddress.setAddress5((String)getFromJsonObject(addressObject,"address5"));
        personAddress.setAddress6((String)getFromJsonObject(addressObject,"address6"));
        personAddress.setCityVillage((String)getFromJsonObject(addressObject,"cityVillage"));
        personAddress.setCountyDistrict((String)getFromJsonObject(addressObject,"countyDistrict"));
        personAddress.setCountry((String)getFromJsonObject(addressObject,"country"));
        personAddress.setStateProvince((String)getFromJsonObject(addressObject,"stateProvince"));
        personAddress.setPostalCode((String)getFromJsonObject(addressObject,"postalCode"));
        personAddress.setLatitude((String)getFromJsonObject(addressObject,"latitude"));
        personAddress.setLongitude((String)getFromJsonObject(addressObject,"longitude"));
        personAddress.setPreferred((Boolean)getFromJsonObject(addressObject, "preferred"));
        if(addressObject.has("startDate")) {
            try {
                Date startDate = parse(addressObject.getString("startDate"));
                personAddress.setStartDate(startDate);
            } catch (ParseException e) {
                Log.e(getClass().getSimpleName(), "Could not parse personaddress.startDate", e);
            }
        }
        if(addressObject.has("endDate")) {
            try {
                Date endDate = parse(addressObject.getString("endDate"));
                personAddress.setEndDate(endDate);
            } catch (ParseException e) {
                Log.e(getClass().getSimpleName(), "Could not parse personaddress.endDate", e);
            }
        }
        if(personAddress.isBlank()) {
            throw new InvalidPersonAddressException("No person address information available.");
        }
        return personAddress;
    }

    private void setPersonAttributes() throws JSONException{
        List<PersonAttribute> attributes = new ArrayList<>();
        if(patientJSON.has("patient.personattribute")){

            Object personAttribute = patientJSON.get("patient.personattribute");
            if(personAttribute instanceof JSONObject){
                try{
                    attributes.add(createPersonAttribute((JSONObject)personAttribute));
                } catch (InvalidPersonAttributeException e){
                    Log.e(getClass().getSimpleName(),"Error while creating attribute.",e);
                }
            } else if(personAttribute instanceof JSONArray){
                JSONArray att = (JSONArray)personAttribute;
                for(int i=0; i<att.length(); i++){
                    try{
                        attributes.add(createPersonAttribute(att.getJSONObject(i)));
                    } catch (InvalidPersonAttributeException e){
                        Log.e(getClass().getSimpleName(),"Error while creating attribute.",e);
                    }
                }
            }
        }
        Iterator<String> keys = patientJSON.keys();
        while(keys.hasNext()){
            String key = keys.next();
            if(key.startsWith("patient.personattribute^")){
                try {
                    attributes.add(createPersonAttribute(patientJSON.getJSONObject(key)));
                } catch (InvalidPersonAttributeException e){
                    Log.e(getClass().getSimpleName(),"Error while creating attribute.",e);
                }
            }
        }
        if(!attributes.isEmpty()) {
            patient.setAttributes(attributes);
        }
    }

    private PersonAttribute createPersonAttribute(JSONObject jsonObject) throws JSONException,InvalidPersonAttributeException{
        if(!jsonObject.has("attribute_value") || !jsonObject.has("attribute_type_name")
                && !jsonObject.has("attribute_type_uuid")) {
            throw new InvalidPersonAttributeException("Could not create person attribute due to missing value or attribute type information");
        } else {
            String attributeValue = jsonObject.getString("attribute_value");
            PersonAttribute attribute = new PersonAttribute();
            attribute.setAttribute(attributeValue);

            PersonAttributeType attributeType = null;
            if (jsonObject.has("attribute_type_uuid")) {
                String personAttributeTypeUuid = jsonObject.getString("attribute_type_uuid");
                attributeType = patientController.getPersonAttributeTypeByUuid(personAttributeTypeUuid);
            } else if (jsonObject.has("attribute_type_name")) {
                String personAttributeTypeName = jsonObject.getString("attribute_type_name");
                List<PersonAttributeType> attributeTypes = patientController.getPersonAttributeTypeByName(personAttributeTypeName);
                if (attributeTypes != null && attributeTypes.size() == 1) {
                    attributeType = attributeTypes.get(0);
                }
            }

            if(attributeType == null){
                attributeType = new PersonAttributeType();
                attributeType.setUuid(jsonObject.getString("attribute_type_uuid"));
                attributeType.setName(jsonObject.getString("attribute_type_name"));
            }
            attribute.setAttributeType(attributeType);
            return attribute;
        }
    }

    private Object getFromJsonObject(JSONObject jsonObject, String key) throws JSONException{
        if (jsonObject.has(key)) {
            return jsonObject.getString(key);
        }
        return null;
    }
}
