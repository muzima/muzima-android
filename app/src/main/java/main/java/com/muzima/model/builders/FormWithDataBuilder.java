/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.model.builders;

import com.muzima.api.model.Form;
import com.muzima.api.model.Patient;
import com.muzima.model.FormWithData;

import java.util.Date;

public abstract class FormWithDataBuilder<B extends FormWithDataBuilder, F extends FormWithData> {
    protected F formWithData;

    public B withForm(Form completeForm) {
        this.formWithData.setName(completeForm.getName());
        this.formWithData.setDescription(completeForm.getDescription());
        this.formWithData.setFormUuid(completeForm.getUuid());
        return (B) this;
    }

    public B withFormDataUuid(String formDataUuid) {
        formWithData.setFormDataUuid(formDataUuid);
        return (B) this;
    }

    public B withLastModifiedDate(Date date) {
        formWithData.setLastModifiedDate(date);
        return (B) this;
    }

    public B withPatient(Patient patient) {
        formWithData.setPatient(patient);
        return (B) this;
    }

    public B withEncounterDate(Date encounterDate){
        formWithData.setEncounterDate(encounterDate);
        return (B) this;
    }

    public F build() {
        return (F) formWithData;
    }
}
