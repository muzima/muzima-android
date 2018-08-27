package com.muzima.model.shr.kenyaemr;


import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)

public class MotherIdentifier {

    @JsonProperty("ID")
    private String id;
    @JsonProperty("IDENTIFIER_TYPE")
    private String identifierType;
    @JsonProperty("ASSIGNING_AUTHORITY")
    private String assigningAuthority;
    @JsonProperty("ASSIGNING_FACILITY")
    private String assigningFacility;
    @JsonIgnore
    private final Map<String, Object> additionalProperties = new HashMap<>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public MotherIdentifier() {
    }

    /**
     * 
     * @param assigningAuthority
     * @param identifierType
     * @param id
     * @param assigningFacility
     */
    public MotherIdentifier(String id, String identifierType, String assigningAuthority, String assigningFacility) {
        super();
        this.id = id;
        this.identifierType = identifierType;
        this.assigningAuthority = assigningAuthority;
        this.assigningFacility = assigningFacility;
    }

    @JsonProperty("ID")
    public String getID() {
        return id;
    }

    @JsonProperty("ID")
    public void setID(String id) {
        this.id = id;
    }

    @JsonProperty("IDENTIFIER_TYPE")
    public String getIdentifierType() {
        return identifierType;
    }

    @JsonProperty("IDENTIFIER_TYPE")
    public void setIdentifierType(String identifierType) {
        this.identifierType = identifierType;
    }

    @JsonProperty("ASSIGNING_AUTHORITY")
    public String getAssigningAuthority() {
        return assigningAuthority;
    }

    @JsonProperty("ASSIGNING_AUTHORITY")
    public void setAssigningAuthority(String assigningAuthority) {
        this.assigningAuthority = assigningAuthority;
    }

    @JsonProperty("ASSIGNING_FACILITY")
    public String getAssigningFacility() {
        return assigningFacility;
    }

    @JsonProperty("ASSIGNING_FACILITY")
    public void setAssigningFacility(String assigningFacility) {
        this.assigningFacility = assigningFacility;
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
