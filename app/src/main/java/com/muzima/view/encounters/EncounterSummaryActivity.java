/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */
package com.muzima.view.encounters;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.RecyclerAdapter;
import com.muzima.adapters.encounters.EncounterObservationsAdapter;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.Patient;
import com.muzima.controller.PatientController;
import com.muzima.utils.DateUtils;
import com.muzima.utils.LanguageUtil;
import com.muzima.utils.StringUtils;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.custom.MuzimaRecyclerView;

public class EncounterSummaryActivity  extends BroadcastListenerActivity implements RecyclerAdapter.BackgroundListQueryTaskListener {
    public static final String ENCOUNTER="encounter";
    private Encounter encounter;
    private EncounterObservationsAdapter encounterObservationsAdapter;
    private LinearLayout noDataView;
    private final LanguageUtil languageUtil = new LanguageUtil();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.getInstance().onCreate(this,true);
        languageUtil.onCreate(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encounter_summary);

        Bundle intentExtras = getIntent().getExtras();
        if (intentExtras != null)
            encounter = (Encounter) intentExtras.getSerializable(ENCOUNTER);

        setupToolbar();
        setupEncounterMetadata();
        setupStillLoadingView();
        setUpEncounterObservations();
        if (encounter != null) {
            logEvent("VIEW_ENCOUNTER_SUMMARY", "{\"patientuuid\":\"" + encounter.getPatient().getUuid() + "\"}");
        } else {
            logEvent("VIEW_ENCOUNTER_SUMMARY");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        languageUtil.onResume(this);
    }

    private void setupToolbar(){
        if (getSupportActionBar() != null) {
            try {
                Patient patient = ((MuzimaApplication) getApplicationContext()).getPatientController().getPatientByUuid(encounter.getPatient().getUuid());
                if (patient != null)
                    getSupportActionBar().setTitle(patient.getSummary());
            } catch (PatientController.PatientLoadException e) {
                Log.e(getClass().getSimpleName(), "Could not load patient details",e);
            }
        }
    }

    private void setupEncounterMetadata(){
        TextView encounterFormName = findViewById(R.id.encounterFormName);
        encounterFormName.setText(encounter.getEncounterType().getName());

        TextView encounterDate = findViewById(R.id.encounterDate);
        encounterDate.setText(DateUtils.getMonthNameFormattedDate(encounter.getEncounterDatetime()));

        TextView encounterProvider = findViewById(R.id.encounterProvider);
        encounterProvider.setText(encounter.getProvider().getDisplayName());

        TextView encounterLocation = findViewById(R.id.encounterLocation);
        encounterLocation.setText(encounter.getLocation().getName());
    }

    private void setupNoDataView() {
        noDataView = findViewById(R.id.no_data_layout);

        TextView noDataMsgTextView = findViewById(R.id.no_data_msg);
        noDataMsgTextView.setText(getResources().getText(R.string.info_observation_unavailable));
    }

    private void setupStillLoadingView(){
        noDataView = findViewById(R.id.no_data_layout);

        TextView noDataMsgTextView = findViewById(R.id.no_data_msg);
        noDataMsgTextView.setText(getResources().getText(R.string.info_observations_load_in_progress));
    }

    private void setUpEncounterObservations(){
        MuzimaRecyclerView encounterObservationsView = findViewById(R.id.encounter_observations_list);
        encounterObservationsAdapter = new EncounterObservationsAdapter(this,
                ((MuzimaApplication) getApplicationContext()).getObservationController(),encounter.getUuid());
        encounterObservationsAdapter.setBackgroundListQueryTaskListener(this);
        encounterObservationsView.setLayoutManager(new LinearLayoutManager(this));
        encounterObservationsView.setAdapter(encounterObservationsAdapter);
        encounterObservationsView.setNoDataLayout(noDataView, StringUtils.EMPTY, StringUtils.EMPTY);
        encounterObservationsAdapter.reloadData();
    }

    @Override
    public void onQueryTaskStarted(){}

    @Override
    public void onQueryTaskFinish(){}

    @Override
    public void onQueryTaskCancelled(){}
}
