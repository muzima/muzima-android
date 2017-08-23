/*
 * Copyright (c) 2014 - 2017. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
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
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.CheckedLinearLayout;
import com.muzima.view.cohort.CohortWizardActivity;
import com.muzima.view.progressdialog.MuzimaProgressDialog;

import java.io.IOException;

import static com.muzima.api.model.APIName.DOWNLOAD_SETUP_CONFIGURATIONS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS;

public class SetupMethodPreferenceWizardActivity extends BroadcastListenerActivity implements ListAdapter.BackgroundListQueryTaskListener, KeyboardWatcher.OnKeyboardToggleListener  {
    private Button activeNextButton;
    private Button inactiveNextButton;
    private CheckedLinearLayout advancedSetupLayout;
    private LinearLayout nextButtonLayout;
    private ListView configsListView;
    private MuzimaProgressDialog progressDialog;
    private boolean isProcessDialogOn = false;
    private final String TAG = "CohortWizardActivity" ;
    private PowerManager powerManager = null;
    private PowerManager.WakeLock wakeLock = null ;
    private KeyboardWatcher keyboardWatcher;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        progressDialog = new MuzimaProgressDialog(this);

        setContentView(R.layout.activity_setup_method_wizard);

        inactiveNextButton = (Button) findViewById(R.id.inactive_next);

        advancedSetupLayout = (CheckedLinearLayout)findViewById(R.id.advanced_setup_layout);
        advancedSetupLayout.setOnClickListener(advancedSetupClickListener());

        final SetupConfigurationAdapter setupConfigurationAdapter = new SetupConfigurationAdapter(
                getApplicationContext(),R.layout.item_setup_configs_list,
                ((MuzimaApplication) getApplicationContext()).getSetupConfigurationController());
        setupConfigurationAdapter.setBackgroundListQueryTaskListener(this);

        final EditText configSetupFilter = (EditText) findViewById(R.id.filter_configs_txt);
        configSetupFilter.addTextChangedListener(textWatcherForFilterText(setupConfigurationAdapter));

        setupConfigurationAdapter.reloadData();

        activeNextButton = (Button) findViewById(R.id.next);
        activeNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(advancedSetupLayout.isChecked()) {
                    navigateToCohortsWizardActivity();
                } else {
                    navigateToGuidedWizardActivity(setupConfigurationAdapter);
                }
            }
        });

        nextButtonLayout = (LinearLayout)findViewById(R.id.next_button_layout);

        keyboardWatcher = new KeyboardWatcher(this);
        keyboardWatcher.setListener(this);

        configsListView = (ListView)findViewById(R.id.configs_wizard_list);
        configsListView.setOnItemClickListener(configsListViewSelectedListener(setupConfigurationAdapter));
        configsListView.setAdapter(setupConfigurationAdapter);
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
        advancedSetupLayout.setVisibility(View.GONE);
        nextButtonLayout.setVisibility(View.GONE);

    }

    @Override
    public void onKeyboardClosed() {
        advancedSetupLayout.setVisibility(View.VISIBLE);
        nextButtonLayout.setVisibility(View.VISIBLE);

    }

    private View.OnClickListener advancedSetupClickListener(){

        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activateNextButton();
                deselectGuidedSetupConfigsView();
                selectAdvancedSetupView();
            }
        };
    }

    private AdapterView.OnItemClickListener configsListViewSelectedListener(final SetupConfigurationAdapter setupConfigurationAdapter){

        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                activateNextButton();
                deselectAdvancedSetupView();
                setupConfigurationAdapter.onListItemClick(position);
            }
        };
    }

    private void selectAdvancedSetupView(){
        advancedSetupLayout.setChecked(true);
    }

    private void deselectAdvancedSetupView(){
        advancedSetupLayout.setChecked(false);
    }

    private void deselectGuidedSetupConfigsView(){
        configsListView.clearChoices();
        configsListView.requestLayout();
    }

    private void activateNextButton(){
        inactiveNextButton.setVisibility(View.GONE);
        activeNextButton.setVisibility(View.VISIBLE);
    }

    private void navigateToCohortsWizardActivity() {
            Intent intent = new Intent(getApplicationContext(), CohortWizardActivity.class);
            startActivity(intent);
            finish();
    }

    private void navigateToGuidedWizardActivity(final SetupConfigurationAdapter setupConfigurationAdapter){
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
                Log.i(TAG, "Restarting timeout timer!");
                ((MuzimaApplication) getApplication()).restartTimer();
                if (result[0] != SUCCESS) {
                    Toast.makeText(SetupMethodPreferenceWizardActivity.this,
                            getString(R.string.error_setup_configuration_template_download), Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        LastSyncTimeService lastSyncTimeService =
                                ((MuzimaApplication) getApplicationContext()).getMuzimaContext().getLastSyncTimeService();
                        SntpService sntpService = ((MuzimaApplication) getApplicationContext()).getSntpService();
                        LastSyncTime lastSyncTime = new LastSyncTime(DOWNLOAD_SETUP_CONFIGURATIONS, sntpService.getLocalTime());
                        lastSyncTimeService.saveLastSyncTime(lastSyncTime);
                    } catch (IOException e) {
                        Log.i(TAG, "Error setting Setup Configuration sync time.");
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

    private int[] downloadSetupConfiguration(SetupConfigurationAdapter setupConfigurationAdapter){
        MuzimaSyncService muzimaSyncService = ((MuzimaApplication) getApplicationContext()).getMuzimaSyncService();
        String selectedConfigUuid = setupConfigurationAdapter.getSelectedConfigurationUuid();
        return muzimaSyncService.downloadSetupConfigurationTemplate(selectedConfigUuid);
    }
    private void keepPhoneAwake(boolean awakeState) {
        Log.d(TAG, "Launching wake state: " + awakeState) ;
        if (awakeState) {
            powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
            wakeLock.acquire();
        } else {
            if(wakeLock != null) {
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
    public void onQueryTaskCancelled(){}

    @Override
    public void onQueryTaskCancelled(Object erroeDefinition){}

    private void turnOnProgressDialog(String message){
        progressDialog.show(message);
        isProcessDialogOn = true;
    }

    private void dismissProgressDialog(){
        if (progressDialog != null){
            progressDialog.dismiss();
            isProcessDialogOn = false;
        }
    }
}
