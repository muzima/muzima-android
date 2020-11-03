package com.muzima.utils;

import android.util.Log;
import com.muzima.MuzimaApplication;
import com.muzima.api.exception.InvalidPersonAddressException;
import com.muzima.api.exception.InvalidPersonAttributeException;
import com.muzima.api.model.PersonAddress;
import com.muzima.api.model.PersonAttribute;
import com.muzima.api.model.PersonAttributeType;
import com.muzima.api.model.PersonName;
import com.muzima.controller.PatientController;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static com.muzima.utils.DateUtils.parse;

public class PersonRegistrationUtils {
    public static PersonName createPersonName(JSONObject personJsonObject) throws JSONException {
        if(personJsonObject == null){
            throw new JSONException("Invalid Person Json object. Null value supplied");
        }

        PersonName personName = new PersonName();
        personName.setFamilyName(personJsonObject.getString("patient.family_name"));
        personName.setGivenName(personJsonObject.getString("patient.given_name"));

        String middleNameJSONString = "patient.middle_name";
        String middleName = "";
        if (personJsonObject.has(middleNameJSONString))
            middleName = personJsonObject.getString(middleNameJSONString);
        personName.setMiddleName(middleName);

        return personName;
    }

    public static Date createBirthDate(JSONObject personJsonObject) throws JSONException {
        if(personJsonObject == null){
            throw new JSONException("Invalid Person Json object. Null value supplied");
        } else if (!personJsonObject.has("patient.birth_date")){
            throw new JSONException("Invalid Person Json object. Value with the key patient.birthdate_estimated not found");
        }

        String birthDateAsString = personJsonObject.getString("patient.birth_date");
        Date birthDate = null;
        try {
            if (birthDateAsString != null)
                birthDate = parse(birthDateAsString);
        } catch (ParseException e) {
            Log.e(PersonRegistrationUtils.class.getSimpleName(), "Could not parse birth_date", e);
        }
        return birthDate;
    }

    public static boolean createBirthDateEstimated(JSONObject personJsonObject) throws JSONException {
        if(personJsonObject == null){
            throw new JSONException("Invalid Person Json object. Null value supplied");
        }
        if (!personJsonObject.has("patient.birthdate_estimated")){
            throw new JSONException("Invalid Person Json object. Value with the key patient.birthdate_estimated not found");
        }
        return personJsonObject.getBoolean("patient.birthdate_estimated");
    }

    public static List<PersonAddress> createPersonAddresses(JSONObject personJsonObject) throws JSONException {
        List<PersonAddress> addresses = new ArrayList<>();

        if(personJsonObject == null){
            throw new JSONException("Invalid Person Json object. Null value supplied");
        }

        if(personJsonObject.has("patient.personaddress")){
            Object personAddress = personJsonObject.get("patient.personaddress");
            if(personAddress instanceof JSONObject){
                try {
                    addresses.add(createPersonAddress((JSONObject) personAddress));
                } catch (InvalidPersonAddressException e){
                    Log.e(PersonRegistrationUtils.class.getSimpleName(),"Error while creating person address.",e);
                }
            } else if(personAddress instanceof JSONArray){
                JSONArray address = (JSONArray)personAddress;
                for(int i=0; i<address.length(); i++){
                    try {
                        addresses.add(createPersonAddress(address.getJSONObject(i)));
                    } catch (InvalidPersonAddressException e){
                        Log.e(PersonRegistrationUtils.class.getSimpleName(),"Error while creating person address.",e);
                    }
                }
            }
        }

        Iterator<String> keys = personJsonObject.keys();
        while(keys.hasNext()){
            String key = keys.next();
            if(key.startsWith("patient.personaddress^")){
                try {
                    addresses.add(createPersonAddress(personJsonObject.getJSONObject(key)));
                } catch (InvalidPersonAddressException e){
                    Log.e(PersonRegistrationUtils.class.getSimpleName(),"Error while creating person address.",e);
                }
            }
        }
        return addresses;
    }

    public static PersonAddress createPersonAddress(JSONObject addressObject) throws JSONException, InvalidPersonAddressException {
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
                Log.e(PersonRegistrationUtils.class.getSimpleName(), "Could not parse personaddress.startDate", e);
            }
        }
        if(addressObject.has("endDate")) {
            try {
                Date endDate = parse(addressObject.getString("endDate"));
                personAddress.setEndDate(endDate);
            } catch (ParseException e) {
                Log.e(PersonRegistrationUtils.class.getSimpleName(), "Could not parse personaddress.endDate", e);
            }
        }

        personAddress.setUuid((String)getFromJsonObject(addressObject,"uuid"));
        if(personAddress.getUuid() == null){
            personAddress.setUuid(UUID.randomUUID().toString());
        }

        if(personAddress.isBlank()) {
            throw new InvalidPersonAddressException("No person address information available.");
        }
        return personAddress;
    }

    public static List<PersonAttribute> createPersonAttributes(JSONObject personJsonObject, MuzimaApplication muzimaApplication) throws JSONException{
        List<PersonAttribute> attributes = new ArrayList<>();

        if(personJsonObject == null){
            throw new JSONException("Invalid Person Json object. Null value supplied");
        }

        if(personJsonObject.has("patient.personattribute")){

            Object personAttribute = personJsonObject.get("patient.personattribute");
            if(personAttribute instanceof JSONObject){
                try{
                    attributes.add(createPersonAttribute((JSONObject)personAttribute, muzimaApplication));
                } catch (InvalidPersonAttributeException e){
                    Log.e(PersonRegistrationUtils.class.getSimpleName(),"Error while creating attribute.",e);
                }
            } else if(personAttribute instanceof JSONArray){
                JSONArray att = (JSONArray)personAttribute;
                for(int i=0; i<att.length(); i++){
                    try{
                        attributes.add(createPersonAttribute(att.getJSONObject(i), muzimaApplication));
                    } catch (InvalidPersonAttributeException e){
                        Log.e(PersonRegistrationUtils.class.getSimpleName(),"Error while creating attribute.",e);
                    }
                }
            }
        }

        Iterator<String> keys = personJsonObject.keys();
        while(keys.hasNext()){
            String key = keys.next();
            if(key.startsWith("patient.personattribute^")){
                try {
                    attributes.add(createPersonAttribute(personJsonObject.getJSONObject(key), muzimaApplication));
                } catch (InvalidPersonAttributeException e){
                    Log.e(PersonRegistrationUtils.class.getSimpleName(),"Error while creating attribute.",e);
                }
            }
        }
        return attributes;
    }

    public static PersonAttribute createPersonAttribute(JSONObject personJsonObject, MuzimaApplication muzimaApplication)
            throws JSONException, InvalidPersonAttributeException {
        if(personJsonObject == null){
            throw new JSONException("Invalid person Json object. Null value supplied");
        }

        if(!personJsonObject.has("attribute_value") || !personJsonObject.has("attribute_type_name")
                && !personJsonObject.has("attribute_type_uuid")) {
            throw new InvalidPersonAttributeException("Could not create person attribute due to missing value or attribute type information");
        } else {
            String attributeValue = personJsonObject.getString("attribute_value");
            PersonAttribute attribute = new PersonAttribute();
            attribute.setAttribute(attributeValue);

            PersonAttributeType attributeType = null;
            PatientController patientController = muzimaApplication.getPatientController();
            if (personJsonObject.has("attribute_type_uuid")) {
                String personAttributeTypeUuid = personJsonObject.getString("attribute_type_uuid");
                attributeType = patientController.getPersonAttributeTypeByUuid(personAttributeTypeUuid);
            } else if (personJsonObject.has("attribute_type_name")) {
                String personAttributeTypeName = personJsonObject.getString("attribute_type_name");
                List<PersonAttributeType> attributeTypes = patientController.getPersonAttributeTypeByName(personAttributeTypeName);
                if (attributeTypes != null && attributeTypes.size() == 1) {
                    attributeType = attributeTypes.get(0);
                }
            }

            if(attributeType == null){
                attributeType = new PersonAttributeType();
                if(personJsonObject.has("attribute_type_uuid")) {
                    attributeType.setUuid(personJsonObject.getString("attribute_type_uuid"));
                }else if(personJsonObject.has("attribute_type_name")) {
                    attributeType.setName(personJsonObject.getString("attribute_type_name"));
                }
            }
            attribute.setAttributeType(attributeType);
            return attribute;
        }
    }

    private static Object getFromJsonObject(JSONObject jsonObject, String key) throws JSONException{
        if (jsonObject.has(key)) {
            return jsonObject.getString(key);
        }
        return null;
    }
}
