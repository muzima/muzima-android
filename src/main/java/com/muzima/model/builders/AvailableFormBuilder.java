package com.muzima.model.builders;

import com.muzima.api.model.Form;
import com.muzima.model.AvailableForm;

public class AvailableFormBuilder{
    private AvailableForm availableForm;

    public AvailableFormBuilder() {
        availableForm = new AvailableForm();
    }

    public AvailableFormBuilder withAvailableForm(Form form) {
        availableForm.setName(form.getName());
        availableForm.setDescription(form.getDescription());
        availableForm.setTags(form.getTags());
        availableForm.setFormUuid(form.getUuid());
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
