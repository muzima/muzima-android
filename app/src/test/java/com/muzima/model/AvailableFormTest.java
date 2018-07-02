/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.model;

import com.muzima.api.model.Tag;
import com.muzima.utils.Constants;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AvailableFormTest {
    String[] registrationDiscriminators = {Constants.FORM_DISCRIMINATOR_REGISTRATION,
            Constants.FORM_JSON_DISCRIMINATOR_REGISTRATION,
            Constants.FORM_JSON_DISCRIMINATOR_GENERIC_REGISTRATION,
            Constants.FORM_JSON_DISCRIMINATOR_SHR_REGISTRATION};

    String[] nonRegistrationDiscriminators = {Constants.FORM_DISCRIMINATOR_CONSULTATION,
            Constants.FORM_JSON_DISCRIMINATOR_CONSULTATION,
            Constants.FORM_JSON_DISCRIMINATOR_DEMOGRAPHICS_UPDATE, Constants.FORM_JSON_DISCRIMINATOR_ENCOUNTER,
            Constants.FORM_XML_DISCRIMINATOR_ENCOUNTER, Constants.FORM_JSON_DISCRIMINATOR_SHR_DEMOGRAPHICS_UPDATE,
            Constants.FORM_JSON_DISCRIMINATOR_SHR_ENCOUNTER};
    @Test
    public void shouldReturnFalseIfHasNoRegistrationDiscriminator() throws Exception {
        AvailableForm availableForm = new AvailableForm();

        assertFalse(availableForm.isRegistrationForm());

        for(String discriminator: nonRegistrationDiscriminators) {
            availableForm.setDiscriminator(discriminator);
            assertFalse(availableForm.isRegistrationForm());
        }
    }

    @Test
    public void shouldReturnTrueIfHasRegistrationDiscriminator() throws Exception {
        AvailableForm availableForm = new AvailableForm();
        for(String discriminator: registrationDiscriminators) {
            availableForm.setDiscriminator(discriminator);
            assertTrue(availableForm.isRegistrationForm());
        }
    }
}
