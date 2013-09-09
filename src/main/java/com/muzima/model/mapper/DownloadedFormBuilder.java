package com.muzima.model.mapper;

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
