package com.muzima.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.forms.FormSummaryCardsAdapter;
import com.muzima.adapters.viewpager.DataCollectionViewPagerAdapter;
import com.muzima.adapters.viewpager.HistoricalDataViewPagerAdapter;
import com.muzima.api.model.Patient;
import com.muzima.controller.PatientController;
import com.muzima.model.FormsSummary;
import com.muzima.tasks.FormsCountService;
import com.muzima.view.patients.PatientsListActivity;

import net.sf.cglib.core.Local;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ClientSummaryActivity extends AppCompatActivity implements FormSummaryCardsAdapter.OnCardClickedListener, FormsCountService.FormsCountServiceCallback {

    public static final String PATIENT_UUID = "patient_uuid";
    private Toolbar toolbar;
    private TextView patientNameTextView;
    private ImageView patientGenderImageView;
    private TextView dobTextView;
    private TextView identifierTextView;
    private TextView gpsAddressTextView;
    private RecyclerView formCountSummaryRecyclerView;
    private View expandHistoricalDataView;
    private View expandDataCollectionView;
    private View expandHistoricalDataImageView;
    private View expandDataCollectionImageView;
    private ViewPager historicalDataViewPager;
    private ViewPager dataCollectionViewPager;
    private String patientUuid;
    private Patient patient;

    private FormSummaryCardsAdapter formSummaryCardsAdapter;
    private List<FormsSummary> formsSummaries = new ArrayList<>();

    private HistoricalDataViewPagerAdapter historicalDataViewPagerAdapter;
    private DataCollectionViewPagerAdapter dataCollectionViewPagerAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_summary_dashboard);
        initializeResources();
        loadPatientData();
        loadSummaryHeaders();
        loadFormsCountData();
    }

    @Override
    public void onFormsCountLoaded(final long completeFormsCount, final long incompleteFormsCount) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                formsSummaries.add(new FormsSummary(getResources().getString(R.string.info_complete_form), completeFormsCount));
                formsSummaries.add(new FormsSummary(getResources().getString(R.string.info_incomplete_form), incompleteFormsCount));
                formsSummaries.add(new FormsSummary(getString(R.string.info_emergency_contacts), 0));
                formSummaryCardsAdapter.notifyDataSetChanged();
            }
        });
    }

    private void loadFormsCountData() {
        ((MuzimaApplication) getApplicationContext()).getExecutorService()
                .execute(new FormsCountService(getApplicationContext(), this));
    }

    private void loadSummaryHeaders() {
    }

    private void loadPatientData() {
        try {
            patientUuid = getIntent().getStringExtra(PATIENT_UUID);
            patient = ((MuzimaApplication) getApplicationContext()).getPatientController().getPatientByUuid(patientUuid);
            patientNameTextView.setText(patient.getDisplayName());
            identifierTextView.setText(patient.getIdentifier());
            dobTextView.setText(String.format("DOB %s", new SimpleDateFormat("MM-dd-yyyy", Locale.getDefault()).format(patient.getBirthdate())));
            patientGenderImageView.setImageResource(getGenderImage(patient.getGender()));
        } catch (PatientController.PatientLoadException e) {
            e.printStackTrace();
        }
    }

    private int getGenderImage(String gender) {
        return gender.equalsIgnoreCase("M") ? R.drawable.ic_male : R.drawable.ic_female;
    }

    private void initializeResources() {
        toolbar = findViewById(R.id.client_summary_dashboard_toolbar);
        patientNameTextView = findViewById(R.id.name);
        patientGenderImageView = findViewById(R.id.genderImg);
        dobTextView = findViewById(R.id.dateOfBirth);
        identifierTextView = findViewById(R.id.distanceToClientAddress);
        formCountSummaryRecyclerView = findViewById(R.id.client_summary_stats_tabs_recycler_view);
        expandHistoricalDataView = findViewById(R.id.expand_historical_data_view);
        expandDataCollectionView = findViewById(R.id.expand_data_collection_view);
        expandHistoricalDataImageView = findViewById(R.id.expand_historical_data_image_view);
        expandDataCollectionImageView = findViewById(R.id.expand_data_collection_image_view);
        historicalDataViewPager = findViewById(R.id.client_summary_historical_data_view_pager);
        dataCollectionViewPager = findViewById(R.id.client_summary_data_collection_view_pager);

        formSummaryCardsAdapter = new FormSummaryCardsAdapter(formsSummaries, this);
        formCountSummaryRecyclerView.setAdapter(formSummaryCardsAdapter);
        formCountSummaryRecyclerView.setLayoutManager( new LinearLayoutManager(getApplicationContext(), RecyclerView.HORIZONTAL,false));
        historicalDataViewPagerAdapter = new HistoricalDataViewPagerAdapter(getSupportFragmentManager(), getApplicationContext());
        dataCollectionViewPagerAdapter = new DataCollectionViewPagerAdapter(getSupportFragmentManager(), getApplicationContext(), getIntent().getStringExtra(PATIENT_UUID));
        historicalDataViewPager.setAdapter(historicalDataViewPagerAdapter);
        dataCollectionViewPager.setAdapter(dataCollectionViewPagerAdapter);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (android.R.id.home == item.getItemId()) {
            Intent intent = new Intent(getApplicationContext(), PatientsListActivity.class);
            startActivity(intent);
            finish();
        }
        return true;
    }

    @Override
    public void onCardClicked(int position) {
        FormsSummary header = formsSummaries.get(position);
    }
}
