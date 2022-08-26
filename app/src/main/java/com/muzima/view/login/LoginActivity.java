/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.login;

import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.material.textfield.TextInputLayout;
import com.muzima.BuildConfig;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.context.Context;
import com.muzima.api.model.AppUsageLogs;
import com.muzima.api.model.MinimumSupportedAppVersion;
import com.muzima.controller.AppUsageLogsController;
import com.muzima.controller.MinimumSupportedAppVersionController;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.domain.Credentials;
import com.muzima.scheduler.MuzimaJobScheduleBuilder;
import com.muzima.service.CredentialsPreferenceService;
import com.muzima.service.LocalePreferenceService;
import com.muzima.service.MuzimaGPSLocationService;
import com.muzima.service.MuzimaLoggerService;
import com.muzima.service.MuzimaSyncService;
import com.muzima.service.OnlineOnlyModePreferenceService;
import com.muzima.service.WizardFinishPreferenceService;
import com.muzima.tasks.MuzimaAsyncTask;
import com.muzima.util.Constants;
import com.muzima.util.NetworkUtils;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.StringUtils;
import com.muzima.utils.SyncSettingsIntent;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.BaseActivity;
import com.muzima.view.MainDashboardActivity;
import com.muzima.view.barcode.BarcodeCaptureActivity;
import com.muzima.view.help.HelpActivity;
import com.muzima.view.initialwizard.SetupMethodPreferenceWizardActivity;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants;
import static com.muzima.utils.Constants.STANDARD_DATE_TIMEZONE_FORMAT;
import static com.muzima.utils.Constants.STANDARD_TIME_FORMAT;
import static com.muzima.utils.DateUtils.convertLongToDateString;
import static com.muzima.utils.DeviceDetailsUtil.generatePseudoDeviceId;

import org.apache.lucene.queryParser.ParseException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

//This class shouldn't extend BaseAuthenticatedActivity. Since it is independent of the application's context
public class LoginActivity extends BaseActivity {
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
    private Button authenticatingText;
    private TextView helpText;
    private ScrollView loginScrollView;
    private FrameLayout loginFrameLayout;
    private TextView onlineModeText;
    private static final int RC_BARCODE_CAPTURE = 9001;

    private ValueAnimator flipFromLoginToAuthAnimator;
    private ValueAnimator flipFromAuthToLoginAnimator;
    private boolean isUpdatePasswordChecked;
    private boolean isOnlineModeEnabled;
    private final LanguageUtil languageUtil = new LanguageUtil();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.getInstance().onCreate(this,false);

        languageUtil.onCreate(this);
        super.onCreate(savedInstanceState);

        ((MuzimaApplication) getApplication()).cancelTimer();
        setContentView(R.layout.activity_login);
        showSessionTimeOutPopUpIfNeeded();

        initViews();
        setupListeners();
        initAnimators();
        getOnlineOnlyModePreference();

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
        onlineModeText.setText(isOnlineModeEnabled ? getResources().getString(R.string.general_online_mode) : "");
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
        languageUtil.onCreate(this);
        setupStatusView();

        if(isOnlineModeEnabled){
            removeChangedPasswordRecentlyCheckbox();
        }
    }

    private void setupStatusView() {
        if (backgroundAuthenticationTask != null && backgroundAuthenticationTask.isTaskRunning) {
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
            backgroundAuthenticationTask.cancel();
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
                    if (backgroundAuthenticationTask != null && backgroundAuthenticationTask.isTaskRunning) {
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
                        TextInputLayout usernameTextInputLayout = findViewById(R.id.username_text_input_layout);
                        usernameTextInputLayout.setHintEnabled(false);
                        usernameText.setHint(getString(R.string.hint_username_prompt));
                        usernameText.setHintTextColor(errorColor);
                    }

                    if (StringUtils.isEmpty(passwordText.getText().toString())) {
                        TextInputLayout passwordTextInputLayout = findViewById(R.id.password_text_input_layout);
                        passwordTextInputLayout.setHintEnabled(false);
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

        usernameText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                focusOnLoginView();
                return false;
            }
        });

        serverUrlText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_UP) {
                   if(event.getRawX() >= serverUrlText.getRight() - serverUrlText.getTotalPaddingRight()) {
                        Intent intent;
                        intent = new Intent(getApplicationContext(), BarcodeCaptureActivity.class);
                        intent.putExtra(BarcodeCaptureActivity.AutoFocus, true);
                        intent.putExtra(BarcodeCaptureActivity.UseFlash, false);

                        startActivityForResult(intent, RC_BARCODE_CAPTURE);
                        return false;
                    }
                }
                return false;
            }
        });

    }


    private void focusOnLoginView(){
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                loginScrollView.smoothScrollTo(0, loginFrameLayout.getTop());
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    serverUrlText.setText(barcode.displayValue);
                } else {
                    Log.d(getClass().getSimpleName(), "No barcode captured, intent data is null");
                }
            } else {
                Log.d(getClass().getSimpleName(), "No barcode captured, intent data is null "+CommonStatusCodes.getStatusCodeString(resultCode));
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
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
        loginScrollView = findViewById(R.id.login_scroll_view);
        loginFrameLayout = findViewById(R.id.login_frame_layout);
        onlineModeText = findViewById(R.id.online_mode);
    }

    public void onUpdatePasswordCheckboxClicked(View view) {
        isUpdatePasswordChecked = ((CheckBox) view).isChecked();
    }

    public void getOnlineOnlyModePreference() {
        OnlineOnlyModePreferenceService onlineOnlyModePreferenceService = new OnlineOnlyModePreferenceService((MuzimaApplication) getApplicationContext());
        isOnlineModeEnabled = onlineOnlyModePreferenceService.getOnlineOnlyModePreferenceValue();
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

    private class BackgroundAuthenticationTask extends MuzimaAsyncTask<Credentials, Void, BackgroundAuthenticationTask.Result> {
        boolean isTaskRunning = false;
        @Override
        protected void onPreExecute() {
            if (loginButton.getVisibility() == View.VISIBLE) {
                flipFromLoginToAuthAnimator.start();
            }
            boolean isTaskRunning = true;
        }

        @Override
        protected Result doInBackground(Credentials... params) {
            Credentials credentials = params[0];
            MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplication()).getMuzimaSyncService();
            int authenticationStatus = muzimaSyncService.authenticate(credentials.getCredentialsArray(), isOnlineModeEnabled || isUpdatePasswordChecked);
            return new Result(credentials, authenticationStatus);
        }

        @Override
        protected void onPostExecute(Result result) {
            MuzimaApplication muzimaApplication = (MuzimaApplication)getApplicationContext();
            if (result.status == SyncStatusConstants.AUTHENTICATION_SUCCESS) {
                if(isOnlineModeEnabled){
                    muzimaApplication.deleteAllPatientsData();
                }

                Date successfulLoginTime = new Date();

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

                checkAndUpdateUsageLogsIfNecessary(muzimaApplication, successfulLoginTime);

                MuzimaJobScheduleBuilder muzimaJobScheduleBuilder = new MuzimaJobScheduleBuilder(getApplicationContext());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    //delay for 10 seconds to allow next UI activity to finish loading
                    muzimaJobScheduleBuilder.schedulePeriodicBackgroundJob(10000,false);
                }
                boolean isTaskRunning = false;
                checkMuzimaCoreModuleCompatibility(result);
            } else {
                boolean isTaskRunning = false;
                MuzimaLoggerService.log((MuzimaApplication)getApplicationContext(),"LOGIN_FAILURE",
                        result.credentials.getUserName(),MuzimaLoggerService.getAndParseGPSLocationForLogging((MuzimaApplication)getApplicationContext()),"{}");
                Toast.makeText(getApplicationContext(), getErrorText(result), Toast.LENGTH_SHORT).show();
                if (authenticatingText.getVisibility() == View.VISIBLE || flipFromLoginToAuthAnimator.isRunning()) {
                    flipFromLoginToAuthAnimator.cancel();
                    flipFromAuthToLoginAnimator.start();
                }
            }
        }

        @Override
        protected void onBackgroundError(Exception e) {

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

        private void checkAndUpdateUsageLogsIfNecessary(MuzimaApplication muzimaApplication, Date date){
            AppUsageLogsController appUsageLogsController = muzimaApplication.getAppUsageLogsController();
            try {
                SimpleDateFormat simpleDateTimezoneFormat = new SimpleDateFormat(STANDARD_DATE_TIMEZONE_FORMAT);
                SimpleDateFormat simpleTimeFormat = new SimpleDateFormat(STANDARD_TIME_FORMAT);
                String loggedInUser = ((MuzimaApplication) getApplicationContext()).getAuthenticatedUserId();
                String pseudoDeviceId = generatePseudoDeviceId();


                //update login time
                AppUsageLogs loginTimeLog = appUsageLogsController.getAppUsageLogByKeyAndUserName(Constants.AppUsageLogs.LAST_LOGIN_TIME,loggedInUser);
                if(loginTimeLog != null) {
                    loginTimeLog.setLogvalue(simpleDateTimezoneFormat.format(date));
                    loginTimeLog.setUpdateDatetime(new Date());
                    loginTimeLog.setDeviceId(pseudoDeviceId);
                    loginTimeLog.setUserName(loggedInUser);
                    loginTimeLog.setLogSynced(false);
                    appUsageLogsController.saveOrUpdateAppUsageLog(loginTimeLog);
                }else{
                    AppUsageLogs loginTime = new AppUsageLogs();
                    loginTime.setUuid(UUID.randomUUID().toString());
                    loginTime.setLogKey(Constants.AppUsageLogs.LAST_LOGIN_TIME);
                    loginTime.setLogvalue(simpleDateTimezoneFormat.format(date));
                    loginTime.setUpdateDatetime(new Date());
                    loginTime.setDeviceId(pseudoDeviceId);
                    loginTime.setUserName(loggedInUser);
                    loginTime.setLogSynced(false);
                    appUsageLogsController.saveOrUpdateAppUsageLog(loginTime);
                }

                //Check and Update app version if need be
                AppUsageLogs appVersionLog = appUsageLogsController.getAppUsageLogByKey(Constants.AppUsageLogs.APP_VERSION);
                if(appVersionLog != null){
                    if(!appVersionLog.getLogvalue().equals(getApplicationVersion())){
                        appVersionLog.setLogvalue(getApplicationVersion());
                        appVersionLog.setUpdateDatetime(new Date());
                        appVersionLog.setDeviceId(pseudoDeviceId);
                        appVersionLog.setLogSynced(false);
                        appUsageLogsController.saveOrUpdateAppUsageLog(appVersionLog);

                        long appInstallationOrUpdateTime = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0).lastUpdateTime;
                        if(appInstallationOrUpdateTime<=0){
                            appInstallationOrUpdateTime = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0).firstInstallTime;
                        }
                        AppUsageLogs appInstallationOrUpdateTimeLog = appUsageLogsController.getAppUsageLogByKey(Constants.AppUsageLogs.APP_INSTALLATION_OR_UPDATE_TIME);
                        if(appInstallationOrUpdateTimeLog != null) {
                            appInstallationOrUpdateTimeLog.setLogvalue(convertLongToDateString(appInstallationOrUpdateTime));
                            appInstallationOrUpdateTimeLog.setUpdateDatetime(new Date());
                            appInstallationOrUpdateTimeLog.setDeviceId(pseudoDeviceId);
                            appInstallationOrUpdateTimeLog.setLogSynced(false);
                            appUsageLogsController.saveOrUpdateAppUsageLog(appInstallationOrUpdateTimeLog);
                        }else{
                            AppUsageLogs appUsageLog1 = new AppUsageLogs();
                            appUsageLog1.setUuid(UUID.randomUUID().toString());
                            appUsageLog1.setLogKey(Constants.AppUsageLogs.APP_INSTALLATION_OR_UPDATE_TIME);
                            appUsageLog1.setLogvalue(convertLongToDateString(appInstallationOrUpdateTime));
                            appUsageLog1.setUpdateDatetime(new Date());
                            appUsageLog1.setDeviceId(pseudoDeviceId);
                            appUsageLog1.setUserName(loggedInUser);
                            appUsageLog1.setLogSynced(false);
                            appUsageLogsController.saveOrUpdateAppUsageLog(appUsageLog1);
                        }
                    }
                }else{
                    AppUsageLogs appUsageLog1 = new AppUsageLogs();
                    appUsageLog1.setUuid(UUID.randomUUID().toString());
                    appUsageLog1.setLogKey(Constants.AppUsageLogs.APP_VERSION);
                    appUsageLog1.setLogvalue(getApplicationVersion());
                    appUsageLog1.setUpdateDatetime(new Date());
                    appUsageLog1.setDeviceId(pseudoDeviceId);
                    appUsageLog1.setUserName(loggedInUser);
                    appUsageLog1.setLogSynced(false);
                    appUsageLogsController.saveOrUpdateAppUsageLog(appUsageLog1);

                    long appInstallationOrUpdateTime = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0).lastUpdateTime;
                    if(appInstallationOrUpdateTime<=0){
                        appInstallationOrUpdateTime = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0).firstInstallTime;
                    }

                    AppUsageLogs appInstallationOrUpdateTimeLog = appUsageLogsController.getAppUsageLogByKey(Constants.AppUsageLogs.APP_INSTALLATION_OR_UPDATE_TIME);
                    if(appInstallationOrUpdateTimeLog != null) {
                        appInstallationOrUpdateTimeLog.setLogvalue(convertLongToDateString(appInstallationOrUpdateTime));
                        appInstallationOrUpdateTimeLog.setUpdateDatetime(new Date());
                        appInstallationOrUpdateTimeLog.setDeviceId(pseudoDeviceId);
                        appInstallationOrUpdateTimeLog.setLogSynced(false);
                        appUsageLogsController.saveOrUpdateAppUsageLog(appInstallationOrUpdateTimeLog);
                    }else{
                        AppUsageLogs appUsageLog = new AppUsageLogs();
                        appUsageLog.setUuid(UUID.randomUUID().toString());
                        appUsageLog.setLogKey(Constants.AppUsageLogs.APP_INSTALLATION_OR_UPDATE_TIME);
                        appUsageLog.setLogvalue(convertLongToDateString(appInstallationOrUpdateTime));
                        appUsageLog.setUpdateDatetime(new Date());
                        appUsageLog.setDeviceId(pseudoDeviceId);
                        appUsageLog.setUserName(loggedInUser);
                        appUsageLog.setLogSynced(false);
                        appUsageLogsController.saveOrUpdateAppUsageLog(appUsageLog);
                    }
                }

                //Check and update earliest login time if need be
                AppUsageLogs earliestLoginTime = appUsageLogsController.getAppUsageLogByKeyAndUserName(Constants.AppUsageLogs.EARLIEST_LOGIN_TIME, loggedInUser);
                Date loginTime = simpleTimeFormat.parse(simpleTimeFormat.format(date));
                if(earliestLoginTime != null){
                    Date logValue = simpleTimeFormat.parse(simpleTimeFormat.format(simpleDateTimezoneFormat.parse(earliestLoginTime.getLogvalue())));
                    if (loginTime.before(logValue)) {
                        earliestLoginTime.setLogvalue(simpleDateTimezoneFormat.format(date));
                        earliestLoginTime.setUpdateDatetime(new Date());
                        earliestLoginTime.setDeviceId(pseudoDeviceId);
                        earliestLoginTime.setUserName(loggedInUser);
                        earliestLoginTime.setLogSynced(false);
                        appUsageLogsController.saveOrUpdateAppUsageLog(earliestLoginTime);
                    }
                }else{
                    AppUsageLogs earliestLoginTime1 = new AppUsageLogs();
                    earliestLoginTime1.setUuid(UUID.randomUUID().toString());
                    earliestLoginTime1.setLogKey(Constants.AppUsageLogs.EARLIEST_LOGIN_TIME);
                    earliestLoginTime1.setLogvalue(simpleDateTimezoneFormat.format(date));
                    earliestLoginTime1.setUpdateDatetime(new Date());
                    earliestLoginTime1.setDeviceId(pseudoDeviceId);
                    earliestLoginTime1.setUserName(loggedInUser);
                    earliestLoginTime1.setLogSynced(false);
                    appUsageLogsController.saveOrUpdateAppUsageLog(earliestLoginTime1);
                }

                //Check and update Latest login time if need be
                AppUsageLogs latestLoginTime = appUsageLogsController.getAppUsageLogByKeyAndUserName(Constants.AppUsageLogs.LATEST_LOGIN_TIME, loggedInUser);
                if(latestLoginTime != null){
                    Date logValue = simpleTimeFormat.parse(simpleTimeFormat.format(simpleDateTimezoneFormat.parse(latestLoginTime.getLogvalue())));
                    if (loginTime.after(logValue)) {
                        latestLoginTime.setLogvalue(simpleDateTimezoneFormat.format(date));
                        latestLoginTime.setUpdateDatetime(new Date());
                        latestLoginTime.setDeviceId(pseudoDeviceId);
                        latestLoginTime.setUserName(loggedInUser);
                        latestLoginTime.setLogSynced(false);
                        appUsageLogsController.saveOrUpdateAppUsageLog(latestLoginTime);
                    }
                }else{
                    AppUsageLogs latestLoginTime1 = new AppUsageLogs();
                    latestLoginTime1.setUuid(UUID.randomUUID().toString());
                    latestLoginTime1.setLogKey(Constants.AppUsageLogs.LATEST_LOGIN_TIME);
                    latestLoginTime1.setLogvalue(simpleDateTimezoneFormat.format(date));
                    latestLoginTime1.setUpdateDatetime(new Date());
                    latestLoginTime1.setDeviceId(pseudoDeviceId);
                    latestLoginTime1.setUserName(loggedInUser);
                    latestLoginTime1.setLogSynced(false);
                    appUsageLogsController.saveOrUpdateAppUsageLog(latestLoginTime1);
                }

            } catch (IOException e) {
                Log.e(getClass().getSimpleName(),"Encountered an exception",e);
            } catch (ParseException e) {
                Log.e(getClass().getSimpleName(),"Encountered an exception",e);
            } catch (java.text.ParseException e) {
                Log.e(getClass().getSimpleName(),"Encountered an exception",e);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
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

    private class DownloadMuzimaAppVersionCodeBackGroundTask extends MuzimaAsyncTask<String, Void,String > {
        @Override
        protected void onPreExecute() {}

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
                        if (appVersionCode < version || version==0) {
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

        @Override
        protected void onBackgroundError(Exception e) {}

        private void startNextActivity(){
            Intent intent;
            if (new WizardFinishPreferenceService(LoginActivity.this).isWizardFinished()) {
                downloadMissingServerSettings();
                intent = new Intent(getApplicationContext(), MainDashboardActivity.class);
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
