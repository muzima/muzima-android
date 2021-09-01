/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.fragments.cohorts;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.cohort.CohortRecyclerViewAdapter;
import com.muzima.api.model.Cohort;
import com.muzima.model.cohort.CohortItem;
import com.muzima.model.events.CohortSearchEvent;
import com.muzima.model.events.CohortsActionModeEvent;
import com.muzima.model.events.DestroyActionModeEvent;
import com.muzima.tasks.CohortSearchTask;
import com.muzima.tasks.LoadAllCohortsTask;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AllCohortsListFragment extends Fragment implements CohortRecyclerViewAdapter.OnCohortClickedListener {
    private ProgressBar progressBar;
    private RecyclerView cohortListRecyclerView;
    private CohortRecyclerViewAdapter recyclerViewAdapter;
    private List<CohortItem> allCohortsList = new ArrayList<>();

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

    @Override
    public void onStart() {
        super.onStart();
        try {
            EventBus.getDefault().register(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void cohortSearchEvent(CohortSearchEvent event) {
        if (event.getPage() == 0)
            searchCohorts(event.getSearchTerm());
    }

    private void searchCohorts(String searchTerm) {
        if (getActivity() == null) return;
        ((MuzimaApplication) getActivity().getApplicationContext()).getExecutorService()
                .execute(new CohortSearchTask(getActivity().getApplicationContext(), searchTerm, new CohortSearchTask.CohortSearchCallback() {
                    @Override
                    public void onCohortSearchFinished(final List<Cohort> cohortList) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                renderCohortsList(cohortList);
                            }
                        });
                    }
                }));
    }

    private void renderCohortsList(final List<Cohort> cohorts) {
        allCohortsList.clear();
        for (Cohort cohort : cohorts) {
            allCohortsList.add(new CohortItem(cohort));
        }
        recyclerViewAdapter.notifyDataSetChanged();
        progressBar.setVisibility(View.GONE);
        cohortListRecyclerView.setVisibility(View.VISIBLE);
    }

    private void loadData() {
        cohortListRecyclerView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        ((MuzimaApplication) getActivity().getApplicationContext()).getExecutorService()
                .execute(new LoadAllCohortsTask(getActivity().getApplicationContext(), new LoadAllCohortsTask.OnAllCohortsLoadedCallback() {
                    @Override
                    public void onCohortsLoaded(final List<Cohort> cohorts) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                allCohortsList.clear();
                                for (Cohort cohort : cohorts) {
                                    allCohortsList.add(new CohortItem(cohort, false));
                                }
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
        progressBar = view.findViewById(R.id.progress_bar);
        recyclerViewAdapter = new CohortRecyclerViewAdapter(getActivity().getApplicationContext(), allCohortsList, this);
        cohortListRecyclerView.setAdapter(recyclerViewAdapter);
        cohortListRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));
    }

    @Override
    public void onCohortClicked(int position) {
        if (allCohortsList.isEmpty() || position < 0) return;
        CohortItem selectedCohortItem = allCohortsList.get(position);
        selectedCohortItem.setSelected(!selectedCohortItem.isSelected());
        recyclerViewAdapter.notifyDataSetChanged();
        EventBus.getDefault().post(new CohortsActionModeEvent(allCohortsList));
    }

    @Subscribe
    public void actionModeClearedEvent(DestroyActionModeEvent event) {
        for (CohortItem cohortItem : allCohortsList) {
            cohortItem.setSelected(false);
        }
        recyclerViewAdapter.notifyDataSetChanged();
    }
}
