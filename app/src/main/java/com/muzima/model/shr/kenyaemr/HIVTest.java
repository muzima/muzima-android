
package com.muzima.model.shr.kenyaemr;

import com.fasterxml.jackson.annotation.*;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "DATE",
    "RESULT",
    "TYPE",
    "FACILITY",
    "STRATEGY",
    "PROVIDER_DETAILS"
})
public class HIVTest {

    @JsonProperty("DATE")
    private String date;
    @JsonProperty("RESULT")
    private String result;
    @JsonProperty("TYPE")
    private String type;
    @JsonProperty("FACILITY")
    private String facility;
    @JsonProperty("STRATEGY")
    private String strategy;
    @JsonProperty("PROVIDER_DETAILS")
    private ProviderDetails providerDetails;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public HIVTest() {
    }

    /**
     * 
     * @param providerDetails
     * @param facility
     * @param strategy
     * @param result
     * @param type
     * @param date
     */
    public HIVTest(String date, String result, String type, String facility, String strategy, ProviderDetails providerDetails) {
        super();
        this.date = date;
        this.result = result;
        this.type = type;
        this.facility = facility;
        this.strategy = strategy;
        this.providerDetails = providerDetails;
    }

    @JsonProperty("DATE")
    public String getDate() {
        return date;
    }

    @JsonProperty("DATE")
    public void setDate(String date) {
        this.date = date;
    }

    @JsonProperty("RESULT")
    public String getResult() {
        return result;
    }

    @JsonProperty("RESULT")
    public void setResult(String result) {
        this.result = result;
    }

    @JsonProperty("TYPE")
    public String getType() {
        return type;
    }

    @JsonProperty("TYPE")
    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty("FACILITY")
    public String getFacility() {
        return facility;
    }

    @JsonProperty("FACILITY")
    public void setFacility(String facility) {
        this.facility = facility;
    }

    @JsonProperty("STRATEGY")
    public String getStrategy() {
        return strategy;
    }

    @JsonProperty("STRATEGY")
    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    @JsonProperty("PROVIDER_DETAILS")
    public ProviderDetails getProviderDetails() {
        return providerDetails;
    }

    @JsonProperty("PROVIDER_DETAILS")
    public void setProviderDetails(ProviderDetails providerDetails) {
        this.providerDetails = providerDetails;
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
