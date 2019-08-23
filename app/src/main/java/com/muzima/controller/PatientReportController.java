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

import com.muzima.api.model.PatientReport;
import com.muzima.api.model.PatientReportHeader;
import com.muzima.api.service.PatientReportService;

import java.io.IOException;
import java.util.List;

public class PatientReportController {
    
    private static final String TAG = "PatientReportController";
    
    private PatientReportService patientReportService;
    
    public PatientReportController(PatientReportService patientReportService) {
        this.patientReportService = patientReportService;
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

    public PatientReport downloadPatientReportByUuid(String uuid)
            throws PatientReportDownloadException {
        try {
            return patientReportService.downloadPatientReportByUuid(uuid);
        }
        catch (IOException e) {
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
