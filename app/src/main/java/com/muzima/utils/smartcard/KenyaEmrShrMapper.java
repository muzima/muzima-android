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
import com.muzima.api.model.PersonName;
import com.muzima.controller.ConceptController;
import com.muzima.controller.EncounterController;
import com.muzima.controller.FormController;
import com.muzima.controller.ObservationController;
import com.muzima.model.shr.kenyaemr.ExternalPatientId;
import com.muzima.model.shr.kenyaemr.HIVTest;
import com.muzima.model.shr.kenyaemr.InternalPatientId;
import com.muzima.model.shr.kenyaemr.KenyaEmrShrModel;
import com.muzima.model.shr.kenyaemr.PatientIdentification;
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
import static com.muzima.utils.Constants.STATUS_INCOMPLETE;

public class KenyaEmrShrMapper {

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
            patient.setIdentifiers(identifiers);

            //date of birth
            Date dob = DateUtils.parseDateByPattern(identification.getDateOfBirth(),"yyyymmdd");
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
    public static List<PersonName> extractPatientNamesFromShrModel(KenyaEmrShrModel shrModel){
        PatientIdentification identification = shrModel.getPatientIdentification();
        final PersonName personName = new PersonName();
        personName.setFamilyName(identification.getPatientName().getFirstName());
        personName.setGivenName(identification.getPatientName().getLastName());
        personName.setMiddleName(identification.getPatientName().getMiddleName());

        List<PersonName> names = new ArrayList<PersonName>(){{
            add(personName);
        }};

        return names;
    }

    public static List<PatientIdentifier> extractPatientIdentifiersFromShrModel(KenyaEmrShrModel shrModel){
        List<PatientIdentifier> identifiers = new ArrayList<PatientIdentifier>();
        PatientIdentification identification = shrModel.getPatientIdentification();
        //External ID
        ExternalPatientId externalPatientId = identification.getExternalPatientId();
        PatientIdentifier patientIdentifier = new PatientIdentifier();
        PatientIdentifierType identifierType = new PatientIdentifierType();
        identifierType.setName(externalPatientId.getIdentifierType());
        patientIdentifier.setIdentifierType(identifierType);
        patientIdentifier.setIdentifier(externalPatientId.getID());
        identifiers.add(patientIdentifier);

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
            if(identifierTypeName != null) {
                identifierType.setName(identifierTypeName);
                patientIdentifier.setIdentifierType(identifierType);
                patientIdentifier.setIdentifier(internalPatientId.getID());
                identifiers.add(patientIdentifier);
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
            throws ShrParseException, ShrSaveException {
        List<String> payloads = createJsonEncounterPayloadFromShrModel(shrModel, patient);
        for(final String payload:payloads) {
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
                formController.saveFormData(formData);
            } catch (FormController.FormDataSaveException e) {
                Log.e("ShrMapper","Could not save Form Data",e);
            }
        }
    }

    public static List<String> createJsonEncounterPayloadFromShrModel(KenyaEmrShrModel shrModel, Patient patient) throws ShrParseException {
        try {
            List<String> encounters = new ArrayList<>();
            List<HIVTest> hivTests = shrModel.getHivTests();
            for (HIVTest hivTest : hivTests) {
                encounters.add(createJsonEncounterPayloadFromHivTest(hivTest,patient));
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


        encounterDetails.put("encounter.provider_id", hivTest.getProviderDetails().getId());
        encounterDetails.put("encounter.location_id", hivTest.getFacility());

        Date encounterDateTime = DateUtils.parseDateByPattern(hivTest.getDate(), "yyyymmdd");
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

    /**
     * Creates a new SHR Model for a given Patient. Iterates through patient demographics, identifiers, addresses and
     * attributes to construct this model
     * @param patient the Patient Object for which to create new SHR
     * @return KenyaEmrShrModel representation of newlyCreatedSHR
     * @throws IOException
     */
    public static KenyaEmrShrModel createInitialSHRModelForPatient(Patient patient) throws ShrParseException{
        return null;
    }

    /**
     * Updates SHR Model Patient details for a given Patient. Iterates through patient demographics, identifiers, addresses and
     * attributes to update this model
     * @param patient the Patient Object for which to update SHR
     * @param shrModel the SHR model Object for which to update Patient details
     * @return KenyaEmrShrModel representation of newlyCreatedSHR
     * @throws IOException
     */
    public static KenyaEmrShrModel updateSHRModelPatientDetailsForPatient(Patient patient, KenyaEmrShrModel shrModel) throws ShrParseException{
        return null;
    }

    /**
     * Adds Observations to an SHR model
     * @param shrModel
     * @param observations
     * @return
     * @throws IOException
     */
    public static KenyaEmrShrModel addObservationsToShrModel(KenyaEmrShrModel shrModel, List<Observation> observations ) throws ShrParseException{
        return null;
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


    public static class ShrSaveException extends Throwable {
        ShrSaveException(Throwable throwable) {
            super(throwable);
        }
        ShrSaveException(String message) {
            super(message);
        }
        ShrSaveException(String message, Throwable e) {
            super(message,e);
        }
    }
}
