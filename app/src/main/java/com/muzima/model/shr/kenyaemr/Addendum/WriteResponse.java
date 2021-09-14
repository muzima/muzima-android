/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

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
