/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.patients;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.view.ActionMode;
import android.view.Menu;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.RecyclerAdapter;
import com.muzima.adapters.patients.PatientAdapterHelper;
import com.muzima.adapters.patients.PatientsRemoteSearchAdapter;
import com.muzima.api.model.Patient;
import com.muzima.controller.PatientController;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.forms.RegistrationFormsActivity;
import java.util.List;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import com.muzima.utils.Constants;

public class PatientRemoteSearchListActivity extends BroadcastListenerActivity implements PatientAdapterHelper.PatientListClickListener,
        RecyclerAdapter.BackgroundListQueryTaskListener {
    private PatientsRemoteSearchAdapter patientAdapter;
    private RecyclerView recyclerView;
    private String searchString;
    private FrameLayout progressBarContainer;
    private Button createPatientBtn;

    private View noDataView;
    private ActionMode actionMode;

    private boolean actionModeActive = false;
    private final LanguageUtil languageUtil = new LanguageUtil();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.getInstance().onCreate(this,true);
        languageUtil.onCreate(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_remote_search_list);
        Bundle intentExtras = getIntent().getExtras();
        if (intentExtras != null) {
            searchString = intentExtras.getString(Constants.SEARCH_STRING_BUNDLE_KEY);
        }
        progressBarContainer = findViewById(R.id.progressbarContainer);

        setUpListView(searchString);
        setupNoDataView();
        setTitle(R.string.general_clients);

        logEvent("VIEW_PATIENT_REMOTE_SEARCH");
        patientAdapter.reloadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        languageUtil.onResume(this);
    }

    private void setUpListView(String searchString) {
        recyclerView = findViewById(R.id.remote_search_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        patientAdapter = new PatientsRemoteSearchAdapter(this,
                ((MuzimaApplication) getApplicationContext()).getPatientController(), searchString, ((MuzimaApplication) getApplicationContext()).getMuzimaSettingController());
        patientAdapter.setBackgroundListQueryTaskListener(this);
        patientAdapter.setPatientListClickListener(this);
        recyclerView.setAdapter(patientAdapter);
    }

    @Override
    public void onQueryTaskStarted() {
        recyclerView.setVisibility(INVISIBLE);
        noDataView.setVisibility(INVISIBLE);
        progressBarContainer.setVisibility(VISIBLE);
    }

    private void setupNoDataView() {

        noDataView = findViewById(R.id.no_data_layout);

        TextView noDataMsgTextView = findViewById(R.id.no_data_msg);
        noDataMsgTextView.setText(getResources().getText(R.string.info_client_remote_search_not_found));

        TextView noDataTipTextView = findViewById(R.id.no_data_tip);
        noDataTipTextView.setText(R.string.hint_client_remote_search);

        createPatientBtn = findViewById(R.id.create_patient_btn);

        if(((MuzimaApplication) getApplicationContext()).getMuzimaSettingController().isPatientRegistrationEnabled()) {
            createPatientBtn.setVisibility(View.VISIBLE);
        }else{
            createPatientBtn.setVisibility(View.GONE);
        }

        createPatientBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(PatientRemoteSearchListActivity.this, RegistrationFormsActivity.class));
            }
        });
    }

    @Override
    public void onQueryTaskFinish() {
        if(patientAdapter.isEmpty()) {
            noDataView.setVisibility(VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            noDataView.setVisibility(View.GONE);
            recyclerView.setVisibility(VISIBLE);
        }
        progressBarContainer.setVisibility(INVISIBLE);
    }

    @Override
    public void onQueryTaskCancelled(){
        noDataView = findViewById(R.id.no_data_layout);
        TextView noDataMsgTextView = findViewById(R.id.no_data_msg);
        noDataMsgTextView.setText(getResources().getText(R.string.error_patient_search));
        createPatientBtn.setVisibility(INVISIBLE);
        noDataView.setVisibility(VISIBLE);
        progressBarContainer.setVisibility(INVISIBLE);
    }

    @Override
    public void onQueryTaskCancelled(Object errorDefinition){
        noDataView = findViewById(R.id.no_data_layout);
        TextView noDataMsgTextView = findViewById(R.id.no_data_msg);
        TextView noDataTipTextView = findViewById(R.id.no_data_tip);
        createPatientBtn.setVisibility(INVISIBLE);

        if (errorDefinition instanceof Constants.SERVER_CONNECTIVITY_STATUS){
            Constants.SERVER_CONNECTIVITY_STATUS serverConnectivityStatus = (Constants.SERVER_CONNECTIVITY_STATUS)errorDefinition;
            if(serverConnectivityStatus == Constants.SERVER_CONNECTIVITY_STATUS.SERVER_OFFLINE) {
                noDataMsgTextView.setText(getResources().getText(R.string.error_server_connection_unavailable));
                noDataTipTextView.setText(R.string.hint_server_connection_unavailable);
            } else if(serverConnectivityStatus == Constants.SERVER_CONNECTIVITY_STATUS.INTERNET_FAILURE) {
                noDataMsgTextView.setText(R.string.error_local_connection_unavailable);
                noDataTipTextView.setText(R.string.hint_local_connection_unavailable);
            }
        } else {
            noDataMsgTextView.setText(getResources().getText(R.string.error_patient_search));
        }

        noDataView.setVisibility(VISIBLE);
        progressBarContainer.setVisibility(INVISIBLE);
    }


    @Override
    public void onItemLongClick(View view, int position) {
        onItemClick(view,position);
    }

    @Override
    public void onItemClick(View view, int position) {
        patientAdapter.toggleSelection(view,position);
        if (!actionModeActive) {
            actionMode = startActionMode(new DownloadPatientMode());
            actionModeActive = true;
        }
        int numOfSelectedPatients = patientAdapter.getSelectedPatientsUuids().size();
        if (numOfSelectedPatients == 0 && actionModeActive) {
            actionMode.finish();
        }

        if(actionMode != null){
            actionMode.setTitle(String.valueOf(numOfSelectedPatients));
        }
    }

    @Override
    protected void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        int syncStatus = intent.getIntExtra(Constants.DataSyncServiceConstants.SYNC_STATUS, Constants.DataSyncServiceConstants.SyncStatusConstants.UNKNOWN_ERROR);
        int syncType = intent.getIntExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, -1);
        String[] patientUUIDs = intent.getStringArrayExtra(Constants.DataSyncServiceConstants.PATIENT_UUID_FOR_DOWNLOAD);

        if (syncType == Constants.DataSyncServiceConstants.DOWNLOAD_SELECTED_PATIENTS_FULL_DATA) {
            if (syncStatus == Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS && patientUUIDs.length == 1) {
                try {
                    PatientController patientController = ((MuzimaApplication) getApplicationContext()).getPatientController();
                    Patient patient = patientController.getPatientByUuid(patientUUIDs[0]);
                    intent = new Intent(this, PatientSummaryActivity.class);
                    intent.putExtra(PatientSummaryActivity.PATIENT, patient);
                    startActivity(intent);
                } catch (PatientController.PatientLoadException e) {
                    Log.e(PatientRemoteSearchListActivity.class.getName(), "Could not load downloaded patient " + e.getMessage());
                    startActivity(new Intent(PatientRemoteSearchListActivity.this, PatientsSearchActivity.class));
                }
            } else if (syncStatus == Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS) {
                startActivity(new Intent(PatientRemoteSearchListActivity.this, PatientsSearchActivity.class));
            }
        }
    }

    private void downloadPatients() {
        String[] patientUUIDs = getSelectedPatientsUuid();
        new PatientDownloadIntent(this, patientUUIDs).start();
    }

    private class DownloadPatientMode implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            PatientRemoteSearchListActivity.this.getMenuInflater()
                    .inflate(R.menu.actionmode_menu_download, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, android.view.MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.menu_download:
                    downloadPatients();
                    finish();
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            actionModeActive = false;
            patientAdapter.resetSelectedPatientsUuids();
            patientAdapter.notifyDataSetChanged();
        }
    }

    private String[] getSelectedPatientsUuid() {
        List<String> patientUUIDs = patientAdapter.getSelectedPatientsUuids();
        return patientUUIDs.toArray(new String[patientUUIDs.size()]);
    }
}
