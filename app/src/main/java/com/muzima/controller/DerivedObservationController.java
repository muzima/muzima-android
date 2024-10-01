/*
 * Copyright (c) Vanderbilt University Medical Center and Lambda Informatics.
 * All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.controller;

import static com.muzima.api.model.APIName.DOWNLOAD_DERIVED_OBSERVATIONS;
import static com.muzima.util.Constants.UUID_TYPE_SEPARATOR;

import com.muzima.api.model.DerivedConcept;
import com.muzima.api.model.DerivedObservation;
import com.muzima.api.model.LastSyncTime;
import com.muzima.api.service.DerivedObservationService;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.service.SntpService;
import com.muzima.utils.StringUtils;

import org.apache.lucene.queryParser.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DerivedObservationController {
    private DerivedObservationService derivedObservationService;
    private LastSyncTimeService lastSyncTimeService;
    private SntpService sntpService;

    public DerivedObservationController(DerivedObservationService derivedObservationService, LastSyncTimeService lastSyncTimeService, SntpService sntpService){
        this.derivedObservationService = derivedObservationService;
        this.lastSyncTimeService = lastSyncTimeService;
        this.sntpService = sntpService;
    }

    public List<DerivedObservation> downloadDerivedObservationsByPatientUuidsAndConceptUuids(List<String> patientUuids, List<String> derivedConceptUuids, String activeSetupConfigUuid) throws DerivedObservationDownloadException {
        try {
            String paramSignature = buildParamSignature(patientUuids, derivedConceptUuids);
            Date lastSyncTime = lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_DERIVED_OBSERVATIONS, paramSignature);
            List<DerivedObservation> derivedObservations = new ArrayList<>();
            if (hasExactCallBeenMadeBefore(lastSyncTime)) {
                derivedObservations.addAll(derivedObservationService.downloadDerivedObservationsByPatientUuidsAndDerivedConceptUuidsAndSyncDate(patientUuids, derivedConceptUuids, lastSyncTime, activeSetupConfigUuid));
            } else {
                derivedObservations.addAll(derivedObservationService.downloadDerivedObservationsByPatientUuidsAndDerivedConceptUuidsAndSyncDate(patientUuids, derivedConceptUuids, null, activeSetupConfigUuid));
            }
            LastSyncTime newLastSyncTime = new LastSyncTime(DOWNLOAD_DERIVED_OBSERVATIONS, sntpService.getTimePerDeviceTimeZone(), paramSignature);
            lastSyncTimeService.saveLastSyncTime(newLastSyncTime);
            return derivedObservations;
        } catch (IOException e) {
            throw new DerivedObservationDownloadException(e);
        }
    }

    private boolean hasExactCallBeenMadeBefore(Date lastSyncTime) {
        return lastSyncTime != null;
    }

    private String buildParamSignature(List<String> patientUuids, List<String> conceptUuids) {
        String paramSignature = StringUtils.getCommaSeparatedStringFromList(patientUuids);
        paramSignature += UUID_TYPE_SEPARATOR;
        paramSignature += StringUtils.getCommaSeparatedStringFromList(conceptUuids);
        return paramSignature;
    }
    
    public List<DerivedObservation> getDerivedObservationByPatientUuid(String patientUuid) throws DerivedObservationFetchException {
        try {
            return derivedObservationService.getDerivedObservationsByPatientUuid(patientUuid);
        } catch (IOException e) {
            throw new DerivedObservationFetchException(e);
        }
    }

    public List<DerivedObservation> getDerivedObservationByDerivedConceptUuid(String derivedConceptUuid) throws DerivedObservationFetchException {
        try {
            return derivedObservationService.getDerivedObservationsByDerivedConceptUuid(derivedConceptUuid);
        } catch (IOException e) {
            throw new DerivedObservationFetchException(e);
        }
    }

    public List<DerivedObservation> getDerivedObservationsByPatientUuidAndCreationDate(String patientUuid, Date date) throws DerivedObservationFetchException {
        try {
            return derivedObservationService.getDerivedObservationsByPatientUuidAndCreationDate(patientUuid, date);
        } catch (IOException | ParseException e) {
            throw new DerivedObservationFetchException(e);
        }
    }

    public List<DerivedObservation> getDerivedObservationByPatientUuidAndDerivedConceptUuid(String patientUuid, String derivedConceptUuid) throws DerivedObservationFetchException {
        try {
            return derivedObservationService.getDerivedObservationsByPatientUuidAndDerivedConceptUuid(patientUuid,derivedConceptUuid);
        } catch (IOException e) {
            throw new DerivedObservationFetchException(e);
        }
    }

    public List<DerivedObservation> getDerivedObservationByPatientUuidAndDerivedConceptId(String patientUuid, int derivedConceptId) throws DerivedObservationFetchException {
        try {
            return derivedObservationService.getDerivedObservationsByPatientUuidAndDerivedConceptId(patientUuid,derivedConceptId);
        } catch (IOException e) {
            throw new DerivedObservationFetchException(e);
        }
    }

    public void saveDerivedObservations(List<DerivedObservation> derivedObservations) throws DerivedObservationSaveException {
        try {
            if(derivedObservations.size()>0) {
                derivedObservationService.saveDerivedObservations(derivedObservations);
            }
        } catch (IOException e) {
            throw new DerivedObservationSaveException(e);
        }
    }

    public void updateDerivedObservations(List<DerivedObservation> derivedObservations) throws  DerivedObservationSaveException {
        try {
            derivedObservationService.updateDerivedObservations(derivedObservations);
        } catch (IOException e) {
            throw new DerivedObservationSaveException(e);
        }
    }

    public void deleteDerivedObservations(List<DerivedObservation> derivedObservations) throws DerivedObservationDeleteException {
        try {
            derivedObservationService.deleteDerivedObservations(derivedObservations);
        } catch (IOException e) {
            throw new DerivedObservationDeleteException(e);
        }
    }

    public void deleteDerivedObservationsForDerivedConcepts(List<DerivedConcept> derivedConcepts) throws DerivedObservationDeleteException {
        try {
            derivedObservationService.deleteDerivedObservations(getDerivedObservationsByDerivedConcepts(derivedConcepts));
        } catch (IOException e) {
            throw new DerivedObservationDeleteException(e);
        }
    }

    private List<DerivedObservation> getDerivedObservationsByDerivedConcepts(List<DerivedConcept> derivedConcepts) throws IOException {
        ArrayList<DerivedObservation> derivedObservations = new ArrayList<>();
        for (DerivedConcept derivedConcept : derivedConcepts) {
            derivedObservations.addAll(derivedObservationService.getDerivedObservationsByDerivedConcept(derivedConcept));
        }
        return derivedObservations;
    }

    public boolean getDerivedObservationsByPatientUuidAndAfterIndexCaseMembershipDate(String personUuid, Date membershipDate, int derivedConceptId) throws IOException {
        List<DerivedObservation> derivedObservations = derivedObservationService.getDerivedObservationsByPatientUuidAndDerivedConceptId(personUuid,derivedConceptId);
        for(DerivedObservation derivedObservation : derivedObservations){
            if(membershipDate.before(derivedObservation.getDateCreated()) || membershipDate.equals(derivedObservation.getDateCreated())){
                return true;
            }
        }
        return false;
    }

    public static class DerivedObservationDownloadException extends Throwable {
        DerivedObservationDownloadException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class DerivedObservationSaveException extends Throwable {
        DerivedObservationSaveException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class DerivedObservationFetchException extends Throwable {
        DerivedObservationFetchException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class DerivedObservationDeleteException extends Throwable {
        DerivedObservationDeleteException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class ParseDerivedObservationException extends Throwable {
        public ParseDerivedObservationException(Throwable e) {
            super(e);
        }
        public ParseDerivedObservationException(String message) {
            super(message);
        }
    }
}
