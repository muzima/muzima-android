/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.observations;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import com.muzima.R;
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
                    getActivity(), R.layout.item_observation_by_encounter_list, conceptController, observationController);
        }
        noDataMsg = getActivity().getResources().getString(R.string.observations_still_loading);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onSearchTextChange(String query) {
        ((ObservationsByEncounterAdapter)listAdapter).search(query);
    }
}
