
package com.muzima.model.shr.kenyaemr;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
class NextOfKin {

    @JsonProperty("NOK_NAME")
    private NokName nokName;
    @JsonProperty("RELATIONSHIP")
    private String relationship;
    @JsonProperty("ADDRESS")
    private String address;
    @JsonProperty("PHONE_NUMBER")
    private String phoneNumber;
    @JsonProperty("SEX")
    private String sex;
    @JsonProperty("DATE_OF_BIRTH")
    private String dateOfBirth;
    @JsonProperty("CONTACT_ROLE")
    private String contactRole;
    @JsonIgnore
    private final Map<String, Object> additionalProperties = new HashMap<>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public NextOfKin() {
    }

    /**
     * 
     * @param address
     * @param phoneNumber
     * @param contactRole
     * @param relationship
     * @param dateOfBirth
     * @param sex
     * @param nokName
     */
    public NextOfKin(NokName nokName, String relationship, String address, String phoneNumber, String sex, String dateOfBirth, String contactRole) {
        super();
        this.nokName = nokName;
        this.relationship = relationship;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.sex = sex;
        this.dateOfBirth = dateOfBirth;
        this.contactRole = contactRole;
    }

    @JsonProperty("NOK_NAME")
    public NokName getNokName() {
        return nokName;
    }

    @JsonProperty("NOK_NAME")
    public void setNokName(NokName nokName) {
        this.nokName = nokName;
    }

    @JsonProperty("RELATIONSHIP")
    public String getRelationship() {
        return relationship;
    }

    @JsonProperty("RELATIONSHIP")
    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    @JsonProperty("ADDRESS")
    public String getAddress() {
        return address;
    }

    @JsonProperty("ADDRESS")
    public void setAddress(String address) {
        this.address = address;
    }

    @JsonProperty("PHONE_NUMBER")
    public String getPhoneNumber() {
        return phoneNumber;
    }

    @JsonProperty("PHONE_NUMBER")
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @JsonProperty("SEX")
    public String getSex() {
        return sex;
    }

    @JsonProperty("SEX")
    public void setSex(String sex) {
        this.sex = sex;
    }

    @JsonProperty("DATE_OF_BIRTH")
    public String getDateOfBirth() {
        return dateOfBirth;
    }

    @JsonProperty("DATE_OF_BIRTH")
    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    @JsonProperty("CONTACT_ROLE")
    public String getContactRole() {
        return contactRole;
    }

    @JsonProperty("CONTACT_ROLE")
    public void setContactRole(String contactRole) {
        this.contactRole = contactRole;
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
