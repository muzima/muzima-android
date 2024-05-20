/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.service;

import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.Concept;
import com.muzima.api.model.ConceptName;
import com.muzima.api.model.ConceptType;
import com.muzima.api.model.DerivedConcept;
import com.muzima.api.model.DerivedObservation;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.EncounterType;
import com.muzima.api.model.Form;
import com.muzima.api.model.Location;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Person;
import com.muzima.api.model.PersonName;
import com.muzima.api.model.Provider;
import com.muzima.controller.ConceptController;
import com.muzima.controller.DerivedConceptController;
import com.muzima.controller.DerivedObservationController;
import com.muzima.controller.FormController;
import com.muzima.controller.ObservationController;
import com.muzima.utils.StringUtils;
import com.muzima.controller.LocationController;
import com.muzima.controller.ProviderController;
import com.muzima.api.service.LocationService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.muzima.util.Constants.CONCEPT_CREATED_ON_PHONE;
import static com.muzima.util.Constants.OBSERVATION_CREATED_ON_PHONE;

class ObservationParserUtility {

    private final ConceptController conceptController;
    private final LocationController locationController;
    private final FormController formController;
    private LocationService locationService;
    private final ProviderController providerController;
    private final List<Concept> newConceptList;
    private final boolean createObservationsForConceptsNotAvailableLocally;
    private final DerivedConceptController derivedConceptController;
    public ObservationParserUtility(MuzimaApplication muzimaApplication, boolean createObservationsForConceptsNotAvailableLocally) {
        this.conceptController = muzimaApplication.getConceptController();
        this.locationController = muzimaApplication.getLocationController();
        this.providerController = muzimaApplication.getProviderController();
        this.formController = muzimaApplication.getFormController();
        this.newConceptList = new ArrayList<>();
        this.createObservationsForConceptsNotAvailableLocally = createObservationsForConceptsNotAvailableLocally;
        this.derivedConceptController = muzimaApplication.getDerivedConceptController();
    }

    public Encounter getEncounterEntity(Date encounterDateTime,String  formUuid,String providerId, int locationId,
                                        String userSystemId, String formDataUuid, Person person, boolean parseAsPersonObs) {
        Encounter encounter = new Encounter();
        encounter.setProvider(getDummyProvider(providerId));
        encounter.setUuid(getEncounterUUID());
        encounter.setLocation(getDummyLocation(locationId));
        encounter.setEncounterType(getDummyEncounterType(formUuid));
        encounter.setEncounterDatetime(encounterDateTime);
        encounter.setUserSystemId(userSystemId);
        encounter.setFormDataUuid(formDataUuid);

        if(parseAsPersonObs){
            encounter.setPersonEncounter(true);
            encounter.setPerson(person);
        } else {
            encounter.setPersonEncounter(false);
            encounter.setPatient((Patient) person);
        }

        return encounter;
    }

    public Concept getConceptEntity(String rawConceptName, boolean isCoded, boolean createConceptIfNotAvailableLocally) throws ConceptController.ConceptFetchException,
            ConceptController.ConceptParseException{
        String conceptIdOrUuid = getConceptId(rawConceptName);
        if(StringUtils.isEmpty(conceptIdOrUuid)){
            throw new ConceptController.ConceptParseException("Could not not get Concept identifier for concept with raw name '"
            + rawConceptName + "'");
        }

        Concept observedConcept;
        boolean isConceptIdNumeric = org.apache.commons.lang.StringUtils.isNumeric(conceptIdOrUuid);

        if(isConceptIdNumeric) {
            int intConceptId = Integer.parseInt(conceptIdOrUuid);
            observedConcept =conceptController.getConceptById(intConceptId);
        } else {
            observedConcept = conceptController.getConceptByUuid(conceptIdOrUuid);
        }

        if (observedConcept == null && createConceptIfNotAvailableLocally == true) {

            String conceptName = getConceptName(rawConceptName);
            if(StringUtils.isEmpty(conceptName)){
                throw new ConceptController.ConceptParseException("Could not not get Concept name for concept with raw name '"
                        + rawConceptName + "'");
            }

            Concept conceptFromExistingList = getConceptFromExistingList(conceptName);
            if (conceptFromExistingList != null) {
                return conceptFromExistingList;
            }

            if(!isConceptIdNumeric){
                observedConcept = buildDummyConcept(0,conceptIdOrUuid, conceptName, isCoded);
            } else {
                int intConceptId = Integer.parseInt(conceptIdOrUuid);
                if (intConceptId > 0) {
                    observedConcept = buildDummyConcept(intConceptId,null, conceptName, isCoded);
                } else {
                    observedConcept = buildDummyConcept(conceptName, isCoded);
                }
            }
            newConceptList.add(observedConcept);
        }
        return observedConcept;
    }

    public Observation getObservationEntity(Concept concept, String value) throws ConceptController.ConceptFetchException,
        ConceptController.ConceptParseException, ObservationController.ParseObservationException{
        if (concept == null) {
            throw new ObservationController.ParseObservationException("Could not create Observation entity." +
                    " Reason: No Concept provided.");
        }
        if (StringUtils.isEmpty(value)) {
            throw new ObservationController.ParseObservationException("Could not create Observation entity for concept '"
                    + concept.getName() + "'. Reason: No Observation value provided.");
        }
        Observation observation = new Observation();
        observation.setUuid(getObservationUuid());
        observation.setValueCoded(defaultValueCodedConcept());

        if (concept.isCoded()) {
            try {
                Concept valueCoded = getConceptEntity(value,false, true);
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
        observation.setConcept(concept);
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

    private EncounterType getDummyEncounterType(String formUuid) {
        String encounterTypeName= "encounterType";
        String encountertypeUuid="encounterTypeForObservationsCreatedOnPhone";
        try{
            Form form = formController.getFormByUuid(formUuid);
            if(form != null){
                encounterTypeName = form.getEncounterType().getName();
                encountertypeUuid = form.getEncounterType().getUuid();
            }
        } catch (FormController.FormFetchException | NullPointerException e) {
            Log.e(getClass().getSimpleName(),"Could not retrieve list of forms",e);
        }
        EncounterType encounterType = new EncounterType();
        encounterType.setUuid(encountertypeUuid);
        encounterType.setName(encounterTypeName);
        return encounterType;
    }

    private Location getDummyLocation(int locationId) {
        List<Location> allLocations = new ArrayList<>();
        try {
            allLocations = locationController.getAllLocations();
        } catch (LocationController.LocationLoadException e) {
            Log.e(getClass().getSimpleName(),"Error retrieving locations ",e);
        } catch (NullPointerException e){
            Log.e(getClass().getSimpleName(),"Could not retrieve list of locations",e);
        }
        String locationName = "";
        String locationUuid = "";
        for(Location loc:allLocations) {
            if(loc.getId()==locationId){
                locationName=loc.getName();
                locationUuid=loc.getUuid();
            }
        }
        Location dummyLocation = new Location();
        dummyLocation.setUuid(locationUuid);
        dummyLocation.setName(locationName);
        return dummyLocation;
    }

    private Person getDummyProvider(String providerId) {
        List<Provider> allProviders = new ArrayList<>();
        try {
            allProviders = providerController.getAllProviders();
        } catch (ProviderController.ProviderLoadException | NullPointerException e) {
            Log.e(getClass().getSimpleName(),"Could not retrieve list of providers",e);
        }
        String providerName = "";
        String providerUuid = "";
        String providerIdentifier = "";
        for(Provider prov:allProviders){
            String identifier = prov.getIdentifier();
            if(identifier != null && identifier.equals(providerId)){
                providerName = prov.getName();
                providerUuid = prov.getUuid();
                providerIdentifier = prov.getIdentifier();
            }
        }
        if(StringUtils.isEmpty(providerUuid)){
            providerUuid = "providerForObservationsCreatedOnPhone";
        }

        if(StringUtils.isEmpty(providerIdentifier)){
            providerIdentifier = "PICOP";//User visible identifier code for Provider Identifier Created On Phone
        }
        Person provider = new Person();
        provider.setUuid(providerUuid);
        provider.setGender("NA");
        PersonName personName = new PersonName();
        personName.setFamilyName(providerName);
        personName.setGivenName(" ");
        personName.setMiddleName(providerIdentifier);
        personName.setPreferred(true);
        ArrayList<PersonName> names = new ArrayList<>();
        names.add(personName);
        provider.setNames(names);
        return provider;
    }

    private Concept buildDummyConcept(String conceptName,boolean isCoded) {
        return buildDummyConcept(0,null, conceptName, isCoded);
    }

    private Concept buildDummyConcept(int conceptId,String conceptUuid, String conceptName, boolean isCoded) {
        Concept concept = new Concept();
        ConceptName dummyConceptName = new ConceptName();
        dummyConceptName.setName(conceptName);
        dummyConceptName.setPreferred(true);
        concept.setConceptNames(Collections.singletonList(dummyConceptName));
        ConceptType conceptType = new ConceptType();
        if(isCoded) {
            conceptType.setName("Coded");
        } else {
            conceptType.setName("ConceptTypeCreatedOnThePhone");
        }
        concept.setConceptType(conceptType);
        if(conceptId > 0) {
            concept.setId(conceptId);
        }
        if(conceptUuid != null){
            concept.setUuid(conceptUuid);
        } else {
            concept.setUuid(CONCEPT_CREATED_ON_PHONE + UUID.randomUUID());
        }
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

    private static String getConceptId(String peek) {
        if (!StringUtils.isEmpty(peek) && peek.split("\\^").length > 1) {
            return peek.split("\\^")[0];
        }
        return "";
    }

    public static boolean isFormattedAsConcept(String peek) {
        return !StringUtils.isEmpty(peek) && peek.split("\\^").length > 1;
    }

    private String getEncounterUUID() {
        return "encounterUuid" + UUID.randomUUID();
    }

    public String getObservationUuid() {
        return OBSERVATION_CREATED_ON_PHONE + UUID.randomUUID();
    }

    public DerivedConcept getDerivedConceptEntity(String rawConceptName) throws DerivedConceptController.DerivedConceptFetchException, DerivedConceptController.DerivedConceptParseException {
        String conceptIdOrUuid = getConceptId(rawConceptName);
        if(StringUtils.isEmpty(conceptIdOrUuid)){
            throw new DerivedConceptController.DerivedConceptParseException("Could not not get Derived Concept identifier for concept with raw name '"
                    + rawConceptName + "'");
        }

        DerivedConcept observedDerivedConcept;
        boolean isConceptIdNumeric = org.apache.commons.lang.StringUtils.isNumeric(conceptIdOrUuid);

        if(isConceptIdNumeric) {
            int intConceptId = Integer.parseInt(conceptIdOrUuid);
            observedDerivedConcept =derivedConceptController.getDerivedConceptById(intConceptId);
        } else {
            observedDerivedConcept = derivedConceptController.getDerivedConceptByUuid(conceptIdOrUuid);
        }

        return observedDerivedConcept;
    }

    public DerivedObservation getDerivedObservationEntity(DerivedConcept derivedConcept, String value) throws DerivedObservationController.ParseDerivedObservationException, ConceptController.ConceptParseException, ConceptController.ConceptFetchException {
        if (derivedConcept == null) {
            throw new DerivedObservationController.ParseDerivedObservationException("Could not create Observation entity." +
                    " Reason: No Concept provided.");
        }
        if (StringUtils.isEmpty(value)) {
            throw new DerivedObservationController.ParseDerivedObservationException("Could not create Observation entity for concept '"
                    + derivedConcept.getDerivedConceptName() + "'. Reason: No Observation value provided.");
        }
        DerivedObservation derivedObservation = new DerivedObservation();
        derivedObservation.setUuid(getObservationUuid());
        derivedObservation.setValueCoded(defaultValueCodedConcept());

        if (derivedConcept.isCoded()) {
            try {
                Concept valueCoded = getConceptEntity(value,false, true);
                derivedObservation.setValueCoded(valueCoded);
            } catch (ConceptController.ConceptParseException e) {
                throw new ConceptController.ConceptParseException("Could not get value for coded concept '"
                        + derivedConcept.getDerivedConceptName() + "', from provided value '" + value + "'");
            }

        } else if (derivedConcept.isNumeric()) {
            derivedObservation.setValueNumeric(getDoubleValue(value));
        } else {
            derivedObservation.setValueText(value);
        }
        derivedObservation.setDerivedConcept(derivedConcept);
        return derivedObservation;
    }
}

