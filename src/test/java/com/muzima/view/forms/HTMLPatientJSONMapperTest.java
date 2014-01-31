package com.muzima.view.forms;

import com.muzima.api.model.FormData;
import com.muzima.api.model.Patient;
import com.muzima.builder.PatientBuilder;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

public class HTMLPatientJSONMapperTest {


    @Test
    public void shouldAddPatientDetailsOnJSONFromPatient() throws Exception {
        Date birthdate = new Date();
        SimpleDateFormat formattedDate = new SimpleDateFormat("yyyy/MM/dd");
        Patient patient = patient("givenname", "middlename", "familyname", "Female", new Date(), "uuid");
        HTMLPatientJSONMapper mapper = new HTMLPatientJSONMapper();
        FormData formData = new FormData();
        formData.setUuid("formUuid");
        String json = mapper.map(patient, formData);
        assertThat(json, containsString("\"patient.given_name\":\"givenname\""));
        assertThat(json, containsString("\"patient.middle_name\":\"middlename\""));
        assertThat(json, containsString("\"patient.family_name\":\"familyname\""));
        assertThat(json, containsString("\"patient.sex\":\"Female\""));
        assertThat(json, containsString("\"patient.uuid\":\"uuid\""));
        assertThat(json, containsString("\"patient.birthdate\":\"" + formattedDate.format(birthdate) + "\""));
        assertThat(json, containsString("\"encounter.form_uuid\":\"formUuid\""));
    }

    @Test
    public void shouldNotFailIFBirthDateIsNull() throws Exception {
        Patient patient = patient("givenname", "middlename", "familyname", "Female", null, "uuid");
        HTMLPatientJSONMapper htmlPatientJSONMapper = new HTMLPatientJSONMapper();
        String json = htmlPatientJSONMapper.map(patient, new FormData());
        assertThat(json,not(containsString("\"patient.birthdate\"")));
    }

    private Patient patient(String givenName, String middleName, String familyName, String sex, Date birthdate, String uuid) {
        return new PatientBuilder()
                .withFamilyName(familyName)
                .withMiddleName(middleName)
                .withGivenName(givenName)
                .withSex(sex)
                .withBirthdate(birthdate)
                .withUuid(uuid)
                .instance();
    }
}
