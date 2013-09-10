package com.muzima.model.builders;

import com.muzima.model.FormWithPatientData;

public class FormWithPatientDataBuilder<B extends  FormWithPatientDataBuilder, F extends FormWithPatientData>
        extends FormWithDataBuilder<B, F> {

    public B withPatientInfo(String familyName, String givenName, String middleName, String identifier){
        formWithData.setPatientFamilyName(familyName);
        formWithData.setPatientGivenName(givenName);
        formWithData.setPatientMiddleName(middleName);
        formWithData.setPatientIdentifier(identifier);
        return (B)this;
    }
}
