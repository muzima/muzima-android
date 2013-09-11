package com.muzima.model;

public class PatientMetaData implements Comparable{
    private String patientFamilyName;
    private String patientGivenName;
    private String patientMiddleName;
    private String patientIdentifier;

    public PatientMetaData(String patientFamilyName, String patientGivenName, String patientMiddleName, String patientIdentifier) {
        this.patientFamilyName = patientFamilyName;
        this.patientGivenName = patientGivenName;
        this.patientMiddleName = patientMiddleName;
        this.patientIdentifier = patientIdentifier;
    }

    public String getPatientFamilyName() {
        return patientFamilyName;
    }

    public void setPatientFamilyName(String patientFamilyName) {
        this.patientFamilyName = patientFamilyName;
    }

    public String getPatientGivenName() {
        return patientGivenName;
    }

    public void setPatientGivenName(String patientGivenName) {
        this.patientGivenName = patientGivenName;
    }

    public String getPatientMiddleName() {
        return patientMiddleName;
    }

    public void setPatientMiddleName(String patientMiddleName) {
        this.patientMiddleName = patientMiddleName;
    }

    public String getPatientIdentifier() {
        return patientIdentifier;
    }

    public void setPatientIdentifier(String patientIdentifier) {
        this.patientIdentifier = patientIdentifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PatientMetaData that = (PatientMetaData) o;

        if (patientIdentifier != null ? !patientIdentifier.equals(that.patientIdentifier) : that.patientIdentifier != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return patientIdentifier != null ? patientIdentifier.hashCode() : 0;
    }

    @Override
    public int compareTo(Object another) {
        PatientMetaData anotherPatient = (PatientMetaData) another;
        int familyNameCompareResult = patientFamilyName.compareTo(anotherPatient.getPatientFamilyName());
        if (familyNameCompareResult != 0) {
            return familyNameCompareResult;
        }

        int givenNameCompareResult = patientGivenName.compareTo(anotherPatient.getPatientGivenName());
        if(givenNameCompareResult !=0){
            return givenNameCompareResult;
        }

        return patientMiddleName.compareTo(anotherPatient.getPatientMiddleName());
    }

    public CharSequence getDisplayName() {
        return patientFamilyName + ", " + patientGivenName + " " + patientMiddleName;
    }
}
