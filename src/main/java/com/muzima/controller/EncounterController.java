package com.muzima.controller;

import com.muzima.api.model.Encounter;
import com.muzima.api.model.LastSyncTime;
import com.muzima.api.service.EncounterService;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.service.SntpService;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.muzima.api.model.APIName.DOWNLOAD_ENCOUNTERS;

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

    public List<Encounter> downloadEncountersByPatientUuids(List<String> patientUuids) throws DownloadEncounterException {
        try {
            String paramSignature = StringUtils.join(patientUuids, ",");
            Date lastSyncTime = lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_ENCOUNTERS, paramSignature);
            List<Encounter> encounters = encounterService.downloadEncountersByPatientUuidsAndSyncDate(patientUuids, lastSyncTime);

            LastSyncTime newLastSyncTime = new LastSyncTime(DOWNLOAD_ENCOUNTERS, sntpService.getLocalTime(), paramSignature);
            lastSyncTimeService.saveLastSyncTime(newLastSyncTime);
            return encounters;
        } catch (IOException e) {
            throw new DownloadEncounterException(e);
        }
    }

    public void saveEncounters(List<Encounter> encounters) throws SaveEncounterException {
        try {
            encounterService.saveEncounters(encounters);
        } catch (IOException e) {
            throw new SaveEncounterException(e);
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
}
