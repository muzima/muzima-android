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

import com.muzima.api.model.Concept;
import com.muzima.api.model.ConceptName;
import com.muzima.api.model.Observation;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class EncounterWithObservationsTest {
    @Test
    public void shouldOrderTheObservationsByConceptName() {
        EncounterWithObservations encounterWithObservations = new EncounterWithObservations();
        Observation observation1 = getObservation("o1", "c1", "Weight");
        Observation observation2 = getObservation("o2", "c2", "Blood Type");
        encounterWithObservations.addObservation(observation1);
        encounterWithObservations.addObservation(observation2);

        List<Observation> expected = new ArrayList<>();
        expected.add(observation2);
        expected.add(observation1);
        assertThat(encounterWithObservations.getObservations(), is(expected));
    }

    @Test
    public void shouldOrderObservationByConceptNameThenByDate() {
        EncounterWithObservations encounterWithObservations = new EncounterWithObservations();
        Observation observation1 = getObservation("o1", "c1", "Weight", new Date(1));
        Observation observation2 = getObservation("o2", "c2", "Blood_Type", new Date(2));
        Observation observation3 = getObservation("o3", "c2", "Blood_Type", new Date(1));
        encounterWithObservations.addObservation(observation1);
        encounterWithObservations.addObservation(observation2);
        encounterWithObservations.addObservation(observation3);

        List<Observation> expected = new ArrayList<>();
        expected.add(observation2);
        expected.add(observation3);
        expected.add(observation1);
        assertThat(encounterWithObservations.getObservations(), is(expected));
    }

    private Observation getObservation(String observationUuid, final String conceptUuid, final String conceptName) {
        return getObservation(observationUuid, conceptUuid, conceptName, new Date());
    }

    private Observation getObservation(String observationUuid, final String conceptUuid, final String conceptName, Date date) {
        Observation observation = new Observation();
        observation.setUuid(observationUuid);
        Concept concept = new Concept();
        concept.addName(new ConceptName(){{
            setUuid(conceptUuid);
            setName(conceptName);
        }});
        observation.setObservationDatetime(date);
        observation.setConcept(concept);
        return observation;
    }
}
