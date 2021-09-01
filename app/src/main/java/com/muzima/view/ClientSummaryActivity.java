package com.muzima.view;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
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
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.tabs.TabLayout;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.forms.ClientDynamicObsFormsAdapter;
import com.muzima.adapters.forms.FormSummaryCardsAdapter;
import com.muzima.api.model.Concept;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PersonAddress;
import com.muzima.controller.ConceptController;
import com.muzima.controller.PatientController;
import com.muzima.model.ObsConceptWrapper;
import com.muzima.model.SingleObsForm;
import com.muzima.model.SummaryCard;
import com.muzima.model.enums.CardsSummaryCategory;
import com.muzima.model.events.ClientSummaryObservationSelectedEvent;
import com.muzima.model.events.CloseSingleFormEvent;
import com.muzima.model.events.ReloadObservationsDataEvent;
import com.muzima.model.location.MuzimaGPSLocation;
import com.muzima.service.MuzimaGPSLocationService;
import com.muzima.tasks.FormsCountService;
import com.muzima.utils.Constants;
import com.muzima.utils.DateUtils;
import com.muzima.utils.FormUtils;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.MuzimaPreferences;
import com.muzima.utils.StringUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.utils.smartcard.SmartCardIntentIntegrator;
import com.muzima.view.custom.ClientSummaryPager;
import com.muzima.view.forms.FormsActivity;
import com.muzima.view.patients.PatientsLocationMapActivity;
import com.muzima.view.patients.PatientsSearchActivity;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ClientSummaryActivity extends AppCompatActivity implements FormSummaryCardsAdapter.OnCardClickedListener, FormsCountService.FormsCountServiceCallback, ClientDynamicObsFormsAdapter.DatePickerClickedListener {
    private static final String TAG = "ClientSummaryActivity";
    public static final String PATIENT_UUID = "patient_uuid";
    public static final String CALLING_ACTIVITY = "calling_activity_key";
    private Toolbar toolbar;
    private TextView patientNameTextView;
    private ImageView patientGenderImageView;
    private TextView dobTextView;
    private TextView identifierTextView;
    private TextView gpsAddressTextView;
    private TextView ageTextView;
    private TextView bottomSheetConceptTitleTextView;
    private TextView incompleteForms;
    private TextView completeForms;
    private View expandHistoricalDataView;
    private View expandDataCollectionView;
    private View childContainerView;
    private ImageView expandHistoricalDataImageView;
    private ImageView expandDataCollectionImageView;
    private View historicalDataContainerView;
    private ViewPager dataCollectionViewPager;
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
        loadFormsCountData();

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        ClientSummaryPager clientSummaryPager = new ClientSummaryPager(this, tabLayout.getTabCount(), patientUuid);
        viewPager.setAdapter(clientSummaryPager);
        viewPager.setUserInputEnabled(false);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
                tab.getIcon().setTint(ContextCompat.getColor(ClientSummaryActivity.this, (R.color.primary_blue)));
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                if (ThemeUtils.getPreferenceLightMode(ClientSummaryActivity.this))
                    tab.getIcon().setTint(ContextCompat.getColor(ClientSummaryActivity.this, (R.color.primary_black)));
                else
                    tab.getIcon().setTint(ContextCompat.getColor(ClientSummaryActivity.this, (R.color.primary_white)));
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_client_summary, menu);
        MenuItem shrMenu = menu.findItem(R.id.menu_shr);
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
        return true;
    }

    private void readSmartCard() {
        SmartCardIntentIntegrator SHRIntegrator = new SmartCardIntentIntegrator(ClientSummaryActivity.this);
        SHRIntegrator.initiateCardRead();
        Toast.makeText(getApplicationContext(), getResources().getString(R.string.general_opening_card_reader), Toast.LENGTH_LONG).show();
    }

    private void navigateToClientsLocationMap() {
        Intent intent = new Intent(getApplicationContext(), PatientsLocationMapActivity.class);
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
            bottomSheetConceptTitleTextView.setText(String.format(Locale.getDefault(), "%s (%s)", selectedBottomSheetConcept.getName(), selectedBottomSheetConcept.getConceptType().getName()));

        }
    }

    @Override
    public void onFormsCountLoaded(final long completeFormsCount, final long incompleteFormsCount) {
        incompleteForms.setText(String.format(Locale.getDefault(), "%d", incompleteFormsCount));
        completeForms.setText(String.format(Locale.getDefault(),"%d", incompleteFormsCount));

//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                formsSummaries.clear();
//                formsSummaries.add(new SummaryCard(CardsSummaryCategory.INCOMPLETE_FORMS, getResources().getString(R.string.info_incomplete_form), incompleteFormsCount));
//                formsSummaries.add(new SummaryCard(CardsSummaryCategory.COMPLETE_FORMS, getResources().getString(R.string.info_complete_form), completeFormsCount));
//                formSummaryCardsAdapter.notifyDataSetChanged();
//            }
//        });
    }

    private void loadFormsCountData() {
        ((MuzimaApplication) getApplicationContext()).getExecutorService()
                .execute(new FormsCountService(patient.getUuid(), getApplicationContext(), this));
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
        clientDynamicObsFormsAdapter = new ClientDynamicObsFormsAdapter(getApplicationContext(), singleObsFormsList, this);
        singleObsFormsRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        singleObsFormsRecyclerView.setAdapter(clientDynamicObsFormsAdapter);
        incompleteForms = findViewById(R.id.dashboard_forms_incomplete_forms_count_view);
        completeForms = findViewById(R.id.dashboard_forms_complete_forms_count_view);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        cancelBottomSheetActionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                singleObsFormsList.clear();
                clientDynamicObsFormsAdapter.notifyDataSetChanged();
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });

        saveBottomSheetEntriesActionView.setOnClickListener(new View.OnClickListener() {
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
                    EventBus.getDefault().post(new ReloadObservationsDataEvent());
                }
            }
        });

        addReadingActionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SingleObsForm form = new SingleObsForm(selectedBottomSheetConcept, new Date(), selectedBottomSheetConcept.getConceptType().getName(), "", singleObsFormsList.size() + 1);
                singleObsFormsList.add(form);
                clientDynamicObsFormsAdapter.notifyDataSetChanged();
            }
        });

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    childContainerView.setVisibility(View.GONE);
                } else {
                    childContainerView.setVisibility(View.VISIBLE);
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
        }
        return true;
    }

    @Subscribe
    public void closeSingleFormEvent(CloseSingleFormEvent event) {
        int position = event.getPosition();
        singleObsFormsList.remove(position);
        clientDynamicObsFormsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onCardClicked(int position) {
        SummaryCard header = formsSummaries.get(position);
        CardsSummaryCategory category = header.getCategory();
        switch (category) {
            case COMPLETE_FORMS:
                MuzimaPreferences.setFormsActivityActionModePreference(getApplicationContext(), Constants.FORMS_LAUNCH_MODE.COMPLETE_FORMS_VIEW);
                Intent completeFormsIntent = new Intent(getApplicationContext(), FormsActivity.class);
                completeFormsIntent.putExtra(FormsActivity.KEY_FORMS_TAB_TO_OPEN, 1);
                startActivity(completeFormsIntent);
                finish();
                break;
            case INCOMPLETE_FORMS:
                MuzimaPreferences.setFormsActivityActionModePreference(getApplicationContext(), Constants.FORMS_LAUNCH_MODE.INCOMPLETE_FORMS_VIEW);
                Intent intent = new Intent(getApplicationContext(), FormsActivity.class);
                intent.putExtra(FormsActivity.KEY_FORMS_TAB_TO_OPEN, 1);
                startActivity(intent);
                finish();
                break;
            case EMERGENCY_CONTACT:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (getIntent().getStringExtra(CALLING_ACTIVITY) != null && getIntent().getStringExtra(CALLING_ACTIVITY).equalsIgnoreCase(PatientsSearchActivity.class.getSimpleName())) {
            Intent intent = new Intent(getApplicationContext(), PatientsSearchActivity.class);
            startActivity(intent);
            finish();
        } else if (getIntent().getStringExtra(CALLING_ACTIVITY) != null && getIntent().getStringExtra(CALLING_ACTIVITY).equalsIgnoreCase(MainDashboardActivity.class.getSimpleName())) {
            Intent intent = new Intent(getApplicationContext(), MainDashboardActivity.class);
            startActivity(intent);
            finish();
        } else {
            //ToDo: Discuss whether we should just navigate back to the dashboard irrespective of the type of calling activity
            Intent intent = new Intent(getApplicationContext(), MainDashboardActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onDatePickerClicked(final int position, EditText dateEditText) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(ClientSummaryActivity.this);
        datePickerDialog.setOnDateSetListener(new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                Log.e(TAG, "onDateSet: year, " + year + " month " + month + "day" + day);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FormsActivity.FORM_VIEW_ACTIVITY_RESULT) {
            loadFormsCountData();
        }
    }
}
