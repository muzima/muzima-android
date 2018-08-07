package com.muzima.model.shr.kenyaemr.Addendum;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Identifier {

    @JsonProperty("ID")
    private String id;

    @JsonProperty("IDENTIFIER_TYPE")
    private String identifierType;

    @JsonProperty("ASSIGNING_AUTHORITY")
    private String assigningAuthority;

    @JsonProperty("ASSIGNING_FACILITY")
    private String assigningFacility;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdentifierType() {
        return identifierType;
    }

    public void setIdentifierType(String identifierType) {
        this.identifierType = identifierType;
    }

    public String getAssigningAuthority() {
        return assigningAuthority;
    }

    public void setAssigningAuthority(String assigningAuthority) {
        this.assigningAuthority = assigningAuthority;
    }

    public String getAssigningFacility() {
        return assigningFacility;
    }

    public void setAssigningFacility(String assigningFacility) {
        this.assigningFacility = assigningFacility;
    }
}
