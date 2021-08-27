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

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.model.cohort.CohortItem;
import com.muzima.model.events.CohortSearchEvent;
import com.muzima.model.events.CohortsActionModeEvent;
import com.muzima.model.events.DestroyActionModeEvent;
import com.muzima.scheduler.MuzimaJobScheduleBuilder;
import com.muzima.tasks.DownloadCohortsTask;
import com.muzima.tasks.DownloadFormsTask;
import com.muzima.utils.Constants;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.StringUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.MainDashboardActivity;
import com.muzima.view.custom.ActivityWithBottomNavigation;
import com.muzima.view.custom.CohortsPager;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CohortPagerActivity extends ActivityWithBottomNavigation {
    private ViewPager viewPager;
    private EditText searchCohorts;
    private final ThemeUtils themeUtils = new ThemeUtils();
    private final LanguageUtil languageUtil = new LanguageUtil();
    private ActionMode.Callback actionModeCallback;
    private ActionMode actionMode;
    private List<CohortItem> selectedCohorts = new ArrayList<>();
    private int selectedCohortsCount = 0;
    private int selectionDifference;
    private MenuItem loadingMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cohort_pager);
        themeUtils.onCreate(this);
        languageUtil.onCreate(this);
        loadBottomNavigation();

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        CohortsPager cohortsPager = new CohortsPager(getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(cohortsPager);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
                searchCohorts.setText(StringUtils.EMPTY);
                EventBus.getDefault().post(new DestroyActionModeEvent());
                if (actionMode != null) {
                    actionMode.finish();
                }
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
    protected void onStart() {
        super.onStart();
        try {
            EventBus.getDefault().register(this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Subscribe
    public void onCohortDownloadActionModeEvent(CohortsActionModeEvent actionModeEvent) {
        selectedCohorts = actionModeEvent.getSelectedCohorts();
        initActionMode(Constants.ACTION_MODE_EVENT.COHORTS_DOWNLOAD_ACTION);
    }

    private void initActionMode(final int action) {
        selectedCohortsCount = 0;
        actionModeCallback = new ActionMode.Callback() {

            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                getMenuInflater().inflate(R.menu.menu_cohort_actions, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                loadingMenuItem = menu.findItem(R.id.menu_downloading_action);
                return true;
            }

            @Override
            public boolean onActionItemClicked(final ActionMode actionMode, MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.menu_download_action) {
                    loadingMenuItem.setActionView(new ProgressBar(CohortPagerActivity.this));
                    loadingMenuItem.setVisible(true);
                    menuItem.setVisible(false);
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.info_muzima_sync_service_in_progress), Toast.LENGTH_LONG).show();
                    if (action == Constants.ACTION_MODE_EVENT.COHORTS_DOWNLOAD_ACTION) {
                        ((MuzimaApplication) getApplicationContext()).getExecutorService()
                                .execute(new DownloadCohortsTask(getApplicationContext(), selectedCohorts, new DownloadCohortsTask.CohortDownloadCallback() {
                                    @Override
                                    public void callbackDownload() {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                actionMode.finish();
                                                loadingMenuItem.setVisible(false);
                                                EventBus.getDefault().post(new DestroyActionModeEvent());
                                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.info_muzima_sync_service_finish), Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    }
                                }));
                    }
                }
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
                if (selectionDifference == selectedCohortsCount)
                    EventBus.getDefault().post(new DestroyActionModeEvent());
                else
                    selectionDifference = selectedCohortsCount;
            }
        };

        for (CohortItem selectedCohort : selectedCohorts) {
            if (selectedCohort.isSelected()) selectedCohortsCount = selectedCohortsCount + 1;
        }

        actionMode = startActionMode(actionModeCallback);

        if (action == Constants.ACTION_MODE_EVENT.COHORTS_DOWNLOAD_ACTION) {
            if (selectedCohortsCount < 1) actionMode.finish();
            actionMode.setTitle(String.format(Locale.getDefault(), "%d %s", selectedCohortsCount, getResources().getString(R.string.general_selected)));
        }
    }

    @Override
    protected int getBottomNavigationMenuItemId() {
        return R.id.action_cohorts;
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, MainDashboardActivity.class));
        finish();
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
