package com.muzima.model.observation;

import com.muzima.api.model.Concept;
import com.muzima.api.model.ConceptName;
import com.muzima.api.model.Observation;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class EncounterWithObservationsTest {
    @Test
    public void shouldOrderTheObservationsByConceptName() throws Exception {
        EncounterWithObservations encounterWithObservations = new EncounterWithObservations();
        Observation observation1 = getObservation("o1", "c1", "Weight");
        Observation observation2 = getObservation("o2", "c2", "Blood Type");
        encounterWithObservations.addObservation(observation1);
        encounterWithObservations.addObservation(observation2);

        List<Observation> expected = new ArrayList<Observation>();
        expected.add(observation2);
        expected.add(observation1);
        assertThat(encounterWithObservations.getObservations(), is(expected));
    }

    private Observation getObservation(String observationUuid, final String conceptUuid, final String conceptName) {
        Observation observation1 = new Observation();
        observation1.setUuid(observationUuid);
        Concept concept = new Concept();
        concept.addName(new ConceptName(){{
            setUuid(conceptUuid);
            setName(conceptName);
        }});
        observation1.setConcept(concept);
        return observation1;
    }
}
