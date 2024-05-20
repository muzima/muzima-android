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
import com.muzima.api.model.FormData;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Person;
import com.muzima.api.model.PersonAddress;
import com.muzima.api.model.PersonAttribute;
import com.muzima.api.model.PersonName;
import com.muzima.api.model.Relationship;
import com.muzima.api.model.User;
import com.muzima.controller.PatientController;
import com.muzima.search.api.util.StringUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class RelationshipJsonMapper {

    private User loggedInUser;
    private Patient patient;
    private Relationship relationship;
    private PatientController patientController;
    private MuzimaApplication muzimaApplication;

    public RelationshipJsonMapper(MuzimaApplication muzimaApplication){
        this.muzimaApplication = muzimaApplication;
        this.patientController = muzimaApplication.getPatientController();
        loggedInUser = muzimaApplication.getAuthenticatedUser();
    }

    public FormData createFormDataFromRelationship(Patient patient, Relationship relationship) throws JSONException {
        String templateUuid;
        if(muzimaApplication.getMuzimaSettingController().isPatientTagGenerationEnabled()){
            templateUuid = Constants.FGH.FormTemplateUuids.INDEX_CASE_PERSON_REGISTRATION_FORM;
        } else {
            templateUuid = UUID.randomUUID().toString();
        }
        String personUuid = null;
        if(StringUtil.equals(relationship.getPersonA().getUuid(), patient.getUuid())){
            personUuid = relationship.getPersonB().getUuid();
        } else {
            personUuid = relationship.getPersonA().getUuid();
        }
        this.patient =patient;
        this.relationship = relationship;
        FormData formData = new FormData();
        formData.setDiscriminator(Constants.FORM_JSON_DISCRIMINATOR_RELATIONSHIP);
        formData.setEncounterDate(new Date());
        formData.setPatientUuid(personUuid);
        formData.setStatus(Constants.STATUS_COMPLETE);
        formData.setUserUuid(loggedInUser.getUuid());
        formData.setTemplateUuid(templateUuid);
        formData.setUuid(relationship.getUuid());
        formData.setUserSystemId(loggedInUser.getSystemId());
        formData.setXmlPayload(null);

        formData.setJsonPayload(createJsonPayload(relationship));

        return formData;
    }

    private String createJsonPayload(Relationship relationship) throws JSONException {

        // Patient
        JSONObject patientJsonObject = new JSONObject();
        patientJsonObject.put("patient.uuid",patient.getUuid());

        // Person A
        Person person = relationship.getPersonA();
        JSONObject personAJsonObject = new JSONObject();
        personAJsonObject.put("uuid", person.getUuid());

        // if PersonA is NOT a patient, then add person metadata
        if (personIsNotPatient(person.getUuid())) {
            personAJsonObject.put("given_name", person.getGivenName());
            personAJsonObject.put("middle_name", person.getMiddleName());
            personAJsonObject.put("family_name", person.getFamilyName());
            personAJsonObject.put("sex", person.getGender());
            personAJsonObject.put("birth_date", DateUtils.getFormattedDateTime(person.getBirthdate()));
            personAJsonObject.put("birthdate_estimated", person.getBirthdateEstimated());
            personAJsonObject.put("addresses", createAddressesJsonArray(person));
            personAJsonObject.put("attributes", createAttributesJsonArray(person));
        }

        // Person B
        person = relationship.getPersonB();
        JSONObject personBJsonObject = new JSONObject();
        personBJsonObject.put("uuid",relationship.getPersonB().getUuid());

        // if PersonB is NOT a patient, then add person metadata
        if (personIsNotPatient(person.getUuid())) {
            personBJsonObject.put("given_name", person.getGivenName());
            personBJsonObject.put("middle_name", person.getMiddleName());
            personBJsonObject.put("family_name", person.getFamilyName());
            personBJsonObject.put("sex", person.getGender());
            personBJsonObject.put("birth_date", DateUtils.getFormattedDateTime(person.getBirthdate()));
            personBJsonObject.put("birthdate_estimated", person.getBirthdateEstimated());
            personBJsonObject.put("addresses", createAddressesJsonArray(person));
            personBJsonObject.put("attributes", createAttributesJsonArray(person));
        }

        // Relationship Type
        JSONObject relationshipTypeJsonObject = new JSONObject();
        relationshipTypeJsonObject.put("uuid", relationship.getRelationshipType().getUuid());

        // Encounter Details
        JSONObject encounterJsonObject = new JSONObject();
        encounterJsonObject.put("encounter.provider_id_select", loggedInUser.getSystemId());
        encounterJsonObject.put("encounter.provider_id", loggedInUser.getSystemId());

        // Discriminator
        JSONObject discriminatorObject = new JSONObject();
        discriminatorObject.put("discriminator", Constants.FORM_JSON_DISCRIMINATOR_RELATIONSHIP);

        JSONObject jsonMainObject = new JSONObject();
        jsonMainObject.put("uuid", relationship.getUuid());
        jsonMainObject.put("patient", patientJsonObject);
        jsonMainObject.put("personA", personAJsonObject);
        jsonMainObject.put("personB", personBJsonObject);
        jsonMainObject.put("relationshipType", relationshipTypeJsonObject);
        jsonMainObject.put("encounter", encounterJsonObject);
        jsonMainObject.put("discriminator", discriminatorObject);

        return jsonMainObject.toString();
    }

    private JSONArray createAddressesJsonArray(Person person) throws JSONException {
        JSONArray addressesJSONArray = new JSONArray();
        if(!person.getAddresses().isEmpty()) {
            List<PersonAddress> addresses = person.getAddresses();
            for (PersonAddress address : addresses) {
                JSONObject addressJSONObject = new JSONObject();
                addressJSONObject.put("address1", address.getAddress1());
                addressJSONObject.put("address2", address.getAddress2());
                addressJSONObject.put("address3", address.getAddress3());
                addressJSONObject.put("address4", address.getAddress4());
                addressJSONObject.put("address5", address.getAddress5());
                addressJSONObject.put("address6", address.getAddress6());
                addressJSONObject.put("cityVillage", address.getCityVillage());
                addressJSONObject.put("stateProvince", address.getStateProvince());
                addressJSONObject.put("country", address.getCountry());
                addressJSONObject.put("postalCode", address.getPostalCode());
                addressJSONObject.put("countyDistrict", address.getCountyDistrict());
                addressJSONObject.put("latitude", address.getLatitude());
                addressJSONObject.put("longitude", address.getLongitude());
                addressJSONObject.put("startDate", address.getStartDate());
                addressJSONObject.put("endDate", address.getEndDate());
                addressJSONObject.put("preferred", address.getPreferred());
                addressJSONObject.put("uuid", address.getUuid());
                addressesJSONArray.put(addressJSONObject);
            }
        }
        return addressesJSONArray;
    }

    private JSONArray createAttributesJsonArray(Person person) throws JSONException {
        JSONArray attributesJSONArray = new JSONArray();
        if(!person.getAtributes().isEmpty()){
            List<PersonAttribute> attributes = person.getAtributes();

            for(PersonAttribute attribute : attributes){
                JSONObject attributeJSONObject = new JSONObject();
                attributeJSONObject.put("attribute_type_uuid",attribute.getAttributeType().getUuid());
                attributeJSONObject.put("attribute_type_name",attribute.getAttributeType().getName());
                attributeJSONObject.put("attribute_value",attribute.getAttribute());
                attributesJSONArray.put(attributeJSONObject);
            }
        }
        return attributesJSONArray;
    }

    public Person createNewPerson( String jsonPayload, String personUuid) {
        try {
            JSONObject responseJSON = new JSONObject(jsonPayload);
            JSONObject personJSON = responseJSON.getJSONObject("patient");

            Person person = new Person();
            person.setUuid(personUuid);
            person.setGender(personJSON.getString("patient.sex"));
            List<PersonName> names = new ArrayList<>();
            names.add(PersonRegistrationUtils.createPersonName(personJSON));
            person.setNames(names);
            person.setBirthdate(PersonRegistrationUtils.createBirthDate(personJSON));
            person.setBirthdateEstimated(PersonRegistrationUtils.createBirthDateEstimated(personJSON));
            person.setAddresses(PersonRegistrationUtils.createPersonAddresses(personJSON));
            person.setAttributes(PersonRegistrationUtils.createPersonAttributes(personJSON, muzimaApplication));
            return person;
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "Could not create new person", e);
        }
        return null;
    }

    private boolean personIsNotPatient(String personUuid) {
        try {
            return patientController.getPatientByUuid(personUuid) == null;
        } catch (PatientController.PatientLoadException e) {
            Log.e(getClass().getSimpleName(), "Could not load patient",e);
        }
        return true;
    }
}
