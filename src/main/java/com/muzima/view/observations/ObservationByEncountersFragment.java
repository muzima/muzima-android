package com.muzima.view.observations;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import com.muzima.R;
import com.muzima.adapters.observations.ObservationsByConceptAdapter;
import com.muzima.adapters.observations.ObservationsByEncounterAdapter;
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
    public void onCreate(Bundle savedInstanceState) {
        if(listAdapter == null){
            listAdapter = new ObservationsByEncounterAdapter(
                    getActivity(), R.layout.item_observation_list, conceptController, observationController);
        }
        noDataMsg = getActivity().getResources().getString(R.string.no_observations_available);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onSearchTextChange(String query) {
    }
}
