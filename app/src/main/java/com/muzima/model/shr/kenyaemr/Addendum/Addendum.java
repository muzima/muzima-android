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
import com.muzima.model.shr.kenyaemr.CardDetails;

import java.util.List;

public class Addendum {

    @JsonProperty("CARD_DETAILS")
    private CardDetails cardDetail;

    @JsonProperty("IDENTIFIERS")
    private List<Identifier> identifiers;

    public List<Identifier> getIdentifiers() {
        return this.identifiers;
    }

    public void setIdentifiers(List<Identifier> identifiers) {
        this.identifiers = identifiers;
    }
}
