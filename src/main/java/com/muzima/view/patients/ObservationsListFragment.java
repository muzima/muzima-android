package com.muzima.view.patients;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.muzima.R;
import com.muzima.controller.ConceptController;
import com.muzima.controller.ObservationController;
import com.muzima.view.MuzimaListFragment;

public abstract class ObservationsListFragment extends MuzimaListFragment{
    private static final String TAG = "ObservationsListFragment";

    protected ConceptController conceptController;
    protected ObservationController observationController;
    protected FrameLayout progressBarContainer;
    protected LinearLayout noDataView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View observationsLayout = setupMainView(inflater,container);
        list = (ListView) observationsLayout.findViewById(R.id.list);
        progressBarContainer = (FrameLayout) observationsLayout.findViewById(R.id.progressbarContainer);
        noDataView = (LinearLayout) observationsLayout.findViewById(R.id.no_data_layout);

        setupNoDataView(observationsLayout);

        // Todo no need to do this check after all list adapters are implemented
        if (listAdapter != null) {
            list.setAdapter(listAdapter);
            list.setOnItemClickListener(this);
//            ((CohortsAdapter)listAdapter).setBackgroundListQueryTaskListener(this);
        }
        list.setEmptyView(observationsLayout.findViewById(R.id.no_data_layout));

        return observationsLayout;
    }

    protected View setupMainView(LayoutInflater inflater, ViewGroup container){
        return inflater.inflate(R.layout.layout_list, container, false);
    }

    @Override
    public void synchronizationComplete(Integer[] status) {
    }

    public abstract void onSearchTextChange(String query);
}
