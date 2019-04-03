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

public class Encounters extends ArrayList<EncounterWithObservations> {
    public Encounters() {
    }

    public Encounters(Observation... observations) {
        for (Observation observation : observations) {
            addObservation(observation);
        }
    }

    public Encounters(List<Observation> observationsByPatient) {
        this(observationsByPatient.toArray(new Observation[observationsByPatient.size()]));
    }

    private void addObservation(final Observation observation) {
        EncounterWithObservations encounterWithObservations = getRelatedEncounterWithObservations(observation);
        encounterWithObservations.addObservation(observation);
    }

    private EncounterWithObservations getRelatedEncounterWithObservations(Observation observation) {
        for (EncounterWithObservations current : this) {
            if (current.getEncounter().equals(observation.getEncounter())) {
                return current;
            }
        }
        EncounterWithObservations encounterWithObservations = new EncounterWithObservations();
        add(encounterWithObservations);
        return encounterWithObservations;
    }

    public void sortByDate() {
        Collections.sort(this, new Comparator<EncounterWithObservations>() {
            @Override
            public int compare(EncounterWithObservations lhs, EncounterWithObservations rhs) {
                if (lhs.getEncounter().getEncounterDatetime()==null)
                    return -1;
                if (rhs.getEncounter().getEncounterDatetime()==null)
                    return 1;
                return -(lhs.getEncounter().getEncounterDatetime()
                        .compareTo(rhs.getEncounter().getEncounterDatetime()));
            }
        });

    }
}
