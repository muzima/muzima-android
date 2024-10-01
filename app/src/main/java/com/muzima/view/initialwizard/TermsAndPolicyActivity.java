/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */
package com.muzima.view.initialwizard;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.muzima.R;
import com.muzima.utils.MuzimaPreferences;
import com.muzima.view.login.LoginActivity;

public class TermsAndPolicyActivity extends AppCompatActivity {

    private CheckBox licenseCheckBox;
    private CheckBox privacyPolicyCheckbox;
    private CheckBox termsAndConditionsCheckbox;
    private ImageView licenseExpandView;
    private ImageView expandPrivacyPolicyView;
    private ImageView expandTermsAndConditionsView;
    private View licenseContentTextView;
    private View privacyPolicyContentTextView;
    private View termsAndConditionsContentTextView;
    private TextView acceptAllButton;
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


        privacyPolicyCheckbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                acceptAllButton.setVisibility(View.VISIBLE);
                if (licenseCheckBox.isChecked() && privacyPolicyCheckbox.isChecked() && termsAndConditionsCheckbox.isChecked()) {
                    nextButton.setEnabled(true);
                    acceptAllButton.setVisibility(View.GONE);
                }else {
                    nextButton.setEnabled(false);
                    acceptAllButton.setVisibility(View.VISIBLE);
                }
            }
        });

        termsAndConditionsCheckbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                acceptAllButton.setVisibility(View.VISIBLE);
                if (licenseCheckBox.isChecked() && privacyPolicyCheckbox.isChecked() && termsAndConditionsCheckbox.isChecked()) {
                    nextButton.setEnabled(true);
                    acceptAllButton.setVisibility(View.GONE);
                }else {
                    nextButton.setEnabled(false);
                    acceptAllButton.setVisibility(View.VISIBLE);
                }
            }
        });

        licenseCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                acceptAllButton.setVisibility(View.VISIBLE);
                if (licenseCheckBox.isChecked() && privacyPolicyCheckbox.isChecked() && termsAndConditionsCheckbox.isChecked()) {
                    nextButton.setEnabled(true);
                    acceptAllButton.setVisibility(View.GONE);
                }else {
                    nextButton.setEnabled(false);
                    acceptAllButton.setVisibility(View.VISIBLE);
                }
            }
        });


        licenseExpandView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                privacyPolicyContentTextView.setVisibility(View.GONE);
                termsAndConditionsContentTextView.setVisibility(View.GONE);
                expandPrivacyPolicyView.setBackgroundResource(R.drawable.ic_action_arrow_down);
                expandTermsAndConditionsView.setBackgroundResource(R.drawable.ic_action_arrow_down);

                if(licenseContentTextView.getVisibility() == View.GONE) {
                    licenseContentTextView.setVisibility(View.VISIBLE);
                    licenseExpandView.setBackgroundResource(R.drawable.ic_action_arrow_up);
                } else {
                    licenseContentTextView.setVisibility(View.GONE);
                    licenseExpandView.setBackgroundResource(R.drawable.ic_action_arrow_down);
                }

            }
        });

        expandPrivacyPolicyView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                termsAndConditionsContentTextView.setVisibility(View.GONE);
                licenseContentTextView.setVisibility(View.GONE);

                expandTermsAndConditionsView.setBackgroundResource(R.drawable.ic_action_arrow_down);
                licenseExpandView.setBackgroundResource(R.drawable.ic_action_arrow_down);

                if(privacyPolicyContentTextView.getVisibility() == View.GONE) {
                    privacyPolicyContentTextView.setVisibility(View.VISIBLE);
                    expandPrivacyPolicyView.setBackgroundResource(R.drawable.ic_action_arrow_up);
                } else {
                    privacyPolicyContentTextView.setVisibility(View.GONE);
                    expandPrivacyPolicyView.setBackgroundResource(R.drawable.ic_action_arrow_down);
                }

            }
        });

        expandTermsAndConditionsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                privacyPolicyContentTextView.setVisibility(View.GONE);
                licenseContentTextView.setVisibility(View.GONE);
                expandPrivacyPolicyView.setBackgroundResource(R.drawable.ic_action_arrow_down);
                licenseExpandView.setBackgroundResource(R.drawable.ic_action_arrow_down);

                if(termsAndConditionsContentTextView.getVisibility() == View.GONE){
                    termsAndConditionsContentTextView.setVisibility(View.VISIBLE);

                    expandTermsAndConditionsView.setBackgroundResource(R.drawable.ic_action_arrow_up);
                } else {
                    termsAndConditionsContentTextView.setVisibility(View.GONE);

                    expandTermsAndConditionsView.setBackgroundResource(R.drawable.ic_action_arrow_down);
                }
            }
        });

        acceptAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                licenseCheckBox.setChecked(true);
                privacyPolicyCheckbox.setChecked(true);
                termsAndConditionsCheckbox.setChecked(true);
                acceptAllButton.setVisibility(View.INVISIBLE);
                nextButton.setEnabled(true);
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

        appCheckBoxPadding(privacyPolicyCheckbox);
        appCheckBoxPadding(termsAndConditionsCheckbox);
        appCheckBoxPadding(licenseCheckBox);

    }

    private void appCheckBoxPadding(CheckBox checkBox) {
        final float scale = this.getResources().getDisplayMetrics().density;
        checkBox.setPadding(checkBox.getPaddingLeft() + (int) (20.0f * scale + 0.5f),
                checkBox.getPaddingTop(),
                checkBox.getPaddingRight(),
                checkBox.getPaddingBottom());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(getApplicationContext(), OnboardScreenActivity.class);
            startActivity(intent);
            finish();
        }
        return true;
    }

    private void navigateToNextActivity() {
        // write the setting to mark the user have accepted the disclaimer
        String disclaimerKey = getResources().getString(R.string.preference_disclaimer);
        MuzimaPreferences.setBooleanPreference(getApplicationContext(),disclaimerKey, true);
        // transition to the login activity
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra(LoginActivity.isFirstLaunch, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
