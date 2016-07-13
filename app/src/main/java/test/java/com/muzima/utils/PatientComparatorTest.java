/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.utils;

import com.muzima.api.model.Patient;
import com.muzima.api.model.PatientIdentifier;
import com.muzima.api.model.PersonName;
import org.junit.Before;
import org.junit.Test;

import static java.util.Arrays.asList;
import static junit.framework.Assert.assertTrue;

public class PatientComparatorTest {


    private PatientComparator patientComparator;

    @Before
    public void setUp() throws Exception {
        patientComparator = new PatientComparator();
    }

    @Test
    public void shouldSortByFamilyName() throws Exception {
        Patient obama = patient("Obama", "Barack", "Hussein", "id1");
        Patient bush = patient("Bush", "George", "W", "id2");
        assertTrue(patientComparator.compare(obama, bush) > 0);
    }

    @Test
    public void shouldSortByGivenNameIfFamilyNameIsSame() throws Exception {
        Patient barack = patient("Obama", "Barack", "Hussein", "id1");
        Patient george = patient("Obama", "George", "W", "id2");
        assertTrue(patientComparator.compare(barack, george) < 0);
    }

    @Test
    public void shouldSortByMiddleNameIfGivenNameAndFamilyNameAreSame() throws Exception {
        Patient hussein = patient("Obama", "Barack", "Hussein", "id1");
        Patient william = patient("Obama", "Barack", "William", "id2");
        assertTrue(patientComparator.compare(hussein, william) < 0);
    }

    private Patient patient(String familyName, String middleName, String givenName, String identifier) {
        Patient patient = new Patient();
        PersonName personName = new PersonName();
        personName.setFamilyName(familyName);
        personName.setMiddleName(middleName);
        personName.setGivenName(givenName);
        patient.setNames(asList(personName));
        PatientIdentifier personId = new PatientIdentifier();
        personId.setIdentifier(identifier);
        patient.setIdentifiers(asList(personId));
        return patient;
    }
}
