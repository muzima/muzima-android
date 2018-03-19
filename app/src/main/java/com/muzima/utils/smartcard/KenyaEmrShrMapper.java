package com.muzima.utils.smartcard;

import android.util.Log;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PatientIdentifier;
import com.muzima.api.model.PatientIdentifierType;
import com.muzima.api.model.PersonName;
import com.muzima.model.shr.kenyaemr.ExternalPatientId;
import com.muzima.model.shr.kenyaemr.InternalPatientId;
import com.muzima.model.shr.kenyaemr.KenyaEmrShrModel;
import com.muzima.model.shr.kenyaemr.PatientIdentification;
import com.muzima.utils.Constants;
import com.muzima.utils.Constants.Shr.KenyaEmr.IdentifierType;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

            //set patient Name
            PersonName personName = new PersonName();
            personName.setFamilyName(identification.getPatientName().getFirstName());
            personName.setGivenName(identification.getPatientName().getLastName());
            personName.setMiddleName(identification.getPatientName().getMiddleName());
            List<PersonName> names = new ArrayList<>();
            names.add(personName);
            patient.setNames(names);

            //set Identifiers
            List<PatientIdentifier> identifiers = new ArrayList<PatientIdentifier>();
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
                }
                identifierType.setName(internalPatientId.getIdentifierType());
                patientIdentifier.setIdentifierType(identifierType);
                patientIdentifier.setIdentifier(internalPatientId.getID());
                identifiers.add(patientIdentifier);
            }
            patient.setIdentifiers(identifiers);

            //date of birth
            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyymmdd");
            String dateOfBirth = identification.getDateOfBirth();
            Date dob = dateFormatter.parse(identification.getDateOfBirth());
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
     * Extracts Observations List from a JSON representation of the SHR model
     * @param shrModel the JSON representation of the SHR model
     * @return Observations List extracted from SHR model
     * @throws IOException
     */
    public static List<Observation> extractObservationsFromShrModel(String shrModel) throws ShrParseException{
        return null;
    }

    /**
     * Extracts Observations List from a KenyaEmrShrModel Object of SHR model
     * @param shrModel the KenyaEmrShrModel Object representation of the SHR model
     * @return Observations List extracted from SHR model
     * @throws IOException
     */
    public static List<Observation> extractObservationsFromShrModel(KenyaEmrShrModel shrModel) throws ShrParseException{
        return null;
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


    static class ShrParseException extends Throwable {
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
