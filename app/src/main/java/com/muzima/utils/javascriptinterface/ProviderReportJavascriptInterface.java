package com.muzima.utils.javascriptinterface;

import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.FormData;
import com.muzima.api.model.ReportDataset;
import com.muzima.controller.FormController;
import com.muzima.controller.ReportDatasetController;
import com.muzima.utils.Constants;
import com.muzima.view.reports.ProviderReportViewActivity;

import net.minidev.json.JSONValue;

import java.util.ArrayList;
import java.util.List;

public class ProviderReportJavascriptInterface {
    private final MuzimaApplication muzimaApplication;
    private final ReportDatasetController reportDatasetController;
    private ProviderReportViewActivity providerReportViewActivity;
    public ProviderReportJavascriptInterface(ProviderReportViewActivity providerReportViewActivity){
        this.muzimaApplication = (MuzimaApplication) providerReportViewActivity.getApplicationContext();
        reportDatasetController = muzimaApplication.getReportDatasetController();
    }

    @JavascriptInterface
    public String getCompleteFormData(){
        List<FormData> completeFormData = new ArrayList<>();
        try {
            completeFormData = muzimaApplication.getFormController().getAllFormData(Constants.STATUS_COMPLETE);
        } catch (FormController.FormDataFetchException e) {
            Log.e(getClass().getSimpleName(),"Error while fetching form data",e);
        }
        return JSONValue.toJSONString(completeFormData);
    }

    @JavascriptInterface
    public String getReportDatasetByDatasetDefinitionId(int datasetDefinitionId) {
        ReportDataset reportDataset = new ReportDataset();
        try {
            reportDataset = reportDatasetController.getReportDatasetByDatasetDefinitionId(datasetDefinitionId);
        } catch (ReportDatasetController.ReportDatasetFetchException e) {
            Toast.makeText(providerReportViewActivity, providerReportViewActivity.getString(R.string.error_report_dataset_load), Toast.LENGTH_SHORT).show();
            Log.e(getClass().getSimpleName(), "Exception occurred while loading report dataset", e);
        }
        return JSONValue.toJSONString(reportDataset);
    }

}
