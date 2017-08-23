/*
 * Copyright (c) 2014 - 2017. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.cohort;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import com.muzima.R;
import com.muzima.adapters.cohort.SyncedCohortsAdapter;
import com.muzima.api.model.Cohort;
import com.muzima.controller.CohortController;
import com.muzima.view.patients.PatientsListActivity;

public class SyncedCohortsListFragment extends CohortListFragment implements AllCohortsListFragment.OnCohortDataDownloadListener {
    private static final String TAG = "SyncedCohortsListFragment";

    public static SyncedCohortsListFragment newInstance(CohortController cohortController) {
        SyncedCohortsListFragment f = new SyncedCohortsListFragment();
        f.cohortController = cohortController;
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if(listAdapter == null){
            listAdapter = new SyncedCohortsAdapter(getActivity(), R.layout.item_synced_cohorts_list, cohortController);
        }
        noDataMsg = getActivity().getResources().getString(R.string.info_no_cohort_download);
        noDataTip = getActivity().getResources().getString(R.string.hint_cohort_sync);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        unselectAllItems(list);
        Cohort cohort = (Cohort) listAdapter.getItem(position);
        Intent intent = new Intent(getActivity(), PatientsListActivity.class);
        intent.putExtra(PatientsListActivity.COHORT_ID, cohort.getUuid());
        intent.putExtra(PatientsListActivity.COHORT_NAME, cohort.getName());
        startActivity(intent);
    }

    @Override
    protected String getSuccessMsg(Integer[] status) {
        return "Downloaded " + status[2] + " patients for " + status[1] + " cohorts";
    }

    @Override
    public void onCohortDataDownloadComplete() {
        listAdapter.reloadData();
    }
}
