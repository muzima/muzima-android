/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.patients;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.adapters.observations.ObservationsAdapter;
import com.muzima.controller.ConceptController;
import com.muzima.controller.ObservationController;
import com.muzima.view.MuzimaListFragment;

public abstract class ObservationsListFragment extends MuzimaListFragment implements ListAdapter.BackgroundListQueryTaskListener{
    private static final String TAG = "ObservationsListFragment";

    protected ConceptController conceptController;
    protected ObservationController observationController;
    protected FrameLayout progressBarContainer;
    protected LinearLayout noDataView;
    protected View observationsLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        observationsLayout = setupMainView(inflater,container);
        list = (ListView) observationsLayout.findViewById(R.id.list);
        progressBarContainer = (FrameLayout) observationsLayout.findViewById(R.id.progressbarContainer);
        noDataView = (LinearLayout) observationsLayout.findViewById(R.id.no_data_layout);

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

    protected View setupMainView(LayoutInflater inflater, ViewGroup container){
        return inflater.inflate(R.layout.layout_list, container, false);
    }

    public abstract void onSearchTextChange(String query);

    @Override
    public void onQueryTaskStarted() {
        noDataMsg = "Observations are loading";
        updateDataLoadStatus(observationsLayout, noDataMsg);
    }

    @Override
    public void onQueryTaskFinish() {
        if(listAdapter.isEmpty()){
            noDataMsg = "No observations Available";
            updateDataLoadStatus(observationsLayout, noDataMsg);
        }
    }
}
