package com.muzima.view.forms;

import com.muzima.api.model.Patient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.muzima.utils.DateUtils.getFormattedDate;
import static org.apache.commons.lang.StringUtils.defaultString;

public class PatientJSONMapper {

    private final JSONObject model;

    public PatientJSONMapper(String modelJSON) throws JSONException {
        model = new JSONObject(modelJSON);
    }

    public String map(Patient patient) throws JSONException {
        Map<String, String> valueMap = convert(patient);
        JSONObject form = model.getJSONObject("form");
        JSONArray fields = form.getJSONArray("fields");
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.getJSONObject(i);
            String name = field.getString("name");
            if (valueMap.containsKey(name)) {
                field.put("value", valueMap.get(name));
            }
        }
        return model.toString();
    }

    private Map<String, String> convert(Patient patient) {
        Map<String, String> patientValueMap = new HashMap<String, String>();
        patientValueMap.put("patient.medical_record_number", defaultString(patient.getIdentifier()));
        patientValueMap.put("patient.family_name", defaultString(patient.getFamilyName()));
        patientValueMap.put("patient.given_name", defaultString(patient.getGivenName()));
        patientValueMap.put("patient.middle_name", defaultString(patient.getMiddleName()));
        patientValueMap.put("patient.sex", defaultString(patient.getGender()));
        if (patient.getBirthdate() != null) {
            patientValueMap.put("patient.birthdate", getFormattedDate(patient.getBirthdate()));
        }
        return patientValueMap;
    }
}
