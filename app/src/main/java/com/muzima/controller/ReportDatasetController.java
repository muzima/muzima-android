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

import static com.muzima.api.model.APIName.DOWNLOAD_REPORT_DATASETS;

import android.util.Log;

import com.muzima.api.model.LastSyncTime;
import com.muzima.api.model.ReportDataset;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.api.service.ReportDatasetService;
import com.muzima.service.SntpService;

import org.apache.lucene.queryParser.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ReportDatasetController {
    private ReportDatasetService reportDatasetService;
    private final LastSyncTimeService lastSyncTimeService;
    private final SntpService sntpService;

    public ReportDatasetController(ReportDatasetService reportDatasetService, LastSyncTimeService lastSyncTimeService,
                                   SntpService sntpService){
        this.reportDatasetService = reportDatasetService;
        this.lastSyncTimeService = lastSyncTimeService;
        this.sntpService = sntpService;
    }

    public List<ReportDataset> downloadReportDatasets(List<Integer> datasetDefinitionIds, boolean isDeltaSync) throws ReportDatasetDownloadException{
        try {
            LastSyncTime lastSyncTime = lastSyncTimeService.getFullLastSyncTimeInfoFor(DOWNLOAD_REPORT_DATASETS);
            Date lastSyncDate = null;
            if(isDeltaSync) {
                if (lastSyncTime != null) {
                    lastSyncDate = lastSyncTime.getLastSyncDate();
                }
            }
            List<ReportDataset> reportDatasets = new ArrayList<>();
            for(Integer datasetDefinitionId : datasetDefinitionIds){
                ReportDataset reportDataset = reportDatasetService.downloadReportDatasetByDefinitionIdAndLastSyncDate(lastSyncDate, datasetDefinitionId);
                if(reportDataset != null) {
                    reportDatasets.add(reportDataset);
                }
            }
            LastSyncTime newLastSyncTime = new LastSyncTime(DOWNLOAD_REPORT_DATASETS, sntpService.getTimePerDeviceTimeZone());
            lastSyncTimeService.saveLastSyncTime(newLastSyncTime);
            return reportDatasets;
        } catch (IOException e) {
            throw new ReportDatasetDownloadException(e);
        }
    }

    public void saveReportDatasets(List<ReportDataset> reportDatasets) throws  ReportDatasetSaveException{
        try {
            if(reportDatasets.size()>0) {
                reportDatasetService.saveReportDatasets(reportDatasets);
            }
        } catch (IOException e) {
            throw new ReportDatasetSaveException(e);
        }
    }

   public void updateReportDatasets(List<ReportDataset> reportDatasets) throws  ReportDatasetSaveException{
        try {
            reportDatasetService.updateReportDatasets(reportDatasets);
        } catch (IOException e) {
            throw new ReportDatasetSaveException(e);
        }
    }

    public  ReportDataset getReportDatasetByDatasetDefinitionId(int datasetDefinitionId) throws ReportDatasetFetchException{
        try {
            return reportDatasetService.getReportDatasetByDatasetDefinitionId(datasetDefinitionId);
        } catch (IOException | ParseException e) {
            throw new ReportDatasetFetchException(e);
        }
    }

    public  List<ReportDataset> getReportDatasets() throws ReportDatasetFetchException{
        try {
            return reportDatasetService.getReportDatasets();
        } catch (IOException | ParseException e) {
            throw new ReportDatasetFetchException(e);
        }
    }

    public  void deleteReportDatasets(List<Integer> datasetToDeleteIds) throws ReportDatasetFetchException{
        try {
            reportDatasetService.deleteReportDatasets(datasetToDeleteIds);
        } catch (IOException | ParseException e) {
            throw new ReportDatasetController.ReportDatasetFetchException(e);
        }
    }

    public static class ReportDatasetFetchException extends Throwable {
        ReportDatasetFetchException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class ReportDatasetSaveException extends Throwable {
        ReportDatasetSaveException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class ReportDatasetDownloadException extends Throwable {
        ReportDatasetDownloadException(Throwable throwable) {
            super(throwable);
        }
    }

}
