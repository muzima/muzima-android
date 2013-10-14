package com.muzima.view.forms;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static com.muzima.builder.PatientBuilder.patient;
import static com.muzima.utils.DateUtils.getFormattedDate;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;

public class PatientFormJSONConverterTest {

    private PatientFormJSONConverter converter;

    @Before
    public void setUp() throws Exception {
        converter = new PatientFormJSONConverter();
    }

    @Test
    public void shouldConvertPatientIdentifierToMedicalRecordsNumber() throws Exception {
        String json = converter.convert(patient().withIdentifier("id").instance());
        JSONObject form = (JSONObject) new JSONObject(json).get("form");

        JSONObject field = (JSONObject) ((JSONArray) form.get("fields")).get(0);
        assertThat((String) field.get("name"), is("patient.medical_record_number"));
        assertThat((String) field.get("value"), is("id"));
    }

    @Test
    public void shouldConvertPatientBirthDateToDateOfBirth() throws Exception {
        Date currentDate = new Date();
        String json = converter.convert(patient().withBirthdate(currentDate).instance());
        JSONObject form = (JSONObject) new JSONObject(json).get("form");

        JSONObject field = (JSONObject) ((JSONArray) form.get("fields")).get(4);
        assertThat((String) field.get("name"), is("patient.birthdate"));
        assertThat((String) field.get("value"), is(getFormattedDate(currentDate)));
    }

    @Test
    public void shouldNotMapDateOfBirthIfPatientBirthDateIsEmpty() throws Exception {
        String json = converter.convert(patient().instance());
        JSONObject form = (JSONObject) new JSONObject(json).get("form");
        JSONArray fieldArray = (JSONArray) form.get("fields");
        for (int i = 0; i < fieldArray.length(); i++) {
            JSONObject jsonObject = (JSONObject) fieldArray.get(i);
            assertNotSame(jsonObject.get("name"), "patient.birthdate");
        }
    }

    @Test
    public void shouldMapPatientsFamilyName() throws Exception {
        String json = converter.convert(patient().withFamilyName("familyName").instance());
        JSONObject form = (JSONObject) new JSONObject(json).get("form");

        JSONObject field = (JSONObject) ((JSONArray) form.get("fields")).get(1);
        assertThat((String) field.get("name"), is("patient.family_name"));
        assertThat((String) field.get("value"), is("familyName"));
    }

    @Test
    public void shouldMapPatientsGivenName() throws Exception {
        String json = converter.convert(patient().withGivenName("givenName").instance());
        JSONObject form = (JSONObject) new JSONObject(json).get("form");

        JSONObject field = (JSONObject) ((JSONArray) form.get("fields")).get(2);
        assertThat((String) field.get("name"), is("patient.given_name"));
        assertThat((String) field.get("value"), is("givenName"));
    }

    @Test
    public void shouldMapPatientsMiddleName() throws Exception {
        String json = converter.convert(patient().withMiddleName("middleName").instance());
        JSONObject form = (JSONObject) new JSONObject(json).get("form");

        JSONObject field = (JSONObject) ((JSONArray) form.get("fields")).get(3);
        assertThat((String) field.get("name"), is("patient.middle_name"));
        assertThat((String) field.get("value"), is("middleName"));
    }

}
