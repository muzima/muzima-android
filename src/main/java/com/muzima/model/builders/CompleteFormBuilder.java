package com.muzima.model.builders;

import com.muzima.model.CompleteForm;

public class CompleteFormBuilder extends FormWithDataBuilder<CompleteFormBuilder, CompleteForm> {
    public CompleteFormBuilder() {
        formWithData = new CompleteForm();
    }

    public CompleteFormBuilder withPatientInfo(String familyName, String givenName, String middleName, String identifier){
        formWithData.setPatientFamilyName(familyName);
        formWithData.setPatientGivenName(givenName);
        formWithData.setPatientMiddleName(middleName);
        formWithData.setPatientIdentifier(identifier);
        return this;
    }
}
