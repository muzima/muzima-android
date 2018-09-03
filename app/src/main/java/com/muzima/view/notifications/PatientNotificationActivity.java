/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.notifications;

import android.content.Intent;
import android.os.Bundle;
import com.muzima.R;
import com.muzima.adapters.MuzimaPagerAdapter;
import com.muzima.adapters.notification.PatientNotificationPagerAdapter;
import com.muzima.api.model.Patient;
import com.muzima.view.patients.PatientSummaryActivity;

public class PatientNotificationActivity extends NotificationActivityBase {
    private Patient patient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_pager);
        Intent intent = getIntent();
        patient = (Patient) intent.getSerializableExtra(PatientSummaryActivity.PATIENT);
        initPager();
        initPagerIndicator();
        getSupportActionBar().setTitle(patient.getSummary());
    }

    @Override
    protected MuzimaPagerAdapter createNotificationsPagerAdapter() {
        return new PatientNotificationPagerAdapter(getApplicationContext(), getSupportFragmentManager(), patient);
    }
}
