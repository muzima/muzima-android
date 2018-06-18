/*
 * Copyright (c) 2014 - 2017. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.login;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.animation.ValueAnimator;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.context.Context;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.domain.Credentials;
import com.muzima.service.CredentialsPreferenceService;
import com.muzima.service.LandingPagePreferenceService;
import com.muzima.service.LocalePreferenceService;
import com.muzima.service.MuzimaSyncService;
import com.muzima.service.RequireMedicalRecordNumberPreferenceService;
import com.muzima.service.WizardFinishPreferenceService;
import com.muzima.util.MuzimaLogger;
import com.muzima.utils.StringUtils;
import com.muzima.view.MainActivity;
import com.muzima.view.setupconfiguration.SetupMethodPreferenceWizardActivity;

import java.util.Locale;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants;

//This class shouldn't extend BaseActivity. Since it is independent of the application's context
public class LoginActivity extends Activity {
    private static final String TAG = "LoginActivity";
    public static final String isFirstLaunch = "isFirstLaunch";
    public static final String sessionTimeOut = "SessionTimeOut";
    private EditText serverUrlText;
    private EditText usernameText;
    private EditText passwordText;
    private Button loginButton;
    private CheckBox updatePassword;
    private TextView versionText;
    private BackgroundAuthenticationTask backgroundAuthenticationTask;
    private TextView authenticatingText;

    private ValueAnimator flipFromNoConnToLoginAnimator;
    private ValueAnimator flipFromLoginToNoConnAnimator;
    private ValueAnimator flipFromLoginToAuthAnimator;
    private ValueAnimator flipFromAuthToLoginAnimator;
    private ValueAnimator flipFromAuthToNoConnAnimator;
    private boolean isUpdatePasswordChecked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
    }

    private void showSessionTimeOutPopUpIfNeeded() {
        if (getIntent().getBooleanExtra(LoginActivity.sessionTimeOut, false)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder
                    .setCancelable(true)
                    .setIcon(getResources().getDrawable(R.drawable.ic_warning))
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
            versionText = getResources().getString(R.string.general_application_version, versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Unable to read application version.", e);
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
    }

    private boolean validInput() {
        return !(StringUtils.isEmpty(serverUrlText.getText().toString())
                || StringUtils.isEmpty(usernameText.getText().toString())
                || StringUtils.isEmpty(passwordText.getText().toString()));
    }

    private void initViews() {
        serverUrlText = (EditText) findViewById(R.id.serverUrl);
        usernameText = (EditText) findViewById(R.id.username);
        passwordText = (EditText) findViewById(R.id.password);
        updatePassword = (CheckBox) findViewById(R.id.update_password);
        loginButton = (Button) findViewById(R.id.login);
        authenticatingText = (TextView) findViewById(R.id.authenticatingText);
        versionText = (TextView) findViewById(R.id.version);

    }

    public void onUpdatePasswordCheckboxClicked(View view) {
        isUpdatePasswordChecked = ((CheckBox) view).isChecked();
    }

    public void removeRemnantDataFromPreviousRunOfWizard() {
        if (!new WizardFinishPreferenceService(this).isWizardFinished()) {
            try {
                MuzimaApplication application = ((MuzimaApplication) getApplicationContext());
                Context context = application.getMuzimaContext();

                //Cohort Wizard activity
                application.getPatientController().deleteAllPatients();
                application.getCohortController().deleteCohortMembers(application.getCohortController().getAllCohorts());
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
                Log.e(TAG, "Unable to delete previous wizard run data. Error: " + e);
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
            if (result.status == SyncStatusConstants.AUTHENTICATION_SUCCESS) {
                MuzimaLogger.log(((MuzimaApplication)getApplicationContext()).getMuzimaContext(),"LOGIN_SUCCESS",
                        "{\"userId\":\"" +result.credentials.getUserName()+"\"}");
                new CredentialsPreferenceService(getApplicationContext()).saveCredentials(result.credentials);
                ((MuzimaApplication) getApplication()).restartTimer();
                LocalePreferenceService localePreferenceService = ((MuzimaApplication) getApplication()).getLocalePreferenceService();
                String currentLocale = Locale.getDefault().toString();
                localePreferenceService.setPreferredLocale(currentLocale);

                //init a background service to download missing settings
                downloadMissingServerSettings();
                startNextActivity();
            } else {
                MuzimaLogger.log(((MuzimaApplication)getApplicationContext()).getMuzimaContext(),"LOGIN_FAILURE",
                        "{\"userId\":\"" +result.credentials.getUserName()+"\"}");
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
                default:
                    return getString(R.string.error_authentication_fail);
            }
        }

        private void startNextActivity() {
            Intent intent;
            if (new WizardFinishPreferenceService(LoginActivity.this).isWizardFinished()) {
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
                    new MissingSettingsDownloadBackgroundTask().execute();
                }
            } catch (MuzimaSettingController.MuzimaSettingFetchException e){

            }
        }

        protected class Result {
            Credentials credentials;
            int status;

            private Result(Credentials credentials, int status) {
                this.credentials = credentials;
                this.status = status;
            }
        }
    }

    private class MissingSettingsDownloadBackgroundTask extends AsyncTask<String, Void,int[] > {
        @Override
        public int[] doInBackground(String... params){
            MuzimaSyncService syncService = ((MuzimaApplication) getApplication()).getMuzimaSyncService();
            return syncService.downloadMissingMandatorySettings();
        }

        @Override
        protected void onPostExecute(int[] result) {
            new RequireMedicalRecordNumberPreferenceService((MuzimaApplication) getApplicationContext())
                    .saveRequireMedicalRecordNumberPreference();
        }
    }

    private void initAnimators() {
        flipFromLoginToNoConnAnimator = ValueAnimator.ofFloat(0, 1);
        flipFromNoConnToLoginAnimator = ValueAnimator.ofFloat(0, 1);
        flipFromLoginToAuthAnimator = ValueAnimator.ofFloat(0, 1);
        flipFromAuthToLoginAnimator = ValueAnimator.ofFloat(0, 1);
        flipFromAuthToNoConnAnimator = ValueAnimator.ofFloat(0, 1);

        initFlipAnimation(flipFromLoginToAuthAnimator, loginButton, authenticatingText);
        initFlipAnimation(flipFromAuthToLoginAnimator, authenticatingText, loginButton);
    }

    public void initFlipAnimation(ValueAnimator valueAnimator, final View from, final View to) {
        valueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        valueAnimator.setDuration(300);
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
}
