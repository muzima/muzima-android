/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.setupconfiguration;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.azimolabs.keyboardwatcher.KeyboardWatcher;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.adapters.setupconfiguration.SetupConfigurationAdapter;
import com.muzima.api.model.LastSyncTime;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.service.MuzimaSyncService;
import com.muzima.service.SntpService;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.progressdialog.MuzimaProgressDialog;

import java.io.IOException;
import java.util.UUID;

import static com.muzima.api.model.APIName.DOWNLOAD_SETUP_CONFIGURATIONS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS;

public class SetupMethodPreferenceWizardActivity extends BroadcastListenerActivity implements ListAdapter.BackgroundListQueryTaskListener, KeyboardWatcher.OnKeyboardToggleListener {
    private Button activeNextButton;
    private Button inactiveNextButton;
    private View nextButtonLayout;
    private ListView configsListView;
    private MuzimaProgressDialog progressDialog;
    private boolean isProcessDialogOn = false;
    private PowerManager.WakeLock wakeLock = null;
    private KeyboardWatcher keyboardWatcher;
    private final ThemeUtils themeUtils = new ThemeUtils(R.style.WizardTheme_Light, R.style.WizardTheme_Dark);

    public void onCreate(Bundle savedInstanceState) {
        themeUtils.onCreate(this);
        super.onCreate(savedInstanceState);

        progressDialog = new MuzimaProgressDialog(this);

        setContentView(R.layout.activity_setup_method_wizard);

        inactiveNextButton = findViewById(R.id.inactive_next);

        final SetupConfigurationAdapter setupConfigurationAdapter = new SetupConfigurationAdapter(
                this, R.layout.item_setup_configs_list,
                ((MuzimaApplication) getApplicationContext()).getSetupConfigurationController());
        setupConfigurationAdapter.setBackgroundListQueryTaskListener(this);

        final EditText configSetupFilter = findViewById(R.id.filter_configs_txt);
        configSetupFilter.addTextChangedListener(textWatcherForFilterText(setupConfigurationAdapter));

        setupConfigurationAdapter.reloadData();

        //set clearing ability for the imageButton under Guided setup
        ImageButton imageButton = findViewById(R.id.cancel_filter_txt);
        imageButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                configSetupFilter.setText("");
            }
        });

        activeNextButton = findViewById(R.id.next);
        activeNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToGuidedWizardActivity(setupConfigurationAdapter);
            }
        });

        nextButtonLayout = findViewById(R.id.next_button_layout);

        keyboardWatcher = new KeyboardWatcher(this);
        keyboardWatcher.setListener(this);

        configsListView = findViewById(R.id.configs_wizard_list);
        configsListView.setOnItemClickListener(configsListViewSelectedListener(setupConfigurationAdapter));
        configsListView.setAdapter(setupConfigurationAdapter);

        logEvent("VIEW_SETUP_METHODS");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        removeSettingsMenu(menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        keyboardWatcher.destroy();
        super.onDestroy();
    }

    @Override
    public void onKeyboardShown(int keyboardSize) {
        nextButtonLayout.setVisibility(View.GONE);

    }

    @Override
    public void onKeyboardClosed() {
        nextButtonLayout.setVisibility(View.VISIBLE);

    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            view.clearFocus();
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private AdapterView.OnItemClickListener configsListViewSelectedListener(final SetupConfigurationAdapter setupConfigurationAdapter) {

        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                activateNextButton();
                hideKeyboard();
                setupConfigurationAdapter.onListItemClick(position);
            }
        };
    }

    private void activateNextButton() {
        inactiveNextButton.setVisibility(View.GONE);
        activeNextButton.setVisibility(View.VISIBLE);
    }

    private void navigateToGuidedWizardActivity(final SetupConfigurationAdapter setupConfigurationAdapter) {
        turnOnProgressDialog(getString(R.string.info_setup_configuration_wizard_prepare));
        new AsyncTask<Void, Void, int[]>() {

            @Override
            protected void onPreExecute() {
                ((MuzimaApplication) getApplication()).cancelTimer();
                keepPhoneAwake(true);
            }

            @Override
            protected int[] doInBackground(Void... voids) {
                return downloadSetupConfiguration(setupConfigurationAdapter);
            }

            @Override
            protected void onPostExecute(int[] result) {
                dismissProgressDialog();
                Log.i(getClass().getSimpleName(), "Restarting timeout timer!");
                ((MuzimaApplication) getApplication()).restartTimer();
                if (result[0] != SUCCESS) {
                    Toast.makeText(SetupMethodPreferenceWizardActivity.this,
                            getString(R.string.error_setup_configuration_template_download), Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        LastSyncTimeService lastSyncTimeService =
                                ((MuzimaApplication) getApplicationContext()).getMuzimaContext().getLastSyncTimeService();
                        SntpService sntpService = ((MuzimaApplication) getApplicationContext()).getSntpService();
                        LastSyncTime lastSyncTime = new LastSyncTime(DOWNLOAD_SETUP_CONFIGURATIONS, sntpService.getTimePerDeviceTimeZone());
                        lastSyncTimeService.saveLastSyncTime(lastSyncTime);
                    } catch (IOException e) {
                        Log.i(getClass().getSimpleName(), "Error setting Setup Configuration sync time.");
                    }
                    keepPhoneAwake(false);
                    Intent intent = new Intent(getApplicationContext(), GuidedConfigurationWizardActivity.class);
                    intent.putExtra(GuidedConfigurationWizardActivity.SETUP_CONFIG_UUID_INTENT_KEY,
                            setupConfigurationAdapter.getSelectedConfigurationUuid());
                    startActivity(intent);
                    finish();
                }
            }
        }.execute();
    }

    private int[] downloadSetupConfiguration(SetupConfigurationAdapter setupConfigurationAdapter) {
        MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
        String selectedConfigUuid = setupConfigurationAdapter.getSelectedConfigurationUuid();
        return muzimaSyncService.downloadSetupConfigurationTemplate(selectedConfigUuid);
    }

    private void keepPhoneAwake(boolean awakeState) {
        Log.d(getClass().getSimpleName(), "Launching wake state: " + awakeState);
        if (awakeState) {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, UUID.randomUUID().toString());
            wakeLock.acquire();
        } else {
            if (wakeLock != null) {
                wakeLock.release();
            }
        }
    }

    private TextWatcher textWatcherForFilterText(final SetupConfigurationAdapter setupConfigurationAdapter) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                setupConfigurationAdapter.filterItems(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        };
    }

    @Override
    public void onQueryTaskStarted() {
        turnOnProgressDialog(getString(R.string.info_setup_config_load));
    }

    @Override
    public void onQueryTaskFinish() {
        dismissProgressDialog();
    }

    @Override
    public void onQueryTaskCancelled() {
    }

    @Override
    public void onQueryTaskCancelled(Object erroeDefinition) {
    }

    private void turnOnProgressDialog(String message) {
        progressDialog.show(message);
        isProcessDialogOn = true;
    }

    private void dismissProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            isProcessDialogOn = false;
        }
    }
}
