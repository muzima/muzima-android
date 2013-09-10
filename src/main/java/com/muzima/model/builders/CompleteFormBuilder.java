package com.muzima.model.builders;

import com.muzima.api.model.Form;
import com.muzima.model.CompleteForm;

public class CompleteFormBuilder {
    private CompleteForm completeForm;

    public CompleteFormBuilder() {
        completeForm = new CompleteForm();
    }

    public CompleteFormBuilder withCompleteForm(Form completeForm) {
        this.completeForm.setName(completeForm.getName());
        this.completeForm.setDescription(completeForm.getDescription());
        this.completeForm.setFormUuid(completeForm.getUuid());
        return this;
    }

    public CompleteFormBuilder withPatientInfo(String familyName, String givenName, String middleName, String identifier){
        completeForm.setPatientFamilyName(familyName);
        completeForm.setPatientGivenName(givenName);
        completeForm.setPatientMiddleName(middleName);
        completeForm.setPatientIdentifier(identifier);
        return this;
    }

    public CompleteFormBuilder withFormDataUuid(String formDataUuid){
        completeForm.setFormDataUuid(formDataUuid);
        return this;
    }

    public CompleteFormBuilder withLastModifiedData(String date){
        completeForm.setLastModifiedDate(date);
        return this;
    }

    public CompleteForm build() {
        return completeForm;
    }
}
