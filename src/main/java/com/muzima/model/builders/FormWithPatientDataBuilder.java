package com.muzima.model.builders;

import com.muzima.model.FormWithPatientData;
import com.muzima.model.PatientMetaData;

public class FormWithPatientDataBuilder<B extends  FormWithPatientDataBuilder, F extends FormWithPatientData>
        extends FormWithDataBuilder<B, F> {

    public B withPatientInfo(String familyName, String givenName, String middleName, String identifier){
        formWithData.setPatientMetaData(new PatientMetaData(familyName, givenName, middleName, identifier));
        return (B)this;
    }
}
