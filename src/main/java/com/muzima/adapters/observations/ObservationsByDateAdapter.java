package com.muzima.adapters.observations;

import android.support.v4.app.FragmentActivity;
import com.muzima.adapters.ListAdapter;
import com.muzima.controller.PatientController;

public class ObservationsByDateAdapter extends ObservationsAdapter {
    public ObservationsByDateAdapter(FragmentActivity activity, int itemCohortsList, PatientController patientController) {
        super(activity,itemCohortsList, patientController);
    }

    @Override
    public void reloadData() {
    }
}
