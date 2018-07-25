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

import com.muzima.api.model.MuzimaGeneratedReport;
import com.muzima.api.model.MuzimaSetting;
import com.muzima.api.service.MuzimaGeneratedReportService;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.service.SntpService;

import java.io.IOException;
import java.util.List;



public class MuzimaGeneratedReportController {
    private static final String TAG = "MuzimaGeneratedReportController";
    private MuzimaGeneratedReportService muzimaGeneratedReportService;
    private LastSyncTimeService lastSyncTimeService;
    private SntpService sntpService;

    public MuzimaGeneratedReportController(MuzimaGeneratedReportService muzimaGeneratedReportService, LastSyncTimeService lastSyncTimeService, SntpService sntpService) {
        this.muzimaGeneratedReportService = muzimaGeneratedReportService;
        this.lastSyncTimeService = lastSyncTimeService;
        this.sntpService = sntpService;
    }

    public List<MuzimaGeneratedReport> getAllMuzimaGeneratedReportService(String uuid) throws MuzimaGeneratedReportException {
        try {
            return muzimaGeneratedReportService.getMuzimaGeneratedReportByPatientUuid(uuid);
        } catch (IOException e) {
            throw new MuzimaGeneratedReportException(e);
        }
    }

    public MuzimaGeneratedReport getLastPriorityMuzimaGeneratedReport (String uuid) throws MuzimaGeneratedReportFetchException {
        try {
            return muzimaGeneratedReportService.getMuzimaGeneratedReportByUuid(uuid);
        } catch (IOException e) {
            throw new MuzimaGeneratedReportFetchException(e);
        }
    }
    
    public List<MuzimaGeneratedReport> downloadLastPriorityMuzimaGeneratedReportByPatientUuid(String patientUuid) throws MuzimaGeneratedReportDownloadException {
        try {
            return muzimaGeneratedReportService.downloadMuzimaGeneratedReportByPatientUuid(patientUuid);
        } catch (IOException e) {
            throw new MuzimaGeneratedReportDownloadException(e);
        }
    }

    public static class MuzimaGeneratedReportFetchException extends Throwable {
        public MuzimaGeneratedReportFetchException(Throwable throwable) {
            super(throwable);
        }
    }
    
    public static class MuzimaGeneratedReportException extends Throwable {
        public MuzimaGeneratedReportException(Throwable throwable) {
            super(throwable);
        }
    }
    public static class MuzimaGeneratedReportDownloadException extends Throwable {
        public MuzimaGeneratedReportDownloadException(Throwable throwable) {
            super(throwable);
        }
    }
}
