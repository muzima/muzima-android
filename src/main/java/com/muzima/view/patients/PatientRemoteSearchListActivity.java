package com.muzima.view.patients;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.*;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.adapters.patients.PatientsRemoteSearchAdapter;
import com.muzima.api.model.Patient;
import com.muzima.controller.PatientController;
import com.muzima.utils.Fonts;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.forms.RegistrationFormsActivity;
import com.muzima.view.preferences.SettingsActivity;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.muzima.utils.Constants.DataSyncServiceConstants.*;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.SUCCESS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants.UNKNOWN_ERROR;
import static com.muzima.utils.Constants.SEARCH_STRING_BUNDLE_KEY;
import static java.lang.String.valueOf;

public class PatientRemoteSearchListActivity extends BroadcastListenerActivity implements AdapterView.OnItemClickListener,
        ListAdapter.BackgroundListQueryTaskListener {
    private PatientsRemoteSearchAdapter patientAdapter;
    private ListView listView;
    private String searchString;
    private FrameLayout progressBarContainer;
    private PatientController patientController;

    private View noDataView;
    private ActionMode actionMode;

    private boolean actionModeActive = false;
    private boolean syncInProgress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_remote_search_list);
        patientController = ((MuzimaApplication) getApplicationContext()).getPatientController();
        Bundle intentExtras = getIntent().getExtras();
        if (intentExtras != null) {
            searchString = intentExtras.getString(SEARCH_STRING_BUNDLE_KEY);
        }
        progressBarContainer = (FrameLayout) findViewById(R.id.progressbarContainer);

        setUpListView(searchString);
        setupNoDataView();
        setupActionbar();
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void setupActionbar() {
        ActionBar supportActionBar = getSupportActionBar();
        supportActionBar.setDisplayShowTitleEnabled(true);
        supportActionBar.setDisplayHomeAsUpEnabled(true);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
        int syncStatus = intent.getIntExtra(SYNC_STATUS, UNKNOWN_ERROR);
        int syncType = intent.getIntExtra(SYNC_TYPE, -1);

        if (syncType == DOWNLOAD_PATIENT_ONLY) {
            syncInProgress = false;
            if (syncStatus == SUCCESS) {
                startActivity(new Intent(PatientRemoteSearchListActivity.this, PatientsListActivity.class));
            }
        }
    }

    private void downloadPatients(String[] patientUUIDs) {
        syncInProgress = true;
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
                    downloadPatients(getSelectedPatientsUuid());
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
