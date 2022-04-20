/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.patients;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
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
import com.muzima.utils.Constants.SERVER_CONNECTIVITY_STATUS;
import com.muzima.utils.Fonts;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.forms.RegistrationFormsActivity;

import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.muzima.utils.Constants.DataSyncServiceConstants;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants;
import static com.muzima.utils.Constants.SEARCH_STRING_BUNDLE_KEY;
import static java.lang.String.valueOf;

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
    private final ThemeUtils themeUtils = new ThemeUtils();
    private final LanguageUtil languageUtil = new LanguageUtil();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeUtils.onCreate(this);
        languageUtil.onCreate(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_remote_search_list);
        Bundle intentExtras = getIntent().getExtras();
        if (intentExtras != null) {
            searchString = intentExtras.getString(SEARCH_STRING_BUNDLE_KEY);
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
        themeUtils.onResume(this);
        languageUtil.onResume(this);
    }

    private void setUpListView(String searchString) {
        recyclerView = findViewById(R.id.remote_search_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        patientAdapter = new PatientsRemoteSearchAdapter(this,
                ((MuzimaApplication) getApplicationContext()).getPatientController(), searchString);
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

        noDataMsgTextView.setTypeface(Fonts.roboto_bold_condensed(this));
        noDataTipTextView.setTypeface(Fonts.roboto_medium(this));

        createPatientBtn = findViewById(R.id.create_patient_btn);
        createPatientBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(PatientRemoteSearchListActivity.this, RegistrationFormsActivity.class));
            }
        });
        createPatientBtn.setVisibility(VISIBLE);
    }

    @Override
    public void onQueryTaskFinish() {
        progressBarContainer.setVisibility(INVISIBLE);
        if(patientAdapter.isEmpty()) {
            noDataView.setVisibility(VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            recyclerView.setVisibility(VISIBLE);
            noDataView.setVisibility(View.GONE);
        }
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

        if (errorDefinition instanceof SERVER_CONNECTIVITY_STATUS){
            SERVER_CONNECTIVITY_STATUS serverConnectivityStatus = (SERVER_CONNECTIVITY_STATUS)errorDefinition;
            if(serverConnectivityStatus == SERVER_CONNECTIVITY_STATUS.SERVER_OFFLINE) {
                noDataMsgTextView.setText(getResources().getText(R.string.error_server_connection_unavailable));
                noDataTipTextView.setText(R.string.hint_server_connection_unavailable);
            } else if(serverConnectivityStatus == SERVER_CONNECTIVITY_STATUS.INTERNET_FAILURE) {
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
        int syncStatus = intent.getIntExtra(DataSyncServiceConstants.SYNC_STATUS, SyncStatusConstants.UNKNOWN_ERROR);
        int syncType = intent.getIntExtra(DataSyncServiceConstants.SYNC_TYPE, -1);
        String[] patientUUIDs = intent.getStringArrayExtra(DataSyncServiceConstants.PATIENT_UUID_FOR_DOWNLOAD);

        if (syncType == DataSyncServiceConstants.DOWNLOAD_SELECTED_PATIENTS_FULL_DATA) {
            if (syncStatus == SyncStatusConstants.SUCCESS && patientUUIDs.length == 1) {
                try {
                    PatientController patientController = ((MuzimaApplication) getApplicationContext()).getPatientController();
                    Patient patient = patientController.getPatientByUuid(patientUUIDs[0]);
                    intent = new Intent(this, PatientSummaryActivity.class);
                    intent.putExtra(PatientSummaryActivity.PATIENT, patient);
                    startActivity(intent);
                } catch (PatientController.PatientLoadException e) {
                    Log.e(PatientRemoteSearchListActivity.class.getName(), "Could not load downloaded patient " + e.getMessage());
                    startActivity(new Intent(PatientRemoteSearchListActivity.this, PatientsListActivity.class));
                }
            } else if (syncStatus == SyncStatusConstants.SUCCESS) {
                startActivity(new Intent(PatientRemoteSearchListActivity.this, PatientsListActivity.class));
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
        }
    }

    private String[] getSelectedPatientsUuid() {
        List<String> patientUUIDs = patientAdapter.getSelectedPatientsUuids();
        return patientUUIDs.toArray(new String[patientUUIDs.size()]);
    }
}
