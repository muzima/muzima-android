
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
import com.muzima.adapters.reports.LeaderboardAdapter;
import com.muzima.adapters.reports.PerformanceComparisonAdapter;
import com.muzima.adapters.reports.SummaryStatisticAdapter;
import com.muzima.api.model.FormTemplate;
import com.muzima.api.model.Provider;
import com.muzima.controller.FormController;
import com.muzima.model.AvailableForm;
import com.muzima.model.ProviderReportStatistic;
import com.muzima.util.JsonUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.progressdialog.MuzimaProgressDialog;


import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;


public class ProviderPerformanceReportViewActivity extends ProviderReportViewActivity {
    public static final String REPORT = "SelectedReport";
    public Provider provider;
    private MuzimaProgressDialog progressDialog;
    private FormTemplate reportTemplate;
    private final ThemeUtils themeUtils = new ThemeUtils();
    private List<ProviderReportStatistic> allProviderReportStatistics = new ArrayList<>();
    private String leaderboardStatisticKey;

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

        extractProviderReportStatistics(); // Consider doing this after syncing from server and storing in db

        List<ProviderReportStatistic> providerReportStatistics = getIndividualProviderDataset("adminx");
        if(providerReportStatistics.isEmpty()){
            createLeaderboardView();
        } else {
            createIndividualPerformanceView(providerReportStatistics);
        }

        logEvent("VIEW_PROVIDER_PERFORMANCE_REPORT","{\"reporttemplateuuid\":\""+reportTemplate.getUuid()+"\"}");
    }

    private void extractProviderReportStatistics(){
        String dataset = "{\"dataset\": [{\"providerSystemId\": \"admin\",\"providerName\": \"Super User\",\"patientsAllocated\": 40,\"patientsVisited\": 20,\"patientsReturned\": 5},\n" +
                "{\"providerSystemId\": \"3-4\",\"providerName\": \"James Mwai\",\"patientsAllocated\": 30,\"patientsVisited\": 7,\"patientsReturned\": 1}]}";
        JSONArray datasetJsonArray = parseDataset(dataset);

        String reportDefinition = reportTemplate.getHtml();
        JSONArray reportTemplateDefinitions = (JSONArray) JsonUtils.readAsObject(reportDefinition,"reportTemplate");
        int templatesCount = reportTemplateDefinitions.size();
        for (int i=0; i<templatesCount; i++){
            try {
                final JSONObject template = (JSONObject)reportTemplateDefinitions.get(i);
                String achievementKey = (String)template.get("achievementKey");
                String expectedAchievementKey = (String) template.get("expectedAchievementKey");

                int datasetSize = datasetJsonArray.size();
                for (int j=0; j<datasetSize; j++) {
                    JSONObject providerDataset = (JSONObject)datasetJsonArray.get(j);
                    allProviderReportStatistics.add(new ProviderReportStatistic() {{
                        if (providerDataset != null && providerDataset.containsKey(achievementKey)) {
                            setAchievement((Integer) providerDataset.get(achievementKey));
                            setAchievementId(achievementKey);

                        }
                        if (providerDataset != null && providerDataset.containsKey(expectedAchievementKey)) {
                            setExpectedAchievement((Integer) providerDataset.get(expectedAchievementKey));
                        }

                        setAchievementGroupAverage(getAchievementAverage(datasetJsonArray, achievementKey, expectedAchievementKey));
                        setStatisticTitle(template.get("title").toString());
                        setStatisticHint(template.get("hint").toString());
                        setProviderName(providerDataset.get("providerName").toString());
                        setProviderId(providerDataset.get("providerSystemId").toString());
                    }});
                }

                if(template.containsKey("leaderboardStatisticKey")){
                    leaderboardStatisticKey = template.get("leaderboardStatisticKey").toString();
                }
            } catch (Exception e) {
                Log.e(getClass().getSimpleName(), "Could not parse details of summary statistic",e);
            }
        }
    }

    private List<ProviderReportStatistic> getIndividualProviderDataset(String providerId){
         return allProviderReportStatistics.stream()
                 .filter(statistic -> statistic.getProviderId().equals(providerId))
                 .collect(Collectors.toList());
    }

    private void createIndividualPerformanceView(List<ProviderReportStatistic> providerStatistics){
        SummaryStatisticAdapter summaryStatisticAdapter = new SummaryStatisticAdapter(providerStatistics, getApplicationContext());
        PerformanceComparisonAdapter performanceComparisonAdapter = new PerformanceComparisonAdapter(providerStatistics, getApplicationContext());

        List<ProviderReportStatistic> leaderboardStatistics = allProviderReportStatistics.stream()
                .filter(statistic -> statistic.getAchievementId().equals(leaderboardStatisticKey)).collect(Collectors.toList());
        LeaderboardAdapter leaderboardAdapter = new LeaderboardAdapter(leaderboardStatistics, getApplicationContext());

        Fragment fragment = PerformanceReportFragment.newInstance(summaryStatisticAdapter, performanceComparisonAdapter, leaderboardAdapter);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.report_framelayout,fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void createLeaderboardView(){
        List<ProviderReportStatistic> leaderboardStatistics = allProviderReportStatistics.stream()
                .filter(statistic -> statistic.getAchievementId().equals(leaderboardStatisticKey)).collect(Collectors.toList());
        LeaderboardAdapter leaderboardAdapter = new LeaderboardAdapter(leaderboardStatistics, getApplicationContext());

        Fragment fragment = LeaderboardFragment.newInstance(leaderboardAdapter);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.report_framelayout,fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private JSONArray parseDataset(String dataset){
        return (JSONArray)JsonUtils.readAsObject(dataset,"dataset");
    }

    private float getAchievementAverage(JSONArray dataset, String achievementKey, String expectedAchievementKey){
        int objLength =dataset.size();
        int total = 0;
        int expectedTotal = 0;
        for (int i=0; i<objLength; i++){
            JSONObject jsonObject = (JSONObject)dataset.get(i);
            total += (Integer)jsonObject.get(achievementKey);
            expectedTotal += (Integer)jsonObject.get(expectedAchievementKey);
        }
        return total*100/expectedTotal;
    }

    @Override
    protected void onDestroy() {
        if (progressDialog != null) {
            progressDialog.dismiss( );
        }
        super.onDestroy( );
    }
}



