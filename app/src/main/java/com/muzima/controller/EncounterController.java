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

import com.muzima.api.model.Encounter;
import com.muzima.api.model.LastSyncTime;
import com.muzima.api.service.EncounterService;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.service.SntpService;
import com.muzima.utils.StringUtils;
import com.muzima.api.model.EncounterType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.muzima.api.model.APIName.DOWNLOAD_ENCOUNTERS;
import static com.muzima.util.Constants.UUID_SEPARATOR;
import static java.util.Arrays.asList;

public class EncounterController {

    private final EncounterService encounterService;
    private final LastSyncTimeService lastSyncTimeService;
    private final SntpService sntpService;

    public EncounterController(EncounterService encounterService, LastSyncTimeService lastSyncTimeService, SntpService sntpService) {
        this.encounterService = encounterService;
        this.lastSyncTimeService = lastSyncTimeService;
        this.sntpService = sntpService;
    }

    public void replaceEncounters(List<Encounter> allEncounters) throws ReplaceEncounterException {
        try {
            encounterService.updateEncounters(allEncounters);
        } catch (IOException e) {
            throw new ReplaceEncounterException(e);
        }
    }
    public int getEncountersCountByPatient(String patientUuid) throws IOException {
        return encounterService.countEncountersByPatientUuid(patientUuid);
    }

    public List<Encounter>  getEncountersByPatientUuid(String patientUuid) throws DownloadEncounterException{
        try {
            return encounterService.getEncountersByPatientUuid(patientUuid);
        } catch (IOException e) {
            throw new DownloadEncounterException(e);
        }
    }

    public List<Encounter> downloadEncountersByPatientUuids(List<String> patientUuids) throws DownloadEncounterException {
        try {
            String paramSignature = StringUtils.getCommaSeparatedStringFromList(patientUuids);
            Date lastSyncTime = lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_ENCOUNTERS, paramSignature);
            List<Encounter> encounters = new ArrayList<>();
            List<String> previousPatientsUuid = new ArrayList<>();
            if (hasThisCallHappenedBefore(lastSyncTime)) {
                encounters.addAll(downloadEncounters(patientUuids, lastSyncTime));
            } else {
                //ToDo: Revise this while working on Encounter Delta download
                //previousPatientsUuid = updateEncountersAndReturnPrevPatientUUIDs(patientUuids, encounters, previousPatientsUuid);
                encounters.addAll(downloadEncounters(patientUuids, null));
            }
            LastSyncTime newLastSyncTime = new LastSyncTime(DOWNLOAD_ENCOUNTERS, sntpService.getLocalTime(), paramSignature);
            lastSyncTimeService.saveLastSyncTime(newLastSyncTime);
            return encounters;
        } catch (IOException e) {
            throw new DownloadEncounterException(e);
        }
    }

    private List<Encounter> downloadEncounters(List<String> patientUuids, Date lastSyncTime) throws IOException {
        return encounterService.downloadEncountersByPatientUuidsAndSyncDate(patientUuids, lastSyncTime);
    }

    private List<String> updateEncountersAndReturnPrevPatientUUIDs(List<String> patientUuids, List<Encounter> encounters, List<String> previousPatientsUuid) throws IOException {
        LastSyncTime lastSyncTimeRecorded = lastSyncTimeService.getFullLastSyncTimeInfoFor(DOWNLOAD_ENCOUNTERS);
        if (hasAnyDownloadHappened(lastSyncTimeRecorded)) {
            previousPatientsUuid = asList(lastSyncTimeRecorded.getParamSignature().split(UUID_SEPARATOR));
            encounters.addAll(downloadEncounters(previousPatientsUuid, lastSyncTimeRecorded.getLastSyncDate()));
            patientUuids.removeAll(previousPatientsUuid);
        }
        encounters.addAll(downloadEncounters(patientUuids, null));
        return previousPatientsUuid;
    }

    private boolean hasThisCallHappenedBefore(Date lastSyncTime) {
        return lastSyncTime != null;
    }

    private boolean hasAnyDownloadHappened(LastSyncTime lastSyncTimeRecorded) {
        return lastSyncTimeRecorded != null;
    }

    private String getUpdatedParam(List<String> patientUuids, List<String> previousPatientsUuid) {
        Set<String> allPatientUUIDs = new HashSet<>();
        allPatientUUIDs.addAll(patientUuids);
        allPatientUUIDs.addAll(previousPatientsUuid);
        List<String> allPatientUUIDList = new ArrayList<>(allPatientUUIDs);
        Collections.sort(allPatientUUIDList);
        return StringUtils.getCommaSeparatedStringFromList(allPatientUUIDList);
    }

    public void saveEncounters(List<Encounter> encounters) throws SaveEncounterException {
        try {
            encounterService.saveEncounters(encounters);
        } catch (IOException e) {
            throw new SaveEncounterException(e);
        }

    }

    public List<EncounterType> getEncounterTypes() throws DownloadEncounterException {
        try {
            return encounterService.getAllEncounterTypes();
        } catch (IOException e) {
            throw new DownloadEncounterException(e);
        }
    }

    public List<Encounter> getEncountersByEncounterTypeNameAndPatientUuid(String name,String patientUuid) throws DownloadEncounterException{
        try{
            return encounterService.getEncountersByEncounterTypeNameAndPatientUuid(name,patientUuid);
        }catch(IOException e){
            throw new DownloadEncounterException(e);
        }
    }

    public List<Encounter> getEncountersByEncounterTypeUuidAndPatientUuid(String encounterTypeUuid,String patientUuid) throws DownloadEncounterException{
        try{
            return encounterService.getEncountersByEncounterTypeUuidAndPatientUuid(encounterTypeUuid,patientUuid);
        }catch(IOException e){
            throw new DownloadEncounterException(e);
        }
    }

    public List<Encounter> getEncountersByEncounterTypeIdAndPatientUuid(int encounterTypeId,String patientUuid) throws DownloadEncounterException{
        try{
            return encounterService.getEncountersByEncounterTypeIdAndPatientUuid(encounterTypeId,patientUuid);
        }catch(IOException e){
            throw new DownloadEncounterException(e);
        }
    }

    public void deleteEncounters(List<Encounter> encounters) throws DeleteEncounterException {
        try {
            encounterService.deleteEncounters(encounters);
        } catch (IOException e) {
            throw new DeleteEncounterException(e);
        }
    }

    public void saveEncounter(Encounter encounter) throws SaveEncounterException {
        ArrayList<Encounter> encounters = new ArrayList<>();
        encounters.add(encounter);
        saveEncounters(encounters);
    }

    public class DownloadEncounterException extends Throwable {
        DownloadEncounterException(IOException e) {
            super(e);
        }
    }

    public class ReplaceEncounterException extends Throwable {
        ReplaceEncounterException(IOException e) {
            super(e);
        }
    }

    public class SaveEncounterException extends Throwable {
        SaveEncounterException(IOException e) {
            super(e);
        }
    }

    public class DeleteEncounterException extends Throwable {
        DeleteEncounterException(IOException e) {
            super(e);
        }
    }
}
