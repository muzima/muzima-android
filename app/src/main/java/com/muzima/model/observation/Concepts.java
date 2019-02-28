/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

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
