
package com.muzima.model.shr.kenyaemr;

import android.util.Log;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "PATIENT_IDENTIFICATION",
    "NEXT_OF_KIN",
    "HIV_TEST",
    "IMMUNIZATION",
    "CARD_DETAILS"
})
public class KenyaEmrShrModel {

    @JsonProperty("PATIENT_IDENTIFICATION")
    private PatientIdentification patientIdentification;
    @JsonProperty("NEXT_OF_KIN")
    private List<NextOfKin> nextOfKins = null;
    @JsonProperty("HIV_TEST")
    private List<HIVTest> hivTests = null;
    @JsonProperty("Immunization")
    private List<Immunization> immunizations = null;
    @JsonProperty("CARD_DETAILS")
    private CardDetails cardDetails;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public KenyaEmrShrModel() {
    }

    /**
     * 
     * @param nextOfKins
     * @param patientIdentification
     * @param immunizations
     * @param hivTests
     * @param cardDetails
     */
    public KenyaEmrShrModel(PatientIdentification patientIdentification, List<NextOfKin> nextOfKins, List<HIVTest> hivTests, List<Immunization> immunizations, CardDetails cardDetails) {
        super();
        this.patientIdentification = patientIdentification;
        this.nextOfKins = nextOfKins;
        this.hivTests = hivTests;
        this.immunizations = immunizations;
        this.cardDetails = cardDetails;
    }

    public static KenyaEmrShrModel createSHRModelFromJsonString(String jsonSHRModel) throws IOException{
        ObjectMapper objectMapper = new ObjectMapper();
        KenyaEmrShrModel shrModel = objectMapper.readValue(jsonSHRModel,KenyaEmrShrModel.class);
        return shrModel;
    }

    public static String createJsonSHRModel(KenyaEmrShrModel shrModel) throws IOException{
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(shrModel);
    }

    public static KenyaEmrShrModel createBlankSHRModel(){
        try {
            return createSHRModelFromJsonString("{ " +
                    "\"SHR\": \"***wholly encryted by the middleware/library***\", " +
                    "\"ADDENDUM\": {" +
                    "\"CARD_DETAILS\": {\"STATUS\": \"ACTIVE/INACTIVE\",\"REASON\": \"LOST/DEATH/DAMAGED\",\"LAST_UPDATED\": \"20180101\",\"LAST_UPDATED_FACILITY\": \"10829\"}," +
                    "\"IDENTIFIERS\": [" +
                    "{\"ID\": \"12345678-ADFGHJY-0987654-NHYI890\",\"IDENTIFIER_TYPE\": \"CARD_SERIAL_NUMBER\",\"ASSIGNING_AUTHORITY\": \"CARD_REGISTRY\",\"ASSIGNING_FACILITY\": \"10829\"}," +
                    "{\"ID\": \"12345678\",\"IDENTIFIER_TYPE\": \"HEI_NUMBER\",\"ASSIGNING_AUTHORITY\": \"MCH\",\"ASSIGNING_FACILITY\": \"10829\"}," +
                    "{\"ID\": \"12345678\",\"IDENTIFIER_TYPE\": \"CCC_NUMBER\",\"ASSIGNING_AUTHORITY\": \"CCC\",\"ASSIGNING_FACILITY\": \"10829\"}," +
                    "{\"ID\": \"001\",\"IDENTIFIER_TYPE\": \"HTS_NUMBER\",\"ASSIGNING_AUTHORITY\": \"HTS\",\"ASSIGNING_FACILITY\": \"10829\"}," +
                    "{\"ID\": \"12345678\",\"IDENTIFIER_TYPE\": \"PMTCT_NUMBER\",\"ASSIGNING_AUTHORITY\": \"PMTCT\",\"ASSIGNING_FACILITY\": \"10829\"}" +
                    "] }" +
                    "}");
        } catch (IOException e) {
            Log.e("SHR Model","Cannot create SHR model",e);
        }
        return null;
    }

    @JsonProperty("PATIENT_IDENTIFICATION")
    public PatientIdentification getPatientIdentification() {
        return patientIdentification;
    }

    @JsonProperty("PATIENT_IDENTIFICATION")
    public void setPatientIdentification(PatientIdentification patientIdentification) {
        this.patientIdentification = patientIdentification;
    }

    @JsonProperty("NEXT_OF_KIN")
    public List<NextOfKin> getNextOfKins() {
        return nextOfKins;
    }

    @JsonProperty("NEXT_OF_KIN")
    public void setNextOfKins(List<NextOfKin> nextOfKins) {
        this.nextOfKins = nextOfKins;
    }

    @JsonProperty("HIV_TEST")
    public List<HIVTest> getHivTests() {
        return hivTests;
    }

    @JsonProperty("HIV_TEST")
    public void setHivTests(List<HIVTest> hivTests) {
        this.hivTests = hivTests;
    }

    @JsonProperty("Immunization")
    public List<Immunization> getImmunizations() {
        return immunizations;
    }

    @JsonProperty("Immunization")
    public void setImmunizations(List<Immunization> immunizations) {
        this.immunizations = immunizations;
    }

    @JsonProperty("CARD_DETAILS")
    public CardDetails getCardDetails() {
        return cardDetails;
    }

    @JsonProperty("CARD_DETAILS")
    public void setCardDetails(CardDetails cardDetails) {
        this.cardDetails = cardDetails;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
