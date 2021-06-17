/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.cohort;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.cohort.AllCohortsAdapter;
import com.muzima.adapters.cohort.CohortRecyclerViewAdapter;
import com.muzima.api.model.APIName;
import com.muzima.api.model.Cohort;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.controller.CohortController;
import com.muzima.tasks.LoadAllCohortsTask;
import com.muzima.utils.DateUtils;
import com.muzima.utils.NetworkUtils;
import com.muzima.view.CheckedLinearLayout;
import com.muzima.view.patients.SyncPatientDataIntent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AllCohortsListFragment extends Fragment {

    private ProgressBar progressBar;
    private RecyclerView cohortListRecyclerView;
    private CohortRecyclerViewAdapter recyclerViewAdapter;
    private List<Cohort> allCohortsList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cohorts_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initializeResources(view);
        loadData();
    }

    private void loadData() {
        cohortListRecyclerView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        ((MuzimaApplication) getActivity().getApplicationContext()).getExecutorService()
                .execute( new LoadAllCohortsTask(getActivity().getApplicationContext(), new LoadAllCohortsTask.OnAllCohortsLoadedCallback() {
                    @Override
                    public void onCohortsLoaded(final List<Cohort> cohorts) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                allCohortsList.addAll(cohorts);
                                recyclerViewAdapter.notifyDataSetChanged();
                                progressBar.setVisibility(View.GONE);
                                cohortListRecyclerView.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                }));
    }

    private void initializeResources(View view) {
        cohortListRecyclerView = view.findViewById(R.id.cohorts_list_recycler_view);
        progressBar = view.findViewById(R.id.chorts_load_progress_bar);
        recyclerViewAdapter = new CohortRecyclerViewAdapter(getActivity().getApplicationContext(), allCohortsList);
        cohortListRecyclerView.setAdapter(recyclerViewAdapter);
        cohortListRecyclerView.addItemDecoration( new DividerItemDecoration(getActivity().getApplicationContext(), RecyclerView.VERTICAL));
        cohortListRecyclerView.setLayoutManager( new LinearLayoutManager(getActivity().getApplicationContext()));
    }
}
