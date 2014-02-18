package com.muzima.view.forms;

import com.muzima.api.model.FormData;
import com.muzima.api.model.Patient;
import com.muzima.utils.DateUtils;
import org.json.JSONException;
import org.json.JSONObject;

import static org.apache.commons.lang.StringUtils.defaultString;

public class HTMLPatientJSONMapper {


    public String map(Patient patient, FormData formData) {
        JSONObject prepopulateJSON = new JSONObject();
        JSONObject patientDetails = new JSONObject();
        JSONObject encounterDetails = new JSONObject();

        try {
            patientDetails.put("patient.medical_record_number", defaultString(patient.getIdentifier()));
            patientDetails.put("patient.given_name", defaultString(patient.getGivenName()));
            patientDetails.put("patient.middle_name", defaultString(patient.getMiddleName()));
            patientDetails.put("patient.family_name", defaultString(patient.getFamilyName()));
            patientDetails.put("patient.sex", defaultString(patient.getGender()));
            patientDetails.put("patient.uuid", defaultString(patient.getUuid()));
            if (patient.getBirthdate() != null) {
                patientDetails.put("patient.birthdate", DateUtils.getFormattedDate(patient.getBirthdate()));
            }
            encounterDetails.put("encounter.form_uuid", defaultString(formData.getUuid()));
            prepopulateJSON.put("patient",patientDetails);
            prepopulateJSON.put("encounter",encounterDetails);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return prepopulateJSON.toString();
    }
}
