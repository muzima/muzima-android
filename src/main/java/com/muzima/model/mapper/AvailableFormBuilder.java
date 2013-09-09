package com.muzima.model.mapper;

import com.muzima.api.model.Form;
import com.muzima.model.AvailableForm;

public class AvailableFormBuilder{
    private AvailableForm availableForm;

    public AvailableFormBuilder() {
        availableForm = new AvailableForm();
    }

    public AvailableFormBuilder withAvailableForm(Form filteredForm) {
        availableForm.setName(filteredForm.getName());
        availableForm.setDescription(filteredForm.getDescription());
        availableForm.setTags(filteredForm.getTags());
        availableForm.setFormUuid(filteredForm.getUuid());
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
