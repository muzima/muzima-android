/*
 * Copyright (c) Vanderbilt University Medical Center and Lambda Informatics.
 * All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.SetupConfiguration;
import com.muzima.api.model.SetupConfigurationTemplate;
import com.muzima.api.model.User;
import com.muzima.controller.SetupConfigurationController;
import com.muzima.utils.DateUtils;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.help.WebViewActivity;

import java.util.Locale;


public class AboutMuzimaActivity extends BaseActivity {
    private static final String PRIVACY_POLICY_URL = "privacy_policy.html";
    private static final String TERMS_AND_CONDITIONS = "terms_and_conditions.html";
    private static final String MUZIMA_DISCLAIMER = "disclaimer.html";

    private TextView loggedInUserTextView;
    private TextView lastLoggedInTextView;
    private TextView activeSetupConfigTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.getInstance().onCreate(AboutMuzimaActivity.this,true);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_muzima);
        setTitle(R.string.general_about_muzima);

        loggedInUserTextView = findViewById(R.id.logged_in_user);
        lastLoggedInTextView = findViewById(R.id.last_logged_in);
        activeSetupConfigTextView = findViewById(R.id.active_setup_config);

        TextView appVersionTextView = findViewById(R.id.app_version);
        appVersionTextView.setText(((MuzimaApplication)getApplicationContext()).getApplicationVersion());

        LanguageUtil languageUtil = new LanguageUtil();
        Locale locale = languageUtil.getSelectedLocale(getApplicationContext());
        String LOCAL_HELP_CONTENT_ROOT_DIRECTORY = "file:///android_asset/www/help-content/"+locale.getLanguage()+"/";

        View privacyPolicy = findViewById(R.id.privacy_policy);
        privacyPolicy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startHelpContentDisplayActivity(LOCAL_HELP_CONTENT_ROOT_DIRECTORY + PRIVACY_POLICY_URL,
                        getString(R.string.title_privacy_policy));
            }
        });

        View termsAndConditions = findViewById(R.id.terms_and_conditions);
        termsAndConditions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startHelpContentDisplayActivity(LOCAL_HELP_CONTENT_ROOT_DIRECTORY + TERMS_AND_CONDITIONS,
                        getString(R.string.info_terms_and_conditions));
            }
        });
        View disclaimer = findViewById(R.id.muzima_disclaimer);
        disclaimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startHelpContentDisplayActivity(LOCAL_HELP_CONTENT_ROOT_DIRECTORY + MUZIMA_DISCLAIMER,
                        getString(R.string.title_muzima_disclaimer));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserInfo();
    }

    private void loadUserInfo(){
        User authenticatedUser = ((MuzimaApplication)getApplicationContext()).getAuthenticatedUser();
        if(authenticatedUser != null && authenticatedUser.getPerson() != null) {
            loggedInUserTextView.setText(authenticatedUser.getPerson().getDisplayName());

            if(authenticatedUser.getDateLastLoggedIn() != null) {
                lastLoggedInTextView.setText(DateUtils.getFormattedStandardDisplayDateTime(authenticatedUser.getDateLastLoggedIn()));
            } else {
                lastLoggedInTextView.setText("---");
            }

            try {
                SetupConfigurationController setupConfigurationController = ((MuzimaApplication) getApplicationContext()).getSetupConfigurationController();
                SetupConfigurationTemplate template = setupConfigurationController.getActiveSetupConfigurationTemplate();
                SetupConfiguration setupConfiguration = setupConfigurationController.getSetupConfigurations(template.getUuid());
                activeSetupConfigTextView.setText(setupConfiguration.getName());
            } catch (SetupConfigurationController.SetupConfigurationFetchException e) {
                Log.e(getClass().getSimpleName(),"Cannot fetch active setup config",e);
            }
        } else {
            loggedInUserTextView.setText("---");
            lastLoggedInTextView.setText("---");
            activeSetupConfigTextView.setText("---");
        }
    }

    private void startHelpContentDisplayActivity(String filePath, String title) {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra(WebViewActivity.HELP_FILE_PATH_PARAM, filePath);
        intent.putExtra(WebViewActivity.HELP_TITLE, title);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}