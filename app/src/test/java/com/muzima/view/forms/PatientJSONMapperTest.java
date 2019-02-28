/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.forms;

import com.muzima.api.model.FormData;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.InputStream;
import java.util.Date;
import java.util.Scanner;

import static com.muzima.builder.PatientBuilder.patient;
import static com.muzima.utils.DateUtils.getFormattedDate;
import static org.hamcrest.CoreMatchers.allOf;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
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
        assertThat(resultJSON, allOf(
                containsString("\"name\":\"patient.medical_record_number\""),
                containsString("\"value\":\"id\""),
                containsString("\"bind\":\"\\/model\\/instance\\/form\\/patient\\/patient.medical_record_number\"")));
    }

    @Test
    public void shouldContainEmptyStringIfAnyStringAttributeIsMissing() throws Exception {
        String resultJSON = mapper.map(patient().instance(), formData);
        assertThat(resultJSON, allOf(
                containsString("\"name\":\"patient.medical_record_number\""),
                containsString("\"value\":\"\""),
                containsString("\"bind\":\"\\/model\\/instance\\/form\\/patient\\/patient.medical_record_number\"")));
    }

    @Test
    public void shouldContainBirthDateIfPresent() throws Exception {
        Date currentDate = new Date();
        String resultJSON = mapper.map(patient().withBirthdate(currentDate).instance(), formData);
        assertThat(resultJSON, allOf(
                containsString("\"name\":\"patient.birthdate\""),
                containsString("\"value\":\"" + getFormattedDate(currentDate) + "\""),
                containsString("\"bind\":\"\\/model\\/instance\\/form\\/patient\\/patient.birthdate\"")));
    }

    @Test
    public void shouldNotContainBirthDateEntryIfNotPresent() throws Exception {
        String resultJSON = mapper.map(patient().instance(), formData);
        assertThat(resultJSON, allOf(
                containsString("\"name\":\"patient.birthdate\""),
                containsString("\"bind\":\"\\/model\\/instance\\/form\\/patient\\/patient.birthdate\"")));
    }

    @Test
    public void shouldContainFamilyName() throws Exception {
        String resultJSON = mapper.map(patient().withFamilyName("familyName").instance(), formData);
        assertThat(resultJSON, allOf(
                containsString("\"name\":\"patient.family_name\""),
                containsString("\"value\":\"familyName\""),
                containsString("\"bind\":\"\\/model\\/instance\\/form\\/patient\\/patient.family_name\"")));
    }

    @Test
    public void shouldContainGivenName() throws Exception {
        String resultJSON = mapper.map(patient().withGivenName("givenName").instance(), formData);
        assertThat(resultJSON, allOf(
                containsString("\"name\":\"patient.given_name\""),
                containsString("\"value\":\"givenName\""),
                containsString("\"bind\":\"\\/model\\/instance\\/form\\/patient\\/patient.given_name\"")));
    }

    @Test
    public void shouldContainMiddleName() throws Exception {
        String resultJSON = mapper.map(patient().withMiddleName("middleName").instance(), formData);
        assertThat(resultJSON, allOf(
                containsString("\"name\":\"patient.middle_name\""),
                containsString("\"value\":\"middleName\""),
                containsString("\"bind\":\"\\/model\\/instance\\/form\\/patient\\/patient.middle_name\"")));
    }

    @Test
    public void shouldContainGender() throws Exception {
        String resultJSON = mapper.map(patient().withSex("f").instance(), formData);
        assertThat(resultJSON, allOf(
                containsString("\"name\":\"patient.sex\""),
                containsString("\"value\":\"f\""),
                containsString("\"bind\":\"\\/model\\/instance\\/form\\/patient\\/patient.sex\"")));
    }

    @Test
    public void shouldContainFormUUID() throws Exception{
        when(formData.getTemplateUuid()).thenReturn("this-is-a-form-uuid");
        String resultJSON = mapper.map(patient().instance(), formData);
        assertThat(resultJSON,allOf(
                containsString("\"name\":\"encounter.form_uuid\""),
                containsString("\"value\":\"this-is-a-form-uuid\""),
                containsString("\"bind\":\"\\/model\\/instance\\/form\\/encounter\\/encounter.form_uuid\"")));
    }

    private String getJSONContent() {
        InputStream fileStream = getClass().getClassLoader().getResourceAsStream("patient/form.json");
        Scanner s = new Scanner(fileStream).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "{}";
    }

}
