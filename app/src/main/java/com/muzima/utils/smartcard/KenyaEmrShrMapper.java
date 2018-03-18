package com.muzima.utils.smartcard;

import android.util.Log;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Patient;
import com.muzima.model.shr.kenyaemr.KenyaEmrShrModel;

import java.io.IOException;
import java.util.List;

public class KenyaEmrShrMapper {

    /**
     * Converts an SHR model from JSON representation to KenyaEmrShrModel
     * @param jsonSHRModel the JSON representation of the SHR model
     * @return Representation of the JSON input as KenyaEmrShrModel object
     * @throws IOException
     */
    public static KenyaEmrShrModel createSHRModelFromJson(String jsonSHRModel) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        KenyaEmrShrModel shrModel = objectMapper.readValue(jsonSHRModel,KenyaEmrShrModel.class);
        return shrModel;
    }

    /**
     * Converts a KenyaEmrShrModel representation of SHR to JSON representation
     * @param shrModel the KenyaEmrShrModel Object representation of the SHR model
     * @return JSON representation of SHR model
     * @throws IOException
     */
    public static String createJsonFromSHRModel(KenyaEmrShrModel shrModel) throws IOException{
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(shrModel);
    }

    /**
     * Extracts a Patient object from a JSON SHR model
     * @param shrModel the JSON representation of the SHR model
     * @return Patient object extracted from SHR model
     * @throws IOException
     */
    public static Patient extractPatientFromShrModel(String shrModel) throws IOException{
        return null;
    }

    /**
     * Extracts a Patient Object from a KenyaEmrShrModel Object of SHR model
     * @param shrModel the KenyaEmrShrModel Object representation of the SHR model
     * @return Patient object extracted from SHR model
     * @throws IOException
     */
    public static Patient extractPatientFromShrModel(KenyaEmrShrModel shrModel) throws IOException{
        return null;
    }

    /**
     * Extracts Observations List from a JSON representation of the SHR model
     * @param shrModel the JSON representation of the SHR model
     * @return Observations List extracted from SHR model
     * @throws IOException
     */
    public static List<Observation> extractObservationsFromShrModel(String shrModel) throws IOException{
        return null;
    }

    /**
     * Extracts Observations List from a KenyaEmrShrModel Object of SHR model
     * @param shrModel the KenyaEmrShrModel Object representation of the SHR model
     * @return Observations List extracted from SHR model
     * @throws IOException
     */
    public static List<Observation> extractObservationsFromShrModel(KenyaEmrShrModel shrModel) throws IOException{
        return null;
    }

    /**
     * Creates a new SHR Model for a given Patient. Iterates through patient demographics, identifiers, addresses and
     * attributes to construct this model
     * @param patient the Patient Object for which to create new SHR
     * @return KenyaEmrShrModel representation of newlyCreatedSHR
     * @throws IOException
     */
    public static KenyaEmrShrModel createInitialSHRModelForPatient(Patient patient) throws IOException{
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
    public static KenyaEmrShrModel updateSHRModelPatientDetailsForPatient(Patient patient, KenyaEmrShrModel shrModel) throws IOException{
        return null;
    }

    /**
     * Adds Observations to an SHR model
     * @param shrModel
     * @param observations
     * @return
     * @throws IOException
     */
    public static KenyaEmrShrModel addObservationsToShrModel(KenyaEmrShrModel shrModel, List<Observation> observations ) throws IOException{
        return null;
    }
}
