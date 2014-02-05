
package com.muzima.view.notifications;

import android.content.Intent;
import android.os.Bundle;
import com.muzima.R;
import com.muzima.adapters.MuzimaPagerAdapter;
import com.muzima.adapters.notification.NotificationPagerAdapter;
import com.muzima.api.model.Patient;
import com.muzima.view.patients.PatientSummaryActivity;


public class PatientNotificationActivity extends NotificationActivityBase {
    private static final String TAG = "PatientNotificationActivity";
    private Patient patient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_with_pager);
        Intent intent = getIntent();
        patient = (Patient) intent.getSerializableExtra(PatientSummaryActivity.PATIENT);
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle(patient.getFamilyName() + ", " + patient.getGivenName() + " " + patient.getMiddleName());
    }


    @Override
    protected MuzimaPagerAdapter createNotificationsPagerAdapter() {
        System.out.println("inside PatientNotificationActivity and patient =" + patient);
        return new NotificationPagerAdapter(getApplicationContext(), getSupportFragmentManager(), patient);
    }
}
