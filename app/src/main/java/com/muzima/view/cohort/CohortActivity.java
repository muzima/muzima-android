/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.cohort;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;
import com.muzima.R;
import com.muzima.adapters.cohort.CohortPagerAdapter;
import com.muzima.utils.Fonts;
import com.muzima.utils.NetworkUtils;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.custom.PagerSlidingTabStrip;

import static com.muzima.utils.Constants.DataSyncServiceConstants;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants;

public class CohortActivity extends BroadcastListenerActivity {
    private ViewPager viewPager;
    private CohortPagerAdapter cohortPagerAdapter;
    private MenuItem menubarLoadButton;
    private boolean syncInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_pager);
        initPager();
        initPagerIndicator();
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
                    Toast.makeText(this, getString(R.string.info_cohort_fetch_in_progress), Toast.LENGTH_SHORT).show();
                    return true;
                }

                syncCohortsInBackgroundService();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        int syncStatus = intent.getIntExtra(DataSyncServiceConstants.SYNC_STATUS, SyncStatusConstants.UNKNOWN_ERROR);
        int syncType = intent.getIntExtra(DataSyncServiceConstants.SYNC_TYPE, -1);

        switch (syncType) {
            case DataSyncServiceConstants.SYNC_COHORTS:
                hideProgressbar();
                syncInProgress = false;
                if (syncStatus == SyncStatusConstants.SUCCESS) {
                    cohortPagerAdapter.onCohortDownloadFinish();
                }
                break;
            case DataSyncServiceConstants.SYNC_PATIENTS_FULL_DATA:
                if (syncStatus == SyncStatusConstants.SUCCESS) {
                    cohortPagerAdapter.onPatientsDownloadFinish();
                }
                break;
            case DataSyncServiceConstants.SYNC_ENCOUNTERS:
                hideProgressbar();
                break;
        }
    }

    private void hideProgressbar() {
        menubarLoadButton.setActionView(null);
    }

    public void showProgressBar() {
        menubarLoadButton.setActionView(R.layout.refresh_menuitem);
    }

    private void initPager() {
        viewPager = findViewById(R.id.pager);
        cohortPagerAdapter = new CohortPagerAdapter(getApplicationContext(), getSupportFragmentManager());
        cohortPagerAdapter.initPagerViews();
        viewPager.setAdapter(cohortPagerAdapter);
    }

    private void initPagerIndicator() {
        PagerSlidingTabStrip pagerTabsLayout = findViewById(R.id.pager_indicator);
        pagerTabsLayout.setTextColor(Color.WHITE);
        pagerTabsLayout.setTextSize((int) getResources().getDimension(R.dimen.pager_indicator_text_size));
        pagerTabsLayout.setSelectedTextColor(getResources().getColor(R.color.tab_indicator));
        pagerTabsLayout.setTypeface(Fonts.roboto_medium(this), -1);
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

    private void onPageChange(int position) {
        if (menubarLoadButton == null) {
            return;
        }
        switch (position) {
            case CohortPagerAdapter.TAB_SYNCED:
                cohortPagerAdapter.reinitializeAllCohortsTab();
                menubarLoadButton.setVisible(false);
                break;
            case CohortPagerAdapter.TAB_All:
                menubarLoadButton.setVisible(true);
                break;
        }
    }

    private void syncCohortsInBackgroundService() {
        syncInProgress = true;
        cohortPagerAdapter.onCohortDownloadStart();
        showProgressBar();
        new SyncCohortsIntent(this).start();
    }

    public void setCurrentView(int position){
        viewPager.setCurrentItem(position);
    }
}
