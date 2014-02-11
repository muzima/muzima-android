package com.muzima.service;

import com.muzima.api.model.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

public class ObservationParserUtility {

    private static ObservationParserUtility OBSERVATION_PARSER_UTILITY;

    private ObservationParserUtility() {
    }

    public static ObservationParserUtility getInstance() {
        if (OBSERVATION_PARSER_UTILITY == null) {
            OBSERVATION_PARSER_UTILITY = new ObservationParserUtility();
        }
        return OBSERVATION_PARSER_UTILITY;
    }

    public Encounter getEncounterEntity(Date encounterDateTime) {
        Encounter encounter = new Encounter();
        EncounterType encounterType = new EncounterType();
        encounter.setEncounterType(encounterType);
        encounterType.setName(getEncounterName());
        encounter.setProvider(getDummyProvider());
        encounter.setUuid(getEncounterUUID());
        encounter.setLocation(getDummyLocation());
        encounter.setEncounterType(getDummyEncounterType());
        encounter.setEncounterDatetime(encounterDateTime);
        return encounter;
    }

    public EncounterType getDummyEncounterType() {
        EncounterType encounterType = new EncounterType();
        encounterType.setUuid("encounterTypeForObservationsCreatedOnPhone");
        encounterType.setName("encounterTypeForObservationsCreatedOnPhone");
        return encounterType;
    }

    public Location getDummyLocation() {
        Location dummyLocation = new Location();
        dummyLocation.setUuid("locationForObservationsCreatedOnPhone");
        dummyLocation.setName("Created On Phone");
        return dummyLocation;
    }

    public Person getDummyProvider() {
        Person provider = new Person();
        provider.setUuid("providerForObservationsCreatedOnPhone");
        provider.setGender("NA");
        PersonName personName = new PersonName();
        personName.setFamilyName("Taken");
        personName.setGivenName(" on");
        personName.setMiddleName("phone");
        personName.setPreferred(true);
        ArrayList<PersonName> names = new ArrayList<PersonName>();
        names.add(personName);
        provider.setNames(names);
        return provider;
    }


    public String getEncounterUUID() {
        return "encounterUuid" + UUID.randomUUID();
    }

    public String getEncounterName() {
        return "EncounterCreatedOnDevice";
    }


}

