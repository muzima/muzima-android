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

public class ProviderDetails {

    @JsonProperty("NAME")
    private String name;
    @JsonProperty("ID")
    private String id;
    @JsonIgnore
    private final Map<String, Object> additionalProperties = new HashMap<>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public ProviderDetails() {
    }

    /**
     * 
     * @param name
     * @param id
     */
    public ProviderDetails(String name, String id) {
        super();
        this.name = name;
        this.id = id;
    }

    @JsonProperty("NAME")
    public String getName() {
        return name;
    }

    @JsonProperty("NAME")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("ID")
    public String getId() {
        return id;
    }

    @JsonProperty("ID")
    public void setId(String id) {
        this.id = id;
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
        return StringUtils.isEmpty(name) || StringUtils.isEmpty(id);
    }

    public boolean equals(Object o) {
        if(o instanceof ProviderDetails) {
            ProviderDetails providerDetails = (ProviderDetails) o;
            return StringUtils.equalsIgnoreCase(this.getName(), providerDetails.getName())
                    && StringUtils.equalsIgnoreCase(this.getId(), providerDetails.getId());
        }
        return false;
    }
}
