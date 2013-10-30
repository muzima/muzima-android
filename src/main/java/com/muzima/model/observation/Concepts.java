package com.muzima.model.observation;

import com.muzima.api.model.Observation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Concepts extends ArrayList<ConceptWithObservations> {
    public Concepts() {
    }

    public Concepts(Observation... observations) {
        for (Observation observation : observations) {
            addObservation(observation);
        }
    }

    public Concepts(List<Observation> observationsByPatient) {
        this(observationsByPatient.toArray(new Observation[observationsByPatient.size()]));
    }

    private void addObservation(final Observation observation) {
        ConceptWithObservations conceptWithObservations = getRelatedConceptWithObservations(observation);
        conceptWithObservations.addObservation(observation);
    }

    private ConceptWithObservations getRelatedConceptWithObservations(Observation observation) {
        for (ConceptWithObservations current : this) {
            if (current.getConcept().equals(observation.getConcept())) {
                return current;
            }
        }
        ConceptWithObservations conceptWithObservations = new ConceptWithObservations();
        add(conceptWithObservations);
        return conceptWithObservations;
    }

    public void sortByDate() {
        Collections.sort(this, new Comparator<ConceptWithObservations>() {
            @Override
            public int compare(ConceptWithObservations lhs, ConceptWithObservations rhs) {
                return -(lhs.getObservations().get(0).getObservationDatetime()
                        .compareTo(rhs.getObservations().get(0).getObservationDatetime()));
            }
        });

    }

}
