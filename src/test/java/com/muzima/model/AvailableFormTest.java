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
