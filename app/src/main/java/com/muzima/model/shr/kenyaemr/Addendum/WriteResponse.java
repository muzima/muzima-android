package com.muzima.model.shr.kenyaemr.Addendum;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WriteResponse {

    @JsonProperty("SHR")
    private String shr;

    @JsonProperty("addendum")
    private Addendum addendum;

    public void setShr(String shr){
        this.shr = shr;
    }

    public String getShr(){
        return this.shr;
    }

    public void setAddendum(Addendum addendum){
        this.addendum = addendum;
    }

    public Addendum getAddendum(){
        return this.addendum;
    }
}
