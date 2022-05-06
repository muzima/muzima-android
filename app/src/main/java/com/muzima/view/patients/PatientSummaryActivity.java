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

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.forms.ClientSummaryFormsAdapter;
import com.muzima.api.model.Patient;
import com.muzima.api.model.PersonAddress;
import com.muzima.controller.FormController;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.controller.PatientController;
import com.muzima.model.AvailableForm;
import com.muzima.model.collections.AvailableForms;
import com.muzima.model.location.MuzimaGPSLocation;
import com.muzima.service.MuzimaGPSLocationService;
import com.muzima.tasks.FormsLoaderService;
import com.muzima.utils.DateUtils;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.StringUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.utils.smartcard.SmartCardIntentIntegrator;
import com.muzima.view.MainDashboardActivity;
import com.muzima.view.custom.ActivityWithPatientSummaryBottomNavigation;
import com.muzima.view.custom.MuzimaRecyclerView;
import com.muzima.view.forms.FormViewIntent;
import com.muzima.view.forms.FormsWithDataActivity;
import com.muzima.view.fragments.patient.ChronologicalObsViewFragment;
import com.muzima.view.relationship.RelationshipsListActivity;
import org.greenrobot.eventbus.EventBus;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.muzima.adapters.forms.FormsPagerAdapter.TAB_COMPLETE;
import static com.muzima.adapters.forms.FormsPagerAdapter.TAB_INCOMPLETE;
import static com.muzima.view.relationship.RelationshipsListActivity.INDEX_PATIENT;

public class PatientSummaryActivity extends ActivityWithPatientSummaryBottomNavigation implements ClientSummaryFormsAdapter.OnFormClickedListener, FormsLoaderService.FormsLoadedCallback {
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
    private TextView incompleteFormsCountView;
    private TextView completeFormsCountView;
    private String patientUuid;
    private Patient patient;
    private final LanguageUtil languageUtil = new LanguageUtil();
    private View incompleteFormsView;
    private View completeFormsView;
    private ClientSummaryFormsAdapter formsAdapter;
    private List<AvailableForm> forms = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ThemeUtils.getInstance().onCreate(this,true);
        languageUtil.onCreate(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_summary);
        initializeResources();
        loadPatientData();
        loadData();
        loadChronologicalObsView();
        setTitle(R.string.title_activity_client_summary);
        loadBottomNavigation(patientUuid);
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

    private void loadData() {
        ((MuzimaApplication) this.getApplicationContext()).getExecutorService()
                .execute(new FormsLoaderService(this.getApplicationContext(), this));
    }

    private void loadChronologicalObsView(){
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(R.id.chronological_fragment, new ChronologicalObsViewFragment(patientUuid)).commit();
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

    private void loadPatientData() {
        try {
            patientUuid = getIntent().getStringExtra(PATIENT_UUID);

            patient = ((MuzimaApplication) getApplicationContext()).getPatientController().getPatientByUuid(patientUuid);
            patientNameTextView.setText(patient.getDisplayName());
            identifierTextView.setText(String.format(Locale.getDefault(), "ID:#%s", patient.getIdentifier()));
            if (patient.getBirthdate() != null)
                dobTextView.setText(String.format("DOB: %s", new SimpleDateFormat("MM-dd-yyyy", Locale.getDefault()).format(patient.getBirthdate())));

            patientGenderImageView.setImageResource(getGenderImage(patient.getGender()));
            if (patient.getBirthdate() != null)
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

        MuzimaRecyclerView formsListRecyclerView = findViewById(R.id.recycler_list);
        formsListRecyclerView.setLayoutManager(new LinearLayoutManager(this.getApplicationContext(), LinearLayoutManager.VERTICAL, false));

        formsAdapter = new ClientSummaryFormsAdapter(forms, this);
        formsListRecyclerView.setAdapter(formsAdapter);
        formsListRecyclerView.setNoDataLayout(findViewById(R.id.no_data_layout),
                getString(R.string.info_forms_unavailable),
                StringUtils.EMPTY);
        try {
            patient = ((MuzimaApplication) getApplication().getApplicationContext()).getPatientController().getPatientByUuid(patientUuid);
        }catch (PatientController.PatientLoadException ex){
            ex.printStackTrace();
        }
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

    @Override
    public void onFormsLoaded(final AvailableForms formList) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                forms.addAll(formList);
                formsAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onFormClickedListener(int position) {
        AvailableForm form = forms.get(position);
        Intent intent = new FormViewIntent(this, form, patient , false);
        intent.putExtra(INDEX_PATIENT, patient);
        this.startActivityForResult(intent, FormsWithDataActivity.FORM_VIEW_ACTIVITY_RESULT);
    }

    @Override
    protected int getBottomNavigationMenuItemId() {
        return R.id.action_cohorts;
    }

    public void loadForms(View v) {
        Intent intent = new Intent(this, DataCollectionActivity.class);
        intent.putExtra(PATIENT_UUID, patientUuid);
        startActivity(intent);
    }

    public void loadObservation(View v) {
        Intent intent = new Intent(this, ObsViewActivity.class);
        intent.putExtra(PATIENT_UUID, patientUuid);
        startActivity(intent);
    }
}
