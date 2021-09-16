/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.model.events;

import com.muzima.model.observation.ConceptWithObservations;

public class ClientSummaryObservationSelectedEvent {
    private ConceptWithObservations conceptWithObservations;

    public ClientSummaryObservationSelectedEvent(ConceptWithObservations conceptWithObservations) {
        this.conceptWithObservations = conceptWithObservations;
    }

    public ConceptWithObservations getConceptWithObservations() {
        return conceptWithObservations;
    }

    public void setConceptWithObservations(ConceptWithObservations conceptWithObservations) {
        this.conceptWithObservations = conceptWithObservations;
    }
}
