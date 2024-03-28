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

import com.muzima.MuzimaApplication;
import com.muzima.api.model.FormData;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PatientIdentifier;
import com.muzima.api.model.PersonAddress;
import com.muzima.api.model.User;
import com.muzima.controller.FormController;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.muzima.utils.Constants.FORM_JSON_DISCRIMINATOR_DEMOGRAPHICS_UPDATE;
import static com.muzima.utils.Constants.STATUS_COMPLETE;

public class GeolocationJsonMapper {
    private final Patient patient;
    private MuzimaApplication muzimaApplication;
    public GeolocationJsonMapper(Patient patient, MuzimaApplication muzimaApplication){
        this.patient = patient;
        this.muzimaApplication = muzimaApplication;
    }

    public void createAndSaveLocationUpdateFormData() throws JSONException, FormController.FormDataSaveException {
            final User user = muzimaApplication.getAuthenticatedUser();

            FormData formData = new FormData() {{
                setUuid(UUID.randomUUID().toString());
                setPatientUuid(patient.getUuid());
                setUserUuid(user.getUuid());
                setUserSystemId(user.getSystemId());
                setStatus(STATUS_COMPLETE);
                setTemplateUuid(UUID.randomUUID().toString());
                setEncounterDate(new Date());
                setDiscriminator(FORM_JSON_DISCRIMINATOR_DEMOGRAPHICS_UPDATE);
            }};

            JSONObject formDataJSON = new JSONObject();

            formDataJSON.put("demographicsupdate", createDemographicsUpdateStub());
            formDataJSON.put("patient",createPatientStub());
            formDataJSON.put("encounter",createEncounterStub(formData));

            formData.setJsonPayload(formDataJSON.toString());
            muzimaApplication.getFormController().saveFormData(formData);
    }

    private JSONObject createPatientStub() throws JSONException {
        JSONObject patientDetails = new JSONObject();
        List<PatientIdentifier> identifiers = patient.getIdentifiers();
        for(PatientIdentifier identifier:identifiers) {
            if(identifier.isPreferred()) {
                JSONObject medicalRecordNumber = new JSONObject();
                medicalRecordNumber.put("identifier_value", identifier.getIdentifier());
                medicalRecordNumber.put("identifier_type_uuid", identifier.getIdentifierType().getUuid());
                medicalRecordNumber.put("identifier_type_name", identifier.getIdentifierType().getName());
                patientDetails.put("patient.medical_record_number", medicalRecordNumber);
                break;
            }
        }
        patientDetails.put("patient.given_name", StringUtils.defaultString(patient.getGivenName()));
        patientDetails.put("patient.middle_name", StringUtils.defaultString(patient.getMiddleName()));
        patientDetails.put("patient.family_name", StringUtils.defaultString(patient.getFamilyName()));
        patientDetails.put("patient.sex", StringUtils.defaultString(patient.getGender()));
        patientDetails.put("patient.uuid", StringUtils.defaultString(patient.getUuid()));
        if (patient.getBirthdate() != null) {
            patientDetails.put("patient.birth_date", DateUtils.getFormattedDate(patient.getBirthdate()));
        }
        return patientDetails;
    }

    private JSONObject createEncounterStub(FormData formData) throws JSONException {
        JSONObject encounterDetails = new JSONObject();
        encounterDetails.put("encounter.form_uuid", StringUtils.defaultString(formData.getTemplateUuid()));
        encounterDetails.put("encounter.user_system_id", StringUtils.defaultString(formData.getUserSystemId()));

        User loggedInUser = muzimaApplication.getAuthenticatedUser();
        encounterDetails.put("encounter.provider_id_select", loggedInUser.getSystemId());
        encounterDetails.put("encounter.provider_id", loggedInUser.getSystemId());
        return encounterDetails;
    }

    private JSONObject createDemographicsUpdateStub() throws JSONException {
        JSONObject demographicsUpdateJson = new JSONObject();

        List<PersonAddress> addresses = patient.getAddresses();
        int count = 0;
        for (PersonAddress personAddress : addresses) {
            JSONObject personAddressUpdateJson = new JSONObject();
            if (!StringUtils.isEmpty(personAddress.getAddress1())) {
                personAddressUpdateJson.put("address1", personAddress.getAddress1());
            }
            if (!StringUtils.isEmpty(personAddress.getAddress2())) {
                personAddressUpdateJson.put("address2", personAddress.getAddress2());
            }
            if (!StringUtils.isEmpty(personAddress.getAddress3())) {
                personAddressUpdateJson.put("address3", personAddress.getAddress3());
            }
            if (!StringUtils.isEmpty(personAddress.getAddress4())) {
                personAddressUpdateJson.put("address4", personAddress.getAddress4());
            }
            if (!StringUtils.isEmpty(personAddress.getAddress5())) {
                personAddressUpdateJson.put("address5", personAddress.getAddress5());
            }
            if (!StringUtils.isEmpty(personAddress.getAddress6())) {
                personAddressUpdateJson.put("address6", personAddress.getAddress6());
            }
            if (!StringUtils.isEmpty(personAddress.getCityVillage())) {
                personAddressUpdateJson.put("cityVillage", personAddress.getCityVillage());
            }
            if (!StringUtils.isEmpty(personAddress.getCountyDistrict())) {
                personAddressUpdateJson.put("countyDistrict", personAddress.getCountyDistrict());
            }
            if (!StringUtils.isEmpty(personAddress.getCountry())) {
                personAddressUpdateJson.put("country", personAddress.getCountry());
            }
            if (!StringUtils.isEmpty(personAddress.getPostalCode())) {
                personAddressUpdateJson.put("postalCode", personAddress.getPostalCode());
            }
            if (!StringUtils.isEmpty(personAddress.getLatitude())) {
                personAddressUpdateJson.put("latitude", personAddress.getLatitude());
            }
            if (!StringUtils.isEmpty(personAddress.getLongitude())) {
                personAddressUpdateJson.put("longitude", personAddress.getLongitude());
            }
            if (personAddress.getStartDate() != null) {
                personAddressUpdateJson.put("startDate", personAddress.getStartDate());
            }
            if (personAddress.getEndDate() != null) {
                personAddressUpdateJson.put("endDate", personAddress.getEndDate());
            }
            personAddressUpdateJson.put("preferred", personAddress.getPreferred());

            count++;
            demographicsUpdateJson.put("demographicsupdate.personaddress^" + count, personAddressUpdateJson);
        }
        return demographicsUpdateJson;
    }
}
