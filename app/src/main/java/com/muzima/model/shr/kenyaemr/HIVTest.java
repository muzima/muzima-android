
package com.muzima.model.shr.kenyaemr;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.muzima.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)

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
    private final Map<String, Object> additionalProperties = new HashMap<>();

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

    public boolean lacksMandatoryValues(){
        return StringUtils.isEmpty(result) || StringUtils.isEmpty(type)
                || StringUtils.isEmpty(strategy) || StringUtils.isEmpty(facility)
                || providerDetails.lacksMandatoryValues();
    }

    @Override
    public boolean equals(Object o){
        if(o instanceof HIVTest) {
            HIVTest hivTest = (HIVTest) o;
            return StringUtils.equalsIgnoreCase(this.getDate(), hivTest.getDate())
                    && StringUtils.equalsIgnoreCase(this.getResult(), hivTest.getResult())
                    && StringUtils.equalsIgnoreCase(this.getType(), hivTest.getType())
                    && StringUtils.equalsIgnoreCase(this.getStrategy(), hivTest.getStrategy())
                    && StringUtils.equalsIgnoreCase(this.getFacility(), hivTest.getFacility())
                    && this.getProviderDetails().equals(hivTest.getProviderDetails());
        }
        return false;
    }

    public String toString(){
        return "DATE: " + getDate() +
                ", RESULT: " + getResult() +
                ", STRATEGY: " + getStrategy() +
                ", FACILITY: " + getFacility();
    }
}
