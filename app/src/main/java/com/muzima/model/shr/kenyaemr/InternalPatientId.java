/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.model.shr.kenyaemr;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.muzima.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "ID",
    "IDENTIFIER_TYPE",
    "ASSIGNING_AUTHORITY",
    "ASSIGNING_FACILITY"
})
public class InternalPatientId {

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
    public InternalPatientId() {
    }

    @JsonProperty("ID")
    public String getID() {
        return id;
    }

    @JsonProperty("ID")
    public void setID(String iD) {
        this.id = iD;
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

    public boolean lacksMandatoryValues(){
        return StringUtils.isEmpty(identifierType) || StringUtils.isEmpty(assigningFacility)
                || StringUtils.isEmpty(id);
    }
}
