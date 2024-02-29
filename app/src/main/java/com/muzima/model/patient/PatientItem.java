package com.muzima.model.patient;

import com.muzima.api.model.Patient;

import java.io.Serializable;

public class PatientItem implements Serializable {
    private Patient patient;
    private boolean selected;

    public PatientItem(Patient patient, boolean selected){
        this.patient = patient;
        this.selected = selected;
    }

    public PatientItem(Patient patient){
        this.patient = patient;
        selected = false;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }
}
