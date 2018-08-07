package com.muzima.model.shr.kenyaemr;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.muzima.utils.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)

public class PatientIdentification {

    @JsonProperty("EXTERNAL_PATIENT_ID")
    private ExternalPatientId externalPatientId;
    @JsonProperty("INTERNAL_PATIENT_ID")
    private List<InternalPatientId> internalPatientIds = null;
    @JsonProperty("PATIENT_NAME")
    private PatientName patientName;
    @JsonProperty("DATE_OF_BIRTH")
    private String dateOfBirth;
    @JsonProperty("DATE_OF_BIRTH_PRECISION")
    private String dateOfBirthPrecision;
    @JsonProperty("SEX")
    private String sex;
    @JsonProperty("DEATH_DATE")
    private String deathDate;
    @JsonProperty("DEATH_INDICATOR")
    private String deathIndicator;
    @JsonProperty("PATIENT_ADDRESS")
    private PatientAddress patientAddress;
    @JsonProperty("PHONE_NUMBER")
    private String phoneNumber;
    @JsonProperty("MARITAL_STATUS")
    private String maritalStatus;
    @JsonProperty("MOTHER_DETAILS")
    private MotherDetails motherDetails;
    @JsonIgnore
    private final Map<String, Object> additionalProperties = new HashMap<>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public PatientIdentification() {
    }

    /**
     * 
     * @param deathDate
     * @param deathIndicator
     * @param phoneNumber
     * @param externalPatientId
     * @param dateOfBirthPrecision
     * @param motherDetails
     * @param patientName
     * @param patientAddress
     * @param sex
     * @param dateOfBirth
     * @param maritalStatus
     * @param internalPatientIds
     */
    public PatientIdentification(ExternalPatientId externalPatientId, List<InternalPatientId> internalPatientIds, PatientName patientName, String dateOfBirth, String dateOfBirthPrecision, String sex, String deathDate, String deathIndicator, PatientAddress patientAddress, String phoneNumber, String maritalStatus, MotherDetails motherDetails) {
        super();
        this.externalPatientId = externalPatientId;
        this.internalPatientIds = internalPatientIds;
        this.patientName = patientName;
        this.dateOfBirth = dateOfBirth;
        this.dateOfBirthPrecision = dateOfBirthPrecision;
        this.sex = sex;
        this.deathDate = deathDate;
        this.deathIndicator = deathIndicator;
        this.patientAddress = patientAddress;
        this.phoneNumber = phoneNumber;
        this.maritalStatus = maritalStatus;
        this.motherDetails = motherDetails;
    }

    @JsonProperty("EXTERNAL_PATIENT_ID")
    public ExternalPatientId getExternalPatientId() {
        return externalPatientId;
    }

    @JsonProperty("EXTERNAL_PATIENT_ID")
    public void setExternalPatientId(ExternalPatientId externalPatientId) {
        this.externalPatientId = externalPatientId;
    }

    @JsonProperty("INTERNAL_PATIENT_ID")
    public List<InternalPatientId> getInternalPatientIds() {
        return internalPatientIds;
    }

    @JsonProperty("INTERNAL_PATIENT_ID")
    public void setInternalPatientIds(List<InternalPatientId> internalPatientIds) {
        this.internalPatientIds = internalPatientIds;
    }

    @JsonProperty("PATIENT_NAME")
    public PatientName getPatientName() {
        return patientName;
    }

    @JsonProperty("PATIENT_NAME")
    public void setPatientName(PatientName patientName) {
        this.patientName = patientName;
    }

    @JsonProperty("DATE_OF_BIRTH")
    public String getDateOfBirth() {
        return dateOfBirth;
    }

    @JsonProperty("DATE_OF_BIRTH")
    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    @JsonProperty("DATE_OF_BIRTH_PRECISION")
    public String getDateOfBirthPrecision() {
        return dateOfBirthPrecision;
    }

    @JsonProperty("DATE_OF_BIRTH_PRECISION")
    public void setDateOfBirthPrecision(String dateOfBirthPrecision) {
        this.dateOfBirthPrecision = dateOfBirthPrecision;
    }

    @JsonProperty("SEX")
    public String getSex() {
        return sex;
    }

    @JsonProperty("SEX")
    public void setSex(String sex) {
        this.sex = sex;
    }

    @JsonProperty("DEATH_DATE")
    public String getDeathDate() {
        return deathDate;
    }

    @JsonProperty("DEATH_DATE")
    public void setDeathDate(String deathDate) {
        this.deathDate = deathDate;
    }

    @JsonProperty("DEATH_INDICATOR")
    public String getDeathIndicator() {
        return deathIndicator;
    }

    @JsonProperty("DEATH_INDICATOR")
    public void setDeathIndicator(String deathIndicator) {
        this.deathIndicator = deathIndicator;
    }

    @JsonProperty("PATIENT_ADDRESS")
    public PatientAddress getPatientAddress() {
        return patientAddress;
    }

    @JsonProperty("PATIENT_ADDRESS")
    public void setPatientAddress(PatientAddress patientAddress) {
        this.patientAddress = patientAddress;
    }

    @JsonProperty("PHONE_NUMBER")
    public String getPhoneNumber() {
        return phoneNumber;
    }

    @JsonProperty("PHONE_NUMBER")
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @JsonProperty("MARITAL_STATUS")
    public String getMaritalStatus() {
        return maritalStatus;
    }

    @JsonProperty("MARITAL_STATUS")
    public void setMaritalStatus(String maritalStatus) {
        this.maritalStatus = maritalStatus;
    }

    @JsonProperty("MOTHER_DETAILS")
    public MotherDetails getMotherDetails() {
        return motherDetails;
    }

    @JsonProperty("MOTHER_DETAILS")
    public void setMotherDetails(MotherDetails motherDetails) {
        this.motherDetails = motherDetails;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public InternalPatientId getInternalPatientIdByIdentifierType(String identifierType){
        if(internalPatientIds != null){
            for(InternalPatientId internalPatientId: internalPatientIds){
                if(StringUtils.equals(internalPatientId.getIdentifierType(),identifierType)){
                    return internalPatientId;
                }
            }
        }
        return null;
    }
}
