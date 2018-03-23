package com.muzima.model.shr.kenyaemr;

import com.fasterxml.jackson.annotation.*;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "NAME",
    "DATE_ADMINISTERED"
})
public class Immunization {

    @JsonProperty("NAME")
    private String name;
    @JsonProperty("DATE_ADMINISTERED")
    private String dateAdministered;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public Immunization() {
    }

    /**
     * 
     * @param name
     * @param dateAdministered
     */
    public Immunization(String name, String dateAdministered) {
        super();
        this.name = name;
        this.dateAdministered = dateAdministered;
    }

    @JsonProperty("NAME")
    public String getName() {
        return name;
    }

    @JsonProperty("NAME")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("DATE_ADMINISTERED")
    public String getDateAdministered() {
        return dateAdministered;
    }

    @JsonProperty("DATE_ADMINISTERED")
    public void setDateAdministered(String dateAdministered) {
        this.dateAdministered = dateAdministered;
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
