package com.muzima.builder;

import com.muzima.api.model.Patient;
import com.muzima.api.model.PatientIdentifier;
import com.muzima.api.model.PersonName;

import java.util.Date;

import static java.util.Arrays.asList;

public class PatientBuilder {
    private String identifier;
    private Date birthDate;
    private String familyName;
    private String givenName;
    private String middleName;
    private String sex;

    public static PatientBuilder patient() {
        return new PatientBuilder();
    }

    public PatientBuilder withIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    public PatientBuilder withBirthdate(Date birthDate) {
        this.birthDate = birthDate;
        return this;
    }

    public PatientBuilder withFamilyName(String familyName) {
        this.familyName = familyName;
        return this;
    }

    public PatientBuilder withGivenName(String givenName) {
        this.givenName = givenName;
        return this;
    }

    public PatientBuilder withMiddleName(String middleName) {
        this.middleName = middleName;
        return this;
    }

    public PatientBuilder withSex(String sex) {
        this.sex = sex;
        return this;
    }

    public Patient instance() {
        PatientIdentifier patientIdentifier = new PatientIdentifier();
        patientIdentifier.setIdentifier(identifier);
        patientIdentifier.setPreferred(true);
        Patient patient = new Patient();
        patient.addIdentifier(patientIdentifier);
        patient.setBirthdate(birthDate);
        PersonName personName = new PersonName();
        personName.setFamilyName(familyName);
        personName.setGivenName(givenName);
        personName.setMiddleName(middleName);
        personName.setPreferred(true);
        patient.setNames(asList(personName));
        patient.setGender(sex);
        return patient;
    }
}
