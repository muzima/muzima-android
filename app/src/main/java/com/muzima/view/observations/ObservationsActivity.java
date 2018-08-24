/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.observations;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.support.v7.widget.SearchView;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.observations.ObservationsPagerAdapter;
import com.muzima.api.model.Patient;
import com.muzima.utils.Fonts;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.custom.CustomObsEntryDialog;
import com.muzima.view.custom.PagerSlidingTabStrip;
import com.muzima.view.patients.PatientSummaryActivity;

import java.util.Calendar;

public class ObservationsActivity extends BroadcastListenerActivity {

    public boolean quickSearch = false;
    private ViewPager viewPager;
    private ObservationsPagerAdapter observationsPagerAdapter;
    private PagerSlidingTabStrip pagerTabsLayout;
    private static final Calendar today = Calendar.getInstance();
    private TextView encounterDateTextView;
    private final Boolean IS_SHR_DATA = false;
    private Patient patient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_observations);
        // Show the Up button in the action bar.
        setupActionBar();
        initPager();
        initPagerIndicator();
        encounterDateTextView = (TextView) findViewById(R.id.date_value_textview);
    }

    private void initPagerIndicator() {
        pagerTabsLayout = (PagerSlidingTabStrip) findViewById(R.id.pager_indicator);
        pagerTabsLayout.setTextColor(Color.WHITE);
        pagerTabsLayout.setTextSize((int) getResources().getDimension(R.dimen.pager_indicator_text_size));
        pagerTabsLayout.setSelectedTextColor(getResources().getColor(R.color.tab_indicator));
        pagerTabsLayout.setTypeface(Fonts.roboto_medium(this), -1);
        pagerTabsLayout.setViewPager(viewPager);
        viewPager.setCurrentItem(0);
        pagerTabsLayout.markCurrentSelected(0);
    }

    private void initPager() {
        viewPager = (ViewPager) findViewById(R.id.pager);
        patient = (Patient) getIntent().getSerializableExtra(PatientSummaryActivity.PATIENT);
        observationsPagerAdapter = new ObservationsPagerAdapter(getApplicationContext(), getSupportFragmentManager(), IS_SHR_DATA, patient);
        observationsPagerAdapter.initPagerViews();
        viewPager.setAdapter(observationsPagerAdapter);
    }

    /**
     * Set up the {@link android.support.v7.app.ActionBar}.
     */
    private void setupActionBar() {
        Patient patient = (Patient) getIntent().getSerializableExtra(PatientSummaryActivity.PATIENT);
        getSupportActionBar().setTitle(patient.getSummary());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.observation_list, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.search)
                .getActionView();
        searchView.setQueryHint(getString(R.string.info_observation_search));
        searchView.setOnQueryTextListener(observationsPagerAdapter);
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        observationsPagerAdapter.cancelBackgroundQueryTasks();
    }

    public void showDatePicketDialog(View view) {
        encounterDateTextView = (TextView) view;
        DatePickerDialog datePickerDialog = new DatePickerDialog(view.getContext(), new DateSetListener(),today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    public class DateSetListener implements DatePickerDialog.OnDateSetListener {

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            monthOfYear = monthOfYear+1;
            String month = ""+monthOfYear;
            if(monthOfYear<10){
                month = "0"+monthOfYear;
            }
            String day = ""+dayOfMonth;
            if(dayOfMonth<10){
                day = "0"+dayOfMonth;
            }
            encounterDateTextView.setText(day+"-"+month+"-"+year);
        }
    }
}
