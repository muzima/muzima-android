package com.muzima.controller;

import com.muzima.api.model.Encounter;
import com.muzima.api.model.LastSyncTime;
import com.muzima.api.service.EncounterService;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.service.SntpService;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.*;

import static com.muzima.api.model.APIName.DOWNLOAD_ENCOUNTERS;
import static java.util.Arrays.asList;
import static com.muzima.util.Constants.UUID_SEPARATOR;

public class EncounterController {

    private EncounterService encounterService;
    private LastSyncTimeService lastSyncTimeService;
    private SntpService sntpService;

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

    public List<Encounter>  getEncountersByPatientUuid(String patientUuid) throws DownloadEncounterException{
        try {
            return encounterService.getEncountersByPatientUuid(patientUuid);
        } catch (IOException e) {
            throw new DownloadEncounterException(e);
        }
    }

    public List<Encounter> downloadEncountersByPatientUuids(List<String> patientUuids) throws DownloadEncounterException {
        try {
            String paramSignature = StringUtils.join(patientUuids, UUID_SEPARATOR);
            Date lastSyncTime = lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_ENCOUNTERS, paramSignature);
            List<Encounter> encounters = new ArrayList<Encounter>();
            List<String> previousPatientsUuid = new ArrayList<String>();
            if (hasThisCallHappenedBefore(lastSyncTime)) {
                encounters.addAll(downloadEncounters(patientUuids, lastSyncTime));
            } else {
                previousPatientsUuid = updateEncountersAndReturnPrevPatientUUIDs(patientUuids, encounters, previousPatientsUuid);
            }
            LastSyncTime newLastSyncTime = new LastSyncTime(DOWNLOAD_ENCOUNTERS, sntpService.getLocalTime(), getUpdatedParam(patientUuids, previousPatientsUuid));
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
        Set<String> allPatientUUIDs = new HashSet<String>();
        allPatientUUIDs.addAll(patientUuids);
        allPatientUUIDs.addAll(previousPatientsUuid);
        List<String> allPatientUUIDList = new ArrayList<String>(allPatientUUIDs);
        Collections.sort(allPatientUUIDList);
        return StringUtils.join(allPatientUUIDList, UUID_SEPARATOR);
    }

    public void saveEncounters(List<Encounter> encounters) throws SaveEncounterException {
        try {
            encounterService.saveEncounters(encounters);
        } catch (IOException e) {
            throw new SaveEncounterException(e);
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
        ArrayList<Encounter> encounters = new ArrayList<Encounter>();
        encounters.add(encounter);
        saveEncounters(encounters);
    }

    public class DownloadEncounterException extends Throwable {
        public DownloadEncounterException(IOException e) {
            super(e);
        }
    }

    public class ReplaceEncounterException extends Throwable {
        public ReplaceEncounterException(IOException e) {
            super(e);
        }
    }

    public class SaveEncounterException extends Throwable {
        public SaveEncounterException(IOException e) {
            super(e);
        }
    }

    public class DeleteEncounterException extends Throwable {
        public DeleteEncounterException(IOException e) {
            super(e);
        }
    }
}
