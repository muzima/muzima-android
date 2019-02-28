/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.model.builders;

import android.content.Context;

import com.muzima.R;
import com.muzima.api.model.Form;
import com.muzima.api.model.Patient;
import com.muzima.model.FormWithData;
import com.muzima.utils.Constants;

import java.util.Date;
import java.util.UUID;

public abstract class FormWithDataBuilder<B extends FormWithDataBuilder, F extends FormWithData> {
    F formWithData;

    public B withForm(Form completeForm) {
        this.formWithData.setName(completeForm.getName());
        this.formWithData.setDescription(completeForm.getDescription());
        this.formWithData.setDiscriminator(completeForm.getDiscriminator());
        this.formWithData.setFormUuid(completeForm.getUuid());
        return (B) this;
    }

    public B withIndividualObsForm(Form completeForm,Context context){
        if(context != null) {
            this.formWithData.setName(context.getString(R.string.individual_obs));
            this.formWithData.setDescription(context.getString(R.string.individual_obs_description));
        }
        this.formWithData.setDiscriminator(Constants.FORM_JSON_DISCRIMINATOR_INDIVIDUAL_OBS);
        this.formWithData.setFormUuid(UUID.randomUUID().toString());
        return (B) this;
    }

    public B withShrRegistartionForm(Form completeForm,Context context){
        if(context != null) {
            this.formWithData.setName(context.getString(R.string.shr_registration));
            this.formWithData.setDescription(context.getString(R.string.shr_registration_description));
        }
        this.formWithData.setDiscriminator(Constants.FORM_JSON_DISCRIMINATOR_SHR_REGISTRATION);
        this.formWithData.setFormUuid(UUID.randomUUID().toString());
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
        return formWithData;
    }
}
