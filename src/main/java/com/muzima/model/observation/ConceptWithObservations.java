package com.muzima.model.observation;

import com.muzima.api.model.Concept;
import com.muzima.api.model.Observation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ConceptWithObservations {
    private Concept concept;
    private List<Observation> observations;

    public ConceptWithObservations() {
        observations = new ArrayList<Observation>();
    }

    public Concept getConcept() {
        return concept;
    }

    private void setConcept(Concept concept) {
        this.concept = concept;
    }

    public List<Observation> getObservations() {
        Collections.sort(observations, observationDateTimeComparator);
        return observations;
    }

    public void addObservation(Observation observation) {
        observations.add(observation);
        if (concept == null)
            setConcept(observation.getConcept());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConceptWithObservations)) return false;

        ConceptWithObservations that = (ConceptWithObservations) o;

        return !(concept != null ? !concept.equals(that.concept) : that.concept != null) && getObservations().equals(that.getObservations());

    }

    @Override
    public int hashCode() {
        int result = concept != null ? concept.hashCode() : 0;
        result = 31 * result + (observations != null ? observations.hashCode() : 0);
        return result;
    }

    private final Comparator<Observation> observationDateTimeComparator = new Comparator<Observation>() {
        @Override
        public int compare(Observation lhs, Observation rhs) {
            if (lhs.getObservationDatetime() == null && rhs.getObservationDatetime() == null) {
                return 0;
            }

            if (lhs.getObservationDatetime() == null && rhs.getObservationDatetime() != null) {
                return -1;
            }

            if (lhs.getObservationDatetime() != null && rhs.getObservationDatetime() == null) {
                return 1;
            }
            return -lhs.getObservationDatetime().compareTo(rhs.getObservationDatetime());
        }
    };
}
