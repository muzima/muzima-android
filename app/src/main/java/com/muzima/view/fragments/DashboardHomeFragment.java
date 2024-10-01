/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */
package com.muzima.view.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.RecyclerAdapter;
import com.muzima.adapters.patients.PatientAdapterHelper;
import com.muzima.adapters.patients.PatientsLocalSearchAdapter;
import com.muzima.api.model.FormTemplate;
import com.muzima.api.model.MuzimaSetting;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PersonName;
import com.muzima.api.model.SetupConfigurationTemplate;
import com.muzima.controller.FormController;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.controller.PatientController;
import com.muzima.controller.SetupConfigurationController;
import com.muzima.model.AvailableForm;
import com.muzima.model.CohortFilter;
import com.muzima.model.events.BottomSheetToggleEvent;
import com.muzima.model.events.CloseBottomSheetEvent;
import com.muzima.model.events.CohortFilterActionEvent;
import com.muzima.model.events.ShowCohortFilterEvent;
import com.muzima.model.events.UploadedFormDataEvent;
import com.muzima.model.location.MuzimaGPSLocation;
import com.muzima.service.MuzimaGPSLocationService;
import com.muzima.util.JsonUtils;
import com.muzima.utils.FormUtils;
import com.muzima.utils.MuzimaPreferences;
import com.muzima.utils.StringUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.utils.smartcard.SmartCardIntentIntegrator;
import com.muzima.view.forms.FormViewIntent;
import com.muzima.view.patients.PatientSummaryActivity;
import com.muzima.view.barcode.BarcodeCaptureActivity;
import com.muzima.view.forms.FormsWithDataActivity;
import com.muzima.view.forms.RegistrationFormsActivity;
import com.muzima.view.patients.SimilarPatientsSearchActivity;
import com.muzima.view.patients.PatientsSearchActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONArray;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static android.view.View.VISIBLE;
import static com.muzima.adapters.forms.FormsPagerAdapter.TAB_COMPLETE;
import static com.muzima.adapters.forms.FormsPagerAdapter.TAB_INCOMPLETE;
import static com.muzima.util.Constants.ServerSettings.COHORT_FILTER_DERIVED_CONCEPT_MAP;
import static com.muzima.utils.Constants.STANDARD_DATE_FORMAT;
import static com.muzima.utils.StringUtils.EMPTY;
import static com.muzima.utils.smartcard.SmartCardIntentIntegrator.SMARTCARD_READ_REQUEST_CODE;
import static com.muzima.util.Constants.ServerSettings.PATIENT_ASSIGNMENT_FORM_UUID_SETTING;

public class DashboardHomeFragment extends Fragment implements RecyclerAdapter.BackgroundListQueryTaskListener,
        PatientAdapterHelper.PatientListClickListener {
    private static final int RC_BARCODE_CAPTURE = 9001;
    private TextView incompleteFormsTextView;
    private TextView completeFormsTextView;
    private View incompleteFormsView;
    private View completeFormsView;
    private View searchPatientEditText;
    private View searchByBarCode;
    private View searchBySmartCard;
    private View filterActionView;
    private View childContainer;
    private View patientSearchView;
    private TextView providerNameTextView;
    private RecyclerView recyclerView;
    private View noDataView;
    private FloatingActionButton fabSearchButton;
    private ProgressBar progressBar;
    private AppBarLayout appBarLayout;
    private TextView filterLabelTextView;
    private ProgressBar filterProgressBar;
    private boolean bottomSheetFilterVisible;
    private PatientsLocalSearchAdapter patientSearchAdapter;
    private CohortFilterActionEvent latestCohortFilterActionEvent;
    private RelativeLayout patientSearchBy;
    private Activity mActivity;

    private EditText patientName;
    private RadioButton radioMale;
    private RadioButton radioFemale;
    private EditText ageEditText;
    private EditText birthDateEditText;

    public static final String SELECTED_PATIENT_UUIDS_KEY = "selectedPatientUuids";

    ActionMode actionMode;
    boolean actionModeActive;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard_home, container, false);
        initializeResources(view);
        setupListView(view);
        setupNoDataView(view);
        initPatientRegistrationSearchView(inflater);
        return view;
    }

    @Override
    public void onResume(){
        super.onResume();
        loadFormsCount();
        if(latestCohortFilterActionEvent != null){
            cohortFilterEvent(latestCohortFilterActionEvent);
        } else if(patientSearchAdapter != null){
            patientSearchAdapter.reloadData();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof Activity){
            mActivity =(Activity) context;
        }
    }

    private void setupListView(View view) {
        recyclerView = view.findViewById(R.id.list);
        Context context = mActivity.getApplicationContext();

        List<String> cohortUuids = new ArrayList<>();
        SetupConfigurationTemplate activeSetupConfig = null;

        try {
            activeSetupConfig = ((MuzimaApplication) mActivity.getApplicationContext()).getSetupConfigurationController().getActiveSetupConfigurationTemplate();
            if (multipleConfigsSupported() && activeSetupConfig != null) {
                // We restrict all patients list to only those in active config cohorts
                List<Object> objects = JsonUtils.readAsObjectList(activeSetupConfig.getConfigJson(), "$['config']['cohorts']");
                for (Object obj : objects) {
                    net.minidev.json.JSONObject cohort = (net.minidev.json.JSONObject) obj;
                    cohortUuids.add(cohort.get("uuid").toString());
                }
            }
        } catch (SetupConfigurationController.SetupConfigurationFetchException e) {
            Log.e(getClass().getSimpleName(), "Cannot load active config", e);
        }

        patientSearchAdapter = new PatientsLocalSearchAdapter(context,
                ((MuzimaApplication) context).getPatientController(), null, null, getCurrentGPSLocation(),  ((MuzimaApplication) context).getMuzimaSettingController(), cohortUuids);

        patientSearchAdapter.setBackgroundListQueryTaskListener(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireActivity().getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(patientSearchAdapter);
        recyclerView.setOnScrollChangeListener(new View.OnScrollChangeListener(){
            @Override
            public void onScrollChange(View recyclerView, int dx, int dy, int var4, int var5) {
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                if (!patientSearchAdapter.isLoading() && !patientSearchAdapter.isLastPage()) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0
                            && totalItemCount < patientSearchAdapter.getTotalPatientCount()) {
                        patientSearchAdapter.loadNextPage();
                    }
                }
            }
        });
        patientSearchAdapter.setPatientListClickListener(this);
    }

    private boolean multipleConfigsSupported(){
        return ((MuzimaApplication) mActivity.getApplicationContext()).getMuzimaSettingController().isMultipleConfigsSupported();
    }

    private void initPatientRegistrationSearchView(LayoutInflater inflater){
        patientSearchView = inflater.inflate(R.layout.patient_search_dialog,null);
        patientName = patientSearchView.findViewById(R.id.patient_name);
        radioMale = patientSearchView.findViewById(R.id.radio_male);
        radioFemale = patientSearchView.findViewById(R.id.radio_female);
        ageEditText = patientSearchView.findViewById(R.id.patient_age);
        birthDateEditText = patientSearchView.findViewById(R.id.patient_birthdate);
    }

    private void initializeResources(View view) {
        incompleteFormsTextView = view.findViewById(R.id.dashboard_forms_incomplete_forms_count_view);
        completeFormsTextView = view.findViewById(R.id.dashboard_forms_complete_forms_count_view);
        searchPatientEditText = view.findViewById(R.id.dashboard_main_patient_search_view);
        searchByBarCode = view.findViewById(R.id.search_barcode_view);
        searchBySmartCard = view.findViewById(R.id.search_smart_card_view);
        filterActionView = view.findViewById(R.id.favourite_list_container);
        fabSearchButton = view.findViewById(R.id.fab_search);
        progressBar = view.findViewById(R.id.patient_loader_progress_bar);
        providerNameTextView = view.findViewById(R.id.dashboard_home_welcome_message_text_view);
        filterLabelTextView = view.findViewById(R.id.dashboard_home_filter_text_view);
        childContainer = view.findViewById(R.id.dashboard_home_fragment_child_container);
        appBarLayout = view.findViewById(R.id.dashboard_home_app_bar);
        incompleteFormsView = view.findViewById(R.id.dashboard_forms_incomplete_forms_view);
        completeFormsView =   view.findViewById(R.id.dashboard_forms_complete_forms_view);
        filterProgressBar = view.findViewById(R.id.patient_list_filtering_progress_bar);
        patientSearchBy = view.findViewById(R.id.patient_search_by);

        filterProgressBar.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);

        if(isSHRFeatureEnabled() || isBarcodeSearchEnabled()) {
            patientSearchBy.setVisibility(View.VISIBLE);
        }else{
            patientSearchBy.setVisibility(View.GONE);
        }

        if(((MuzimaApplication) mActivity.getApplicationContext()).getMuzimaSettingController().isPatientRegistrationEnabled()) {
            fabSearchButton.setVisibility(View.VISIBLE);
        }else{
            fabSearchButton.setVisibility(View.GONE);
        }

        if(isBarcodeSearchEnabled()){
            searchByBarCode.setVisibility(VISIBLE);
        }


        incompleteFormsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bottomSheetFilterVisible) {
                    closeBottomSheet();
                } else {
                    launchFormDataList(true);
                }
            }
        });

        completeFormsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bottomSheetFilterVisible) {
                    closeBottomSheet();
                } else {
                    launchFormDataList(false);
                }
            }
        });

        filterActionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bottomSheetFilterVisible) {
                    closeBottomSheet();
                } else {
                    EventBus.getDefault().post(new ShowCohortFilterEvent());
                    bottomSheetFilterVisible = true;
                    childContainer.setBackgroundColor(mActivity.getResources().getColor(R.color.hint_text_grey_opaque));
                }
            }
        });

        searchPatientEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchPatientsSearchActivity(EMPTY);
            }
        });

        childContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeBottomSheet();
            }
        });

        searchBySmartCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                readSmartCard();
            }
        });
        if(isSHRFeatureEnabled()) {
            searchBySmartCard.setVisibility(View.VISIBLE);
        }

        searchByBarCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                invokeBarcodeScan();
            }
        });

        fabSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isRegistrationSearchEnabled = ((MuzimaApplication)mActivity.getApplication()).getMuzimaSettingController().isPatientSearchBeforeRegistrationEnabled();
                if(isRegistrationSearchEnabled) {
                    patientSearchDialog();
                } else {
                    callRegisterPatientConfirmationDialog();
                }
            }
        });

        if (((MuzimaApplication) mActivity.getApplicationContext()).getAuthenticatedUser() != null)
            providerNameTextView.setText(String.format(Locale.getDefault(), "%s, %s",
                mActivity.getResources().getString(R.string.hello_general),
                ((MuzimaApplication) mActivity.getApplicationContext()).getAuthenticatedUser().getUsername()));

    }

    private void loadFormsCount() {
        try {
            long incompleteForms = ((MuzimaApplication) mActivity.getApplicationContext()).getFormController().countAllIncompleteForms();
            long completeForms = ((MuzimaApplication) mActivity.getApplicationContext()).getFormController().countAllCompleteForms();
            if(incompleteForms == 0){
                incompleteFormsView.setBackground(mActivity.getResources().getDrawable(R.drawable.rounded_corners_green));
            }else if(incompleteForms>0 && incompleteForms<=5){
                incompleteFormsView.setBackground(mActivity.getResources().getDrawable(R.drawable.rounded_corners_orange));
            }else{
                incompleteFormsView.setBackground(mActivity.getResources().getDrawable(R.drawable.rounded_corners_red));
            }
            incompleteFormsTextView.setText(String.valueOf(incompleteForms));

            if(completeForms == 0){
                completeFormsView.setBackground(mActivity.getResources().getDrawable(R.drawable.rounded_corners_green));
            }else if(completeForms>0 && completeForms<=5){
                completeFormsView.setBackground(mActivity.getResources().getDrawable(R.drawable.rounded_corners_orange));
            }else{
                completeFormsView.setBackground(mActivity.getResources().getDrawable(R.drawable.rounded_corners_red));
            }
            completeFormsTextView.setText(String.valueOf(completeForms));
        } catch (FormController.FormFetchException e) {
            Log.e(getClass().getSimpleName(), "Could not count complete and incomplete forms",e);
        }
    }

    private void setupNoDataView(View view) {
        noDataView = view.findViewById(R.id.no_data_layout);
        TextView noDataMsgTextView = view.findViewById(R.id.no_data_msg);
        noDataMsgTextView.setText(mActivity.getResources().getText(R.string.info_no_client_available));
    }

    private boolean isSHRFeatureEnabled(){
        return ((MuzimaApplication) mActivity.getApplication()).getMuzimaSettingController().isSHREnabled();
    }

    private void showRegistrationFormsMissingAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        AlertDialog dialog = builder.setCancelable(false)
                .setIcon(ThemeUtils.getIconWarning(mActivity.getApplicationContext()))
                .setTitle(mActivity.getResources().getString(R.string.general_alert))
                .setMessage(mActivity.getResources().getString(R.string.general_registration_form_missing_message))
                .setPositiveButton(R.string.general_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .create();
        dialog.show();
    }

    private void launchPatientsSearchActivity(String searchString) {
        patientSearchAdapter.cancelBackgroundTask();
        Intent intent = new Intent(mActivity, PatientsSearchActivity.class);
        intent.putExtra(PatientsSearchActivity.SEARCH_STRING, searchString);
        startActivity(intent);
    }

    // Confirmation dialog for confirming if the patient have an existing ID
    private void callRegisterPatientConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setCancelable(true)
                .setIcon(ThemeUtils.getIconWarning(mActivity.getApplicationContext()))
                .setTitle(mActivity.getResources().getString(R.string.title_logout_confirm))
                .setMessage(mActivity.getResources().getString(R.string.confirm_patient_id_exists))
                .setPositiveButton(R.string.general_yes, launchPatientsList())
                .setNegativeButton(R.string.general_no, launchClientRegistrationFormIfPossible()).create().show();
    }

    private boolean areAllFieldsEntered(){

        return  !(StringUtils.isEmpty(String.valueOf(patientName.getText())))
                && (radioMale.isChecked() || radioFemale.isChecked())
                && (!(StringUtils.isEmpty(String.valueOf(ageEditText.getText())))
                    || !(StringUtils.isEmpty(String.valueOf(birthDateEditText.getText()))));
    }

    private void resetPatientSearchDialogFields(){
        patientName.setText("");
        radioMale.setChecked(false);
        radioFemale.setChecked(false);
        ageEditText.setText("");
        birthDateEditText.setText("");
    }

    private void patientSearchDialog() {
        if(patientSearchView.getParent() != null){
            ((ViewGroup) patientSearchView.getParent()).removeView(patientSearchView);
            resetPatientSearchDialogFields();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setView(patientSearchView);
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();

        Button proceedButton = patientSearchView.findViewById(R.id.proceed_pt_registration_search);
        patientName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(areAllFieldsEntered()){
                    proceedButton.setEnabled(true);
                } else {
                    proceedButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        radioMale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(areAllFieldsEntered()){
                    proceedButton.setEnabled(true);
                } else {
                    proceedButton.setEnabled(false);
                }
            }
        });
        radioFemale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(areAllFieldsEntered()){
                    proceedButton.setEnabled(true);
                } else {
                    proceedButton.setEnabled(false);
                }
            }
        });


        final Date[] birthDate = new Date[1];
        ageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(areAllFieldsEntered()){
                    proceedButton.setEnabled(true);
                } else {
                    proceedButton.setEnabled(false);
                }

                if(!StringUtils.isEmpty(ageEditText.getText().toString())) {
                    int ageInYears = Integer.parseInt(ageEditText.getText().toString());

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        LocalDate date = LocalDate.of(LocalDate.now().getYear() - ageInYears, 1, 1);
                        birthDate[0] = Date.from(Instant.from(ZonedDateTime.of(date.atStartOfDay(), ZoneId.systemDefault())));
                    } else {
                        birthDate[0] = new Date(ageInYears, 0, 1);
                    }
                    SimpleDateFormat sf = new SimpleDateFormat(STANDARD_DATE_FORMAT);
                    birthDateEditText.setText(sf.format(birthDate[0]));
                } else if(ageEditText.hasFocus()){
                    birthDateEditText.setText(EMPTY);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });



        birthDateEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(areAllFieldsEntered()){
                    proceedButton.setEnabled(true);
                } else {
                    proceedButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        birthDateEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ageEditText.clearFocus();
                DatePickerDialog datePickerDialog = new DatePickerDialog(mActivity);
                datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
                if (birthDate[0] != null) {
                    datePickerDialog.updateDate((1900 + birthDate[0].getYear()), birthDate[0].getMonth(), birthDate[0].getDate());
                }

                datePickerDialog.setOnDateSetListener(new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, day);
                        birthDate[0] = calendar.getTime();

                        SimpleDateFormat sf = new SimpleDateFormat(STANDARD_DATE_FORMAT);
                        birthDateEditText.setText(sf.format(birthDate[0]));
                        ageEditText.setText("");
                    }
                });
                datePickerDialog.show();
            }
        });


        proceedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Patient patient = new Patient();
                patient.setUuid(String.valueOf(UUID.randomUUID()));

                PersonName name = new PersonName();
                String enteredName = String.valueOf(patientName.getText());
                String[] splittedNames = enteredName.split("\\s+");
                if(splittedNames.length > 0){
                    name.setFamilyName(splittedNames[0]);
                }
                if(splittedNames.length > 2){
                    name.setGivenName(splittedNames[2]);
                    name.setMiddleName(splittedNames[1]);
                } else if(splittedNames.length > 1){
                    name.setGivenName(splittedNames[1]);
                }

                List personNames = new ArrayList<PersonName>();
                personNames.add(name);
                patient.setNames(new ArrayList<>(personNames));

                patient.setBirthdate(birthDate[0]);
                if(!StringUtils.isEmpty(ageEditText.getText().toString())) {
                    patient.setBirthdateEstimated(true);
                }

                if(radioMale.isChecked()){
                    patient.setGender("M");
                } else if(radioFemale.isChecked()){
                    patient.setGender("F");
                }

                Intent intent = new Intent(mActivity.getApplicationContext(), SimilarPatientsSearchActivity.class);
                intent.putExtra(PatientSummaryActivity.PATIENT, patient);
                startActivity(intent);
                dialog.dismiss();
                progressBar.setVisibility(VISIBLE);
            }
        });

        Button cancelButton = patientSearchView.findViewById(R.id.cancel_pt_registration_search);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
    }

    private Dialog.OnClickListener launchPatientsList() {
        return new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                launchPatientsSearchActivity(EMPTY);
            }
        };
    }

    private Dialog.OnClickListener launchClientRegistrationFormIfPossible() {
        if (FormUtils.getRegistrationForms(((MuzimaApplication) mActivity.getApplicationContext()).getFormController()).isEmpty()) {
            return new Dialog.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    showRegistrationFormsMissingAlert();
                }
            };
        } else {
            return new Dialog.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(mActivity.getApplicationContext(), RegistrationFormsActivity.class));
                }
            };
        }
    }

    private void invokeBarcodeScan() {
        Intent intent;
        intent = new Intent(mActivity.getApplicationContext(), BarcodeCaptureActivity.class);
        intent.putExtra(BarcodeCaptureActivity.AutoFocus, true);
        intent.putExtra(BarcodeCaptureActivity.UseFlash, false);

        startActivityForResult(intent, RC_BARCODE_CAPTURE);
    }

    private void readSmartCard() {
        if (mActivity == null) return;
        SmartCardIntentIntegrator SHRIntegrator = new SmartCardIntentIntegrator(mActivity);
        SHRIntegrator.initiateCardRead();
        Toast.makeText(mActivity.getApplicationContext(), "Opening Card Reader", Toast.LENGTH_LONG).show();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent dataIntent) {
        switch (requestCode) {
            case SMARTCARD_READ_REQUEST_CODE:
                break;
            case RC_BARCODE_CAPTURE:
                if (resultCode == CommonStatusCodes.SUCCESS) {
                    if (dataIntent != null) {
                        Barcode barcode = dataIntent.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                        launchPatientsSearchActivity(barcode.displayValue);
                    } else {
                        Log.d(getClass().getSimpleName(), "No barcode captured, intent data is null");
                    }
                } else {
                    Log.d(getClass().getSimpleName(), "No barcode captured, intent data is null "+CommonStatusCodes.getStatusCodeString(resultCode));
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, dataIntent);
        }
    }

    private void launchFormDataList(boolean isIncompleteFormsData) {
        Intent intent = new Intent(mActivity, FormsWithDataActivity.class);
        if (isIncompleteFormsData) {
            intent.putExtra(FormsWithDataActivity.KEY_FORMS_TAB_TO_OPEN, TAB_INCOMPLETE);
        } else {
            intent.putExtra(FormsWithDataActivity.KEY_FORMS_TAB_TO_OPEN, TAB_COMPLETE);
        }
        startActivity(intent);
    }

    private void closeBottomSheet() {
        EventBus.getDefault().post(new CloseBottomSheetEvent());
        bottomSheetFilterVisible = false;
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            if (!EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().register(this);
            }
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(),"Encountered an exception",e);
        }
    }

    @Subscribe
    public void bottomNavigationToggleEvent(BottomSheetToggleEvent event) {
        if (event.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            childContainer.setVisibility(View.VISIBLE);
            appBarLayout.setBackgroundColor(mActivity.getResources().getColor(R.color.hint_text_grey_opaque));
        } else if (event.getState() == BottomSheetBehavior.STATE_HIDDEN) {
            childContainer.setVisibility(View.GONE);
            if (MuzimaPreferences.getIsLightModeThemeSelectedPreference(mActivity.getApplicationContext()))
                appBarLayout.setBackgroundColor(mActivity.getResources().getColor(R.color.primary_white));
            else
                appBarLayout.setBackgroundColor(mActivity.getResources().getColor(R.color.primary_black));
        }
    }

    @Subscribe
    public void cohortFilterEvent(final CohortFilterActionEvent event) {

        latestCohortFilterActionEvent = event;
        bottomSheetFilterVisible = false;
        updateCohortFilterLabel(event);

        List<CohortFilter> filters = event.getFilters();
        MuzimaSetting muzimaSetting = null;
        try {
            muzimaSetting = ((MuzimaApplication) mActivity.getApplicationContext()).getMuzimaSettingController().getSettingByProperty(COHORT_FILTER_DERIVED_CONCEPT_MAP);
        } catch (MuzimaSettingController.MuzimaSettingFetchException e) {
            Log.e(getClass().getSimpleName(),"Encountered a setting fetch exception ",e);
        }

        if(muzimaSetting != null && !StringUtils.isEmpty(muzimaSetting.getValueString())) {
            patientSearchAdapter.filterByCohortsWithDerivedConceptFilter(filters);
        }else{
            List<String> cohortUuids = new ArrayList<>();
            for(CohortFilter cohortFilter : filters){
                if(cohortFilter.getCohortWithFilter() != null && !cohortUuids.contains(cohortFilter.getCohortWithFilter().getCohort().getUuid()))
                    cohortUuids.add(cohortFilter.getCohortWithFilter().getCohort().getUuid());
            }
            patientSearchAdapter.filterByCohorts(cohortUuids);
        }
    }

    @Subscribe
    public void uploadedFormDataEvent(final UploadedFormDataEvent event){
        loadFormsCount();
    }

    private void updateCohortFilterLabel(CohortFilterActionEvent event) {
        if (event.getFilters().size() == 1) {
            if (event.getFilters().get(0).getCohortWithFilter() == null)
                filterLabelTextView.setText(mActivity.getResources().getString(R.string.general_all_clients));

            else if(((MuzimaApplication) mActivity.getApplicationContext()).getMuzimaSettingController().isSameDerivedConceptUsedToFilterMoreThanOneCohort(event.getFilters().get(0).getCohortWithFilter().getDerivedConceptUuid()))
                filterLabelTextView.setText(event.getFilters().get(0).getCohortWithFilter().getCohort().getName()+" - "+event.getFilters().get(0).getCohortWithFilter().getDerivedObservationFilter());
            else if(((MuzimaApplication) mActivity.getApplicationContext()).getMuzimaSettingController().isSameConceptUsedToFilterMoreThanOneCohort(event.getFilters().get(0).getCohortWithFilter().getConceptUuid()))
                filterLabelTextView.setText(event.getFilters().get(0).getCohortWithFilter().getCohort().getName()+" - "+event.getFilters().get(0).getCohortWithFilter().getObservationFilter());
            else if(!StringUtils.isEmpty(event.getFilters().get(0).getCohortWithFilter().getDerivedObservationFilter()))
                filterLabelTextView.setText(event.getFilters().get(0).getCohortWithFilter().getDerivedObservationFilter());
            else if(!StringUtils.isEmpty(event.getFilters().get(0).getCohortWithFilter().getObservationFilter()))
                filterLabelTextView.setText(event.getFilters().get(0).getCohortWithFilter().getObservationFilter());
            else
                filterLabelTextView.setText(event.getFilters().get(0).getCohortWithFilter().getCohort().getName());

        } else if (event.getFilters().isEmpty())
            filterLabelTextView.setText(mActivity.getResources().getString(R.string.general_all_clients));
        else if (event.getFilters().size() == 1 && event.getFilters().get(0) != null && event.getFilters().get(0).getCohortWithFilter().getCohort() == null)
            filterLabelTextView.setText(mActivity.getResources().getString(R.string.general_all_clients));
        else if (event.getFilters().size() > 1) {
            filterLabelTextView.setText(mActivity.getResources().getString(R.string.general_filtered_list));
        }

        for (CohortFilter filter : event.getFilters()) {
            if (filter.getCohortWithFilter().getCohort() == null && filter.isSelected())
                filterLabelTextView.setText(mActivity.getResources().getString(R.string.general_all_clients));
        }
    }

    private MuzimaGPSLocation getCurrentGPSLocation() {
        MuzimaGPSLocationService muzimaLocationService = ((MuzimaApplication) mActivity.getApplicationContext())
                .getMuzimaGPSLocationService();
        return muzimaLocationService.getLastKnownGPSLocation();
    }

    private boolean isBarcodeSearchEnabled(){
        MuzimaSettingController muzimaSettingController = ((MuzimaApplication) mActivity.getApplicationContext()).getMuzimaSettingController();
        return muzimaSettingController.isBarcodeEnabled();
    }

    @Override
    public void onQueryTaskStarted() {
        filterProgressBar.setVisibility(View.VISIBLE);
        noDataView.setVisibility(View.GONE);
    }

    @Override
    public void onQueryTaskFinish() {
        if(patientSearchAdapter.hasLessItemsThanMaxSizeForLoadedPages() && !patientSearchAdapter.isLastPage()
                && ((MuzimaApplication) mActivity.getApplicationContext()).getPatientController().getSelectedTagUuids().size()>0){
            patientSearchAdapter.loadNextPage();
        } else {
            filterProgressBar.setVisibility(View.GONE);
            if (patientSearchAdapter.isEmpty()) {
                noDataView.setVisibility(VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                recyclerView.setVisibility(VISIBLE);
                noDataView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onQueryTaskCancelled() {
        filterProgressBar.setVisibility(View.GONE);
        if(patientSearchAdapter.isEmpty())
            noDataView.setVisibility(VISIBLE);
    }

    @Override
    public void onQueryTaskCancelled(Object errorDefinition) {
        filterProgressBar.setVisibility(View.GONE);
        if(patientSearchAdapter.isEmpty())
            noDataView.setVisibility(VISIBLE);
    }

    @Override
    public void onItemClick(View view, int position) {
        if (bottomSheetFilterVisible) {
            closeBottomSheet();
        } else {
            patientSearchAdapter.cancelBackgroundTask();
            Patient patient = patientSearchAdapter.getPatient(position);
            if(actionModeActive){
                patientSearchAdapter.toggleSelection(view,position);
                int numOfSelectedPatients = patientSearchAdapter.getSelectedPatientsUuids().size();
                if (numOfSelectedPatients == 0 && actionModeActive) {
                    actionMode.finish();
                }
                actionMode.setTitle(String.valueOf(numOfSelectedPatients));
            } else if (patient != null){
                Intent intent = new Intent(mActivity.getApplicationContext(), PatientSummaryActivity.class);
                intent.putExtra(PatientSummaryActivity.PATIENT_UUID, patient.getUuid());
                startActivity(intent);
            }
        }
    }

    @Override
    public void onItemLongClick(View view, int position) {
        patientSearchAdapter.toggleSelection(view,position);
        if (!actionModeActive) {
            actionMode = mActivity.startActionMode(new MultiplePatientsSelectionActionModeCallback());
            actionModeActive = true;
        }
        int numOfSelectedPatients = patientSearchAdapter.getSelectedPatientsUuids().size();
        if (numOfSelectedPatients == 0 && actionModeActive) {
            actionMode.finish();
        }
        actionMode.setTitle(String.valueOf(numOfSelectedPatients));
    }

    final class MultiplePatientsSelectionActionModeCallback  implements ActionMode.Callback{
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mActivity.getMenuInflater().inflate(R.menu.actionmode_menu_assign, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.menu_assign:
                    //Launch index assignment form and avail selected patient UUIDs to the form.
                    boolean formUuidSettingAndFormAvailable = false;
                    try {
                        // The uuid of the form shall be specified by a server side setting
                        // The form shall load details of the patient whose UUIDs were selected (by use of repeating section if multiple forms were selected)
                        // The structure of the form should enable the app to generate a paylod with full assignment details,
                        // and the server side module should be able to process the payload

                        MuzimaSettingController muzimaSettingController = ((MuzimaApplication) mActivity.getApplication()).getMuzimaSettingController();
                        MuzimaSetting formUuidSetting = muzimaSettingController.getSettingByProperty(PATIENT_ASSIGNMENT_FORM_UUID_SETTING);


                        if(formUuidSetting==null || StringUtils.isEmpty(formUuidSetting.getValueString())) {
                            Toast.makeText(mActivity.getApplicationContext(),R.string.assignment_form_uuid_missing_warning, Toast.LENGTH_LONG).show();
                        }  else {
                            FormController formController = ((MuzimaApplication) mActivity.getApplicationContext()).getFormController();
                            String formUuid = formUuidSetting.getValueString();
                            AvailableForm assignmentForm = ((MuzimaApplication) mActivity.getApplicationContext()).getFormController().getAvailableFormByFormUuid(formUuid);
                            String patientUuid = patientSearchAdapter.getSelectedPatientsUuids().get(0);
                            Patient patient = ((MuzimaApplication) mActivity.getApplicationContext()).getPatientController().getPatientByUuid(patientUuid);

                            FormTemplate formTemplate = formController.getFormTemplateByUuid(assignmentForm.getFormUuid());
                            if (formTemplate == null) {
                                Toast.makeText(((MuzimaApplication) mActivity.getApplicationContext()),R.string.assignment_form_not_downloaded_warning, Toast.LENGTH_LONG).show();
                            } else {
                                formUuidSettingAndFormAvailable = true;
                                FormViewIntent intent = new FormViewIntent(mActivity, assignmentForm, patient, false);
                                intent.putExtra(FormViewIntent.FORM_COMPLETION_STATUS_INTENT, FormViewIntent.FORM_COMPLETION_STATUS_RECOMMENDED);
                                intent.putExtra(SELECTED_PATIENT_UUIDS_KEY, getSelectedPatientsUuids());
                                startActivityForResult(intent, FormsWithDataActivity.FORM_VIEW_ACTIVITY_RESULT);
                            }
                        }
                    } catch (FormController.FormFetchException e) {
                        Log.e(getClass().getSimpleName(), "Could not open form",e);
                    } catch (PatientController.PatientLoadException e) {
                        Log.e(getClass().getSimpleName(), "Could not load patient",e);
                    } catch (MuzimaSettingController.MuzimaSettingFetchException e) {
                        Toast.makeText(mActivity.getApplicationContext(),R.string.assignment_form_uuid_missing_warning, Toast.LENGTH_LONG).show();
                        Log.e(getClass().getSimpleName(), "Could not get setting",e);
                    }
                    if(formUuidSettingAndFormAvailable) {
                        endActionMode();
                    }
            }
            return true;
        }

        private String getSelectedPatientsUuids(){
            JSONArray jsonArray = new JSONArray();
            for (String uuid : patientSearchAdapter.getSelectedPatientsUuids()){
                jsonArray.put(uuid);
            }
            return jsonArray.toString();
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            actionModeActive = false;
            patientSearchAdapter.resetSelectedPatientsUuids();
            patientSearchAdapter.notifyDataSetChanged();
        }
    }

    public void endActionMode() {
        if (this.actionMode != null) {
            this.actionMode.finish();
        }
    }
}
