/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.model.builders;

import com.muzima.api.model.Form;
import com.muzima.model.CompleteFormWithPatientData;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CompleteFormBuilderTest {

    @Test
    public void withCompleteForm_shouldSetGivenValues() {
        Form form = new Form();
        form.setName("name");
        form.setName("description");
        form.setUuid("uuid");

        CompleteFormWithPatientData completeForm = new CompleteFormWithPatientDataBuilder().withForm(form).build();

        assertThat(completeForm.getName(), is(form.getName()));
        assertThat(completeForm.getDescription(), is(form.getDescription()));
        assertThat(completeForm.getFormUuid(), is(form.getUuid()));
    }

    @Test
    public void withFormDataId_shouldSetFromDataUuid() {
        CompleteFormWithPatientData completeForm = new CompleteFormWithPatientDataBuilder().withFormDataUuid("uuid").build();

        assertThat(completeForm.getFormDataUuid(), is("uuid"));
    }

    @Test
    public void withLastModifiedDate_shouldSetLastModifiedDate() {
        Calendar saveTimeCalender = Calendar.getInstance();
        saveTimeCalender.set(2014,8,29,10,10,10); //MONTH  Jan = 0, dec = 11
        Date formSaveDateTime = saveTimeCalender.getTime();
        CompleteFormWithPatientData completeForm = new CompleteFormWithPatientDataBuilder().withLastModifiedDate(formSaveDateTime).build();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String formSaveTime = dateFormat.format(completeForm.getLastModifiedDate());
        assertThat(formSaveTime, is("29-09-2014 10:10:10"));
    }
}
