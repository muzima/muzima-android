/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */
package com.muzima.view.login;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.setupconfiguration.SetupConfigurationRecyclerViewAdapter;
import com.muzima.api.model.SetupConfiguration;
import com.muzima.api.model.SetupConfigurationTemplate;
import com.muzima.controller.SetupConfigurationController;
import com.muzima.service.ActiveConfigPreferenceService;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.MainDashboardActivity;
import com.muzima.view.progressdialog.MuzimaProgressDialog;

import java.util.ArrayList;
import java.util.List;

public class ActiveConfigSelectionActivity extends BroadcastListenerActivity
        implements SetupConfigurationRecyclerViewAdapter.OnSetupConfigurationClickedListener {
    private RecyclerView configsListView;
    private MuzimaProgressDialog progressDialog;
    private SetupConfigurationRecyclerViewAdapter setupConfigurationAdapter;
    private List<SetupConfiguration> setupConfigurationList = new ArrayList<>();
    private LinearLayout noDataLayout;
    private ScrollView scrollView;
    private TextView noDataMessage;
    private TextView noDataTip;


    public void onCreate(Bundle savedInstanceState) {
        ThemeUtils.getInstance().onCreate(this,false);
        super.onCreate(savedInstanceState);
        initializeResources();
        loadLocalConfigs();
    }

    private void initializeResources() {
        progressDialog = new MuzimaProgressDialog(this);
        setContentView(R.layout.activity_active_config_selection);
        setupConfigurationAdapter = new SetupConfigurationRecyclerViewAdapter(getApplicationContext(), setupConfigurationList, this);
        configsListView = findViewById(R.id.configs_wizard_list);

        configsListView.setAdapter(setupConfigurationAdapter);
        configsListView.setLayoutManager( new LinearLayoutManager(getApplicationContext()));
        noDataLayout = findViewById(R.id.no_data_layout);
        scrollView = findViewById(R.id.scroll_view);
        noDataMessage = findViewById(R.id.no_data_msg);
        noDataTip = findViewById(R.id.no_data_tip);
        hideKeyboard();
        logEvent("VIEW_SETUP_METHODS");
    }


    private void loadLocalConfigs(){
        try {
            List<SetupConfiguration> configurationList = new ArrayList<>();
            for (SetupConfigurationTemplate template:((MuzimaApplication) getApplicationContext()).getSetupConfigurationController().getSetupConfigurationTemplates()) {
                SetupConfiguration config = ((MuzimaApplication) getApplicationContext()).getSetupConfigurationController().getSetupConfigurations(template.getUuid());
                configurationList.add(config);
            }
            setupConfigurationList.clear();
            setupConfigurationList.addAll(configurationList);
            setupConfigurationAdapter.notifyDataSetChanged();
            setupConfigurationAdapter.setItemsCopy(configurationList);
        }  catch (SetupConfigurationController.SetupConfigurationFetchException e) {
            Log.e(getClass().getSimpleName(), "Exception when trying to save setup configs");
        }

    }

    @Override
    public void onSetupConfigClicked(View view, int position) {
        SetupConfiguration configuration = setupConfigurationAdapter.getConfig(position);
        (new ActiveConfigPreferenceService((MuzimaApplication) getApplicationContext())).setActiveConfigUuid(configuration.getUuid());
        Intent intent = new Intent(getApplicationContext(), MainDashboardActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            view.clearFocus();
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void turnOnProgressDialog(String message) {
        progressDialog.show(message);
    }

    private void dismissProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }
}