package com.muzima.view.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.domain.Credentials;
import com.muzima.search.api.util.StringUtil;
import com.muzima.service.MuzimaSyncService;
import com.muzima.view.MainActivity;

import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.AUTHENTICATION_SUCCESS;

public class LoginActivity extends SherlockActivity {
    public static final String isFirstLaunch = "isFirstLaunch";
    private EditText serverUrlText;
    private EditText usernameText;
    private EditText passwordText;
    private Button loginButton;
    private BackgroundAuthenticationTask backgroundAuthenticationTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        setupListeners();

        boolean isFirstLaunch = getIntent().getBooleanExtra(LoginActivity.isFirstLaunch, true);
        if (!isFirstLaunch) {
            Credentials credentials = credentials();
            serverUrlText.setText(credentials.getServerUrl());
            serverUrlText.setEnabled(false);
            serverUrlText.setInputType(0);
            serverUrlText.setTextColor(Color.parseColor("#666666"));

            usernameText.setText(credentials.getUserName());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(backgroundAuthenticationTask != null){
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

                    Credentials credentials = new Credentials(usernameText.getText().toString(),
                            passwordText.getText().toString(),
                            serverUrlText.getText().toString());
                    backgroundAuthenticationTask = new BackgroundAuthenticationTask();
                    backgroundAuthenticationTask.execute(credentials);
                } else {
                    int errorColor = getResources().getColor(R.color.error_text_color);
                    if (serverUrlText.getText().toString().isEmpty()) {
                        serverUrlText.setHint("Please Enter Server URL");
                        serverUrlText.setHintTextColor(errorColor);
                    }

                    if (usernameText.getText().toString().isEmpty()) {
                        usernameText.setHint("Please Enter Username");
                        usernameText.setHintTextColor(errorColor);
                    }

                    if (passwordText.getText().toString().isEmpty()) {
                        passwordText.setHint("Please Enter Password");
                        passwordText.setHintTextColor(errorColor);
                    }
                }
            }
        });
    }


    private boolean validInput() {
        if (serverUrlText.getText().toString().isEmpty()
                || usernameText.getText().toString().isEmpty()
                || passwordText.getText().toString().isEmpty()) {
            return false;
        }
        return true;
    }

    private void initViews() {
        serverUrlText = (EditText) findViewById(R.id.serverUrl);
        usernameText = (EditText) findViewById(R.id.username);
        passwordText = (EditText) findViewById(R.id.password);
        loginButton = (Button) findViewById(R.id.login);
    }

    private Credentials credentials() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String usernameKey = getResources().getString(R.string.preference_username);
        String passwordKey = getResources().getString(R.string.preference_password);
        String serverKey = getResources().getString(R.string.preference_server);
        return new Credentials(settings.getString(usernameKey, StringUtil.EMPTY),
                settings.getString(passwordKey, StringUtil.EMPTY),
                settings.getString(serverKey, StringUtil.EMPTY));
    }

    private class BackgroundAuthenticationTask extends AsyncTask<Credentials, Void, BackgroundAuthenticationTask.Result> {

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
                saveCredentials(result.credentials);

                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
            }

        }

        private void saveCredentials(Credentials credentials) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String usernameKey = getResources().getString(R.string.preference_username);
            String passwordKey = getResources().getString(R.string.preference_password);
            String serverKey = getResources().getString(R.string.preference_server);

            settings.edit()
                    .putString(usernameKey, credentials.getUserName())
                    .putString(passwordKey, credentials.getPassword())
                    .putString(serverKey, credentials.getServerUrl())
                    .commit();
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
}
