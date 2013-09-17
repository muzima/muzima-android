package com.muzima.model.observation;

import com.muzima.api.model.Concept;
import com.muzima.api.model.Observation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class ConceptWithObservations {
    private Concept concept;
    private Set<Observation> observations;

    public ConceptWithObservations() {
        observations = new TreeSet<Observation>(observationDateTimeComparator);
    }

    public Concept getConcept() {
        return concept;
    }

    public void setConcept(Concept concept) {
        this.concept = concept;
    }

    public List<Observation> getObservations() {
        return new ArrayList<Observation>(observations);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConceptWithObservations that = (ConceptWithObservations) o;

        if (concept != null ? !concept.getUuid().equals(that.concept.getUuid()) : that.concept != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return concept != null ? concept.getUuid().hashCode() : 0;
    }

    public void addObservation(Observation observation) {
        observations.add(observation);
    }

    private final Comparator<Observation> observationDateTimeComparator = new Comparator<Observation>() {
        @Override
        public int compare(Observation lhs, Observation rhs) {
            return -lhs.getObservationDatetime().compareTo(rhs.getObservationDatetime());
        }
    };
}
