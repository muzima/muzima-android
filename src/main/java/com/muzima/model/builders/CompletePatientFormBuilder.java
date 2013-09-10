package com.muzima.model.builders;

import com.muzima.model.CompletePatientForm;

public class CompletePatientFormBuilder extends FormWithDataBuilder<CompletePatientFormBuilder, CompletePatientForm>{

    public CompletePatientFormBuilder() {
        formWithData = new CompletePatientForm();
    }
}
