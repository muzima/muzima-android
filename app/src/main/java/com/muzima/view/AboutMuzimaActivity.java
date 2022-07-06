package com.muzima.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.SetupConfiguration;
import com.muzima.api.model.SetupConfigurationTemplate;
import com.muzima.api.model.User;
import com.muzima.controller.SetupConfigurationController;
import com.muzima.utils.DateUtils;
import com.muzima.utils.HtmlCompat;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.ThemeUtils;

import java.util.Locale;


public class AboutMuzimaActivity extends BaseActivity {
    private final ThemeUtils themeUtils = new ThemeUtils();
    private static final String PRIVACY_POLICY_URL = "privacy_policy.html";
    private static final String TERMS_AND_CONDITIONS = "terms_and_conditions.html";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeUtils.onCreate(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_muzima);
        setTitle(R.string.general_about_muzima);

        User authenticatedUser = ((MuzimaApplication)getApplicationContext()).getAuthenticatedUser();
        TextView loggedInUserTextView = findViewById(R.id.logged_in_user);
        loggedInUserTextView.setText(authenticatedUser.getPerson().getDisplayName());

        TextView lastLoggedInTextView = findViewById(R.id.last_logged_in);
        if(authenticatedUser.getDateLastLoggedIn() != null) {
            lastLoggedInTextView.setText(DateUtils.getFormattedStandardDisplayDateTime(authenticatedUser.getDateLastLoggedIn()));
        } else {
            lastLoggedInTextView.setText("---");
        }

        try {
            SetupConfigurationController setupConfigurationController = ((MuzimaApplication) getApplicationContext()).getSetupConfigurationController();
            SetupConfigurationTemplate template = setupConfigurationController.getActiveSetupConfigurationTemplate();
            SetupConfiguration setupConfiguration = setupConfigurationController.getSetupConfigurations(template.getUuid());
            TextView activeSetupConfigTextView = findViewById(R.id.active_setup_config);
            activeSetupConfigTextView.setText(setupConfiguration.getName());
        } catch (SetupConfigurationController.SetupConfigurationFetchException e) {
            Log.e(getClass().getSimpleName(),"Cannot fetch active setup config",e);
        }

        TextView disclaimerTextView = findViewById(R.id.disclaimer);
        String disclaimerText = getResources().getString(R.string.info_disclaimer);
        disclaimerTextView.setText(HtmlCompat.fromHtml(disclaimerText));

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

    }

    private void startHelpContentDisplayActivity(String filePath, String title) {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra(WebViewActivity.HELP_FILE_PATH_PARAM, filePath);
        intent.putExtra(WebViewActivity.HELP_TITLE, title);
        startActivity(intent);
    }
}