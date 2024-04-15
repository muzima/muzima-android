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

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.reports.LeaderboardAdapter;
import com.muzima.adapters.reports.PerformanceComparisonAdapter;
import com.muzima.adapters.reports.SummaryStatisticAdapter;
import com.muzima.api.model.FormTemplate;
import com.muzima.api.model.Provider;
import com.muzima.api.model.ReportDataset;
import com.muzima.controller.FormController;
import com.muzima.controller.ReportDatasetController;
import com.muzima.model.AvailableForm;
import com.muzima.model.ProviderReportStatistic;
import com.muzima.util.JsonUtils;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.StringUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.progressdialog.MuzimaProgressDialog;


import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;


public class ProviderPerformanceReportViewActivity extends ProviderReportViewActivity implements LeaderboardAdapter.LeaderboardItemClickListener {
    public static final String REPORT = "SelectedReport";
    public Provider provider;
    private MuzimaProgressDialog progressDialog;
    private FormTemplate reportTemplate;
    private List<ProviderReportStatistic> allProviderReportStatistics = new ArrayList<>();
    private List<ProviderReportStatistic> individualProviderStatistics = new ArrayList<>();
    private String leaderboardStatisticKey;
    private LeaderboardAdapter leaderboardAdapter;
    private SummaryStatisticAdapter summaryStatisticAdapter;
    private PerformanceComparisonAdapter performanceComparisonAdapter;

    private Fragment individualPerformanceSummaryFragment;
    private Fragment leaderboardFragment;
    private Fragment currentFragment;
    private final LanguageUtil languageUtil = new LanguageUtil();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        ThemeUtils.getInstance().onCreate(this,true);
        languageUtil.onCreate(this);
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
        initializeIndividualPerformanceView();
        initializeLeaderboardView();

        String userSystemId = ((MuzimaApplication)getApplicationContext()).getAuthenticatedUser().getSystemId();
        loadDefaultReportView(userSystemId);

        logEvent("VIEW_PROVIDER_PERFORMANCE_REPORT","{\"reporttemplateuuid\":\""+reportTemplate.getUuid()+"\"}");
    }

    @Override
    public void onLeaderboardItemClick(View view, int position) {
        if(currentFragment == leaderboardFragment) {
            ProviderReportStatistic reportStatistic = getLeaderboardAdapter().getReportStatistic(position);
            loadDefaultReportView(reportStatistic.getProviderId());
        }
    }

    private void extractProviderReportStatistics(){
        Map<String, JSONArray> datasetMap = new HashMap<>();
        String reportDefinition = reportTemplate.getHtml();
        JSONArray reportTemplateDefinitions = (JSONArray) JsonUtils.readAsObject(reportDefinition,"reportTemplate");
        int templatesCount = reportTemplateDefinitions.size();
        for (int i=0; i<templatesCount; i++){
            try {
                final JSONObject template = (JSONObject)reportTemplateDefinitions.get(i);
                String achievementKey = (String)template.get("achievementKey");
                String expectedAchievementKey = (String) template.get("expectedAchievementKey");
                JSONArray datasetJsonArray = null;
                String datasetId = template.get("datasetId").toString();
                if (datasetMap.containsKey(datasetId)){
                    datasetJsonArray = datasetMap.get(datasetId);
                } else {
                    ReportDataset reportDataSet =  ((MuzimaApplication)getApplicationContext()).getReportDatasetController()
                            .getReportDatasetByDatasetDefinitionId(Integer.valueOf(datasetId));
                    if(reportDataSet != null){
                        datasetJsonArray = parseDataset(reportDataSet.getDataSet());
                        datasetMap.put(datasetId,datasetJsonArray);
                    } else {
                        // ToDo: delete this dummy dataset stub
                        String dataset = "[{\"startDate\": \"1st July\",\"endDate\": \"31st July 2022\",\"providerSystemId\": \"admin\",\"providerName\": \"Super User\",\"patientsAllocated\": 40,\"patientsVisited\": 20,\"patientsReturned\": 5}," +
                                "{\"startDate\": \"1st July\",\"endDate\": \"31st July 2022\",\"providerSystemId\": \"3-4\",\"providerName\": \"James Mwai\",\"patientsAllocated\": 30,\"patientsVisited\": 7,\"patientsReturned\": 1}," +
                                "{\"startDate\": \"1st July\",\"endDate\": \"31st July 2022\",\"providerSystemId\": \"4-10\",\"providerName\": \"Agwero Chaplin\",\"patientsAllocated\": 20,\"patientsVisited\": 19,\"patientsReturned\": 1}]";
                        datasetJsonArray = parseDataset(dataset);
                        datasetMap.put(datasetId,datasetJsonArray);
                    }
                }

                int datasetSize = datasetJsonArray.size();
                for (int j=0; j<datasetSize; j++) {
                    JSONObject providerDataset = (JSONObject)datasetJsonArray.get(j);

                    ProviderReportStatistic reportStatistic = new ProviderReportStatistic();
                    if (providerDataset != null && providerDataset.containsKey(achievementKey)) {
                        reportStatistic.setAchievement((Integer) providerDataset.get(achievementKey));
                        reportStatistic.setAchievementId(achievementKey);
                    }
                    if (providerDataset != null && providerDataset.containsKey(expectedAchievementKey)) {
                        reportStatistic.setExpectedAchievement((Integer) providerDataset.get(expectedAchievementKey));
                    }

                    reportStatistic.setAchievementGroupAverage(getAchievementAverage(datasetJsonArray, achievementKey, expectedAchievementKey));

                    int leaderboardScore = reportStatistic.getExpectedAchievement() == 0 ? 0 : reportStatistic.getAchievement()*100/reportStatistic.getExpectedAchievement();
                    reportStatistic.setScore(leaderboardScore);

                    reportStatistic.setStatisticTitle(template.get("title").toString());
                    reportStatistic.setStatisticHint(template.get("hint").toString());
                    reportStatistic.setSummaryColorCode(template.get("colorCode").toString());
                    reportStatistic.setProviderName(providerDataset.get("providerName").toString());
                    reportStatistic.setProviderId(providerDataset.get("providerSystemId").toString());
                    reportStatistic.setStartDate(providerDataset.get("startDate").toString());
                    reportStatistic.setEndDate(providerDataset.get("endDate").toString());

                    Random rnd = new Random();
                    int color = Color.argb(255, 128+rnd.nextInt(120), 128+rnd.nextInt(120), 128+rnd.nextInt(120));
                    reportStatistic.setLeaderboardColor(color);

                    allProviderReportStatistics.add(reportStatistic);
                }

                if(template.containsKey("leaderboardStatisticKey")){
                    leaderboardStatisticKey = template.get("leaderboardStatisticKey").toString();
                }
            } catch (Exception e) {
                Log.e(getClass().getSimpleName(), "Could not parse details of summary statistic",e);
            } catch (ReportDatasetController.ReportDatasetFetchException e) {
                Log.e(getClass().getSimpleName(), "Could not get report dataset",e);
            }
        }
    }

    private void loadDefaultReportView(String userSystemId){
        if(StringUtils.isEmpty(userSystemId)){
            renderLeaderboardView();
        }

        individualProviderStatistics.clear();
        individualProviderStatistics.addAll(getIndividualProviderDataset(userSystemId));
        if(individualProviderStatistics.isEmpty()){
            renderLeaderboardView();
        } else {
            renderIndividualPerformanceFragment();
        }
    }

    private List<ProviderReportStatistic> getIndividualProviderDataset(String providerId){
        return allProviderReportStatistics.stream()
                .filter(statistic -> statistic.getProviderId().equals(providerId))
                .collect(Collectors.toList());
    }

    private void initializeIndividualPerformanceView(){
        PerformanceSummaryFragment.LeaderBoardTitleClickListener leaderBoardTitleClickListener = () -> renderLeaderboardView();
        individualPerformanceSummaryFragment = PerformanceSummaryFragment.newInstance(
                getSummaryStatisticAdapter(), getPerformanceComparisonAdapter(), getLeaderboardAdapter(),
                leaderBoardTitleClickListener);
    }

    private void renderIndividualPerformanceFragment(){
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.report_framelayout,individualPerformanceSummaryFragment);
        transaction.addToBackStack(null);
        transaction.commit();

        getSummaryStatisticAdapter().notifyDataSetChanged();
        getPerformanceComparisonAdapter().notifyDataSetChanged();
        getLeaderboardAdapter().notifyDataSetChanged();

        if(getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.general_individual_summary));
        }
        currentFragment = individualPerformanceSummaryFragment;
    }

    private void initializeLeaderboardView(){
        leaderboardFragment = LeaderboardFragment.newInstance(getLeaderboardAdapter());
    }

    private void renderLeaderboardView(){
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.report_framelayout,leaderboardFragment);
        transaction.addToBackStack(null);
        transaction.commit();
        getLeaderboardAdapter().notifyDataSetChanged();

        if(getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.general_leaderboard));
        }
        currentFragment = leaderboardFragment;
    }

    private LeaderboardAdapter getLeaderboardAdapter(){
        if (leaderboardAdapter == null) {
            List<ProviderReportStatistic> leaderboardStatistics = allProviderReportStatistics.stream()
                    .filter(statistic -> statistic.getAchievementId().equals(leaderboardStatisticKey)).collect(Collectors.toList());
            Collections.sort(leaderboardStatistics, Collections.reverseOrder());
            leaderboardAdapter = new LeaderboardAdapter(leaderboardStatistics, this, getApplicationContext());
        }
        return leaderboardAdapter;
    }

    private SummaryStatisticAdapter getSummaryStatisticAdapter(){
        if (summaryStatisticAdapter == null) {
            summaryStatisticAdapter = new SummaryStatisticAdapter(individualProviderStatistics, getApplicationContext());
        }
        return summaryStatisticAdapter;
    }

    private PerformanceComparisonAdapter getPerformanceComparisonAdapter(){
        if (performanceComparisonAdapter == null) {
            performanceComparisonAdapter = new PerformanceComparisonAdapter(individualProviderStatistics, getApplicationContext());
        }
        return performanceComparisonAdapter;
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

    @Override
    public void onBackPressed() {
        if(currentFragment == individualPerformanceSummaryFragment){
            renderLeaderboardView();
        } else {
            finish();
        }
    }
}
