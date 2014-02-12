package com.muzima.service;

import com.muzima.api.model.*;
import com.muzima.controller.ConceptController;
import com.muzima.search.api.util.StringUtil;
import com.muzima.utils.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import static java.util.Arrays.asList;

public class ObservationParserUtility {

    private static ObservationParserUtility OBSERVATION_PARSER_UTILITY;
    public static String OBSERVATION_ON_PHONE_UUID_PREFIX = "observationFromPhoneUuid";

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

    public Observation createObservation(String rawConceptName, String value, ConceptController conceptController) throws ConceptController.ConceptFetchException, ConceptController.ConceptSaveException {
        if(StringUtil.isEmpty(value)) {
            return null;
        }
        Concept concept = buildConcept(rawConceptName, conceptController, true);

        Observation observation = new Observation();
        observation.setConcept(concept);

        // Default value
        Concept valueCoded = new Concept();
        valueCoded.setConceptType(new ConceptType());
        observation.setValueCoded(valueCoded);
        if(concept.isCoded()){
            Concept observedConcept = buildConcept(value, conceptController, false);
            observation.setValueCoded(observedConcept);
        } else if(concept.isNumeric())
        {
            double valueNumeric = Double.parseDouble(value);
            BigDecimal bigDecimal = new BigDecimal(valueNumeric);
            bigDecimal = bigDecimal.setScale(2, RoundingMode.HALF_UP);

            observation.setValueNumeric(bigDecimal.doubleValue());
        } else {
            observation.setValueText(value);
        }
        return observation;
    }

    private Concept buildConcept(String conceptValue, ConceptController conceptController, boolean isNewConcept) throws ConceptController.ConceptFetchException, ConceptController.ConceptSaveException {
        String observedConceptName = getConceptName(conceptValue);
        Concept observedConcept = conceptController.getConceptByName(observedConceptName);
        if(observedConcept == null){
            observedConcept = buildDummyConcept(observedConceptName);
            if (isNewConcept) {
                conceptController.saveConcepts(asList(observedConcept));
            }
        }
        return observedConcept;
    }

    private Concept buildDummyConcept(String conceptName) {
        Concept concept;
        concept = new Concept();
        ConceptName dummyConceptName = new ConceptName();
        dummyConceptName.setName(conceptName);
        dummyConceptName.setPreferred(true);
        concept.setConceptNames(asList(dummyConceptName));
        concept.setConceptType(new ConceptType());
        return concept;
    }

    private static String getConceptName(String peek) {
        if (!StringUtils.isEmpty(peek) && peek.split("\\^").length > 1) {
            return peek.split("\\^")[1];
        }
        return "";
    }

    public String getEncounterUUID() {
        return "encounterUuid" + UUID.randomUUID();
    }

    public String getEncounterName() {
        return "EncounterCreatedOnDevice";
    }
}

