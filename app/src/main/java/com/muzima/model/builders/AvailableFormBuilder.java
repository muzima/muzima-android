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
import com.muzima.model.AvailableForm;

public class AvailableFormBuilder{
    private final AvailableForm availableForm;

    public AvailableFormBuilder() {
        availableForm = new AvailableForm();
    }

    public AvailableFormBuilder withAvailableForm(Form form) {
        availableForm.setName(form.getName());
        availableForm.setDescription(form.getDescription());
        availableForm.setTags(form.getTags());
        availableForm.setDiscriminator(form.getDiscriminator());
        availableForm.setFormUuid(form.getUuid());
        availableForm.setDiscriminator(form.getDiscriminator());
        return this;
    }

    public AvailableFormBuilder withDownloadStatus(boolean downloadStatus) {
        availableForm.setDownloaded(downloadStatus);
        return this;
    }

    public AvailableForm build() {
        return availableForm;
    }
}
