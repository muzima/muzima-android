package com.muzima.model.builders;

import com.muzima.api.model.Form;
import com.muzima.model.CompleteForm;
import com.muzima.model.CompletePatientForm;

public class CompletePatientFormBuilder {
    private CompletePatientForm completePatientForm;

    public CompletePatientFormBuilder() {
        completePatientForm = new CompletePatientForm();
    }

    public CompletePatientFormBuilder withCompleteForm(Form completeForm) {
        this.completePatientForm.setName(completeForm.getName());
        this.completePatientForm.setDescription(completeForm.getDescription());
        this.completePatientForm.setFormUuid(completeForm.getUuid());
        return this;
    }


    public CompletePatientForm build() {
        return completePatientForm;
    }
}
