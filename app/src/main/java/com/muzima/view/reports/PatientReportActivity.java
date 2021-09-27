/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.reports;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.viewpager.widget.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.muzima.R;
import com.muzima.adapters.reports.PatientReportPagerAdapter;
import com.muzima.api.model.Patient;
import com.muzima.utils.Constants;
import com.muzima.utils.NetworkUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.custom.PagerSlidingTabStrip;
import com.muzima.view.patients.PatientSummaryActivity;

public class PatientReportActivity extends BroadcastListenerActivity {
    private static final int REPORT_VIEW_ACTIVITY_RESULT = 1;
    private Patient patient;
    private MenuItem menubarLoadButton;
    private boolean syncInProgress;

    private ViewPager viewPager;
    private PatientReportPagerAdapter patientReportPagerAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ThemeUtils.getInstance().onCreate(this,true);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_pager);
        Intent intent = getIntent();
        patient = (Patient) intent.getSerializableExtra(PatientSummaryActivity.PATIENT);
        initPager();
        initPagerIndicator();
        getActionBar().setTitle(patient.getSummary());
        logEvent("VIEW_CLIENT_DOWNLOADED_REPORTS", "{\"patientuuid\":\""+patient.getUuid()+"\"}");
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.cohort_list_menu, menu);
        menubarLoadButton = menu.findItem(R.id.menu_load);
        menubarLoadButton.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_load:
                if (!NetworkUtils.isConnectedToNetwork(this)) {
                    Toast.makeText(this, R.string.error_local_connection_unavailable, Toast.LENGTH_SHORT).show();
                    return true;
                }

                if (syncInProgress) {
                    Toast.makeText(this, getString(R.string.info_patient_reports_fetch_in_progress), Toast.LENGTH_SHORT).show();
                    return true;
                }

                syncPatientReportsInBackgroundService();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        int syncStatus = intent.getIntExtra(Constants.DataSyncServiceConstants.SYNC_STATUS, Constants.DataSyncServiceConstants.SyncStatusConstants.UNKNOWN_ERROR);
        int syncType = intent.getIntExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, -1);

        switch (syncType) {
            case Constants.DataSyncServiceConstants.SYNC_PATIENT_REPORTS_HEADERS:
                hideProgressbar();
                syncInProgress = false;
                if (syncStatus == Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS) {
                    patientReportPagerAdapter.onPatientReportsDownloadFinish();
                }
                break;
            case Constants.DataSyncServiceConstants.SYNC_PATIENT_REPORTS:
                hideProgressbar();
                if (syncStatus == Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS) {
                    patientReportPagerAdapter.onSelectedReportDownloadFinish();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REPORT_VIEW_ACTIVITY_RESULT) {
            patientReportPagerAdapter.reloadData();
        }
    }

    private void initPager() {
        viewPager = findViewById(R.id.pager);
//        patientReportPagerAdapter = new PatientReportPagerAdapter(getApplicationContext(), getSupportFragmentManager(), patient.getUuid());;
//        patientReportPagerAdapter.initPagerViews();
//        viewPager.setAdapter(patientReportPagerAdapter);
    }


    private void initPagerIndicator() {
        PagerSlidingTabStrip pagerTabsLayout = findViewById(R.id.pager_indicator);
        pagerTabsLayout.setTextColor(pagerTabsLayout.getIndicatorTextColor());
        pagerTabsLayout.setTextSize((int) getResources().getDimension(R.dimen.pager_indicator_text_size));
        pagerTabsLayout.setSelectedTextColor(getResources().getColor(R.color.tab_indicator));
        pagerTabsLayout.setViewPager(viewPager);
        viewPager.setCurrentItem(0);
        pagerTabsLayout.markCurrentSelected(0);
        pagerTabsLayout.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener(){
            @Override
            public void onPageSelected(int position) {
                onPageChange(position);
            }
        });
    }

    private void onPageChange(int position){
        if (menubarLoadButton == null) {
            return;
        }
        switch (position) {
            case PatientReportPagerAdapter.TAB_AVAILABLE:
                patientReportPagerAdapter.reinitializeAllPatientReportsTab();
                menubarLoadButton.setVisible(false);
                break;
            case PatientReportPagerAdapter.TAB_All:
                menubarLoadButton.setVisible(true);
                break;
        }
    }

    private void hideProgressbar() {
        menubarLoadButton.setActionView(null);
    }

    public void showProgressBar() {
        menubarLoadButton.setActionView(R.layout.refresh_menuitem);
    }

    private void syncPatientReportsInBackgroundService() {
        syncInProgress = true;
        patientReportPagerAdapter.onPatientReportsDownloadStart();
        showProgressBar();
        new SyncPatientReportsIntent(this, patient.getUuid()).start();
    }
}
