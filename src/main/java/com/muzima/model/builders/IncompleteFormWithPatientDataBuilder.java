package com.muzima.model.builders;

import com.muzima.model.IncompleteFormWithPatientData;

public class IncompleteFormWithPatientDataBuilder extends FormWithPatientDataBuilder<IncompleteFormWithPatientDataBuilder, IncompleteFormWithPatientData> {
    public IncompleteFormWithPatientDataBuilder() {
        formWithData = new IncompleteFormWithPatientData();
    }
}
