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

import com.muzima.api.model.Encounter;
import com.muzima.api.model.Observation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EncounterWithObservations {
    private Encounter encounter;
    private List<Observation> observations = new ArrayList<>();
    public Encounter getEncounter() {
        return encounter;
    }

    public void setEncounter(Encounter encounter) {
        this.encounter = encounter;
    }

    public List<Observation> getObservations() {
        Collections.sort(observations, observationDateTimeComparator);
        return observations;
    }

    public void setObservations(List<Observation> observations) {
        this.observations = observations;
    }

    public void addObservation(Observation observation) {
        encounter = observation.getEncounter();
        observations.add(observation);
    }

    private final Comparator<Observation> observationDateTimeComparator = new Comparator<Observation>() {
        @Override
        public int compare(Observation lhs, Observation rhs) {
            int isConceptNameEqual = lhs.getConcept().getName().compareTo(rhs.getConcept().getName());
            if(isConceptNameEqual != 0){
                return isConceptNameEqual;
            }

            return -lhs.getObservationDatetime().compareTo(rhs.getObservationDatetime());
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EncounterWithObservations)) return false;

        EncounterWithObservations encounterWithObservations = (EncounterWithObservations) o;

        return encounter.equals(encounterWithObservations.encounter) && observations.equals(encounterWithObservations.observations);
    }

    @Override
    public int hashCode() {
        int result = encounter.hashCode();
        result = 31 * result + observations.hashCode();
        return result;
    }
}
