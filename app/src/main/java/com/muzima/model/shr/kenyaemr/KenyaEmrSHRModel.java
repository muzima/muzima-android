package com.muzima.model.shr.kenyaemr;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.muzima.utils.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)

public class KenyaEmrSHRModel {

    @JsonProperty("PATIENT_IDENTIFICATION")
    private PatientIdentification patientIdentification;
    @JsonProperty("NEXT_OF_KIN")
    private List<NextOfKin> nextOfKins = null;
    @JsonProperty("HIV_TEST")
    private List<HIVTest> hivTests = null;
    @JsonProperty("IMMUNIZATION")
    private List<Immunization> immunizations = null;
    @JsonProperty("CARD_DETAILS")
    private CardDetails cardDetails;
    @JsonIgnore
    private final Map<String, Object> additionalProperties = new HashMap<>();

    public static final String newShrModelTemplate =
            "{  \"CARD_DETAILS\": {}," +
                "\"HIV_TEST\": []," +
                "\"IMMUNIZATION\": []," +
                 "\"NEXT_OF_KIN\": []," +
                "\"PATIENT_IDENTIFICATION\": {" +
                    "\"EXTERNAL_PATIENT_ID\": {}," +
                    "\"INTERNAL_PATIENT_ID\": []," +
                    "\"MOTHER_DETAILS\": {" +
                        "\"MOTHER_IDENTIFIER\": []," +
                        "\"MOTHER_NAME\": {}" +
                    "}," +
                    "\"PATIENT_ADDRESS\": {}" +
                "}," +
                "\"VERSION\": \"1.0.0\"" +
            "}";

    /**
     * No args constructor for use in serialization
     * 
     */
    public KenyaEmrSHRModel() {

    }

    /**
     * 
     * @param nextOfKins
     * @param patientIdentification
     * @param immunizations
     * @param hivTests
     * @param cardDetails
     */
    public KenyaEmrSHRModel(PatientIdentification patientIdentification, List<NextOfKin> nextOfKins, List<HIVTest> hivTests, List<Immunization> immunizations, CardDetails cardDetails) {
        super();
        this.patientIdentification = patientIdentification;
        this.nextOfKins = nextOfKins;
        this.hivTests = hivTests;
        this.immunizations = immunizations;
        this.cardDetails = cardDetails;
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
        return hivTests == null ?  new ArrayList<HIVTest>() : hivTests;
    }

    @JsonProperty("HIV_TEST")
    public void setHivTests(List<HIVTest> hivTests) {
        this.hivTests = hivTests;
    }

    @JsonProperty("IMMUNIZATION")
    public List<Immunization> getImmunizations() {
        return immunizations == null ? new ArrayList<Immunization>() : immunizations;
    }

    @JsonProperty("IMMUNIZATION")
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

    @JsonIgnore
    public boolean isNewSHRModel(){
        try {
            return StringUtils.isEmpty(getPatientIdentification().getExternalPatientId().getID());
        } catch (NullPointerException e){
            return true;
        }
    }



}
