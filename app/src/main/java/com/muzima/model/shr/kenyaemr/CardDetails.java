
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

    /**
     * 
     * @param lastUpdatedFacility
     * @param lastUpdated
     * @param status
     * @param reason
     */
    public CardDetails(String status, String reason, String lastUpdated, String lastUpdatedFacility) {
        super();
        this.status = status;
        this.reason = reason;
        this.lastUpdated = lastUpdated;
        this.lastUpdatedFacility = lastUpdatedFacility;
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
