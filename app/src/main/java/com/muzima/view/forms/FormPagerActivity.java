/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.tabs.TabLayout;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Form;
import com.muzima.model.events.DestroyActionModeEvent;
import com.muzima.model.events.FormFilterBottomSheetClosedEvent;
import com.muzima.model.events.FormSearchEvent;
import com.muzima.model.events.FormSortEvent;
import com.muzima.model.events.FormsActionModeEvent;
import com.muzima.model.events.ShowFormsFilterEvent;
import com.muzima.scheduler.MuzimaJobScheduleBuilder;
import com.muzima.tasks.DownloadFormsTask;
import com.muzima.utils.Constants;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.MuzimaPreferences;
import com.muzima.utils.StringUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.custom.ActivityWithBottomNavigation;
import com.muzima.adapters.forms.NewFormsPagerAdapter;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static androidx.core.view.WindowCompat.FEATURE_ACTION_BAR;

public class FormPagerActivity extends ActivityWithBottomNavigation {
    private ViewPager viewPager;
    private EditText searchForms;
    private final LanguageUtil languageUtil = new LanguageUtil();
    private ActionMode.Callback actionModeCallback;
    private ActionMode actionMode;
    private boolean formSelected = false;
    private List<Form> selectedForms = new ArrayList<>();
    private MenuItem loadingMenuItem;

    private BottomSheetBehavior formFilterBottomSheetBehavior;
    private View formFilterBottomSheetView;
    private View closeFormsBottomSheetView;
    private View formFilterStatusContainer;
    private CheckBox formFilterStatusCheckbox;
    private View formFilterNamesContainer;
    private CheckBox formFilterNamesCheckbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.getInstance().onCreate(this,true);
        languageUtil.onCreate(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_pager);
        loadBottomNavigation();

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        NewFormsPagerAdapter formsPager = new NewFormsPagerAdapter(getSupportFragmentManager() , tabLayout.getTabCount(), this);
        viewPager.setAdapter(formsPager);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
                searchForms.setText(StringUtils.EMPTY);
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

        searchForms = findViewById(R.id.search_forms);
        searchForms.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                EventBus.getDefault().post(new FormSearchEvent(searchForms.getText().toString(), viewPager.getCurrentItem()));
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        formFilterBottomSheetView = findViewById(R.id.dashboard_home_form_bottom_view_container);
        formFilterBottomSheetBehavior = BottomSheetBehavior.from(formFilterBottomSheetView);
        formFilterNamesContainer = findViewById(R.id.form_filter_by_name_container);
        formFilterNamesCheckbox = findViewById(R.id.form_filter_name_checkbox);
        formFilterStatusContainer = findViewById(R.id.form_filter_by_status_container);
        formFilterStatusCheckbox = findViewById(R.id.form_filter_status_checkbox);
        closeFormsBottomSheetView = findViewById(R.id.forms_bottom_sheet_close_view);

        closeFormsBottomSheetView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                formFilterBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });

        formFilterStatusCheckbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                formFilterNamesCheckbox.setChecked(false);
                formFilterStatusContainer.setBackground(getResources().getDrawable(R.drawable.global_highlight_background));
                if (MuzimaPreferences.getIsLightModeThemeSelectedPreference(getApplicationContext()))
                    formFilterNamesContainer.setBackgroundColor(getResources().getColor(R.color.primary_white));
                else
                    formFilterNamesContainer.setBackgroundColor(getResources().getColor(R.color.primary_black));
                EventBus.getDefault().post(new FormSortEvent(Constants.FORM_SORT_STRATEGY.SORT_BY_STATUS));
                formFilterBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });

        formFilterNamesCheckbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                formFilterStatusCheckbox.setChecked(false);
                formFilterNamesContainer.setBackground(getResources().getDrawable(R.drawable.global_highlight_background));
                if (MuzimaPreferences.getIsLightModeThemeSelectedPreference(getApplicationContext()))
                    formFilterStatusContainer.setBackgroundColor(getResources().getColor(R.color.primary_white));
                else
                    formFilterStatusContainer.setBackgroundColor(getResources().getColor(R.color.primary_black));
                EventBus.getDefault().post(new FormSortEvent(Constants.FORM_SORT_STRATEGY.SORT_BY_NAME));
                formFilterBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });
        formFilterBottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    EventBus.getDefault().post(new FormFilterBottomSheetClosedEvent(true));
                } else {
                    EventBus.getDefault().post(new FormFilterBottomSheetClosedEvent(false));
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

        formFilterBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
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
    public void showFormsFilterBottomSheetEvent(ShowFormsFilterEvent event) {
        if (event.isCloseAction()) {
            formFilterBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        } else {
            formFilterBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    @Subscribe
    public void onFormsDownloadActionModeEvent(FormsActionModeEvent actionModeEvent) {
        selectedForms = actionModeEvent.getSelectedFormsList();
        formSelected = actionModeEvent.getFormSelected();
        initActionMode(Constants.ACTION_MODE_EVENT.FORMS_DOWNLOAD_ACTION);
    }

    private void initActionMode(final int action) {
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
                    loadingMenuItem.setActionView(new ProgressBar(FormPagerActivity.this));
                    loadingMenuItem.setVisible(true);
                    menuItem.setVisible(false);
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.info_muzima_sync_service_in_progress), Toast.LENGTH_LONG).show();
                    if (action == Constants.ACTION_MODE_EVENT.FORMS_DOWNLOAD_ACTION) {
                        ((MuzimaApplication) getApplicationContext()).getExecutorService()
                                .execute(new DownloadFormsTask(getApplicationContext(), selectedForms, new DownloadFormsTask.FormsDownloadCallback() {
                                    @Override
                                    public void formsDownloadFinished() {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                selectedForms.clear();
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
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
                if (!formSelected)
                    EventBus.getDefault().post(new DestroyActionModeEvent());
            }
        };

        actionMode = startActionMode(actionModeCallback);

        formSelected = false;
        if (selectedForms.size() < 1) actionMode.finish();
        actionMode.setTitle(String.format(Locale.getDefault(), "%d %s", selectedForms.size(), getResources().getString(R.string.general_selected)));
    }

    @Override
    protected int getBottomNavigationMenuItemId() {
        return R.id.action_forms;
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
