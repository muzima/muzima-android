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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.muzima.api.model.APIName.DOWNLOAD_OBSERVATIONS;
import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
public class ObservationControllerTest {

    private ObservationController observationController;
    private ObservationService observationService;
    private ConceptService conceptService;
    private LastSyncTimeService lastSyncTimeService;
    private SntpService sntpService;

    @Before
    public void setUp() throws Exception {
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
        List<String> patientUuids = asList(new String[]{"PatientUuid1", "PatientUuid2"});
        List<String> conceptUuids = asList(new String[]{"ConceptUuid1", "ConceptUuid2"});
        observationController.downloadObservationsByPatientUuidsAndConceptUuids(patientUuids, conceptUuids);
        verify(lastSyncTimeService).getLastSyncTimeFor(DOWNLOAD_OBSERVATIONS, "PatientUuid1,PatientUuid2|ConceptUuid1,ConceptUuid2");
    }

    @Test
    public void shouldUseLastSyncTimeToDownloadObservations() throws Exception, ObservationController.DownloadObservationException {
        List<String> patientUuids = asList(new String[]{"PatientUuid1", "PatientUuid2"});
        List<String> conceptUuids = asList(new String[]{"ConceptUuid1", "ConceptUuid2"});
        Date lastSyncTime = mock(Date.class);
        when(lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_OBSERVATIONS, "PatientUuid1,PatientUuid2|ConceptUuid1,ConceptUuid2")).thenReturn(lastSyncTime);

        observationController.downloadObservationsByPatientUuidsAndConceptUuids(patientUuids, conceptUuids);
        verify(observationService, never()).downloadObservationsByPatientUuidsAndConceptUuids(anyList(), anyList());
        verify(observationService).downloadObservations(patientUuids, conceptUuids, lastSyncTime);
    }

    @Test
    public void shouldUpdateLastSyncTimeForObservation() throws Exception, ObservationController.DownloadObservationException {
        List<String> patientUuids = asList(new String[]{"PatientUuid1", "PatientUuid2"});
        List<String> conceptUuids = asList(new String[]{"ConceptUuid1", "ConceptUuid2"});
        Date currentDate = mock(Date.class);
        when(sntpService.getLocalTime()).thenReturn(currentDate);

        observationController.downloadObservationsByPatientUuidsAndConceptUuids(patientUuids, conceptUuids);
        ArgumentCaptor<LastSyncTime> argumentCaptor = ArgumentCaptor.forClass(LastSyncTime.class);
        verify(lastSyncTimeService).saveLastSyncTime(argumentCaptor.capture());
        LastSyncTime savedLastSyncTime = argumentCaptor.getValue();
        assertThat(savedLastSyncTime.getApiName(), is(DOWNLOAD_OBSERVATIONS));
        assertThat(savedLastSyncTime.getLastSyncDate(), is(currentDate));
        assertThat(savedLastSyncTime.getParamSignature(), is("PatientUuid1,PatientUuid2|ConceptUuid1,ConceptUuid2"));
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

        ArrayList<Observation> observations = new ArrayList<Observation>();
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
