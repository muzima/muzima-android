/*
 * Copyright (c) 2014 - 2017. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.observations;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import com.muzima.R;
import com.muzima.adapters.observations.ObservationsByConceptAdapter;
import com.muzima.controller.ConceptController;
import com.muzima.controller.ObservationController;

public class ObservationsByConceptFragment extends ObservationsListFragment {

    private Boolean isShrData;

    public static ObservationsByConceptFragment newInstance(ConceptController conceptController, ObservationController observationController,Boolean isShrData) {
        ObservationsByConceptFragment f = new ObservationsByConceptFragment();
        f.observationController = observationController;
        f.conceptController = conceptController;
        f.isShrData = isShrData;
        return f;
    }

   

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if(listAdapter == null){
            listAdapter = new ObservationsByConceptAdapter(
                    getActivity(), R.layout.item_observation_by_concept_list, conceptController, observationController,isShrData);
        }
        noDataMsg = getActivity().getResources().getString(R.string.info_observation_in_progress);
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




    @Override
    public void onQueryTaskCancelled(){
        ObservationsByConceptAdapter observationsByConceptAdapter = ((ObservationsByConceptAdapter)listAdapter);
        observationsByConceptAdapter.cancelBackgroundQueryTask();
    }

    @Override
    public void onQueryTaskCancelled(Object status){
        ObservationsByConceptAdapter observationsByConceptAdapter = ((ObservationsByConceptAdapter)listAdapter);
        observationsByConceptAdapter.cancelBackgroundQueryTask();
    }

}
