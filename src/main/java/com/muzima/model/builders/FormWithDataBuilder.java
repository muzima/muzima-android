package com.muzima.model.builders;

import com.muzima.api.model.Form;
import com.muzima.api.model.Patient;
import com.muzima.model.FormWithData;

public abstract class FormWithDataBuilder<B extends FormWithDataBuilder, F extends FormWithData> {
    protected F formWithData;

    public B withForm(Form completeForm) {
        this.formWithData.setName(completeForm.getName());
        this.formWithData.setDescription(completeForm.getDescription());
        this.formWithData.setFormUuid(completeForm.getUuid());
        return (B) this;
    }

    public B withFormDataUuid(String formDataUuid) {
        formWithData.setFormDataUuid(formDataUuid);
        return (B) this;
    }

    public B withLastModifiedData(String date) {
        formWithData.setLastModifiedDate(date);
        return (B) this;
    }

    public B withPatient(Patient patient) {
        formWithData.setPatient(patient);
        return (B) this;
    }

    public F build() {
        return (F) formWithData;
    }
}
