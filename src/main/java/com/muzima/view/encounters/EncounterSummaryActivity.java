/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */
package com.muzima.view.encounters;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.adapters.encounters.EncounterObservationsAdapter;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.Patient;
import com.muzima.utils.DateUtils;
import com.muzima.utils.Fonts;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.patients.PatientSummaryActivity;

public class EncounterSummaryActivity  extends BroadcastListenerActivity implements ListAdapter.BackgroundListQueryTaskListener {
    public static final String ENCOUNTER="encounter";
    private Encounter encounter;
    private ListView encounterObservationsLayout;
    private EncounterObservationsAdapter encounterObservationsAdapter;
    private View noDataView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encounter_summary);

        Bundle intentExtras = getIntent().getExtras();
        if (intentExtras != null) {
            encounter = (Encounter) intentExtras.getSerializable(ENCOUNTER);
        }

        setupActionBar();
        setupEncounterMetadata();
        setupNoDataView();
        setUpEncounterObservations();
    }

    private void setupActionBar() {
        Patient patient = (Patient) getIntent().getSerializableExtra(PatientSummaryActivity.PATIENT);
        getSupportActionBar().setTitle(patient.getSummary());
    }

    private void setupEncounterMetadata(){
        TextView encounterFormName = (TextView)findViewById(R.id.encounterFormName);
        encounterFormName.setText(encounter.getEncounterType().getName());

        TextView encounterDate =(TextView)findViewById(R.id.encounterDate);
        encounterDate.setText(DateUtils.getMonthNameFormattedDate(encounter.getEncounterDatetime()));

        TextView encounterProvider = (TextView)findViewById(R.id.encounterProvider);
        encounterProvider.setText(encounter.getProvider().getDisplayName());

        TextView encounterLocation = (TextView)findViewById(R.id.encounterLocation);
        encounterLocation.setText(encounter.getLocation().getName());


    }

    private void setupNoDataView() {

        noDataView = findViewById(R.id.no_data_layout);

        TextView noDataMsgTextView = (TextView) findViewById(R.id.no_data_msg);
        noDataMsgTextView.setText(getResources().getText(R.string.no_observations_available));
        noDataMsgTextView.setTypeface(Fonts.roboto_bold_condensed(this));

    }

    private void setUpEncounterObservations(){
        encounterObservationsLayout = (ListView)findViewById(R.id.encounter_observations_list);
        encounterObservationsAdapter = new EncounterObservationsAdapter(EncounterSummaryActivity.this,R.layout.item_encounter_observation,
                ((MuzimaApplication) getApplicationContext()).getObservationController(),encounter);
        encounterObservationsAdapter.setBackgroundListQueryTaskListener(this);
        encounterObservationsLayout.setEmptyView(noDataView);
        encounterObservationsLayout.setAdapter(encounterObservationsAdapter);
        encounterObservationsAdapter.reloadData();
    }

    @Override
    public void onQueryTaskStarted(){}

    @Override
    public void onQueryTaskFinish(){}

}
