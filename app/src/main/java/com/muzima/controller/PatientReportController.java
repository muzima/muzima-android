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

import android.util.Log;

import com.muzima.api.model.APIName;
import com.muzima.api.model.LastSyncTime;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PatientReport;
import com.muzima.api.model.PatientReportHeader;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.api.service.PatientReportService;
import com.muzima.service.SntpService;
import com.muzima.utils.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import static com.muzima.api.model.APIName.DOWNLOAD_PATIENT_REPORTS;
import static com.muzima.util.Constants.UUID_SEPARATOR;
import static java.util.Arrays.asList;

public class PatientReportController {
    
    private static final String TAG = "PatientReportController";
    
    private PatientReportService patientReportService;
    private final LastSyncTimeService lastSyncTimeService;
    private final SntpService sntpService;


    public PatientReportController(PatientReportService patientReportService, LastSyncTimeService lastSyncTimeService,SntpService sntpService) {
        this.patientReportService = patientReportService;
        this.lastSyncTimeService = lastSyncTimeService;
        this.sntpService = sntpService;
    }

    public int getPatientReportCountByPatientUuid(String patientUuid) throws IOException {
        return patientReportService.countReportsByPatientUuid(patientUuid);
    }

    public List<PatientReportHeader> getPatientReportHeadersByPatientUuid(String patientUuid) throws PatientReportException {
        try {
            return patientReportService.getPatientReportHeadersByPatientUuid(patientUuid);
        }
        catch (IOException e) {
            throw new PatientReportException(e);
        }
    }
    
    public List<PatientReport> getPatientReportsByPatientUuid(String patientUuid) throws PatientReportException {
        try {
            return patientReportService.getPatientReportByPatientUuid(patientUuid);
        }
        catch (IOException e) {
            throw new PatientReportException(e);
        }
    }
    
    public PatientReport getPatientReportByUuid(String uuid) throws PatientReportFetchException {
        try {
            return patientReportService.getPatientReportByUuid(uuid);
        }
        catch (IOException e) {
            throw new PatientReportFetchException(e);
        }
    }

    public List<PatientReportHeader> downloadPatientReportHeadersByPatientUuid(String patientUuid)
            throws PatientReportDownloadException {
        try {
            return patientReportService.downloadPatientReportHeadersByPatientUuid(patientUuid);
        }
        catch (IOException e) {
            throw new PatientReportDownloadException(e);
        }
    }

    public List<PatientReportHeader> downloadPatientReportHeadersByPatientUuid(List<String> patientUuids)
            throws PatientReportDownloadException {
        try {
            String paramSignature = StringUtils.getCommaSeparatedStringFromList(patientUuids);
            Date lastSyncTime = lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_PATIENT_REPORTS, paramSignature);
            List<PatientReportHeader> patientReportHeaders = new ArrayList<>();
            if(hasThisCallHappenedBefore(lastSyncTime)) {
                patientReportHeaders.addAll(patientReportService.downloadPatientReportHeadersByPatientUuid(patientUuids, lastSyncTime));
            }else{
                LastSyncTime fullLastSyncTimeInfo = lastSyncTimeService.getFullLastSyncTimeInfoFor(DOWNLOAD_PATIENT_REPORTS);
                if (isFirstCallToDownloadReportsEver(fullLastSyncTimeInfo)) {
                    patientReportHeaders.addAll(patientReportService.downloadPatientReportHeadersByPatientUuid(patientUuids, null));
                } else {
                    String parameterSplit = fullLastSyncTimeInfo.getParamSignature();
                    List<String> knownPatientsUuid = asList(parameterSplit.split(UUID_SEPARATOR));
                    List<String> newPatientsUuids = getNewUuids(patientUuids, knownPatientsUuid);
                    List<String> allPatientsUuids = getAllUuids(knownPatientsUuid, newPatientsUuids);
                    paramSignature =  StringUtils.getCommaSeparatedStringFromList(allPatientsUuids);
                    if(newPatientsUuids.size() != 0) {
                        patientReportHeaders.addAll(patientReportService.downloadPatientReportHeadersByPatientUuid(newPatientsUuids, null));
                        patientReportHeaders.addAll(patientReportService.downloadPatientReportHeadersByPatientUuid(knownPatientsUuid,fullLastSyncTimeInfo.getLastSyncDate()));
                    }else{
                        patientReportHeaders.addAll(patientReportService.downloadPatientReportHeadersByPatientUuid(patientUuids,fullLastSyncTimeInfo.getLastSyncDate()));
                    }
                }
            }
            LastSyncTime newLastSyncTime = new LastSyncTime(DOWNLOAD_PATIENT_REPORTS, sntpService.getTimePerDeviceTimeZone(), paramSignature);
            lastSyncTimeService.saveLastSyncTime(newLastSyncTime);
            LastSyncTime fullLastSyncTimeInfos = lastSyncTimeService.getFullLastSyncTimeInfoFor(DOWNLOAD_PATIENT_REPORTS);
            String parameterSplits = fullLastSyncTimeInfos.getParamSignature();
            return patientReportHeaders;
        }
        catch (IOException e) {
            Log.e(TAG,"Encountered an IOException while downloading report headers",e);
            throw new PatientReportDownloadException(e);
        }
    }

    public PatientReport downloadPatientReportByUuid(String uuid)
            throws PatientReportDownloadException {
        try {
            return patientReportService.downloadPatientReportByUuid(uuid);
        }
        catch (IOException e) {
            throw new PatientReportDownloadException(e);
        }
    }

    public List<PatientReport> downloadPatientReportByUuid(List<PatientReportHeader> patientReportHeaders)
            throws PatientReportDownloadException {
        try {
            Date lastSyncDate = lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_PATIENT_REPORTS);
            return patientReportService.downloadPatientReportByUuid(patientReportHeaders,lastSyncDate);
        }
        catch (IOException e) {
            Log.e(TAG,"Encountered an IOException while downloading reports",e);
            throw new PatientReportDownloadException(e);
        }
    }

    public void savePatientReportHeaders(List<PatientReportHeader> patientReportHeaders) throws PatientReportSaveException {
        try {
            for (PatientReportHeader patientReportHeader : patientReportHeaders) {
                if (isDownloadedHeader(patientReportHeader)) {
                    patientReportService.updatePatientReportHeader(patientReportHeader);
                } else {
                    patientReportService.savePatientReportHeader(patientReportHeader);
                }
            }
        }
        catch (IOException e) {
            throw new PatientReportSaveException(e);
        }
    }
    
    public void saveOrUpdatePatientReports(List<PatientReport> patientReports) throws PatientReportSaveException {
        try {
            for (PatientReport patientReport : patientReports) {
                if (isDownloaded(patientReport)) {
                    patientReportService.updatePatientReport(patientReport);
                } else {
                    patientReportService.savePatientReport(patientReport);
                }
            }
        }
        catch (IOException e) {
            throw new PatientReportSaveException(e);
        }
    }
    
    public void deletePatientReport(PatientReport patientReport) throws PatientReportDeleteException {
        try {
            patientReportService.deletePatientReport(patientReport);
        }
        catch (IOException e) {
            throw new PatientReportDeleteException(e);
        }
    }

    public boolean isDownloadedHeader(PatientReportHeader patientReportHeader) {
        try {
            return patientReportService.getPatientReportHeaderByUuid(patientReportHeader.getUuid()) != null;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean isDownloaded(PatientReport patientReport) {
        try {
            return patientReportService.getPatientReportByUuid(patientReport.getUuid()) != null;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean hasThisCallHappenedBefore(Date lastSyncTime) {
        return lastSyncTime != null;
    }

    private boolean isFirstCallToDownloadReportsEver(LastSyncTime fullLastSyncTimeInfo) {
        return fullLastSyncTimeInfo == null;
    }

    private List<String> getNewUuids(List<String> patientUuids, List<String> knownPatientsUuid) {
        List<String> newPatientsUuids = new ArrayList<>(patientUuids);
        newPatientsUuids.removeAll(knownPatientsUuid);
        return newPatientsUuids;
    }

    private ArrayList<String> getAllUuids(List<String> knownUuids, List<String> newUuids) {
        HashSet<String> allUuids = new HashSet<>(knownUuids);
        allUuids.addAll(newUuids);
        ArrayList<String> sortedUuids = new ArrayList<>(allUuids);
        Collections.sort(sortedUuids);
        return sortedUuids;
    }

    public static class PatientReportFetchException extends Throwable {
        public PatientReportFetchException(Throwable throwable) {
            super(throwable);
        }
    }
    
    public static class PatientReportException extends Throwable {
        public PatientReportException(Throwable throwable) {
            super(throwable);
        }
    }
    
    public static class PatientReportDownloadException extends Throwable {
        public PatientReportDownloadException(Throwable throwable) {
            super(throwable);
        }
    }
    
    public static class PatientReportSaveException extends Throwable {
        public PatientReportSaveException(Throwable throwable) {
            super(throwable);
        }
    }
    
    public static class PatientReportDeleteException extends Throwable {
        public PatientReportDeleteException(Throwable throwable) {
            super(throwable);
        }
    }
}
