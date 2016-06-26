/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.model.builders;

import com.muzima.api.model.Form;
import com.muzima.model.DownloadedForm;

public class DownloadedFormBuilder {
    private DownloadedForm downloadedForm;

    public DownloadedFormBuilder() {
        downloadedForm = new DownloadedForm();
    }

    public DownloadedFormBuilder withDownloadedForm(Form filteredForm) {
        downloadedForm.setName(filteredForm.getName());
        downloadedForm.setDescription(filteredForm.getDescription());
        downloadedForm.setFormUuid(filteredForm.getUuid());
        return this;
    }

    public DownloadedForm build() {
        return downloadedForm;
    }
}
