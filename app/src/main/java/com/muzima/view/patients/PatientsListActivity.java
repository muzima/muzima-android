/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.patients;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v7.widget.SearchView;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.adapters.patients.PatientsLocalSearchAdapter;
import com.muzima.api.model.Patient;
import com.muzima.controller.PatientController;
import com.muzima.utils.Fonts;
import com.muzima.utils.barcode.IntentIntegrator;
import com.muzima.utils.barcode.IntentResult;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.forms.RegistrationFormsActivity;
import android.support.design.widget.FloatingActionButton;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.muzima.utils.Constants.DataSyncServiceConstants;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants;
import static com.muzima.utils.Constants.SEARCH_STRING_BUNDLE_KEY;

public class PatientsListActivity extends BroadcastListenerActivity implements AdapterView.OnItemClickListener,
        ListAdapter.BackgroundListQueryTaskListener {
    public static final String COHORT_ID = "cohortId";
    public static final String COHORT_NAME = "cohortName";
    public static final String QUICK_SEARCH = "quickSearch";
    private ListView listView;
    private boolean quickSearch = false;
    private String cohortId = null;
    private PatientsLocalSearchAdapter patientAdapter;
    private FrameLayout progressBarContainer;
    private View noDataView;
    private String searchString;
    private Button searchServerBtn;
    FloatingActionButton fabSearchButton;
    private LinearLayout searchServerLayout;
    private SearchView searchView;
    private MenuItem searchMenuItem;
    private boolean intentBarcodeResults = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_list);
        Bundle intentExtras = getIntent().getExtras();
        if (intentExtras != null) {
            quickSearch = intentExtras.getBoolean(QUICK_SEARCH);
            cohortId = intentExtras.getString(COHORT_ID);
            String title = intentExtras.getString(COHORT_NAME);
            if (title != null) {
                setTitle(title);
            }
        }

        progressBarContainer = (FrameLayout) findViewById(R.id.progressbarContainer);
        setupNoDataView();
        setupListView(cohortId);

        fabSearchButton =  (FloatingActionButton) findViewById(R.id.fab_search);
        fabSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchMenuItem.setVisible(true);
                searchView.setIconified(false);
                searchView.requestFocusFromTouch();
                fabSearchButton.setVisibility(GONE);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        searchServerLayout = (LinearLayout) findViewById(R.id.search_server_layout);

        searchServerBtn = (Button) findViewById(R.id.search_server_btn);
        searchServerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PatientsListActivity.this, PatientRemoteSearchListActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString(SEARCH_STRING_BUNDLE_KEY, searchString);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });
    }
    @Override
    public void onReceive(Context context, Intent intent){
        super.onReceive(context, intent);
        int syncStatus = intent.getIntExtra(DataSyncServiceConstants.SYNC_STATUS, SyncStatusConstants.UNKNOWN_ERROR);
        int syncType = intent.getIntExtra(DataSyncServiceConstants.SYNC_TYPE, -1);
        int downloadCount = intent.getIntExtra(DataSyncServiceConstants.DOWNLOAD_COUNT_SECONDARY,0);
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
                }
            } else if (syncStatus == SyncStatusConstants.SUCCESS && downloadCount > 0) {
                patientAdapter.reloadData();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.client_list, menu);
        searchMenuItem = menu.findItem(R.id.search);
        searchView = (SearchView) searchMenuItem.getActionView();

        searchView.setQueryHint(getString(R.string.hint_client_search));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                searchString = s;
                activateRemoteAfterThreeCharacterEntered(s);
                patientAdapter.search(s.trim());
                return true;
            }

        });
        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener(){
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    fabSearchButton.setVisibility(GONE);
                } else {
                    fabSearchButton.postDelayed(new Runnable() {
                        public void run() {
                            fabSearchButton.setVisibility(VISIBLE);
                        }
                    }, 500);

                    if (searchView.getQuery().toString().trim().isEmpty()) {
                        searchMenuItem.setVisible(false);
                    }
                }
            }
        });

        if (quickSearch) {
            searchMenuItem.setVisible(true);
            searchView.requestFocus();
        }
        super.onCreateOptionsMenu(menu);
        return true;
    }

    private void activateRemoteAfterThreeCharacterEntered(String searchString) {
        if (searchString.trim().length() < 3) {
            searchServerLayout.setVisibility(View.INVISIBLE);
        } else {
            searchServerLayout.setVisibility(View.VISIBLE);
        }
    }

    // Confirmation dialog for confirming if the patient have an existing ID
    private void callConfirmationDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(PatientsListActivity.this);
        builder
                .setCancelable(true)
                .setIcon(getResources().getDrawable(R.drawable.ic_warning))
                .setTitle(getResources().getString(R.string.title_logout_confirm))
                .setMessage(getResources().getString(R.string.confirm_patient_id_exists))
                .setPositiveButton("Yes", yesClickListener())
                .setNegativeButton("No", noClickListener()).create().show();
    }

    private Dialog.OnClickListener yesClickListener() {
        return new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                searchMenuItem.setVisible(true);
                searchView.setIconified(false);
                searchView.requestFocus();
            }
        };
    }

    private Dialog.OnClickListener noClickListener() {
        return new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(PatientsListActivity.this, RegistrationFormsActivity.class));
            }
        };
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_client_add:
                callConfirmationDialog();
                return true;

            case R.id.scan:
                invokeBarcodeScan();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!intentBarcodeResults)
            patientAdapter.reloadData();
    }

    private void setupListView(String cohortId) {
        listView = (ListView) findViewById(R.id.list);
        patientAdapter = new PatientsLocalSearchAdapter(getApplicationContext(),
                R.layout.layout_list,
                ((MuzimaApplication) getApplicationContext()).getPatientController(), cohortId);
        patientAdapter.setBackgroundListQueryTaskListener(this);
        listView.setAdapter(patientAdapter);
        listView.setOnItemClickListener(this);
    }

    private void setupNoDataView() {

        noDataView = findViewById(R.id.no_data_layout);

        TextView noDataMsgTextView = (TextView) findViewById(R.id.no_data_msg);
        noDataMsgTextView.setText(getResources().getText(R.string.info_client_local_search_not_found));

        TextView noDataTipTextView = (TextView) findViewById(R.id.no_data_tip);
        noDataTipTextView.setText(R.string.hint_client_local_search);

        noDataMsgTextView.setTypeface(Fonts.roboto_bold_condensed(this));
        noDataTipTextView.setTypeface(Fonts.roboto_light(this));
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        patientAdapter.cancelBackgroundTask();
        Patient patient = patientAdapter.getItem(position);
        Intent intent = new Intent(this, PatientSummaryActivity.class);
        intent.putExtra(PatientSummaryActivity.PATIENT, patient);
        startActivity(intent);
    }

    @Override
    public void onQueryTaskStarted() {
        listView.setVisibility(INVISIBLE);
        noDataView.setVisibility(INVISIBLE);
        listView.setEmptyView(progressBarContainer);
        progressBarContainer.setVisibility(VISIBLE);
    }

    @Override
    public void onQueryTaskFinish() {
        listView.setVisibility(VISIBLE);
        listView.setEmptyView(noDataView);
        progressBarContainer.setVisibility(INVISIBLE);
    }

    @Override
    public void onQueryTaskCancelled(){}

    @Override
    public void onQueryTaskCancelled(Object errorDefinition){}

    public void invokeBarcodeScan() {
        IntentIntegrator scanIntegrator = new IntentIntegrator(this);

        scanIntegrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent dataIntent) {
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, dataIntent);
        if (scanningResult != null) {
            intentBarcodeResults = true;
            searchView.setQuery(scanningResult.getContents(), false);
        }
    }

    @Override
    public void onBackPressed(){
        patientAdapter.cancelBackgroundTask();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
