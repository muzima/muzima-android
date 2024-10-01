/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view;

import static android.content.DialogInterface.BUTTON_POSITIVE;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.StrictMode;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentContainerView;
import androidx.legacy.app.ActionBarDrawerToggle;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.appcompat.widget.Toolbar;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.cohort.CohortFilterAdapter;
import com.muzima.adapters.patients.PatientTagsListAdapter;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PatientIdentifier;
import com.muzima.api.model.PatientTag;
import com.muzima.api.model.SetupConfiguration;
import com.muzima.api.model.SetupConfigurationTemplate;
import com.muzima.api.model.SmartCardRecord;
import com.muzima.controller.FormController;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.controller.PatientController;
import com.muzima.controller.SetupConfigurationController;
import com.muzima.domain.Credentials;
import com.muzima.model.CohortFilter;
import com.muzima.model.CohortWithFilter;
import com.muzima.model.events.BottomSheetToggleEvent;
import com.muzima.model.events.CloseBottomSheetEvent;
import com.muzima.model.events.CohortFilterActionEvent;
import com.muzima.model.events.ShowCohortFilterEvent;
import com.muzima.model.events.UploadedFormDataEvent;
import com.muzima.scheduler.MuzimaJobScheduleBuilder;
import com.muzima.scheduler.RealTimeFormUploader;
import com.muzima.service.TagPreferenceService;
import com.muzima.service.WizardFinishPreferenceService;
import com.muzima.tasks.LoadDownloadedCohortsTask;
import com.muzima.utils.Constants;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.MuzimaPreferences;
import com.muzima.utils.StringUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.utils.smartcard.KenyaEmrShrMapper;
import com.muzima.utils.smartcard.SmartCardIntentIntegrator;
import com.muzima.utils.smartcard.SmartCardIntentResult;
import com.muzima.view.barcode.BarcodeCaptureActivity;
import com.muzima.view.custom.ActivityWithBottomNavigation;
import com.muzima.view.login.LoginActivity;
import com.muzima.view.patients.PatientsLocationMapActivity;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.muzima.utils.smartcard.SmartCardIntentIntegrator.SMARTCARD_READ_REQUEST_CODE;

public class MainDashboardActivity extends ActivityWithBottomNavigation implements CohortFilterAdapter.CohortFilterClickedListener {
    private static final int RC_BARCODE_CAPTURE = 9001;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView headerTitleTextView;
    private TextView headerTitleConfigNameTextView;
    private final LanguageUtil languageUtil = new LanguageUtil();
    private Credentials credentials;
    private BottomSheetBehavior cohortFilterBottomSheetBehavior;
    private View cohortFilterBottomSheetView;
    private View closeBottomSheet;
    private CohortFilterAdapter cohortFilterAdapter;
    private RecyclerView filterOptionsRecyclerView;
    private List<CohortFilter> cohortList = new ArrayList<>();
    private List<CohortFilter> selectedCohortFilters = new ArrayList<>();
    private SmartCardRecord smartCardRecord;
    private Patient SHRPatient;
    private Patient SHRToMuzimaMatchingPatient;

    private AppBarConfiguration mAppBarConfiguration;
    private NavController navController;
    private boolean isChangedToOnlineMode;
    private boolean isAlmostAutoLoggedOut;
    private static final long INTERVAL = 1000L;
    private long timeRemaining;
    private boolean isTimerReset = false;
    private ActionMenuItemView refreshMenuActionView;
    private ActionMenuItemView syncReportMenuActionView;
    private Drawable syncReportMenuIconDrawable;
    private Animation refreshIconRotateAnimation;

    private CountDownTimer mCountDownTimer;
    private DrawerLayout mainLayout;
    private TagPreferenceService tagPreferenceService;
    private PatientTagsListAdapter tagsListAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeUtils.getInstance().onCreate(MainDashboardActivity.this,false);
        languageUtil.onCreate(MainDashboardActivity.this);
        super.onCreate(savedInstanceState);
        mainLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_main, null);
        setContentView(mainLayout);
        if (android.os.Build.VERSION.SDK_INT > 9) { StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build(); StrictMode.setThreadPolicy(policy); }
        loadBottomNavigation();
        RealTimeFormUploader.getInstance().uploadAllCompletedForms(getApplicationContext(), false);
        initializeResources();
        loadCohorts(false);

        tagPreferenceService = new TagPreferenceService(this);
        initDrawer();

        Intent intent = getIntent();
        if (intent.hasExtra("OnlineMode")) {
            isChangedToOnlineMode = (boolean) intent.getSerializableExtra("OnlineMode");
            showDialog();
        }

        if (intent.hasExtra("AutoLogOutTimer")) {
            timeRemaining = (long) intent.getSerializableExtra("RemainingTime");
            isAlmostAutoLoggedOut = (boolean) intent.getSerializableExtra("AutoLogOutTimer");
            showLogOutDialog();
        }
    }

    public void showDialog(){
        if(isChangedToOnlineMode) {
            new AlertDialog.Builder(this)
                    .setCancelable(true)
                    .setIcon(ThemeUtils.getIconWarning(this))
                    .setTitle(getResources().getString(R.string.online_mode_switch))
                    .setMessage(getResources().getString(R.string.online_mode_switch_warning))
                    .setPositiveButton(getString(R.string.general_ok), null)
                    .create()
                    .show();
        }
    }

    public void showLogOutDialog(){
        int remainingtime = (int) ((int)Math.round(timeRemaining*0.001 * 10d) / 10d);
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(R.string.title_auto_logout);
        alertDialog.setMessage(getString(R.string.message_auto_logout, remainingtime));
        alertDialog.setButton(BUTTON_POSITIVE, getString(R.string.general_reset), positiveClickListener());
        alertDialog.show();
        mCountDownTimer = new CountDownTimer(timeRemaining, INTERVAL) {
            @Override
            public void onTick(long l) {
                int remainingtime = (int) ((int)Math.round(l*0.001 * 10d) / 10d);
                alertDialog.setMessage(getString(R.string.message_auto_logout, remainingtime));
            }

            @Override
            public void onFinish() {
                if(!isTimerReset) {
                    if(((MuzimaApplication) getApplication()).getAuthenticatedUser() != null) {
                        ((MuzimaApplication) getApplication()).logOut();
                        launchLoginActivity();
                    }
                }
            }
        };
        mCountDownTimer.start();
    }

    private Dialog.OnClickListener positiveClickListener() {
        return new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                isTimerReset = true;
                ((MuzimaApplication) getApplicationContext()).restartTimer();
                dialog.dismiss();
            }

        };
    }

    private void loadCohorts(final boolean showFilter) {
        Dialog progressDialog = new ProgressDialog(this, android.R.style.Theme_Panel);
        if (showFilter) {
            progressDialog.setCancelable(false);
            progressDialog.show();
        }
        selectedCohortFilters.clear();
        ((MuzimaApplication) getApplicationContext()).getExecutorService()
                .execute(new LoadDownloadedCohortsTask(getApplicationContext(), new LoadDownloadedCohortsTask.OnDownloadedCohortsLoadedCallback() {
                    @Override
                    public void onCohortsLoaded(final List<CohortWithFilter> cohortWithFilters) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                cohortList.clear();
                                if (selectedCohortFilters.size()==0)
                                    cohortList.add(new CohortFilter(null, true));
                                else if(selectedCohortFilters.size()==1 && selectedCohortFilters.get(0).getCohortWithFilter()==null)
                                    cohortList.add(new CohortFilter(null, true));
                                else
                                    cohortList.add(new CohortFilter(null, false));

                                for (CohortWithFilter cohortWithFilter : cohortWithFilters) {
                                    boolean isCohortSelected = false;
                                    for(CohortFilter cohortFilter : selectedCohortFilters){
                                        if(cohortFilter.getCohortWithFilter() != null) {
                                           if (!cohortWithFilter.getDerivedObservationFilter().isEmpty() && cohortFilter.getCohortWithFilter().getCohort().getUuid().equals(cohortWithFilter.getCohort().getUuid()) && cohortFilter.getCohortWithFilter().getDerivedObservationFilter().equals(cohortWithFilter.getDerivedObservationFilter())) {
                                                isCohortSelected = true;
                                            }
                                            else if (!cohortWithFilter.getObservationFilter().isEmpty() && cohortFilter.getCohortWithFilter().getCohort().getUuid().equals(cohortWithFilter.getCohort().getUuid()) && cohortFilter.getCohortWithFilter().getObservationFilter().equals(cohortWithFilter.getObservationFilter())) {
                                                isCohortSelected = true;
                                            }
                                        }
                                    }
                                    cohortList.add(new CohortFilter(cohortWithFilter, isCohortSelected));
                                }
                                cohortFilterAdapter.notifyDataSetChanged();
                                if (showFilter) {
                                    cohortFilterBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                                    progressDialog.dismiss();
                                }
                            }
                        });
                    }
                }));
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            if (!EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().register(this);
            }
        } catch (Exception ex) {
            Log.e(getClass().getSimpleName(), "Encountered an Exception while unregistering an event ", ex);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            if (EventBus.getDefault().isRegistered(this))
                EventBus.getDefault().unregister(this);
        } catch (Exception ex) {
            Log.e(getClass().getSimpleName(), "Encountered an Exception while unregistering an event ", ex);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (!EventBus.getDefault().isRegistered(this))
                EventBus.getDefault().unregister(this);
        } catch (Exception ex) {
            Log.e(getClass().getSimpleName(), "Encountered an Exception while unregistering an event ", ex);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            if (!EventBus.getDefault().isRegistered(this))
                EventBus.getDefault().unregister(this);
        } catch (Exception ex) {
            Log.e(getClass().getSimpleName(), "Encountered an Exception while unregistering an event ", ex);
        }
    }

    @Subscribe
    public void closeBottomSheetEvent(CloseBottomSheetEvent event) {
        cohortFilterBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    @Subscribe
    public void showCohortFilterEvent(ShowCohortFilterEvent event) {
        loadCohorts(true);
    }

    private void initializeResources() {
        Toolbar toolbar = findViewById(R.id.dashboard_toolbar);

        refreshIconRotateAnimation = AnimationUtils.loadAnimation(MainDashboardActivity.this, R.anim.rotate_refresh);
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
                showBackgroundSyncProgressDialog(MainDashboardActivity.this);
            }
        });
        syncReportMenuActionView.setVisibility(View.GONE);
        syncReportMenuIconDrawable = toolbar.getMenu().findItem(R.id.menu_sync_report).getIcon();

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                switch(item.getItemId()){
                    case R.id.menu_location:
                        navigateToClientsLocationMap();
                        break;

                    case R.id.menu_tags:
                        if (mainLayout.isDrawerOpen(GravityCompat.END)) {
                            mainLayout.closeDrawer(GravityCompat.END);
                        } else {
                            mainLayout.openDrawer(GravityCompat.END);
                        }
                        return true;
                }

                return true;
            }
        });
        ActionMenuItemView locationMenu = findViewById(R.id.menu_location);
        ActionMenuItemView tagsMenu = findViewById(R.id.menu_tags);


        MuzimaSettingController muzimaSettingController = ((MuzimaApplication) getApplicationContext()).getMuzimaSettingController();
        boolean isGeomappingEnabled = muzimaSettingController.isGeoMappingEnabled();
        boolean isTagGenerationEnabled = muzimaSettingController.isPatientTagGenerationEnabled();

        if(!isGeomappingEnabled)
            locationMenu.setVisibility(View.GONE);

        if(!isTagGenerationEnabled && tagsMenu != null){
            tagsMenu.setVisibility(View.GONE);
        }

        drawerLayout = findViewById(R.id.main_dashboard_drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        cohortFilterBottomSheetView = findViewById(R.id.dashboard_home_bottom_view_container);

        cohortFilterBottomSheetBehavior = BottomSheetBehavior.from(cohortFilterBottomSheetView);

        closeBottomSheet = findViewById(R.id.bottom_sheet_close_view);
        filterOptionsRecyclerView = findViewById(R.id.dashboard_home_filter_recycler_view);
        cohortFilterAdapter = new CohortFilterAdapter(getApplicationContext(), cohortList, this);

        filterOptionsRecyclerView.setAdapter(cohortFilterAdapter);
        filterOptionsRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        headerTitleTextView = navigationView.getHeaderView(0).findViewById(R.id.dashboard_header_title_text_view);
        headerTitleConfigNameTextView = navigationView.getHeaderView(0).findViewById(R.id.dashboard_header_title_config_name_text_view);

        FragmentContainerView fragmentContainerView = findViewById(R.id.nav_host_fragment);

        getBottomNavigationView().addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) fragmentContainerView.getLayoutParams();
                params.setMargins(0, 0, 0, view.getHeight());
                fragmentContainerView.setLayoutParams(params);

                LinearLayout.LayoutParams bottomSheetFilterOptionsParams = (LinearLayout.LayoutParams)filterOptionsRecyclerView.getLayoutParams();
                bottomSheetFilterOptionsParams.setMargins(0, 0, 0,view.getHeight());
                filterOptionsRecyclerView.setLayoutParams(bottomSheetFilterOptionsParams);
            }
        });

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_settings, R.id.nav_active_config_selection, R.id.nav_help, R.id.nav_feedback, R.id.nav_contact, R.id.nav_about_us)
                .setOpenableLayout(drawerLayout)
                .build();
        navigationView.post(() -> {
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
            if (navHostFragment != null) {
                navController = navHostFragment.getNavController();
                NavigationUI.setupWithNavController(toolbar, navController, mAppBarConfiguration);
                NavigationUI.setupWithNavController(navigationView, navController);
            }
        });

        MenuItem navLogout = navigationView.getMenu().findItem(R.id.nav_logout);
        navLogout.setOnMenuItemClickListener(item -> {
            showExitAlertDialog();
            return true;
        });

        MenuItem navSwitchConfigs = navigationView.getMenu().findItem(R.id.nav_active_config_selection);
        SetupConfigurationController configController = ((MuzimaApplication) getApplicationContext()).getSetupConfigurationController();
        navSwitchConfigs.setVisible(configController.hasMultipleConfigTemplates());

        credentials = new Credentials(this);

        closeBottomSheet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cohortFilterBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });

        cohortFilterBottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    EventBus.getDefault().post(new CohortFilterActionEvent(selectedCohortFilters));
                }
                EventBus.getDefault().post(new BottomSheetToggleEvent(newState));
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

        cohortFilterBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        if (((MuzimaApplication) getApplicationContext()).getAuthenticatedUser() != null) {
            headerTitleTextView.setText(((MuzimaApplication) getApplicationContext()).getAuthenticatedUser().getUsername());

            try {
                SetupConfigurationController setupConfigurationController = ((MuzimaApplication) getApplicationContext()).getSetupConfigurationController();
                SetupConfigurationTemplate template = setupConfigurationController.getActiveSetupConfigurationTemplate();
                SetupConfiguration setupConfiguration = setupConfigurationController.getSetupConfigurations(template.getUuid());
                headerTitleConfigNameTextView.setText("(" + setupConfiguration.getName() + ")");
            } catch (Throwable e) {
                Log.e(getClass().getSimpleName(),"Cannot fetch active setup config",e);
            }
        }
        setTitle(StringUtils.EMPTY);
    }

    private void processSync(Animation rotation){
        if(!isDataSyncRunning()) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.info_muzima_sync_service_in_progress), Toast.LENGTH_LONG).show();
            new MuzimaJobScheduleBuilder(getApplicationContext()).schedulePeriodicBackgroundJob(1000, true);

            refreshMenuActionView.startAnimation(rotation);
            syncReportMenuActionView.setVisibility(View.GONE);

            notifySyncStarted();
            showBackgroundSyncProgressDialog(MainDashboardActivity.this);
        } else {
            showBackgroundSyncProgressDialog(MainDashboardActivity.this);
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


    @Override
    public boolean onNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onNavigateUp();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent dataIntent) {
        super.onActivityResult(requestCode, resultCode, dataIntent);
        switch (requestCode) {
            case SMARTCARD_READ_REQUEST_CODE:
                processSmartCardReadResult(requestCode, resultCode, dataIntent);
                break;
            case RC_BARCODE_CAPTURE:
                Intent intent;
                intent = new Intent(getApplicationContext(), BarcodeCaptureActivity.class);
                intent.putExtra(BarcodeCaptureActivity.AutoFocus, true);
                intent.putExtra(BarcodeCaptureActivity.UseFlash, false);

                startActivityForResult(intent, RC_BARCODE_CAPTURE);
                break;
            default:
                break;
        }
    }

    private void readSmartCard() {
        SmartCardIntentIntegrator SHRIntegrator = new SmartCardIntentIntegrator(this);
        SHRIntegrator.initiateCardRead();
        Toast.makeText(getApplicationContext(), "Opening Card Reader", Toast.LENGTH_LONG).show();
    }

    private void processSmartCardReadResult(int requestCode, int resultCode, Intent dataIntent) {
        SmartCardIntentResult cardReadIntentResult = null;

        try {
            cardReadIntentResult = SmartCardIntentIntegrator.parseActivityResult(requestCode, resultCode, dataIntent);
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "Could not get result", e);
        }
        if (cardReadIntentResult == null) {
            Toast.makeText(getApplicationContext(), "Card Read Failed", Toast.LENGTH_LONG).show();
            return;
        }

        if (cardReadIntentResult.isSuccessResult()) {
            smartCardRecord = cardReadIntentResult.getSmartCardRecord();
            if (smartCardRecord != null) {
                String SHRPayload = smartCardRecord.getPlainPayload();
                if (!SHRPayload.equals("") && !SHRPayload.isEmpty()) {
                    try {
                        SHRPatient = KenyaEmrShrMapper.extractPatientFromSHRModel(((MuzimaApplication) getApplicationContext()), SHRPayload);
                        if (SHRPatient != null) {
                            PatientIdentifier cardNumberIdentifier = SHRPatient.getIdentifier(Constants.Shr.KenyaEmr.PersonIdentifierType.CARD_SERIAL_NUMBER.name);

                            SHRToMuzimaMatchingPatient = null;

                            if (cardNumberIdentifier == null) {
                                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
                                alertBuilder.setMessage("Could not find Card Serial number in shared health record")
                                        .setCancelable(true).show();
                            } else {
                                Toast.makeText(getApplicationContext(), "Searching Patient Locally", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(getApplicationContext(), "This card seems to be blank", Toast.LENGTH_LONG).show();
                        }
                    } catch (KenyaEmrShrMapper.ShrParseException e) {
                        Log.e("EMR_IN", "EMR Error ", e);
                    }
                }
            }
        } else {
            Snackbar.make(findViewById(R.id.patient_lists_layout), getResources().getString(R.string.general_card_read_failed_msg) + cardReadIntentResult.getErrors(), Snackbar.LENGTH_LONG)
                    .setAction(getResources().getString(R.string.general_retry), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            readSmartCard();
                        }
                    }).show();
        }
    }

    private void navigateToClientsLocationMap() {
        Intent intent = new Intent(getApplicationContext(), PatientsLocationMapActivity.class);
        startActivity(intent);
    }

    @Override
    public void onUserInteraction() {
        ((MuzimaApplication) getApplication()).restartTimer();
        super.onUserInteraction();
    }

    @Override
    public void onCohortFilterClicked(int position) {
        List<CohortFilter> cfilter = new ArrayList<>(selectedCohortFilters);
        CohortFilter cohortFilter = cohortList.get(position);
        if (cohortFilter.getCohortWithFilter() == null) {
            if (cohortFilter.isSelected()) {
                cohortFilter.setSelected(false);
            } else {
                cohortFilter.setSelected(true);
                for (CohortFilter filter : cohortList) {
                    if (filter.getCohortWithFilter() != null) {
                        filter.setSelected(false);
                        for (CohortFilter cf : cfilter){
                            if(cf.getCohortWithFilter() != null) {
                                if (filter.getCohortWithFilter().getCohort().getUuid().equals(cf.getCohortWithFilter().getCohort().getUuid()) && filter.getCohortWithFilter().getDerivedObservationFilter().equals(cf.getCohortWithFilter().getDerivedObservationFilter())) {
                                    selectedCohortFilters.remove(cf);
                                }else if(filter.getCohortWithFilter().getCohort().getUuid().equals(cf.getCohortWithFilter().getCohort().getUuid()) && filter.getCohortWithFilter().getObservationFilter().equals(cf.getCohortWithFilter().getObservationFilter())){
                                    selectedCohortFilters.remove(cf);
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (cohortFilter.isSelected()) {
                for (CohortFilter cf : cfilter){
                    if(cf.getCohortWithFilter() != null && cohortFilter.getCohortWithFilter() != null) {
                        if (!cf.getCohortWithFilter().getDerivedObservationFilter().isEmpty() && cf.getCohortWithFilter().getCohort().getUuid().equals(cohortFilter.getCohortWithFilter().getCohort().getUuid()) && cf.getCohortWithFilter().getDerivedObservationFilter().equals(cohortFilter.getCohortWithFilter().getDerivedObservationFilter())) {
                            selectedCohortFilters.remove(cf);
                        }else if(!cf.getCohortWithFilter().getObservationFilter().isEmpty() && cf.getCohortWithFilter().getCohort().getUuid().equals(cohortFilter.getCohortWithFilter().getCohort().getUuid()) && cf.getCohortWithFilter().getObservationFilter().equals(cohortFilter.getCohortWithFilter().getObservationFilter())){
                            selectedCohortFilters.remove(cf);
                        }
                    }
                }
                cohortFilter.setSelected(false);
                markAllClientsCohortFilter(selectedCohortFilters.isEmpty());
            } else {
                cohortFilter.setSelected(true);
                for (CohortFilter cf : cfilter){
                    if(cf.getCohortWithFilter() == null) {
                        selectedCohortFilters.remove(cf);
                    }
                }
                selectedCohortFilters.add(cohortFilter);
                markAllClientsCohortFilter(false);
            }
        }

        cohortFilterAdapter.notifyDataSetChanged();
    }

    private void markAllClientsCohortFilter(boolean b) {
        for (CohortFilter filter : cohortList) {
            if (filter.getCohortWithFilter() == null || filter.getCohortWithFilter().getCohort() == null)
                filter.setSelected(b);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        languageUtil.onResume(this);
        showIncompleteWizardWarning();
        tagsListAdapter.reloadData();

        if(isDataSyncRunning() && refreshMenuActionView != null){
            refreshMenuActionView.startAnimation(refreshIconRotateAnimation);
        } else if(refreshMenuActionView != null){
            refreshMenuActionView.clearAnimation();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        showIncompleteWizardWarning();
        tagsListAdapter.reloadData();
    }


    private void showIncompleteWizardWarning() {
        if (!new WizardFinishPreferenceService(this).isWizardFinished()) {
            if (checkIfDisclaimerIsAccepted()) {
                Toast.makeText(getApplicationContext(), getString(R.string.error_wizard_interrupted), Toast.LENGTH_LONG)
                        .show();
            }
        }
    }

    private boolean checkIfDisclaimerIsAccepted() {
        String disclaimerKey = getResources().getString(R.string.preference_disclaimer);
        return MuzimaPreferences.getBooleanPreference(getApplicationContext(), disclaimerKey, false);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawers();
            return;
        }

        if(Objects.requireNonNull(navController.getCurrentDestination()).getId() == R.id.nav_home)
            showExitAlertDialog();
        else
            super.onBackPressed();
    }

    @Override
    protected void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        int syncStatus = intent.getIntExtra(Constants.DataSyncServiceConstants.SYNC_STATUS, Constants.DataSyncServiceConstants.SyncStatusConstants.UNKNOWN_ERROR);
        int syncType = intent.getIntExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, -1);
        switch (syncType) {
            case Constants.DataSyncServiceConstants.SYNC_UPLOAD_FORMS:
            case Constants.DataSyncServiceConstants.SYNC_REAL_TIME_UPLOAD_FORMS:
                if (syncStatus == Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS) {
                    EventBus.getDefault().post(new UploadedFormDataEvent());
                }
                break;
        }
    }

    private void showExitAlertDialog() {

        MuzimaSettingController muzimaSettingController = ((MuzimaApplication) getApplicationContext()).getMuzimaSettingController();
        boolean isOnlineOnlyModeEnabled = muzimaSettingController.isOnlineOnlyModeEnabled();

        FormController formController = ((MuzimaApplication) getApplicationContext()).getFormController();
        int incompleteForms = 0;
        int completeForms = 0;
        try {
            incompleteForms = formController.countAllIncompleteForms();
            completeForms = formController.countAllCompleteForms();
        } catch (FormController.FormFetchException e) {
            Log.e(getClass().getSimpleName(),"Not able to fetch forms");
        }

        String message = getResources().getString(R.string.warning_logout_confirm);
        if(isOnlineOnlyModeEnabled && (incompleteForms>0 || completeForms>0)){
            message = getResources().getString(R.string.warning_logout_confirm_with_form_deletion);
        }

        new AlertDialog.Builder(MainDashboardActivity.this)
                .setCancelable(true)
                .setIcon(ThemeUtils.getIconWarning(this))
                .setTitle(getResources().getString(R.string.title_logout_confirm))
                .setMessage(message)
                .setPositiveButton(getString(R.string.general_yes), exitApplication())
                .setNegativeButton(getString(R.string.general_no), null)
                .create()
                .show();
    }

    private Dialog.OnClickListener exitApplication() {
        return new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ((MuzimaApplication) getApplication()).logOut();
                launchLoginActivity();
            }
        };
    }

    private void launchLoginActivity() {
        Intent intent = new Intent(getApplication(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(LoginActivity.isFirstLaunch, false);
        intent.putExtra(LoginActivity.sessionTimeOut, false);
        getApplication().startActivity(intent);
    }

    @Override
    protected int getBottomNavigationMenuItemId() {
        return R.id.action_home;
    }

    private void initDrawer() {
        initSelectedTags();
        ListView tagsDrawerList = findViewById(R.id.tags_list);
        tagsDrawerList.setEmptyView(findViewById(R.id.tags_no_data_msg));
        tagsListAdapter = new PatientTagsListAdapter(this, R.layout.item_tags_list, ((MuzimaApplication) getApplicationContext()).getPatientController(), ((MuzimaApplication) getApplicationContext()).getMuzimaSettingController());
        tagsDrawerList.setAdapter(tagsListAdapter);
        tagsDrawerList.setOnItemClickListener(tagsListAdapter);
        ActionBarDrawerToggle actionbarDrawerToggle = new ActionBarDrawerToggle(this, mainLayout,
                R.drawable.ic_labels, R.string.hint_drawer_open, R.string.hint_drawer_close) {

            /**
             * Called when a drawer has settled in a completely closed state.
             */
            public void onDrawerClosed(View view) {
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                mainLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                EventBus.getDefault().post(new CohortFilterActionEvent(selectedCohortFilters));
            }

            /**
             * Called when a drawer has settled in a completely open state.
             */
            public void onDrawerOpened(View drawerView) {
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                mainLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            }
        };
        mainLayout.setDrawerListener(actionbarDrawerToggle);
        mainLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

    }

    private void initSelectedTags() {
        List<String> selectedTagsInPref = tagPreferenceService.getPatientSelectedTags();
        List<PatientTag> allTags = null;
        try {
            allTags = ((MuzimaApplication) getApplicationContext()).getPatientController().getAllTags();
        } catch (PatientController.PatientLoadException e) {
            Log.e(getClass().getSimpleName(), "Error occurred while get all tags from local repository", e);
        }
        List<PatientTag> selectedTags = new ArrayList<>();

        if (selectedTagsInPref != null) {
            for (PatientTag tag : allTags) {
                if (selectedTagsInPref.contains(tag.getName())) {
                    selectedTags.add(tag);
                }
            }
        }
        ((MuzimaApplication) getApplicationContext()).getPatientController().setSelectedTags(selectedTags);
    }
}
