/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.controller;

import com.muzima.api.model.Encounter;
import com.muzima.api.service.EncounterService;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.service.SntpService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EncounterController {

    private final EncounterService encounterService;
    private final LastSyncTimeService lastSyncTimeService;
    private final SntpService sntpService;

    public EncounterController(EncounterService encounterService, LastSyncTimeService lastSyncTimeService, SntpService sntpService) {
        this.encounterService = encounterService;
        this.lastSyncTimeService = lastSyncTimeService;
        this.sntpService = sntpService;
    }

    public List<Encounter>  getEncountersByPatientUuid(String patientUuid) throws FetchEncounterException{
        try {
            return encounterService.getEncountersByPatientUuid(patientUuid);
        } catch (IOException e) {
            throw new FetchEncounterException(e);
        }
    }

    public void saveEncounters(List<Encounter> encounters) throws SaveEncounterException {
        try {
            encounterService.saveEncounters(encounters);
        } catch (IOException e) {
            throw new SaveEncounterException(e);
        }

    }

    public List<Encounter> getEncountersByEncounterTypeUuidAndPatientUuid(String encounterTypeUuid,String patientUuid) throws FetchEncounterException{
        try{
            return encounterService.getEncountersByEncounterTypeUuidAndPatientUuid(encounterTypeUuid,patientUuid);
        }catch(IOException e){
            throw new FetchEncounterException(e);
        }
    }

    public Encounter getEncounterByUuid(String encounterUuid) throws IOException {
        return encounterService.getEncounterByUuid(encounterUuid);
    }

    public List<Encounter> getAllEncounters() throws FetchEncounterException {
        try {
            return encounterService.getAllEncounters();
        } catch (IOException e) {
            throw new FetchEncounterException(e);
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

    public class FetchEncounterException extends Throwable {
        FetchEncounterException(IOException e) {
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
