package com.muzima.controller;

import static com.muzima.api.model.APIName.DOWNLOAD_REPORT_DATASETS;

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

    public ReportDatasetController(ReportDatasetService reportDatasetService,  LastSyncTimeService lastSyncTimeService,
                                   SntpService sntpService){
        this.reportDatasetService = reportDatasetService;
        this.lastSyncTimeService = lastSyncTimeService;
        this.sntpService = sntpService;
    }

    public List<ReportDataset> downloadReportDatasets(List<Integer> datasetDefinitionIds) throws ReportDatasetDownloadException{
        try {
            LastSyncTime lastSyncTime = lastSyncTimeService.getFullLastSyncTimeInfoFor(DOWNLOAD_REPORT_DATASETS);
            Date lastSyncDate = null;
            if(lastSyncTime != null){
                lastSyncDate = lastSyncTime.getLastSyncDate();
            }
            List<ReportDataset> reportDatasets = new ArrayList<>();
            for(Integer datasetDefinitionId : datasetDefinitionIds){
                ReportDataset reportDataset = reportDatasetService.downloadReportDatasetByDefinitionIdAndLastSyncDate(lastSyncDate, datasetDefinitionId);
                reportDatasets.add(reportDataset);
            }
            LastSyncTime newLastSyncTime = new LastSyncTime(DOWNLOAD_REPORT_DATASETS, sntpService.getTimePerDeviceTimeZone());
            lastSyncTimeService.saveLastSyncTime(newLastSyncTime);
            return reportDatasets;
        } catch (IOException e) {
            throw new ReportDatasetController.ReportDatasetDownloadException(e);
        }
    }

    public void saveReportDatasets(List<ReportDataset> reportDatasets) throws  ReportDatasetSaveException{
        try {
              reportDatasetService.saveReportDatasets(reportDatasets);
        } catch (IOException e) {
            throw new ReportDatasetController.ReportDatasetSaveException(e);
        }
    }

   public void updateReportDatasets(List<ReportDataset> reportDatasets) throws  ReportDatasetSaveException{
        try {
            reportDatasetService.updateReportDatasets(reportDatasets);
        } catch (IOException e) {
            throw new ReportDatasetController.ReportDatasetSaveException(e);
        }
    }

    public  ReportDataset getReportDatasetByDatasetDefinitionId(int datasetDefinitionId) throws ReportDatasetFetchException{
        try {
            return reportDatasetService.getReportDatasetByDatasetDefinitionId(datasetDefinitionId);
        } catch (IOException | ParseException e) {
            throw new ReportDatasetController.ReportDatasetFetchException(e);
        }
    }

    public  List<ReportDataset> getReportDatasets() throws ReportDatasetFetchException{
        try {
            return reportDatasetService.getReportDatasets();
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
