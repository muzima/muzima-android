/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.observations;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.observations.ObservationsPagerAdapter;
import com.muzima.api.model.Patient;
import com.muzima.controller.PatientController;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.patients.PatientSummaryActivity;
import com.muzima.view.custom.PagerSlidingTabStrip;

import java.util.Calendar;

public class ObservationsFragment extends Fragment {

    public boolean quickSearch = false;
    private ViewPager viewPager;
    private ObservationsPagerAdapter observationsPagerAdapter;
    private PagerSlidingTabStrip pagerTabsLayout;
    private static final Calendar today = Calendar.getInstance();
    private TextView encounterDateTextView;
    private final Boolean IS_SHR_DATA = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeUtils.getInstance().onCreate(getActivity(),true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patient_observations, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initializeResources(view);
    }

    private void initializeResources(View view) {
        initPager(view);
        initPagerIndicator(view);
//        logEvent("VIEW_CLIENT_OBS_BY_CONCEPT", "{\"patientuuid\":\"" + patient.getUuid() + "\"}");
    }

    private void initPagerIndicator(View view) {
        pagerTabsLayout = (PagerSlidingTabStrip) view.findViewById(R.id.pager_indicator);
        pagerTabsLayout.setTextColor(pagerTabsLayout.getIndicatorTextColor());
        pagerTabsLayout.setTextSize((int) getResources().getDimension(R.dimen.pager_indicator_text_size));
        pagerTabsLayout.setSelectedTextColor(getResources().getColor(R.color.tab_indicator));
        pagerTabsLayout.setViewPager(viewPager);
        viewPager.setCurrentItem(0);
        pagerTabsLayout.markCurrentSelected(0);
    }

    private void initPager(View view) {
        try {
            viewPager = view.findViewById(R.id.pager);
            String patientUUid = (String) getActivity().getIntent().getSerializableExtra(PatientSummaryActivity.PATIENT_UUID);
            Patient patient = ((MuzimaApplication) getActivity().getApplicationContext()).getPatientController().getPatientByUuid(patientUUid);
            observationsPagerAdapter = new ObservationsPagerAdapter(getActivity().getApplicationContext(), getChildFragmentManager(), IS_SHR_DATA, patient);
            observationsPagerAdapter.initPagerViews();
            viewPager.setAdapter(observationsPagerAdapter);
        } catch (PatientController.PatientLoadException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        observationsPagerAdapter.cancelBackgroundQueryTasks();
    }

    public void showDatePicketDialog(View view) {
        encounterDateTextView = (TextView) view;
        DatePickerDialog datePickerDialog = new DatePickerDialog(view.getContext(), new DateSetListener(), today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    public class DateSetListener implements DatePickerDialog.OnDateSetListener {

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            monthOfYear = monthOfYear + 1;
            String month = "" + monthOfYear;
            if (monthOfYear < 10) {
                month = "0" + monthOfYear;
            }
            String day = "" + dayOfMonth;
            if (dayOfMonth < 10) {
                day = "0" + dayOfMonth;
            }
            encounterDateTextView.setText(day + "-" + month + "-" + year);
        }
    }
}
