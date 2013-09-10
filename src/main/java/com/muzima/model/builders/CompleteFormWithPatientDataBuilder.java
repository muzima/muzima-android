package com.muzima.model.builders;

import com.muzima.model.CompleteFormWithPatientData;

public class CompleteFormWithPatientDataBuilder extends FormWithPatientDataBuilder<CompleteFormWithPatientDataBuilder, CompleteFormWithPatientData> {
    public CompleteFormWithPatientDataBuilder() {
        formWithData = new CompleteFormWithPatientData();
    }
}
