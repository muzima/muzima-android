/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.custom;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.FormTemplate;
import com.muzima.api.model.Media;
import com.muzima.api.model.MediaCategory;
import com.muzima.api.model.ReportDataset;
import com.muzima.controller.FormController;
import com.muzima.controller.MediaCategoryController;
import com.muzima.controller.MediaController;
import com.muzima.controller.ReportDatasetController;
import com.muzima.model.AvailableForm;
import com.muzima.util.JsonUtils;
import com.muzima.utils.StringUtils;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.MainDashboardActivity;
import com.muzima.view.MediaActivity;
import com.muzima.view.cohort.CohortPagerActivity;
import com.muzima.view.forms.FormPagerActivity;
import com.muzima.view.reports.ProviderReportListActivity;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import java.util.List;

public abstract class ActivityWithBottomNavigation extends BroadcastListenerActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    protected BottomNavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void loadBottomNavigation() {
        navigationView = findViewById(R.id.bottom_navigation);
        if(!((MuzimaApplication)getApplicationContext()).getMuzimaSettingController().isBottomNavigationCohortEnabled()) {
            navigationView.getMenu().removeItem(R.id.action_cohorts);
        }
        if(!((MuzimaApplication)getApplicationContext()).getMuzimaSettingController().isBottomNavigationFormEnabled()) {
            navigationView.getMenu().removeItem(R.id.action_forms);
        }

        boolean isAnyGroupWithMedia = false;
        MediaCategoryController mediaCategoryController = ((MuzimaApplication) getApplication()).getMediaCategoryController();
        MediaController mediaController = ((MuzimaApplication) getApplication()).getMediaController();
        try {
            List<MediaCategory> mediaCategoryList = mediaCategoryController.getMediaCategories();
            for(MediaCategory mediaCategory:mediaCategoryList){
                List<Media> mediaList = mediaController.getMediaByCategoryUuid(mediaCategory.getUuid());
                if(mediaList.size()>0) {
                    isAnyGroupWithMedia = true;
                }
            }
        } catch (MediaCategoryController.MediaCategoryFetchException e) {
            Log.e(getClass().getSimpleName(),"Encountered error while fetching media category ",e);
        } catch (MediaController.MediaFetchException e) {
            Log.e(getClass().getSimpleName(),"Encountered error while fetching media ",e);
        }

        if(!isAnyGroupWithMedia){
            navigationView.getMenu().removeItem(R.id.media);
        }


        boolean isReportDatasetAndTemplateAvailable = false;
        ReportDatasetController reportDatasetController = ((MuzimaApplication) getApplication()).getReportDatasetController();
        FormController formController = ((MuzimaApplication) getApplication()).getFormController();
        try {
            List<AvailableForm> availableForms = formController.getProviderReports();

            if(availableForms.size()>0){
                for(AvailableForm availableForm : availableForms){
                    if(availableForm.isProviderReport()){
                        isReportDatasetAndTemplateAvailable = true;
                        break;
                    }else{
                        FormTemplate formTemplate = formController.getFormTemplateByUuid(availableForm.getFormUuid());
                        String reportDefinition = formTemplate.getHtml();
                        JSONArray reportTemplateDefinitions = (JSONArray) JsonUtils.readAsObject(reportDefinition,"reportTemplate");
                        int templatesCount = reportTemplateDefinitions.size();
                        for (int i=0; i<templatesCount; i++){
                            final JSONObject template = (JSONObject)reportTemplateDefinitions.get(i);
                            String datasetId = template.get("datasetId").toString();
                            if(!StringUtils.isEmpty(datasetId)) {
                                ReportDataset reportDataset = reportDatasetController.getReportDatasetByDatasetDefinitionId(Integer.parseInt(datasetId));
                                if(reportDataset != null){
                                    isReportDatasetAndTemplateAvailable = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (ReportDatasetController.ReportDatasetFetchException e) {
            Log.e(getClass().getSimpleName(),"Encountered an exception while fetching report datasets", e);
        } catch (FormController.FormFetchException e) {
            Log.e(getClass().getSimpleName(),"Encountered an exception while fetching forms", e);
        }

        if(!isReportDatasetAndTemplateAvailable){
            navigationView.getMenu().removeItem(R.id.action_reports);
        }

        navigationView.setOnNavigationItemSelectedListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateNavigationBarState();
    }

    // Remove inter-activity transition to avoid screen tossing on tapping bottom navigation items
    @Override
    public void onPause() {
        super.onPause();
        overridePendingTransition(0, 0);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == getBottomNavigationMenuItemId())
            return true;

        navigationView.post(() -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_home) {
                // Check if no view has focus:
                View view = this.getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                startActivity(new Intent(this, MainDashboardActivity.class));
            } else if (itemId == R.id.action_cohorts) {
                startActivity(new Intent(this, CohortPagerActivity.class));
            } else if (itemId == R.id.action_forms) {
                startActivity(new Intent(this, FormPagerActivity.class));
            } else if (itemId == R.id.action_reports) {
                startActivity(new Intent(this, ProviderReportListActivity.class));
            }else if (itemId == R.id.media) {
                startActivity(new Intent(this, MediaActivity.class));
            }
        });
        return true;
    }

    private void updateNavigationBarState() {
        int actionId = getBottomNavigationMenuItemId();
        selectBottomNavigationBarItem(actionId);
    }

    private void selectBottomNavigationBarItem(int itemId) {
        MenuItem item = navigationView.getMenu().findItem(itemId);
        if(item != null) {
            item.setChecked(true);
        }else{
            startActivity(new Intent(this, MainDashboardActivity.class));
        }
    }

    protected abstract int getBottomNavigationMenuItemId();

    protected BottomNavigationView getBottomNavigationView(){
        return navigationView;
    }
}
