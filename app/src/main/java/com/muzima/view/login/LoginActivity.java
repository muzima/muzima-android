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

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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
import com.muzima.api.model.AppRelease;
import com.muzima.api.model.AppUsageLogs;
import com.muzima.api.model.MinimumSupportedAppVersion;
import com.muzima.api.model.MuzimaSetting;

import com.muzima.controller.AppUsageLogsController;
import com.muzima.controller.AppReleaseController;
import com.muzima.controller.MinimumSupportedAppVersionController;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.domain.Credentials;
import com.muzima.scheduler.MuzimaJobScheduleBuilder;
import com.muzima.scheduler.RealTimeFormUploader;
import com.muzima.service.ConfidentialityNoticeDisplayPreferenceService;
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
import com.muzima.view.main.HTCMainActivity;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import org.apache.lucene.queryParser.ParseException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.muzima.utils.DateUtils;
import com.muzima.utils.DeviceDetailsUtil;


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
    private static final int EXTERNAL_STORAGE_MANAGEMENT = 9002;

    private ValueAnimator flipFromLoginToAuthAnimator;
    private ValueAnimator flipFromAuthToLoginAnimator;
    private boolean isUpdatePasswordChecked;
    private boolean isOnlineModeEnabled;
    private final LanguageUtil languageUtil = new LanguageUtil();
    private android.content.Context context;
    private long downloadID;
    private String filename = "";
    private String appUrl = "";
    private boolean isConnectedToServer = false;
    boolean isFirstLaunchValue;

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

        isFirstLaunchValue = getIntent().getBooleanExtra(LoginActivity.isFirstLaunch, true);
        String serverURL = getServerURL();
        if (!isFirstLaunchValue && !StringUtils.isEmpty(serverURL)) {
            removeServerUrlAsInput();
        }

        useSavedServerUrl(serverURL);

        if(isFirstLaunchValue){
            removeChangedPasswordRecentlyCheckbox();
        }

        //Hack to get it to use default font space.
        passwordText.setTypeface(Typeface.DEFAULT);
        versionText.setText(getApplicationVersion());
        onlineModeText.setText(isOnlineModeEnabled ? getResources().getString(R.string.general_online_mode) : "");

        String savedUsername = getUsername();
        if(!StringUtils.isEmpty(savedUsername)){
            usernameText.setText(savedUsername);
            passwordText.requestFocus();
        }else{
            usernameText.requestFocus();
        }
        initializeGPSDataCollection();
        context = getApplicationContext();

        if(isConfidentialityTextVisibilityRequired()) {
            TextView confidentialityMsgTextView = findViewById(R.id.info_confidentiality_text);
            confidentialityMsgTextView.setVisibility(View.VISIBLE);
        }
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

        if(!StringUtils.isEmpty(getUsername())) {
            passwordText.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    focusOnLoginView();
                    return false;
                }
            });
        }

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
        }else if(requestCode == EXTERNAL_STORAGE_MANAGEMENT) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    downloadAPK();
                } else {
                    Toast.makeText(this, "Allow permission for storage access!", Toast.LENGTH_SHORT).show();
                }
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

    public boolean isConfidentialityTextVisibilityRequired() {
        ConfidentialityNoticeDisplayPreferenceService confidentialityTextPreferenceService = new ConfidentialityNoticeDisplayPreferenceService((MuzimaApplication) getApplicationContext());
        return confidentialityTextPreferenceService.getConfidentialityNoticeDisplayPreferenceValue();
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
        boolean isNewUser = false;
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
            isNewUser = ((MuzimaApplication) getApplication()).isNewUser(credentials.getUserName());
            MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplication()).getMuzimaSyncService();
            int authenticationStatus = muzimaSyncService.authenticate(credentials.getCredentialsArray(), isOnlineModeEnabled || isUpdatePasswordChecked);
            return new Result(credentials, authenticationStatus);
        }

        @Override
        protected void onPostExecute(Result result) {
            MuzimaApplication muzimaApplication = (MuzimaApplication)getApplicationContext();
            if (result.status == com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.AUTHENTICATION_SUCCESS) {
                if(isNewUser && !isFirstLaunchValue && ((MuzimaApplication) getApplication()).getMuzimaSettingController().isClearAppDataIfNewUserEnabled()){
                    showAlertDialog(result.credentials);
                }else {
                    if (isOnlineModeEnabled) {
                        muzimaApplication.deleteAllPatientsData();
                    }

                    Date successfulLoginTime = new Date();

                    MuzimaLoggerService.scheduleLogSync(muzimaApplication);
                    MuzimaLoggerService.log(muzimaApplication, "LOGIN_SUCCESS",
                            result.credentials.getUserName(), MuzimaLoggerService.getAndParseGPSLocationForLogging(muzimaApplication), "{}");
                    new CredentialsPreferenceService(getApplicationContext()).saveCredentials(result.credentials);
                    ((MuzimaApplication) getApplication()).restartTimer();
                    LocalePreferenceService localePreferenceService = ((MuzimaApplication) getApplication()).getLocalePreferenceService();

                    String languageKey = getApplicationContext().getResources().getString(R.string.preference_app_language);
                    String defaultLanguage = getApplicationContext().getString(R.string.language_portuguese);
                    String preferredLocale = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(languageKey, defaultLanguage);

                    localePreferenceService.setPreferredLocale(preferredLocale);

                    checkAndUpdateUsageLogsIfNecessary(muzimaApplication, successfulLoginTime, result.credentials.getUserName());

                    MuzimaJobScheduleBuilder muzimaJobScheduleBuilder = new MuzimaJobScheduleBuilder(getApplicationContext());
                    //delay for 10 seconds to allow next UI activity to finish loading
                    muzimaJobScheduleBuilder.schedulePeriodicBackgroundJob(10000, false);
                    boolean isTaskRunning = false;
                    checkMuzimaCoreModuleCompatibility(result);
                }
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
                case com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.MALFORMED_URL_ERROR:
                    return getString(R.string.error_server_url_invalid);
                case com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.INVALID_CREDENTIALS_ERROR:
                    return getString(R.string.error_credential_invalid);
                case com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.INVALID_CHARACTER_IN_USERNAME:
                    return getString(R.string.error_username_invalid_format) + com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.INVALID_CHARACTER_FOR_USERNAME;
                case com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.LOCAL_CONNECTION_ERROR:
                    return getString(R.string.error_local_connection_unavailable);
                case com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.SERVER_CONNECTION_ERROR:
                    return getString(R.string.error_server_connection_unavailable);
                case com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.UNKNOWN_ERROR:
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

        private void showAlertDialog(Credentials credentials) {
            new AlertDialog.Builder(LoginActivity.this)
                    .setCancelable(true)
                    .setIcon(ThemeUtils.getIconWarning(LoginActivity.this))
                    .setTitle(getResources().getString(R.string.general_caution))
                    .setMessage(getResources().getString(R.string.warning_new_user_login))
                    .setPositiveButton(getString(R.string.general_ok), positiveClickListener(credentials))
                    .setNegativeButton(getString(R.string.general_no), negativeClickListener(credentials))
                    .create()
                    .show();
        }

        private Dialog.OnClickListener positiveClickListener(Credentials credentials) {
            return new Dialog.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    clearData(credentials);
                }
            };
        }

        private Dialog.OnClickListener negativeClickListener(Credentials credentials) {
            return new Dialog.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ((MuzimaApplication)getApplicationContext()).deleteUserByUserName(credentials.getUserName());
                    launchLoginActivity();
                }
            };
        }
    }


    public void checkAndUpdateUsageLogsIfNecessary(MuzimaApplication muzimaApplication, Date date, String loggedInUser){
        AppUsageLogsController appUsageLogsController = muzimaApplication.getAppUsageLogsController();
        try {
            SimpleDateFormat simpleDateTimezoneFormat = new SimpleDateFormat(com.muzima.utils.Constants.STANDARD_DATE_TIMEZONE_FORMAT);
            SimpleDateFormat simpleTimeFormat = new SimpleDateFormat(com.muzima.utils.Constants.STANDARD_TIME_FORMAT);
            String pseudoDeviceId = DeviceDetailsUtil.generatePseudoDeviceId();


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
                        appInstallationOrUpdateTimeLog.setLogvalue(DateUtils.convertLongToDateString(appInstallationOrUpdateTime));
                        appInstallationOrUpdateTimeLog.setUpdateDatetime(new Date());
                        appInstallationOrUpdateTimeLog.setDeviceId(pseudoDeviceId);
                        appInstallationOrUpdateTimeLog.setLogSynced(false);
                        appUsageLogsController.saveOrUpdateAppUsageLog(appInstallationOrUpdateTimeLog);
                    }else{
                        AppUsageLogs appUsageLog1 = new AppUsageLogs();
                        appUsageLog1.setUuid(UUID.randomUUID().toString());
                        appUsageLog1.setLogKey(Constants.AppUsageLogs.APP_INSTALLATION_OR_UPDATE_TIME);
                        appUsageLog1.setLogvalue(DateUtils.convertLongToDateString(appInstallationOrUpdateTime));
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
                    appInstallationOrUpdateTimeLog.setLogvalue(DateUtils.convertLongToDateString(appInstallationOrUpdateTime));
                    appInstallationOrUpdateTimeLog.setUpdateDatetime(new Date());
                    appInstallationOrUpdateTimeLog.setDeviceId(pseudoDeviceId);
                    appInstallationOrUpdateTimeLog.setLogSynced(false);
                    appUsageLogsController.saveOrUpdateAppUsageLog(appInstallationOrUpdateTimeLog);
                }else{
                    AppUsageLogs appUsageLog = new AppUsageLogs();
                    appUsageLog.setUuid(UUID.randomUUID().toString());
                    appUsageLog.setLogKey(Constants.AppUsageLogs.APP_INSTALLATION_OR_UPDATE_TIME);
                    appUsageLog.setLogvalue(DateUtils.convertLongToDateString(appInstallationOrUpdateTime));
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
            Log.e(getClass().getSimpleName(), "Encountered an exception",e);
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
                    isConnectedToServer = true;
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
                            checkIfNewAppReleaseAvailable(serverUrl);
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

        private void checkIfNewAppReleaseAvailable(String serverUrl){
            new DownloadAppReleaseBackGroundTask().execute(serverUrl);
        }

    }

    private class DownloadAppReleaseBackGroundTask extends MuzimaAsyncTask<String, Void,String > {
        @Override
        protected void onPreExecute() {}

        @Override
        public String doInBackground(String... params){
            String serverUrl = params[0];
            AppReleaseController appReleaseController = ((MuzimaApplication) getApplication()).getAppReleaseController();
            try {
                if(isConnectedToServer) {
                    List<AppRelease> downloadedAppReleases = appReleaseController.downloadAppRelease();
                    appReleaseController.saveAppRelease(downloadedAppReleases);
                }
            } catch (AppReleaseController.AppReleaseDownloadException e) {
                Log.e(getClass().getSimpleName(),"Encountered an exception while downloading app releases ",e);
            } catch (AppReleaseController.AppReleaseSaveException e) {
                Log.e(getClass().getSimpleName(),"Encountered an exception while saving app releases ",e);
            }
            return serverUrl;
        }

        @Override
        protected void onPostExecute(String serverUrl) {
            if(isConnectedToServer) {
                AppRelease newAppRelease;
                AppReleaseController appReleaseController = ((MuzimaApplication) getApplication()).getAppReleaseController();
                try {
                    newAppRelease = appReleaseController.getAppRelease();
                    if (newAppRelease == null || newAppRelease.getVersionCode() == null) {
                        //No Release set. Do nothing, and start next activity
                        startNextActivity();
                    } else {
                        Integer newVersionCode = newAppRelease.getVersionCode();
                        Integer minSDKVersion = newAppRelease.getMinSDKVersion();
                        String newVersionName = newAppRelease.getVersionName();
                        Date availabilityDate = newAppRelease.getAvailabilityDate();
                        boolean isTodayEqualToOrGreaterThanAvailabilityDate = false;
                        Integer installedVersion = BuildConfig.VERSION_CODE;
                        //check if current date is greater than availability date
                        if(availabilityDate == null){
                            isTodayEqualToOrGreaterThanAvailabilityDate = true;
                        }else{
                            Date today = new Date();
                            if(today.after(availabilityDate) || today.equals(availabilityDate)) {
                                isTodayEqualToOrGreaterThanAvailabilityDate = true;
                            }
                        }
                        appUrl = newAppRelease.getUrl();
                        if (installedVersion < newVersionCode && Build.VERSION.SDK_INT >= minSDKVersion && isTodayEqualToOrGreaterThanAvailabilityDate) {
                            showAlertDialog(newVersionName, newAppRelease.isEnforcedUpdate());
                        } else {
                            startNextActivity();
                        }
                    }
                } catch (AppReleaseController.AppReleaseFetchException e) {
                    Log.e(getClass().getSimpleName(), "Encountered an exception while fetching/retrieving supported app release ", e);
                }
            }else{
                startNextActivity();
            }
        }

        @Override
        protected void onBackgroundError(Exception e) {}

        private void showAlertDialog(String newVersion, boolean isEnforcedUpdate) {
            ViewGroup viewGroup = findViewById(android.R.id.content);
            View dialogView = LayoutInflater.from(LoginActivity.this).inflate(R.layout.new_version_alert_dialog, viewGroup, false);
            Button positiveButton = (Button) dialogView.findViewById(R.id.buttonPositive);
            Button negativeButton = (Button) dialogView.findViewById(R.id.buttonNegative);
            TextView message = (TextView) dialogView.findViewById(R.id.message);
            message.setText(getResources().getString(R.string.warning_new_version_available, newVersion));

            if(isEnforcedUpdate) {
                negativeButton.setVisibility(View.GONE);
                positiveButton.setText(getString(R.string.general_ok));
                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        initiateInstall();
                    }
                });
            }else{
                positiveButton.setText(getString(R.string.general_yes));
                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        initiateInstall();
                    }
                });

                negativeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startNextActivity();
                    }
                });
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
            builder.setView(dialogView);
            AlertDialog alertDialog = builder.create();
            alertDialog.setCancelable(false);
            alertDialog.show();
        }
    }

    private void startNextActivity(){
        Intent intent;
        if (new WizardFinishPreferenceService(LoginActivity.this).isWizardFinished()) {
            downloadMissingServerSettings();

            MuzimaSettingController muzimaSettingController = ((MuzimaApplication) getApplicationContext()).getMuzimaSettingController();
            MuzimaSetting setting = null;
            try {
                setting = muzimaSettingController.getSettingByProperty("Program.defintion");
            } catch (MuzimaSettingController.MuzimaSettingFetchException e) {
                e.printStackTrace();
            }
            if ((setting != null && setting.getValueString() != null) && setting.getValueString().equals("ATS")) {
                intent = new Intent(getApplicationContext(), HTCMainActivity.class);
            } else {
                intent = new Intent(getApplicationContext(), MainDashboardActivity.class);
            }
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

    public void downloadAPK(){
        registerReceiver(onDownloadComplete,new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        String filepath = appUrl;
        URL url = null;
        try {
            url  = new URL(filepath);
        } catch (MalformedURLException e) {
            Log.e(getClass().getSimpleName(), "Malformed UARl Exception ",e);
        }

        filename = url.getPath();
        filename = filename.substring(filename.lastIndexOf('/')+1);

        //Delete file if exists
        String PATH = Objects.requireNonNull(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)).getAbsolutePath();
        File file = new File(PATH + "/"+filename);
        if(file.exists()) {
            file.delete();
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url+""));
        request.setTitle(filename);
        request.allowScanningByMediaScanner();
        request.setAllowedOverMetered(true);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        downloadID = dm.enqueue(request);

        startNextActivity();
    }

    private BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, Intent intent) {
            //Fetching the download id received with the broadcast
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            //Checking if the received broadcast is for our enqueued download by matching download id
            if (downloadID == id) {
                installApk();
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private boolean checkPermission() {
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.R){
             return Environment.isExternalStorageManager();
        } else {
            int result = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
            int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), READ_EXTERNAL_STORAGE);
            boolean granted = result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
            return granted;
        }
    }

    private void requestPermission() {
        Log.e(getClass().getSimpleName(),"Permissions requesting");
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.R){
            try{
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s", new Object[]{getApplicationContext().getPackageName()})));
                startActivityForResult(intent, EXTERNAL_STORAGE_MANAGEMENT);
            }catch(Exception e){
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, EXTERNAL_STORAGE_MANAGEMENT);
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE}, 200);
        }
    }

    public void initiateInstall(){
        if (checkPermission()) {
            downloadAPK();
        } else {
            Log.e(getClass().getSimpleName(),"Permissions not granted");
            requestPermission();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 200) {
            if (grantResults.length > 0) {
                downloadAPK();
            }
        }
    }

    private void installApk() {
        try {
            String PATH = Objects.requireNonNull(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)).getAbsolutePath();

            File file = new File(PATH + "/"+filename);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri downloaded_apk = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", file);
            intent.setDataAndType(downloaded_apk, "application/vnd.android.package-archive");
            List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                context.grantUriPermission(context.getApplicationContext().getPackageName() + ".provider", downloaded_apk, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "Exception ",e);
        }
    }

    private String getUsername() {
        boolean isUsernameAutoPopulationEnabled = ((MuzimaApplication) getApplication()).getMuzimaSettingController().isUsernameAutoPopulationEnabled();
        if(isUsernameAutoPopulationEnabled) {
            Credentials credentials;
            credentials = new Credentials(this);
            return credentials.getUserName();
        }else
            return StringUtils.EMPTY;
    }

    private void clearData(Credentials credentials){
        BackgroundClearAppTask backgroundClearAppTask =  new BackgroundClearAppTask();
        backgroundClearAppTask.execute(credentials);
    }

    public void launchNewSetup() {
        Intent intent;
        intent = new Intent(getApplicationContext(), SetupMethodPreferenceWizardActivity.class);
        startActivity(intent);
        finish();
    }

    public void launchLoginActivity(){
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(LoginActivity.isFirstLaunch, false);
        startActivity(intent);
    }

    class BackgroundClearAppTask extends MuzimaAsyncTask<Credentials, Void, Void> {

        @Override
        protected void onPreExecute() {}

        @Override
        protected Void doInBackground(Credentials... params) {
            Credentials credentials = params[0];
            //Attempt to upload complete forms
            RealTimeFormUploader.getInstance().uploadAllCompletedForms(getApplicationContext(), true);

            MuzimaApplication muzimaApplication = (MuzimaApplication)getApplicationContext();
            muzimaApplication.clearApplicationData();
            new WizardFinishPreferenceService(getApplicationContext()).resetWizard();
            new CredentialsPreferenceService(getApplicationContext()).saveCredentials(new Credentials("", null, null));
            com.muzima.api.context.Context muzimaContext = muzimaApplication.getMuzimaContext();
            new CredentialsPreferenceService(getApplicationContext()).deleteUserData(muzimaContext);

            Date successfulLoginTime = new Date();

            MuzimaLoggerService.scheduleLogSync(muzimaApplication);
            MuzimaLoggerService.log(muzimaApplication, "LOGIN_SUCCESS",
                    credentials.getUserName(), MuzimaLoggerService.getAndParseGPSLocationForLogging(muzimaApplication), "{}");
            new CredentialsPreferenceService(getApplicationContext()).saveCredentials(credentials);
            LocalePreferenceService localePreferenceService = muzimaApplication.getLocalePreferenceService();

            String languageKey = getApplicationContext().getResources().getString(R.string.preference_app_language);
            String defaultLanguage = getApplicationContext().getString(R.string.language_portuguese);
            String preferredLocale = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(languageKey, defaultLanguage);

            localePreferenceService.setPreferredLocale(preferredLocale);

            checkAndUpdateUsageLogsIfNecessary(muzimaApplication, successfulLoginTime, credentials.getUserName());

            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            launchNewSetup();
        }

        @Override
        protected void onBackgroundError(Exception e) {

        }
    }
}
