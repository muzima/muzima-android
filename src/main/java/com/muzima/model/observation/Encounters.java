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
                return -(lhs.getEncounter().getEncounterDatetime()
                        .compareTo(rhs.getEncounter().getEncounterDatetime()));
            }
        });

    }
}
