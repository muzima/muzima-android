package com.muzima.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.muzima.R;
import com.muzima.view.login.LoginActivity;

public class TermsAndPolicyActivity extends AppCompatActivity {

    private CheckBox licenseCheckBox;
    private CheckBox privacyPolicyCheckbox;
    private CheckBox termsAndConditionsCheckbox;
    private ImageView licenseExpandView;
    private ImageView expandPrivacyPolicyView;
    private ImageView expandTermsAndConditionsView;
    private TextView licenseContentTextView;
    private TextView privacyPolicyContentTextView;
    private TextView termsAndConditionsContentTextView;
    private Button acceptAllButton;
    private Button nextButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_policy_layout);

        initializeResources();
    }

    private void initializeResources() {
        licenseCheckBox = findViewById(R.id.license_disclaimer_checkbox);
        privacyPolicyCheckbox = findViewById(R.id.privacy_policy_checkbox);
        termsAndConditionsCheckbox = findViewById(R.id.terms_and_conditions_checkbox);
        licenseExpandView = findViewById(R.id.expand_license_disclaimer_view);
        expandPrivacyPolicyView = findViewById(R.id.expand_privacy_policy_view);
        expandTermsAndConditionsView = findViewById(R.id.expand_terms_and_conditions_view);
        licenseContentTextView = findViewById(R.id.license_disclaimer_container_text_view);
        privacyPolicyContentTextView = findViewById(R.id.privacy_policy_container_text_view);
        termsAndConditionsContentTextView = findViewById(R.id.terms_and_conditions_container_text_view);
        acceptAllButton = findViewById(R.id.policy_accept_all_button);
        nextButton = findViewById(R.id.policy_next_button);

        licenseExpandView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                licenseContentTextView.setVisibility(View.VISIBLE);
                privacyPolicyContentTextView.setVisibility(View.GONE);
                termsAndConditionsContentTextView.setVisibility(View.GONE);

                licenseExpandView.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_arrow_up_dark));
                expandPrivacyPolicyView.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_arrow_down_dark));
                expandTermsAndConditionsView.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_arrow_down_dark));
            }
        });

        expandPrivacyPolicyView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                privacyPolicyContentTextView.setVisibility(View.VISIBLE);
                licenseContentTextView.setVisibility(View.GONE);
                termsAndConditionsContentTextView.setVisibility(View.GONE);

                expandPrivacyPolicyView.setImageDrawable(getResources()
                        .getDrawable(R.drawable.ic_action_arrow_up_dark));
                licenseExpandView.setImageDrawable(getResources()
                        .getDrawable(R.drawable.ic_action_arrow_down_dark));
                expandTermsAndConditionsView.setImageDrawable(getResources()
                        .getDrawable(R.drawable.ic_action_arrow_down_dark));
            }
        });

        expandTermsAndConditionsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                termsAndConditionsContentTextView.setVisibility(View.VISIBLE);
                privacyPolicyContentTextView.setVisibility(View.GONE);
                licenseContentTextView.setVisibility(View.GONE);

                expandTermsAndConditionsView.setImageDrawable(getResources()
                        .getDrawable(R.drawable.ic_action_arrow_up_dark));
                expandPrivacyPolicyView.setImageDrawable(getResources()
                        .getDrawable(R.drawable.ic_action_arrow_down_dark));
                licenseExpandView.setImageDrawable(getResources()
                        .getDrawable(R.drawable.ic_action_arrow_down_dark));
            }
        });

        acceptAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                licenseCheckBox.setChecked(true);
                privacyPolicyCheckbox.setChecked(true);
                termsAndConditionsCheckbox.setChecked(true);
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (licenseCheckBox.isChecked() && privacyPolicyCheckbox.isChecked() && termsAndConditionsCheckbox.isChecked())
                    navigateToNextActivity();
                else
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.error_policy_not_accepted), Toast.LENGTH_LONG).show();
            }
        });

    }

    private void navigateToNextActivity() {
        // write the setting to mark the user have accepted the disclaimer
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String disclaimerKey = getResources().getString(R.string.preference_disclaimer);
        settings.edit().putBoolean(disclaimerKey, true).commit();
        // transition to the login activity
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra(LoginActivity.isFirstLaunch, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
