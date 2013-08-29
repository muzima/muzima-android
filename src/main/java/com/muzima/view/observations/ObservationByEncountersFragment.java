package com.muzima.view.observations;

import android.view.View;
import android.widget.AdapterView;
import com.muzima.controller.PatientController;
import com.muzima.view.patients.ObservationsListFragment;

public class ObservationByEncountersFragment extends ObservationsListFragment{
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    }

    public static ObservationByEncountersFragment newInstance(PatientController patientController) {
        ObservationByEncountersFragment f = new ObservationByEncountersFragment();
        f.patientController = patientController;
        f.setRetainInstance(true);
        return f;
    }
}
