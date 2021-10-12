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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.legacy.app.ActionBarDrawerToggle;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.adapters.patients.PatientTagsListAdapter;
import com.muzima.adapters.patients.PatientsLocalSearchAdapter;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PatientIdentifier;
import com.muzima.api.model.PatientTag;
import com.muzima.api.model.SmartCardRecord;
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
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.StringUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.utils.smartcard.KenyaEmrShrMapper;
import com.muzima.utils.smartcard.SmartCardIntentIntegrator;
import com.muzima.utils.smartcard.SmartCardIntentResult;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.MainDashboardActivity;

import com.muzima.view.barcode.BarcodeCaptureActivity;
import com.muzima.view.forms.FormsWithDataActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.muzima.utils.Constants.DataSyncServiceConstants;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SyncStatusConstants;
import static com.muzima.utils.Constants.SEARCH_STRING_BUNDLE_KEY;
import static com.muzima.utils.smartcard.SmartCardIntentIntegrator.SMARTCARD_READ_REQUEST_CODE;

public class PatientsSearchActivity extends BroadcastListenerActivity implements AdapterView.OnItemClickListener,
        ListAdapter.BackgroundListQueryTaskListener {
    private static final int RC_BARCODE_CAPTURE = 9001;
    private static final String TAG = "PatientsSearchActivity";
    public static final String COHORT_ID = "cohortId";
    public static final String COHORT_NAME = "cohortName";
    public static final String SEARCH_STRING = "searchString";
    private ListView listView;
    private String cohortId = null;
    private PatientsLocalSearchAdapter patientAdapter;
    private FrameLayout progressBarContainer;
    private View noDataView;
    private String searchString;
    private LinearLayout searchServerLayout;
    private SearchView searchMenuItem;

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
    private Toolbar toolbar;

    private ProgressDialog serverSearchProgressDialog;
    private ProgressDialog patientRegistrationProgressDialog;

    private static final boolean DEFAULT_SHR_STATUS = false;
    private boolean isSHREnabled;
    private boolean searchViewClosed;
    private DrawerLayout mainLayout;
    private PatientTagsListAdapter tagsListAdapter;
    private TagPreferenceService tagPreferenceService;
    private final LanguageUtil languageUtil = new LanguageUtil();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.getInstance().onCreate(this,true);
        languageUtil.onCreate(this);
        super.onCreate(savedInstanceState);
        mainLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_patient_list, null);
        setContentView(mainLayout);
        Bundle intentExtras = getIntent().getExtras();

        setTitle(R.string.general_clients);

        muzimaApplication = (MuzimaApplication) getApplicationContext();
        toolbar = findViewById(R.id.patient_list_toolbar);
        getSupportActionBar().setCustomView(toolbar);

        if (intentExtras != null) {
            searchString = intentExtras.getString(SEARCH_STRING);
            cohortId = intentExtras.getString(COHORT_ID);
            String title = intentExtras.getString(COHORT_NAME);
            if (title != null)
                setTitle(title);
        }

        progressBarContainer = findViewById(R.id.progressbarContainer);
        setupListView(cohortId);

        searchServerLayout = findViewById(R.id.search_server_layout);

        Button searchServerBtn = findViewById(R.id.search_server_btn);
        searchServerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PatientsSearchActivity.this, PatientRemoteSearchListActivity.class);
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
        logEvent("VIEW_CLIENT_LIST", "{\"cohortId\":\"" + cohortId + "\"}");

        setupNoDataView();
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
                    intent.putExtra(PatientSummaryActivity.PATIENT_UUID, patient.getUuid());
                    intent.putExtra(PatientSummaryActivity.CALLING_ACTIVITY, PatientsSearchActivity.class.getSimpleName());
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
        setUpSearchFeatureMenuItems(menu);
        return true;
    }

    private void setUpSearchFeatureMenuItems(Menu menu) {
        searchMenuItem = (SearchView) menu.findItem(R.id.search).getActionView();
        searchMenuItem.setMinimumWidth(toolbar.getWidth());
        searchMenuItem.setQueryHint(getString(R.string.general_search_patient));
        searchMenuItem.setIconifiedByDefault(false);

        searchMenuItem.setQuery(searchString,false);
        searchMenuItem.requestFocus();
        activateRemoteAfterThreeCharacterEntered(searchString);
        patientAdapter.search(searchString);

        searchMenuItem.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                searchString = s;
                activateRemoteAfterThreeCharacterEntered(s);
                patientAdapter.search(s);
                return true;
            }
        });

        searchMenuItem.setEnabled(true);

    }

    private void activateRemoteAfterThreeCharacterEntered(String searchString) {
        if (StringUtils.isEmpty(searchString) || searchString.trim().length() < 3)
            searchServerLayout.setVisibility(View.INVISIBLE);
        else
            searchServerLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            launchDashboardActivity();
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        languageUtil.onResume(this);
        if (isSHRSettingEnabled()) {
            invalidateOptionsMenu();
            preparedServerSearchNegativeResultHandlerDialog();
        }
//        tagsListAdapter.reloadData();
    }


    private void prepareLocalSearchNotifyDialog(Patient patient) {

        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = layoutInflater.inflate(R.layout.patient_shr_card_search_dialog, null);
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(PatientsSearchActivity.this);

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
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(PatientsSearchActivity.this);

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
        listView.setDividerHeight(0);
        patientAdapter = new PatientsLocalSearchAdapter(this, R.layout.layout_list,
                ((MuzimaApplication) getApplicationContext()).getPatientController(), new ArrayList<String>(){{add(cohortId);}},
                getCurrentGPSLocation());

        patientAdapter.setBackgroundListQueryTaskListener(this);
        listView.setAdapter(patientAdapter);
        listView.setOnItemClickListener(this);
    }

    private MuzimaGPSLocation getCurrentGPSLocation() {
        MuzimaGPSLocationService muzimaLocationService = muzimaApplication.getMuzimaGPSLocationService();
        return muzimaLocationService.getLastKnownGPSLocation();
    }

    private void setupNoDataView() {
        try {
            noDataView = findViewById(R.id.no_data_layout);
            TextView noDataMsgTextView = findViewById(R.id.no_data_msg);

            TextView noDataTipTextView = findViewById(R.id.no_data_tip);

            int localPatientsCount = patientController.countAllPatients();
            if (localPatientsCount == 0) {
                noDataMsgTextView.setText(getResources().getText(R.string.info_no_client_available_locally));
                noDataTipTextView.setText(R.string.hint_client_remote_search);
            } else {
                noDataMsgTextView.setText(getResources().getText(R.string.info_client_local_search_not_found));
                noDataTipTextView.setText(R.string.hint_client_local_search);
            }

        } catch (PatientController.PatientLoadException ex) {
            Toast.makeText(PatientsSearchActivity.this, R.string.error_patient_search, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        patientAdapter.cancelBackgroundTask();
        Patient patient = patientAdapter.getItem(position);
        Intent intent = new Intent(this, PatientSummaryActivity.class);
        intent.putExtra(PatientSummaryActivity.CALLING_ACTIVITY, PatientsSearchActivity.class.getSimpleName());
        intent.putExtra(PatientSummaryActivity.PATIENT_UUID, patient.getUuid());
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
        Intent intent;
        intent = new Intent(getApplicationContext(), BarcodeCaptureActivity.class);
        intent.putExtra(BarcodeCaptureActivity.AutoFocus, true);
        intent.putExtra(BarcodeCaptureActivity.UseFlash, false);

        startActivityForResult(intent, RC_BARCODE_CAPTURE);
    }

    private void readSmartCard() {
        SmartCardIntentIntegrator SHRIntegrator = new SmartCardIntentIntegrator(this);
        SHRIntegrator.initiateCardRead();
        Toast.makeText(getApplicationContext(), "Opening Card Reader", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent dataIntent) {
        super.onActivityResult(requestCode, resultCode, dataIntent);
        switch (requestCode) {
            case SMARTCARD_READ_REQUEST_CODE:
                processSmartCardReadResult(requestCode, resultCode, dataIntent);
                serverSearchProgressDialog.dismiss();
                serverSearchProgressDialog.cancel();
                break;
            case RC_BARCODE_CAPTURE:
               invokeBarcodeScan();
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
                if (!SHRPayload.equals("") && !SHRPayload.isEmpty()) {
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

    private void launchDashboardActivity() {
        Intent intent = new Intent(getApplicationContext(), MainDashboardActivity.class);
        startActivity(intent);
        finish();
    }

    private void launchCompleteFormsActivity() {
        Intent intent = new Intent(getApplicationContext(), FormsWithDataActivity.class);
        intent.putExtra(FormsWithDataActivity.KEY_FORMS_TAB_TO_OPEN, 3);
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

            if (serverSearchResultPatients.size() == 1) {
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
                Intent intent = new Intent(PatientsSearchActivity.this, PatientSummaryActivity.class);
                intent.putExtra(PatientSummaryActivity.CALLING_ACTIVITY, PatientsSearchActivity.class.getSimpleName());
                intent.putExtra(PatientSummaryActivity.PATIENT_UUID, SHRToMuzimaMatchingPatient.getUuid());
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
                KenyaEmrShrMapper.createAndSaveRegistrationPayloadForPatient(muzimaApplication, SHRPatient);
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
            if (patientRegistrationProgressDialog != null) {
                patientRegistrationProgressDialog.dismiss();
                patientRegistrationProgressDialog.cancel();
            }

            Intent intent = new Intent(PatientsSearchActivity.this, PatientSummaryActivity.class);
            intent.putExtra(PatientSummaryActivity.PATIENT_UUID, SHRPatient.getUuid());
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
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(PatientsSearchActivity.this);

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

    private void executePatientRegistrationBackgroundTask() {
        RegisterPatientBackgroundTask patientRegistrationTask = new RegisterPatientBackgroundTask();
        patientRegistrationTask.execute();
    }

    private boolean isSHRSettingEnabled() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(muzimaApplication.getApplicationContext());
        return preferences.getBoolean(muzimaApplication.getResources().getString(R.string.preference_enable_shr_key), PatientSummaryActivity.DEFAULT_SHR_STATUS);
    }

    private boolean isGeoMappingFeatureEnabled() {
        try {
            return muzimaApplication.getMuzimaSettingController().isGeoMappingEnabled();
        }catch(StringIndexOutOfBoundsException e){
            Log.e(getClass().getSimpleName(),"Encountered an exception while getting geomapping feature setting");
            return false;
        }
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
                getActionBar().setTitle(title);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                mainLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }

            /**
             * Called when a drawer has settled in a completely open state.
             */
            public void onDrawerOpened(View drawerView) {
                String title = getResources().getString(R.string.general_tags);
                getActionBar().setTitle(title);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                mainLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            }
        };
        mainLayout.setDrawerListener(actionbarDrawerToggle);
        mainLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        TextView tagsNoDataMsg = findViewById(R.id.tags_no_data_msg);
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
