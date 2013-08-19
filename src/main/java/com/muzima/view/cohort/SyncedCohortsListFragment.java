package com.muzima.view.cohort;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.muzima.R;
import com.muzima.adapters.cohort.AllCohortsAdapter;
import com.muzima.adapters.cohort.SyncedCohortsAdapter;
import com.muzima.api.model.Cohort;
import com.muzima.api.model.Form;
import com.muzima.controller.CohortController;
import com.muzima.view.forms.FormWebViewActivity;
import com.muzima.view.patients.CohortPatientsActivity;

public class SyncedCohortsListFragment extends CohortListFragment{

    public static SyncedCohortsListFragment newInstance(CohortController cohortController) {
        SyncedCohortsListFragment f = new SyncedCohortsListFragment();
        f.cohortController = cohortController;
        f.setRetainInstance(true);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(listAdapter == null){
            listAdapter = new SyncedCohortsAdapter(getActivity(), R.layout.item_cohorts_list, cohortController);
        }
        noDataMsg = getActivity().getResources().getString(R.string.no_cohorts_synced);
        noDataTip = getActivity().getResources().getString(R.string.no_cohorts_synced_tip);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Cohort cohort = (Cohort) listAdapter.getItem(position);
        Intent intent = new Intent(getActivity(), CohortPatientsActivity.class);
        intent.putExtra(CohortPatientsActivity.COHORT_ID, cohort.getUuid());
        startActivity(intent);
    }

}
