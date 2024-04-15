/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.cohort;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;
import com.muzima.R;
import com.muzima.adapters.cohort.CohortsPagerAdapter;
import com.muzima.model.events.CohortSearchEvent;
import com.muzima.model.events.CohortsDownloadedEvent;
import com.muzima.model.events.DestroyActionModeEvent;
import com.muzima.scheduler.MuzimaJobScheduleBuilder;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.StringUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.custom.ActivityWithBottomNavigation;
import org.greenrobot.eventbus.EventBus;;
import com.muzima.utils.Constants;

public class CohortPagerActivity extends ActivityWithBottomNavigation {
    private ViewPager viewPager;
    private EditText searchCohorts;
    private final LanguageUtil languageUtil = new LanguageUtil();

    private ActionMenuItemView refreshMenuActionView;
    private ActionMenuItemView syncReportMenuActionView;
    private Drawable syncReportMenuIconDrawable;
    private Animation refreshIconRotateAnimation;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            CohortPagerActivity.this.onReceive(context, intent);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.getInstance().onCreate(this,true);
        languageUtil.onCreate(this);
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_cohort_pager);
        loadBottomNavigation();

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        CohortsPagerAdapter cohortsPager = new CohortsPagerAdapter(getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(cohortsPager);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
                searchCohorts.setText(StringUtils.EMPTY);
                EventBus.getDefault().post(new DestroyActionModeEvent());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        searchCohorts = findViewById(R.id.search_cohorts);
        searchCohorts.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (searchCohorts.getText().toString() != null && !searchCohorts.getText().toString().isEmpty())
                    EventBus.getDefault().post(new CohortSearchEvent(searchCohorts.getText().toString(), viewPager.getCurrentItem()));
                else if (searchCohorts.getText().toString().isEmpty())
                    EventBus.getDefault().post(new CohortSearchEvent(searchCohorts.getText().toString(), viewPager.getCurrentItem()));
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        refreshIconRotateAnimation = AnimationUtils.loadAnimation(CohortPagerActivity.this, R.anim.rotate_refresh);
        refreshIconRotateAnimation.setRepeatCount(Animation.INFINITE);
        refreshMenuActionView = findViewById(R.id.menu_load);
        syncReportMenuActionView = findViewById(R.id.menu_sync_report);

        refreshMenuActionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                processSync(refreshIconRotateAnimation);
            }
        });

        syncReportMenuActionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showBackgroundSyncProgressDialog(CohortPagerActivity.this);
            }
        });
        syncReportMenuActionView.setVisibility(View.GONE);

        Toolbar toolbar = findViewById(R.id.cohort_pager_toolbar);
        syncReportMenuIconDrawable = toolbar.getMenu().findItem(R.id.menu_sync_report).getIcon();

        findViewById(R.id.menu_location).setVisibility(View.GONE);
        findViewById(R.id.menu_tags).setVisibility(View.GONE);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        setTitle(StringUtils.EMPTY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(MESSAGE_SENT_ACTION));
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(PROGRESS_UPDATE_ACTION));

        if(isDataSyncRunning() && refreshMenuActionView != null){
            refreshMenuActionView.startAnimation(refreshIconRotateAnimation);
        } else if(refreshMenuActionView != null){
            refreshMenuActionView.clearAnimation();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            EventBus.getDefault().register(this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected void onReceive(Context context, Intent intent) {
        int syncStatus = intent.getIntExtra(Constants.DataSyncServiceConstants.SYNC_STATUS, Constants.DataSyncServiceConstants.SyncStatusConstants.UNKNOWN_ERROR);
        int syncType = intent.getIntExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, -1);
        int downloadCount = intent.getIntExtra(Constants.DataSyncServiceConstants.DOWNLOAD_COUNT_PRIMARY, 0);

        if (syncStatus != Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS)
            EventBus.getDefault().post(new CohortsDownloadedEvent(false));
        else {
            String msg = StringUtils.EMPTY;

            switch (syncType) {
                case Constants.DataSyncServiceConstants.SYNC_COHORTS_METADATA:
                    msg = getString(R.string.info_new_cohort_download, downloadCount);
                    break;
                case Constants.DataSyncServiceConstants.SYNC_SELECTED_COHORTS_PATIENTS_FULL_DATA:
                    int downloadCountSec = intent.getIntExtra(Constants.DataSyncServiceConstants.DOWNLOAD_COUNT_SECONDARY, 0);
                    msg = getString(R.string.info_cohort_new_patient_download, downloadCount, downloadCountSec) + getString(R.string.info_patient_data_download);
                    break;
                case Constants.DataSyncServiceConstants.SYNC_OBSERVATIONS:
                    msg = getString(R.string.info_new_observation_download, downloadCount);
                    break;
                case Constants.DataSyncServiceConstants.SYNC_ENCOUNTERS:
                    msg = getString(R.string.info_new_encounter_download, downloadCount);
                    EventBus.getDefault().post(new CohortsDownloadedEvent(true));
                    break;
            }

            if (StringUtils.isEmpty(msg))
                msg = getString(R.string.info_download_complete, syncStatus) + " Sync type = " + intent.getIntExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, -1);

            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected int getBottomNavigationMenuItemId() {
        return R.id.action_cohorts;
    }

    private void processSync(Animation rotation){
        if(!isDataSyncRunning()) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.info_muzima_sync_service_in_progress), Toast.LENGTH_LONG).show();
            new MuzimaJobScheduleBuilder(getApplicationContext()).schedulePeriodicBackgroundJob(1000, true);

            refreshMenuActionView.startAnimation(rotation);
            syncReportMenuActionView.setVisibility(View.GONE);

            notifySyncStarted();
            showBackgroundSyncProgressDialog(CohortPagerActivity.this);
        } else {
            showBackgroundSyncProgressDialog(CohortPagerActivity.this);
        }
    }

    protected void updateSyncProgressWidgets(boolean isSyncRunning){
        if(isSyncRunning == false){
            refreshMenuActionView.clearAnimation();
            syncReportMenuActionView.setVisibility(View.VISIBLE);
            if(isSyncCompletedWithError()){
                syncReportMenuIconDrawable.mutate();
                syncReportMenuIconDrawable.setColorFilter(getResources().getColor(R.color.red,getTheme()), PorterDuff.Mode.SRC_ATOP);
            } else {
                syncReportMenuIconDrawable.mutate();
                syncReportMenuIconDrawable.setColorFilter(getResources().getColor(R.color.green,getTheme()), PorterDuff.Mode.SRC_ATOP);
            }
        } else{
            refreshMenuActionView.startAnimation(refreshIconRotateAnimation);
        }
    }
}
