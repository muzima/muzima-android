package com.muzima.view.custom;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.controller.MuzimaSettingController;
import com.muzima.view.BroadcastListenerActivity;
import com.muzima.view.MainDashboardActivity;
import com.muzima.view.patients.DataCollectionActivity;
import com.muzima.view.patients.ObsViewActivity;
import com.muzima.view.patients.PatientSummaryActivity;

public abstract class ActivityWithPatientSummaryBottomNavigation extends BroadcastListenerActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    protected BottomNavigationView navigationView;
    private String patientUuid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void loadBottomNavigation(String patientUuid) {
        navigationView = findViewById(R.id.bottom_navigation);
        navigationView.setOnNavigationItemSelectedListener(this);
        this.patientUuid = patientUuid;

        MuzimaSettingController muzimaSettingController = ((MuzimaApplication) getApplicationContext()).getMuzimaSettingController();
        if(!muzimaSettingController.isHistoricalDataTabEnabled()){
            MenuItem historicalDataMenu = navigationView.getMenu().findItem(R.id.action_historical_data);
            historicalDataMenu.setVisible(false);
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        updateNavigationBarState();
    }

    // Remove inter-activity transition to avoid screen tossing on tapping bottom navigation items
    @Override
    public void onPause() {
        super.onPause();
        overridePendingTransition(0, 0);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == getBottomNavigationMenuItemId())
            return true;

        navigationView.post(() -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_home) {
                startActivity(new Intent(this, MainDashboardActivity.class));
            } else if (itemId == R.id.action_data_collection) {
                Intent intent = new Intent(this, DataCollectionActivity.class);
                intent.putExtra(PatientSummaryActivity.PATIENT_UUID, patientUuid);
                startActivity(intent);
            } else if (itemId == R.id.action_historical_data) {
                Intent intent = new Intent(this, ObsViewActivity.class);
                intent.putExtra(PatientSummaryActivity.PATIENT_UUID, patientUuid);
                startActivity(intent);
            }
        });
        return true;
    }

    private void updateNavigationBarState() {
        int actionId = getBottomNavigationMenuItemId();
        selectBottomNavigationBarItem(actionId);
    }

    private void selectBottomNavigationBarItem(int itemId) {
        MenuItem item = navigationView.getMenu().findItem(itemId);
        if(item != null) {
            item.setChecked(true);
        }
    }

    protected abstract int getBottomNavigationMenuItemId();

    protected BottomNavigationView getBottomNavigationView(){
        return navigationView;
    }
}
