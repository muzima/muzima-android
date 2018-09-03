package com.muzima.model.shr.kenyaemr;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)

public class PhysicalAddress {

    @JsonProperty("VILLAGE")
    private String village;
    @JsonProperty("WARD")
    private String ward;
    @JsonProperty("SUB_COUNTY")
    private String subcounty;
    @JsonProperty("COUNTY")
    private String county;
    @JsonProperty("NEAREST_LANDMARK")
    private String nearestLandmark;
    @JsonIgnore
    private final Map<String, Object> additionalProperties = new HashMap<>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public PhysicalAddress() {
    }

    /**
     * 
     * @param village
     * @param nearestLandmark
     * @param county
     * @param ward
     * @param subcounty
     */
    public PhysicalAddress(String village, String ward, String subcounty, String county, String nearestLandmark) {
        super();
        this.village = village;
        this.ward = ward;
        this.subcounty = subcounty;
        this.county = county;
        this.nearestLandmark = nearestLandmark;
    }

    @JsonProperty("VILLAGE")
    public String getVillage() {
        return village;
    }

    @JsonProperty("VILLAGE")
    public void setVillage(String village) {
        this.village = village;
    }

    @JsonProperty("WARD")
    public String getWard() {
        return ward;
    }

    @JsonProperty("WARD")
    public void setWard(String ward) {
        this.ward = ward;
    }

    @JsonProperty("SUB_COUNTY")
    public String getSubcounty() {
        return subcounty;
    }

    @JsonProperty("SUB_COUNTY")
    public void setSubcounty(String subcounty) {
        this.subcounty = subcounty;
    }

    @JsonProperty("COUNTY")
    public String getCounty() {
        return county;
    }

    @JsonProperty("COUNTY")
    public void setCounty(String county) {
        this.county = county;
    }

    @JsonProperty("NEAREST_LANDMARK")
    public String getNearestLandmark() {
        return nearestLandmark;
    }

    @JsonProperty("NEAREST_LANDMARK")
    public void setNearestLandmark(String nearestLandmark) {
        this.nearestLandmark = nearestLandmark;
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
