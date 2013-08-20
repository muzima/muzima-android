package com.muzima.view.cohort;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import com.muzima.R;
import com.muzima.adapters.cohort.SyncedCohortsAdapter;
import com.muzima.api.model.Cohort;
import com.muzima.controller.CohortController;
import com.muzima.tasks.DownloadMuzimaTask;
import com.muzima.view.patients.CohortPatientsActivity;

public class SyncedCohortsListFragment extends CohortListFragment implements AllCohortsListFragment.OnCohortDataDownloadListener {
    private static final String TAG = "SyncedCohortsListFragment";

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

    @Override
    public void formDownloadComplete(Integer[] status) {
        Integer downloadStatus = status[0];
        String msg = "Download Complete with status " + downloadStatus;
        Log.i(TAG, msg);
        if (downloadStatus == DownloadMuzimaTask.SUCCESS) {
            msg = "Downloaded " + status[2] + " patients for " + status[1] + " cohorts";
            if (listAdapter != null) {
                listAdapter.reloadData();
            }
        } else if (downloadStatus == DownloadMuzimaTask.DOWNLOAD_ERROR) {
            msg = "An error occurred while downloading cohorts";
        } else if (downloadStatus == DownloadMuzimaTask.AUTHENTICATION_ERROR) {
            msg = "Authentication error occurred while downloading cohorts";
        } else if (downloadStatus == DownloadMuzimaTask.DELETE_ERROR) {
            msg = "An error occurred while deleting existing cohorts";
        } else if (downloadStatus == DownloadMuzimaTask.SAVE_ERROR) {
            msg = "An error occurred while saving the downloaded cohorts";
        } else if (downloadStatus == DownloadMuzimaTask.CANCELLED) {
            msg = "Cohort download task has been cancelled";
        } else if (downloadStatus == DownloadMuzimaTask.CONNECTION_ERROR) {
            msg = "Connection error occurred while downloading cohorts";
        } else if (downloadStatus == DownloadMuzimaTask.PARSING_ERROR) {
            msg = "Parse exception has been thrown while fetching data";
        }else if (downloadStatus == DownloadMuzimaTask.REPLACE_ERROR) {
            msg = "An error occurred while replace existing cohort data";
        }
        Toast.makeText(getActivity().getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCohortDataDownloadComplete(Integer[] result) {
        formDownloadComplete(result);
    }
}
