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

import com.muzima.MuzimaApplication;
import com.muzima.api.model.FormData;
import com.muzima.api.model.Patient;
import com.muzima.api.model.User;
import com.muzima.builder.PatientBuilder;
import com.muzima.utils.StringUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.muzima.utils.Constants.STANDARD_DATE_FORMAT;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class HTMLPatientJSONMapperTest {
    private MuzimaApplication muzimaApplication;

    @Before
    public void setup() throws IOException {
        muzimaApplication = mock(MuzimaApplication.class);
        User user = new User();
        when(muzimaApplication.getAuthenticatedUser()).thenReturn(user);
    }

    @Test
    public void shouldAddPatientDetailsOnJSONFromPatient() {
        Date birthdate = new Date();
        SimpleDateFormat formattedDate = new SimpleDateFormat(STANDARD_DATE_FORMAT);
        Patient patient = patient(new Date());
        HTMLPatientJSONMapper mapper = new HTMLPatientJSONMapper();
        FormData formData = new FormData();
        formData.setTemplateUuid("formUuid");
        String json = mapper.map(muzimaApplication,patient, formData, false, StringUtils.EMPTY);
        assertThat(json, containsString("\"patient\":{"));
        assertThat(json, containsString("\"encounter\":{"));
        assertThat(json, containsString("\"patient.given_name\":\"givenname\""));
        assertThat(json, containsString("\"patient.middle_name\":\"middlename\""));
        assertThat(json, containsString("\"patient.family_name\":\"familyname\""));
        assertThat(json, containsString("\"patient.sex\":\"Female\""));
        assertThat(json, containsString("\"patient.uuid\":\"uuid\""));
        assertThat(json, containsString("\"patient.birth_date\":\"" + formattedDate.format(birthdate) + "\""));
        assertThat(json, containsString("\"encounter.form_uuid\":\"formUuid\""));
    }

    @Test
    public void shouldNotFailIFBirthDateIsNull() {
        Patient patient = patient(null);
        HTMLPatientJSONMapper htmlPatientJSONMapper = new HTMLPatientJSONMapper();
        String json = htmlPatientJSONMapper.map(muzimaApplication,patient, new FormData(), false, StringUtils.EMPTY);
        assertThat(json,not(containsString("\"patient.birth_date\"")));
    }

    private Patient patient(Date birthdate) {
        return new PatientBuilder()
                .withFamilyName("familyname")
                .withMiddleName("middlename")
                .withGivenName("givenname")
                .withSex("Female")
                .withBirthdate(birthdate)
                .withUuid("uuid")
                .instance();
    }
}
