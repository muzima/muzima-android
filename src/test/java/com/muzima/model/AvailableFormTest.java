/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.model;

import com.muzima.api.model.Tag;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AvailableFormTest {
    @Test
    public void shouldReturnFalseIfThereAreNoTags() throws Exception {
        AvailableForm availableForm = new AvailableForm();
        assertFalse(availableForm.isRegistrationForm());
    }

    @Test
    public void shouldReturnTrueIfThereIsARegistrationTag() throws Exception {
        AvailableForm availableForm = new AvailableForm();
        availableForm.setTags(new Tag[]{new Tag(){{setName("Registration");}}});
        assertTrue(availableForm.isRegistrationForm());
    }
}
