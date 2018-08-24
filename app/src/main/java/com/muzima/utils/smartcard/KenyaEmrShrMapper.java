package com.muzima.utils.smartcard;

import android.util.Log;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muzima.MuzimaApplication;
import com.muzima.api.model.Concept;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.FormData;
import com.muzima.api.model.Location;
import com.muzima.api.model.LocationAttribute;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PatientIdentifier;
import com.muzima.api.model.Person;
import com.muzima.api.model.PersonName;
import com.muzima.api.model.SmartCardRecord;
import com.muzima.api.model.User;
import com.muzima.controller.ConceptController;
import com.muzima.controller.EncounterController;
import com.muzima.controller.FormController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.PatientController;
import com.muzima.controller.SmartCardController;
import com.muzima.model.observation.EncounterWithObservations;
import com.muzima.model.observation.Encounters;
import com.muzima.model.shr.kenyaemr.CardDetails;
import com.muzima.model.shr.kenyaemr.ExternalPatientId;
import com.muzima.model.shr.kenyaemr.HIVTest;
import com.muzima.model.shr.kenyaemr.Immunization;
import com.muzima.model.shr.kenyaemr.InternalPatientId;
import com.muzima.model.shr.kenyaemr.KenyaEmrSHRModel;
import com.muzima.model.shr.kenyaemr.MotherDetails;
import com.muzima.model.shr.kenyaemr.MotherIdentifier;
import com.muzima.model.shr.kenyaemr.MotherName;
import com.muzima.model.shr.kenyaemr.PatientAddress;
import com.muzima.model.shr.kenyaemr.PatientIdentification;
import com.muzima.model.shr.kenyaemr.PatientName;
import com.muzima.model.shr.kenyaemr.PhysicalAddress;
import com.muzima.model.shr.kenyaemr.ProviderDetails;
import com.muzima.service.HTMLFormObservationCreator;
import com.muzima.utils.Constants;
import com.muzima.utils.Constants.Shr.KenyaEmr.PersonIdentifierType;
import com.muzima.utils.Constants.Shr.KenyaEmr.CONCEPTS;
import com.muzima.utils.DateUtils;
import com.muzima.utils.LocationUtils;
import com.muzima.utils.PatientIdentifierUtils;
import com.muzima.utils.StringUtils;
import com.muzima.view.forms.HTMLPatientJSONMapper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.muzima.utils.Constants.FORM_JSON_DISCRIMINATOR_SHR_DEMOGRAPHICS_UPDATE;
import static com.muzima.utils.Constants.FORM_JSON_DISCRIMINATOR_SHR_REGISTRATION;
import static com.muzima.utils.Constants.STATUS_COMPLETE;

public class KenyaEmrShrMapper {

    /**
     * Converts an SHR model from JSON representation to KenyaEmrSHRModel
     * @param jsonSHRModel the JSON representation of the SHR model
     * @return Representation of the JSON input as KenyaEmrSHRModel object
     * @throws IOException
     */
    public static KenyaEmrSHRModel createSHRModelFromJson(String jsonSHRModel) throws ShrParseException {
        ObjectMapper objectMapper = new ObjectMapper();
        KenyaEmrSHRModel SHRModel = null;
        try {
            SHRModel = objectMapper.readValue(jsonSHRModel,KenyaEmrSHRModel.class);
        } catch (IOException e) {
            throw new ShrParseException(e);
        }
        return SHRModel;
    }

    /**
     * Converts a KenyaEmrSHRModel representation of SHR to JSON representation
     * @param SHRModel the KenyaEmrSHRModel Object representation of the SHR model
     * @return JSON representation of SHR model
     * @throws IOException
     */
    public static String createJsonFromSHRModel(KenyaEmrSHRModel SHRModel) throws ShrParseException{
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(SHRModel);
        } catch (IOException e) {
            throw new ShrParseException(e);
        }
    }

    /**
     * Extracts a Patient object from a JSON SHR model
     * @param SHRModel the JSON representation of the SHR model
     * @return Patient object extracted from SHR model
     * @throws IOException
     */
    public static Patient extractPatientFromSHRModel(MuzimaApplication muzimaApplication,String SHRModel) throws ShrParseException{
        KenyaEmrSHRModel kenyaEmrSHRModel = createSHRModelFromJson(SHRModel);
        if(kenyaEmrSHRModel.getCardDetails().getLastUpdated() != null && kenyaEmrSHRModel.getPatientIdentification().getDateOfBirth() != null)
            return extractPatientFromSHRModel(muzimaApplication, kenyaEmrSHRModel);
        return null;
    }

    /**
     * Extracts a Patient Object from a KenyaEmrSHRModel Object of SHR model
     * @param SHRModel the KenyaEmrSHRModel Object representation of the SHR model
     * @return Patient object extracted from SHR model
     * @throws IOException
     */
    private static Patient extractPatientFromSHRModel(MuzimaApplication muzimaApplication, KenyaEmrSHRModel SHRModel) throws ShrParseException{
        try {
            Patient patient = new Patient();
            PatientIdentification identification = SHRModel.getPatientIdentification();
            List<PersonName> names = extractPatientNamesFromSHRModel(SHRModel);
            patient.setNames(names);

            //set Identifiers
            List<PatientIdentifier> identifiers = extractPatientIdentifiersFromSHRModel(muzimaApplication, SHRModel);
            if(!identifiers.isEmpty()) {
                patient.setIdentifiers(identifiers);
            }

            //date of birth
            Date dob = DateUtils.parseDateByPattern(identification.getDateOfBirth(),"yyyyMMdd");
            patient.setBirthdate(dob);

            //Gender
            String gender = identification.getSex();
            if(gender.equalsIgnoreCase("M")){
                patient.setGender("M");
            } else if (gender.equalsIgnoreCase("F")){
                patient.setGender("F");
            } else {
                throw new ShrParseException("Could not determine gender from SHR model");
            }
            return patient;
        } catch (ParseException e){
            throw new ShrParseException(e);
        }
    }

    public static void createAndSaveRegistrationPayloadForPatient(MuzimaApplication muzimaApplication, final Patient patient){
        try {
            FormData formData = new FormData() {{
                setUuid(UUID.randomUUID().toString());
                setPatientUuid(patient.getUuid());
                setUserUuid("userUuid");
                setStatus(STATUS_COMPLETE);
                setTemplateUuid(Constants.Shr.KenyaEmr.REGISTRATION.FORM.FORM_UUID);
                setDiscriminator(FORM_JSON_DISCRIMINATOR_SHR_REGISTRATION);
            }};

            User user = muzimaApplication.getAuthenticatedUser();

            formData.setJsonPayload(new HTMLPatientJSONMapper().map(patient, formData, user, true));
            muzimaApplication.getFormController().saveFormData(formData);
        } catch (Throwable E){
            Log.e("Kenya EMR Shr", "Could not create and save registration payload");
        }
    }

    public static void updatePatientDemographicsWithCardSerialNumberAsIdentifier(MuzimaApplication muzimaApplication, final Patient patient, final String cardSerialNumber){
        try {
            Location defaultLocation = LocationUtils.getDefaultEncounterLocationPreference(muzimaApplication);
            if(defaultLocation != null) {
                LocationAttribute attribute= defaultLocation.getAttribute(Constants.Shr.KenyaEmr.LocationAttributeType.MASTER_FACILITY_CODE.name);
                String facilityMflCode = attribute.getAttribute();
                PatientIdentifier patientIdentifier = PatientIdentifierUtils.getOrCreateKenyaEmrIdentifier(muzimaApplication,
                        cardSerialNumber, Constants.Shr.KenyaEmr.PersonIdentifierType.CARD_SERIAL_NUMBER.shr_name,
                        facilityMflCode);
                patient.addIdentifier(patientIdentifier);
                muzimaApplication.getPatientController().updatePatient(patient);

                //ToDo: Create demographics update payload for card serial number
                FormData formData = new FormData() {{
                    setUuid(UUID.randomUUID().toString());
                    setPatientUuid(patient.getUuid());
                    setUserUuid("userUuid");
                    setStatus(STATUS_COMPLETE);
                    setTemplateUuid(Constants.Shr.KenyaEmr.REGISTRATION.FORM.FORM_UUID);
                    setDiscriminator(FORM_JSON_DISCRIMINATOR_SHR_DEMOGRAPHICS_UPDATE);
                }};

                User user = muzimaApplication.getAuthenticatedUser();

                JSONObject formDataJSON = new JSONObject(new HTMLPatientJSONMapper().map(patient, formData, user, true));
                JSONObject demographicsUpdateJson = new JSONObject();

                JSONObject newIdentifierJson = new JSONObject();
                newIdentifierJson.put("identifier_type_name", PersonIdentifierType.CARD_SERIAL_NUMBER.name);
                newIdentifierJson.put("identifier_type_uuid", PersonIdentifierType.CARD_SERIAL_NUMBER.uuid);
                newIdentifierJson.put("identifier_value", cardSerialNumber);

                demographicsUpdateJson.put("demographicsupdate.otheridentifier",newIdentifierJson);
                formDataJSON.put("demographicsupdate", demographicsUpdateJson);

                formData.setJsonPayload(formDataJSON.toString());
                muzimaApplication.getFormController().saveFormData(formData);
            }
        } catch (Throwable e){
            Log.e("Kenya EMR Shr", "Error updating patient identifier. ",e);
        }
    }

    /**
     *
     * @param SHRModel
     * @return
     */
    private static List<PersonName> extractPatientNamesFromSHRModel(KenyaEmrSHRModel SHRModel) throws ShrParseException {
        PatientIdentification identification = SHRModel.getPatientIdentification();
        if(identification != null && identification.getPatientName()!= null) {
            final PersonName personName = new PersonName();
            personName.setFamilyName(identification.getPatientName().getFirstName());
            personName.setGivenName(identification.getPatientName().getLastName());
            personName.setMiddleName(identification.getPatientName().getMiddleName());

            return new ArrayList<PersonName>(){{
                add(personName);
            }};
        } else {
            throw new ShrParseException("Could not find patient names");
        }
    }

    private static KenyaEmrSHRModel putIdentifiersIntoSHRModel(KenyaEmrSHRModel shrModel, List<PatientIdentifier> identifiers) {
        PatientIdentification patientIdentification = shrModel.getPatientIdentification();
        if(patientIdentification == null){
            patientIdentification = new PatientIdentification();
        }

        List<InternalPatientId> internalPatientIds = patientIdentification.getInternalPatientIds();
        if(internalPatientIds == null){
            internalPatientIds = new ArrayList<>();
        }

        for(PatientIdentifier identifier:identifiers){

            try {
                String shrIdentifierTypeName = null;
                String assigningAuthority = null;
                String assigningFacility = LocationUtils.getKenyaEmrMasterFacilityListCode(identifier.getLocation());
                if(StringUtils.isEmpty(assigningFacility)) {
                    assigningFacility = Constants.Shr.KenyaEmr.DEFAULT_SHR_FACILITY.MFL_CODE;
                }

                switch (identifier.getIdentifierType().getName()) {
                    case Constants.Shr.KenyaEmr.PersonIdentifierType.GODS_NUMBER.name:
                        ExternalPatientId externalPatientId = patientIdentification.getExternalPatientId();
                        if(externalPatientId == null){
                            externalPatientId = new ExternalPatientId();
                        }
                        externalPatientId.setIdentifierType(Constants.Shr.KenyaEmr.PersonIdentifierType.GODS_NUMBER.shr_name);
                        externalPatientId.setID(identifier.getIdentifier());
                        externalPatientId.setAssigningAuthority("MPI");
                        externalPatientId.setAssigningFacility(assigningFacility);
                        patientIdentification.setExternalPatientId(externalPatientId);
                        break;
                    case Constants.Shr.KenyaEmr.PersonIdentifierType.CARD_SERIAL_NUMBER.name:
                        shrIdentifierTypeName = PersonIdentifierType.CARD_SERIAL_NUMBER.shr_name;
                        assigningAuthority = "CARD_REGISTRY";
                        break;
                    case PersonIdentifierType.CCC_NUMBER.name:
                        shrIdentifierTypeName = PersonIdentifierType.CCC_NUMBER.shr_name;
                        assigningAuthority = "CCC";
                        break;
                    case PersonIdentifierType.HEI_NUMBER.name:
                        shrIdentifierTypeName = PersonIdentifierType.HEI_NUMBER.shr_name;
                        assigningAuthority = "MCH";
                        break;
                    case PersonIdentifierType.HTS_NUMBER.name:
                        shrIdentifierTypeName = PersonIdentifierType.HTS_NUMBER.shr_name;
                        assigningAuthority = "HTS";
                        break;
                    case PersonIdentifierType.NATIONAL_ID.name:
                        shrIdentifierTypeName = PersonIdentifierType.NATIONAL_ID.shr_name;
                        assigningAuthority = "GK";
                        break;
                    case CONCEPTS.ANC_NUMBER.name:
                        shrIdentifierTypeName = CONCEPTS.ANC_NUMBER.shr_name;
                        assigningAuthority = "ANC";
                        break;
                }
                if(!StringUtils.isEmpty(shrIdentifierTypeName) && !StringUtils.isEmpty(identifier.getIdentifier())){
                    InternalPatientId internalPatientId = new InternalPatientId();
                    internalPatientId.setIdentifierType(shrIdentifierTypeName);
                    internalPatientId.setAssigningAuthority(assigningAuthority);
                    internalPatientId.setID(identifier.getIdentifier());
                    internalPatientId.setAssigningFacility(assigningFacility);
                    internalPatientIds.add(internalPatientId);
                }
            }catch(Exception e){
                Log.e("Kenya EMR Shr","Could not add identifier",e);
            }
        }
        patientIdentification.setInternalPatientIds(internalPatientIds);
        shrModel.setPatientIdentification(patientIdentification);
        return shrModel;
    }

    private static List<PatientIdentifier> extractPatientIdentifiersFromSHRModel(MuzimaApplication muzimaApplication, KenyaEmrSHRModel shrModel){
        List<PatientIdentifier> identifiers = new ArrayList<>();
        try {

            PatientIdentification identification = shrModel.getPatientIdentification();
            PatientIdentifier patientIdentifier;
            //External ID
            ExternalPatientId externalPatientId = identification.getExternalPatientId();
            if(!externalPatientId.lacksMandatoryValues()) {
                patientIdentifier = PatientIdentifierUtils.getOrCreateKenyaEmrIdentifier(muzimaApplication, externalPatientId.getID(),
                        externalPatientId.getIdentifierType(), externalPatientId.getAssigningFacility());
                identifiers.add(patientIdentifier);
            }

            //Internal IDs
            List<InternalPatientId> internalPatientIds = identification.getInternalPatientIds();
            for (InternalPatientId internalPatientId : internalPatientIds) {
                if(!internalPatientId.lacksMandatoryValues()) {
                    patientIdentifier = PatientIdentifierUtils.getOrCreateKenyaEmrIdentifier(muzimaApplication, internalPatientId.getID(),
                            internalPatientId.getIdentifierType(), internalPatientId.getAssigningFacility());
                    identifiers.add(patientIdentifier);
                }
            }

        } catch (Exception e){
            Log.e("KenyaEmrShrMapper","Could not create Kenyaemr identifier",e);
        }

        return identifiers;
    }

    public static void createNewObservationsAndEncountersFromShrModel(MuzimaApplication muzimaApplication, KenyaEmrSHRModel shrModel, final Patient patient)
            throws ShrParseException {
        Log.e("KenyaEmrShrMapper","Saving encounters data ");
        List<String> payloads = createJsonEncounterPayloadFromShrModel(muzimaApplication, shrModel, patient);
        for(final String payload:payloads) {
            Log.e("KenyaEmrShrMapper","Saving payload data ");
            final String newFormDataUuid = UUID.randomUUID().toString();
            HTMLFormObservationCreator htmlFormObservationCreator = new HTMLFormObservationCreator(muzimaApplication);
            htmlFormObservationCreator.createObservationsAndRelatedEntities(payload, newFormDataUuid);

            List<Concept> newConcepts = new ArrayList(htmlFormObservationCreator.getNewConceptList());
            if(!newConcepts.isEmpty()){
                ConceptController conceptController = muzimaApplication.getConceptController();
                try {
                    conceptController.saveConcepts(newConcepts);
                } catch (ConceptController.ConceptSaveException e) {
                    Log.e("ShrMapper","Could not save new Concepts",e);
                }
            }

            Encounter encounter = htmlFormObservationCreator.getEncounter();
            EncounterController encounterController = muzimaApplication.getEncounterController();
            try {
                encounterController.saveEncounter(encounter);
            } catch (EncounterController.SaveEncounterException e) {
                Log.e("ShrMapper","Could not save Encounter",e);
            }

            List<Observation> observations = htmlFormObservationCreator.getObservations();
            ObservationController observationController = muzimaApplication.getObservationController();
            try {
                observationController.saveObservations(observations);
            } catch (ObservationController.SaveObservationException e) {
                Log.e("ShrMapper","Could not save Observations",e);
            }

            final FormData formData = new FormData( ) {{
                setUuid(newFormDataUuid);
                setPatientUuid(patient.getUuid( ));
                setUserUuid("userUuid");
                setStatus(STATUS_COMPLETE);
                setTemplateUuid(StringUtils.defaultString(CONCEPTS.HIV_TESTS.FORM.FORM_UUID));
                setDiscriminator(Constants.FORM_JSON_DISCRIMINATOR_SHR_ENCOUNTER);
                setJsonPayload(payload);
            }};

            FormController formController = muzimaApplication.getFormController();
            try {
                Log.e("KenyaEmrShrMapper","Saving form data ");
                formController.saveFormData(formData);
            } catch (FormController.FormDataSaveException e) {
                Log.e("ShrMapper","Could not save Form Data",e);
            }
        }
    }

    private static List<String> createJsonEncounterPayloadFromShrModel(MuzimaApplication muzimaApplication, KenyaEmrSHRModel shrModel, Patient patient) throws ShrParseException {
        try {
            Log.e("KenyaEmrShrMapper","Obtaining payloads ");

            List<String> encounterPayloads = new ArrayList<>();
            List<HIVTest> hivTests = shrModel.getHivTests();
            List<Immunization> immunizations = shrModel.getImmunizations();
            if(hivTests != null || immunizations != null) {
                Encounters encountersWithObservations = muzimaApplication.getObservationController().getEncountersWithObservations(patient.getUuid());

                List<HIVTest> existingHivTests = new ArrayList<>();
                List<Immunization> existingImmunizations = new ArrayList<>();
                for (EncounterWithObservations encounterWithObservations : encountersWithObservations) {
                    HIVTest hivTest = getHivTestFromEncounter(encounterWithObservations);
                    if(hivTest != null && !hivTest.lacksMandatoryValues()){
                        existingHivTests.add(hivTest);
                    }

                    Immunization immunization = getImmunizationFromEncounter(encounterWithObservations);
                    if(immunization != null && !immunization.lacksMandatoryValues()){
                        existingImmunizations.add(immunization);
                    }
                }

                if (hivTests != null) {
                    for (HIVTest hivTest : hivTests) {
                        if (!hivTest.lacksMandatoryValues()) {
                            boolean testExists = false;
                            for(HIVTest existingHivTest: existingHivTests){
                                if(hivTest.equals(existingHivTest)){
                                    testExists = true;
                                    break;
                                }
                            }

                            if(!testExists) {
                                encounterPayloads.add(createJsonEncounterPayloadFromHivTest(muzimaApplication, hivTest, patient));
                            }
                        }
                    }
                } else {
                    Log.e("KenyaEmrShrMapper", "No HIV Tests found");
                }

                if (immunizations != null) {
                    for (Immunization immunization : immunizations) {
                        if (!immunization.lacksMandatoryValues()) {
                            boolean immunizationExists = false;
                            for(Immunization existingImmunization : existingImmunizations){
                                if(immunization.equals(existingImmunization)){
                                    immunizationExists = true;
                                    break;
                                }
                            }

                            if(!immunizationExists) {
                                encounterPayloads.add(createJsonEncounterPayloadFromImmunization(muzimaApplication, immunization, patient));
                            }
                        }
                    }
                } else {
                    Log.e("KenyaEmrShrMapper", "No Immunizations found");
                }
            }
            return encounterPayloads;
        } catch(ObservationController.LoadObservationException e){
            throw new ShrParseException("Could not load observations",e);
        }
    }

    private static String createJsonEncounterPayloadFromHivTest(MuzimaApplication muzimaApplication, HIVTest hivTest, Patient patient) throws ShrParseException {
        JSONObject encounterJSON = new JSONObject();
        JSONObject patientDetails = new JSONObject();
        JSONObject observationDetails = new JSONObject();
        JSONObject encounterDetails = new JSONObject();

        Log.e("KenyaEmrShrMapper","Processing HIV test ");

        try {
            encounterDetails.put("encounter.provider_id", hivTest.getProviderDetails().getId());
            Location location = LocationUtils.getOrCreateDummyLocationByKenyaEmrMasterFacilityListCode(muzimaApplication, hivTest.getFacility());
            encounterDetails.put("encounter.location_id", location.getId());

            String user_system_id = muzimaApplication.getAuthenticatedUser( ).getSystemId( );
            encounterDetails.put("encounter.user_system_id", user_system_id);

            Date encounterDateTime = DateUtils.parseDateByPattern(hivTest.getDate(), "yyyyMMdd");
            encounterDetails.put("encounter.encounter_datetime", DateUtils.getFormattedDate(encounterDateTime));

            encounterDetails.put("encounter.form_uuid", StringUtils.defaultString(CONCEPTS.HIV_TESTS.FORM.FORM_UUID));
            encounterJSON.put("encounter", encounterDetails);

            patientDetails.put("patient.medical_record_number", StringUtils.defaultString(patient.getIdentifier()));
            patientDetails.put("patient.given_name", StringUtils.defaultString(patient.getGivenName()));
            patientDetails.put("patient.middle_name", StringUtils.defaultString(patient.getMiddleName()));
            patientDetails.put("patient.family_name", StringUtils.defaultString(patient.getFamilyName()));
            patientDetails.put("patient.sex", StringUtils.defaultString(patient.getGender()));
            patientDetails.put("patient.uuid", StringUtils.defaultString(patient.getUuid()));
            if (patient.getBirthdate() != null) {
                patientDetails.put("patient.birth_date", DateUtils.getFormattedDate(patient.getBirthdate()));
            }

            encounterJSON.put("patient", patientDetails);

            //Test Result
            String answer = null;
            String testResult = hivTest.getResult();
            switch (testResult) {
                case CONCEPTS.HIV_TESTS.TEST_RESULT.ANSWERS.POSITIVE.name:
                    answer = CONCEPTS.HIV_TESTS.TEST_RESULT.ANSWERS.POSITIVE.concept_id + "^"
                            + CONCEPTS.HIV_TESTS.TEST_RESULT.ANSWERS.POSITIVE.name + "^" + "99DCT";
                    break;
                case CONCEPTS.HIV_TESTS.TEST_RESULT.ANSWERS.NEGATIVE.name:
                    answer = CONCEPTS.HIV_TESTS.TEST_RESULT.ANSWERS.NEGATIVE.concept_id + "^"
                            + CONCEPTS.HIV_TESTS.TEST_RESULT.ANSWERS.NEGATIVE.name + "^" + "99DCT";
                    break;
                case CONCEPTS.HIV_TESTS.TEST_RESULT.ANSWERS.INCONCLUSIVE.name:
                    answer = CONCEPTS.HIV_TESTS.TEST_RESULT.ANSWERS.INCONCLUSIVE.concept_id + "^"
                            + CONCEPTS.HIV_TESTS.TEST_RESULT.ANSWERS.INCONCLUSIVE.name + "^" + "99DCT";
            }
            if (!StringUtils.isEmpty(answer)) {
                String conceptQuestion = CONCEPTS.HIV_TESTS.TEST_RESULT.concept_id + "^"
                        + CONCEPTS.HIV_TESTS.TEST_RESULT.name + "^" + "99DCT";
                observationDetails.put(conceptQuestion, answer);
            }

            //Test Type
            answer = null;
            String testType = hivTest.getType();
            switch (testType) {
                case CONCEPTS.HIV_TESTS.TEST_TYPE.ANSWERS.SCREENING.name:
                    answer = CONCEPTS.HIV_TESTS.TEST_TYPE.ANSWERS.SCREENING.concept_id + "^"
                            + CONCEPTS.HIV_TESTS.TEST_TYPE.ANSWERS.SCREENING.name + "^" + "99DCT";
                    break;
                case CONCEPTS.HIV_TESTS.TEST_TYPE.ANSWERS.CONFIRMATORY.name:
                    answer = CONCEPTS.HIV_TESTS.TEST_TYPE.ANSWERS.CONFIRMATORY.concept_id + "^"
                            + CONCEPTS.HIV_TESTS.TEST_TYPE.ANSWERS.CONFIRMATORY.name + "^" + "99DCT";
            }
            if (!StringUtils.isEmpty(answer)) {
                String conceptQuestion = CONCEPTS.HIV_TESTS.TEST_TYPE.concept_id + "^"
                        + CONCEPTS.HIV_TESTS.TEST_TYPE.name + "^" + "99DCT";
                observationDetails.put(conceptQuestion, answer);
            }

            //Test Strategy
            answer = null;
            String testStrategy = hivTest.getStrategy();
            switch (testStrategy) {
                case CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.HP.name:
                    answer = CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.HP.concept_id + "^"
                            + CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.HP.name + "^" + "99DCT";
                    break;
                case CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.NP.name:
                    answer = CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.NP.concept_id + "^"
                            + CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.NP.name + "^" + "99DCT";
                    break;
                case CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.VI.name:
                    answer = CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.VI.concept_id + "^"
                            + CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.VI.name + "^" + "99DCT";
                    break;
                case CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.VS.name:
                    answer = CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.VS.concept_id + "^"
                            + CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.VS.name + "^" + "99DCT";
                    break;
                case CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.HB.name:
                    answer = CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.HB.concept_id + "^"
                            + CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.HB.name + "^" + "99DCT";
                    break;
                case CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.MO.name:
                    answer = CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.MO.concept_id + "^"
                            + CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.MO.name + "^" + "99DCT";
                    break;
            }
            if (!StringUtils.isEmpty(answer)) {
                String conceptQuestion = CONCEPTS.HIV_TESTS.TEST_STRATEGY.concept_id + "^"
                        + CONCEPTS.HIV_TESTS.TEST_STRATEGY.name + "^" + "99DCT";
                observationDetails.put(conceptQuestion, answer);
            }

            //Test Facility
            String facility = hivTest.getFacility();
            if (!StringUtils.isEmpty(facility)) {
                String conceptQuestion = CONCEPTS.HIV_TESTS.TEST_FACILITY.concept_id + "^"
                        + CONCEPTS.HIV_TESTS.TEST_FACILITY.name + "^" + "99DCT";
                observationDetails.put(conceptQuestion, facility);
            }

            //Test Details
            ProviderDetails providerDetails = hivTest.getProviderDetails();
            if (providerDetails != null) {
                String conceptQuestion = CONCEPTS.HIV_TESTS.PROVIDER_DETAILS.NAME.concept_id + "^"
                        + CONCEPTS.HIV_TESTS.PROVIDER_DETAILS.NAME.name + "^" + "99DCT";
                observationDetails.put(conceptQuestion, providerDetails.getName());

                conceptQuestion = CONCEPTS.HIV_TESTS.PROVIDER_DETAILS.ID.concept_id + "^"
                        + CONCEPTS.HIV_TESTS.PROVIDER_DETAILS.ID.name + "^" + "99DCT";
                observationDetails.put(conceptQuestion, providerDetails.getId());
            }

            encounterJSON.put("patient", patientDetails);
            encounterJSON.put("observation", observationDetails);
            encounterJSON.put("encounter", encounterDetails);

            return encounterJSON.toString();
        } catch (Exception e){
            throw new ShrParseException(e);
        }
    }
    private static String createJsonEncounterPayloadFromImmunization(MuzimaApplication muzimaApplication, Immunization immunization, Patient patient) throws ShrParseException {
        try {
            JSONObject encounterJSON = new JSONObject();
            JSONObject patientDetails = new JSONObject();
            JSONObject observationDetails = new JSONObject();
            JSONObject encounterDetails = new JSONObject();

            Log.e("KenyaEmrShrMapper", "Processing Immunization ");


            encounterDetails.put("encounter.provider_id", Constants.Shr.KenyaEmr.DEFAULT_SHR_USER.id);
            Location location = LocationUtils.getOrCreateDummyLocationByKenyaEmrMasterFacilityListCode(muzimaApplication, Constants.Shr.KenyaEmr.DEFAULT_SHR_FACILITY.MFL_CODE);
            encounterDetails.put("encounter.location_id", location.getId());

            String user_system_id = muzimaApplication.getAuthenticatedUser( ).getSystemId( );
            encounterDetails.put("encounter.user_system_id", user_system_id);
            encounterDetails.put("encounter.location_id", location.getId());

            Date encounterDateTime = DateUtils.parseDateByPattern(immunization.getDateAdministered(), "yyyyMMdd");
            encounterDetails.put("encounter.encounter_datetime", DateUtils.getFormattedDate(encounterDateTime));

            encounterDetails.put("encounter.form_uuid", StringUtils.defaultString(CONCEPTS.IMMUNIZATION.FORM.FORM_UUID));
            encounterJSON.put("encounter", encounterDetails);

            patientDetails.put("patient.medical_record_number", StringUtils.defaultString(patient.getIdentifier()));
            patientDetails.put("patient.given_name", StringUtils.defaultString(patient.getGivenName()));
            patientDetails.put("patient.middle_name", StringUtils.defaultString(patient.getMiddleName()));
            patientDetails.put("patient.family_name", StringUtils.defaultString(patient.getFamilyName()));
            patientDetails.put("patient.sex", StringUtils.defaultString(patient.getGender()));
            patientDetails.put("patient.uuid", StringUtils.defaultString(patient.getUuid()));
            if (patient.getBirthdate() != null) {
                patientDetails.put("patient.birth_date", DateUtils.getFormattedDate(patient.getBirthdate()));
            }

            encounterJSON.put("patient", patientDetails);

            JSONObject vaccineJson = new JSONObject();

            String answer = null;
            int sequence = -1;
            String vaccine = immunization.getName();
            switch (vaccine) {
                case CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.BCG.name:
                    answer = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.BCG.concept_id + "^"
                            + CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.BCG.name + "^" + "99DCT";
                    break;
                case CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.IPV.name:
                    answer = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.IPV.concept_id + "^"
                            + CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.IPV.name + "^" + "99DCT";
                    sequence = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.IPV.sequence;
                    break;
                case CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.MEASLES6.name:
                    answer = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.MEASLES6.concept_id + "^"
                            + CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.MEASLES6.name + "^" + "99DCT";
                    sequence = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.MEASLES6.sequence;
                    break;
                case CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.MEASLES9.name:
                    answer = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.MEASLES9.concept_id + "^"
                            + CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.MEASLES9.name + "^" + "99DCT";
                    sequence = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.MEASLES9.sequence;
                    break;
                case CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.MEASLES18.name:
                    answer = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.MEASLES18.concept_id + "^"
                            + CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.MEASLES18.name + "^" + "99DCT";
                    sequence = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.MEASLES18.sequence;
                    break;
                case CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.OPV1.name:
                    answer = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.OPV1.concept_id + "^"
                            + CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.OPV1.name + "^" + "99DCT";
                    sequence = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.OPV1.sequence;
                    break;
                case CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.OPV2.name:
                    answer = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.OPV2.concept_id + "^"
                            + CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.OPV2.name + "^" + "99DCT";
                    sequence = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.OPV2.sequence;
                    break;
                case CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.OPV3.name:
                    answer = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.OPV3.concept_id + "^"
                            + CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.OPV3.name + "^" + "99DCT";
                    sequence = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.OPV3.sequence;
                    break;
                case CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.OPV_AT_BIRTH.name:
                    answer = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.OPV_AT_BIRTH.concept_id + "^"
                            + CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.OPV_AT_BIRTH.name + "^" + "99DCT";
                    sequence = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.OPV_AT_BIRTH.sequence;
                    break;
                case CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PCV10_1.name:
                    answer = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PCV10_1.concept_id + "^"
                            + CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PCV10_1.name + "^" + "99DCT";
                    sequence = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PCV10_1.sequence;
                    break;
                case CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PCV10_2.name:
                    answer = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PCV10_2.concept_id + "^"
                            + CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PCV10_2.name + "^" + "99DCT";
                    sequence = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PCV10_2.sequence;
                    break;
                case CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PCV10_3.name:
                    answer = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PCV10_3.concept_id + "^"
                            + CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PCV10_3.name + "^" + "99DCT";
                    sequence = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PCV10_3.sequence;
                    break;
                case CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PENTA1.name:
                    answer = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PENTA1.concept_id + "^"
                            + CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PENTA1.name + "^" + "99DCT";
                    sequence = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PENTA1.sequence;
                    break;
                case CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PENTA2.name:
                    answer = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PENTA2.concept_id + "^"
                            + CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PENTA2.name + "^" + "99DCT";
                    sequence = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PENTA2.sequence;
                    break;
                case CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PENTA3.name:
                    answer = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PENTA3.concept_id + "^"
                            + CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PENTA3.name + "^" + "99DCT";
                    sequence = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PENTA3.sequence;
                    break;
                case CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.ROTA1.name:
                    answer = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.ROTA1.concept_id + "^"
                            + CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.ROTA1.name + "^" + "99DCT";
                    sequence = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.ROTA1.sequence;
                    break;
                case CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.ROTA2.name:
                    answer = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.ROTA2.concept_id + "^"
                            + CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.ROTA2.name + "^" + "99DCT";
                    sequence = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.ROTA2.sequence;
            }
            if (sequence != -1) {
                String conceptQuestion = CONCEPTS.IMMUNIZATION.VACCINE.concept_id + "^"
                        + CONCEPTS.IMMUNIZATION.VACCINE.name + "^" + "99DCT";
                vaccineJson.put(conceptQuestion, answer);
            }
            if (!StringUtils.isEmpty(answer)) {
                String conceptQuestion = CONCEPTS.IMMUNIZATION.VACCINE.concept_id + "^"
                        + CONCEPTS.IMMUNIZATION.VACCINE.name + "^" + "99DCT";
                vaccineJson.put(conceptQuestion, answer);

                String groupConceptQuestion = CONCEPTS.IMMUNIZATION.GROUP.concept_id + "^"
                        + CONCEPTS.IMMUNIZATION.GROUP.name + "^" + "99DCT";
                observationDetails.put(groupConceptQuestion, vaccineJson);
                encounterJSON.put("observation", observationDetails);
            }

            encounterJSON.put("patient", patientDetails);
            encounterJSON.put("encounter", encounterDetails);

            Log.e("KenyaEmrShrMapper", "IMMUNIZATION PAYLOAD: " + encounterJSON.toString());

            return encounterJSON.toString();
        } catch (Exception e){
            throw new ShrParseException(e);
        }
    }

    public static void updateSHRSmartCardRecordForPatient(MuzimaApplication application, String patientUuid) throws ShrParseException {
        try {
            ObservationController observationController = application.getObservationController();
            SmartCardController smartCardController = application.getSmartCardController();
            PatientController patientController = application.getPatientController();
            SmartCardRecord smartCardRecord = smartCardController.getSmartCardRecordByPersonUuid(patientUuid);

            Patient patient = patientController.getPatientByUuid(patientUuid);
            if(patient == null){
                throw new PatientController.PatientLoadException("Could not find patient with Uuid: "+patientUuid);
            }
            if(smartCardRecord == null ){
                throw new ShrParseException("Could not update Shr. Existing Shr not found");
            } else {
                Encounters encountersWithObservations = observationController.getEncountersWithObservations(patient.getUuid());

                if(!encountersWithObservations.isEmpty()) {
                    KenyaEmrSHRModel shrModel = createSHRModelFromJson(smartCardRecord.getPlainPayload());
                    shrModel = addEncounterObservationsToShrModel(shrModel, encountersWithObservations);

                    String jsonShr = KenyaEmrShrMapper.createJsonFromSHRModel(shrModel);
                    smartCardRecord.setPlainPayload(jsonShr);
                    smartCardController.updateSmartCardRecord(smartCardRecord);
                }
            }
        } catch (Throwable e) {
            Log.e("KenyaEmrShrMapper", "Cannot add encounters to patient SHR ",e);
            throw new ShrParseException(e);
        }
    }

    /**
     * Creates a new SHR Model for a given Patient. Iterates through patient demographics, identifiers, addresses and
     * attributes to construct this model
     * @param patient the Patient Object for which to create new SHR
     * @return KenyaEmrSHRModel representation of newlyCreatedSHR
     * @throws IOException
     */
    public static KenyaEmrSHRModel createInitialSHRModelForPatient(MuzimaApplication muzimaApplication, Patient patient, String cardSerialNumber) throws ShrParseException{
        KenyaEmrSHRModel shrModel = createSHRModelFromJson(KenyaEmrSHRModel.newShrModelTemplate);
        // Card details
        CardDetails cardDetails = shrModel.getCardDetails();
        String cardStatus = "ACTIVE";
        String cardReason = "";
        String cardLastUpdated = DateUtils.getFormattedDate(new Date(),"yyyyMMdd");
        String cardLastUpdateFacility = Constants.Shr.KenyaEmr.DEFAULT_SHR_FACILITY.MFL_CODE;

        if(cardDetails == null){
            cardDetails = new CardDetails();
        }
        cardDetails.setStatus(cardStatus);
        cardDetails.setReason(cardReason);
        cardDetails.setLastUpdated(cardLastUpdated);
        cardDetails.setLastUpdatedFacility(cardLastUpdateFacility);

        //Patient identification
        PatientIdentification identification = shrModel.getPatientIdentification();
        if(identification == null){
            identification = new PatientIdentification();
        }
      //Patient name
        PatientName patientName = new PatientName();

        patientName.setFirstName(patient.getFamilyName());
        patientName.setLastName(patient.getGivenName());
        patientName.setMiddleName(patient.getMiddleName());
        identification.setPatientName(patientName);

        //Mother Details
        MotherDetails motherDetails = new MotherDetails();
        //Mother_identifier
        List<MotherIdentifier> motherIdentifiers = motherDetails.getMotherIdentifiers();
        if(motherIdentifiers == null){
            motherIdentifiers = new ArrayList<>();
        }
        //Mother_name
        MotherName motherName = new MotherName();
        motherName.setFirstName("");
        motherName.setMiddleName("");
        motherName.setLastName("");

        motherDetails.setMotherName(motherName);
        motherDetails.setMotherIdentifiers(motherIdentifiers);
        identification.setMotherDetails(motherDetails);

        String dateOfBirth = DateUtils.getFormattedDate(patient.getBirthdate(),"yyyyMMdd");
        identification.setDateOfBirth(dateOfBirth);

        String dateObBirthPrecision = "EXACT";
        if(patient.getBirthdateEstimated()){
            dateObBirthPrecision = "ESTIMATED";
        }
        identification.setDateOfBirthPrecision(dateObBirthPrecision);
        identification.setSex(patient.getGender());
        identification.setMaritalStatus("");
        identification.setPhoneNumber("");
        identification.setDeathDate("");
        identification.setDeathIndicator("N");

        //Patient address
        PatientAddress patientAddress = new PatientAddress();
//        PersonAddress kenyaEmrPersonAddress = null;
//        try{
//            kenyaEmrPersonAddress = patient.getPreferredAddress();
//        } catch(NullPointerException e){
//            Log.e("Kenya EMR Shr","Could not get preferred Address");
//        }
//        if(kenyaEmrPersonAddress == null){
//            List<PersonAddress> kenyaEmrPersonAddresses = patient.getAddresses();
//            if(kenyaEmrPersonAddresses.size() > 0){
//                kenyaEmrPersonAddress = kenyaEmrPersonAddresses.get(0);
//            }
//        }
//        if(kenyaEmrPersonAddress != null){
//            PatientAddress postalAddress = identification.getPatientAddress();
//            if(postalAddress == null){
//                postalAddress = new PatientAddress();
//            }

        PhysicalAddress physicalAddress = new PhysicalAddress();
        if(physicalAddress == null){
            physicalAddress = new PhysicalAddress();
        }
        physicalAddress.setCounty("");
        physicalAddress.setSubcounty("");
        physicalAddress.setWard("");
        physicalAddress.setNearestLandmark("");
        physicalAddress.setVillage("");

        //postalAddress.setPostalAddress(kenyaEmrPersonAddress.getAddress1());

        patientAddress.setPhysicalAddress(physicalAddress);
        patientAddress.setPostalAddress("");
        identification.setPatientAddress(patientAddress);
        //  }

        //add card serial number as identifier
        List<InternalPatientId> internalPatientIds = identification.getInternalPatientIds();
        if(internalPatientIds == null){
            internalPatientIds = new ArrayList<>();
        }

        InternalPatientId internalPatientId = new InternalPatientId();
        internalPatientId.setIdentifierType(PersonIdentifierType.CARD_SERIAL_NUMBER.shr_name);
        internalPatientId.setAssigningAuthority("CARD_REGISTRY");
        internalPatientId.setID(cardSerialNumber);

        Location defaultLocation = null;
        try {
            defaultLocation = LocationUtils.getDefaultEncounterLocationPreference(muzimaApplication);
        } catch (Exception e) {
            Log.e("Kenya EMR SHR Mapper", "Could not get default location",e);
        }
        String assigningFacility = LocationUtils.getKenyaEmrMasterFacilityListCode(defaultLocation);
        if(StringUtils.isEmpty(assigningFacility)) {
            assigningFacility = Constants.Shr.KenyaEmr.DEFAULT_SHR_FACILITY.MFL_CODE;
        }
        internalPatientId.setAssigningFacility(assigningFacility);
        internalPatientIds.add(internalPatientId);

        identification.setInternalPatientIds(internalPatientIds);

        shrModel.setCardDetails(cardDetails);
        shrModel.setPatientIdentification(identification);
        shrModel = putIdentifiersIntoSHRModel(shrModel,patient.getIdentifiers());

        EncounterController encounterController = muzimaApplication.getEncounterController();
        ObservationController observationController = muzimaApplication.getObservationController();
        try {
            List<Encounter> encounters = encounterController.getEncountersByEncounterTypeUuidAndPatientUuid(
                    CONCEPTS.HIV_TESTS.ENCOUNTER.ENCOUNTER_TYPE_UUID, patient.getUuid());
            Encounters encountersWithObservations = new Encounters();
            for(Encounter encounter:encounters) {
                Encounters encounterObs = observationController.getObservationsByEncounterUuid(encounter.getUuid());
                if(!encounters.isEmpty()){
                    encountersWithObservations.addAll(encounterObs);
                }
            }

            if(!encountersWithObservations.isEmpty()){
                shrModel = addEncounterObservationsToShrModel(shrModel,encountersWithObservations);
            }
        } catch (EncounterController.DownloadEncounterException e) {
            Log.e("Kenya EMR Shr","Could not obtain encounterType");
        } catch (ObservationController.LoadObservationException e) {
            Log.e("Kenya EMR Shr","Could not obtain Observations");
        }
        return shrModel;
    }
    /**
     * Creates a new SHR Model for a given Patient. Iterates through patient demographics, identifiers, addresses and
     * attributes to construct this model
     * @param patient the Patient Object for which to create new SHR
     * @return KenyaEmrSHRModel representation of newlyCreatedSHR
     * @throws IOException
     */
    public static KenyaEmrSHRModel createInitialSHRModelForPatient(MuzimaApplication muzimaApplication, Patient patient ) throws ShrParseException{
        KenyaEmrSHRModel shrModel = createSHRModelFromJson(KenyaEmrSHRModel.newShrModelTemplate);
        // Card details
        CardDetails cardDetails = shrModel.getCardDetails();
        String cardStatus = "ACTIVE";
        String cardReason = "";
        String cardLastUpdated = DateUtils.getFormattedDate(new Date(),"yyyyMMdd");
        String cardLastUpdateFacility = Constants.Shr.KenyaEmr.DEFAULT_SHR_FACILITY.MFL_CODE;

        if(cardDetails == null){
            cardDetails = new CardDetails();
        }
        cardDetails.setStatus(cardStatus);
        cardDetails.setReason(cardReason);
        cardDetails.setLastUpdated(cardLastUpdated);
        cardDetails.setLastUpdatedFacility(cardLastUpdateFacility);

        //Patient identification
        PatientIdentification identification = shrModel.getPatientIdentification();
        if(identification == null){
            identification = new PatientIdentification();
        }
       //Patient Name
        PatientName patientName = new PatientName();

        patientName.setFirstName(patient.getFamilyName());
        patientName.setLastName(patient.getGivenName());
        patientName.setMiddleName(patient.getMiddleName());
        identification.setPatientName(patientName);

        //Mother Details
        MotherDetails motherDetails = new MotherDetails();
                    //Mother_identifier
        List<MotherIdentifier> motherIdentifiers = motherDetails.getMotherIdentifiers();
        if(motherIdentifiers == null){
            motherIdentifiers = new ArrayList<>();
        }
                  //Mother_name
        MotherName motherName = new MotherName();
        motherName.setFirstName("");
        motherName.setMiddleName("");
        motherName.setLastName("");

        motherDetails.setMotherName(motherName);
        motherDetails.setMotherIdentifiers(motherIdentifiers);
        identification.setMotherDetails(motherDetails);

        String dateOfBirth = DateUtils.getFormattedDate(patient.getBirthdate(),"yyyyMMdd");
        identification.setDateOfBirth(dateOfBirth);

        String dateObBirthPrecision = "EXACT";
        if(patient.getBirthdateEstimated()){
            dateObBirthPrecision = "ESTIMATED";
        }
        identification.setDateOfBirthPrecision(dateObBirthPrecision);
        identification.setSex(patient.getGender());
        identification.setMaritalStatus("");
        identification.setPhoneNumber("");
        identification.setDeathDate("");
        identification.setDeathIndicator("N");

        //Patient address
        PatientAddress patientAddress = new PatientAddress();
//        PersonAddress kenyaEmrPersonAddress = null;
//        try{
//            kenyaEmrPersonAddress = patient.getPreferredAddress();
//        } catch(NullPointerException e){
//            Log.e("Kenya EMR Shr","Could not get preferred Address");
//        }
//        if(kenyaEmrPersonAddress == null){
//            List<PersonAddress> kenyaEmrPersonAddresses = patient.getAddresses();
//            if(kenyaEmrPersonAddresses.size() > 0){
//                kenyaEmrPersonAddress = kenyaEmrPersonAddresses.get(0);
//            }
//        }
//        if(kenyaEmrPersonAddress != null){
//            PatientAddress postalAddress = identification.getPatientAddress();
//            if(postalAddress == null){
//                postalAddress = new PatientAddress();
//            }

            PhysicalAddress physicalAddress = new PhysicalAddress();
            if(physicalAddress == null){
                physicalAddress = new PhysicalAddress();
            }
            physicalAddress.setCounty("");
            physicalAddress.setSubcounty("");
            physicalAddress.setWard("");
            physicalAddress.setNearestLandmark("");
            physicalAddress.setVillage("");

            //postalAddress.setPostalAddress(kenyaEmrPersonAddress.getAddress1());

            patientAddress.setPhysicalAddress(physicalAddress);
            patientAddress.setPostalAddress("");
            identification.setPatientAddress(patientAddress);
      //  }

        //add card serial number as identifier
        List<InternalPatientId> internalPatientIds = identification.getInternalPatientIds();
        if(internalPatientIds == null){
            internalPatientIds = new ArrayList<>();
        }

        InternalPatientId internalPatientId = new InternalPatientId();
        internalPatientId.setIdentifierType(PersonIdentifierType.CARD_SERIAL_NUMBER.shr_name);
        internalPatientId.setAssigningAuthority("CARD_REGISTRY");
        internalPatientId.setID("");

        Location defaultLocation = null;
        try {
            defaultLocation = LocationUtils.getDefaultEncounterLocationPreference(muzimaApplication);
        } catch (Exception e) {
            Log.e("Kenya EMR Shr", "Could not get default location",e);
        }
        String assigningFacility = LocationUtils.getKenyaEmrMasterFacilityListCode(defaultLocation);
        if(StringUtils.isEmpty(assigningFacility)) {
            assigningFacility = Constants.Shr.KenyaEmr.DEFAULT_SHR_FACILITY.MFL_CODE;
        }
        internalPatientId.setAssigningFacility(assigningFacility);
        internalPatientIds.add(internalPatientId);

        identification.setInternalPatientIds(internalPatientIds);

        shrModel.setCardDetails(cardDetails);
        shrModel.setPatientIdentification(identification);
        shrModel = putIdentifiersIntoSHRModel(shrModel,patient.getIdentifiers());

        EncounterController encounterController = muzimaApplication.getEncounterController();
        ObservationController observationController = muzimaApplication.getObservationController();
        try {
            List<Encounter> encounters = encounterController.getEncountersByEncounterTypeUuidAndPatientUuid(
                    CONCEPTS.HIV_TESTS.ENCOUNTER.ENCOUNTER_TYPE_UUID, patient.getUuid());
            Encounters encountersWithObservations = new Encounters();
            for(Encounter encounter:encounters) {
                Encounters encounterObs = observationController.getObservationsByEncounterUuid(encounter.getUuid());
                if(!encounters.isEmpty()){
                    encountersWithObservations.addAll(encounterObs);
                }
            }

            if(!encountersWithObservations.isEmpty()){
                shrModel = addEncounterObservationsToShrModel(shrModel,encountersWithObservations);
            }
        } catch (EncounterController.DownloadEncounterException e) {
            Log.e("Kenya EMR Shr","Could not obtain encounterType");
        } catch (ObservationController.LoadObservationException e) {
            Log.e("Kenya EMR Shr","Could not obtain Observations");
        }
        return shrModel;
    }

    /**
     * Adds Observations to an SHR model
     * @param shrModel
     * @param encountersWithObservations
     * @return
     * @throws IOException
     */
    private static KenyaEmrSHRModel addEncounterObservationsToShrModel(KenyaEmrSHRModel shrModel, Encounters encountersWithObservations) throws ShrParseException{
        List<HIVTest> hivTests = shrModel.getHivTests() == null ? new ArrayList<HIVTest>() : shrModel.getHivTests();
        List<Immunization> immunizations = shrModel.getImmunizations() == null ? new ArrayList<Immunization>() : shrModel.getImmunizations();
        for(EncounterWithObservations encounterWithObservations: encountersWithObservations){
            HIVTest hivTest = getHivTestFromEncounter(encounterWithObservations);
            if(hivTest != null){
                boolean testExists = false;
                for (HIVTest shrHivTest : hivTests ){
                    if(shrHivTest.equals(hivTest)){
                        testExists = true;
                        break;
                    }
                }
                if(!testExists) {

                    hivTests.add(hivTest);
                }
            } else {
                Immunization immunization = getImmunizationFromEncounter(encounterWithObservations);
                boolean immunizationExists = false;
                if (immunization != null) {
                    for(Immunization shrImmunization: immunizations){
                        if(immunization.equals(shrImmunization)){
                            immunizationExists = true;
                            break;
                        }
                    }
                    if(!immunizationExists)
                        immunizations.add(immunization);
                }
            }
        }
        shrModel.setHivTests(hivTests);
        shrModel.setImmunizations(immunizations);
        return shrModel;
    }

    private static HIVTest getHivTestFromEncounter(EncounterWithObservations encounterWithObservations) throws ShrParseException {
        List<Observation> observations = encounterWithObservations.getObservations();
        HIVTest hivTest = new HIVTest();
        ProviderDetails providerDetails = null;
        boolean isHivEncounter = false;
        for(Observation observation:observations){
            Concept answerConcept;
            String shrAnswer = null;
            switch(observation.getConcept().getId()){
                case CONCEPTS.HIV_TESTS.TEST_RESULT.concept_id:
                    isHivEncounter = true;
                    answerConcept = observation.getValueCoded();
                    if(answerConcept!= null) {
                        switch (answerConcept.getId()) {
                            case CONCEPTS.HIV_TESTS.TEST_RESULT.ANSWERS.POSITIVE.concept_id:
                                shrAnswer = CONCEPTS.HIV_TESTS.TEST_RESULT.ANSWERS.POSITIVE.name;
                                break;
                            case CONCEPTS.HIV_TESTS.TEST_RESULT.ANSWERS.NEGATIVE.concept_id:
                                shrAnswer = CONCEPTS.HIV_TESTS.TEST_RESULT.ANSWERS.NEGATIVE.name;
                                break;
                            case CONCEPTS.HIV_TESTS.TEST_RESULT.ANSWERS.INCONCLUSIVE.concept_id:
                                shrAnswer = CONCEPTS.HIV_TESTS.TEST_RESULT.ANSWERS.INCONCLUSIVE.name;
                                break;
                        }
                        if(!StringUtils.isEmpty(shrAnswer)){
                            hivTest.setResult(shrAnswer);
                        }
                    }
                    break;

                case CONCEPTS.HIV_TESTS.TEST_TYPE.concept_id:
                    isHivEncounter = true;
                    answerConcept = observation.getValueCoded();
                    if(answerConcept!= null) {
                        switch (answerConcept.getId()) {
                            case CONCEPTS.HIV_TESTS.TEST_TYPE.ANSWERS.CONFIRMATORY.concept_id:
                                shrAnswer = CONCEPTS.HIV_TESTS.TEST_TYPE.ANSWERS.CONFIRMATORY.name;
                                break;
                            case CONCEPTS.HIV_TESTS.TEST_TYPE.ANSWERS.SCREENING.concept_id:
                                shrAnswer = CONCEPTS.HIV_TESTS.TEST_TYPE.ANSWERS.SCREENING.name;
                                break;
                        }
                        if(!StringUtils.isEmpty(shrAnswer)){
                            hivTest.setType(shrAnswer);
                        }
                    }
                    break;

                case CONCEPTS.HIV_TESTS.TEST_STRATEGY.concept_id:
                    isHivEncounter = true;
                    answerConcept = observation.getValueCoded();
                    if(answerConcept!= null) {
                        switch (answerConcept.getId()) {
                            case CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.NP.concept_id:
                                shrAnswer = CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.NP.name;
                                break;
                            case CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.HB.concept_id:
                                shrAnswer = CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.HB.name;
                                break;
                            case CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.HP.concept_id:
                                shrAnswer = CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.HP.name;
                                break;
                            case CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.MO.concept_id:
                                shrAnswer = CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.MO.name;
                                break;
                            case CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.VI.concept_id:
                                shrAnswer = CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.VI.name;
                                break;
                            case CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.VS.concept_id:
                                shrAnswer = CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.VS.name;
                                break;
                        }
                        if(!StringUtils.isEmpty(shrAnswer)){
                            hivTest.setStrategy(shrAnswer);
                        }
                    }
                    break;

                case CONCEPTS.HIV_TESTS.TEST_FACILITY.concept_id:
                    hivTest.setFacility(observation.getValueAsString());
                    break;

                case CONCEPTS.HIV_TESTS.PROVIDER_DETAILS.ID.concept_id:
                    if(providerDetails == null){
                        providerDetails = new ProviderDetails();
                    }
                    providerDetails.setId(observation.getValueAsString());
                    break;

                case CONCEPTS.HIV_TESTS.PROVIDER_DETAILS.NAME.concept_id:
                    if(providerDetails == null){
                        providerDetails = new ProviderDetails();
                    }
                    providerDetails.setName(observation.getValueAsString());
                    break;
            }
        }
        if(!isHivEncounter){
            return null;
        }

        if(StringUtils.isEmpty(hivTest.getFacility())){
            Location location = encounterWithObservations.getEncounter().getLocation();
            String facility = LocationUtils.getKenyaEmrMasterFacilityListCode(location);
            if(!StringUtils.isEmpty(facility)) {
                hivTest.setFacility(facility);
            } else {
                hivTest.setFacility(Constants.Shr.KenyaEmr.DEFAULT_SHR_FACILITY.MFL_CODE);
                //throw new ShrParseException("Cannot get Facility MFL code from encounter location");
            }
        }

        if(providerDetails == null){
            providerDetails = new ProviderDetails();
            Person provider = encounterWithObservations.getEncounter().getProvider();
            if(provider != null){
                providerDetails.setId(provider.getMiddleName());
                providerDetails.setName(provider.getFamilyName() + " " + provider.getGivenName());
            }
        }

        if(providerDetails != null){
            hivTest.setProviderDetails(providerDetails);
        }

        Date encounterDate = encounterWithObservations.getEncounter().getEncounterDatetime();
        if(encounterDate != null) {
            String date = DateUtils.getFormattedDate(encounterDate, "yyyyMMdd");
            hivTest.setDate(date);
        } else {
            throw new ShrParseException("Cannot get encounter date from encounter");
        }

        String date = DateUtils.getFormattedDate(encounterWithObservations.getEncounter().getEncounterDatetime(),"yyyyMMdd");
        hivTest.setDate(date);

        return hivTest;
    }

    private static Immunization getImmunizationFromEncounter(EncounterWithObservations encounterWithObservations){
        List<Observation> observations = encounterWithObservations.getObservations();
        Immunization immunization = new Immunization();
        boolean isImmunizationEncounter = false;
        for(Observation observation:observations){
            String answer = null;
            Concept concept = observation.getConcept();
            Concept valueCoded = observation.getValueCoded();
            if(concept.getId() == CONCEPTS.IMMUNIZATION.VACCINE.concept_id && valueCoded!= null) {
                isImmunizationEncounter = true;
                switch (valueCoded.getId()) {
                    case CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.BCG.concept_id:
                        answer = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.BCG.name;
                        break;
                    case CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.IPV.concept_id:
                        answer = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.IPV.name;
                        break;
                    case CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.MEASLES6.concept_id:
                        answer = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.MEASLES6.name;
                        break;
                    case CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.MEASLES9.concept_id:
                        answer = valueCoded.getName();
                        break;
                    case CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.OPV2.concept_id:
                        answer = CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.IPV.name;
                        break;
                    case CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PCV10_1.concept_id:
                        answer = valueCoded.getName();
                        break;
                    case CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.PENTA1.concept_id:
                        answer = valueCoded.getName();
                        break;
                    case CONCEPTS.IMMUNIZATION.VACCINE.ANSWERS.ROTA1.concept_id:
                        answer = valueCoded.getName();
                }
                if(!StringUtils.isEmpty(answer)){
                    immunization.setName(answer);
                    Date obsDateTime = observation.getObservationDatetime();
                    String dateAdministered = DateUtils.getFormattedDate(obsDateTime, "yyyyMMdd");
                    immunization.setDateAdministered(dateAdministered);
                    break;
                }
            }
        }
        if(!isImmunizationEncounter){
            return null;
        }
        return immunization;
    }


    public static class ShrParseException extends Throwable {
        ShrParseException(Throwable throwable) {
            super(throwable);
        }
        ShrParseException(String message) {
            super(message);
        }
        ShrParseException(String message, Throwable e) {
            super(message,e);
        }
    }
}
