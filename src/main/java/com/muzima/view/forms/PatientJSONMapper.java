package com.muzima.view.forms;

import com.muzima.api.model.Patient;
import com.muzima.api.model.PersonName;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.*;

import static com.muzima.utils.DateUtils.getFormattedDate;
import static com.muzima.utils.DateUtils.parse;
import static java.util.Arrays.asList;
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

    public Patient map() throws JSONException {
        JSONObject form = model.getJSONObject("form");
        JSONArray fields = form.getJSONArray("fields");
        Map<String, String> paramsMap = convertJSONToPatientAttr(fields);
        return patient(paramsMap);
    }

    private Patient patient(Map<String, String> paramsMap) {
        Patient patient = new Patient();
        patient.setUuid(String.valueOf(UUID.randomUUID()));
        patient.setNames(asList(personName(paramsMap)));
        patient.setGender(paramsMap.get("patient.sex"));
        patient.setBirthdate(getDate(paramsMap, "patient.birthdate"));
        return patient;
    }

    private Date getDate(Map<String, String> paramsMap, String property) {
        String dateAsString = paramsMap.get(property);
        try {
            return dateAsString == null ? null : parse(dateAsString);
        } catch (ParseException e) {
            return null;
        }
    }

    private PersonName personName(Map<String, String> paramsMap) {
        PersonName personName = new PersonName();
        personName.setFamilyName(paramsMap.get("patient.family_name"));
        personName.setMiddleName(paramsMap.get("patient.middle_name"));
        personName.setGivenName(paramsMap.get("patient.given_name"));
        return personName;
    }

    private Map<String, String> convertJSONToPatientAttr(JSONArray fields) throws JSONException {
        Map<String, String> patientParamsMap = new HashMap<String, String>();
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.getJSONObject(i);
            try {
                patientParamsMap.put(field.getString("name"), field.getString("value"));
            } catch (JSONException e) {
                //Ignore
            }
        }
        return patientParamsMap;
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
