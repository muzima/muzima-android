package com.muzima.model.shr.kenyaemr;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)

public class Name {

    @JsonProperty("FIRST_NAME")
    private String firstName;
    @JsonProperty("MIDDLE_NAME")
    private String middleName;
    @JsonProperty("LAST_NAME")
    private String lastName;
    @JsonIgnore
    private final Map<String, Object> additionalProperties = new HashMap<>();

    /**
     * No args constructor for use in serialization
     *
     */
    Name() {
    }

    /**
     *
     * @param firstName
     * @param middleName
     * @param lastName
     */
    public Name(String firstName, String middleName, String lastName) {
        super();
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
    }

    @JsonProperty("FIRST_NAME")
    public String getFirstName() {
        return firstName;
    }

    @JsonProperty("FIRST_NAME")
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @JsonProperty("MIDDLE_NAME")
    public String getMiddleName() {
        return middleName;
    }

    @JsonProperty("MIDDLE_NAME")
    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    @JsonProperty("LAST_NAME")
    public String getLastName() {
        return lastName;
    }

    @JsonProperty("LAST_NAME")
    public void setLastName(String lastName) {
        this.lastName = lastName;
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
