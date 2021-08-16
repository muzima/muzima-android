package com.muzima.model;

import com.muzima.api.model.Concept;
import com.muzima.api.model.Observation;

import java.util.List;

public class ObsConceptWrapper {
    private Concept concept;
    private List<Observation> matchingConcepts;

    public ObsConceptWrapper(Concept concept, List<Observation> matchingConcepts) {
        this.concept = concept;
        this.matchingConcepts = matchingConcepts;
    }

    public Concept getConcept() {
        return concept;
    }

    public void setConcept(Concept concept) {
        this.concept = concept;
    }

    public List<Observation> getMatchingConcepts() {
        return matchingConcepts;
    }

    public void setMatchingConcepts(List<Observation> matchingConcepts) {
        this.matchingConcepts = matchingConcepts;
    }
}
