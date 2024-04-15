package com.muzima.view.patients;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.RecyclerAdapter;
import com.muzima.adapters.patients.PatientAdapterHelper;
import com.muzima.adapters.patients.SimilarPatientsLocalSearchAdapter;
import com.muzima.api.model.Patient;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.forms.RegistrationFormsActivity;

import java.util.List;
import java.util.UUID;

public class SimilarPatientsSearchActivity extends BroadcastListenerActivity implements PatientAdapterHelper.PatientListClickListener,
        RecyclerAdapter.BackgroundListQueryTaskListener {
    private DrawerLayout mainLayout;
    private final LanguageUtil languageUtil = new LanguageUtil();
    private Patient patient;
    private List<Patient> similarPatients;
    private SimilarPatientsLocalSearchAdapter patientAdapter;

    private RecyclerView recyclerView;
    private FrameLayout progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.getInstance().onCreate(this,true);
        languageUtil.onCreate(this);

        super.onCreate(savedInstanceState);
        mainLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_patient_registration_search, null);
        setContentView(mainLayout);
        Bundle intentExtras = getIntent().getExtras();

        setTitle(getString(R.string.title_found_similar_people));
        setupListView();

         findSimilarPatients();

        Button openRegFormBtn = findViewById(R.id.open_reg_form_button);
        openRegFormBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startWebViewActivity();
            }
        });

        CheckBox cannotFindPtCheckbox = findViewById(R.id.cannot_find_pt_checkbox);
        cannotFindPtCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked){
                    openRegFormBtn.setEnabled(true);
                } else {
                    openRegFormBtn.setEnabled(false);
                }
            }
        });

    }
    private void setupListView() {
        patient = (Patient) getIntent().getSerializableExtra(PatientSummaryActivity.PATIENT);

        recyclerView = findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        patientAdapter = new SimilarPatientsLocalSearchAdapter(this,
                ((MuzimaApplication) getApplicationContext()).getPatientController(), patient);

        patientAdapter.setBackgroundListQueryTaskListener(this);
        patientAdapter.setPatientListClickListener(this);
        recyclerView.setAdapter(patientAdapter);

        progressBar = findViewById(R.id.progressbarContainer);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void findSimilarPatients(){
        patientAdapter.search(getFormattedNameForSearch(patient));
    }

    private String getFormattedNameForSearch(Patient patient){
        StringBuilder formattedName = new StringBuilder();
        if(patient.getFamilyName() != null){
            formattedName.append(patient.getFamilyName());
        }

        if(patient.getMiddleName() != null){
            formattedName.append(" "+patient.getMiddleName());
        }

        if(patient.getGivenName() != null){
            formattedName.append(" "+patient.getGivenName());
        }
        return formattedName.toString();
    }

    private void startWebViewActivity() {
        if (patient == null) {
            patient = new Patient();
            patient.setUuid(String.valueOf(UUID.randomUUID()));
        }
        Intent intent = new Intent(SimilarPatientsSearchActivity.this, RegistrationFormsActivity.class);
        intent.putExtra(PatientSummaryActivity.PATIENT,patient);
        startActivity(intent);
        finish();
    }

    @Override
    public void onItemLongClick(View view, int position) {

    }

    @Override
    public void onItemClick(View view, int position) {
        patientAdapter.cancelBackgroundTask();
        Patient patient = patientAdapter.getPatient(position);
        if(patient != null) {
            Intent intent = new Intent(this, PatientSummaryActivity.class);
            intent.putExtra(PatientSummaryActivity.CALLING_ACTIVITY, SimilarPatientsSearchActivity.class.getSimpleName());
            intent.putExtra(PatientSummaryActivity.PATIENT_UUID, patient.getUuid());
            startActivity(intent);
        }
    }

    @Override
    public void onQueryTaskStarted() {
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onQueryTaskFinish() {
        if(patientAdapter.getItemCount()<=0){
            startWebViewActivity();
        }
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onQueryTaskCancelled() {

    }

    @Override
    public void onQueryTaskCancelled(Object errorDefinition) {

    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
