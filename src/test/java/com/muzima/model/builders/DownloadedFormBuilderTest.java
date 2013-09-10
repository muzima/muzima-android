package com.muzima.model.builders;

import com.muzima.api.model.Form;
import com.muzima.model.DownloadedForm;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class DownloadedFormBuilderTest {

    @Test
    public void withDownloadedForm_shouldSetGivenValues() throws Exception {
        Form form = new Form();
        form.setName("name");
        form.setName("description");
        form.setUuid("uuid");

        DownloadedForm downloadedForm = new DownloadedFormBuilder().withDownloadedForm(form).build();

        assertThat(downloadedForm.getName(), is(form.getName()));
        assertThat(downloadedForm.getDescription(), is(form.getDescription()));
        assertThat(downloadedForm.getFormUuid(), is(form.getUuid()));
    }
}
