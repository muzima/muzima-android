
/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.reports;

import android.os.Bundle;
import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.reports.PerformanceComparisonAdapter;
import com.muzima.adapters.reports.SummaryStatisticAdapter;
import com.muzima.api.model.FormTemplate;
import com.muzima.api.model.Provider;
import com.muzima.controller.FormController;
import com.muzima.model.AvailableForm;
import com.muzima.model.ProviderAchievementStatistic;
import com.muzima.util.JsonUtils;
import com.muzima.utils.StringUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.progressdialog.MuzimaProgressDialog;


import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;


public class ProviderPerformanceReportViewActivity extends ProviderReportViewActivity {
    public static final String REPORT = "SelectedReport";
    public Provider provider;
    private MuzimaProgressDialog progressDialog;
    private FormTemplate reportTemplate;
    private final ThemeUtils themeUtils = new ThemeUtils();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        themeUtils.onCreate(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_performance_report);
        progressDialog = new MuzimaProgressDialog(this);
        FormController formController = ((MuzimaApplication) getApplicationContext()).getFormController();
        try {
            AvailableForm availableForm = (AvailableForm)getIntent().getSerializableExtra(REPORT);
            reportTemplate = formController.getFormTemplateByUuid(availableForm.getFormUuid());
        } catch (FormController.FormFetchException e) {
            Log.e(getClass().getSimpleName(),"Could not obtain report template");
        }

        createPerformanceView();

        logEvent("VIEW_PROVIDER_PERFORMANCE_REPORT","{\"reporttemplateuuid\":\""+reportTemplate.getUuid()+"\"}");
    }

    private void createPerformanceView(){

        //get dataset
        String dataset = "{\"dataset\": [{\"providerSystemId\": \"admin\",\"providerName\": \"Super User\",\"patientsAllocated\": 40,\"patientsVisited\": 20,\"patientsReturned\": 5},\n" +
                "{\"providerSystemId\": \"3-4\",\"providerName\": \"James Mwai\",\"patientsAllocated\": 30,\"patiensVisited\": 180,\"patientsReturned\": 8}]}";
        JSONArray datasetJsonArray = parseDataset(dataset);
        JSONObject providerDataset = getDatasetForProvider(datasetJsonArray,"admin");
        //Set up summary statistics 1
        String reportDefinition = reportTemplate.getHtml();
        JSONArray summaryStatistic1 = (JSONArray) JsonUtils.readAsObject(reportDefinition,"summaryStatistic1");
        int arrLength = summaryStatistic1.size();
        List<ProviderAchievementStatistic> achievementStatistics = new ArrayList<>();

        for (int i=0; i<arrLength; i++){
            try {
                final JSONObject statistic = (JSONObject)summaryStatistic1.get(i);
                String achievementKey = (String)statistic.get("achievementKey");
                String expectedAchievementKey = (String) statistic.get("expectedAchievementKey");
                achievementStatistics.add(new ProviderAchievementStatistic(){{
                    if(providerDataset != null && providerDataset.containsKey(achievementKey)) {
                        setAchievement((Integer) providerDataset.get(achievementKey));
                    }
                    if(providerDataset != null && providerDataset.containsKey(expectedAchievementKey)) {
                        setExpectedAchievement((Integer) providerDataset.get(expectedAchievementKey));
                    }
                    setStatisticTitle(statistic.get("title").toString());
                    setStatisticHint(statistic.get("hint").toString());
                }});


            } catch (Exception e) {
                Log.e(getClass().getSimpleName(), "Could not parse details of summary statistic",e);
            }
        }
        SummaryStatisticAdapter summaryStatisticAdapter = new SummaryStatisticAdapter(achievementStatistics, getApplicationContext());
        PerformanceComparisonAdapter performanceComparisonAdapter = new PerformanceComparisonAdapter(achievementStatistics, getApplicationContext());



        Fragment fragment = PerformanceReportFragment.newInstance(summaryStatisticAdapter, performanceComparisonAdapter);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.report_framelayout,fragment);
        transaction.addToBackStack(null);
        transaction.commit();

    }

    private JSONObject getDatasetForProvider(JSONArray dataset, String providerSystemId){
        int objLength =dataset.size();

        for (int i=0; i<objLength; i++){
            JSONObject jsonObject = (JSONObject)dataset.get(i);
            if(jsonObject.containsKey("providerSystemId") && StringUtils.equals(jsonObject.get("providerSystemId").toString(),providerSystemId)){
                return jsonObject;
            }
        }
        return null;
    }

    private JSONArray parseDataset(String dataset){
         return (JSONArray)JsonUtils.readAsObject(dataset,"dataset");
    }

    @Override
    protected void onDestroy() {
        if (progressDialog != null) {
            progressDialog.dismiss( );
        }
        super.onDestroy( );
    }
}



