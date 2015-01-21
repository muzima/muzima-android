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
import com.muzima.api.model.User;
import com.muzima.utils.DateUtils;
import com.muzima.utils.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

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
//                encounterDetails.put("encounter.provider_id_select", loggedInUser.getSystemId());
//                encounterDetails.put("encounter.provider_id", loggedInUser.getSystemId());
            }
            prepopulateJSON.put("patient",patientDetails);
            prepopulateJSON.put("encounter",encounterDetails);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return prepopulateJSON.toString();
    }
}
