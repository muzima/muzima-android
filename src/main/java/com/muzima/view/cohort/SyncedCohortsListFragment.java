package com.muzima.view.cohort;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import com.muzima.R;
import com.muzima.adapters.cohort.SyncedCohortsAdapter;
import com.muzima.api.model.Cohort;
import com.muzima.controller.CohortController;
import com.muzima.view.patients.PatientsListActivity;

public class SyncedCohortsListFragment extends CohortListFragment implements AllCohortsListFragment.OnCohortDataDownloadListener {
    private static final String TAG = "SyncedCohortsListFragment";
    private ListView listView;

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
        noDataMsg = getActivity().getResources().getString(R.string.no_cohorts_synced);
        noDataTip = getActivity().getResources().getString(R.string.no_cohorts_synced_tip);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        unselectAllItems(listView);
        Cohort cohort = (Cohort) listAdapter.getItem(position);
        Intent intent = new Intent(getActivity(), PatientsListActivity.class);
        intent.putExtra(PatientsListActivity.COHORT_ID, cohort.getUuid());
        intent.putExtra(PatientsListActivity.COHORT_NAME, cohort.getName());
        startActivity(intent);
    }

    private void unselectAllItems(ListView listView) {
        for (int i = listView.getCount() - 1; i >= 0; i--){
            listView.setItemChecked(i, false);
        }
    }

    @Override
    protected View setupMainView(LayoutInflater inflater, ViewGroup container) {
        View view = inflater.inflate(R.layout.layout_synced_list, container, false);
        listView = (ListView) view.findViewById(R.id.list);
        return view;
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
