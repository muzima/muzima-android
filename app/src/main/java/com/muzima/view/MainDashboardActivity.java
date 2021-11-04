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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentContainerView;
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
import com.muzima.api.model.Cohort;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PatientIdentifier;
import com.muzima.api.model.SmartCardRecord;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.domain.Credentials;
import com.muzima.model.CohortFilter;
import com.muzima.model.events.BottomSheetToggleEvent;
import com.muzima.model.events.CloseBottomSheetEvent;
import com.muzima.model.events.CohortFilterActionEvent;
import com.muzima.model.events.ShowCohortFilterEvent;
import com.muzima.model.events.UploadedFormDataEvent;
import com.muzima.scheduler.MuzimaJobScheduleBuilder;
import com.muzima.scheduler.RealTimeFormUploader;
import com.muzima.service.WizardFinishPreferenceService;
import com.muzima.tasks.LoadDownloadedCohortsTask;
import com.muzima.utils.Constants;
import com.muzima.utils.LanguageUtil;
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeUtils.getInstance().onCreate(MainDashboardActivity.this,false);
        languageUtil.onCreate(MainDashboardActivity.this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadBottomNavigation();
        RealTimeFormUploader.getInstance().uploadAllCompletedForms(getApplicationContext(), false);
        initializeResources();
        loadCohorts(false);
    }

    private void loadCohorts(final boolean showFilter) {
        ((MuzimaApplication) getApplicationContext()).getExecutorService()
                .execute(new LoadDownloadedCohortsTask(getApplicationContext(), new LoadDownloadedCohortsTask.OnDownloadedCohortsLoadedCallback() {
                    @Override
                    public void onCohortsLoaded(final List<Cohort> cohorts) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                cohortList.clear();
                                if (selectedCohortFilters.size()==0)
                                    cohortList.add(new CohortFilter(null, true));
                                else if(selectedCohortFilters.size()==1 && selectedCohortFilters.get(0).getCohort()==null)
                                    cohortList.add(new CohortFilter(null, true));
                                else
                                    cohortList.add(new CohortFilter(null, false));
                                for (Cohort cohort : cohorts) {
                                    boolean isCohortSeleted = false;
                                    for(CohortFilter cohortFilter : selectedCohortFilters){
                                        if(cohortFilter.getCohort() != null) {
                                            if (cohortFilter.getCohort().getUuid().equals(cohort.getUuid())) {
                                                isCohortSeleted = true;
                                            }
                                        }
                                    }
                                    cohortList.add(new CohortFilter(cohort, isCohortSeleted));
                                }
                                cohortFilterAdapter.notifyDataSetChanged();
                                if (showFilter)
                                    cohortFilterBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

                            }
                        });
                    }
                }));
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
    public void closeBottomSheetEvent(CloseBottomSheetEvent event) {
        cohortFilterBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    @Subscribe
    public void showCohortFilterEvent(ShowCohortFilterEvent event) {
        loadCohorts(true);
    }

    private void initializeResources() {
        Toolbar toolbar = findViewById(R.id.dashboard_toolbar);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                switch(item.getItemId()){
                    case R.id.menu_location:
                        navigateToClientsLocationMap();
                        break;
                    case R.id.menu_load:
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.info_muzima_sync_service_in_progress), Toast.LENGTH_LONG).show();
                        new MuzimaJobScheduleBuilder(getApplicationContext()).schedulePeriodicBackgroundJob(1000, true);
                }

                return true;
            }
        });
        ActionMenuItemView locationMenu = findViewById(R.id.menu_location);
        MuzimaSettingController muzimaSettingController = ((MuzimaApplication) getApplicationContext()).getMuzimaSettingController();
        boolean isGeomappingEnabled = muzimaSettingController.isGeoMappingEnabled();

        if(!isGeomappingEnabled)
            locationMenu.setVisibility(View.GONE);

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
                R.id.nav_home, R.id.nav_settings, R.id.nav_help, R.id.nav_feedback, R.id.nav_contact)
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
        if (((MuzimaApplication) getApplicationContext()).getAuthenticatedUser() != null)
            headerTitleTextView.setText(((MuzimaApplication) getApplicationContext()).getAuthenticatedUser().getUsername());
        setTitle(StringUtils.EMPTY);

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
//                                prepareRegisterLocallyDialog();
//                                prepareLocalSearchNotifyDialog(SHRPatient);
//                                executeLocalPatientSearchInBackgroundTask();
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
        if (cohortFilter.getCohort() == null) {
            if (cohortFilter.isSelected()) {
                cohortFilter.setSelected(false);
            } else {
                cohortFilter.setSelected(true);
                for (CohortFilter filter : cohortList) {
                    if (filter.getCohort() != null) {
                        filter.setSelected(false);
                        for (CohortFilter cf : cfilter){
                            if(cf.getCohort() != null) {
                                if (filter.getCohort().getUuid().equals(cf.getCohort().getUuid())) {
                                    selectedCohortFilters.remove(cf);
                                }
                            }
                        }
                    }
                }
                selectedCohortFilters.add(cohortFilter);
            }
        } else {
            if (cohortFilter.isSelected()) {
                for (CohortFilter cf : cfilter){
                    if(cf.getCohort() != null && cohortFilter.getCohort() != null) {
                        if (cf.getCohort().getUuid().equals(cohortFilter.getCohort().getUuid())) {
                            selectedCohortFilters.remove(cf);
                        }
                    }
                }
                cohortFilter.setSelected(false);
                markAllClientsCohortFilter(selectedCohortFilters.isEmpty());
            } else {
                cohortFilter.setSelected(true);
                for (CohortFilter cf : cfilter){
                    if(cf.getCohort() == null) {
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
            if (filter.getCohort() == null)
                filter.setSelected(b);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        languageUtil.onResume(this);
        showIncompleteWizardWarning();
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
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String disclaimerKey = getResources().getString(R.string.preference_disclaimer);
        return settings.getBoolean(disclaimerKey, false);
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
        new AlertDialog.Builder(MainDashboardActivity.this)
                .setCancelable(true)
                .setIcon(ThemeUtils.getIconWarning(this))
                .setTitle(getResources().getString(R.string.title_logout_confirm))
                .setMessage(getResources().getString(R.string.warning_logout_confirm))
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
}
