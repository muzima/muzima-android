package com.muzima.model.events;

import com.muzima.model.ObsConceptWrapper;

public class ClientSummaryObservationSelectedEvent {
    private ObsConceptWrapper conceptWrapper;

    public ClientSummaryObservationSelectedEvent(ObsConceptWrapper conceptWrapper) {
        this.conceptWrapper = conceptWrapper;
    }

    public ObsConceptWrapper getConceptWrapper() {
        return conceptWrapper;
    }

    public void setConceptWrapper(ObsConceptWrapper conceptWrapper) {
        this.conceptWrapper = conceptWrapper;
    }
}
