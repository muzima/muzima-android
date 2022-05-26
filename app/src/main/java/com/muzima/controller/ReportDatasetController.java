package com.muzima.controller;

import com.muzima.api.model.ReportDataset;
import com.muzima.api.service.ReportDatasetService;

import org.apache.lucene.queryParser.ParseException;

import java.io.IOException;
import java.util.List;

public class ReportDatasetController {
    private ReportDatasetService reportDatasetService;

    public ReportDatasetController(ReportDatasetService reportDatasetService){
        this.reportDatasetService = reportDatasetService;
    }

    public List<ReportDataset> downloadReportDatasets() throws ReportDatasetDownloadException{
        try {
            return reportDatasetService.downloadReportDatasets();
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
