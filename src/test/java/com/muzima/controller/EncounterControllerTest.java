package com.muzima.controller;

import com.muzima.api.model.APIName;
import com.muzima.api.model.LastSyncTime;
import com.muzima.api.service.EncounterService;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.service.SntpService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Created by Thibaut on 2014/02/24.
 */
public class EncounterControllerTest {

    private EncounterController encounterController;
    private LastSyncTimeService lastSyncTimeService;
    private EncounterService encounterService;
    private SntpService sntpService;

    @Before
    public void setUp() throws Exception {
        encounterService = mock(EncounterService.class);
        lastSyncTimeService = mock(LastSyncTimeService.class);
        sntpService = mock(SntpService.class);
        encounterController = new EncounterController(encounterService, lastSyncTimeService, sntpService);
    }

    @Test
    public void shouldGetLastSyncTimeOfEncounter() throws Exception, EncounterController.DownloadEncounterException {
        List<String> patientUuids = asList(new String[]{"patientUuid1", "patientUuid2"});
        encounterController.downloadEncountersByPatientUuids(patientUuids);

        verify(lastSyncTimeService).getLastSyncTimeFor(APIName.DOWNLOAD_ENCOUNTERS,"patientUuid1,patientUuid2");
    }

    @Test
    public void shouldUseTheLastSyncTimeWhenDownloadingEncounters() throws Exception, EncounterController.DownloadEncounterException {
        Date lastSyncDate = mock(Date.class);
        when(lastSyncTimeService.getLastSyncTimeFor(APIName.DOWNLOAD_ENCOUNTERS,"patientUuid1,patientUuid2")).thenReturn(lastSyncDate);

        List<String> patientUuids = asList(new String[]{"patientUuid1", "patientUuid2"});
        encounterController.downloadEncountersByPatientUuids(patientUuids);
        verify(encounterService, never()).downloadEncountersByPatientUuids(anyList());
        verify(encounterService).downloadEncountersByPatientUuidsAndSyncDate(patientUuids, lastSyncDate);
    }

    @Test
    public void shouldSaveTheUpdatedLastSyncTime() throws Exception, EncounterController.DownloadEncounterException {
        List<String> patientUuids = asList(new String[]{"patientUuid1", "patientUuid2"});
        Date updatedDate = mock(Date.class);
        when(sntpService.getUTCTime()).thenReturn(updatedDate);
        encounterController.downloadEncountersByPatientUuids(patientUuids);

        ArgumentCaptor<LastSyncTime> argumentCaptor = ArgumentCaptor.forClass(LastSyncTime.class);
        verify(lastSyncTimeService).saveLastSyncTime(argumentCaptor.capture());
        LastSyncTime savedLastSyncTime = argumentCaptor.getValue();
        assertThat(savedLastSyncTime.getApiName(), is(APIName.DOWNLOAD_ENCOUNTERS));
        assertThat(savedLastSyncTime.getLastSyncDate(), is(updatedDate));
        assertThat(savedLastSyncTime.getParamSignature(), is("patientUuid1,patientUuid2"));
    }
}
