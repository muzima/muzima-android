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
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)

public class PatientAddress {

    @JsonProperty("PHYSICAL_ADDRESS")
    private PhysicalAddress physicalAddress;
    @JsonProperty("POSTAL_ADDRESS")
    private String postalAddress;
    @JsonIgnore
    private final Map<String, Object> additionalProperties = new HashMap<>();

    /**
     * No args constructor for use in serialization
     *
     */
    public PatientAddress() {
    }

    /**
     *
     * @param physicalAddress
     * @param postalAddress
     */
    public PatientAddress(PhysicalAddress physicalAddress, String postalAddress) {
        super();
        this.physicalAddress = physicalAddress;
        this.postalAddress = postalAddress;
    }

    @JsonProperty("PHYSICAL_ADDRESS")
    public PhysicalAddress getPhysicalAddress() {
        return physicalAddress;
    }

    @JsonProperty("PHYSICAL_ADDRESS")
    public void setPhysicalAddress(PhysicalAddress physicalAddress) {
        this.physicalAddress = physicalAddress;
    }

    @JsonProperty("POSTAL_ADDRESS")
    public String getPostalAddress() {
        return postalAddress;
    }

    @JsonProperty("POSTAL_ADDRESS")
    public void setPostalAddress(String postalAddress) {
        this.postalAddress = postalAddress;
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
