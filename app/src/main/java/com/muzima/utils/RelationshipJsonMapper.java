/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.utils;

import com.muzima.api.model.FormData;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Person;
import com.muzima.api.model.Relationship;
import com.muzima.api.model.User;
import com.muzima.controller.PatientController;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.UUID;

import static com.muzima.utils.Constants.FORM_JSON_DISCRIMINATOR_RELATIONSHIP;
import static com.muzima.utils.Constants.STATUS_COMPLETE;

public class RelationshipJsonMapper {

    private User loggedInUser;
    private Patient patient;
    private Relationship relationship;
    private PatientController patientController;

    public RelationshipJsonMapper(Relationship relationship, Patient patient, PatientController patientController, User loggedInUser){
        this.relationship = relationship;
        this.patientController = patientController;
        this.patient =patient;
        this.loggedInUser =loggedInUser;
    }

    public FormData createFormDataFromRelationship() throws JSONException {
        FormData formData = new FormData();
        formData.setDiscriminator(FORM_JSON_DISCRIMINATOR_RELATIONSHIP);
        formData.setEncounterDate(new Date());
        formData.setPatientUuid(patient.getUuid());
        formData.setStatus(STATUS_COMPLETE);
        formData.setUserUuid(loggedInUser.getUuid());
        formData.setTemplateUuid(UUID.randomUUID().toString());
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
        discriminatorObject.put("discriminator", FORM_JSON_DISCRIMINATOR_RELATIONSHIP);

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

    private boolean personIsNotPatient(String personUuid) {
        try {
            return patientController.getPatientByUuid(personUuid) == null;
        } catch (PatientController.PatientLoadException e) {
            e.printStackTrace();
        }
        return true;
    }
}