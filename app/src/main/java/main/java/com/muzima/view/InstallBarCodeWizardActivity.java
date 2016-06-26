/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.actionbarsherlock.view.Menu;
import com.muzima.R;
import com.muzima.utils.barcode.IntentIntegrator;
import com.muzima.view.concept.CustomConceptWizardActivity;

public class InstallBarCodeWizardActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_install_barcode_wizard);
        attachCheckScannerAction();
        attachNextActivityListener();
        Button previousButton = (Button) findViewById(R.id.previous);
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
        Button installBarCodeBtn = (Button) findViewById(R.id.install_barcode_btn);
        installBarCodeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IntentIntegrator intentIntegrator = new IntentIntegrator(InstallBarCodeWizardActivity.this);
                AlertDialog alertDialog = intentIntegrator.checkForScannerAppInstallation();
                if (alertDialog == null) {
                    findViewById(R.id.scanner_already_exists).setVisibility(View.VISIBLE);
                    Button skip = (Button)findViewById(R.id.skip);
                    skip.setText("Finish");
                }
            }
        });
    }

    private void attachNextActivityListener() {
        Button skipInstallationButton = (Button) findViewById(R.id.skip);
        skipInstallationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
