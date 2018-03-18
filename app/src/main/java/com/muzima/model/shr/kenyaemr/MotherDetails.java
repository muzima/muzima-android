
package com.muzima.model.shr.kenyaemr;

import com.fasterxml.jackson.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "MOTHER_NAME",
    "MOTHER_IDENTIFIER"
})
public class MotherDetails {

    @JsonProperty("MOTHER_NAME")
    private MotherName motherName;
    @JsonProperty("MOTHER_IDENTIFIER")
    private List<MotherIdentifier> motherIdentifiers = null;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public MotherDetails() {
    }

    /**
     * 
     * @param MotherName
     * @param motherIdentifiers
     */
    public MotherDetails(MotherName MotherName, List<MotherIdentifier> motherIdentifiers) {
        super();
        this.motherName = MotherName;
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
