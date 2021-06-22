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
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.forms.FormSummaryCardsAdapter;
import com.muzima.adapters.viewpager.DataCollectionViewPagerAdapter;
import com.muzima.api.model.Patient;
import com.muzima.controller.PatientController;
import com.muzima.model.SummaryCard;
import com.muzima.model.enums.CardsSummaryCategory;
import com.muzima.tasks.FormsCountService;
import com.muzima.utils.Constants;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.MuzimaPreferences;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.forms.FormsActivity;
import com.muzima.view.observations.ObservationsFragment;
import com.muzima.view.patients.PatientsListActivity;

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
    private ViewPager dataCollectionViewPager;
    private String patientUuid;
    private Patient patient;
    private final ThemeUtils themeUtils = new ThemeUtils();
    private final LanguageUtil languageUtil = new LanguageUtil();

    private FormSummaryCardsAdapter formSummaryCardsAdapter;
    private List<SummaryCard> formsSummaries = new ArrayList<>();

    private DataCollectionViewPagerAdapter dataCollectionViewPagerAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        themeUtils.onCreate(ClientSummaryActivity.this);
        languageUtil.onCreate(ClientSummaryActivity.this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_summary_dashboard);
        initializeResources();
        loadPatientData();
        loadSummaryHeaders();
        loadFormsCountData();
        loadHistoricalDataView();
    }

    @Override
    public void onUserInteraction() {
        ((MuzimaApplication) getApplication()).restartTimer();
        super.onUserInteraction();
    }

    private void loadHistoricalDataView() {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.historical_data_fragment_container, new ObservationsFragment());
        fragmentTransaction.commit();
    }

    @Override
    public void onFormsCountLoaded(final long completeFormsCount, final long incompleteFormsCount) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                formsSummaries.add(new SummaryCard(CardsSummaryCategory.COMPLETE_FORMS, getResources().getString(R.string.info_complete_form), completeFormsCount));
                formsSummaries.add(new SummaryCard(CardsSummaryCategory.COMPLETE_FORMS, getResources().getString(R.string.info_incomplete_form), incompleteFormsCount));
                formsSummaries.add(new SummaryCard(CardsSummaryCategory.COMPLETE_FORMS, getString(R.string.info_emergency_contacts), 0));
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
        dataCollectionViewPager = findViewById(R.id.client_summary_data_collection_view_pager);

        formSummaryCardsAdapter = new FormSummaryCardsAdapter(formsSummaries, this);
        formCountSummaryRecyclerView.setAdapter(formSummaryCardsAdapter);
        formCountSummaryRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext(), RecyclerView.HORIZONTAL, false));
        dataCollectionViewPagerAdapter = new DataCollectionViewPagerAdapter(getSupportFragmentManager(), getApplicationContext(), getIntent().getStringExtra(PATIENT_UUID));
        dataCollectionViewPager.setAdapter(dataCollectionViewPagerAdapter);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        expandHistoricalDataView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        expandDataCollectionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dataCollectionViewPager.getVisibility() == View.VISIBLE)
                    dataCollectionViewPager.setVisibility(View.GONE);
                else dataCollectionViewPager.setVisibility(View.VISIBLE);
            }
        });
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
}
