/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.controller;

import com.muzima.api.model.Encounter;
import com.muzima.api.model.LastSyncTime;
import com.muzima.api.service.EncounterService;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.service.SntpService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.muzima.api.model.APIName.DOWNLOAD_ENCOUNTERS;
import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItems;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 */
public class EncounterControllerTest {

    private EncounterController encounterController;
    private LastSyncTimeService lastSyncTimeService;
    private EncounterService encounterService;
    private SntpService sntpService;

    @Before
    public void setUp() {
        encounterService = mock(EncounterService.class);
        lastSyncTimeService = mock(LastSyncTimeService.class);
        sntpService = mock(SntpService.class);
        encounterController = new EncounterController(encounterService, lastSyncTimeService, sntpService);
    }

    @Test
    public void shouldGetLastSyncTimeOfEncounter() throws Exception, EncounterController.DownloadEncounterException {
        List<String> patientUuids = asList("patientUuid1", "patientUuid2");
        Date aDate = mock(Date.class);
        when(lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_ENCOUNTERS,"patientUuid1,patientUuid2")).thenReturn(aDate);
        encounterController.downloadEncountersByPatientUuids(patientUuids);

        verify(lastSyncTimeService).getLastSyncTimeFor(DOWNLOAD_ENCOUNTERS,"patientUuid1,patientUuid2");
        verify(lastSyncTimeService, never()).getFullLastSyncTimeInfoFor(DOWNLOAD_ENCOUNTERS);
    }

    @Test
    public void shouldUseTheLastSyncTimeWhenDownloadingEncounters() throws Exception, EncounterController.DownloadEncounterException {
        Date lastSyncDate = mock(Date.class);
        when(lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_ENCOUNTERS,"patientUuid1,patientUuid2")).thenReturn(lastSyncDate);

        List<String> patientUuids = asList("patientUuid1", "patientUuid2");
        encounterController.downloadEncountersByPatientUuids(patientUuids);
        verify(encounterService, never()).downloadEncountersByPatientUuids(anyList());
        verify(encounterService).downloadEncountersByPatientUuidsAndSyncDate(patientUuids, lastSyncDate);
    }

    @Test
    public void shouldSaveTheUpdatedLastSyncTime() throws Exception, EncounterController.DownloadEncounterException {
        List<String> patientUuids = asList("patientUuid1", "patientUuid2");
        Date updatedDate = mock(Date.class);
        when(sntpService.getLocalTime()).thenReturn(updatedDate);
        Date lastSyncDate = mock(Date.class);
        when(lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_ENCOUNTERS,"patientUuid1,patientUuid2")).thenReturn(lastSyncDate);
        encounterController.downloadEncountersByPatientUuids(patientUuids);

        ArgumentCaptor<LastSyncTime> argumentCaptor = ArgumentCaptor.forClass(LastSyncTime.class);
        verify(lastSyncTimeService).saveLastSyncTime(argumentCaptor.capture());
        LastSyncTime savedLastSyncTime = argumentCaptor.getValue();
        assertThat(savedLastSyncTime.getApiName(), is(DOWNLOAD_ENCOUNTERS));
        assertThat(savedLastSyncTime.getLastSyncDate(), is(updatedDate));
        assertThat(savedLastSyncTime.getParamSignature(), is("patientUuid1,patientUuid2"));
    }

    //ToDo: Revise Delta sync logic
    //@Test
    public void shouldUpdateLastSyncTimeParamSignatureWhenThereIsAChangeInKnownPatient() throws EncounterController.DownloadEncounterException, IOException {
        List<String> patientUuids = new ArrayList<>(asList("patientUuid1", "patientUuid2"));
        List<String> previouslySynchedPatient = asList("patientUuid1", "patientUuid3");
        List<String> newPatients = asList("patientUuid2");
        when(lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_ENCOUNTERS,"patientUuid1,patientUuid2")).thenReturn(null);
        LastSyncTime fullLastSyncTime = mock(LastSyncTime.class);
        when(fullLastSyncTime.getParamSignature()).thenReturn("patientUuid1,patientUuid3");
        Date lastSyncTime = mock(Date.class);
        when(fullLastSyncTime.getLastSyncDate()).thenReturn(lastSyncTime);
        when(lastSyncTimeService.getFullLastSyncTimeInfoFor(DOWNLOAD_ENCOUNTERS)).thenReturn(fullLastSyncTime);
        Encounter anEncounter = mock(Encounter.class);
        Encounter anotherEncounter = mock(Encounter.class);
        ArrayList<Encounter> someEncounters = new ArrayList<>();
        someEncounters.add(anEncounter);
        ArrayList<Encounter> someOtherEncounters = new ArrayList<>();
        someOtherEncounters.add(anotherEncounter);
        when(encounterService.downloadEncountersByPatientUuidsAndSyncDate(previouslySynchedPatient, lastSyncTime)).thenReturn(someEncounters);
        when(encounterService.downloadEncountersByPatientUuidsAndSyncDate(newPatients, null)).thenReturn(someOtherEncounters);
        Date updatedDate = mock(Date.class);
        when(sntpService.getLocalTime()).thenReturn(updatedDate);

        encounterController.downloadEncountersByPatientUuids(patientUuids);

        ArgumentCaptor<LastSyncTime> argumentCaptor = ArgumentCaptor.forClass(LastSyncTime.class);
        verify(lastSyncTimeService).saveLastSyncTime(argumentCaptor.capture());
        LastSyncTime savedLastSyncTime = argumentCaptor.getValue();
        assertThat(savedLastSyncTime.getApiName(), is(DOWNLOAD_ENCOUNTERS));
        assertThat(savedLastSyncTime.getLastSyncDate(), is(updatedDate));
        assertThat(savedLastSyncTime.getParamSignature(), is("patientUuid1,patientUuid2,patientUuid3"));
    }

    //ToDo: Revise Delta sync logic
    //@Test
    public void shouldReturnEncountersDownloadedForOldPatientAndNewOnes() throws IOException, EncounterController.DownloadEncounterException {
        List<String> patientUuids = new ArrayList<>(asList("patientUuid1", "patientUuid2"));
        List<String> previouslySynchedPatient = asList("patientUuid1", "patientUuid3");
        List<String> newPatients = asList("patientUuid2");
        LastSyncTime fullLastSyncTime = mock(LastSyncTime.class);
        Date lastSyncTime = mock(Date.class);
        when(lastSyncTimeService.getFullLastSyncTimeInfoFor(DOWNLOAD_ENCOUNTERS)).thenReturn(fullLastSyncTime);
        when(fullLastSyncTime.getParamSignature()).thenReturn("patientUuid1,patientUuid3");
        when(fullLastSyncTime.getLastSyncDate()).thenReturn(lastSyncTime);
        Encounter anotherEncounter = mock(Encounter.class);
        Encounter anEncounter = mock(Encounter.class);
        ArrayList<Encounter> someEncounters = new ArrayList<>();
        someEncounters.add(anEncounter);
        ArrayList<Encounter> someOtherEncounters = new ArrayList<>();
        someOtherEncounters.add(anotherEncounter);
        when(encounterService.downloadEncountersByPatientUuidsAndSyncDate(previouslySynchedPatient, lastSyncTime)).thenReturn(someEncounters);
        when(encounterService.downloadEncountersByPatientUuidsAndSyncDate(newPatients, null)).thenReturn(someOtherEncounters);

        List<Encounter> encounters = encounterController.downloadEncountersByPatientUuids(patientUuids);

        assertThat(encounters, hasItems(anEncounter, anotherEncounter));
        assertThat(encounters.size(), is(2));
    }

    //ToDo: Revise Delta sync logic
    //@Test
    public void shouldMakeTwoSeparateDownloadCallsForAChangeInKnownPatient() throws EncounterController.DownloadEncounterException, IOException {
        List<String> patientUuids = new ArrayList<>(asList("patientUuid1", "patientUuid2"));
        List<String> previouslySynchedPatient = asList("patientUuid1", "patientUuid3");
        List<String> newPatients = asList("patientUuid2");
        LastSyncTime fullLastSyncTime = mock(LastSyncTime.class);
        Date lastSyncTime = mock(Date.class);
        when(lastSyncTimeService.getFullLastSyncTimeInfoFor(DOWNLOAD_ENCOUNTERS)).thenReturn(fullLastSyncTime);
        when(fullLastSyncTime.getParamSignature()).thenReturn("patientUuid1,patientUuid3");
        when(fullLastSyncTime.getLastSyncDate()).thenReturn(lastSyncTime);

        encounterController.downloadEncountersByPatientUuids(patientUuids);

        verify(encounterService).downloadEncountersByPatientUuidsAndSyncDate(previouslySynchedPatient, lastSyncTime);
        verify(encounterService).downloadEncountersByPatientUuidsAndSyncDate(newPatients, null);
    }

    //ToDo: Revise Delta sync logic
    //@Test
    public void shouldGetLastRecordedEntryForEncounterLastSyncTimeWhenCurrentPatientListDoesntHaveLastSyncTime() throws EncounterController.DownloadEncounterException, IOException {
        List<String> patientUuids = new ArrayList<>(asList("patientUuid1", "patientUuid2"));
        when(lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_ENCOUNTERS,"patientUuid1,patientUuid2")).thenReturn(null);

        encounterController.downloadEncountersByPatientUuids(patientUuids);

        verify(lastSyncTimeService).getFullLastSyncTimeInfoFor(DOWNLOAD_ENCOUNTERS);
    }

    @Test
    public void shouldRecognisedAnInitiallyNonInitialisedLastSyncTime() throws EncounterController.DownloadEncounterException, IOException {
        List<String> newPatients = asList("patientUuid2");
        when(lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_ENCOUNTERS,"patientUuid1,patientUuid2")).thenReturn(null);
        when(lastSyncTimeService.getFullLastSyncTimeInfoFor(DOWNLOAD_ENCOUNTERS)).thenReturn(null);

        List<String> mPatientUuids = asList("patientUuid1", "patientUuid2");
        encounterController.downloadEncountersByPatientUuids(mPatientUuids);

        verify(encounterService, never()).downloadEncountersByPatientUuidsAndSyncDate(newPatients, null);
    }
}

