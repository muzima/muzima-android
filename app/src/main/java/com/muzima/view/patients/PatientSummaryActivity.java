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
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabItem;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textview.MaterialTextView;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.forms.ClientDynamicObsFormsAdapter;
import com.muzima.adapters.patients.ClientSummaryPagerAdapter;
import com.muzima.api.model.Concept;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PersonAddress;
import com.muzima.controller.FormController;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.controller.PatientController;
import com.muzima.model.SingleObsForm;
import com.muzima.model.events.ClientSummaryObservationSelectedEvent;
import com.muzima.model.events.CloseSingleFormEvent;
import com.muzima.model.events.ReloadObservationsDataEvent;
import com.muzima.model.location.MuzimaGPSLocation;
import com.muzima.model.observation.ConceptWithObservations;
import com.muzima.service.MuzimaGPSLocationService;
import com.muzima.utils.DateUtils;
import com.muzima.utils.FormUtils;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.StringUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.utils.smartcard.SmartCardIntentIntegrator;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.MainDashboardActivity;
import com.muzima.view.forms.FormsWithDataActivity;
import com.muzima.view.relationship.RelationshipsListActivity;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.muzima.adapters.forms.FormsPagerAdapter.TAB_COMPLETE;
import static com.muzima.adapters.forms.FormsPagerAdapter.TAB_INCOMPLETE;

public class PatientSummaryActivity extends BroadcastListenerActivity implements ClientDynamicObsFormsAdapter.DatePickerClickedListener, ClientDynamicObsFormsAdapter.DateValuePickerClickedListener {
    private static final String TAG = "PatientSummaryActivity";
    public static final String PATIENT = "patient";
    public static final String PATIENT_UUID = "patient_uuid";
    public static final String CALLING_ACTIVITY = "calling_activity_key";
    public static final boolean DEFAULT_SHR_STATUS = false;
    private static final boolean DEFAULT_RELATIONSHIP_STATUS = false;
    private TextView patientNameTextView;
    private ImageView patientGenderImageView;
    private TextView dobTextView;
    private TextView identifierTextView;
    private TextView gpsAddressTextView;
    private TextView ageTextView;
    private TextView bottomSheetConceptTitleTextView;
    private TextView incompleteFormsCountView;
    private TextView completeFormsCountView;
    private String patientUuid;
    private Patient patient;
    private Concept selectedBottomSheetConcept;
    private final LanguageUtil languageUtil = new LanguageUtil();
    private ClientDynamicObsFormsAdapter clientDynamicObsFormsAdapter;
    private final List<SingleObsForm> singleObsFormsList = new ArrayList<>();
    private ViewPager2 viewPager;
    private View incompleteFormsView;
    private View completeFormsView;
    private boolean isSingleElementEnabled;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeUtils.getInstance().onCreate(this,true);
        languageUtil.onCreate(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_summary);
        initializeResources();
        loadPatientData();

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        ClientSummaryPagerAdapter clientSummaryPager = new ClientSummaryPagerAdapter(this, tabLayout.getTabCount(), patientUuid, isSingleElementEnabled);
        viewPager.setAdapter(clientSummaryPager);
        viewPager.setUserInputEnabled(false);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            EventBus.getDefault().register(this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFormsCount();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_client_summary, menu);
        MenuItem shrMenu = menu.findItem(R.id.menu_shr);
        MenuItem relationshipMenu = menu.findItem(R.id.menu_relationship);
        MenuItem locationMenu = menu.findItem(R.id.menu_location_item);

        MuzimaSettingController muzimaSettingController = ((MuzimaApplication) getApplicationContext()).getMuzimaSettingController();
        boolean isSHRSettingEnabled = muzimaSettingController.isSHREnabled();
        boolean isRelationshipEnabled = muzimaSettingController.isRelationshipEnabled();
        boolean isGeomappingEnabled = muzimaSettingController.isGeoMappingEnabled();

        if(!isSHRSettingEnabled)
            shrMenu.setVisible(false);

        if(!isRelationshipEnabled)
            relationshipMenu.setVisible(false);

        if(!isGeomappingEnabled)
            locationMenu.setVisible(false);

        locationMenu.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.general_launching_map_message), Toast.LENGTH_SHORT).show();
                navigateToClientsLocationMap();
                return true;
            }
        });

        shrMenu.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                readSmartCard();
                return true;
            }
        });

        relationshipMenu.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                navigateToRelationshipsView();
                return true;
            }
        });
        return true;
    }

    private void readSmartCard() {
        SmartCardIntentIntegrator SHRIntegrator = new SmartCardIntentIntegrator(PatientSummaryActivity.this);
        SHRIntegrator.initiateCardRead();
        Toast.makeText(getApplicationContext(), getResources().getString(R.string.general_opening_card_reader), Toast.LENGTH_LONG).show();
    }

    private void navigateToClientsLocationMap() {
        Intent intent = new Intent(getApplicationContext(), PatientsLocationMapActivity.class);
        startActivity(intent);
    }
    private void navigateToRelationshipsView() {
        Intent intent = new Intent(this, RelationshipsListActivity.class);
        intent.putExtra(PATIENT,patient);
        startActivity(intent);
    }
    @Override
    public void onUserInteraction() {
        ((MuzimaApplication) getApplication()).restartTimer();
        super.onUserInteraction();
    }

    @Subscribe
    public void clientSummaryObservationSelectedEvent(ClientSummaryObservationSelectedEvent event) {
        ConceptWithObservations conceptWrapper = event.getConceptWithObservations();
        if (conceptWrapper.getConcept().isCoded() || conceptWrapper.getConcept().isSet() || conceptWrapper.getConcept().isPrecise()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(true)
                    .setMessage("This data type is not support, consult admin.")
                    .show();
        } else {
            selectedBottomSheetConcept = conceptWrapper.getConcept();
            openDialog();
        }
    }

    private void openDialog() {
        View view = getLayoutInflater().inflate(R.layout.activity_client_summary_bottom_sheet_dialog, null);
        Dialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(view);

        bottomSheetConceptTitleTextView = view.findViewById(R.id.cohort_name_text_view);
        View addReadingActionView = view.findViewById(R.id.general_add_reading_button);
        View cancelBottomSheetActionView = view.findViewById(R.id.close_summary_bottom_sheet_view);
        View saveBottomSheetEntriesActionView = view.findViewById(R.id.client_summary_save_action_bottom_sheet);
        clientDynamicObsFormsAdapter = new ClientDynamicObsFormsAdapter(getApplicationContext(), singleObsFormsList, this,this);
        RecyclerView singleObsFormsRecyclerViews = view.findViewById(R.id.client_summary_single_obs_form_recycler_view);
        singleObsFormsRecyclerViews.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        singleObsFormsRecyclerViews.setAdapter(clientDynamicObsFormsAdapter);

        SingleObsForm form = new SingleObsForm(selectedBottomSheetConcept, new Date(), selectedBottomSheetConcept.getConceptType().getName(), "", singleObsFormsList.size() + 1);
        if(singleObsFormsList.size() > 0){
            if(singleObsFormsList.get(0).getConcept().getId() == selectedBottomSheetConcept.getId()){
                singleObsFormsList.add(form);
            }
        }else {
            bottomSheetConceptTitleTextView.setText(String.format(Locale.getDefault(), "%s (%s)", selectedBottomSheetConcept.getName(), selectedBottomSheetConcept.getConceptType().getName()));
            singleObsFormsList.add(form);
        }
        clientDynamicObsFormsAdapter.notifyDataSetChanged();


        cancelBottomSheetActionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                singleObsFormsList.clear();
                clientDynamicObsFormsAdapter.notifyDataSetChanged();
                dialog.dismiss();
                EventBus.getDefault().post(new ReloadObservationsDataEvent());
            }
        });

        addReadingActionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SingleObsForm form = new SingleObsForm(selectedBottomSheetConcept, new Date(), selectedBottomSheetConcept.getConceptType().getName(), "", singleObsFormsList.size() + 1);
                if(singleObsFormsList.size() > 0){
                    if(singleObsFormsList.get(0).getConcept().getId() == selectedBottomSheetConcept.getId()){
                        singleObsFormsList.add(form);
                    }
                }else {
                    bottomSheetConceptTitleTextView.setText(String.format(Locale.getDefault(), "%s (%s)", selectedBottomSheetConcept.getName(), selectedBottomSheetConcept.getConceptType().getName()));
                    singleObsFormsList.add(form);
                }
                clientDynamicObsFormsAdapter.notifyDataSetChanged();
            }
        });

        saveBottomSheetEntriesActionView.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                if (singleObsFormsList.isEmpty()) {
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.general_enter_value_to_save), Toast.LENGTH_LONG).show();
                } else {
                    for (SingleObsForm form : singleObsFormsList) {
                        FormUtils.handleSaveIndividualObsData(getApplicationContext(), patient, form.getDate(), selectedBottomSheetConcept, form.getInputValue());
                    }
                    singleObsFormsList.clear();
                    clientDynamicObsFormsAdapter.notifyDataSetChanged();
                    dialog.dismiss();
                    loadFormsCount();
                    EventBus.getDefault().post(new ReloadObservationsDataEvent());
                }
            }
        });
        dialog.setCancelable(false);
        dialog.show();
    }

    private void loadPatientData() {
        try {
            patientUuid = getIntent().getStringExtra(PATIENT_UUID);

            patient = ((MuzimaApplication) getApplicationContext()).getPatientController().getPatientByUuid(patientUuid);
            patientNameTextView.setText(patient.getDisplayName());
            identifierTextView.setText(String.format(Locale.getDefault(), "ID:#%s", patient.getIdentifier()));
            dobTextView.setText(String.format("DOB: %s", new SimpleDateFormat("MM-dd-yyyy", Locale.getDefault()).format(patient.getBirthdate())));
            patientGenderImageView.setImageResource(getGenderImage(patient.getGender()));
            ageTextView.setText(String.format(Locale.getDefault(), "%d Yrs", DateUtils.calculateAge(patient.getBirthdate())));
            gpsAddressTextView.setText(getDistanceToClientAddress(patient));
        } catch (PatientController.PatientLoadException e) {
            e.printStackTrace();
        }
    }

    private int getGenderImage(String gender) {
        return gender.equalsIgnoreCase("M") ? R.drawable.gender_male : R.drawable.gender_female;
    }

    private String getDistanceToClientAddress(Patient patient) {
        try {
            MuzimaGPSLocation currentLocation = getCurrentGPSLocation();
            PersonAddress personAddress = patient.getPreferredAddress();
            if (currentLocation != null && personAddress != null && !StringUtils.isEmpty(personAddress.getLatitude()) && !StringUtils.isEmpty(personAddress.getLongitude())) {
                double startLatitude = Double.parseDouble(currentLocation.getLatitude());
                double startLongitude = Double.parseDouble(currentLocation.getLongitude());
                double endLatitude = Double.parseDouble(personAddress.getLatitude());
                double endLongitude = Double.parseDouble(personAddress.getLongitude());

                float[] results = new float[1];
                Location.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, results);
                return String.format("%.02f", results[0] / 1000) + " km";
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return "";
    }

    private MuzimaGPSLocation getCurrentGPSLocation() {
        MuzimaGPSLocationService muzimaLocationService = ((MuzimaApplication) getApplicationContext())
                .getMuzimaGPSLocationService();
        return muzimaLocationService.getLastKnownGPSLocation();
    }

    private void initializeResources() {
        patientNameTextView = findViewById(R.id.name);
        patientGenderImageView = findViewById(R.id.genderImg);
        dobTextView = findViewById(R.id.dateOfBirth);
        identifierTextView = findViewById(R.id.identifier);
        ageTextView = findViewById(R.id.age_text_label);
        gpsAddressTextView = findViewById(R.id.distanceToClientAddress);
        incompleteFormsCountView = findViewById(R.id.dashboard_forms_incomplete_forms_count_view);
        completeFormsCountView = findViewById(R.id.dashboard_forms_complete_forms_count_view);
        incompleteFormsView = findViewById(R.id.dashboard_forms_incomplete_forms_view);
        completeFormsView = findViewById(R.id.dashboard_forms_complete_forms_view);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        MuzimaSettingController muzimaSettingController = ((MuzimaApplication) getApplicationContext()).getMuzimaSettingController();
        isSingleElementEnabled = muzimaSettingController.isSingleElementEntryEnabled();

        if(isSingleElementEnabled){
            tabLayout.getTabAt(0).setText(R.string.general_data_collection);
        }else{
            tabLayout.getTabAt(0).setText(R.string.general_filling_forms);
        }

        incompleteFormsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchFormDataList(true);
            }
        });

        completeFormsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchFormDataList(false);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (android.R.id.home == item.getItemId()) {
            if (getIntent().getStringExtra(CALLING_ACTIVITY) != null && getIntent().getStringExtra(CALLING_ACTIVITY).equalsIgnoreCase(PatientsSearchActivity.class.getSimpleName())) {
                Intent intent = new Intent(getApplicationContext(), PatientsSearchActivity.class);
                startActivity(intent);
                finish();
            } else if (getIntent().getStringExtra(CALLING_ACTIVITY) != null && getIntent().getStringExtra(CALLING_ACTIVITY).equalsIgnoreCase(MainDashboardActivity.class.getSimpleName())) {
                Intent intent = new Intent(getApplicationContext(), MainDashboardActivity.class);
                startActivity(intent);
                finish();
            }
            onBackPressed();
        }
        return true;
    }

    private void loadFormsCount() {
        try {
            long incompleteForms = ((MuzimaApplication) getApplicationContext()).getFormController().countIncompleteFormsForPatient(patientUuid);
            long completeForms = ((MuzimaApplication) getApplicationContext()).getFormController().countCompleteFormsForPatient(patientUuid);
            incompleteFormsCountView.setText(String.valueOf(incompleteForms));

            if(incompleteForms == 0){
                incompleteFormsView.setBackground(getResources().getDrawable(R.drawable.rounded_corners_green));
            }else if(incompleteForms>0 && incompleteForms<=5){
                incompleteFormsView.setBackground(getResources().getDrawable(R.drawable.rounded_corners_orange));
            }else{
                incompleteFormsView.setBackground(getResources().getDrawable(R.drawable.rounded_corners_red));
            }

            incompleteFormsCountView.setText(String.valueOf(incompleteForms));

            if(completeForms == 0){
                completeFormsView.setBackground(getResources().getDrawable(R.drawable.rounded_corners_green));
            }else if(completeForms>0 && completeForms<=5){
                completeFormsView.setBackground(getResources().getDrawable(R.drawable.rounded_corners_orange));
            }else{
                completeFormsView.setBackground(getResources().getDrawable(R.drawable.rounded_corners_red));
            }
            completeFormsCountView.setText(String.valueOf(completeForms));
        } catch (FormController.FormFetchException e) {
            Log.e(getClass().getSimpleName(), "Could not count complete and incomplete forms",e);
        }
    }

    @Subscribe
    public void closeSingleFormEvent(CloseSingleFormEvent event) {
        int position = event.getPosition();
        if(singleObsFormsList.size() != 1) {
            singleObsFormsList.remove(position);
            clientDynamicObsFormsAdapter.notifyDataSetChanged();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onDatePickerClicked(final int position, EditText dateEditText) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(PatientSummaryActivity.this);
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.setOnDateSetListener(new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, day);
                singleObsFormsList.get(position).setDate(calendar.getTime());
                clientDynamicObsFormsAdapter.notifyDataSetChanged();
            }
        });
        datePickerDialog.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onDateValuePickerClicked(final int position, MaterialTextView dateEditText) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(PatientSummaryActivity.this);
        datePickerDialog.setOnDateSetListener(new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, day);
                singleObsFormsList.get(position).setInputDateValue(DateUtils.convertDateToDayMonthYearString(calendar.getTime()));
                clientDynamicObsFormsAdapter.notifyDataSetChanged();
            }
        });
        datePickerDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FormsWithDataActivity.FORM_VIEW_ACTIVITY_RESULT) {
            loadFormsCount();
        }
    }


    private void launchFormDataList(boolean isIncompleteFormsData) {
        Intent intent = new Intent(this, FormsWithDataActivity.class);

        intent.putExtra(PATIENT_UUID,patientUuid);
        intent.putExtra(PATIENT,patient);
        if (isIncompleteFormsData) {
            intent.putExtra(FormsWithDataActivity.KEY_FORMS_TAB_TO_OPEN, TAB_INCOMPLETE);
        } else {
            intent.putExtra(FormsWithDataActivity.KEY_FORMS_TAB_TO_OPEN, TAB_COMPLETE);
        }
        startActivity(intent);
    }
}
