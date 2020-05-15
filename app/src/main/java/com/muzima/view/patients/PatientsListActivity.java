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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.app.ActionBarDrawerToggle;
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
import com.muzima.adapters.patients.PatientTagsListAdapter;
import com.muzima.adapters.patients.PatientsLocalSearchAdapter;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PatientIdentifier;
import com.muzima.api.model.PatientTag;
import com.muzima.api.service.SmartCardRecordService;
import com.muzima.controller.CohortController;
import com.muzima.controller.PatientController;
import com.muzima.controller.SmartCardController;
import com.muzima.model.location.MuzimaGPSLocation;
import com.muzima.model.shr.kenyaemr.KenyaEmrSHRModel;
import com.muzima.service.MuzimaGPSLocationService;
import com.muzima.service.MuzimaSyncService;
import com.muzima.service.TagPreferenceService;
import com.muzima.utils.Constants;
import com.muzima.utils.Fonts;
import com.muzima.utils.ThemeUtils;
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

import static android.view.MenuItem.SHOW_AS_ACTION_ALWAYS;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.muzima.utils.Constants.DataSyncServiceConstants;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants;
import static com.muzima.utils.Constants.SEARCH_STRING_BUNDLE_KEY;
import static com.muzima.utils.barcode.BarCodeScannerIntentIntegrator.BARCODE_SCAN_REQUEST_CODE;
import static com.muzima.utils.smartcard.SmartCardIntentIntegrator.SMARTCARD_READ_REQUEST_CODE;

import com.muzima.api.model.SmartCardRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PatientsListActivity extends BroadcastListenerActivity implements AdapterView.OnItemClickListener,
        ListAdapter.BackgroundListQueryTaskListener {

    public static final String COHORT_ID = "cohortId";
    public static final String COHORT_NAME = "cohortName";
    private static final String QUICK_SEARCH = "quickSearch";
    private ListView listView;
    private boolean quickSearch = false;
    private String cohortId = null;
    private PatientsLocalSearchAdapter patientAdapter;
    private FrameLayout progressBarContainer;
    private View noDataView;
    private String searchString;
    private FloatingActionButton fabSearchButton;
    private LinearLayout searchServerLayout;
    private SearchView searchView;
    private MenuItem searchMenuItem;
    private boolean intentBarcodeResults = false;

    private PatientController patientController;
    private MuzimaApplication muzimaApplication;
    private SmartCardController smartCardController;
    private SmartCardRecordService smartCardService;
    private SmartCardRecord smartCardRecord;
    private MuzimaSyncService muzimaSyncService;

    private Patient SHRPatient;
    private Patient SHRToMuzimaMatchingPatient;

    private AlertDialog negativeServerSearchResultNotifyAlertDialog;
    private AlertDialog localSearchResultNotifyAlertDialog;
    private AlertDialog registerSHRPatientLocallyDialog;

    private TextView searchDialogTextView;
    private Button yesOptionSHRSearchButton;
    private Button noOptionSHRSearchButton;

    private ProgressDialog serverSearchProgressDialog;
    private ProgressDialog patientRegistrationProgressDialog;

    private static final boolean DEFAULT_SHR_STATUS = false;
    private final ThemeUtils themeUtils = new ThemeUtils();
    private boolean isSHREnabled;

    private DrawerLayout mainLayout;
    private PatientTagsListAdapter tagsListAdapter;
    private TagPreferenceService tagPreferenceService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themeUtils.onCreate(this);
        super.onCreate(savedInstanceState);
        mainLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_patient_list, null);
        setContentView(mainLayout);
        Bundle intentExtras = getIntent().getExtras();

        muzimaApplication = (MuzimaApplication) getApplicationContext();

        if (intentExtras != null) {
            quickSearch = intentExtras.getBoolean(QUICK_SEARCH);
            cohortId = intentExtras.getString(COHORT_ID);
            String title = intentExtras.getString(COHORT_NAME);
            if (title != null)
                setTitle(title);
        }

        progressBarContainer = findViewById(R.id.progressbarContainer);
        setupNoDataView();
        setupListView(cohortId);

        fabSearchButton = findViewById(R.id.fab_search);
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

        searchServerLayout = findViewById(R.id.search_server_layout);

        Button searchServerBtn = findViewById(R.id.search_server_btn);
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

        muzimaSyncService = muzimaApplication.getMuzimaSyncService();
        patientController = muzimaApplication.getPatientController();
        serverSearchProgressDialog = new ProgressDialog(this);

        serverSearchProgressDialog.setCancelable(false);
        serverSearchProgressDialog.setIndeterminate(true);

        smartCardController = ((MuzimaApplication) getApplicationContext()).getSmartCardController();
        tagPreferenceService = new TagPreferenceService(this);
        initDrawer();
        logEvent("VIEW_CLIENT_LIST","{\"cohortId\":\""+cohortId+"\"}");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        int syncStatus = intent.getIntExtra(DataSyncServiceConstants.SYNC_STATUS, SyncStatusConstants.UNKNOWN_ERROR);
        int syncType = intent.getIntExtra(DataSyncServiceConstants.SYNC_TYPE, -1);
        int downloadCount = intent.getIntExtra(DataSyncServiceConstants.DOWNLOAD_COUNT_SECONDARY, 0);
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
                }
            } else if (syncStatus == SyncStatusConstants.SUCCESS && downloadCount > 0) {
                patientAdapter.reloadData();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.client_list, menu);

        setUpGeoMappingFeatureMenuItems(menu);
        setUpSHRFeatureMenuItems(menu);
        setUpSearchFeatureMenuItems(menu);
        super.onCreateOptionsMenu(menu);
        return true;
    }

    private void setUpGeoMappingFeatureMenuItems(Menu menu){
        MenuItem geoMappingFeatureMenuItem = menu.findItem(R.id.clients_geomapping);
        if(isGeoMappingFeatureEnabled()){
            geoMappingFeatureMenuItem.setVisible(true);
        } else {
            geoMappingFeatureMenuItem.setVisible(false);
        }
    }

    private void setUpSHRFeatureMenuItems(Menu menu){
        MenuItem shrCardItem = menu.findItem(R.id.scan_SHR_card);
        if(isSHRSettingEnabled()) {
            shrCardItem.setShowAsAction(SHOW_AS_ACTION_ALWAYS);
        } else {
            shrCardItem.setVisible(false);
        }
    }

    private void setUpSearchFeatureMenuItems(Menu menu){
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
    }

    private void activateRemoteAfterThreeCharacterEntered(String searchString) {
        if (searchString.trim().length() < 3)
            searchServerLayout.setVisibility(View.INVISIBLE);
        else
            searchServerLayout.setVisibility(View.VISIBLE);
    }

    // Confirmation dialog for confirming if the patient have an existing ID
    private void callConfirmationDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(PatientsListActivity.this);
        builder.setCancelable(true)
                .setIcon(ThemeUtils.getIconWarning(this))
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

            case R.id.scan_SHR_card:
                readSmartCard();
                return true;

            case R.id.menu_tags:
                if (mainLayout.isDrawerOpen(GravityCompat.END)) {
                    mainLayout.closeDrawer(GravityCompat.END);
                } else {
                    mainLayout.openDrawer(GravityCompat.END);
                }
                return true;

            case R.id.clients_geomapping:
                navigateToClientsLocationMap();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        themeUtils.onResume(this);
        if(isSHRSettingEnabled()){
            invalidateOptionsMenu();
            preparedServerSearchNegativeResultHandlerDialog();
        }
        if (!intentBarcodeResults)
            patientAdapter.reloadData();
        tagsListAdapter.reloadData();
    }

    private void navigateToClientsLocationMap() {
        Intent intent = new Intent(this, PatientsLocationMapActivity.class);
        startActivity(intent);
    }

    private void prepareLocalSearchNotifyDialog(Patient patient) {

        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = layoutInflater.inflate(R.layout.patient_shr_card_search_dialog, null);
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(PatientsListActivity.this);

        localSearchResultNotifyAlertDialog = alertBuilder
                .setView(dialogView)
                .create();

        localSearchResultNotifyAlertDialog.setCancelable(true);
        searchDialogTextView = dialogView.findViewById(R.id.patent_dialog_message_textview);
        yesOptionSHRSearchButton = dialogView.findViewById(R.id.yes_SHR_search_dialog);
        noOptionSHRSearchButton = dialogView.findViewById(R.id.no_SHR_search_dialog);
        searchDialogTextView
                .setText(String.format("Smartcard client %s, not in mUzima list. Search the client in server now ?", patient.getGivenName()));

        yesOptionSHRSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executePatientServerSearchInBackgroundQueryTask();
                localSearchResultNotifyAlertDialog.cancel();
                localSearchResultNotifyAlertDialog.dismiss();
                //todo integrate progress dialog.
                serverSearchProgressDialog.setMessage("Searching server...");
                serverSearchProgressDialog.show();
            }
        });

        noOptionSHRSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                localSearchResultNotifyAlertDialog.cancel();
                localSearchResultNotifyAlertDialog.dismiss();
                registerSHRPatientLocallyDialog.show();
            }
        });
    }

    private void preparedServerSearchNegativeResultHandlerDialog() {

        patientRegistrationProgressDialog = new ProgressDialog(this);
        patientRegistrationProgressDialog.setCancelable(false);
        patientRegistrationProgressDialog.setIndeterminate(true);
        patientRegistrationProgressDialog.setTitle(getString(R.string.registering_patient_message_title_text));

        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = layoutInflater.inflate(R.layout.patient_shr_card_search_dialog, null);
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(PatientsListActivity.this);

        negativeServerSearchResultNotifyAlertDialog = alertBuilder
                .setView(dialogView)
                .create();

        negativeServerSearchResultNotifyAlertDialog.setCancelable(true);
        searchDialogTextView = dialogView.findViewById(R.id.patent_dialog_message_textview);
        yesOptionSHRSearchButton = dialogView.findViewById(R.id.yes_SHR_search_dialog);
        noOptionSHRSearchButton = dialogView.findViewById(R.id.no_SHR_search_dialog);
        searchDialogTextView.setText(R.string.error_smartcard_client_not_in_server);

        yesOptionSHRSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                negativeServerSearchResultNotifyAlertDialog.dismiss();
                negativeServerSearchResultNotifyAlertDialog.cancel();
                patientRegistrationProgressDialog.show();
                executePatientRegistrationBackgroundTask();
                hideDialog();
            }
        });

        noOptionSHRSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                negativeServerSearchResultNotifyAlertDialog.cancel();
                negativeServerSearchResultNotifyAlertDialog.dismiss();
            }
        });
    }

    private void setupListView(String cohortId) {
        listView = findViewById(R.id.list);
        patientAdapter = new PatientsLocalSearchAdapter(this, R.layout.layout_list,
                ((MuzimaApplication) getApplicationContext()).getPatientController(), cohortId, getCurrentGPSLocation());

        patientAdapter.setBackgroundListQueryTaskListener(this);
        listView.setAdapter(patientAdapter);
        listView.setOnItemClickListener(this);
    }

    private MuzimaGPSLocation getCurrentGPSLocation(){
        MuzimaGPSLocationService muzimaLocationService = muzimaApplication.getMuzimaGPSLocationService();
        return muzimaLocationService.getLastKnownGPSLocation();
    }

    private void setupNoDataView() {

        noDataView = findViewById(R.id.no_data_layout);

        TextView noDataMsgTextView = findViewById(R.id.no_data_msg);
        noDataMsgTextView.setText(getResources().getText(R.string.info_client_local_search_not_found));

        TextView noDataTipTextView = findViewById(R.id.no_data_tip);
        noDataTipTextView.setText(R.string.hint_client_local_search);

        noDataMsgTextView.setTypeface(Fonts.roboto_bold_condensed(this));
        noDataTipTextView.setTypeface(Fonts.roboto_medium(this));
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
    public void onQueryTaskCancelled() {
        Log.e(getClass().getSimpleName(), "Cancelled...");
    }

    @Override
    public void onQueryTaskCancelled(Object errorDefinition) {
        Log.e(getClass().getSimpleName(), "Cancelled...");
    }

    private void invokeBarcodeScan() {
        BarCodeScannerIntentIntegrator scanIntegrator = new BarCodeScannerIntentIntegrator(this);
        scanIntegrator.initiateScan();
    }

    private void readSmartCard() {
        SmartCardIntentIntegrator SHRIntegrator = new SmartCardIntentIntegrator(this);
        SHRIntegrator.initiateCardRead();
        Toast.makeText(getApplicationContext(), "Opening Card Reader", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent dataIntent) {
        switch (requestCode) {
            case SMARTCARD_READ_REQUEST_CODE:
                processSmartCardReadResult(requestCode, resultCode, dataIntent);
                serverSearchProgressDialog.dismiss();
                serverSearchProgressDialog.cancel();
                break;
            case BARCODE_SCAN_REQUEST_CODE:
                IntentResult scanningResult = BarCodeScannerIntentIntegrator.parseActivityResult(requestCode, resultCode, dataIntent);
                if (scanningResult != null) {
                    intentBarcodeResults = true;
                    searchView.setQuery(scanningResult.getContents(), false);
                } else {
                    Snackbar.make(findViewById(R.id.patient_lists_layout), "Card read failed.", Snackbar.LENGTH_LONG)
                            .setAction("RETRY", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    readSmartCard();
                                }
                            }).show();
                }
                break;
            default:
                break;
        }
    }

    private void processSmartCardReadResult(int requestCode, int resultCode, Intent dataIntent) {
        SmartCardIntentResult cardReadIntentResult = null;

        try {
            cardReadIntentResult = SmartCardIntentIntegrator.parseActivityResult(requestCode, resultCode, dataIntent);
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "Could not get result", e);
        }
        if (cardReadIntentResult == null) {
            Toast.makeText(getApplicationContext(), "Card Read Failed", Toast.LENGTH_LONG).show();
            return;
        }

        if (cardReadIntentResult.isSuccessResult()) {
            smartCardRecord = cardReadIntentResult.getSmartCardRecord();
            if (smartCardRecord != null) {
                String SHRPayload = smartCardRecord.getPlainPayload();
                if(!SHRPayload.equals("") && !SHRPayload.isEmpty()) {
                    try {
                        SHRPatient = KenyaEmrShrMapper.extractPatientFromSHRModel(muzimaApplication, SHRPayload);
                        if (SHRPatient != null) {
                            PatientIdentifier cardNumberIdentifier = SHRPatient.getIdentifier(Constants.Shr.KenyaEmr.PersonIdentifierType.CARD_SERIAL_NUMBER.name);

                            SHRToMuzimaMatchingPatient = null;

                            if (cardNumberIdentifier == null) {
                                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
                                alertBuilder.setMessage("Could not find Card Serial number in shared health record")
                                        .setCancelable(true).show();
                            } else {
                                Toast.makeText(getApplicationContext(), "Searching Patient Locally", Toast.LENGTH_LONG).show();
                                prepareRegisterLocallyDialog();
                                prepareLocalSearchNotifyDialog(SHRPatient);
                                executeLocalPatientSearchInBackgroundTask();
                            }
                        } else {
                            Toast.makeText(getApplicationContext(), "This card seems to be blank", Toast.LENGTH_LONG).show();
                        }
                    } catch (KenyaEmrShrMapper.ShrParseException e) {
                        Log.e("EMR_IN", "EMR Error ", e);
                    }
                }
            }
        } else {
            Snackbar.make(findViewById(R.id.patient_lists_layout), "Card read failed." + cardReadIntentResult.getErrors(), Snackbar.LENGTH_LONG)
                    .setAction("RETRY", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            readSmartCard();
                        }
                    }).show();
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

    private void hideDialog() {
        if (negativeServerSearchResultNotifyAlertDialog.isShowing())
            negativeServerSearchResultNotifyAlertDialog.cancel();
    }

    class BackgroundPatientDownloadTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            String[] uuids = {SHRToMuzimaMatchingPatient.getUuid()};
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

    class BackgroundPatientServerSearchQueryTask extends AsyncTask<Void, Void, Patient> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            preparedServerSearchNegativeResultHandlerDialog();
            Toast.makeText(getApplicationContext(), "Searching server.", Toast.LENGTH_LONG).show();
        }

        @Override
        protected Patient doInBackground(Void... voids) {
            MuzimaApplication muzimaApplication = (MuzimaApplication) getApplication();
            Patient patient = null;
            PatientController patientController = muzimaApplication.getPatientController();
            List<Patient> serverSearchResultPatients = new ArrayList<>();
            serverSearchResultPatients = patientController.searchPatientOnServer(SHRPatient.getIdentifier(Constants.Shr.KenyaEmr.PersonIdentifierType.CARD_SERIAL_NUMBER.name).getIdentifier());

            if(serverSearchResultPatients.size() == 1){
                patientRegistrationProgressDialog.dismiss();
                patientRegistrationProgressDialog.cancel();
                SHRToMuzimaMatchingPatient = serverSearchResultPatients.get(0);
                executeDownloadPatientInBackgroundTask();
                hideDialog();
            }
            return patient;
        }

        @Override
        protected void onPostExecute(Patient patient) {
            serverSearchProgressDialog.cancel();
            if (SHRToMuzimaMatchingPatient != null) {
                try {
                    smartCardController.saveSmartCardRecord(smartCardRecord);
                } catch (SmartCardController.SmartCardRecordSaveException e) {
                    e.printStackTrace();
                }
            } else if (SHRToMuzimaMatchingPatient == null) {
                negativeServerSearchResultNotifyAlertDialog.show();
            }
        }
    }

    private class BackgroundPatientLocalSearchQueryTask extends AsyncTask<Void, Void, Patient> {

        Patient foundPatient = null;

        @Override
        protected Patient doInBackground(Void... voids) {
            String searchTerm = SHRPatient.getIdentifier(Constants.Shr.KenyaEmr.PersonIdentifierType.CARD_SERIAL_NUMBER.name).getIdentifier();
            MuzimaApplication muzimaApplication = (MuzimaApplication) getApplication();
            Patient patient = null;
            PatientController patientController = muzimaApplication.getPatientController();
            CohortController cohortController = muzimaApplication.getCohortController();
            List<Patient> localSearchResultPatients = new ArrayList<>();
            try {
                //for (Cohort cohort : cohortController.getSyncedCohorts()) {
                localSearchResultPatients = patientController.searchPatientLocally(searchTerm, null);
                for (Patient searchResultPatient : localSearchResultPatients) {
                    PatientIdentifier identifier = searchResultPatient.getIdentifier(Constants.Shr.KenyaEmr.PersonIdentifierType.CARD_SERIAL_NUMBER.name);
                    if (searchResultPatient.getIdentifier(Constants.Shr.KenyaEmr.PersonIdentifierType.CARD_SERIAL_NUMBER.name).getIdentifier()
                            .equals(SHRPatient.getIdentifier(Constants.Shr.KenyaEmr.PersonIdentifierType.CARD_SERIAL_NUMBER.name).getIdentifier())) {
                        foundPatient = searchResultPatient;

                        break;
                    }
                }
            } catch (PatientController.PatientLoadException e) {
                Log.e(getClass().getSimpleName(), "Unable to search for patient locally." + e.getMessage());
                e.printStackTrace();
            }
            return patient;
        }

        @Override
        protected void onPostExecute(Patient patient) {
            SHRToMuzimaMatchingPatient = foundPatient;
            if (SHRToMuzimaMatchingPatient == null) {
                localSearchResultNotifyAlertDialog.show();
            }

            if (SHRToMuzimaMatchingPatient != null) {
                Toast.makeText(getApplicationContext(), "Found Patient SHR Record " + SHRPatient.getGivenName(), Toast.LENGTH_LONG);
                try {
                    try {
                        SmartCardRecord sm = smartCardController.getSmartCardRecordByPersonUuid(SHRToMuzimaMatchingPatient.getUuid());
                        if (sm != null) {
                            smartCardRecord.setUuid(sm.getUuid());
                            smartCardRecord.setPersonUuid(sm.getPersonUuid());
                            smartCardController.updateSmartCardRecord(smartCardRecord);
                        } else {
                            smartCardRecord.setUuid(UUID.randomUUID().toString());
                            smartCardRecord.setPersonUuid(SHRToMuzimaMatchingPatient.getUuid());
                            smartCardController.saveSmartCardRecord(smartCardRecord);
                        }
                    } catch (SmartCardController.SmartCardRecordSaveException | SmartCardController.SmartCardRecordFetchException e) {
                        Log.e(getClass().getSimpleName(), "Failed to write or update SHR", e);
                    }
                    KenyaEmrSHRModel kenyaEmrSHRModel = KenyaEmrShrMapper.createSHRModelFromJson(smartCardRecord.getPlainPayload());
                    KenyaEmrShrMapper.createNewObservationsAndEncountersFromShrModel(muzimaApplication, kenyaEmrSHRModel, SHRToMuzimaMatchingPatient);
                } catch (KenyaEmrShrMapper.ShrParseException e) {
                    Log.e(getClass().getSimpleName(), "Failed to parse SHR", e);
                }
                Intent intent = new Intent(PatientsListActivity.this, PatientSummaryActivity.class);
                intent.putExtra(PatientSummaryActivity.PATIENT, SHRToMuzimaMatchingPatient);
                startActivity(intent);
            }
        }
    }

    private class RegisterPatientBackgroundTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {

            SHRPatient.setUuid(UUID.randomUUID().toString());
            try {
                patientController.savePatient(SHRPatient);
                KenyaEmrShrMapper.createAndSaveRegistrationPayloadForPatient(muzimaApplication,SHRPatient);
            } catch (PatientController.PatientSaveException e) {
                e.printStackTrace();
            }
            if (smartCardRecord != null) {
                smartCardRecord.setUuid(UUID.randomUUID().toString());
                smartCardRecord.setPersonUuid(SHRPatient.getUuid());
                try {
                    smartCardController.saveSmartCardRecord(smartCardRecord);
                } catch (SmartCardController.SmartCardRecordSaveException e) {
                    Log.e(getClass().getSimpleName(), "Cannot save SHR ", e);
                }
                KenyaEmrSHRModel kenyaEmrSHRModel = null;
                try {
                    kenyaEmrSHRModel = KenyaEmrShrMapper.createSHRModelFromJson(smartCardRecord.getPlainPayload());
                } catch (KenyaEmrShrMapper.ShrParseException e) {
                    e.printStackTrace();
                }
                try {
                    KenyaEmrShrMapper.createNewObservationsAndEncountersFromShrModel(muzimaApplication, kenyaEmrSHRModel, SHRPatient);
                } catch (KenyaEmrShrMapper.ShrParseException e) {
                    e.printStackTrace();
                }
                Log.e(getClass().getSimpleName(), "Patient registered");
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if (patientRegistrationProgressDialog != null){
                patientRegistrationProgressDialog.dismiss();
                patientRegistrationProgressDialog.cancel();
            }

            Intent intent = new Intent(PatientsListActivity.this, PatientSummaryActivity.class);
            intent.putExtra(PatientSummaryActivity.PATIENT, SHRPatient);
            startActivity(intent);
            super.onPostExecute(aBoolean);
        }
    }

    private void prepareRegisterLocallyDialog() {

        patientRegistrationProgressDialog = new ProgressDialog(this);
        patientRegistrationProgressDialog.setCancelable(false);
        patientRegistrationProgressDialog.setIndeterminate(true);
        patientRegistrationProgressDialog.setTitle(getString(R.string.registering_patient_message_title_text));

        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = layoutInflater.inflate(R.layout.patient_shr_card_search_dialog, null);
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(PatientsListActivity.this);

        registerSHRPatientLocallyDialog = alertBuilder
                .setView(dialogView)
                .create();

        registerSHRPatientLocallyDialog.setCancelable(true);
        searchDialogTextView = dialogView.findViewById(R.id.patent_dialog_message_textview);
        yesOptionSHRSearchButton = dialogView.findViewById(R.id.yes_SHR_search_dialog);
        noOptionSHRSearchButton = dialogView.findViewById(R.id.no_SHR_search_dialog);
        searchDialogTextView.setText(R.string.client_registration_question_text);

        yesOptionSHRSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerSHRPatientLocallyDialog.dismiss();
                registerSHRPatientLocallyDialog.cancel();
                patientRegistrationProgressDialog.show();
                executePatientRegistrationBackgroundTask();
            }
        });

        noOptionSHRSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerSHRPatientLocallyDialog.cancel();
                registerSHRPatientLocallyDialog.dismiss();
            }
        });
    }

    private void executePatientServerSearchInBackgroundQueryTask() {
        BackgroundPatientServerSearchQueryTask patientServerSearchQueryTask = new BackgroundPatientServerSearchQueryTask();
        patientServerSearchQueryTask.execute();
    }

    private void executeLocalPatientSearchInBackgroundTask() {
        BackgroundPatientLocalSearchQueryTask mBackgroundQueryTask = new BackgroundPatientLocalSearchQueryTask();
        mBackgroundQueryTask.execute();
    }

    private void executeDownloadPatientInBackgroundTask() {
        BackgroundPatientDownloadTask patientDownloadTask = new BackgroundPatientDownloadTask();
        patientDownloadTask.execute();
    }

    private void executePatientRegistrationBackgroundTask(){
        RegisterPatientBackgroundTask patientRegistrationTask = new RegisterPatientBackgroundTask();
        patientRegistrationTask.execute();
    }

    private boolean isSHRSettingEnabled(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(muzimaApplication.getApplicationContext());
        return preferences.getBoolean(muzimaApplication.getResources().getString(R.string.preference_enable_shr_key),PatientSummaryActivity.DEFAULT_SHR_STATUS);
    }

    private boolean isGeoMappingFeatureEnabled(){
        return muzimaApplication.getMuzimaSettingController().isGeoMappingEnabled();
    }

    private void initDrawer() {
        initSelectedTags();
        ListView tagsDrawerList = findViewById(R.id.tags_list);
        tagsDrawerList.setEmptyView(findViewById(R.id.tags_no_data_msg));
        tagsListAdapter = new PatientTagsListAdapter(this, R.layout.item_tags_list, patientController);
        tagsDrawerList.setAdapter(tagsListAdapter);
        tagsDrawerList.setOnItemClickListener(tagsListAdapter);
        ActionBarDrawerToggle actionbarDrawerToggle = new ActionBarDrawerToggle(this, mainLayout,
                R.drawable.ic_labels, R.string.hint_drawer_open, R.string.hint_drawer_close) {

            /**
             * Called when a drawer has settled in a completely closed state.
             */
            public void onDrawerClosed(View view) {
                String title = getResources().getString(R.string.general_client_list);
                getSupportActionBar().setTitle(title);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                mainLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }

            /**
             * Called when a drawer has settled in a completely open state.
             */
            public void onDrawerOpened(View drawerView) {
                String title = getResources().getString(R.string.general_tags);
                getSupportActionBar().setTitle(title);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                mainLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            }
        };
        mainLayout.setDrawerListener(actionbarDrawerToggle);
        mainLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        TextView tagsNoDataMsg = findViewById(R.id.tags_no_data_msg);
        tagsNoDataMsg.setTypeface(Fonts.roboto_bold_condensed(this));
    }

    private void initSelectedTags() {
        List<String> selectedTagsInPref = tagPreferenceService.getPatientSelectedTags();
        List<PatientTag> allTags = null;
        try {
            allTags = patientController.getAllTags();
        } catch (PatientController.PatientLoadException e) {
            Log.e(getClass().getSimpleName(), "Error occurred while get all tags from local repository", e);
        }
        List<PatientTag> selectedTags = new ArrayList<>();

        if (selectedTagsInPref != null) {
            for (PatientTag tag : allTags) {
                if (selectedTagsInPref.contains(tag.getName())) {
                    selectedTags.add(tag);
                }
            }
        }
        patientController.setSelectedTags(selectedTags);
    }

    @Override
    protected void onPause() {
        super.onPause();
        storeSelectedTags();
    }

    private void storeSelectedTags() {
        Set<String> newSelectedTags = new HashSet<>();
        for (PatientTag selectedTag : patientController.getSelectedTags()) {
            newSelectedTags.add(selectedTag.getName());
        }
        tagPreferenceService.savePatientSelectedTags(newSelectedTags);
    }
}