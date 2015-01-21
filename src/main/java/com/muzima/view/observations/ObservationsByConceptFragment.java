/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.observations;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import com.muzima.R;
import com.muzima.adapters.observations.ObservationsByConceptAdapter;
import com.muzima.controller.ConceptController;
import com.muzima.controller.ObservationController;
import com.muzima.view.patients.ObservationsListFragment;

public class ObservationsByConceptFragment extends ObservationsListFragment {

    public static ObservationsByConceptFragment newInstance(ConceptController conceptController, ObservationController observationController) {
        ObservationsByConceptFragment f = new ObservationsByConceptFragment();
        f.observationController = observationController;
        f.conceptController = conceptController;
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if(listAdapter == null){
            listAdapter = new ObservationsByConceptAdapter(
                    getActivity(), R.layout.item_observation_by_concept_list, conceptController, observationController);
        }
        noDataMsg = getActivity().getResources().getString(R.string.observations_still_loading);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected View setupMainView(LayoutInflater inflater, ViewGroup container) {
        View view = inflater.inflate(R.layout.layout_synced_list, container, false);
        return view;
    }

    @Override
    public void onSearchTextChange(String query) {
        ((ObservationsByConceptAdapter)listAdapter).search(query);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    }

}
