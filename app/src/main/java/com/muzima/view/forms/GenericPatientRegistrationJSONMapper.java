/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.util.Log;
import com.muzima.MuzimaApplication;
import com.muzima.api.exception.InvalidPatientIdentifierException;
import com.muzima.api.model.FormData;
import com.muzima.api.model.Location;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PatientIdentifier;
import com.muzima.api.model.PatientIdentifierType;
import com.muzima.api.model.Person;
import com.muzima.api.model.PersonAddress;
import com.muzima.api.model.PersonAttribute;
import com.muzima.api.model.PersonAttributeType;
import com.muzima.api.model.PersonName;
import com.muzima.api.model.Relationship;
import com.muzima.api.model.RelationshipType;
import com.muzima.api.model.User;
import com.muzima.controller.FormController;
import com.muzima.controller.LocationController;
import com.muzima.controller.PatientController;
import com.muzima.controller.PersonController;
import com.muzima.controller.RelationshipController;
import com.muzima.utils.Constants;
import com.muzima.utils.DateUtils;
import com.muzima.utils.RelationshipJsonMapper;
import com.muzima.utils.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static com.muzima.utils.PersonRegistrationUtils.copyPersonAddress;
import static com.muzima.utils.PersonRegistrationUtils.createBirthDate;
import static com.muzima.utils.PersonRegistrationUtils.createBirthDateEstimated;
import static com.muzima.utils.PersonRegistrationUtils.createDemographicsUpdateBirthDate;
import static com.muzima.utils.PersonRegistrationUtils.createDemographicsUpdateBirthDateEstimated;
import static com.muzima.utils.PersonRegistrationUtils.createDemographicsUpdatePersonAddresses;
import static com.muzima.utils.PersonRegistrationUtils.createDemographicsUpdatePersonAttributes;
import static com.muzima.utils.PersonRegistrationUtils.createDemographicsUpdatePersonName;
import static com.muzima.utils.PersonRegistrationUtils.createPersonAddresses;
import static com.muzima.utils.PersonRegistrationUtils.createPersonAttributes;
import static com.muzima.utils.PersonRegistrationUtils.createPersonName;

public class GenericPatientRegistrationJSONMapper{

    private JSONObject patientJSON;
    private JSONObject personJSON;
    private JSONObject encounterJSON;
    private JSONObject demographicsUpdateJSON;
    private Patient patient;
    private MuzimaApplication muzimaApplication;

    public String map(MuzimaApplication muzimaApplication, Patient patient, FormData formData, boolean isLoggedInUserIsDefaultProvider, Patient indexPatient) {
        JSONObject prepopulateJSON = new JSONObject();
        JSONObject patientDetails = new JSONObject();
        JSONObject encounterDetails = new JSONObject();
        JSONObject relationshipDetails = new JSONObject();
        JSONObject indexPatientDetails = new JSONObject();

        try {
            patientDetails.put("patient.given_name", StringUtils.defaultString(patient.getGivenName()));
            patientDetails.put("patient.middle_name", StringUtils.defaultString(patient.getMiddleName()));
            patientDetails.put("patient.family_name", StringUtils.defaultString(patient.getFamilyName()));
            patientDetails.put("patient.sex", StringUtils.defaultString(patient.getGender()));
            patientDetails.put("patient.uuid", StringUtils.defaultString(patient.getUuid()));
            if (patient.getBirthdate() != null) {
                patientDetails.put("patient.birth_date", DateUtils.getFormattedDate(patient.getBirthdate()));
                patientDetails.put("patient.birthdate_estimated", Boolean.toString(patient.getBirthdateEstimated()));
            }

            encounterDetails.put("encounter.form_uuid", StringUtils.defaultString(formData.getTemplateUuid()));
            encounterDetails.put("encounter.user_system_id", StringUtils.defaultString(formData.getUserSystemId()));

            if (isLoggedInUserIsDefaultProvider) {
                User loggedInUser = muzimaApplication.getAuthenticatedUser();
                encounterDetails.put("encounter.provider_id_select", loggedInUser.getSystemId());
                encounterDetails.put("encounter.provider_id", loggedInUser.getSystemId());
            }

            if (!patient.getIdentifiers().isEmpty()) {
                List<PatientIdentifier> patientIdentifiers = patient.getIdentifiers();

                JSONArray identifierJSONArray = new JSONArray();

                for (PatientIdentifier identifier : patientIdentifiers) {
                    if (identifier.getIdentifier() != null){
                        if(identifier.getIdentifier().equals(patient.getIdentifier())){
                            JSONObject preferredIdentifierJSONObject = new JSONObject();
                            preferredIdentifierJSONObject.put("identifier_value", identifier.getIdentifier());
                            preferredIdentifierJSONObject.put("identifier_type_uuid", identifier.getIdentifierType().getUuid());
                            preferredIdentifierJSONObject.put("identifier_type_name", identifier.getIdentifierType().getName());
                            patientDetails.put("patient.medical_record_number", preferredIdentifierJSONObject);
                        } else if (!identifier.getIdentifier().equals(patient.getUuid())) {
                            JSONObject identifierJSONObject = new JSONObject();
                            identifierJSONObject.put("identifier_value", identifier.getIdentifier());
                            identifierJSONObject.put("identifier_type_uuid", identifier.getIdentifierType().getUuid());
                            identifierJSONObject.put("identifier_type_name", identifier.getIdentifierType().getName());
                            identifierJSONArray.put(identifierJSONObject);
                        }
                    }
                }
                patientDetails.put("patient.otheridentifier", identifierJSONArray);
            }

            if(!patient.getAtributes().isEmpty()){
                List<PersonAttribute> attributes = patient.getAtributes();

                JSONArray attributesJSONArray = new JSONArray();

                for(PersonAttribute attribute : attributes){
                    JSONObject attributeJSONObject = new JSONObject();
                    attributeJSONObject.put("attribute_type_uuid",attribute.getAttributeType().getUuid());
                    attributeJSONObject.put("attribute_type_name",attribute.getAttributeType().getName());
                    attributeJSONObject.put("attribute_value",attribute.getAttribute());
                    attributesJSONArray.put(attributeJSONObject);
                }
                patientDetails.put("patient.personattribute",attributesJSONArray);
            }

            if(!patient.getAddresses().isEmpty()){
                List<PersonAddress> addresses = patient.getAddresses();
                JSONArray addressesJSONArray = new JSONArray();
                for(PersonAddress address : addresses){
                    JSONObject addressJSONObject = new JSONObject();
                    addressJSONObject.put("address1",address.getAddress1());
                    addressJSONObject.put("address2",address.getAddress2());
                    addressJSONObject.put("address3",address.getAddress3());
                    addressJSONObject.put("address4",address.getAddress4());
                    addressJSONObject.put("address5",address.getAddress5());
                    addressJSONObject.put("address6",address.getAddress6());
                    addressJSONObject.put("cityVillage",address.getCityVillage());
                    addressJSONObject.put("stateProvince",address.getStateProvince());
                    addressJSONObject.put("country",address.getCountry());
                    addressJSONObject.put("postalCode",address.getPostalCode());
                    addressJSONObject.put("countyDistrict",address.getCountyDistrict());
                    addressJSONObject.put("latitude",address.getLatitude());
                    addressJSONObject.put("longitude",address.getLongitude());
                    addressJSONObject.put("startDate",address.getStartDate());
                    addressJSONObject.put("endDate",address.getEndDate());
                    addressJSONObject.put("preferred",address.getPreferred());
                    addressJSONObject.put("uuid",address.getUuid());
                    addressesJSONArray.put(addressJSONObject);
                }
                patientDetails.put("patient.personaddress",addressesJSONArray);
            }

            try {
                List<Relationship> relationships = (muzimaApplication.getRelationshipController())
                        .getRelationshipsForPerson(patient.getUuid());
                JSONArray relationshipsJsonArray = new JSONArray();
                for (Relationship relationship:relationships) {
                    JSONObject relationshipJsonObject = new JSONObject();
                    relationshipJsonObject.put("personA.uuid",relationship.getPersonA().getUuid());
                    relationshipJsonObject.put("personB.uuid",relationship.getPersonB().getUuid());
                    relationshipJsonObject.put("person.relationshipType",relationship.getRelationshipType().getUuid());

                    relationshipsJsonArray.put(relationshipJsonObject);
                }
                relationshipDetails.put("person.relationships",relationshipsJsonArray);
            } catch (RelationshipController.RetrieveRelationshipException e) {
                Log.e(getClass().getSimpleName(), "Could not retrieve relationships",e);
            } catch (JSONException e) {
                Log.e(getClass().getSimpleName(), "Could not build relationships JSON",e);
            }

            if(indexPatient != null){
                indexPatientDetails.put("index_patient.given_name", StringUtils.defaultString(indexPatient.getGivenName()));
                indexPatientDetails.put("index_patient.middle_name", StringUtils.defaultString(indexPatient.getMiddleName()));
                indexPatientDetails.put("index_patient.family_name", StringUtils.defaultString(indexPatient.getFamilyName()));
                indexPatientDetails.put("index_patient.uuid", StringUtils.defaultString(indexPatient.getUuid()));
                indexPatientDetails.put("index_patient.sex", StringUtils.defaultString(indexPatient.getGender()));
                indexPatientDetails.put("index_patient.medical_record_number", StringUtils.defaultString(indexPatient.getIdentifier()));
                if(indexPatient.getBirthdate() != null) {
                    indexPatientDetails.put("index_patient.birth_date", DateUtils.getFormattedDate(indexPatient.getBirthdate()));
                    indexPatientDetails.put("index_patient.birthdate_estimated", Boolean.toString(indexPatient.getBirthdateEstimated()));
                }
            }

            prepopulateJSON.put("patient", patientDetails);
            prepopulateJSON.put("encounter", encounterDetails);
            prepopulateJSON.put("person", relationshipDetails);
            prepopulateJSON.put("index_patient", indexPatientDetails);
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "Could not populate patient registration data to JSON", e);
        }
        return prepopulateJSON.toString();
    }

    public Patient getPatient(MuzimaApplication muzimaApplication, String jsonPayload) throws JSONException {
        setMuzimaApplication(muzimaApplication);
        setJSONObjects(jsonPayload);
        createPatient();
        createRelationships();
        return patient;
    }

    public Patient processDemographicsUpdateForPatient(MuzimaApplication muzimaApplication, String jsonPayload, Patient patient) throws JSONException {
        setJSONObjects(jsonPayload);
        setMuzimaApplication(muzimaApplication);
        setDemographicsUpdatePatient(patient);
        updatePatient();
        if(isRelationshipStubDefined()) {
            createRelationships();
        }
        return patient;
    }

    public Person processDemographicsUpdateForPerson(MuzimaApplication muzimaApplication, String jsonPayload, Person person) throws JSONException {
        setJSONObjects(jsonPayload);
        setMuzimaApplication(muzimaApplication);
        setDemographicsUpdatePersonAsPatient(person);
        updatePersonAsPatient();
        copyDemographicsUpdateFromPatient(person);
        return person;
    }

    private void setMuzimaApplication(MuzimaApplication muzimaApplication){
        this.muzimaApplication = muzimaApplication;
    }

    private void setJSONObjects(String jsonPayload) throws JSONException {
        JSONObject responseJSON = new JSONObject(jsonPayload);
        patientJSON = responseJSON.getJSONObject("patient");
        encounterJSON = responseJSON.getJSONObject("encounter");
        if(responseJSON.has("person")) {
            personJSON = responseJSON.getJSONObject("person");
        }
        if(responseJSON.has("demographicsupdate")) {
            demographicsUpdateJSON = responseJSON.getJSONObject("demographicsupdate");
        }
    }

    private void createPatient() throws JSONException {
        if(patientJSON != null) {
            initializePatient();
            setPatientIdentifiers();
            setPatientNames();
            setPatientGender();
            setPatientBirthDate();
            setPersonAddresses();
            setPersonAttributes();
        }
    }

    private void updatePatient() throws JSONException {
        if(demographicsUpdateJSON != null) {
            updatePatientIdentifiers();
            updatePatientNames();
            updatePatientGender();
            updatePatientBirthDate();
            updatePersonAddresses();
            updatePersonAttributes();
        }
    }

    private void updatePersonAsPatient() throws JSONException {
        if(demographicsUpdateJSON != null) {
            updatePatientNames();
            updatePatientGender();
            updatePatientBirthDate();
            updatePersonAddresses();
            updatePersonAttributes();
        }
    }

    private void initializePatient() throws JSONException {
        patient = new Patient();
        patient.setUuid(patientJSON.getString("patient.uuid"));
    }

    private void setDemographicsUpdatePatient(Patient patient) {
        this.patient = patient;
    }

    private void setDemographicsUpdatePersonAsPatient(Person person) throws JSONException {
        patient = new Patient();
        patient.setUuid(person.getUuid());
        patient.setNames(person.getNames());
        patient.setGender(person.getGender());
        patient.setBirthdate(person.getBirthdate());
        patient.setAddresses(person.getAddresses());
        patient.setAttributes(person.getAtributes());
    }

    private void copyDemographicsUpdateFromPatient(Person person) throws JSONException {
        person.setNames(patient.getNames());
        person.setGender(patient.getGender());
        person.setBirthdate(patient.getBirthdate());
        person.setAddresses(patient.getAddresses());
        person.setAttributes(patient.getAtributes());
    }

    private void setPatientIdentifiers() throws JSONException {
        List<PatientIdentifier> identifiers = getPatientIdentifiers();
        Location location = getEncounterLocation();
        for(PatientIdentifier identifier:identifiers){
            identifier.setLocation(location);
        }
        patient.setIdentifiers(identifiers);
    }

    private void updatePatientIdentifiers() throws JSONException {
        List<PatientIdentifier> identifiers = getUpdatedPatientIdentifiers();
        Location location = getEncounterLocation();
        for(PatientIdentifier identifier:identifiers){
            identifier.setLocation(location);
            PatientIdentifier existingIdentifier = patient.getIdentifier(identifier.getIdentifierType().getUuid());
            if(existingIdentifier == null){
                existingIdentifier = patient.getIdentifier(identifier.getIdentifierType().getName());
            }
            if(existingIdentifier != null){
                existingIdentifier.setIdentifier(identifier.getIdentifier());
            } else {
                patient.addIdentifier(identifier);
            }
        }
    }

    private void setPatientNames() throws JSONException {
        List<PersonName> names = new ArrayList<>();
        names.add(createPersonName(patientJSON));
        patient.setNames(names);
    }

    private void updatePatientNames() throws JSONException {
        if(demographicsUpdateJSON != null && demographicsUpdateJSON.has("demographicsupdate.given_name")) {
            PersonName newName = createDemographicsUpdatePersonName(demographicsUpdateJSON);
            if(newName != null) {
                for (PersonName personName : patient.getNames()) {
                    personName.setPreferred(false);
                }
                newName.setPreferred(true);
                patient.addName(newName);
            }
        }
    }

    private void setPatientGender() throws JSONException {
        String gender = patientJSON.getString("patient.sex");
        patient.setGender(gender);
    }

    private void updatePatientGender() throws JSONException {
        if(!muzimaApplication.getMuzimaSettingController().isDemographicsUpdateManulReviewNeeded() &&
                demographicsUpdateJSON != null && demographicsUpdateJSON.has("demographicsupdate.sex")
        ){
            String gender = demographicsUpdateJSON.getString("demographicsupdate.sex");
            patient.setGender(gender);
        }
    }

    private void setPatientBirthDate() throws JSONException {
        patient.setBirthdate(createBirthDate(patientJSON));
        patient.setBirthdateEstimated(createBirthDateEstimated(patientJSON));
    }

    private void updatePatientBirthDate() throws JSONException {
        if(!muzimaApplication.getMuzimaSettingController().isDemographicsUpdateManulReviewNeeded() &&
                demographicsUpdateJSON != null && demographicsUpdateJSON.has("demographicsupdate.birth_date")
        ){
            patient.setBirthdate(createDemographicsUpdateBirthDate(demographicsUpdateJSON));
            patient.setBirthdateEstimated(createDemographicsUpdateBirthDateEstimated(demographicsUpdateJSON));
        }
    }

    private List<PatientIdentifier> getPatientIdentifiers() throws JSONException {
        List<PatientIdentifier> patientIdentifiers = new ArrayList<>();

        patientIdentifiers.add(getPatientUuidAsIdentifier());
        boolean requireMedicalRecordNumber = muzimaApplication.getMuzimaSettingController()
                .isMedicalRecordNumberRequiredDuringRegistration();
        PatientIdentifier medicalRecordIdentifier = null;
        if(requireMedicalRecordNumber || patientJSON.has("patient.medical_record_number")) {
            medicalRecordIdentifier = getMedicalRecordNumberIdentifier();
        }

        if(medicalRecordIdentifier != null) {
            if(requireMedicalRecordNumber){
                medicalRecordIdentifier.setPreferred(true);
            }
            patientIdentifiers.add(medicalRecordIdentifier);
        }

        List<PatientIdentifier> otherIdentifiers = getOtherPatientIdentifiers();
        if (!otherIdentifiers.isEmpty())
            patientIdentifiers.addAll(otherIdentifiers);
        return patientIdentifiers;
    }

    private List<PatientIdentifier> getUpdatedPatientIdentifiers() throws JSONException {
        List<PatientIdentifier> patientIdentifiers = new ArrayList<>();

        PatientIdentifier medicalRecordIdentifier = null;
        if(demographicsUpdateJSON != null && demographicsUpdateJSON.has("demographicsupdate.medical_record_number")){
             medicalRecordIdentifier = getDemographicsUpdateMedicalRecordNumberIdentifier();
        }

        if(medicalRecordIdentifier != null) {
            medicalRecordIdentifier.setPreferred(true);
            patientIdentifiers.add(medicalRecordIdentifier);
        }

        List<PatientIdentifier> otherIdentifiers = getOtherUpdatedPatientIdentifiers();
        if (!otherIdentifiers.isEmpty())
            patientIdentifiers.addAll(otherIdentifiers);
        return patientIdentifiers;
    }

    private Location getEncounterLocation() throws JSONException {
        String locationId = encounterJSON.getString("encounter.location_id");
        LocationController locationController = muzimaApplication.getLocationController();
        try {
            Location location = locationController.getLocationById(Integer.parseInt(locationId));
            if(location == null){
                throw new JSONException("Could not find location in local repo");
            }
            return location;
        } catch (LocationController.LocationLoadException e) {
            throw new JSONException("Could not find location in local repo");
        }
    }

    private PatientIdentifier getDemographicsUpdateMedicalRecordNumberIdentifier() throws JSONException {
        if(demographicsUpdateJSON.has("demographicsupdate.medical_record_number")) {
            Object identifierObject = patientJSON.getJSONObject("demographicsupdate.medical_record_number");
            try {
                if(identifierObject instanceof String && !StringUtils.isEmpty((String)identifierObject)){
                    return createPatientIdentifier(Constants.LOCAL_PATIENT, null, (String)identifierObject);
                } else if(identifierObject instanceof JSONObject){
                    return createPatientIdentifier((JSONObject) identifierObject);
                }
            } catch (InvalidPatientIdentifierException e){

                throw new RuntimeException("Invalid demographicsupdate medical record number", e);
            }
        }
        throw new RuntimeException("Cannot find demographicsupdate medical record number");
    }

    private PatientIdentifier getMedicalRecordNumberIdentifier() throws JSONException {
        if(patientJSON.has("patient.medical_record_number")) {
            Object identifierObject = patientJSON.getJSONObject("patient.medical_record_number");
            try {
                if(identifierObject instanceof String && !StringUtils.isEmpty((String)identifierObject)){
                    return createPatientIdentifier(Constants.LOCAL_PATIENT, null, (String)identifierObject);
                } else if(identifierObject instanceof JSONObject){
                    return createPatientIdentifier((JSONObject) identifierObject);
                }
            } catch (InvalidPatientIdentifierException e){

                throw new RuntimeException("Invalid patient medical record number", e);
            }
        }
        throw new RuntimeException("Cannot find patient medical record number");
    }

    private List<PatientIdentifier> getOtherPatientIdentifiers() throws JSONException {
        List<PatientIdentifier> otherIdentifiers = new ArrayList<>();
        if (patientJSON != null && patientJSON.has("patient.otheridentifier")) {
            Object otherIdentifierObject = patientJSON.get("patient.otheridentifier");

            if (otherIdentifierObject instanceof JSONArray) {
                JSONArray identifiers = (JSONArray) otherIdentifierObject;
                for (int i = 0; i < identifiers.length(); i++) {
                    try {
                        PatientIdentifier identifier = createPatientIdentifier(identifiers.getJSONObject(i));
                        if (identifier != null) {
                            otherIdentifiers.add(identifier);
                        }
                    }catch (InvalidPatientIdentifierException e){
                        Log.e(getClass().getSimpleName(), "Error while creating identifier.", e);
                    }
                }
            } else if (otherIdentifierObject instanceof JSONObject) {
                try {
                    PatientIdentifier identifier = createPatientIdentifier((JSONObject) otherIdentifierObject);
                    if (identifier != null) {
                        otherIdentifiers.add(identifier);
                    }
                } catch (InvalidPatientIdentifierException e){
                    Log.e(getClass().getSimpleName(), "Error while creating identifier.", e);
                }
            }
        }

        Iterator<String> keys = patientJSON.keys();
        while(keys.hasNext()){
            String key = keys.next();
            if(key.startsWith("patient.otheridentifier^")){
                try {
                    otherIdentifiers.add(createPatientIdentifier(patientJSON.getJSONObject(key)));
                } catch (InvalidPatientIdentifierException e){
                    Log.e(getClass().getSimpleName(), "Error while creating identifier.", e);
                }
            }
        }

        return otherIdentifiers;
    }

    private List<PatientIdentifier> getOtherUpdatedPatientIdentifiers() throws JSONException {
        List<PatientIdentifier> otherIdentifiers = new ArrayList<>();
        if (demographicsUpdateJSON != null) {
            if(demographicsUpdateJSON.has("demographicsupdate.otheridentifier")) {
                Object otherIdentifierObject = demographicsUpdateJSON.get("demographicsupdate.otheridentifier");

                if (otherIdentifierObject instanceof JSONArray) {
                    JSONArray identifiers = (JSONArray) otherIdentifierObject;
                    for (int i = 0; i < identifiers.length(); i++) {
                        try {
                            PatientIdentifier identifier = createPatientIdentifier(identifiers.getJSONObject(i));
                            if (identifier != null) {
                                otherIdentifiers.add(identifier);
                            }
                        }catch (InvalidPatientIdentifierException e){
                            Log.e(getClass().getSimpleName(), "Error while creating identifier.", e);
                        }
                    }
                } else if (otherIdentifierObject instanceof JSONObject) {
                    try {
                        PatientIdentifier identifier = createPatientIdentifier((JSONObject) otherIdentifierObject);
                        if (identifier != null) {
                            otherIdentifiers.add(identifier);
                        }
                    } catch (InvalidPatientIdentifierException e){
                        Log.e(getClass().getSimpleName(), "Error while creating identifier.", e);
                    }
                }
            }

            Iterator<String> keys = demographicsUpdateJSON.keys();
            while(keys.hasNext()){
                String key = keys.next();
                if(key.startsWith("demographicsupdate.otheridentifier^")){
                    try {
                        otherIdentifiers.add(createPatientIdentifier(demographicsUpdateJSON.getJSONObject(key)));
                    } catch (InvalidPatientIdentifierException e){
                        Log.e(getClass().getSimpleName(), "Error while creating identifier.", e);
                    }
                }
            }
        }

        return otherIdentifiers;
    }

    private PatientIdentifier getPatientUuidAsIdentifier() {
        try {
            return createPatientIdentifier(Constants.LOCAL_PATIENT, null, patient.getUuid());
        } catch (InvalidPatientIdentifierException e){
            throw new RuntimeException("Invalid patient medical record number", e);
        }
    }

    private PatientIdentifier createPatientIdentifier(JSONObject identifierJSONObject) throws JSONException,InvalidPatientIdentifierException{
        if((identifierJSONObject.has("identifier_type_uuid") || identifierJSONObject.has("identifier_type_name"))
                && identifierJSONObject.has("identifier_value")) {
            String identifierTypeName = (String) getFromJsonObject(identifierJSONObject, "identifier_type_name");
            String identifierTypeUuid = (String) getFromJsonObject(identifierJSONObject, "identifier_type_uuid");
            String identifierValue = (String) getFromJsonObject(identifierJSONObject, "identifier_value");
            return createPatientIdentifier(identifierTypeName, identifierTypeUuid, identifierValue);
        } else {
            throw new InvalidPatientIdentifierException("Cannot create patient identifier due to missing identifier_type or identifier_value");
        }
    }

    private PatientIdentifier createPatientIdentifier(String identifierTypeName, String identifierTypeUuid, String identifierValue)
            throws InvalidPatientIdentifierException{
        if(StringUtils.isEmpty(identifierValue) || StringUtils.isEmpty(identifierTypeName)
                && StringUtils.isEmpty(identifierTypeUuid)){
            throw new InvalidPatientIdentifierException("Cannot create Identifier. Missing identifier value and identifier type name or" +
                    " identifier type uuid");
        }

        PatientIdentifier patientIdentifier = new PatientIdentifier();
        PatientIdentifierType identifierType = null;

        if(!StringUtils.isEmpty(identifierTypeUuid)){
            identifierType = muzimaApplication.getPatientController()
                    .getPatientIdentifierTypeByUuid(identifierTypeUuid);
        } else if(!StringUtils.isEmpty(identifierTypeName)){
            List<PatientIdentifierType> tmpIdentifierTypes = muzimaApplication.getPatientController()
                    .getPatientIdentifierTypeByName(identifierTypeName);
            if(tmpIdentifierTypes.size() == 1){
                identifierType = tmpIdentifierTypes.get(0);
            }
        }

        if(identifierType == null){
            identifierType = new PatientIdentifierType();
            if(!StringUtils.isEmpty(identifierTypeUuid)){
                identifierType.setUuid(identifierTypeUuid);
            }
            if(!StringUtils.isEmpty(identifierTypeName)){
                identifierType.setName(identifierTypeName);
            }
        }
        patientIdentifier.setIdentifierType(identifierType);
        patientIdentifier.setIdentifier(identifierValue);
        return patientIdentifier;
    }

    private void setPersonAddresses() throws JSONException {
        List<PersonAddress> addresses = createPersonAddresses(patientJSON);
        if(!addresses.isEmpty()){
            patient.setAddresses(addresses);
        }
    }
    private void updatePersonAddresses() throws JSONException {
        List<PersonAddress> demographicsUpdateAddresses = createDemographicsUpdatePersonAddresses(demographicsUpdateJSON);
        for(PersonAddress demographicsUpdateAddress:demographicsUpdateAddresses){
            boolean preExistingAddressFound = false;
            for(PersonAddress preExistingAddress:patient.getAddresses()){
                if (StringUtils.equals(demographicsUpdateAddress.getUuid(), preExistingAddress.getUuid())) {
                    preExistingAddressFound = true;
                    try{
                        copyPersonAddress(demographicsUpdateAddress, preExistingAddress);
                    } catch (Exception e){
                        Log.e(getClass().getSimpleName(), "Could not copy address",e);
                    }
                    break;
                }
            }
            if(!preExistingAddressFound){
                patient.addAddress(demographicsUpdateAddress);
            }
        }
    }

    private void setPersonAttributes() throws JSONException{
        List<PersonAttribute> attributes = createPersonAttributes(patientJSON,muzimaApplication);

        if(!attributes.isEmpty()) {
            patient.setAttributes(attributes);
        }
    }

    private void updatePersonAttributes() throws JSONException{
        List<PersonAttribute> demographicsUpdatePersonAttributes  = createDemographicsUpdatePersonAttributes(demographicsUpdateJSON,muzimaApplication);

        Iterator<PersonAttribute> demographicsUpdateAttributesIterator = demographicsUpdatePersonAttributes.iterator();
        while(demographicsUpdateAttributesIterator.hasNext()) {
            boolean preExistingAttributeFound = false;
            PersonAttribute demographicsUpdateAttribute = demographicsUpdateAttributesIterator.next();
            PersonAttributeType demographicsUpdateAttributeType = demographicsUpdateAttribute.getAttributeType();

            for (PersonAttribute preExistingAttribute:patient.getAtributes()) {
                PersonAttributeType preExistingAttributeType = preExistingAttribute.getAttributeType();
                if(StringUtils.equals(preExistingAttributeType.getUuid(), demographicsUpdateAttributeType.getUuid()) ||
                        StringUtils.equals(preExistingAttributeType.getName(), demographicsUpdateAttributeType.getName())) {
                    preExistingAttributeFound = true;
                    preExistingAttribute.setAttribute(demographicsUpdateAttribute.getAttribute());
                    break;
                }
            }

            if(!preExistingAttributeFound){
                patient.addattribute(demographicsUpdateAttribute);
            }
        }
    }

    private void createRelationship(JSONObject jsonObject) throws JSONException{
        try {
            if(jsonObject.has("person.relationshipType")) {
                String relationshipTypeUuid = (String) getFromJsonObject(jsonObject, "person.relationshipType");
                RelationshipType relationshipType = muzimaApplication.getRelationshipController().getRelationshipTypeByUuid(relationshipTypeUuid);
                String personBUuid = (String) getFromJsonObject(jsonObject, "personB.uuid");
                String personAUuid = (String) getFromJsonObject(jsonObject, "personA.uuid");

                if (StringUtils.isEmpty(personAUuid) || StringUtils.isEmpty(personBUuid)) {
                    return;
                }

                Person personA;
                if (StringUtils.equals(personAUuid, patient.getUuid())) {
                    personA = patient;
                } else {
                    personA = muzimaApplication.getPersonController().getPersonByUuid(personAUuid);
                    if (personA == null) {
                        personA = muzimaApplication.getPatientController().getPatientByUuid(personAUuid);
                    }
                }

                Person personB;
                if (StringUtils.equals(personBUuid, patient.getUuid())) {
                    personB = patient;
                } else {
                    personB = muzimaApplication.getPersonController().getPersonByUuid(personBUuid);
                    if (personB == null) {
                        personB = muzimaApplication.getPatientController().getPatientByUuid(personBUuid);
                    }
                }

                if (relationshipType != null && personA != null && personB != null && personA != personB) {
                    List<Relationship> existingRelationships = muzimaApplication.getRelationshipController().getRelationshipsForPerson(personA.getUuid());
                    Relationship existingRelationship = null;
                    for (Relationship relationship : existingRelationships) {
                        if ((StringUtils.equals(relationship.getPersonA().getUuid(), personA.getUuid()) ||
                                StringUtils.equals(relationship.getPersonA().getUuid(), personB.getUuid()))
                                && (StringUtils.equals(relationship.getPersonB().getUuid(), personA.getUuid()) ||
                                StringUtils.equals(relationship.getPersonB().getUuid(), personB.getUuid()))) {
                            existingRelationship = relationship;
                            break;
                        }
                    }

                    if (existingRelationship == null) {
                        Relationship newRelationship = new Relationship(personA, personB, relationshipType, false);
                        newRelationship.setUuid(UUID.randomUUID().toString());

                        RelationshipJsonMapper relationshipJsonMapper = new RelationshipJsonMapper(muzimaApplication);
                        muzimaApplication.getFormController().saveFormData(relationshipJsonMapper.createFormDataFromRelationship(patient, newRelationship));

                        muzimaApplication.getRelationshipController().saveRelationship(newRelationship);
                    } else if (!StringUtils.equals(existingRelationship.getRelationshipType().getUuid(), relationshipType.getUuid())) {
                        //ToDo: Consider updating relationship type for existing relationship
                        Log.d(getClass().getSimpleName(), "Could not create relationship");
                    }
                } else {
                    throw new JSONException("Could not create relationship");
                }
            }
        } catch (RelationshipController.RetrieveRelationshipTypeException | JSONException |
                RelationshipController.SaveRelationshipException | PersonController.PersonLoadException | PatientController.PatientLoadException |
                FormController.FormDataSaveException | RelationshipController.RetrieveRelationshipException e) {
            Log.e(getClass().getSimpleName(), "Could not create relationship",e);
            throw new JSONException("Could not create relationship");
        }
    }

    private void createRelationships() throws JSONException{
        if(isRelationshipStubDefined()) {
            Object relationshipObject = personJSON.get("person.relationships");

            if (relationshipObject instanceof JSONArray) {
                JSONArray relationships = (JSONArray) relationshipObject;
                for (int i = 0; i < relationships.length(); i++) {
                    createRelationship(relationships.getJSONObject(i));
                }
            } else if (relationshipObject instanceof JSONObject) {
                createRelationship((JSONObject) relationshipObject);
            }
        }
    }

    private boolean isRelationshipStubDefined(){
        return personJSON != null && personJSON.has("person.relationships");
    }

    private Object getFromJsonObject(JSONObject jsonObject, String key) throws JSONException{
        if (jsonObject.has(key)) {
            return jsonObject.getString(key);
        }
        return null;
    }
}
