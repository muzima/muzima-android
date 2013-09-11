package com.muzima.model;

import org.junit.Test;

import static junit.framework.Assert.assertTrue;

public class PatientMetaDataTest {
    @Test
    public void compareTo_shouldSortByFamilyName() throws Exception {
        PatientMetaData patient1MetaData = new PatientMetaData("Obama", "Barack", "Hussein", "id1");
        PatientMetaData patient2MetaData = new PatientMetaData("Bush", "George", "W", "id2");

        assertTrue(patient1MetaData.compareTo(patient2MetaData) > 0);
    }

    @Test
    public void compareTo_shouldSortByGivenNameIfFamilyNameIsSame() throws Exception {
        PatientMetaData patient1MetaData = new PatientMetaData("Obama", "Barack", "Hussein", "id1");
        PatientMetaData patient2MetaData = new PatientMetaData("Obama", "George", "W", "id2");

        assertTrue(patient1MetaData.compareTo(patient2MetaData) < 0);
    }

    @Test
    public void compareTo_shouldSortByMiddleNameIfGivenNameAndFamilyNameAreSame() throws Exception {
        PatientMetaData patient1MetaData = new PatientMetaData("Obama", "Barack", "Hussein", "id1");
        PatientMetaData patient2MetaData = new PatientMetaData("Obama", "Barack", "W", "id2");

        assertTrue(patient1MetaData.compareTo(patient2MetaData) < 0);
    }
}
