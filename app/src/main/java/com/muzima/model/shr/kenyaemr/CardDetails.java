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

public class CardDetails {

    @JsonProperty("STATUS")
    private String status;
    @JsonProperty("REASON")
    private String reason;
    @JsonProperty("LAST_UPDATED")
    private String lastUpdated;
    @JsonProperty("LAST_UPDATED_FACILITY")
    private String lastUpdatedFacility;
    @JsonIgnore
    private final Map<String, Object> additionalProperties = new HashMap<>();

    /**
     * No args constructor for use in serialization
     *
     */
    public CardDetails() {
    }

    @JsonProperty("STATUS")
    public String getStatus() {
        return status;
    }

    @JsonProperty("STATUS")
    public void setStatus(String status) {
        this.status = status;
    }

    @JsonProperty("REASON")
    public String getReason() {
        return reason;
    }

    @JsonProperty("REASON")
    public void setReason(String reason) {
        this.reason = reason;
    }

    @JsonProperty("LAST_UPDATED")
    public String getLastUpdated() {
        return lastUpdated;
    }

    @JsonProperty("LAST_UPDATED")
    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @JsonProperty("LAST_UPDATED_FACILITY")
    public String getLastUpdatedFacility() {
        return lastUpdatedFacility;
    }

    @JsonProperty("LAST_UPDATED_FACILITY")
    public void setLastUpdatedFacility(String lastUpdatedFacility) {
        this.lastUpdatedFacility = lastUpdatedFacility;
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
