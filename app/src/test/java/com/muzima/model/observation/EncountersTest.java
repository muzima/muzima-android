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


import com.muzima.api.model.Encounter;
import com.muzima.api.model.Observation;
import org.junit.Test;

import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.junit.matchers.JUnitMatchers.hasItems;

public class EncountersTest {

    @Test
    public void shouldAddSingleObservation() {
        final Observation observation = createObservation(createEncounter("uuid1"), "o1");
        assertThat(new Encounters(observation), hasItem(encounterWithObservations(observation)));
    }

    @Test
    public void shouldAddMultipleObservation() {
        final Encounter encounter = createEncounter("uuid1");

        final Observation observation = createObservation(encounter, "o1");
        final Observation observationTwo = createObservation(encounter, "o2");

        assertThat(new Encounters(observation, observationTwo), hasItems(encounterWithObservations(observation, observationTwo)));
    }

    @Test
    public void shouldGroupObservationsByEncounter() {

        Observation observation1 = createObservation(createEncounter("c1"), "01");
        Observation observation2 = createObservation(createEncounter("c2"), "02");
        Observation observation3 = createObservation(createEncounter("c1"), "03");

        assertThat(new Encounters(observation1, observation2, observation3), hasItems(encounterWithObservations(observation1, observation3), encounterWithObservations(observation2)));
    }

    @Test
    public void shouldSortTheEncountersByDate() {
        final Observation observation1 = createObservation(createEncounter("c1", new Date(1)), "01");
        final Observation observation2 = createObservation(createEncounter("c2", new Date(3)), "02");

        final Encounters encounters = new Encounters(observation1, observation2);
        encounters.sortByDate();

        final Encounters expectedOrderedConcept = new Encounters() {{
            add(encounterWithObservations(observation2));
            add(encounterWithObservations(observation1));
        }};

        assertThat(encounters, is(expectedOrderedConcept));
    }

    @Test
    public void shouldPutEncounterWithNullDateAtTheTopWhenItsNotAtTheTop() {
        final Observation observation1 = createObservation(createEncounter("c1", new Date(1)), "01");
        final Observation observation2 = createObservation(createEncounter("c2", null), "02");

        final Encounters encounters = new Encounters(observation1, observation2);
        encounters.sortByDate();

        final Encounters expectedOrderedConcept = new Encounters() {{
            add(encounterWithObservations(observation2));
            add(encounterWithObservations(observation1));
        }};

        assertThat(encounters, is(expectedOrderedConcept));
    }

    @Test
    public void shouldPutEncounterWithNullDateAtTheTopWhenItsAtTheTop() {
        final Observation observation1 = createObservation(createEncounter("c1", null), "01");
        final Observation observation2 = createObservation(createEncounter("c2", new Date(1)), "02");

        final Encounters encounters = new Encounters(observation1, observation2);
        encounters.sortByDate();

        final Encounters expectedOrderedConcept = new Encounters() {{
            add(encounterWithObservations(observation1));
            add(encounterWithObservations(observation2));
        }};

        assertThat(encounters, is(expectedOrderedConcept));
    }

    private Encounter createEncounter(final String uuid, final Date date) {
        return new Encounter(){{
            setUuid(uuid);
            setEncounterDatetime(date);
        }};
    }

    private Observation createObservation(final Encounter encounter, final String uuid, final Date date) {
        return new Observation() {{
            setUuid(uuid);
            setEncounter(encounter);
            setObservationDatetime(date);
        }};
    }

    private EncounterWithObservations encounterWithObservations(Observation... observations) {
        final EncounterWithObservations withObservations = new EncounterWithObservations();
        for (Observation observation : observations) {
            withObservations.addObservation(observation);
        }
        return withObservations;
    }

    private Encounter createEncounter(final String uuid) {
        return new Encounter() {{
            setUuid(uuid);
        }};
    }

    private Observation createObservation(final Encounter encounter, final String uuid) {
        return createObservation(encounter, uuid, new Date());
    }
}
