/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.model.builders;

import com.muzima.api.model.Form;
import com.muzima.model.CompleteFormWithPatientData;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CompleteFormBuilderTest {

    @Test
    public void withCompleteForm_shouldSetGivenValues() throws Exception {
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
    public void withFormDataId_shouldSetFromDataUuid() throws Exception {
        CompleteFormWithPatientData completeForm = new CompleteFormWithPatientDataBuilder().withFormDataUuid("uuid").build();

        assertThat(completeForm.getFormDataUuid(), is("uuid"));
    }

    @Test
    public void withLastModifiedDate_shouldSetLastModifiedDate() throws Exception {
        CompleteFormWithPatientData completeForm = new CompleteFormWithPatientDataBuilder().withLastModifiedData("24/09/2013").build();

        assertThat(completeForm.getLastModifiedDate(), is("24/09/2013"));
    }
}
