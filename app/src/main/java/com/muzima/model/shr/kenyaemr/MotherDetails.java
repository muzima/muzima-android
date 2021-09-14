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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)

public class MotherDetails {

    @JsonProperty("MOTHER_NAME")
    private MotherName motherName;
    @JsonProperty("MOTHER_IDENTIFIER")
    private List<MotherIdentifier> motherIdentifiers = null;
    @JsonIgnore
    private final Map<String, Object> additionalProperties = new HashMap<>();

    /**
     * No args constructor for use in serialization
     *
     */
    public MotherDetails() {
    }

    /**
     *
     * @param motherName
     * @param motherIdentifiers
     */
    public MotherDetails(MotherName motherName, List<MotherIdentifier> motherIdentifiers) {
        super();
        this.motherName = motherName;
        this.motherIdentifiers = motherIdentifiers;
    }

    @JsonProperty("MOTHER_NAME")
    public MotherName getMotherName() {
        return motherName;
    }

    @JsonProperty("MOTHER_NAME")
    public void setMotherName(MotherName motherName) {
        this.motherName = motherName;
    }

    @JsonProperty("MOTHER_IDENTIFIER")
    public List<MotherIdentifier> getMotherIdentifiers() {
        return motherIdentifiers;
    }

    @JsonProperty("MOTHER_IDENTIFIER")
    public void setMotherIdentifiers(List<MotherIdentifier> motherIdentifiers) {
        this.motherIdentifiers = motherIdentifiers;
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
