package com.muzima.view;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.SetupConfiguration;
import com.muzima.api.model.SetupConfigurationTemplate;
import com.muzima.api.model.User;
import com.muzima.controller.SetupConfigurationController;
import com.muzima.utils.DateUtils;
import com.muzima.utils.HtmlCompat;
import com.muzima.utils.ThemeUtils;


public class AboutMuzimaActivity extends BaseActivity {
    private final ThemeUtils themeUtils = new ThemeUtils();

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

    }
}