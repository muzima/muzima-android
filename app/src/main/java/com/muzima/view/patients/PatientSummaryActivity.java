package com.muzima.view.patients;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textview.MaterialTextView;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.forms.ClientDynamicObsFormsAdapter;
import com.muzima.adapters.patients.ClientSummaryPagerAdapter;
import com.muzima.api.model.Concept;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PersonAddress;
import com.muzima.controller.ConceptController;
import com.muzima.controller.FormController;
import com.muzima.controller.PatientController;
import com.muzima.model.ObsConceptWrapper;
import com.muzima.model.SingleObsForm;
import com.muzima.model.SummaryCard;
import com.muzima.model.events.ClientSummaryObservationSelectedEvent;
import com.muzima.model.events.CloseSingleFormEvent;
import com.muzima.model.events.ReloadObservationsDataEvent;
import com.muzima.model.location.MuzimaGPSLocation;
import com.muzima.service.MuzimaGPSLocationService;
import com.muzima.utils.DateUtils;
import com.muzima.utils.FormUtils;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.StringUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.utils.smartcard.SmartCardIntentIntegrator;
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

public class PatientSummaryActivity extends AppCompatActivity implements ClientDynamicObsFormsAdapter.DatePickerClickedListener, ClientDynamicObsFormsAdapter.DateValuePickerClickedListener {
    private static final String TAG = "PatientSummaryActivity";
    public static final String PATIENT = "patient";
    public static final String PATIENT_UUID = "patient_uuid";
    public static final String CALLING_ACTIVITY = "calling_activity_key";
    public static final boolean DEFAULT_SHR_STATUS = false;
    private static final boolean DEFAULT_RELATIONSHIP_STATUS = false;
    private Toolbar toolbar;
    private TextView patientNameTextView;
    private ImageView patientGenderImageView;
    private TextView dobTextView;
    private TextView identifierTextView;
    private TextView gpsAddressTextView;
    private TextView ageTextView;
    private TextView bottomSheetConceptTitleTextView;
    private TextView incompleteFormsCountView;
    private TextView completeFormsCountView;
    private View incompleteFormsView;
    private View completeFormsView;
    private View childContainerView;
    private View addReadingActionView;
    private View cancelBottomSheetActionView;
    private View saveBottomSheetEntriesActionView;
    private BottomSheetBehavior bottomSheetBehavior;
    private RecyclerView singleObsFormsRecyclerView;
    private View bottomSheetView;
    private String patientUuid;
    private Patient patient;
    private Concept selectedBottomSheetConcept;
    private final ThemeUtils themeUtils = new ThemeUtils();
    private final LanguageUtil languageUtil = new LanguageUtil();
    private ClientDynamicObsFormsAdapter clientDynamicObsFormsAdapter;
    private List<SummaryCard> formsSummaries = new ArrayList<>();
    private List<SingleObsForm> singleObsFormsList = new ArrayList<>();
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        themeUtils.onCreate(this);
        languageUtil.onCreate(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_client_summary);
        initializeResources();
        loadPatientData();

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        ClientSummaryPagerAdapter clientSummaryPager = new ClientSummaryPagerAdapter(this, tabLayout.getTabCount(), patientUuid);
        viewPager.setAdapter(clientSummaryPager);
        viewPager.setUserInputEnabled(false);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
                tab.getIcon().setTint(ContextCompat.getColor(PatientSummaryActivity.this, (R.color.primary_blue)));
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                if (ThemeUtils.getPreferenceLightMode(PatientSummaryActivity.this))
                    tab.getIcon().setTint(ContextCompat.getColor(PatientSummaryActivity.this, (R.color.primary_black)));
                else
                    tab.getIcon().setTint(ContextCompat.getColor(PatientSummaryActivity.this, (R.color.primary_white)));
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
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
        ObsConceptWrapper conceptWrapper = event.getConceptWrapper();
        if (conceptWrapper.getConcept().isCoded() || conceptWrapper.getConcept().isSet() || conceptWrapper.getConcept().isPrecise()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(true)
                    .setMessage("This data type is not support, consult admin.")
                    .show();
        } else {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            selectedBottomSheetConcept = conceptWrapper.getConcept();
            addReadingActionView.callOnClick();
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
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
        toolbar = findViewById(R.id.client_summary_dashboard_toolbar);
        patientNameTextView = findViewById(R.id.name);
        patientGenderImageView = findViewById(R.id.genderImg);
        dobTextView = findViewById(R.id.dateOfBirth);
        identifierTextView = findViewById(R.id.identifier);
        ageTextView = findViewById(R.id.age_text_label);
        gpsAddressTextView = findViewById(R.id.distanceToClientAddress);
        addReadingActionView = findViewById(R.id.general_add_reading_button);
        cancelBottomSheetActionView = findViewById(R.id.close_summary_bottom_sheet_view);
        saveBottomSheetEntriesActionView = findViewById(R.id.client_summary_save_action_bottom_sheet);
        bottomSheetView = findViewById(R.id.client_summary_dynamic_form_bottom_sheet_container);
        singleObsFormsRecyclerView = findViewById(R.id.client_summary_single_obs_form_recycler_view);
        childContainerView = findViewById(R.id.bottom_sheet_child_container);
        bottomSheetConceptTitleTextView = findViewById(R.id.cohort_name_text_view);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView);
        clientDynamicObsFormsAdapter = new ClientDynamicObsFormsAdapter(getApplicationContext(), singleObsFormsList, this,this);
        singleObsFormsRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        singleObsFormsRecyclerView.setAdapter(clientDynamicObsFormsAdapter);
        incompleteFormsCountView = findViewById(R.id.dashboard_forms_incomplete_forms_count_view);
        completeFormsCountView = findViewById(R.id.dashboard_forms_complete_forms_count_view);
        incompleteFormsView = findViewById(R.id.dashboard_forms_incomplete_forms_view);
        completeFormsView = findViewById(R.id.dashboard_forms_complete_forms_view);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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

        cancelBottomSheetActionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                singleObsFormsList.clear();
                clientDynamicObsFormsAdapter.notifyDataSetChanged();
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
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
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                    singleObsFormsList.clear();
                    clientDynamicObsFormsAdapter.notifyDataSetChanged();

                    loadFormsCount();
                    EventBus.getDefault().post(new ReloadObservationsDataEvent());
                }
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

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
//                    childContainerView.setVisibility(View.GONE);
                } else {
//                    childContainerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

        //todo remove
        try {
            List<Concept> conceptList = ((MuzimaApplication) getApplicationContext()).getConceptController().getConcepts();
            if (!conceptList.isEmpty())
                selectedBottomSheetConcept = conceptList.get(0);
            if (selectedBottomSheetConcept != null)
                bottomSheetConceptTitleTextView.setText(String.format(Locale.getDefault(), "%s (%s)", selectedBottomSheetConcept.getName(), selectedBottomSheetConcept.getConceptType().getName()));
        } catch (ConceptController.ConceptFetchException ex) {
            ex.printStackTrace();
        }

        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
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
        datePickerDialog.setOnDateSetListener(new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                SingleObsForm form = singleObsFormsList.get(position);
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, day);
                form.setDate(calendar.getTime());
                singleObsFormsList.remove(position);
                singleObsFormsList.add(position, form);
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
                SingleObsForm form = singleObsFormsList.get(position);
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, day);
                form.setInputDateValue(DateUtils.convertDateToDayMonthYearString(calendar.getTime()));
                singleObsFormsList.remove(position);
                singleObsFormsList.add(position, form);
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
