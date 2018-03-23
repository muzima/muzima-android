/*
 * Copyright (c) 2014 - 2017. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.patients;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.LayoutInflater;
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
import com.muzima.api.model.PatientIdentifier;
import com.muzima.api.service.SmartCardRecordService;
import com.muzima.controller.CohortController;
import com.muzima.controller.PatientController;
import com.muzima.controller.SmartCardController;
import com.muzima.model.shr.kenyaemr.KenyaEmrShrModel;
import com.muzima.service.MuzimaSyncService;
import com.muzima.utils.Constants;
import com.muzima.utils.Fonts;
import com.muzima.utils.barcode.BarCodeScannerIntentIntegrator;
import com.muzima.utils.barcode.IntentResult;
import com.muzima.utils.smartcard.KenyaEmrShrMapper;
import com.muzima.utils.smartcard.SmartCardIntentIntegrator;
import com.muzima.utils.smartcard.SmartCardIntentResult;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.MainActivity;
import com.muzima.view.forms.FormsActivity;
import com.muzima.view.forms.RegistrationFormsActivity;

import android.support.design.widget.FloatingActionButton;
import android.widget.Toast;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.muzima.utils.Constants.DataSyncServiceConstants;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants;
import static com.muzima.utils.Constants.SEARCH_STRING_BUNDLE_KEY;
import static com.muzima.utils.barcode.BarCodeScannerIntentIntegrator.*;
import static com.muzima.utils.smartcard.SmartCardIntentIntegrator.*;

import com.muzima.api.model.SmartCardRecord;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;


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
    private boolean intentShrResults = false;

    private PatientController patientController;
    private MuzimaApplication muzimaApplication;
    private CohortController cohortController;
    private SmartCardController smartCardController;
    private SmartCardRecordService smartCardService;
    private SmartCardRecord smartCardRecord;
    private MuzimaSyncService muzimaSyncService;

    Patient shrPatient;
    Patient shrToMuzimaMatchingPatient;

    private PatientsListActivity.BackgroundPatientLocalSearchQueryTask mBackgroundQueryTask;
    private PatientsListActivity.BackgroundPatientServerSearchQueryTask patientServerSearchQueryTask;
    private PatientsListActivity.BackgroundPatientDownloadTask patientDownloadTask;

    private AlertDialog negativeServerSearchResultNotifyAlertDialog;
    private AlertDialog localSearchResultNotifyAlertDialog;

    private TextView searchDialogTextView;
    private Button yesOptionShrSearchButton;
    private Button noOptionShrSearchButton;

    private ProgressDialog progressDialog;

    private final String TAG = this.getClass().getName();

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

        fabSearchButton = (FloatingActionButton) findViewById(R.id.fab_search);
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


        muzimaApplication = (MuzimaApplication) getApplicationContext();
        muzimaSyncService = muzimaApplication.getMuzimaSyncService();
        patientController = muzimaApplication.getPatientController();
        cohortController = muzimaApplication.getCohortController();
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        smartCardController = ((MuzimaApplication)getApplicationContext()).getSmartCardController();
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        int syncStatus = intent.getIntExtra(DataSyncServiceConstants.SYNC_STATUS, SyncStatusConstants.UNKNOWN_ERROR);
        int syncType = intent.getIntExtra(DataSyncServiceConstants.SYNC_TYPE, -1);
        int downloadCount = intent.getIntExtra(DataSyncServiceConstants.DOWNLOAD_COUNT_SECONDARY, 0);
        String[] patientUUIDs = intent.getStringArrayExtra(DataSyncServiceConstants.PATIENT_UUID_FOR_DOWNLOAD);

        if (syncType == DataSyncServiceConstants.DOWNLOAD_PATIENT_ONLY) {

            if (syncStatus == SyncStatusConstants.SUCCESS && patientUUIDs.length == 1) {
                try {
                    PatientController patientController = ((MuzimaApplication) getApplicationContext()).getPatientController();
                    Patient patient = patientController.getPatientByUuid(patientUUIDs[0]);
                    intent = new Intent(this, PatientSummaryActivity.class);

                    /**
                     * todo check if this patient is registred in shr
                     * before opening PatientSummary activity
                     */
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
        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
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
        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
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
                    searchMenuItem.setVisible(false);
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
            case R.id.menu_client_add_icon:
                callConfirmationDialog();
                return true;

            case R.id.menu_client_add_text:
                callConfirmationDialog();
                return true;

            case R.id.bar_card_scan:
                invokeBarcodeScan();
                return true;

            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.menu_dashboard:
                launchDashboardActivity();
                return true;

            case R.id.menu_complete_form_data:
                launchCompleteFormsActivity();
                return true;

            case R.id.scan_shr_card:
                invokeShrApplication();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        preparedServerSearchNegativeResultHandlerDialog(getApplicationContext());
        if (!intentBarcodeResults)
            patientAdapter.reloadData();

    }

    public void prepareLocalSearchNotifyDialog(Context context,Patient patient) {

        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = layoutInflater.inflate(R.layout.patient_shr_card_search_dialog, null);
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(PatientsListActivity.this);

        localSearchResultNotifyAlertDialog = alertBuilder
                .setView(dialogView)
                .create();

        localSearchResultNotifyAlertDialog.setCancelable(true);
        searchDialogTextView = (TextView) dialogView.findViewById(R.id.patent_dialog_message_textview);
        yesOptionShrSearchButton = (Button) dialogView.findViewById(R.id.yes_shr_search_dialog);
        noOptionShrSearchButton = (Button) dialogView.findViewById(R.id.no_shr_search_dialog);
        searchDialogTextView
                .setText("Smartcard client ["+patient.getDisplayName()+"] not in mUzima list. Search the client in server now ?");

        yesOptionShrSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executePatientServerSearchInBackgroundQueryTask();
                localSearchResultNotifyAlertDialog.cancel();
                localSearchResultNotifyAlertDialog.dismiss();
            }
        });

        noOptionShrSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                localSearchResultNotifyAlertDialog.cancel();
                localSearchResultNotifyAlertDialog.dismiss();
            }
        });
    }

    public void preparedServerSearchNegativeResultHandlerDialog(Context context) {

        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = layoutInflater.inflate(R.layout.patient_shr_card_search_dialog, null);
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(PatientsListActivity.this);

        negativeServerSearchResultNotifyAlertDialog = alertBuilder
                .setView(dialogView)
                .create();

        negativeServerSearchResultNotifyAlertDialog.setCancelable(true);
        searchDialogTextView = (TextView) dialogView.findViewById(R.id.patent_dialog_message_textview);
        yesOptionShrSearchButton = (Button) dialogView.findViewById(R.id.yes_shr_search_dialog);
        noOptionShrSearchButton = (Button) dialogView.findViewById(R.id.no_shr_search_dialog);
        searchDialogTextView.setText("Smartcard client  not in server. Register client in mUzima now ?");

        yesOptionShrSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // callConfirmationDialog();
                /**
                 * Register patient using shrPatient
                 */
                shrPatient.setUuid(UUID.randomUUID().toString());
                try {
                    patientController.savePatient(shrPatient);
                    if (smartCardRecord != null) {
                        smartCardRecord.setUuid(UUID.randomUUID().toString());
                        smartCardRecord.setPersonUuid(shrPatient.getUuid());
                        try {
                            smartCardController.saveSmartCardRecord(smartCardRecord);
                        } catch (SmartCardController.SmartCardRecordSaveException e) {
                            Log.e(TAG, "Cannot save shr ", e);
                        }
                        KenyaEmrShrModel kenyaEmrShrModel = KenyaEmrShrMapper.createSHRModelFromJson(smartCardRecord.getPlainPayload());
                        KenyaEmrShrMapper.createNewObservationsAndEncountersFromShrModel(muzimaApplication, kenyaEmrShrModel, shrPatient);
                        Toast.makeText(getApplicationContext(), "Patient registered.", Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Patient registered");


                        Intent intent = new Intent(PatientsListActivity.this, PatientSummaryActivity.class);
                        /**
                         * todo check if this patient is registred in shr
                         * before opening PatientSummary activity
                         */
                        intent.putExtra("isRegisteredOnShr", true);
                        intent.putExtra(PatientSummaryActivity.PATIENT, shrPatient);
                        startActivity(intent);
                    } else
                        Log.e(TAG, "Unable to save smart card record");
                } catch (PatientController.PatientSaveException | KenyaEmrShrMapper.ShrParseException e) {
                    Log.e(TAG, "Error", e);
                }
                hideDialog();
            }
        });

        noOptionShrSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                negativeServerSearchResultNotifyAlertDialog.cancel();
                negativeServerSearchResultNotifyAlertDialog.dismiss();
            }
        });
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
        SmartCardController smartCardController = ((MuzimaApplication) getApplicationContext()).getSmartCardController();
        try {
            if (smartCardController.getSmartCardRecordByPersonUuid(patient.getUuid()) != null){
                intent.putExtra("isRegisteredOnShr", true);
                Log.e("TAG","isShr");
            }else {
                Log.e("TAG","isNotShr");

                intent.putExtra("isRegisteredOnShr", false);
            }
        } catch (SmartCardController.SmartCardRecordFetchException e) {
            e.printStackTrace();
        }
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
    public void onQueryTaskCancelled() {
        Log.e("TAG", "Cancelled...");
    }

    @Override
    public void onQueryTaskCancelled(Object errorDefinition) {
        Log.e("TAG", "Cancelled...");

    }

    public void invokeBarcodeScan() {
        BarCodeScannerIntentIntegrator scanIntegrator = new BarCodeScannerIntentIntegrator(this);
        scanIntegrator.initiateScan();
    }

    public void invokeShrApplication() {
        SmartCardIntentIntegrator shrIntegrator = new SmartCardIntentIntegrator(this);
        shrIntegrator.initiateCardRead();
        Toast.makeText(getApplicationContext(),"Opening Card Reader",Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent dataIntent) {
        /**
         * Confirm request code, to distinguish SHR card calls request from
         * barcode requests.
         */
        switch (requestCode) {
            case SMARTCARD_READ_REQUEST_CODE:
                readSmartCardWithDefaultWorkflow(requestCode, resultCode, dataIntent);
                break;
            case BARCODE_SCAN_REQUEST_CODE:
                IntentResult scanningResult = BarCodeScannerIntentIntegrator.parseActivityResult(requestCode, resultCode, dataIntent);
                if (scanningResult != null) {
                    intentBarcodeResults = true;
                    searchView.setQuery(scanningResult.getContents(), false);
                }else {
                    /**
                     * Card read was interrupted and failed
                     */
                    Snackbar.make(findViewById(R.id.patient_lists_layout), "Card read failed.", Snackbar.LENGTH_LONG)
                            .setAction("RETRY", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    invokeShrApplication();
                                }
                            })
                            .show();
                }
                break;
            default:
                break;
        }

    }

    public void readSmartCardWithDefaultWorkflow(int requestCode, int resultCode, Intent dataIntent) {


        SmartCardIntentResult cardReadIntentResult = null;

        try {
            cardReadIntentResult = SmartCardIntentIntegrator.parseActivityResult(requestCode, resultCode, dataIntent);
        } catch (Exception e) {
            Log.e(TAG,"Could not get result",e);
        }
        //todo remove logging code.
        Log.e("SHR_REQ", "Read Activity result invoked with value..." + cardReadIntentResult.isSuccessResult());

        if (cardReadIntentResult.isSuccessResult()) {
            /**
             * Card was read successfully and a result returned.
             */
            smartCardRecord = cardReadIntentResult.getSmartCardRecord();
            if (smartCardRecord != null) {
                intentShrResults = false;
                String shrPayload = smartCardRecord.getPlainPayload();
                Log.e("SHR_REQ", "Read Activity result invoked with value..." + shrPayload);

                try {
                    shrPatient = KenyaEmrShrMapper.extractPatientFromShrModel(shrPayload);
                    if(shrPatient != null) {
                        PatientIdentifier cardNumberIdentifier = shrPatient.getIdentifier(Constants.Shr.KenyaEmr.IdentifierType.CARD_SERIAL_NUMBER.name);

                        shrToMuzimaMatchingPatient = null;

                        if (cardNumberIdentifier == null) {
                            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
                            alertBuilder.setMessage("Could not find Card Serial number in shared health record")
                                    .setCancelable(true)
                                    .show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Searching Patient Locally", Toast.LENGTH_LONG).show();
                            executeLocalPatientSearchInBackgroundTask();
                        }
                    }


                } catch (KenyaEmrShrMapper.ShrParseException e) {
                    Log.e("EMR_IN", "EMR Error ", e);
                }

            }
        } else {
            /**
             * Card read was interrupted and failed
             */
            Snackbar.make(findViewById(R.id.patient_lists_layout), "Card read failed." + cardReadIntentResult.getErrors(), Snackbar.LENGTH_LONG)
                    .setAction("RETRY", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            invokeShrApplication();
                        }
                    })
                    .show();
        }
    }

    @Override
    public void onBackPressed() {
        patientAdapter.cancelBackgroundTask();
        if (getCallingActivity() == null) {
            launchDashboardActivity();
        } else {
            super.onBackPressed();
        }
    }

    private void launchDashboardActivity() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }

    private void launchCompleteFormsActivity() {
        Intent intent = new Intent(getApplicationContext(), FormsActivity.class);
        intent.putExtra(FormsActivity.KEY_FORMS_TAB_TO_OPEN, 3);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void hideDialog() {
        if (negativeServerSearchResultNotifyAlertDialog.isShowing())
            negativeServerSearchResultNotifyAlertDialog.cancel();
    }

    public class BackgroundPatientDownloadTask extends AsyncTask<Void, Void, Void> {

        Patient downloadedPatient = null;

        @Override
        protected Void doInBackground(Void... voids) {
            List<String> uuidsList = Collections.singletonList(shrToMuzimaMatchingPatient.getUuid());
            String[] uuids = {};
            int index = 0;
            for (String uuid : uuidsList) {
                assert uuids != null;
                uuids[index] = uuid;
            }
            muzimaSyncService.downloadPatients(uuids);
            return null;
        }

        @Override
        protected void onPostExecute(Void val) {
            super.onPostExecute(val);
            patientAdapter.reloadData();
            patientAdapter.notifyDataSetChanged();
        }
    }

    public class BackgroundPatientServerSearchQueryTask extends AsyncTask<Void, Void, Patient> {

        Patient foundPatient = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            preparedServerSearchNegativeResultHandlerDialog(getApplicationContext());
            Toast.makeText(getApplicationContext(),"Searching server.",Toast.LENGTH_LONG).show();
        }

        @Override
        protected Patient doInBackground(Void... voids) {
            MuzimaApplication muzimaApplication = (MuzimaApplication) getApplication();
            Patient patient = null;
            PatientController patientController = muzimaApplication.getPatientController();
            /**
             * Search for Patient locally without invoking search view
             */
            List<Patient> serverSearchResultPatients = new ArrayList<>();
            serverSearchResultPatients = patientController.searchPatientOnServer(shrPatient.getIdentifier(Constants.Shr.KenyaEmr.IdentifierType.CARD_SERIAL_NUMBER.name).getIdentifier());
            for (Patient searchResultPatient : serverSearchResultPatients) {
                if (searchResultPatient.getIdentifier(Constants.Shr.KenyaEmr.IdentifierType.CARD_SERIAL_NUMBER.name)
                        .equals(patient.getIdentifier(Constants.Shr.KenyaEmr.IdentifierType.CARD_SERIAL_NUMBER.name))) {
                    /**
                     * Search result contains patient obtained from PSmart
                     * close search and return patient.
                     */
                    shrToMuzimaMatchingPatient = searchResultPatient;
                    negativeServerSearchResultNotifyAlertDialog.setTitle("Search successful, " + patient.getGivenName() + " record found.");
                    /**
                     * TODO Display download optionDialog
                     *
                     */
                    executeDownloadPatientInBackgroundTask();
                    hideDialog();
                    break;
                }
            }
            return patient;
        }

        @Override
        protected void onPostExecute(Patient patient) {
            if (shrToMuzimaMatchingPatient != null) {
                /**
                 * shr patient found in mUzima data layer.
                 */
                try {
                    smartCardController.saveSmartCardRecord(smartCardRecord);
                } catch (SmartCardController.SmartCardRecordSaveException e) {
                    e.printStackTrace();
                }
            } else if(shrToMuzimaMatchingPatient == null){
                negativeServerSearchResultNotifyAlertDialog.show();
            }

        }
    }


    private class BackgroundPatientLocalSearchQueryTask extends AsyncTask<Void, Void, Patient> {

        Patient foundPatient = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            prepareLocalSearchNotifyDialog(getApplicationContext(),shrPatient);
        }

        @Override
        protected Patient doInBackground(Void... voids) {
            String searchTerm = shrPatient.getIdentifier(Constants.Shr.KenyaEmr.IdentifierType.CARD_SERIAL_NUMBER.name).getIdentifier();
            Log.e("SEARCHING", "Search TERM: " + Constants.Shr.KenyaEmr.IdentifierType.CARD_SERIAL_NUMBER.name + " : " + searchTerm);
            MuzimaApplication muzimaApplication = (MuzimaApplication) getApplication();
            Patient patient = null;
            PatientController patientController = muzimaApplication.getPatientController();
            CohortController cohortController = muzimaApplication.getCohortController();
            /**
             * Search for Patient locally without invoking search view
             */
            List<Patient> localSearchResultPatients = new ArrayList<>();
            try {
                //for (Cohort cohort : cohortController.getSyncedCohorts()) {
                localSearchResultPatients = patientController.searchPatientLocally(searchTerm, null);
                for (Patient searchResultPatient : localSearchResultPatients) {
                    PatientIdentifier identifier = searchResultPatient.getIdentifier(Constants.Shr.KenyaEmr.IdentifierType.CARD_SERIAL_NUMBER.name);
                    if (searchResultPatient.getIdentifier(Constants.Shr.KenyaEmr.IdentifierType.CARD_SERIAL_NUMBER.name).getIdentifier()
                            .equals(shrPatient.getIdentifier(Constants.Shr.KenyaEmr.IdentifierType.CARD_SERIAL_NUMBER.name).getIdentifier())) {
                        /**
                         * Search result contains patient obtained from PSmart
                         * close search and return patient.
                         */
                        foundPatient = searchResultPatient;

                        break;
                    }
                }
                //}
            } catch (PatientController.PatientLoadException e) {
                Log.e(TAG, "Unable to search for patient locally." + e.getMessage());
                e.printStackTrace();
            }
            return patient;
        }

        @Override
        protected void onPostExecute(Patient patient) {
            shrToMuzimaMatchingPatient = foundPatient;
            if (shrToMuzimaMatchingPatient == null) {
                /**
                 * Search data on server.
                 */
                localSearchResultNotifyAlertDialog.show();

               // executePatientServerSearchInBackgroundQueryTask();
            }

            if (shrToMuzimaMatchingPatient == null) {
            } else {
                Toast.makeText(getApplicationContext(), "Found Patient Shr Record " + shrPatient.getGivenName(), Toast.LENGTH_LONG);
                /**
                 * shr patient found in mUzima data layer.
                 *
                 */
                try {
                    try {
                        SmartCardRecord sm = smartCardController.getSmartCardRecordByPersonUuid(shrToMuzimaMatchingPatient.getUuid());
                        if (sm != null) {
                            smartCardRecord.setUuid(sm.getUuid());
                            smartCardController.updateSmartCardRecord(smartCardRecord);
                        } else {
                            smartCardRecord.setUuid(UUID.randomUUID().toString());
                            smartCardRecord.setPersonUuid(shrToMuzimaMatchingPatient.getUuid());
                            smartCardController.saveSmartCardRecord(smartCardRecord);
                        }
                    } catch (SmartCardController.SmartCardRecordSaveException | SmartCardController.SmartCardRecordFetchException e) {
                        Log.e(TAG, "Failed to write or update shr", e);
                    }
                    KenyaEmrShrModel kenyaEmrShrModel = KenyaEmrShrMapper.createSHRModelFromJson(smartCardRecord.getPlainPayload());
                    KenyaEmrShrMapper.createNewObservationsAndEncountersFromShrModel(muzimaApplication, kenyaEmrShrModel, shrToMuzimaMatchingPatient);
                } catch (KenyaEmrShrMapper.ShrParseException e) {
                    Log.e(TAG, "Failed to parse shr", e);
                }
                Intent intent = new Intent(PatientsListActivity.this, PatientSummaryActivity.class);
                intent.putExtra(PatientSummaryActivity.PATIENT, shrToMuzimaMatchingPatient);
                intent.putExtra("isRegisteredOnShr", true);
                startActivity(intent);
            }
        }

    }

    private void executePatientServerSearchInBackgroundQueryTask() {
        patientServerSearchQueryTask = new PatientsListActivity.BackgroundPatientServerSearchQueryTask();
        patientServerSearchQueryTask.execute();
    }

    private void executeLocalPatientSearchInBackgroundTask() {
        mBackgroundQueryTask = new BackgroundPatientLocalSearchQueryTask();
        mBackgroundQueryTask.execute();
    }

    private void executeDownloadPatientInBackgroundTask() {
        patientDownloadTask = new PatientsListActivity.BackgroundPatientDownloadTask();
        patientDownloadTask.execute();

    }
}