/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.forms;

import com.muzima.api.model.FormData;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.Date;
import java.util.Scanner;

import static com.muzima.builder.PatientBuilder.patient;
import static com.muzima.utils.DateUtils.getFormattedDate;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PatientJSONMapperTest {

    private PatientJSONMapper mapper;
    private FormData formData;

    @Before
    public void setUp() throws Exception {
        mapper = new PatientJSONMapper(getJSONContent());
        formData = mock(FormData.class);
    }


    @Test
    public void shouldContainAMRSIdOfPatient() throws Exception {
        String resultJSON = mapper.map(patient().withIdentifier("id").instance(), formData);
        assertThat(resultJSON, containsString("{\"name\":\"patient.medical_record_number\",\"value\":\"id\",\"bind\":\"/model/instance/form/patient/patient.medical_record_number\"}"));
    }

    @Test
    public void shouldContainEmptyStringIfAnyStringAttributeIsMissing() throws Exception {
        String resultJSON = mapper.map(patient().instance(), formData);
        assertThat(resultJSON, containsString("{\"name\":\"patient.medical_record_number\",\"value\":\"\",\"bind\":\"/model/instance/form/patient/patient.medical_record_number\"}"));
    }

    @Test
    public void shouldContainBirthDateIfPresent() throws Exception {
        Date currentDate = new Date();
        String resultJSON = mapper.map(patient().withBirthdate(currentDate).instance(), formData);
        assertThat(resultJSON, containsString("{\"name\":\"patient.birthdate\",\"value\":\"" + getFormattedDate(currentDate) + "\",\"bind\":\"/model/instance/form/patient/patient.birthdate\"}"));
    }

    @Test
    public void shouldNotContainBirthDateEntryIfNotPresent() throws Exception {
        String resultJSON = mapper.map(patient().instance(), formData);
        assertThat(resultJSON, containsString("{\"name\":\"patient.birthdate\",\"bind\":\"/model/instance/form/patient/patient.birthdate\"}"));
    }

    @Test
    public void shouldContainFamilyName() throws Exception {
        String resultJSON = mapper.map(patient().withFamilyName("familyName").instance(), formData);
        assertThat(resultJSON, containsString("{\"name\":\"patient.family_name\",\"value\":\"familyName\",\"bind\":\"/model/instance/form/patient/patient.family_name\"}"));
    }

    @Test
    public void shouldContainGivenName() throws Exception {
        String resultJSON = mapper.map(patient().withGivenName("givenName").instance(), formData);
        assertThat(resultJSON, containsString("{\"name\":\"patient.given_name\",\"value\":\"givenName\",\"bind\":\"/model/instance/form/patient/patient.given_name\"}"));
    }

    @Test
    public void shouldContainMiddleName() throws Exception {
        String resultJSON = mapper.map(patient().withMiddleName("middleName").instance(), formData);
        assertThat(resultJSON, containsString("{\"name\":\"patient.middle_name\",\"value\":\"middleName\",\"bind\":\"/model/instance/form/patient/patient.middle_name\"}"));
    }

    @Test
    public void shouldContainGender() throws Exception {
        String resultJSON = mapper.map(patient().withSex("f").instance(), formData);
        assertThat(resultJSON, containsString("{\"name\":\"patient.sex\",\"value\":\"f\",\"bind\":\"/model/instance/form/patient/patient.sex\"}"));
    }

    @Test
    public void shouldContainFormUUID() throws Exception{
        when(formData.getTemplateUuid()).thenReturn("this-is-a-form-uuid");
        String resultJSON = mapper.map(patient().instance(), formData);
        assertThat(resultJSON,containsString("{\"name\":\"encounter.form_uuid\",\"value\":\"this-is-a-form-uuid\",\"bind\":\"/model/instance/form/encounter/encounter.form_uuid\"}"));
    }

    private String getJSONContent() {
        InputStream fileStream = getClass().getClassLoader().getResourceAsStream("patient/form.json");
        Scanner s = new Scanner(fileStream).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "{}";
    }

}
