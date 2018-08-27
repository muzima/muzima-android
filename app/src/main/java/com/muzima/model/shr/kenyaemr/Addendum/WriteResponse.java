package com.muzima.model.shr.kenyaemr.Addendum;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WriteResponse {

    @JsonProperty("SHR")
    private String SHR;

    @JsonProperty("addendum")
    private Addendum addendum;

    public void setSHR(String SHR){
        this.SHR = SHR;
    }

    public String getSHR(){
        return this.SHR;
    }

    public void setAddendum(Addendum addendum){
        this.addendum = addendum;
    }

    public Addendum getAddendum(){
        return this.addendum;
    }
}
