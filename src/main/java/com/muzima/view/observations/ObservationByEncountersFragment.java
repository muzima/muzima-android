package com.muzima.view.observations;

import android.view.View;
import android.widget.AdapterView;
import com.muzima.controller.ObservationController;
import com.muzima.view.patients.ObservationsListFragment;

public class ObservationByEncountersFragment extends ObservationsListFragment{
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    }

    public static ObservationByEncountersFragment newInstance(ObservationController observationController) {
        ObservationByEncountersFragment f = new ObservationByEncountersFragment();
        f.observationController = observationController;
        return f;
    }

    @Override
    public void onSearchTextChange(String query) {
    }
}
