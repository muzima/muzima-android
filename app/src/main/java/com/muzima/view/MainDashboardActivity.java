package com.muzima.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.MainDashboardAdapter;
import com.muzima.api.model.User;
import com.muzima.controller.CohortController;
import com.muzima.controller.FormController;
import com.muzima.controller.NotificationController;
import com.muzima.controller.PatientController;
import com.muzima.domain.Credentials;
import com.muzima.scheduler.RealTimeFormUploader;
import com.muzima.service.WizardFinishPreferenceService;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.patients.PatientsListActivity;
import com.muzima.view.preferences.SettingsActivity;

import org.apache.lucene.queryParser.ParseException;

import static com.muzima.utils.Constants.NotificationStatusConstants.NOTIFICATION_UNREAD;

public class MainDashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private ViewPager viewPager;
    private TextView headerTitleTextView;
    private MainDashboardAdapter adapter;
    private BottomNavigationView bottomNavigationView;
    private ActionBarDrawerToggle drawerToggle;
    private final ThemeUtils themeUtils = new ThemeUtils();
    private final LanguageUtil languageUtil = new LanguageUtil();
    private MenuItem menuItemRegisterPatient;
    private Credentials credentials;
    private BackgroundQueryTask mBackgroundQueryTask;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        themeUtils.onCreate(MainDashboardActivity.this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard_main_layout);
        credentials = new Credentials(this);
        RealTimeFormUploader.getInstance().uploadAllCompletedForms(getApplicationContext(), false);
        initializeResources();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.register_patient_menu, menu);
        menuItemRegisterPatient = menu.findItem(R.id.menu_client_add_icon);
        menuItemRegisterPatient.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                Intent intent = new Intent(getApplicationContext(), PatientsListActivity.class);
                startActivity(intent);
                finish();
                return false;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    public void hideProgressbar() {
        menuItemRegisterPatient.setActionView(null);
    }

    public void showProgressBar() {
        menuItemRegisterPatient.setActionView(R.layout.refresh_menuitem);
    }

    private void initializeResources() {
        viewPager = findViewById(R.id.main_dashboard_view_pager);
        bottomNavigationView = findViewById(R.id.main_dashboard_bottom_navigation);
        toolbar = findViewById(R.id.dashboard_toolbar);
        drawerLayout = findViewById(R.id.main_dashboard_drawer_layout);
        navigationView = findViewById(R.id.dashboard_navigation);
        headerTitleTextView = navigationView.getHeaderView(0).findViewById(R.id.dashboard_header_title_text_view);

        setSupportActionBar(toolbar);
        drawerToggle = new ActionBarDrawerToggle(MainDashboardActivity.this, drawerLayout,
                toolbar, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
        adapter = new MainDashboardAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                menuItem.setChecked(true);
                menuItem.setEnabled(true);
                if (menuItem.getItemId() == R.id.main_dashboard_home_menu) {
                    viewPager.setCurrentItem(0);
                } else if (menuItem.getItemId() == R.id.main_dashboard_cohorts_menu) {
                    viewPager.setCurrentItem(1);
                } else if (menuItem.getItemId() == R.id.main_dashboard_forms_menu) {
                    viewPager.setCurrentItem(2);
                }
                return false;
            }
        });

        headerTitleTextView.setText(((MuzimaApplication) getApplicationContext()).getAuthenticatedUser().getUsername());

        setTitle(" ");

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

    @Override
    protected void onStart() {
        super.onStart();
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
