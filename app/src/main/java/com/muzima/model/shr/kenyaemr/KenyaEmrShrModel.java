
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
