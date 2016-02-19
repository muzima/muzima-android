/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.patients;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.adapters.patients.PatientsRemoteSearchAdapter;
import com.muzima.api.model.Patient;
import com.muzima.controller.PatientController;
import com.muzima.utils.Fonts;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.forms.RegistrationFormsActivity;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.muzima.utils.Constants.DataSyncServiceConstants;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants;
import static com.muzima.utils.Constants.SEARCH_STRING_BUNDLE_KEY;
import static java.lang.String.valueOf;

public class PatientRemoteSearchListActivity extends BroadcastListenerActivity implements AdapterView.OnItemClickListener,
        ListAdapter.BackgroundListQueryTaskListener {
    private PatientsRemoteSearchAdapter patientAdapter;
    private ListView listView;
    private String searchString;
    private String[] patientUUIDs;
    private FrameLayout progressBarContainer;

    private View noDataView;
    private ActionMode actionMode;

    private boolean actionModeActive = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_remote_search_list);
        Bundle intentExtras = getIntent().getExtras();
        if (intentExtras != null) {
            searchString = intentExtras.getString(SEARCH_STRING_BUNDLE_KEY);
        }
        progressBarContainer = (FrameLayout) findViewById(R.id.progressbarContainer);

        setUpListView(searchString);
        setupNoDataView();
        patientAdapter.reloadData();
        Button createPatientBtn = (Button) findViewById(R.id.create_patient_btn);
        createPatientBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(PatientRemoteSearchListActivity.this, RegistrationFormsActivity.class));
            }
        });

    }

    private void setUpListView(String searchString) {
        listView = (ListView) findViewById(R.id.remote_search_list);
        listView.setEmptyView(findViewById(R.id.no_data_layout));
        patientAdapter = new PatientsRemoteSearchAdapter(getApplicationContext(),
                R.layout.activity_patient_remote_search_list,
                ((MuzimaApplication) getApplicationContext()).getPatientController(), searchString);
        patientAdapter.setBackgroundListQueryTaskListener(this);
        listView.setAdapter(patientAdapter);
        listView.setOnItemClickListener(this);
    }

    @Override
    public void onQueryTaskStarted() {
        listView.setVisibility(INVISIBLE);
        noDataView.setVisibility(INVISIBLE);
        progressBarContainer.setVisibility(VISIBLE);
    }

    private void setupNoDataView() {

        noDataView = findViewById(R.id.no_data_layout);

        TextView noDataMsgTextView = (TextView) findViewById(R.id.no_data_msg);
        noDataMsgTextView.setText(getResources().getText(R.string.no_clients_matched_remotely));

        TextView noDataTipTextView = (TextView) findViewById(R.id.no_data_tip);
        noDataTipTextView.setText(R.string.no_clients_matched_tip_remotely);

        noDataMsgTextView.setTypeface(Fonts.roboto_bold_condensed(this));
        noDataTipTextView.setTypeface(Fonts.roboto_light(this));
    }

    @Override
    public void onQueryTaskFinish() {
        listView.setVisibility(VISIBLE);
        progressBarContainer.setVisibility(INVISIBLE);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (!actionModeActive && getCheckedItemCount(listView) > 0) {
            actionMode = this.startActionMode(new DownloadPatientMode());
            actionModeActive = true;
        } else if (getCheckedItemCount(listView) == 0) {
            actionMode.finish();
        }
        actionMode.setTitle(valueOf(getCheckedItemCount(listView)));
    }

    private int getCheckedItemCount(ListView listView) {
        if (Build.VERSION.SDK_INT >= 11) return listView.getCheckedItemCount();
        else
        {
            int count = 0;
            for (int i = listView.getCount() - 1; i >= 0; i--)
                if (listView.isItemChecked(i)) count++;
            return count;
        }
    }


    @Override
    protected void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        int syncStatus = intent.getIntExtra(DataSyncServiceConstants.SYNC_STATUS, SyncStatusConstants.UNKNOWN_ERROR);
        int syncType = intent.getIntExtra(DataSyncServiceConstants.SYNC_TYPE, -1);
        String[] patientUUIDs = intent.getStringArrayExtra(DataSyncServiceConstants.PATIENT_UUID_FOR_DOWNLOAD);

        if (syncType == DataSyncServiceConstants.DOWNLOAD_PATIENT_ONLY) {
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
        patientUUIDs = getSelectedPatientsUuid();
        new PatientDownloadIntent(this, patientUUIDs).start();
    }

    private class DownloadPatientMode implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            PatientRemoteSearchListActivity.this.getSupportMenuInflater()
                    .inflate(R.menu.actionmode_menu_download, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, com.actionbarsherlock.view.MenuItem menuItem) {
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
            for (int i = 0; i < listView.getChildCount(); i++)
                listView.setItemChecked(i, false);
        }
    }

    private String[] getSelectedPatientsUuid() {
        List<String> patientUUIDs = new ArrayList<String>();
        SparseBooleanArray checkedItemPositions = listView.getCheckedItemPositions();
        for (int i = 0; i < checkedItemPositions.size(); i++) {
            if (checkedItemPositions.valueAt(i)) {
                patientUUIDs.add(((Patient) listView.getItemAtPosition(checkedItemPositions.keyAt(i))).getUuid());
            }
        }
        return patientUUIDs.toArray(new String[patientUUIDs.size()]);
    }
}
