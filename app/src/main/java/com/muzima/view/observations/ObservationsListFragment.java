/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.observations;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.adapters.observations.ObservationsAdapter;
import com.muzima.controller.ConceptController;
import com.muzima.controller.EncounterController;
import com.muzima.controller.ObservationController;
import com.muzima.view.MuzimaListFragment;

public abstract class ObservationsListFragment extends MuzimaListFragment implements ListAdapter.BackgroundListQueryTaskListener{

    ConceptController conceptController;
    ObservationController observationController;
    EncounterController encounterController;
    private View observationsLayout;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        observationsLayout = setupMainView(inflater,container);
        list = observationsLayout.findViewById(R.id.list);
        FrameLayout progressBarContainer = observationsLayout.findViewById(R.id.progressbarContainer);
        LinearLayout noDataView = observationsLayout.findViewById(R.id.no_data_layout);

        setupNoDataView(observationsLayout);

        // Todo no need to do this check after all list adapters are implemented
        if (listAdapter != null) {
            list.setAdapter(listAdapter);
            list.setOnItemClickListener(this);
            ((ObservationsAdapter)listAdapter).setBackgroundListQueryTaskListener(this);
        }
        list.setEmptyView(observationsLayout.findViewById(R.id.no_data_layout));

        return observationsLayout;
    }

    View setupMainView(LayoutInflater inflater, ViewGroup container){
        return inflater.inflate(R.layout.layout_list, container, false);
    }

    public abstract void onSearchTextChange(String query);

    @Override
    public void onQueryTaskStarted() {
        noDataMsg = getActivity().getString(R.string.info_observation_load);
        updateDataLoadStatus(observationsLayout, noDataMsg);
    }

    @Override
    public void onQueryTaskFinish() {
        if(listAdapter.isEmpty()){
            noDataMsg = getActivity().getString(R.string.error_observation_unavailable);
            updateDataLoadStatus(observationsLayout, noDataMsg);
        }
    }
}
