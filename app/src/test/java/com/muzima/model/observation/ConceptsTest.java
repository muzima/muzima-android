/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.model.observation;


import com.muzima.api.model.Concept;
import com.muzima.api.model.Observation;

import org.junit.Test;

import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.junit.matchers.JUnitMatchers.hasItems;

public class ConceptsTest {

    @Test
    public void shouldAddSingleObservation() {
        final Observation observation = createObservation(createConcept("uuid1"), "o1");
        assertThat(new Concepts(observation), hasItem(conceptWithObservations(observation)));
    }

    @Test
    public void shouldAddMultipleObservation() {
        final Concept concept = createConcept("uuid1");

        final Observation observation = createObservation(concept, "o1");
        final Observation observationTwo = createObservation(concept, "o2");

        assertThat(new Concepts(observation, observationTwo), hasItems(conceptWithObservations(observation, observationTwo)));
    }

    @Test
    public void shouldGroupObservationsByConcept() {

        Observation observation1 = createObservation(createConcept("c1"), "01");
        Observation observation2 = createObservation(createConcept("c2"), "02");
        Observation observation3 = createObservation(createConcept("c1"), "03");

        assertThat(new Concepts(observation1, observation2, observation3), hasItems(conceptWithObservations(observation1, observation3), conceptWithObservations(observation2)));
    }

    @Test
    public void shouldSortTheConceptsByDate() {
        final Observation observation1 = createObservation(createConcept("c1"), "01", new Date(1));
        final Observation observation2 = createObservation(createConcept("c2"), "02", new Date(3));
        final Observation observation3 = createObservation(createConcept("c1"), "03", new Date(2));

        final Concepts concepts = new Concepts(observation1, observation2, observation3);
        concepts.sortByDate();

        final Concepts expectedOrderedConcept = new Concepts() {{
            add(conceptWithObservations(observation2));
            add(conceptWithObservations(observation3, observation1));
        }};

        assertThat(concepts, is(expectedOrderedConcept));

    }

    private Observation createObservation(final Concept concept, final String uuid, final Date date) {
        return new Observation() {{
            setUuid(uuid);
            setConcept(concept);
            setObservationDatetime(date);
        }};
    }

    private ConceptWithObservations conceptWithObservations(Observation... observations) {
        final ConceptWithObservations withObservations = new ConceptWithObservations();
        for (Observation observation : observations) {
            withObservations.addObservation(observation);
        }
        return withObservations;
    }

    private Concept createConcept(final String uuid) {
        return new Concept() {{
            setUuid(uuid);
        }};
    }

    private Observation createObservation(final Concept concept, final String uuid) {
        return createObservation(concept, uuid, new Date());
    }
}
