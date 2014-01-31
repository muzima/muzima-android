package com.muzima.view.forms;

import com.muzima.api.model.FormData;
import com.muzima.api.model.Patient;
import com.muzima.utils.DateUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class HTMLPatientJSONMapper {


    public String map(Patient patient, FormData formData) {
        JSONObject details = new JSONObject();
        try {
            details.put("patient.given_name", patient.getGivenName());
            details.put("patient.middle_name", patient.getMiddleName());
            details.put("patient.family_name", patient.getFamilyName());
            details.put("patient.sex", patient.getGender());
            details.put("patient.uuid", patient.getUuid());
            if (patient.getBirthdate() != null) {
                details.put("patient.birthdate", DateUtils.getFormattedDate(patient.getBirthdate()));
            }
            details.put("encounter.form_uuid", formData.getUuid());

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return details.toString();
    }
}
