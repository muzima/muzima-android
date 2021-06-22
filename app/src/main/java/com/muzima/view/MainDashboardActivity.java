package com.muzima.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.navigation.NavigationView;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.MainDashboardAdapter;
import com.muzima.adapters.cohort.CohortFilterAdapter;
import com.muzima.api.model.Cohort;
import com.muzima.api.model.User;
import com.muzima.controller.CohortController;
import com.muzima.controller.FormController;
import com.muzima.controller.NotificationController;
import com.muzima.controller.PatientController;
import com.muzima.domain.Credentials;
import com.muzima.model.CohortFilter;
import com.muzima.model.events.BottomSheetToggleEvent;
import com.muzima.model.events.CohortFilterActionEvent;
import com.muzima.model.events.CohortsActionModeEvent;
import com.muzima.model.events.DestroyActionModeEvent;
import com.muzima.model.events.ShowCohortFilterEvent;
import com.muzima.scheduler.RealTimeFormUploader;
import com.muzima.service.WizardFinishPreferenceService;
import com.muzima.tasks.DownloadCohortsTask;
import com.muzima.tasks.LoadDownloadedCohortsTask;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.preferences.SettingsActivity;

import org.apache.lucene.queryParser.ParseException;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.muzima.utils.Constants.NotificationStatusConstants.NOTIFICATION_UNREAD;

public class MainDashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, CohortFilterAdapter.CohortFilterClickedListener {
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MaterialToolbar toolbar;
    private ViewPager viewPager;
    private TextView headerTitleTextView;
    private MainDashboardAdapter adapter;
    private BottomNavigationView bottomNavigationView;
    private ActionBarDrawerToggle drawerToggle;
    private final ThemeUtils themeUtils = new ThemeUtils();
    private final LanguageUtil languageUtil = new LanguageUtil();
    private MenuItem menuLocation;
    private MenuItem menuRefresh;
    private ActionMode.Callback actionModeCallback;
    private ActionMode actionMode;
    private Credentials credentials;
    private BackgroundQueryTask mBackgroundQueryTask;
    private MenuItem loadingMenuItem;
    private BottomSheetBehavior bottomSheetBehavior;
    private View bottomSheetView;
    private View closeBottomSheet;
    private CohortFilterAdapter cohortFilterAdapter;
    private RecyclerView filterOptionsRecyclerView;
    private List<Cohort> selectedCohorts = new ArrayList<>();
    private List<CohortFilter> cohortList = new ArrayList<>();
    private List<CohortFilter> selectedCohortFilters = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        themeUtils.onCreate(MainDashboardActivity.this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_root_layout);
        RealTimeFormUploader.getInstance().uploadAllCompletedForms(getApplicationContext(), false);
        initializeResources();
        loadCohorts();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_dashboard_home, menu);
        menuLocation = menu.findItem(R.id.menu_location);
        menuRefresh = menu.findItem(R.id.menu_load);
        return true;
    }

    private void loadCohorts() {
        ((MuzimaApplication) getApplicationContext()).getExecutorService()
                .execute(new LoadDownloadedCohortsTask(getApplicationContext(), new LoadDownloadedCohortsTask.OnDownloadedCohortsLoadedCallback() {
                    @Override
                    public void onCohortsLoaded(final List<Cohort> cohorts) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                cohortList.clear();
                                cohortList.add(new CohortFilter(null, true));
                                for (Cohort cohort : cohorts) {
                                    cohortList.add(new CohortFilter(cohort, false));
                                }
                                cohortFilterAdapter.notifyDataSetChanged();
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
    public void showCohortFilterEvent(ShowCohortFilterEvent event) {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Subscribe
    public void onCohortDownloadActionModeEvent(CohortsActionModeEvent actionModeEvent) {
        selectedCohorts = actionModeEvent.getSelectedCohorts();
        initActionMode();
    }

    private void initActionMode() {
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
                    loadingMenuItem.setActionView(new ProgressBar(MainDashboardActivity.this));
                    loadingMenuItem.setVisible(true);
                    menuItem.setVisible(false);
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.info_muzima_sync_service_in_progress), Toast.LENGTH_LONG).show();
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
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
            }
        };

        actionMode = startActionMode(actionModeCallback);
        actionMode.setTitle(String.format(Locale.getDefault(), "%d %s", selectedCohorts.size(), getResources().getString(R.string.general_selected)));
    }

    public void hideProgressbar() {
        menuRefresh.setActionView(null);
    }

    public void showProgressBar() {
        menuRefresh.setActionView(R.layout.refresh_menuitem);
    }

    private void initializeResources() {
        viewPager = findViewById(R.id.main_dashboard_view_pager);
        bottomNavigationView = findViewById(R.id.main_dashboard_bottom_navigation);
        toolbar = findViewById(R.id.dashboard_toolbar);
        drawerLayout = findViewById(R.id.main_dashboard_drawer_layout);
        navigationView = findViewById(R.id.dashboard_navigation);
        bottomSheetView = findViewById(R.id.dashboard_home_bottom_view_container);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView);
        closeBottomSheet = findViewById(R.id.bottom_sheet_close_view);
        filterOptionsRecyclerView = findViewById(R.id.dashboard_home_filter_recycler_view);
        cohortFilterAdapter = new CohortFilterAdapter(getApplicationContext(), cohortList, this);
        filterOptionsRecyclerView.setAdapter(cohortFilterAdapter);
        filterOptionsRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        headerTitleTextView = navigationView.getHeaderView(0).findViewById(R.id.dashboard_header_title_text_view);

        setSupportActionBar(toolbar);
        drawerToggle = new ActionBarDrawerToggle(MainDashboardActivity.this, drawerLayout,
                toolbar, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
        adapter = new MainDashboardAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);
        credentials = new Credentials(this);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                menuItem.setChecked(true);
                menuItem.setEnabled(true);
                if (menuItem.getItemId() == R.id.main_dashboard_home_menu) {
                    viewPager.setCurrentItem(0);
                    if (menuLocation != null)
                        menuLocation.setVisible(true);
                } else if (menuItem.getItemId() == R.id.main_dashboard_cohorts_menu) {
                    viewPager.setCurrentItem(1);
                    if (menuLocation != null)
                        menuLocation.setVisible(false);
                } else if (menuItem.getItemId() == R.id.main_dashboard_forms_menu) {
                    viewPager.setCurrentItem(2);
                    if (menuLocation != null)
                        menuLocation.setVisible(false);
                }
                return false;
            }
        });

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (position == viewPager.getCurrentItem()) return;
                switch (position) {
                    case 0:
                        bottomNavigationView.setSelectedItemId(R.id.main_dashboard_cohorts_menu);
                        break;
                    case 1:
                        bottomNavigationView.setSelectedItemId(R.id.main_dashboard_forms_menu);
                        break;
                    case 2:
                        bottomNavigationView.setSelectedItemId(R.id.main_dashboard_forms_menu);
                    default:
                        break;
                }
            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        closeBottomSheet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    if (!selectedCohortFilters.isEmpty()) {
                        List<CohortFilter> updatedList = unselectAllFilters(cohortList);
                        cohortList.clear();
                        cohortList.addAll(updatedList);
                        cohortFilterAdapter.notifyDataSetChanged();
                        EventBus.getDefault().post(new CohortFilterActionEvent(selectedCohortFilters, false));
                    }
                } else if (newState == BottomSheetBehavior.STATE_EXPANDED)
                    selectedCohortFilters.clear();
                EventBus.getDefault().post(new BottomSheetToggleEvent(newState));
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        headerTitleTextView.setText(((MuzimaApplication) getApplicationContext()).getAuthenticatedUser().getUsername());

        setTitle(" ");

    }

    @Override
    public void onUserInteraction() {
        ((MuzimaApplication) getApplication()).restartTimer();
        super.onUserInteraction();
    }

    @Override
    public void onCohortFilterClicked(int position) {
        CohortFilter cohortFilter = cohortList.get(position);
        if (cohortFilter.isSelected()) {
            cohortFilter.setSelected(false);
            selectedCohortFilters.remove(cohortFilter);
        } else {
            cohortFilter.setSelected(true);
            selectedCohortFilters.add(cohortFilter);
        }
        cohortFilterAdapter.notifyDataSetChanged();
    }

    private List<CohortFilter> unselectAllFilters(List<CohortFilter> cohortList) {
        List<CohortFilter> filters = new ArrayList<>();
        for (CohortFilter cohortFilter : cohortList) {
            cohortFilter.setSelected(false);
            filters.add(cohortFilter);
        }
        return filters;
    }

    @Override
    protected void onResume() {
        super.onResume();
        themeUtils.onResume(this);
        languageUtil.onResume(this);
        showIncompleteWizardWarning();
        executeBackgroundTask();
    }

    private void executeBackgroundTask() {
        mBackgroundQueryTask = new BackgroundQueryTask();
        mBackgroundQueryTask.execute();
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
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        Intent intent;

        if (menuItem.getItemId() == R.id.drawer_menu_home) {
            intent = new Intent(getApplicationContext(), MainDashboardActivity.class);
            startActivity(intent);
            finish();
        } else if (menuItem.getItemId() == R.id.drawer_menu_settings) {
            intent = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(intent);
            finish();
        } else if (menuItem.getItemId() == R.id.drawer_menu_help) {
            intent = new Intent(getApplicationContext(), HelpActivity.class);
            startActivity(intent);
            finish();
        } else if (menuItem.getItemId() == R.id.drawer_menu_feedback) {
            intent = new Intent(getApplicationContext(), FeedbackActivity.class);
            startActivity(intent);
            finish();
        } else if (menuItem.getItemId() == R.id.drawer_menu_contact_us) {
            intent = new Intent(getApplicationContext(), FeedbackActivity.class);
            startActivity(intent);
            finish();
        } else if (menuItem.getItemId() == R.id.drawer_menu_logout) {
            finishAffinity();
        }
        return false;
    }

    private static class HomeActivityMetadata {
        int totalCohorts;
        int syncedCohorts;
        int syncedPatients;
        int incompleteForms;
        int completeAndUnsyncedForms;
        int newNotifications;
        int totalNotifications;
        boolean isCohortUpdateAvailable;
    }

    class BackgroundQueryTask extends AsyncTask<Void, Void, HomeActivityMetadata> {

        @Override
        protected HomeActivityMetadata doInBackground(Void... voids) {
            MuzimaApplication muzimaApplication = (MuzimaApplication) getApplication();
            HomeActivityMetadata homeActivityMetadata = new HomeActivityMetadata();
            CohortController cohortController = muzimaApplication.getCohortController();
            PatientController patientController = muzimaApplication.getPatientController();
            FormController formController = muzimaApplication.getFormController();
            NotificationController notificationController = muzimaApplication.getNotificationController();
            try {
                homeActivityMetadata.totalCohorts = cohortController.countAllCohorts();
                homeActivityMetadata.syncedCohorts = cohortController.countSyncedCohorts();
                homeActivityMetadata.isCohortUpdateAvailable = cohortController.isUpdateAvailable();
                homeActivityMetadata.syncedPatients = patientController.countAllPatients();
                homeActivityMetadata.incompleteForms = formController.countAllIncompleteForms();
                homeActivityMetadata.completeAndUnsyncedForms = formController.countAllCompleteForms();

                // Notifications
                User authenticatedUser = ((MuzimaApplication) getApplicationContext()).getAuthenticatedUser();
                if (authenticatedUser != null) {
                    homeActivityMetadata.newNotifications = notificationController
                            .getAllNotificationsByReceiverCount(authenticatedUser.getPerson().getUuid(), NOTIFICATION_UNREAD);
                    homeActivityMetadata.totalNotifications = notificationController
                            .getAllNotificationsByReceiverCount(authenticatedUser.getPerson().getUuid(), null);
                } else {
                    homeActivityMetadata.newNotifications = 0;
                    homeActivityMetadata.totalNotifications = 0;
                }
            } catch (CohortController.CohortFetchException e) {
                Log.w(getClass().getSimpleName(), "CohortFetchException occurred while fetching metadata in MainActivityBackgroundTask", e);
            } catch (PatientController.PatientLoadException e) {
                Log.w(getClass().getSimpleName(), "PatientLoadException occurred while fetching metadata in MainActivityBackgroundTask", e);
            } catch (FormController.FormFetchException e) {
                Log.w(getClass().getSimpleName(), "FormFetchException occurred while fetching metadata in MainActivityBackgroundTask", e);
            } catch (NotificationController.NotificationFetchException e) {
                Log.w(getClass().getSimpleName(), "NotificationFetchException occurred while fetching metadata in MainActivityBackgroundTask", e);
            } catch (ParseException e) {
                Log.w(getClass().getSimpleName(), "ParseException occurred while fetching metadata in MainActivityBackgroundTask", e);
            }
            return homeActivityMetadata;
        }

        @Override
        protected void onPostExecute(HomeActivityMetadata homeActivityMetadata) {
//            ImageView cortUpdateAvailable = (ImageView) findViewById(R.id.pendingUpdateImg);
//            if (homeActivityMetadata.isCohortUpdateAvailable) {
//                cortUpdateAvailable.setVisibility(View.VISIBLE);
//            } else {
//                cortUpdateAvailable.setVisibility(View.GONE);
//            }
//
//            TextView patientDescriptionView = findViewById(R.id.patientDescription);
//            patientDescriptionView.setText(getString(R.string.hint_dashboard_clients_description,
//                    homeActivityMetadata.syncedPatients));
//
//            TextView formsDescription = findViewById(R.id.formDescription);
//            formsDescription.setText(getString(R.string.hint_dashboard_forms_description,
//                    homeActivityMetadata.incompleteForms, homeActivityMetadata.completeAndUnsyncedForms));
//
//            TextView notificationsDescription = findViewById(R.id.notificationDescription);
//            notificationsDescription.setText(getString(R.string.hint_dashboard_notifications_description,
//                    homeActivityMetadata.newNotifications, homeActivityMetadata.totalNotifications));

//            TextView currentUser = findViewById(R.id.currentUser);
//            currentUser.setText(getResources().getString(R.string.general_welcome) + " " + credentials.getUserName());
        }
    }
}
