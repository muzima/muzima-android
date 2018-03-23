package com.muzima.utils.smartcard;

import android.util.Log;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muzima.MuzimaApplication;
import com.muzima.api.model.Concept;
import com.muzima.api.model.ConceptName;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.FormData;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PatientIdentifier;
import com.muzima.api.model.PatientIdentifierType;
import com.muzima.api.model.PersonAddress;
import com.muzima.api.model.PersonName;
import com.muzima.api.model.SmartCardRecord;
import com.muzima.controller.ConceptController;
import com.muzima.controller.EncounterController;
import com.muzima.controller.FormController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.PatientController;
import com.muzima.controller.SmartCardController;
import com.muzima.model.observation.EncounterWithObservations;
import com.muzima.model.observation.Encounters;
import com.muzima.model.shr.kenyaemr.ExternalPatientId;
import com.muzima.model.shr.kenyaemr.HIVTest;
import com.muzima.model.shr.kenyaemr.Immunization;
import com.muzima.model.shr.kenyaemr.InternalPatientId;
import com.muzima.model.shr.kenyaemr.KenyaEmrShrModel;
import com.muzima.model.shr.kenyaemr.PatientAddress;
import com.muzima.model.shr.kenyaemr.PatientIdentification;
import com.muzima.model.shr.kenyaemr.PatientName;
import com.muzima.model.shr.kenyaemr.PhysicalAddress;
import com.muzima.model.shr.kenyaemr.ProviderDetails;
import com.muzima.service.HTMLFormObservationCreator;
import com.muzima.utils.Constants;
import com.muzima.utils.Constants.Shr.KenyaEmr.IdentifierType;
import com.muzima.utils.Constants.Shr.KenyaEmr.CONCEPTS;
import com.muzima.utils.DateUtils;
import com.muzima.utils.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.muzima.utils.Constants.STATUS_COMPLETE;

public class KenyaEmrShrMapper {
    private static final String TAG = KenyaEmrShrMapper.class.getSimpleName();

    /**
     * Converts an SHR model from JSON representation to KenyaEmrShrModel
     * @param jsonSHRModel the JSON representation of the SHR model
     * @return Representation of the JSON input as KenyaEmrShrModel object
     * @throws IOException
     */
    public static KenyaEmrShrModel createSHRModelFromJson(String jsonSHRModel) throws ShrParseException {
        ObjectMapper objectMapper = new ObjectMapper();
        KenyaEmrShrModel shrModel = null;
        try {
            shrModel = objectMapper.readValue(jsonSHRModel,KenyaEmrShrModel.class);
        } catch (IOException e) {
            throw new ShrParseException(e);
        }
        return shrModel;
    }

    /**
     * Converts a KenyaEmrShrModel representation of SHR to JSON representation
     * @param shrModel the KenyaEmrShrModel Object representation of the SHR model
     * @return JSON representation of SHR model
     * @throws IOException
     */
    public static String createJsonFromSHRModel(KenyaEmrShrModel shrModel) throws ShrParseException{
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(shrModel);
        } catch (IOException e) {
            throw new ShrParseException(e);
        }
    }

    /**
     * Extracts a Patient object from a JSON SHR model
     * @param shrModel the JSON representation of the SHR model
     * @return Patient object extracted from SHR model
     * @throws IOException
     */
    public static Patient extractPatientFromShrModel(String shrModel) throws ShrParseException{
        KenyaEmrShrModel kenyaEmrShrModel = createSHRModelFromJson(shrModel);
        return extractPatientFromShrModel(kenyaEmrShrModel);
    }

    /**
     * Extracts a Patient Object from a KenyaEmrShrModel Object of SHR model
     * @param shrModel the KenyaEmrShrModel Object representation of the SHR model
     * @return Patient object extracted from SHR model
     * @throws IOException
     */
    public static Patient extractPatientFromShrModel(KenyaEmrShrModel shrModel) throws ShrParseException{
        try {
            Patient patient = new Patient();
            PatientIdentification identification = shrModel.getPatientIdentification();
            List<PersonName> names = extractPatientNamesFromShrModel(shrModel);
            patient.setNames(names);

            //set Identifiers
            List<PatientIdentifier> identifiers = extractPatientIdentifiersFromShrModel(shrModel);
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

    /**
     *
     * @param shrModel
     * @return
     */
    public static List<PersonName> extractPatientNamesFromShrModel(KenyaEmrShrModel shrModel) throws ShrParseException {
        PatientIdentification identification = shrModel.getPatientIdentification();
        if(identification != null && identification.getPatientName()!= null) {
            final PersonName personName = new PersonName();
            personName.setFamilyName(identification.getPatientName().getFirstName());
            personName.setGivenName(identification.getPatientName().getLastName());
            personName.setMiddleName(identification.getPatientName().getMiddleName());

            List<PersonName> names = new ArrayList<PersonName>(){{
                add(personName);
            }};

            return names;
        } else {
            throw new ShrParseException("Could not find patient names");
        }
    }

    public static KenyaEmrShrModel putIdentifiersIntoShrModel(KenyaEmrShrModel shrModel,List<PatientIdentifier> identifiers ){
        PatientIdentification patientIdentification = shrModel.getPatientIdentification();
        if(patientIdentification == null){
            patientIdentification = new PatientIdentification();
        }

        List<InternalPatientId> internalPatientIds = patientIdentification.getInternalPatientIds();
        if(internalPatientIds == null){
            internalPatientIds = new ArrayList<>();
        }

        String assigningFacility = "10829";

        for(PatientIdentifier identifier:identifiers){

            try {
                String shrIdentifierTypeName = null;
                String assigningAuthority = null;

                switch (identifier.getIdentifierType().getName()) {
                    case IdentifierType.GODS_NUMBER.name:
                        ExternalPatientId externalPatientId = patientIdentification.getExternalPatientId();
                        if(externalPatientId == null){
                            externalPatientId = new ExternalPatientId();
                        }
                        externalPatientId.setIdentifierType(IdentifierType.GODS_NUMBER.shr_name);
                        externalPatientId.setID(identifier.getIdentifier());
                        externalPatientId.setAssigningAuthority("MPI");
                        externalPatientId.setAssigningFacility(assigningFacility);
                        patientIdentification.setExternalPatientId(externalPatientId);
                        break;
                    case IdentifierType.CARD_SERIAL_NUMBER.name:
                        shrIdentifierTypeName = IdentifierType.CARD_SERIAL_NUMBER.shr_name;
                        assigningAuthority = "CARD_REGISTRY";
                        break;
                    case IdentifierType.CCC_NUMBER.name:
                        shrIdentifierTypeName = IdentifierType.CCC_NUMBER.shr_name;
                        assigningAuthority = "CCC";
                        break;
                    case IdentifierType.HEI_NUMBER.name:
                        shrIdentifierTypeName = IdentifierType.HEI_NUMBER.shr_name;
                        assigningAuthority = "MCH";
                        break;
                    case IdentifierType.HTS_NUMBER.name:
                        shrIdentifierTypeName = IdentifierType.HTS_NUMBER.shr_name;
                        assigningAuthority = "HTS";
                        break;
                    case IdentifierType.NATIONAL_ID.name:
                        shrIdentifierTypeName = IdentifierType.NATIONAL_ID.shr_name;
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
                Log.e(TAG,"Could not add identifier",e);
            }
        }
        patientIdentification.setInternalPatientIds(internalPatientIds);
        shrModel.setPatientIdentification(patientIdentification);
        return shrModel;
    }

    public static List<PatientIdentifier> extractPatientIdentifiersFromShrModel(KenyaEmrShrModel shrModel){
        List<PatientIdentifier> identifiers = new ArrayList<PatientIdentifier>();
        PatientIdentification identification = shrModel.getPatientIdentification();
        //External ID
        ExternalPatientId externalPatientId = identification.getExternalPatientId();
        PatientIdentifier patientIdentifier = new PatientIdentifier();
        PatientIdentifierType identifierType = new PatientIdentifierType();

        if(!StringUtils.isEmpty(externalPatientId.getIdentifierType()) && !StringUtils.isEmpty(externalPatientId.getID())) {
            identifierType.setName(externalPatientId.getIdentifierType());
            patientIdentifier.setIdentifierType(identifierType);
            patientIdentifier.setIdentifier(externalPatientId.getID());
            identifiers.add(patientIdentifier);
        }

        //Internal IDs
        List<InternalPatientId> internalPatientIds = identification.getInternalPatientIds();
        for (InternalPatientId internalPatientId : internalPatientIds) {
            patientIdentifier = new PatientIdentifier();
            identifierType = new PatientIdentifierType();
            String identifierTypeName = null;
            switch (internalPatientId.getIdentifierType()){
                case IdentifierType.CARD_SERIAL_NUMBER.shr_name:
                    identifierTypeName = IdentifierType.CARD_SERIAL_NUMBER.name;
                    break;
                case IdentifierType.CCC_NUMBER.shr_name:
                    identifierTypeName = IdentifierType.CCC_NUMBER.name;
                    break;
                case IdentifierType.GODS_NUMBER.shr_name:
                    identifierTypeName = IdentifierType.GODS_NUMBER.name;
                    break;
                case IdentifierType.HEI_NUMBER.shr_name:
                    identifierTypeName = IdentifierType.HEI_NUMBER.name;
                    break;
                case IdentifierType.HTS_NUMBER.shr_name:
                    identifierTypeName = IdentifierType.HTS_NUMBER.name;
                    break;
                case IdentifierType.NATIONAL_ID.shr_name:
                    identifierTypeName = IdentifierType.NATIONAL_ID.name;
                    break;
                case CONCEPTS.ANC_NUMBER.shr_name:
                    identifierTypeName = CONCEPTS.ANC_NUMBER.name;
                    break;
            }
            if(!StringUtils.isEmpty(identifierTypeName) && !StringUtils.isEmpty(internalPatientId.getID())) {
                identifierType.setName(identifierTypeName);
                patientIdentifier.setIdentifierType(identifierType);
                patientIdentifier.setIdentifier(internalPatientId.getID());
                identifiers.add(patientIdentifier);
                Log.e("KenyaEmrShrMapper","Added determine Kenyaemr identifier name for "+identifierTypeName+ " = "+patientIdentifier.getIdentifier() );
            } else {
                Log.e("KenyaEmrShrMapper","Could not determine Kenyaemr identifier name for "
                        + internalPatientId.getIdentifierType());
            }
        }
        return identifiers;
    }

    /**
     * Extracts Observations List from a JSON representation of the SHR model
     * @param shrModel the JSON representation of the SHR model
     * @return Observations List extracted from SHR model
     * @throws IOException
     */
    public static List<Observation> extractObservationsFromShrModel(String shrModel) throws ShrParseException{
        KenyaEmrShrModel kenyaEmrShrModel = createSHRModelFromJson(shrModel);
        return extractObservationsFromShrModel(shrModel);
    }

    public static void createNewObservationsAndEncountersFromShrModel(MuzimaApplication muzimaApplication, KenyaEmrShrModel shrModel, final Patient patient)
            throws ShrParseException {
        Log.e("KenyaEmrShrMapper","Saving encounters data ");
        List<String> payloads = createJsonEncounterPayloadFromShrModel(shrModel, patient);
        for(final String payload:payloads) {
            Log.e("KenyaEmrShrMapper","Saving payload data ");
            final String newFormDataUuid = UUID.randomUUID().toString();
            HTMLFormObservationCreator htmlFormObservationCreator = new HTMLFormObservationCreator(muzimaApplication);
            htmlFormObservationCreator.createObservationsAndRelatedEntities(payload, newFormDataUuid);

            List<Concept> newConcepts = new ArrayList();
            newConcepts.addAll(htmlFormObservationCreator.getNewConceptList());
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
                setDiscriminator(Constants.FORM_JSON_DISCRIMINATOR_ENCOUNTER);
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

    public static List<String> createJsonEncounterPayloadFromShrModel(KenyaEmrShrModel shrModel, Patient patient) throws ShrParseException {
        try {
            Log.e("KenyaEmrShrMapper","Obtaining payloads ");
            List<String> encounters = new ArrayList<>();
            List<HIVTest> hivTests = shrModel.getHivTests();
            if(hivTests != null) {
                for (HIVTest hivTest : hivTests) {
                    encounters.add(createJsonEncounterPayloadFromHivTest(hivTest, patient));
                }
            } else {
                Log.e("KenyaEmrShrMapper","No HIV Tests found");
            }

            List<Immunization> immunizations = shrModel.getImmunizations();
            if(immunizations != null) {
                for (Immunization immunization : immunizations) {
                    encounters.add(createJsonEncounterPayloadFromImmunization(immunization, patient));
                }
            } else {
                Log.e("KenyaEmrShrMapper","No Immunizations found");
            }
            return encounters;
        } catch(ParseException e){
            throw new ShrParseException("Could not parse SHR model",e);
        } catch(JSONException e){
            throw new ShrParseException("Could not parse SHR model",e);
        }
    }

    public static String createJsonEncounterPayloadFromHivTest(HIVTest hivTest, Patient patient) throws JSONException, ParseException {
        JSONObject encounterJSON = new JSONObject();
        JSONObject patientDetails = new JSONObject();
        JSONObject observationDetails = new JSONObject();
        JSONObject encounterDetails = new JSONObject();

        Log.e("KenyaEmrShrMapper","Processing HIV test ");

        encounterDetails.put("encounter.provider_id", hivTest.getProviderDetails().getId());
        encounterDetails.put("encounter.location_id", hivTest.getFacility());

        Date encounterDateTime = DateUtils.parseDateByPattern(hivTest.getDate(), "yyyyMMdd");
        encounterDetails.put("encounter.encounter_datetime", DateUtils.getFormattedDate(encounterDateTime));

        encounterDetails.put("encounter.form_uuid", StringUtils.defaultString(CONCEPTS.HIV_TESTS.FORM.FORM_UUID));
        encounterJSON.put("encounter",encounterDetails);

        patientDetails.put("patient.medical_record_number", StringUtils.defaultString(patient.getIdentifier()));
        patientDetails.put("patient.given_name", StringUtils.defaultString(patient.getGivenName()));
        patientDetails.put("patient.middle_name", StringUtils.defaultString(patient.getMiddleName()));
        patientDetails.put("patient.family_name", StringUtils.defaultString(patient.getFamilyName()));
        patientDetails.put("patient.sex", StringUtils.defaultString(patient.getGender()));
        patientDetails.put("patient.uuid", StringUtils.defaultString(patient.getUuid()));
        if (patient.getBirthdate() != null) {
            patientDetails.put("patient.birth_date", DateUtils.getFormattedDate(patient.getBirthdate()));
        }

        encounterJSON.put("patient",patientDetails);

        //Test Result
        String answer = null;
        String testResult = hivTest.getResult();
        switch (testResult){
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
        if(!StringUtils.isEmpty(answer)){
            String conceptQuestion = CONCEPTS.HIV_TESTS.TEST_RESULT.concept_id + "^"
                    + CONCEPTS.HIV_TESTS.TEST_RESULT.name + "^" + "99DCT";
            observationDetails.put(conceptQuestion, answer);
        }

        //Test Type
        answer = null;
        String testType = hivTest.getType();
        switch (testType){
            case CONCEPTS.HIV_TESTS.TEST_TYPE.ANSWERS.SCREENING.name:
                answer = CONCEPTS.HIV_TESTS.TEST_TYPE.ANSWERS.SCREENING.concept_id + "^"
                        + CONCEPTS.HIV_TESTS.TEST_TYPE.ANSWERS.SCREENING.name + "^" + "99DCT";
                break;
            case CONCEPTS.HIV_TESTS.TEST_TYPE.ANSWERS.CONFIRMATORY.name:
                answer = CONCEPTS.HIV_TESTS.TEST_TYPE.ANSWERS.CONFIRMATORY.concept_id + "^"
                        + CONCEPTS.HIV_TESTS.TEST_TYPE.ANSWERS.CONFIRMATORY.name + "^" + "99DCT";
        }
        if(!StringUtils.isEmpty(answer)){
            String conceptQuestion = CONCEPTS.HIV_TESTS.TEST_TYPE.concept_id + "^"
                    + CONCEPTS.HIV_TESTS.TEST_TYPE.name + "^" + "99DCT";
            observationDetails.put(conceptQuestion, answer);
        }

        //Test Strategy
        answer = null;
        String testStrategy = hivTest.getStrategy();
        switch (testStrategy){
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
        if(!StringUtils.isEmpty(answer)){
            String conceptQuestion = CONCEPTS.HIV_TESTS.TEST_STRATEGY.concept_id + "^"
                    + CONCEPTS.HIV_TESTS.TEST_STRATEGY.name + "^" + "99DCT";
            observationDetails.put(conceptQuestion, answer);
        }

        //Test Facility
        String facility = hivTest.getFacility();
        if(!StringUtils.isEmpty(facility)){
            String conceptQuestion = CONCEPTS.HIV_TESTS.TEST_FACILITY.concept_id + "^"
                    + CONCEPTS.HIV_TESTS.TEST_FACILITY.name + "^" + "99DCT";
            observationDetails.put(conceptQuestion, facility);
        }

        //Test Details
        ProviderDetails providerDetails = hivTest.getProviderDetails();
        if(providerDetails != null){
            String conceptQuestion = CONCEPTS.HIV_TESTS.PROVIDER_DETAILS.NAME.concept_id + "^"
                    + CONCEPTS.HIV_TESTS.PROVIDER_DETAILS.NAME.name + "^" + "99DCT";
            observationDetails.put(conceptQuestion, providerDetails.getName());

            conceptQuestion = CONCEPTS.HIV_TESTS.PROVIDER_DETAILS.ID.concept_id + "^"
                    + CONCEPTS.HIV_TESTS.PROVIDER_DETAILS.ID.name + "^" + "99DCT";
            observationDetails.put(conceptQuestion, providerDetails.getId());
        }

        encounterJSON.put("patient",patientDetails);
        encounterJSON.put("observation",observationDetails);
        encounterJSON.put("encounter",encounterDetails);

        return encounterJSON.toString();
    }
    public static String createJsonEncounterPayloadFromImmunization(Immunization immunization, Patient patient) throws JSONException, ParseException{
        JSONObject encounterJSON = new JSONObject();
        JSONObject patientDetails = new JSONObject();
        JSONObject observationDetails = new JSONObject();
        JSONObject encounterDetails = new JSONObject();

        Log.e("KenyaEmrShrMapper","Processing Immunization ");


        encounterDetails.put("encounter.provider_id", "SHR_USER");
        encounterDetails.put("encounter.location_id", "SHR_FACILITY");

        Date encounterDateTime = DateUtils.parseDateByPattern(immunization.getDateAdministered(), "yyyyMMdd");
        encounterDetails.put("encounter.encounter_datetime", DateUtils.getFormattedDate(encounterDateTime));

        encounterDetails.put("encounter.form_uuid", StringUtils.defaultString(CONCEPTS.IMMUNIZATION.FORM.FORM_UUID));
        encounterJSON.put("encounter",encounterDetails);

        patientDetails.put("patient.medical_record_number", StringUtils.defaultString(patient.getIdentifier()));
        patientDetails.put("patient.given_name", StringUtils.defaultString(patient.getGivenName()));
        patientDetails.put("patient.middle_name", StringUtils.defaultString(patient.getMiddleName()));
        patientDetails.put("patient.family_name", StringUtils.defaultString(patient.getFamilyName()));
        patientDetails.put("patient.sex", StringUtils.defaultString(patient.getGender()));
        patientDetails.put("patient.uuid", StringUtils.defaultString(patient.getUuid()));
        if (patient.getBirthdate() != null) {
            patientDetails.put("patient.birth_date", DateUtils.getFormattedDate(patient.getBirthdate()));
        }

        encounterJSON.put("patient",patientDetails);

        JSONObject vaccineJson = new JSONObject();

        String answer = null;
        int sequence = -1;
        String vaccine = immunization.getName();
        switch (vaccine){
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
        if(sequence != -1){
            String conceptQuestion = CONCEPTS.IMMUNIZATION.VACCINE.concept_id + "^"
                    + CONCEPTS.IMMUNIZATION.VACCINE.name + "^" + "99DCT";
            vaccineJson.put(conceptQuestion, answer);
        }
        if(!StringUtils.isEmpty(answer)){
            String conceptQuestion = CONCEPTS.IMMUNIZATION.VACCINE.concept_id + "^"
                    + CONCEPTS.IMMUNIZATION.VACCINE.name + "^" + "99DCT";
            vaccineJson.put(conceptQuestion, answer);

            String groupConceptQuestion = CONCEPTS.IMMUNIZATION.GROUP.concept_id + "^"
                    + CONCEPTS.IMMUNIZATION.GROUP.name + "^" + "99DCT";
            observationDetails.put(groupConceptQuestion,vaccineJson);
            encounterJSON.put("observation",observationDetails);
        }

        encounterJSON.put("patient",patientDetails);
        encounterJSON.put("encounter",encounterDetails);

        return encounterJSON.toString();
    }

    public static void updateSHRSmartCardRecordForPatient(MuzimaApplication application, String patientUuid){
        try {
            ObservationController observationController = application.getObservationController();
            SmartCardController smartCardController = application.getSmartCardController();
            PatientController patientController = application.getPatientController();
            SmartCardRecord smartCardRecord = smartCardController.getSmartCardRecordByPersonUuid(patientUuid);

            Patient patient = patientController.getPatientByUuid(patientUuid);
            if(smartCardRecord == null && patient != null){
                KenyaEmrShrModel shrModel = KenyaEmrShrMapper.createInitialSHRModelForPatient(patient);
                String jsonShr = KenyaEmrShrMapper.createJsonFromSHRModel(shrModel);
                smartCardRecord = new SmartCardRecord();
                smartCardRecord.setPlainPayload(jsonShr);
                smartCardRecord.setPersonUuid(patient.getUuid());
                smartCardRecord.setUuid(UUID.randomUUID().toString());
                smartCardController.updateSmartCardRecord(smartCardRecord);
            } else {
                Encounters encountersWithObservations = observationController.getEncountersWithObservations(patient.getUuid());
                KenyaEmrShrModel shrModel = KenyaEmrShrMapper.createSHRModelFromJson(smartCardRecord.getPlainPayload());
                shrModel = KenyaEmrShrMapper.addEncounterObservationsToShrModel(shrModel, encountersWithObservations);
                String jsonShr = KenyaEmrShrMapper.createJsonFromSHRModel(shrModel);
                smartCardRecord.setPlainPayload(jsonShr);
                smartCardController.updateSmartCardRecord(smartCardRecord);
            }
        } catch (KenyaEmrShrMapper.ShrParseException e) {
            Log.e(TAG, "Cannot get HIV Encounter");
        } catch (SmartCardController.SmartCardRecordFetchException e) {
            Log.e(TAG, "Cannot get HIV Encounter");
        } catch (SmartCardController.SmartCardRecordSaveException e) {
            Log.e(TAG, "Cannot get HIV Encounter");
        } catch (ObservationController.LoadObservationException e) {
            Log.e(TAG, "Cannot get HIV Encounter");
        } catch (PatientController.PatientLoadException e) {
            Log.e(TAG, "Cannot get HIV Encounter");
        }
    }

    /**
     * Creates a new SHR Model for a given Patient. Iterates through patient demographics, identifiers, addresses and
     * attributes to construct this model
     * @param patient the Patient Object for which to create new SHR
     * @return KenyaEmrShrModel representation of newlyCreatedSHR
     * @throws IOException
     */
    public static KenyaEmrShrModel createInitialSHRModelForPatient(Patient patient) throws ShrParseException{
        KenyaEmrShrModel shrModel = new KenyaEmrShrModel();
        PatientIdentification identification = new PatientIdentification();

        PatientName patientName = new PatientName();

        patientName.setFirstName(patient.getFamilyName());
        patientName.setLastName(patient.getGivenName());
        patientName.setMiddleName(patient.getMiddleName());
        identification.setPatientName(patientName);

        String dateOfBirth = DateUtils.getFormattedDate(patient.getBirthdate(),"yyyyMMdd");
        identification.setDateOfBirth(dateOfBirth);

        String dateObBirthPrecision = "EXACT";
        if(patient.getBirthdateEstimated()){
            dateObBirthPrecision = "ESTIMATED";
        }
        identification.setDateOfBirthPrecision(dateObBirthPrecision);

        PersonAddress kenyaEmrPersonAddress = patient.getPreferredAddress();
        if(kenyaEmrPersonAddress == null){
            List<PersonAddress> kenyaEmrPersonAddresses = patient.getAddresses();
            if(kenyaEmrPersonAddresses.size() > 0){
                kenyaEmrPersonAddress = kenyaEmrPersonAddresses.get(0);
            }
        }
        if(kenyaEmrPersonAddress != null){
            PatientAddress shrAddress = identification.getPatientAddress();
            if(shrAddress == null){
                shrAddress = new PatientAddress();
            }

            PhysicalAddress physicalAddress = shrAddress.getPhysicalAddress();
            if(physicalAddress == null){
                physicalAddress = new PhysicalAddress();
            }

            physicalAddress.setCounty(kenyaEmrPersonAddress.getCountry());
            physicalAddress.setSubcounty(kenyaEmrPersonAddress.getCountyDistrict());
            physicalAddress.setWard(kenyaEmrPersonAddress.getAddress4());
            physicalAddress.setNearestLandmark(kenyaEmrPersonAddress.getAddress2());
            physicalAddress.setVillage(kenyaEmrPersonAddress.getCityVillage());
            shrAddress.setPostalAddress(kenyaEmrPersonAddress.getAddress1());
            identification.setPatientAddress(shrAddress);
        }

        shrModel = putIdentifiersIntoShrModel(shrModel,patient.getIdentifiers());
        EncounterController encounterController = null;
        ObservationController observationController = null;
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
            Log.e(TAG,"Could not obtain encounterType");
        } catch (ObservationController.LoadObservationException e) {
            Log.e(TAG,"Could not obtain Observations");
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
    public static KenyaEmrShrModel addEncounterObservationsToShrModel(KenyaEmrShrModel shrModel, Encounters encountersWithObservations ) throws ShrParseException{
        List<HIVTest> hivTests = shrModel.getHivTests() == null ? new ArrayList<HIVTest>() : shrModel.getHivTests();
        List<Immunization> immunizations = shrModel.getImmunizations() == null ? new ArrayList<Immunization>() : shrModel.getImmunizations();
        for(EncounterWithObservations encounterWithObservations: encountersWithObservations){
            Encounter encounter = encounterWithObservations.getEncounter();
            if(encounter.getEncounterType().getUuid().equalsIgnoreCase(CONCEPTS.HIV_TESTS.ENCOUNTER.ENCOUNTER_TYPE_UUID)){
                HIVTest hivTest = getHivTestFromEncounter(encounterWithObservations);
                if(hivTest != null){
                    hivTests.add(hivTest);
                }
            } else if(encounter.getEncounterType().getUuid().equalsIgnoreCase(CONCEPTS.IMMUNIZATION.ENCOUNTER.ENCOUNTER_TYPE_UUID)){
                Immunization immunization = getImmunizationFromEncounter(encounterWithObservations);
                if(immunization != null){
                    immunizations.add(immunization);
                }
            }
        }
        shrModel.setHivTests(hivTests);
        shrModel.setImmunizations(immunizations);
        return shrModel;
    }

    public static HIVTest getHivTestFromEncounter(EncounterWithObservations encounterWithObservations){
        List<Observation> observations = encounterWithObservations.getObservations();
        HIVTest hivTest = new HIVTest();
        ProviderDetails providerDetails = null;
        for(Observation observation:observations){
            Concept answerConcept = null;
            String shrAnswer = null;
            switch(observation.getConcept().getId()){
                case CONCEPTS.HIV_TESTS.TEST_RESULT.concept_id:
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
                                shrAnswer = CONCEPTS.HIV_TESTS.TEST_STRATEGY.ANSWERS.HB.name;
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
        if(providerDetails != null){
            hivTest.setProviderDetails(providerDetails);
        }

        return hivTest;
    }

    public static Immunization getImmunizationFromEncounter(EncounterWithObservations encounterWithObservations){
        List<Observation> observations = encounterWithObservations.getObservations();
        Immunization immunization = new Immunization();
        for(Observation observation:observations){
            String answer = null;
            Concept concept = observation.getConcept();
            Concept valueCoded = observation.getValueCoded();
            if(concept.getId() == CONCEPTS.IMMUNIZATION.VACCINE.concept_id && valueCoded!= null) {
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
