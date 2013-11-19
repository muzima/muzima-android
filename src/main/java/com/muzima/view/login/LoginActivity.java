package com.muzima.view.login;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.internal.nineoldandroids.animation.ValueAnimator;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.domain.Credentials;
import com.muzima.service.CredentialsPreferenceService;
import com.muzima.service.MuzimaSyncService;
import com.muzima.service.WizardFinishPreferenceService;
import com.muzima.utils.NetworkUtils;
import com.muzima.utils.StringUtils;
import com.muzima.view.MainActivity;
import com.muzima.view.cohort.CohortWizardActivity;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.AUTHENTICATION_SUCCESS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.CONNECTION_ERROR;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.INVALID_CREDENTIALS_ERROR;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.MALFORMED_URL_ERROR;

public class LoginActivity extends SherlockActivity {
    private static final String TAG = "LoginActivity";
    public static final String isFirstLaunch = "isFirstLaunch";
    private EditText serverUrlText;
    private EditText usernameText;
    private EditText passwordText;
    private Button loginButton;
    private BackgroundAuthenticationTask backgroundAuthenticationTask;
    private TextView noConnectivityText;
    private TextView authenticatingText;

    private ValueAnimator flipFromNoConnToLoginAnimator;
    private ValueAnimator flipFromLoginToNoConnAnimator;
    private ValueAnimator flipFromLoginToAuthAnimator;
    private ValueAnimator flipFromAuthToLoginAnimator;
    private ValueAnimator flipFromAuthToNoConnAnimator;
    private boolean honeycombOrGreater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            honeycombOrGreater = true;
        }

        initViews();
        setupListeners();
        initAnimators();

        boolean isFirstLaunch = getIntent().getBooleanExtra(LoginActivity.isFirstLaunch, true);
        if (!isFirstLaunch) {
            removeServerUrlAsInput();
        }
        useSavedServerUrl();

        //Hack to get it to use default font space.
        passwordText.setTypeface(Typeface.DEFAULT);
    }

    private void removeServerUrlAsInput() {
        serverUrlText.setVisibility(View.GONE);
        findViewById(R.id.server_url_divider).setVisibility(View.GONE);
    }

    private void useSavedServerUrl() {
        Credentials credentials;
        credentials = new Credentials(this);
        serverUrlText.setText(credentials.getServerUrl());
    }

    @Override
    protected void onResume() {
        super.onResume();

        setupStatusView();

        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(connectivityChangeReceiver, intentFilter);
    }

    private void setupStatusView() {
        if (!NetworkUtils.isConnectedToNetwork(this)) {
            if (backgroundAuthenticationTask != null) {
                backgroundAuthenticationTask.cancel(true);
            }
            noConnectivityText.setVisibility(View.VISIBLE);
            loginButton.setVisibility(View.GONE);
            authenticatingText.setVisibility(View.GONE);
        } else if (backgroundAuthenticationTask != null && backgroundAuthenticationTask.getStatus() == AsyncTask.Status.RUNNING) {
            noConnectivityText.setVisibility(View.GONE);
            loginButton.setVisibility(View.GONE);
            authenticatingText.setVisibility(View.VISIBLE);
        } else {
            noConnectivityText.setVisibility(View.GONE);
            loginButton.setVisibility(View.VISIBLE);
            authenticatingText.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(connectivityChangeReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (backgroundAuthenticationTask != null) {
            backgroundAuthenticationTask.cancel(true);
        }
    }

    private void setupListeners() {
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validInput()) {
                    if (backgroundAuthenticationTask != null && backgroundAuthenticationTask.getStatus() == AsyncTask.Status.RUNNING) {
                        Toast.makeText(getApplicationContext(), "Authentication in progress...", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    backgroundAuthenticationTask = new BackgroundAuthenticationTask();
                    backgroundAuthenticationTask.execute(
                            new Credentials(serverUrlText.getText().toString(), usernameText.getText().toString(),
                                    passwordText.getText().toString()
                            ));
                } else {
                    int errorColor = getResources().getColor(R.color.error_text_color);
                    if (StringUtils.isEmpty(serverUrlText.getText().toString())) {
                        serverUrlText.setHint("Please Enter Server URL");
                        serverUrlText.setHintTextColor(errorColor);
                    }

                    if (StringUtils.isEmpty(usernameText.getText().toString())) {
                        usernameText.setHint("Please Enter Username");
                        usernameText.setHintTextColor(errorColor);
                    }

                    if (StringUtils.isEmpty(passwordText.getText().toString())) {
                        passwordText.setHint("Please Enter Password");
                        passwordText.setHintTextColor(errorColor);
                    }
                }
            }
        });
    }

    private boolean validInput() {
        if (StringUtils.isEmpty(serverUrlText.getText().toString())
                || StringUtils.isEmpty(usernameText.getText().toString())
                || StringUtils.isEmpty(passwordText.getText().toString())) {
            return false;
        }
        return true;
    }

    private void initViews() {
        serverUrlText = (EditText) findViewById(R.id.serverUrl);
        usernameText = (EditText) findViewById(R.id.username);
        passwordText = (EditText) findViewById(R.id.password);
        loginButton = (Button) findViewById(R.id.login);
        noConnectivityText = (TextView) findViewById(R.id.noConnectionText);
        authenticatingText = (TextView) findViewById(R.id.authenticatingText);
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
            int authenticationStatus = muzimaSyncService.authenticate(credentials.getCredentialsArray());
            return new Result(credentials, authenticationStatus);
        }

        @Override
        protected void onPostExecute(Result result) {
            if (result.status == AUTHENTICATION_SUCCESS) {
                new CredentialsPreferenceService(getApplicationContext()).saveCredentials(result.credentials);

                startNextActivity();
            } else {
                Toast.makeText(getApplicationContext(), getErrorText(result), Toast.LENGTH_SHORT).show();
                if (authenticatingText.getVisibility() == View.VISIBLE || flipFromLoginToAuthAnimator.isRunning()) {
                    flipFromLoginToAuthAnimator.cancel();
                    flipFromAuthToLoginAnimator.start();
                }
            }
        }

        private String getErrorText(Result result) {
            switch (result.status) {
                case MALFORMED_URL_ERROR:
                    return "Invalid Server URL.";
                case INVALID_CREDENTIALS_ERROR:
                    return "Invalid Username, Password, Server combination.";
                case CONNECTION_ERROR:
                    return "Error while connecting your server.";
                default:
                    return "Authentication failed";
            }
        }

        private void startNextActivity() {
            Intent intent;
            if (new WizardFinishPreferenceService(LoginActivity.this).isWizardFinished()) {
                intent = new Intent(getApplicationContext(), MainActivity.class);
            } else {
                intent = new Intent(getApplicationContext(), CohortWizardActivity.class);
            }
            startActivity(intent);
            finish();
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

    private BroadcastReceiver connectivityChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean connectedToNetwork = NetworkUtils.isConnectedToNetwork(context);
            Log.d(TAG, "onReceive(), connectedToNetwork : " + connectedToNetwork);
            if (connectedToNetwork) {
                onConnected();
            } else {
                onDisconnected();
            }
        }
    };

    private void onConnected() {
        if (noConnectivityText.getVisibility() == View.VISIBLE) {
            flipFromNoConnToLoginAnimator.start();
        }
    }

    private void onDisconnected() {
        if (loginButton.getVisibility() == View.VISIBLE) {
            flipFromLoginToNoConnAnimator.start();
        } else if (authenticatingText.getVisibility() == View.VISIBLE) {
            flipFromAuthToNoConnAnimator.start();
        }

        if (backgroundAuthenticationTask != null && backgroundAuthenticationTask.getStatus() == AsyncTask.Status.RUNNING) {
            backgroundAuthenticationTask.cancel(true);
        }
    }

    private void initAnimators() {
        flipFromLoginToNoConnAnimator = ValueAnimator.ofFloat(0, 1);
        flipFromNoConnToLoginAnimator = ValueAnimator.ofFloat(0, 1);
        flipFromLoginToAuthAnimator = ValueAnimator.ofFloat(0, 1);
        flipFromAuthToLoginAnimator = ValueAnimator.ofFloat(0, 1);
        flipFromAuthToNoConnAnimator = ValueAnimator.ofFloat(0, 1);

        initFlipAnimation(flipFromLoginToNoConnAnimator, loginButton, noConnectivityText);
        initFlipAnimation(flipFromNoConnToLoginAnimator, noConnectivityText, loginButton);
        initFlipAnimation(flipFromLoginToAuthAnimator, loginButton, authenticatingText);
        initFlipAnimation(flipFromAuthToLoginAnimator, authenticatingText, loginButton);
        initFlipAnimation(flipFromAuthToNoConnAnimator, authenticatingText, noConnectivityText);
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
                    if (honeycombOrGreater) {
                        to.setRotationX(-180 * (1 - animatedFraction));
                    }
                }

                if (from.getVisibility() == View.VISIBLE) {
                    if (honeycombOrGreater) {
                        from.setRotationX(180 * animatedFraction);
                    }
                }
            }
        });
    }
}
