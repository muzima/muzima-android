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
import com.muzima.api.model.Tag;
import com.muzima.model.AvailableForm;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class AvailableFormBuilderTest {

    @Test
    public void withAvailableForm_shouldSetGivenValues() {
        Form form = new Form();
        form.setName("name");
        form.setName("description");
        form.setUuid("uuid");
        form.setTags(new Tag[]{
                new Tag(){{
                    setName("tag1");
                    setUuid("tag1Uuid");
                }},
                new Tag(){{
                    setName("tag2");
                    setUuid("tag2Uuid");
                }}
        });

        AvailableForm availableForm = new AvailableFormBuilder().withAvailableForm(form).build();

        assertThat(availableForm.getName(), is(form.getName()));
        assertThat(availableForm.getDescription(), is(form.getDescription()));
        assertThat(availableForm.getTags(), is(form.getTags()));
        assertThat(availableForm.getFormUuid(), is(form.getUuid()));
    }

    @Test
    public void withDownloadStatus_shouldSetDownloadStatus() {
        AvailableForm availableForm = new AvailableFormBuilder().withDownloadStatus(true).build();

        assertThat(availableForm.isDownloaded(), is(true));
    }

}
