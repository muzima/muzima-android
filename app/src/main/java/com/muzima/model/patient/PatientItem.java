/*
 * Copyright (c) Vanderbilt University Medical Center and Lambda Informatics.
 * All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.model.patient;

import com.muzima.api.model.Patient;

public class PatientItem {
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
