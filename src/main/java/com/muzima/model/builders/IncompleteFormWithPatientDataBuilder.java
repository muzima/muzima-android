package com.muzima.model.builders;

import com.muzima.model.IncompleteFormWithPatientData;

public class IncompleteFormWithPatientDataBuilder extends FormWithDataBuilder<IncompleteFormWithPatientDataBuilder, IncompleteFormWithPatientData> {
    public IncompleteFormWithPatientDataBuilder() {
        formWithData = new IncompleteFormWithPatientData();
    }
}
