package com.muzima.view.forms;

import com.muzima.api.model.Patient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.muzima.utils.DateUtils.getFormattedDate;

public class PatientFormJSONConverter {

    public String convert(Patient patient) throws JSONException {
        Form form = new Form();
        form.addField("patient.medical_record_number", patient.getIdentifier());
        form.addField("patient.family_name", patient.getFamilyName());
        form.addField("patient.given_name", patient.getGivenName());
        form.addField("patient.middle_name", patient.getMiddleName());
        if (patient.getBirthdate() != null) {
            form.addField("patient.birthdate", getFormattedDate(patient.getBirthdate()));
        }
        return form.toString();
    }


    private class Form {

        private JSONArray fieldArray;
        private JSONObject root;

        private Form() throws JSONException {
            root = new JSONObject();
            JSONObject fields = new JSONObject();
            root.put("form", fields);
            fieldArray = new JSONArray();
            fields.put("fields", fieldArray);
        }

        public void addField(String name, Object value) throws JSONException {
            JSONObject field = new JSONObject();
            field.put("name", name);
            field.put("value", value);
            fieldArray.put(field);
        }

        @Override
        public String toString() {
            return root.toString();
        }
    }
}
