package com.muzima.model;

public class FormWithPatientData extends FormWithData {
    private PatientMetaData patientMetaData;

    public PatientMetaData getPatientMetaData() {
        return patientMetaData;
    }

    public void setPatientMetaData(PatientMetaData patientMetaData) {
        this.patientMetaData = patientMetaData;
    }
}
