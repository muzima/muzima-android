/*
 * Copyright (c) Vanderbilt University Medical Center and Lambda Informatics.
 * All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.utils.javascriptinterface;

import android.app.Activity;
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

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

import java.util.ArrayList;
import java.util.List;

public class ReportDatasetJavascriptInterface {
    private final MuzimaApplication muzimaApplication;
    private final ReportDatasetController reportDatasetController;
    private Activity activity;
    public ReportDatasetJavascriptInterface(Activity activity){
        this.activity = activity;
        this.muzimaApplication = (MuzimaApplication) activity.getApplicationContext();
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
            Toast.makeText(activity, activity.getString(R.string.error_report_dataset_load), Toast.LENGTH_SHORT).show();
            Log.e(getClass().getSimpleName(), "Exception occurred while loading report dataset", e);
        }
        return JSONValue.toJSONString(reportDataset);
    }

    @JavascriptInterface
    public String getReportDatasetByDatasetDefinitionIdAndKeyValue(int datasetDefinitionId, String key, String value) {
        ReportDataset reportDataset = new ReportDataset();
        try {
            reportDataset = reportDatasetController.getReportDatasetByDatasetDefinitionId(datasetDefinitionId);
            if(reportDataset != null) {
                JSONArray datasetJsonArray = parseDataset(reportDataset.getDataSet());
                JSONArray filteredArray = new JSONArray();
                int datasetSize = datasetJsonArray.size();
                for (int j = 0; j < datasetSize; j++) {
                    JSONObject providerDataset = (JSONObject) datasetJsonArray.get(j);
                    if (providerDataset != null && providerDataset.containsKey(key) && providerDataset.get(key) != null && providerDataset.get(key).equals(value)) {
                        filteredArray.add(providerDataset);
                    }
                }
                return filteredArray.toJSONString();
            }
        } catch (ReportDatasetController.ReportDatasetFetchException e) {
            Toast.makeText(activity, activity.getString(R.string.error_report_dataset_load), Toast.LENGTH_SHORT).show();
            Log.e(getClass().getSimpleName(), "Exception occurred while loading report dataset", e);
        }
        return "[]";
    }
    private JSONArray parseDataset(String dataset){
        JSONArray jsonArray = new JSONArray();
        JSONParser jp = new JSONParser(JSONParser.MODE_PERMISSIVE);
        try {
            jsonArray = (JSONArray) jp.parse(dataset);
        } catch (ParseException e) {
            Log.e(getClass().getSimpleName(),"Encountered an exception",e);
        }
        return jsonArray;
    }
}