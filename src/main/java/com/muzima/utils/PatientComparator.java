package com.muzima.utils;

import com.muzima.api.model.Patient;

import java.util.Comparator;

public class PatientComparator implements Comparator<Patient> {

    @Override
    public int compare(Patient patient1, Patient patient2) {
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
