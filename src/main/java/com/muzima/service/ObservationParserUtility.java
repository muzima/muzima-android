/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.service;

import android.util.Log;
import com.muzima.api.model.Concept;
import com.muzima.api.model.ConceptName;
import com.muzima.api.model.ConceptType;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.EncounterType;
import com.muzima.api.model.Location;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Person;
import com.muzima.api.model.PersonName;
import com.muzima.controller.ConceptController;
import com.muzima.controller.ObservationController;
import com.muzima.search.api.util.StringUtil;
import com.muzima.utils.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.muzima.util.Constants.CONCEPT_CREATED_ON_PHONE;
import static com.muzima.util.Constants.OBSERVATION_CREATED_ON_PHONE;
import static java.util.Arrays.asList;

public class ObservationParserUtility {

    private ConceptController conceptController;
    private List<Concept> newConceptList;

    public ObservationParserUtility(ConceptController conceptController) {
        this.conceptController = conceptController;
        this.newConceptList = new ArrayList<Concept>();
    }

    public Encounter getEncounterEntity(Date encounterDateTime, Patient patient, String formDataUuid) {
        Encounter encounter = new Encounter();
        encounter.setProvider(getDummyProvider());
        encounter.setUuid(getEncounterUUID());
        encounter.setLocation(getDummyLocation());
        encounter.setEncounterType(getDummyEncounterType());
        encounter.setEncounterDatetime(encounterDateTime);
        encounter.setFormDataUuid(formDataUuid);
        encounter.setPatient(patient);
        return encounter;
    }

    public Concept getConceptEntity(String rawConceptName) throws ConceptController.ConceptFetchException,
            ConceptController.ConceptParseException{
        String conceptName = getConceptName(rawConceptName);
        if(StringUtil.isEmpty(conceptName)){
            throw new ConceptController.ConceptParseException("Could not not get Concept name for concept with raw name '"
            + rawConceptName + "'");
        }
        Concept conceptFromExistingList = getConceptFromExistingList(conceptName);
        if (conceptFromExistingList != null) {
            return conceptFromExistingList;
        }
        Concept observedConcept = conceptController.getConceptByName(conceptName);
        if (observedConcept == null) {
            observedConcept = buildDummyConcept(conceptName);
            newConceptList.add(observedConcept);
        }
        return observedConcept;
    }

    public Observation getObservationEntity(Concept concept, String value) throws ConceptController.ConceptFetchException,
        ConceptController.ConceptParseException, ObservationController.ParseObservationException{
        if (StringUtil.isEmpty(value)) {
            throw new ObservationController.ParseObservationException("Could not create Observation entity for concept '"
                    + concept.getName() + "'. Reason: No Observation value provided.");
        }
        Observation observation = new Observation();
        observation.setUuid(getObservationUuid());
        observation.setConcept(concept);
        observation.setValueCoded(defaultValueCodedConcept());
        if (concept.isCoded()) {
            try {
                Concept valueCoded = getConceptEntity(value);
                observation.setValueCoded(valueCoded);
            } catch (ConceptController.ConceptParseException e) {
                throw new ConceptController.ConceptParseException("Could not get value for coded concept '"
                        + concept.getName() + "', from provided value '" + value + "'");
            }

        } else if (concept.isNumeric()) {
            observation.setValueNumeric(getDoubleValue(value));
        } else {
            observation.setValueText(value);
        }
        return observation;
    }

    public List<Concept> getNewConceptList() {
        return newConceptList;
    }

    private double getDoubleValue(String value) {
        double valueNumeric = Double.parseDouble(value);
        BigDecimal bigDecimal = new BigDecimal(valueNumeric);
        bigDecimal = bigDecimal.setScale(2, RoundingMode.HALF_UP);
        return bigDecimal.doubleValue();
    }

    private Concept defaultValueCodedConcept() {
        Concept valueCoded = new Concept();
        valueCoded.setConceptType(new ConceptType());
        return valueCoded;
    }

    private EncounterType getDummyEncounterType() {
        EncounterType encounterType = new EncounterType();
        encounterType.setUuid("encounterTypeForObservationsCreatedOnPhone");
        encounterType.setName("encounterTypeForObservationsCreatedOnPhone");
        return encounterType;
    }

    private Location getDummyLocation() {
        Location dummyLocation = new Location();
        dummyLocation.setUuid("locationForObservationsCreatedOnPhone");
        dummyLocation.setName("Created On Phone");
        return dummyLocation;
    }

    private Person getDummyProvider() {
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

    private Concept buildDummyConcept(String conceptName) {
        Concept concept = new Concept();
        concept.setUuid(CONCEPT_CREATED_ON_PHONE + UUID.randomUUID());
        ConceptName dummyConceptName = new ConceptName();
        dummyConceptName.setName(conceptName);
        dummyConceptName.setPreferred(true);
        concept.setConceptNames(asList(dummyConceptName));
        ConceptType conceptType = new ConceptType();
        conceptType.setName("ConceptTypeCreatedOnThePhone");
        concept.setConceptType(conceptType);
        return concept;
    }

    private Concept getConceptFromExistingList(String conceptName) {
        for (Concept concept : newConceptList) {
            if (conceptName.equals(concept.getName())) {
                return concept;
            }
        }
        return null;
    }

    private static String getConceptName(String peek) {
        if (!StringUtils.isEmpty(peek) && peek.split("\\^").length > 1) {
            return peek.split("\\^")[1];
        }
        return "";
    }

    private String getEncounterUUID() {
        return "encounterUuid" + UUID.randomUUID();
    }

    public String getObservationUuid() {
        return OBSERVATION_CREATED_ON_PHONE + UUID.randomUUID();
    }
}

