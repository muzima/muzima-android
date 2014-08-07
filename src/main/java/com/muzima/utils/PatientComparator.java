/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.utils;

import com.muzima.api.model.Patient;

import java.util.Comparator;

public class PatientComparator implements Comparator<Patient> {

    @Override
    public int compare(Patient patient1, Patient patient2) {
        if (patient1 == null) {
            patient1 = new Patient();
        }
        if (patient2 == null) {
            patient2 = new Patient();
        }
        int familyNameCompareResult = patient1.getFamilyName().compareTo(patient2.getFamilyName());
        if (familyNameCompareResult != 0) {
            return familyNameCompareResult;
        }
        int givenNameCompareResult = patient1.getGivenName().compareTo(patient2.getGivenName());
        if (givenNameCompareResult != 0) {
            return givenNameCompareResult;
        }
        return patient1.getMiddleName().compareTo(patient2.getMiddleName());
    }

}
