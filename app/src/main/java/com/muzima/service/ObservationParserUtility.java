/*
 * Copyright (c) 2014 - 2017. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.service;

import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.Concept;
import com.muzima.api.model.ConceptName;
import com.muzima.api.model.ConceptType;
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
import com.muzima.controller.FormController;
import com.muzima.controller.ObservationController;
import com.muzima.utils.StringUtils;
import com.muzima.controller.LocationController;
import com.muzima.controller.ProviderController;
import com.muzima.api.service.LocationService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.muzima.util.Constants.CONCEPT_CREATED_ON_PHONE;
import static com.muzima.util.Constants.OBSERVATION_CREATED_ON_PHONE;
import static java.util.Arrays.asList;

public class ObservationParserUtility {

    private ConceptController conceptController;
    public LocationController locationController;
    public FormController formController;
    private LocationService locationService;
    public ProviderController providerController;
    private List<Concept> newConceptList;

    public ObservationParserUtility(MuzimaApplication muzimaApplication) {
        this.conceptController = muzimaApplication.getConceptController();
        this.locationController = muzimaApplication.getLocationController();
        this.providerController = muzimaApplication.getProviderController();
        this.formController = muzimaApplication.getFormController();
        this.newConceptList = new ArrayList<Concept>();
    }

    public Encounter getEncounterEntity(Date encounterDateTime,String  formUuid,String providerId, int locationId, String userSystemId, Patient patient, String formDataUuid) {
        Encounter encounter = new Encounter();
        encounter.setProvider(getDummyProvider(providerId));
        encounter.setUuid(getEncounterUUID());
        encounter.setLocation(getDummyLocation(locationId));
        encounter.setEncounterType(getDummyEncounterType(formUuid));
        encounter.setEncounterDatetime(encounterDateTime);
        encounter.setUserSystemId(userSystemId);
        encounter.setFormDataUuid(formDataUuid);
        encounter.setPatient(patient);

        return encounter;
    }

    public Concept getConceptEntity(String rawConceptName, boolean isCoded) throws ConceptController.ConceptFetchException,
            ConceptController.ConceptParseException{
        String conceptName = getConceptName(rawConceptName);
        if(StringUtils.isEmpty(conceptName)){
            throw new ConceptController.ConceptParseException("Could not not get Concept name for concept with raw name '"
            + rawConceptName + "'");
        }
        Concept conceptFromExistingList = getConceptFromExistingList(conceptName);
        if (conceptFromExistingList != null) {
            return conceptFromExistingList;
        }
        Concept observedConcept = conceptController.getConceptByName(conceptName);
        if (observedConcept == null) {
            String conceptId = getConceptId(rawConceptName);
            int intConceptId = Integer.parseInt(conceptId);
            if(intConceptId >0){
                observedConcept = buildDummyConcept(intConceptId, conceptName,isCoded);
            } else {
                observedConcept = buildDummyConcept(conceptName,isCoded);
            }
            newConceptList.add(observedConcept);
        }
        return observedConcept;
    }

    public Observation getObservationEntity(Concept concept, String value) throws ConceptController.ConceptFetchException,
        ConceptController.ConceptParseException, ObservationController.ParseObservationException{
        if (StringUtils.isEmpty(value)) {
            throw new ObservationController.ParseObservationException("Could not create Observation entity for concept '"
                    + concept.getName() + "'. Reason: No Observation value provided.");
        }
        Observation observation = new Observation();
        observation.setUuid(getObservationUuid());
        observation.setValueCoded(defaultValueCodedConcept());

        if (concept.isCoded()) {
            try {
                Concept valueCoded = getConceptEntity(value,false);
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
        String encounterTypeName="";
        String encountertypeUuid="";
        try{
            Form form = formController.getFormByUuid(formUuid);
            if(form == null){
                encounterTypeName = "encounterType";
            } else {
                encounterTypeName = form.getEncounterType().getName();
            }
        } catch (FormController.FormFetchException e) {
            e.printStackTrace( );
        }
        EncounterType encounterType = new EncounterType();
        encounterType.setUuid(encountertypeUuid);
        encounterType.setName(encounterTypeName);
        return encounterType;
    }

    private Location getDummyLocation(int locationId) {
        List<Location> allLocations = new ArrayList<Location>();
        try {
            allLocations = locationController.getAllLocations();
        } catch (LocationController.LocationLoadException e) {
            Log.e("Location Error:","= error loading locations ="+e);
            e.printStackTrace( );
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
        List<Provider> allProviders = new ArrayList<Provider>();
        try {
            allProviders = providerController.getAllProviders();
        } catch (ProviderController.ProviderLoadException e) {
            e.printStackTrace( );
        }
        String providerName = "";
        String providerUuid = "";
        String providerIdentifier = "";
        for(Provider prov:allProviders){
            if(prov.getIdentifier().equals(providerId)){
                providerName = prov.getName();
                providerUuid = prov.getUuid();
                providerIdentifier = prov.getIdentifier();
            }
        }
        Person provider = new Person();
        provider.setUuid(providerUuid);
        provider.setGender("NA");
        PersonName personName = new PersonName();
        personName.setFamilyName(providerName);
        personName.setGivenName(" ");
        personName.setMiddleName(providerIdentifier);
        personName.setPreferred(true);
        ArrayList<PersonName> names = new ArrayList<PersonName>();
        names.add(personName);
        provider.setNames(names);
        return provider;
    }

    private Concept buildDummyConcept(String conceptName,boolean isCoded) {
        return buildDummyConcept(0, conceptName, isCoded);
    }

    private Concept buildDummyConcept(int conceptId, String conceptName, boolean isCoded) {
        Concept concept = new Concept();
        concept.setUuid(CONCEPT_CREATED_ON_PHONE + UUID.randomUUID());
        ConceptName dummyConceptName = new ConceptName();
        dummyConceptName.setName(conceptName);
        dummyConceptName.setPreferred(true);
        concept.setConceptNames(asList(dummyConceptName));
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
}

