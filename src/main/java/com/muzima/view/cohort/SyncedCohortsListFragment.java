package com.muzima.view.cohort;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.muzima.R;
import com.muzima.adapters.cohort.AllCohortsAdapter;
import com.muzima.controller.CohortController;

public class SyncedCohortsListFragment extends CohortListFragment{

    public static SyncedCohortsListFragment newInstance(CohortController cohortController) {
        SyncedCohortsListFragment f = new SyncedCohortsListFragment();
        f.cohortController = cohortController;
        f.setRetainInstance(true);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        noDataMsg = getActivity().getResources().getString(R.string.no_cohorts_synced);
        noDataTip = getActivity().getResources().getString(R.string.no_cohorts_synced_tip);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
    }

}
