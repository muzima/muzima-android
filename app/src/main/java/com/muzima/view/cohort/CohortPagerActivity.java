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
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
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
import com.muzima.utils.Constants.DataSyncServiceConstants;
import com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.StringUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.custom.ActivityWithBottomNavigation;
import org.greenrobot.eventbus.EventBus;;

import static com.muzima.view.BroadcastListenerActivity.MESSAGE_SENT_ACTION;
import static com.muzima.view.BroadcastListenerActivity.PROGRESS_UPDATE_ACTION;

public class CohortPagerActivity extends ActivityWithBottomNavigation {
    private ViewPager viewPager;
    private EditText searchCohorts;
    private final LanguageUtil languageUtil = new LanguageUtil();

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
        setTitle(StringUtils.EMPTY);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_dashboard_home, menu);
        menu.findItem(R.id.menu_location).setVisible(false);
        MenuItem menuRefresh = menu.findItem(R.id.menu_load);
        menuRefresh.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.info_muzima_sync_service_in_progress), Toast.LENGTH_LONG).show();
                new MuzimaJobScheduleBuilder(getApplicationContext()).schedulePeriodicBackgroundJob(1000, true);
                return true;
            }
        });
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(MESSAGE_SENT_ACTION));
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(PROGRESS_UPDATE_ACTION));
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
        int syncStatus = intent.getIntExtra(DataSyncServiceConstants.SYNC_STATUS, SyncStatusConstants.UNKNOWN_ERROR);
        int syncType = intent.getIntExtra(DataSyncServiceConstants.SYNC_TYPE, -1);
        int downloadCount = intent.getIntExtra(DataSyncServiceConstants.DOWNLOAD_COUNT_PRIMARY, 0);

        if (syncStatus != SyncStatusConstants.SUCCESS)
            EventBus.getDefault().post(new CohortsDownloadedEvent(false));
        else {
            String msg = StringUtils.EMPTY;

            switch (syncType) {
                case DataSyncServiceConstants.SYNC_COHORTS_METADATA:
                    msg = getString(R.string.info_new_cohort_download, downloadCount);
                    break;
                case DataSyncServiceConstants.SYNC_SELECTED_COHORTS_PATIENTS_FULL_DATA:
                    int downloadCountSec = intent.getIntExtra(DataSyncServiceConstants.DOWNLOAD_COUNT_SECONDARY, 0);
                    msg = getString(R.string.info_cohort_new_patient_download, downloadCount, downloadCountSec) + getString(R.string.info_patient_data_download);
                    break;
                case DataSyncServiceConstants.SYNC_OBSERVATIONS:
                    msg = getString(R.string.info_new_observation_download, downloadCount);
                    break;
                case DataSyncServiceConstants.SYNC_ENCOUNTERS:
                    msg = getString(R.string.info_new_encounter_download, downloadCount);
                    EventBus.getDefault().post(new CohortsDownloadedEvent(true));
                    break;
            }

            if (StringUtils.isEmpty(msg))
                msg = getString(R.string.info_download_complete, syncStatus) + " Sync type = " + intent.getIntExtra(DataSyncServiceConstants.SYNC_TYPE, -1);

            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected int getBottomNavigationMenuItemId() {
        return R.id.action_cohorts;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }
}
