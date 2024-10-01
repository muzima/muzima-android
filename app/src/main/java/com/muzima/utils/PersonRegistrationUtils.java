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
    public static PersonName createDemographicsUpdatePersonName(JSONObject demographicsUpdateJsonObject) throws JSONException {
        if(demographicsUpdateJsonObject == null){
            throw new JSONException("Invalid DemographicsUpdate Json object. Null value supplied");
        }

        PersonName personName = new PersonName();
        personName.setFamilyName(demographicsUpdateJsonObject.getString("demographicsupdate.family_name"));
        personName.setGivenName(demographicsUpdateJsonObject.getString("demographicsupdate.given_name"));

        String middleNameJSONString = "demographicsupdate.middle_name";
        String middleName = "";
        if (demographicsUpdateJsonObject.has(middleNameJSONString))
            middleName = demographicsUpdateJsonObject.getString(middleNameJSONString);
        personName.setMiddleName(middleName);

        return personName;
    }

    public static PersonName createPersonName(JSONObject personJsonObject) throws JSONException {
        if(personJsonObject == null){
            throw new JSONException("Invalid Person Json object. Null value supplied");
        }

        PersonName personName = new PersonName();

        String familyNameKey = "patient.family_name";
        if(personJsonObject.has(familyNameKey))
            personName.setFamilyName(personJsonObject.getString(familyNameKey));
        else
            personName.setFamilyName(StringUtils.EMPTY);

        String givenNameKey = "patient.given_name";
        if(personJsonObject.has(givenNameKey))
            personName.setGivenName(personJsonObject.getString(givenNameKey));
        else
            personName.setGivenName(StringUtils.EMPTY);

        String middleNameKey = "patient.middle_name";
        if (personJsonObject.has(middleNameKey))
            personName.setMiddleName(personJsonObject.getString(middleNameKey));
        else
            personName.setMiddleName(StringUtils.EMPTY);

        return personName;
    }

    public static Date createDemographicsUpdateBirthDate(JSONObject demographicsUpdateJsonObject) throws JSONException {
        if(demographicsUpdateJsonObject == null){
            throw new JSONException("Invalid DemographicsUpdate Json object. Null value supplied");
        } else if (!demographicsUpdateJsonObject.has("demographicsupdate.birth_date")){
            throw new JSONException("Invalid DemographicsUpdate Json object. Value with the key demographicsupdate.birth_date not found");
        }

        String birthDateAsString = demographicsUpdateJsonObject.getString("demographicsupdate.birth_date");
        Date birthDate = null;
        try {
            if (birthDateAsString != null)
                birthDate = parse(birthDateAsString);
        } catch (ParseException e) {
            Log.e(PersonRegistrationUtils.class.getSimpleName(), "Could not parse birth_date", e);
        }
        return birthDate;
    }

    public static Date createBirthDate(JSONObject personJsonObject) throws JSONException {
        if(personJsonObject == null){
            throw new JSONException("Invalid Person Json object. Null value supplied");
        } else if (!personJsonObject.has("patient.birth_date")){
            throw new JSONException("Invalid Person Json object. Value with the key patient.birth_date not found");
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

    public static boolean createDemographicsUpdateBirthDateEstimated(JSONObject demographicsUpdateJsonObject) throws JSONException {
        if(demographicsUpdateJsonObject == null){
            throw new JSONException("Invalid DemographicsUpdate Json object. Null value supplied");
        }
        if (!demographicsUpdateJsonObject.has("demographicsupdate.birthdate_estimated")){
            throw new JSONException("Invalid DemographicsUpdate Json object. Value with the key demographicsupdate.birthdate_estimated not found");
        }
        return demographicsUpdateJsonObject.getBoolean("demographicsupdate.birthdate_estimated");
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
    public static List<PersonAddress> createDemographicsUpdatePersonAddresses(JSONObject demographicsUpdateJsonObject) throws JSONException {

        if(demographicsUpdateJsonObject == null){
            throw new JSONException("Invalid demographicsUpdate Json object. Null value supplied");
        }

        List<PersonAddress> addresses = new ArrayList<>();
        List<Object> personAddressObjects = new ArrayList<>();
        if(demographicsUpdateJsonObject.has("demographicsupdate.personaddress")) {
            personAddressObjects.add(demographicsUpdateJsonObject.get("demographicsupdate.personaddress"));
        }
        for (Object personAddress:personAddressObjects){
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

        Iterator<String> keys = demographicsUpdateJsonObject.keys();
        while(keys.hasNext()){
            String key = keys.next();
            if(key.startsWith("demographicsupdate.personaddress^")){
                try {
                    addresses.add(createPersonAddress(demographicsUpdateJsonObject.getJSONObject(key)));
                } catch (InvalidPersonAddressException e){
                    Log.e(PersonRegistrationUtils.class.getSimpleName(),"Error while creating person address.",e);
                }
            }
        }
        return addresses;
    }

    public static List<PersonAddress> createPersonAddresses(JSONObject personJsonObject) throws JSONException {
        List<PersonAddress> addresses = new ArrayList<>();

        if(personJsonObject == null){
            throw new JSONException("Invalid Person Json object. Null value supplied");
        }

        List<Object> personAddressObjects = new ArrayList<>();
        if(personJsonObject.has("patient.personaddress")) {
            personAddressObjects.add(personJsonObject.get("patient.personaddress"));
        }

        for (Object personAddress:personAddressObjects){
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
        if(addressObject.has("preferred")) {
            personAddress.setPreferred(Boolean.parseBoolean((String) getFromJsonObject(addressObject, "preferred")));
        }
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

    public static void copyPersonAddress(PersonAddress copyFrom, PersonAddress copyTo) throws Exception {
        if(copyFrom == null || copyTo == null){
            throw new Exception("unable to copy person address due to null object'");
        }
        copyTo.setAddress1(copyFrom.getAddress1());
        copyTo.setAddress2(copyFrom.getAddress2());
        copyTo.setAddress3(copyFrom.getAddress3());
        copyTo.setAddress4(copyFrom.getAddress4());
        copyTo.setAddress5(copyFrom.getAddress5());
        copyTo.setAddress6(copyFrom.getAddress6());
        copyTo.setCityVillage(copyFrom.getCityVillage());
        copyTo.setCountyDistrict(copyFrom.getCountyDistrict());
        copyTo.setStateProvince(copyFrom.getStateProvince());
        copyTo.setCountry(copyFrom.getCountry());
        copyTo.setPostalCode(copyFrom.getPostalCode());
        copyTo.setLatitude(copyFrom.getLatitude());
        copyTo.setLongitude(copyFrom.getLongitude());
        copyTo.setStartDate(copyFrom.getStartDate());
        copyTo.setEndDate(copyFrom.getEndDate());
        copyTo.setPreferred(copyFrom.getPreferred());
    }

    public static List<PersonAttribute> createDemographicsUpdatePersonAttributes(JSONObject demographicsupdateJsonObject, MuzimaApplication muzimaApplication) throws JSONException{
        List<PersonAttribute> attributes = new ArrayList<>();

        if(demographicsupdateJsonObject == null){
            throw new JSONException("Invalid demographicsupdate Json object. Null value supplied");
        }

        List<Object> personAttributeObjects = new ArrayList<>();

        if(demographicsupdateJsonObject.has("demographicsupdate.personattribute")) {
            personAttributeObjects.add(demographicsupdateJsonObject.get("demographicsupdate.personattribute"));
        }

        for (Object personAttribute:personAttributeObjects){

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

        Iterator<String> keys = demographicsupdateJsonObject.keys();
        while(keys.hasNext()){
            String key = keys.next();
            if(key.startsWith("demographicsupdate.personattribute^")){
                try {
                    attributes.add(createPersonAttribute(demographicsupdateJsonObject.getJSONObject(key), muzimaApplication));
                } catch (InvalidPersonAttributeException e){
                    Log.e(PersonRegistrationUtils.class.getSimpleName(),"Error while creating attribute.",e);
                }
            }
        }
        return attributes;
    }

    public static List<PersonAttribute> createPersonAttributes(JSONObject personJsonObject, MuzimaApplication muzimaApplication) throws JSONException{
        List<PersonAttribute> attributes = new ArrayList<>();

        if(personJsonObject == null){
            throw new JSONException("Invalid Person Json object. Null value supplied");
        }

        List<Object> personAttributeObjects = new ArrayList<>();

        if(personJsonObject.has("patient.personattribute")) {
            personAttributeObjects.add(personJsonObject.get("patient.personattribute"));
        }

        for (Object personAttribute:personAttributeObjects){

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
            if(key.startsWith("patient.personattribute^") || key.startsWith("demographicsupdate.personattribute^")){
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
                    attributeType.setPersonAttributeTypeUuid(personJsonObject.getString("attribute_type_uuid"));
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
