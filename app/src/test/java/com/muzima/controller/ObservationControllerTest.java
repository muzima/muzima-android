/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.controller;

import com.muzima.api.model.Concept;
import com.muzima.api.model.LastSyncTime;
import com.muzima.api.model.Observation;
import com.muzima.api.service.ConceptService;
import com.muzima.api.service.EncounterService;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.api.service.ObservationService;
import com.muzima.model.observation.ConceptWithObservations;
import com.muzima.service.SntpService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.muzima.api.model.APIName.DOWNLOAD_OBSERVATIONS;
import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ObservationControllerTest {

    private ObservationController observationController;
    private ObservationService observationService;
    private ConceptService conceptService;
    private LastSyncTimeService lastSyncTimeService;
    private SntpService sntpService;

    @Before
    public void setUp() {
        observationService = mock(ObservationService.class);
        conceptService = mock(ConceptService.class);
        EncounterService encounterService = mock(EncounterService.class);
        lastSyncTimeService = mock(LastSyncTimeService.class);
        sntpService = mock(SntpService.class);
        observationController = new ObservationController(observationService, conceptService,
                encounterService, lastSyncTimeService, sntpService);
    }

    @Test
    public void shouldCheckLastSyncTimeBeforeDownloadingObservations() throws Exception, ObservationController.DownloadObservationException {
        List<String> patientUuids = asList("PatientUuid1", "PatientUuid2");
        List<String> conceptUuids = asList("ConceptUuid1", "ConceptUuid2");
        Date aDate = mock(Date.class);
        when(lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_OBSERVATIONS, "PatientUuid1,PatientUuid2;ConceptUuid1,ConceptUuid2")).thenReturn(aDate);
        observationController.downloadObservationsByPatientUuidsAndConceptUuids(patientUuids, conceptUuids);
        verify(lastSyncTimeService).getLastSyncTimeFor(DOWNLOAD_OBSERVATIONS, "PatientUuid1,PatientUuid2;ConceptUuid1,ConceptUuid2");
        verify(lastSyncTimeService, never()).getFullLastSyncTimeInfoFor(DOWNLOAD_OBSERVATIONS);
    }

    @Test
    public void shouldUseLastSyncTimeToDownloadObservations() throws Exception, ObservationController.DownloadObservationException {
        List<String> patientUuids = asList("PatientUuid1", "PatientUuid2");
        List<String> conceptUuids = asList("ConceptUuid1", "ConceptUuid2");
        Date lastSyncTime = mock(Date.class);
        when(lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_OBSERVATIONS, "PatientUuid1,PatientUuid2;ConceptUuid1,ConceptUuid2")).thenReturn(lastSyncTime);

        observationController.downloadObservationsByPatientUuidsAndConceptUuids(patientUuids, conceptUuids);
        verify(observationService, never()).downloadObservationsByPatientUuidsAndConceptUuids(anyList(), anyList());
        verify(observationService).downloadObservations(patientUuids, conceptUuids, lastSyncTime);
    }

    @Test
    public void shouldUpdateLastSyncTimeForObservation() throws Exception, ObservationController.DownloadObservationException {
        List<String> patientUuids = asList("PatientUuid1", "PatientUuid2");
        List<String> conceptUuids = asList("ConceptUuid1", "ConceptUuid2");
        Date currentDate = mock(Date.class);
        when(sntpService.getLocalTime()).thenReturn(currentDate);

        observationController.downloadObservationsByPatientUuidsAndConceptUuids(patientUuids, conceptUuids);
        ArgumentCaptor<LastSyncTime> argumentCaptor = ArgumentCaptor.forClass(LastSyncTime.class);
        verify(lastSyncTimeService).saveLastSyncTime(argumentCaptor.capture());
        LastSyncTime savedLastSyncTime = argumentCaptor.getValue();
        assertThat(savedLastSyncTime.getApiName(), is(DOWNLOAD_OBSERVATIONS));
        assertThat(savedLastSyncTime.getLastSyncDate(), is(currentDate));
        assertThat(savedLastSyncTime.getParamSignature(), is("PatientUuid1,PatientUuid2;ConceptUuid1,ConceptUuid2"));
    }

    //ToDo: Revise Delta sync logic
    //@Test
    public void shouldProperlyProcessChangeInKnownPatientOrConcept() throws ObservationController.DownloadObservationException, IOException {
        List<String> patientUuids = asList("PatientUuid1", "PatientUuid2");
        List<String> conceptUuids = asList("ConceptUuid1", "ConceptUuid2");
        List<String> previousPatientUuids = asList("PatientUuid1", "PatientUuid3");
        List<String> previousConceptUuids = asList("ConceptUuid1", "ConceptUuid3");
        List<String> newPatientUuids = Collections.singletonList("PatientUuid2");
        List<String> newConceptUuids = Collections.singletonList("ConceptUuid2");
        LastSyncTime lastSyncTimeInFull = mock(LastSyncTime.class);
        when(lastSyncTimeInFull.getParamSignature()).thenReturn("PatientUuid1,PatientUuid3;ConceptUuid1,ConceptUuid3");
        Date aDate =  mock(Date.class);
        when(lastSyncTimeInFull.getLastSyncDate()).thenReturn(aDate);
        when(lastSyncTimeService.getFullLastSyncTimeInfoFor(DOWNLOAD_OBSERVATIONS)).thenReturn(lastSyncTimeInFull);
        List<Observation> anObservationSet = new ArrayList<>();
        Observation anObservation = mock(Observation.class);
        anObservationSet.add(anObservation);
        when(observationService.downloadObservations(previousPatientUuids, previousConceptUuids, aDate)).thenReturn(anObservationSet);
        List<Observation> anotherObservationSet = new ArrayList<>();
        Observation anotherObservation = mock(Observation.class);
        anotherObservationSet.add(anotherObservation);
        List<String> allConceptUuids = new ArrayList<>();
        allConceptUuids.addAll(previousConceptUuids);
        allConceptUuids.addAll(newConceptUuids);
        Collections.sort(allConceptUuids);
        when(observationService.downloadObservations(newPatientUuids, allConceptUuids, null)).thenReturn(anotherObservationSet);
        Date currentDate = mock(Date.class);
        when(sntpService.getLocalTime()).thenReturn(currentDate);

        List<Observation> observations = observationController.downloadObservationsByPatientUuidsAndConceptUuids(patientUuids, conceptUuids);

        verify(lastSyncTimeService).getFullLastSyncTimeInfoFor(DOWNLOAD_OBSERVATIONS);
        verify(observationService).downloadObservations(previousPatientUuids, previousConceptUuids, aDate);
        verify(observationService).downloadObservations(previousPatientUuids, newConceptUuids, null);
        verify(observationService).downloadObservations(newPatientUuids, allConceptUuids, null);
        assertThat(observations.size(), is(2));
        assertThat(observations, hasItems(anObservation, anotherObservation));

        ArgumentCaptor<LastSyncTime> argumentCaptor = ArgumentCaptor.forClass(LastSyncTime.class);
        verify(lastSyncTimeService).saveLastSyncTime(argumentCaptor.capture());
        LastSyncTime savedLastSyncTime = argumentCaptor.getValue();
        assertThat(savedLastSyncTime.getApiName(), is(DOWNLOAD_OBSERVATIONS));
        assertThat(savedLastSyncTime.getLastSyncDate(), is(currentDate));
        assertThat(savedLastSyncTime.getParamSignature(), is("PatientUuid1,PatientUuid2,PatientUuid3;ConceptUuid1,ConceptUuid2,ConceptUuid3"));
    }

    @Test
    public void shouldRecognisedNonInitialisedLastSyncTime() throws ObservationController.DownloadObservationException, IOException {
        List<String> patientUuids = asList("PatientUuid1", "PatientUuid2");
        List<String> conceptUuids = asList("ConceptUuid1", "ConceptUuid2");
        when(lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_OBSERVATIONS, "PatientUuid1,PatientUuid2;ConceptUuid1,ConceptUuid2")).thenReturn(null);
        when(lastSyncTimeService.getFullLastSyncTimeInfoFor(DOWNLOAD_OBSERVATIONS)).thenReturn(null);

        observationController.downloadObservationsByPatientUuidsAndConceptUuids(patientUuids, conceptUuids);

        verify(observationService).downloadObservations(patientUuids, conceptUuids, null);
    }

    @Test
    public void getConceptWithObservations_shouldReturnConceptsWithObservations() throws Exception, ObservationController.LoadObservationException {
        final Concept concept1 = new Concept() {{
            setUuid("concept1");
        }};
        final Concept concept2 = new Concept() {{
            setUuid("concept2");
        }};
        final List<Observation> observations = buildObservations(concept1, concept2);
        String patientUuid = "patientUuid";

        when(observationService.getObservationsByPatient(patientUuid)).thenReturn(observations);
        when(conceptService.getConceptByUuid("concept1")).thenReturn(concept1);
        when(conceptService.getConceptByUuid("concept2")).thenReturn(concept2);

        List<ConceptWithObservations> result = observationController.getConceptWithObservations(patientUuid);

        assertThat(result.size(), is(2));
        assertThat(result.get(0).getConcept().getUuid(), is("concept1"));
        assertThat(result.get(1).getConcept().getUuid(), is("concept2"));

        ConceptWithObservations conceptWithObservations1 = result.get(0);
        ConceptWithObservations conceptWithObservations2 = result.get(1);

        assertThat(conceptWithObservations1.getObservations().size(), is(3));
        assertThat(conceptWithObservations2.getObservations().size(), is(1));
    }

    @Test
    public void getConceptWithObservations_shouldReturnConceptsWithSortedObservations() throws Exception, ObservationController.LoadObservationException {
        final Concept concept1 = new Concept() {{
            setUuid("concept1");
        }};
        final Concept concept2 = new Concept() {{
            setUuid("concept2");
        }};
        final List<Observation> observations = buildObservations(concept1, concept2);
        String patientUuid = "patientUuid";

        when(observationService.getObservationsByPatient(patientUuid)).thenReturn(observations);
        when(conceptService.getConceptByUuid("concept1")).thenReturn(concept1);
        when(conceptService.getConceptByUuid("concept2")).thenReturn(concept2);

        List<ConceptWithObservations> result = observationController.getConceptWithObservations(patientUuid);

        ConceptWithObservations conceptWithObservations1 = result.get(0);

        assertThat(conceptWithObservations1.getObservations().get(0), is(observations.get(2)));
        assertThat(conceptWithObservations1.getObservations().get(1), is(observations.get(3)));
        assertThat(conceptWithObservations1.getObservations().get(2), is(observations.get(0)));
    }

    @Test
    public void saveObservations_shouldSaveObservationsForPatient() throws Exception, ObservationController.SaveObservationException {
        final Concept concept1 = new Concept() {{
            setUuid("concept1");
        }};
        final Concept concept2 = new Concept() {{
            setUuid("concept2");
        }};
        final List<Observation> observations = buildObservations(concept1, concept2);

        observationController.saveObservations(observations);
        verify(observationService).saveObservations(observations);
    }

    private List<Observation> buildObservations(final Concept concept1, final Concept concept2) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        final Date date1 = sdf.parse("21/12/2012");
        final Date date2 = sdf.parse("25/12/2012");
        final Date date3 = sdf.parse("09/04/2013");

        ArrayList<Observation> observations = new ArrayList<>();
        final Observation ob1 = new Observation() {{
            setUuid("ob1");
            setConcept(concept1);
            setObservationDatetime(date1);
        }};
        final Observation ob2 = new Observation() {{
            setUuid("ob2");
            setConcept(concept2);
            setObservationDatetime(date2);
        }};
        final Observation ob3 = new Observation() {{
            setUuid("ob3");
            setConcept(concept1);
            setObservationDatetime(date3);
        }};
        final Observation ob4 = new Observation() {{
            setUuid("ob4");
            setConcept(concept1);
            setObservationDatetime(date2);
        }};

        observations.add(ob1);
        observations.add(ob2);
        observations.add(ob3);
        observations.add(ob4);
        return observations;
    }
}
