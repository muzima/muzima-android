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
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.api.service.MuzimaGeneratedReportService;
import com.muzima.service.SntpService;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public class MuzimaGeneratedReportController {
    
    private static final String TAG = "MuzimaGeneratedReportController";
    
    private MuzimaGeneratedReportService muzimaGeneratedReportService;
    
    private LastSyncTimeService lastSyncTimeService;
    
    private SntpService sntpService;
    
    Logger logger;
    
    public MuzimaGeneratedReportController(MuzimaGeneratedReportService muzimaGeneratedReportService) {
        this.muzimaGeneratedReportService = muzimaGeneratedReportService;
    }
    
    public List<MuzimaGeneratedReport> getAllMuzimaGeneratedReportsByPatientUuid(String uuid)
            throws MuzimaGeneratedReportException {
        try {
            return muzimaGeneratedReportService.getMuzimaGeneratedReportByPatientUuid(uuid);
        }
        catch (IOException e) {
            throw new MuzimaGeneratedReportException(e);
        }
    }
    
    public MuzimaGeneratedReport getLastPriorityMuzimaGeneratedReport(String uuid)
            throws MuzimaGeneratedReportFetchException {
        try {
            return muzimaGeneratedReportService.getMuzimaGeneratedReportByUuid(uuid);
        }
        catch (IOException e) {
            throw new MuzimaGeneratedReportFetchException(e);
        }
    }
    
    public List<MuzimaGeneratedReport> downloadLastPriorityMuzimaGeneratedReportByPatientUuid(String patientUuid)
            throws MuzimaGeneratedReportDownloadException {
        try {
            logger = Logger.getLogger(this.getClass().getName());
            logger.warning("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
            List<MuzimaGeneratedReport> muzimaGeneratedReports = muzimaGeneratedReportService
                    .downloadMuzimaGeneratedReportByPatientUuid(patientUuid);
            logger.warning("aaaaaaaaaaaaaaaaaaaaaaaaasssssssssssssssssssssss"+muzimaGeneratedReports.size()+" "+patientUuid+" ffffffffff  "+muzimaGeneratedReports.get(0).getPatientUuid()+"ppppp"+muzimaGeneratedReports.get(0).getUuid());
            return muzimaGeneratedReports;
        }
        catch (IOException e) {
            throw new MuzimaGeneratedReportDownloadException(e);
        }
    }
    
    public void saveAllMuzimaGeneratedReports(List<MuzimaGeneratedReport> muzimaGeneratedReports)
            throws MuzimaGeneratedReportSaveException {
        logger.warning("kkkkkkkksssssssssssssssssssssss in the method"+muzimaGeneratedReports.size());
        try {
            for (MuzimaGeneratedReport muzimaGeneratedReport : muzimaGeneratedReports) {
                muzimaGeneratedReportService.saveMuzimaGeneratedReport(muzimaGeneratedReport);
                logger.warning("llllllllllllllllllsssssssssssssssssssssss"+muzimaGeneratedReport.getUuid()+"llll"+muzimaGeneratedReport.getPatientUuid());
            }
    
        }
        catch (IOException e) {
            throw new MuzimaGeneratedReportSaveException(e);
        }
    }
    
    public void deleteMuzimaGeneratedReport(MuzimaGeneratedReport muzimaGeneratedReport)
            throws MuzimaGeneratedReportDeleteException {
        try {
            
            muzimaGeneratedReportService.deleteMuzimaGeneratedReport(muzimaGeneratedReport);
        }
        catch (IOException e) {
            throw new MuzimaGeneratedReportDeleteException(e);
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
    
    public static class MuzimaGeneratedReportSaveException extends Throwable {
        
        public MuzimaGeneratedReportSaveException(Throwable throwable) {
            super(throwable);
        }
    }
    
    public static class MuzimaGeneratedReportDeleteException extends Throwable {
        
        public MuzimaGeneratedReportDeleteException(Throwable throwable) {
            super(throwable);
        }
    }
}
