package com.muzima.model.builders;

import com.muzima.api.model.Form;
import com.muzima.model.CompleteForm;

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

        CompleteForm completeForm = new CompleteFormBuilder().withCompleteForm(form).build();

        assertThat(completeForm.getName(), is(form.getName()));
        assertThat(completeForm.getDescription(), is(form.getDescription()));
        assertThat(completeForm.getFormUuid(), is(form.getUuid()));
    }

    @Test
    public void withPatientInfo_shouldSetPatientInfoValues() throws Exception {
        CompleteForm completeForm = new CompleteFormBuilder().withPatientInfo("Obama","Barack", "Hussein",  "USNO1").build();

        assertThat(completeForm.getPatientFamilyName(), is("Obama"));
        assertThat(completeForm.getPatientGivenName(), is("Barack"));
        assertThat(completeForm.getPatientMiddleName(), is("Hussein"));
        assertThat(completeForm.getPatientIdentifier(), is("USNO1"));
    }

    @Test
    public void withFormDataId_shouldSetFromDataUuid() throws Exception {
        CompleteForm completeForm = new CompleteFormBuilder().withFormDataUuid("uuid").build();

        assertThat(completeForm.getFormDataUuid(), is("uuid"));
    }

    @Test
    public void withLastModifiedDate_shouldSetLastModifiedDate() throws Exception {
        CompleteForm completeForm = new CompleteFormBuilder().withLastModifiedData("24/09/2013").build();

        assertThat(completeForm.getLastModifiedDate(), is("24/09/2013"));
    }
}
