/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.model.builders;

import com.muzima.api.model.Form;
import com.muzima.model.DownloadedForm;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class DownloadedFormBuilderTest {

    @Test
    public void withDownloadedForm_shouldSetGivenValues() {
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
