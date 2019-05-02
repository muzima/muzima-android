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
public class Immunization {

    @JsonProperty("NAME")
    private String name;
    @JsonProperty("DATE_ADMINISTERED")
    private String dateAdministered;
    @JsonIgnore
    private final Map<String, Object> additionalProperties = new HashMap<>();

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

    public boolean lacksMandatoryValues(){
        return StringUtils.isEmpty(name) || StringUtils.isEmpty(dateAdministered);
    }

    public boolean equals(Object o){
        if(o instanceof Immunization) {
            Immunization immunization = (Immunization) o;
            return StringUtils.equalsIgnoreCase(this.getDateAdministered(), immunization.getDateAdministered())
                    && StringUtils.equalsIgnoreCase(this.getName(), immunization.getName());
        }
        return false;
    }
}
