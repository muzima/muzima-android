/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.login;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.muzima.BuildConfig;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.context.Context;
import com.muzima.api.model.MinimumSupportedAppVersion;
import com.muzima.api.model.MuzimaCoreModuleVersion;
import com.muzima.controller.MinimumSupportedAppVersionController;
import com.muzima.controller.MuzimaCoreModuleVersionController;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.domain.Credentials;
import com.muzima.scheduler.MuzimaJobScheduleBuilder;
import com.muzima.service.CredentialsPreferenceService;
import com.muzima.service.LandingPagePreferenceService;
import com.muzima.service.LocalePreferenceService;
import com.muzima.service.MuzimaGPSLocationService;
import com.muzima.service.MuzimaLoggerService;
import com.muzima.service.MuzimaSyncService;
import com.muzima.service.WizardFinishPreferenceService;
import com.muzima.util.Constants;
import com.muzima.util.NetworkUtils;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.StringUtils;
import com.muzima.utils.SyncSettingsIntent;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.HelpActivity;
import com.muzima.view.setupconfiguration.SetupMethodPreferenceWizardActivity;

import java.util.Locale;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants;

//This class shouldn't extend BaseActivity. Since it is independent of the application's context
public class LoginActivity extends Activity {
    public static final String isFirstLaunch = "isFirstLaunch";
    public static final String sessionTimeOut = "SessionTimeOut";
    MuzimaGPSLocationService gpsLocationService;
    private EditText serverUrlText;
    private EditText usernameText;
    private EditText passwordText;
    private Button loginButton;
    private CheckBox updatePassword;
    private TextView versionText;
    private BackgroundAuthenticationTask backgroundAuthenticationTask;
    private TextView authenticatingText;
    private TextView helpText;

    private ValueAnimator flipFromLoginToAuthAnimator;
    private ValueAnimator flipFromAuthToLoginAnimator;
    private boolean isUpdatePasswordChecked;
    private ThemeUtils themeUtils = new ThemeUtils(R.style.LoginTheme_Light, R.style.LoginTheme_Dark);
    private final LanguageUtil languageUtil = new LanguageUtil();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeUtils.onCreate(this);
        languageUtil.onCreate(this);
        super.onCreate(savedInstanceState);
        ((MuzimaApplication) getApplication()).cancelTimer();
        setContentView(R.layout.activity_login);
        showSessionTimeOutPopUpIfNeeded();

        initViews();
        setupListeners();
        initAnimators();

        boolean isFirstLaunch = getIntent().getBooleanExtra(LoginActivity.isFirstLaunch, true);
        String serverURL = getServerURL();
        if (!isFirstLaunch && !StringUtils.isEmpty(serverURL)) {
            removeServerUrlAsInput();
        }

        useSavedServerUrl(serverURL);

        if(isFirstLaunch){
            removeChangedPasswordRecentlyCheckbox();
        }

        //Hack to get it to use default font space.
        passwordText.setTypeface(Typeface.DEFAULT);
        versionText.setText(getApplicationVersion());
        usernameText.requestFocus();
        initializeGPSDataCollection();
    }

    private void showSessionTimeOutPopUpIfNeeded() {
        if (getIntent().getBooleanExtra(LoginActivity.sessionTimeOut, false)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder
                    .setCancelable(true)
                    .setIcon(ThemeUtils.getIconWarning(this))
                    .setTitle(getResources().getString(R.string.general_alert))
                    .setMessage(getResources().getString(R.string.info_session_time_out))
                    .setPositiveButton(R.string.general_ok, null).show();
        }
    }

    private void removeServerUrlAsInput() {
        serverUrlText.setVisibility(View.GONE);
        findViewById(R.id.server_url_divider).setVisibility(View.GONE);
    }

    private void useSavedServerUrl(String serverUrl) {
        if (!StringUtils.isEmpty(serverUrl)) {
            serverUrlText.setText(serverUrl);
        }
    }

    private void removeChangedPasswordRecentlyCheckbox() {
        updatePassword.setVisibility(View.GONE);
    }

    private String getApplicationVersion() {
        String versionText = "";
        String versionCode = "";
        try {
            versionCode = String.valueOf(getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
            LanguageUtil languageUtil = new LanguageUtil();
            android.content.Context localizedContext = languageUtil.getLocalizedContext(LoginActivity.this.getApplication());
            versionText = localizedContext.getResources().getString(R.string.general_application_version, versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(getClass().getSimpleName(), "Unable to read application version.", e);
        }
        return versionText;
    }

    private String getServerURL() {
        Credentials credentials;
        credentials = new Credentials(this);
        return credentials.getServerUrl();
    }

    @Override
    public void onResume() {
        super.onResume();
        themeUtils.onResume(this);
        languageUtil.onCreate(this);
        setupStatusView();
    }

    private void setupStatusView() {
        if (backgroundAuthenticationTask != null && backgroundAuthenticationTask.getStatus() == AsyncTask.Status.RUNNING) {
            loginButton.setVisibility(View.GONE);
            authenticatingText.setVisibility(View.VISIBLE);
        } else {
            loginButton.setVisibility(View.VISIBLE);
            authenticatingText.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (backgroundAuthenticationTask != null) {
            backgroundAuthenticationTask.cancel(true);
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    private void setupListeners() {
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validInput()) {
                    if (backgroundAuthenticationTask != null && backgroundAuthenticationTask.getStatus() == AsyncTask.Status.RUNNING) {
                        Toast.makeText(getApplicationContext(), getString(R.string.info_authentication_in_progress), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String username = (usernameText.getText() == null) ? "" : usernameText.getText().toString().trim();
                    //not trimming passwords since passwords may contain space.
                    String password = (passwordText.getText() == null) ? "" : passwordText.getText().toString();

                    backgroundAuthenticationTask = new BackgroundAuthenticationTask();
                    backgroundAuthenticationTask.execute(
                            new Credentials(serverUrlText.getText().toString(), username, password)
                    );
                } else {
                    int errorColor = getResources().getColor(R.color.error_text_color);
                    if (StringUtils.isEmpty(serverUrlText.getText().toString())) {
                        serverUrlText.setHint(getString(R.string.hint_server_url_prompt));
                        serverUrlText.setHintTextColor(errorColor);
                    }

                    if (StringUtils.isEmpty(usernameText.getText().toString())) {
                        usernameText.setHint(getString(R.string.hint_username_prompt));
                        usernameText.setHintTextColor(errorColor);
                    }

                    if (StringUtils.isEmpty(passwordText.getText().toString())) {
                        passwordText.setHint(getString(R.string.hint_password_prompt));
                        passwordText.setHintTextColor(errorColor);
                    }
                }
            }
        });

        helpText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent helpIntent = new Intent(getApplicationContext(), HelpActivity.class);
                startActivity(helpIntent);
            }
        });
    }

    private boolean validInput() {
        return !(StringUtils.isEmpty(serverUrlText.getText().toString())
                || StringUtils.isEmpty(usernameText.getText().toString())
                || StringUtils.isEmpty(passwordText.getText().toString()));
    }

    private void initViews() {
        serverUrlText = findViewById(R.id.serverUrl);
        usernameText = findViewById(R.id.username);
        passwordText = findViewById(R.id.password);
        updatePassword = findViewById(R.id.update_password);
        loginButton = findViewById(R.id.login);
        authenticatingText = findViewById(R.id.authenticatingText);
        versionText = findViewById(R.id.version);
        helpText = findViewById(R.id.helpText);
    }

    public void onUpdatePasswordCheckboxClicked(View view) {
        isUpdatePasswordChecked = ((CheckBox) view).isChecked();
    }

    private void removeRemnantDataFromPreviousRunOfWizard() {
        if (!new WizardFinishPreferenceService(this).isWizardFinished()) {
            try {
                MuzimaApplication application = ((MuzimaApplication) getApplicationContext());
                Context context = application.getMuzimaContext();

                //Cohort Wizard activity
                application.getPatientController().deleteAllPatients();
                application.getCohortController().deleteAllCohortMembers(application.getCohortController().getAllCohorts());
                application.getCohortController().deleteAllCohorts();
                context.getLastSyncTimeService().deleteAll();

                //FormTemplateWizardActivity
                application.getConceptController().deleteAllConcepts();
                application.getLocationController().deleteAllLocations();
                application.getProviderController().deleteAllProviders();
                application.getFormController().deleteAllForms();
                application.getFormController().deleteAllFormTemplates();

                //CustomConceptWizardActivity
                context.getObservationService().deleteAll();
                context.getEncounterService().deleteAll();
            } catch (Throwable e) {
                Log.e(getClass().getSimpleName(), "Unable to delete previous wizard run data. Error: " + e);
            }
        }
    }

    private void initializeGPSDataCollection(){
        gpsLocationService = ((MuzimaApplication)getApplicationContext()).getMuzimaGPSLocationService();

        if(gpsLocationService.isGPSLocationFeatureEnabled()) {
            if (!gpsLocationService.isGPSLocationPermissionsGranted()) {
                gpsLocationService.requestGPSLocationPermissions(this);
            }

            if (!gpsLocationService.isLocationServicesSwitchedOn()) {
                gpsLocationService.requestSwitchOnLocation(this);
            }
        }
    }
    
    private class BackgroundAuthenticationTask extends AsyncTask<Credentials, Void, BackgroundAuthenticationTask.Result> {

        @Override
        protected void onPreExecute() {
            if (loginButton.getVisibility() == View.VISIBLE) {
                flipFromLoginToAuthAnimator.start();
            }
        }

        @Override
        protected Result doInBackground(Credentials... params) {
            Credentials credentials = params[0];
            MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplication()).getMuzimaSyncService();
            int authenticationStatus = muzimaSyncService.authenticate(credentials.getCredentialsArray(), isUpdatePasswordChecked);
            return new Result(credentials, authenticationStatus);
        }

        @Override
        protected void onPostExecute(Result result) {
            MuzimaApplication muzimaApplication = (MuzimaApplication)getApplicationContext();
            if (result.status == SyncStatusConstants.AUTHENTICATION_SUCCESS) {
                MuzimaLoggerService.scheduleLogSync(muzimaApplication);
                MuzimaLoggerService.log(muzimaApplication,"LOGIN_SUCCESS",
                        result.credentials.getUserName(),MuzimaLoggerService.getAndParseGPSLocationForLogging(muzimaApplication), "{}");
                new CredentialsPreferenceService(getApplicationContext()).saveCredentials(result.credentials);
                ((MuzimaApplication) getApplication()).restartTimer();
                LocalePreferenceService localePreferenceService = ((MuzimaApplication) getApplication()).getLocalePreferenceService();

                String languageKey = getApplicationContext().getResources().getString(R.string.preference_app_language);
                String defaultLanguage = getApplicationContext().getString(R.string.language_english);
                String preferredLocale = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(languageKey,defaultLanguage);

                localePreferenceService.setPreferredLocale(preferredLocale);

                MuzimaJobScheduleBuilder muzimaJobScheduleBuilder = new MuzimaJobScheduleBuilder(getApplicationContext());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    //delay for 10 seconds to allow next UI activity to finish loading
                    muzimaJobScheduleBuilder.schedulePeriodicBackgroundJob(10000,false);
                }
                checkMuzimaCoreModuleCompatibility(result);
            } else {
                MuzimaLoggerService.log((MuzimaApplication)getApplicationContext(),"LOGIN_FAILURE",
                        result.credentials.getUserName(),MuzimaLoggerService.getAndParseGPSLocationForLogging((MuzimaApplication)getApplicationContext()),"{}");
                Toast.makeText(getApplicationContext(), getErrorText(result), Toast.LENGTH_SHORT).show();
                if (authenticatingText.getVisibility() == View.VISIBLE || flipFromLoginToAuthAnimator.isRunning()) {
                    flipFromLoginToAuthAnimator.cancel();
                    flipFromAuthToLoginAnimator.start();
                }
            }
        }

        private String getErrorText(Result result) {
            switch (result.status) {
                case SyncStatusConstants.MALFORMED_URL_ERROR:
                    return getString(R.string.error_server_url_invalid);
                case SyncStatusConstants.INVALID_CREDENTIALS_ERROR:
                    return getString(R.string.error_credential_invalid);
                case SyncStatusConstants.INVALID_CHARACTER_IN_USERNAME:
                    return getString(R.string.error_username_invalid_format) + SyncStatusConstants.INVALID_CHARACTER_FOR_USERNAME;
                case SyncStatusConstants.LOCAL_CONNECTION_ERROR:
                    return getString(R.string.error_local_connection_unavailable);
                case SyncStatusConstants.SERVER_CONNECTION_ERROR:
                    return getString(R.string.error_server_connection_unavailable);
                case SyncStatusConstants.UNKNOWN_ERROR:
                    return getString(R.string.error_authentication_fail);
                default:
                    return getString(R.string.error_authentication_fail);
            }
        }

        private void checkMuzimaCoreModuleCompatibility(Result result){
            new DownloadMuzimaAppVersionCodeBackGroundTask().execute(result.credentials.getServerUrl());
        }


        protected class Result {
            final Credentials credentials;
            final int status;

            private Result(Credentials credentials, int status) {
                this.credentials = credentials;
                this.status = status;
            }
        }
    }

    private void initAnimators() {
        ValueAnimator flipFromLoginToNoConnAnimator = ValueAnimator.ofFloat(0, 1);
        ValueAnimator flipFromNoConnToLoginAnimator = ValueAnimator.ofFloat(0, 1);
        flipFromLoginToAuthAnimator = ValueAnimator.ofFloat(0, 1);
        flipFromAuthToLoginAnimator = ValueAnimator.ofFloat(0, 1);
        ValueAnimator flipFromAuthToNoConnAnimator = ValueAnimator.ofFloat(0, 1);

        initFlipAnimation(flipFromLoginToAuthAnimator, loginButton, authenticatingText);
        initFlipAnimation(flipFromAuthToLoginAnimator, authenticatingText, loginButton);
    }

    private void initFlipAnimation(ValueAnimator valueAnimator, final View from, final View to) {
        valueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        valueAnimator.setDuration(0);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animatedFraction = animation.getAnimatedFraction();

                if (from.getVisibility() == View.VISIBLE) {
                    if (animatedFraction > 0.5) {
                        from.setVisibility(View.INVISIBLE);
                        to.setVisibility(View.VISIBLE);
                    }
                } else if (to.getVisibility() == View.VISIBLE) {
                    to.setRotationX(-180 * (1 - animatedFraction));
                }

                if (from.getVisibility() == View.VISIBLE) {
                    from.setRotationX(180 * animatedFraction);
                }
            }
        });
    }

    private class DownloadMuzimaAppVersionCodeBackGroundTask extends AsyncTask<String, Void,String > {
        @Override
        public String doInBackground(String... params){
            String serverUrl = params[0];
            MinimumSupportedAppVersionController minimumSupportedAppVersionController = ((MuzimaApplication) getApplication()).getMinimumSupportedVersionController();
            try {
                if(NetworkUtils.isAddressReachable(serverUrl, Constants.CONNECTION_TIMEOUT)) {
                    MinimumSupportedAppVersion localMinimumSupportedAppVersion = minimumSupportedAppVersionController.getMinimumSupportedAppVersion();
                    MinimumSupportedAppVersion downloadedMinimumSupportedAppVersion = minimumSupportedAppVersionController.downloadMinimumSupportedAppVersion();
                    if(downloadedMinimumSupportedAppVersion != null) {
                        if (localMinimumSupportedAppVersion.getVersion() != null) {
                            minimumSupportedAppVersionController.updateMinimumSupportedAppVersion(downloadedMinimumSupportedAppVersion);
                        } else {
                            minimumSupportedAppVersionController.saveMinimumSupportedAppVersion(downloadedMinimumSupportedAppVersion);
                        }
                    }
                }
            } catch (MinimumSupportedAppVersionController.MinimumSupportedAppVersionDownloadException e) {
                Log.e(getClass().getSimpleName(),"Encountered an exception while downloading supported app version ",e);
            } catch (MinimumSupportedAppVersionController.MinimumSupportedAppVersionFetchException e) {
                Log.e(getClass().getSimpleName(),"Encountered an exception while fetching/retrieving supported app version ",e);
            } catch (MinimumSupportedAppVersionController.MinimumSupportedAppVersionSaveException e) {
                Log.e(getClass().getSimpleName(),"Encountered an exception while saving supported app version ",e);
            }
            return serverUrl;
        }

        @Override
        protected void onPostExecute(String serverUrl) {
            MinimumSupportedAppVersion currentMinimumSupportedAppVersion;
            MinimumSupportedAppVersionController minimumSupportedAppVersionController = ((MuzimaApplication) getApplication()).getMinimumSupportedVersionController();
            try {
                currentMinimumSupportedAppVersion = minimumSupportedAppVersionController.getMinimumSupportedAppVersion();
                if(currentMinimumSupportedAppVersion == null || currentMinimumSupportedAppVersion.getVersion() == null){
                    showAlertDialog();
                }else {
                    int version = currentMinimumSupportedAppVersion.getVersion();
                    int appVersionCode = BuildConfig.VERSION_CODE;
                    try{
                        if (appVersionCode < version) {
                            showAlertDialog();
                        } else {
                            startNextActivity();
                        }
                    }catch (NumberFormatException e){
                        Log.e(getClass().getSimpleName(),"Encountered an exception while parsing string to integer ",e);
                        showAlertDialog();
                    }
                }
            } catch (MinimumSupportedAppVersionController.MinimumSupportedAppVersionFetchException e) {
                Log.e(getClass().getSimpleName(),"Encountered an exception while fetching/retrieving supported app version ",e);
            }
        }

        private void startNextActivity(){
            Intent intent;
            if (new WizardFinishPreferenceService(LoginActivity.this).isWizardFinished()) {
                downloadMissingServerSettings();
                intent = new LandingPagePreferenceService(getApplicationContext()).getLandingPageActivityLauchIntent();
            } else {
                removeRemnantDataFromPreviousRunOfWizard();
                intent = new Intent(getApplicationContext(), SetupMethodPreferenceWizardActivity.class);
            }
            startActivity(intent);
            finish();
        }

        private void downloadMissingServerSettings(){
            try {
                boolean isSettingsDownloadNeeded = !((MuzimaApplication) getApplication()).getMuzimaSettingController()
                        .isAllMandatorySettingsDownloaded();
                if (isSettingsDownloadNeeded) {
                    new SyncSettingsIntent(getApplicationContext()).start();
                }
            } catch (MuzimaSettingController.MuzimaSettingFetchException e){
                Log.e(getClass().getSimpleName(),""+e.getMessage());
            }
        }

        private void showAlertDialog() {
            new AlertDialog.Builder(LoginActivity.this)
                    .setCancelable(true)
                    .setIcon(ThemeUtils.getIconWarning(LoginActivity.this))
                    .setTitle(getResources().getString(R.string.general_caution))
                    .setMessage(getResources().getString(R.string.warning_incompatible_module))
                    .setPositiveButton(getString(R.string.general_ok), positiveClickListener())
                    .create()
                    .show();
        }

        private Dialog.OnClickListener positiveClickListener() {
            return new Dialog.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (authenticatingText.getVisibility() == View.VISIBLE || flipFromLoginToAuthAnimator.isRunning()) {
                        flipFromLoginToAuthAnimator.cancel();
                        flipFromAuthToLoginAnimator.start();
                    }
                }
            };
        }

    }
}
