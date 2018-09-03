/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.view.Menu;
import com.muzima.R;
import com.muzima.service.LandingPagePreferenceService;
import com.muzima.utils.barcode.BarCodeScannerIntentIntegrator;
import com.muzima.view.concept.CustomConceptWizardActivity;

public class InstallBarCodeWizardActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_install_barcode_wizard);
        attachCheckScannerAction();
        attachNextActivityListener();
        Button previousButton = findViewById(R.id.previous);
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToPreviousActivity();
            }
        });
    }

    private void navigateToPreviousActivity() {
        Intent intent = new Intent(getApplicationContext(), CustomConceptWizardActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        removeSettingsMenu(menu);
        return true;
    }

    private void attachCheckScannerAction() {
        Button installBarCodeBtn = findViewById(R.id.install_barcode_btn);
        installBarCodeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BarCodeScannerIntentIntegrator barCodeScannerIntentIntegrator = new BarCodeScannerIntentIntegrator(InstallBarCodeWizardActivity.this);
                AlertDialog alertDialog = barCodeScannerIntentIntegrator.checkForScannerAppInstallation();
                if (alertDialog == null) {
                    findViewById(R.id.scanner_already_exists).setVisibility(View.VISIBLE);
                    Button skip = findViewById(R.id.skip);
                    skip.setText(R.string.general_finish_text);
                }
            }
        });
    }

    private void attachNextActivityListener() {
        Button skipInstallationButton = findViewById(R.id.skip);
        skipInstallationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new LandingPagePreferenceService(getApplicationContext()).getLandingPageActivityLauchIntent();
                startActivity(intent);
                finish();
            }
        });
    }
}
