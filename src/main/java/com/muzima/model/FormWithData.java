package com.muzima.model;

import com.muzima.api.model.Patient;

public abstract class FormWithData extends BaseForm {
    private String formDataUuid;
    private String lastModifiedDate;
    private Patient patient;

    public String getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(String lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getFormDataUuid() {
        return formDataUuid;
    }

    public void setFormDataUuid(String formDataUuid) {
        this.formDataUuid = formDataUuid;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }
}
