package com.muzima.view.cohort;

import android.os.Bundle;
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
import com.muzima.tasks.LoadAvailableCohortsTask;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

public class AvailableCohortsFragment extends Fragment implements CohortRecyclerViewAdapter.OnCohortClickedListener {

    private ProgressBar progressBar;
    private RecyclerView cohortListRecyclerView;
    private CohortRecyclerViewAdapter recyclerViewAdapter;
    private List<CohortItem> allCohortsList = new ArrayList<>();
    private List<Cohort> selectedCohorts = new ArrayList<>();

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
        if (event.getPage() == 2)
            searchCohorts(event.getSearchTerm());
    }

    private void searchCohorts(String searchTerm) {
        ((MuzimaApplication) getActivity().getApplicationContext()).getExecutorService()
                .execute(new CohortSearchTask(getActivity().getApplicationContext(), searchTerm, new CohortSearchTask.CohortSearchCallback() {
                    @Override
                    public void onCohortSearchFinished(final List<Cohort> cohortList) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                List<Cohort> availableCohortsList = new ArrayList<>();
                                for (Cohort cohort : cohortList) {
                                    if (!((MuzimaApplication) getActivity().getApplicationContext()).getCohortController()
                                            .isDownloaded(cohort)) {
                                        availableCohortsList.add(cohort);
                                    }
                                }
                                renderCohortsList(availableCohortsList);
                            }
                        });
                    }
                }));
    }

    private void loadData() {
        cohortListRecyclerView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        ((MuzimaApplication) getActivity().getApplicationContext()).getExecutorService()
                .execute(new LoadAvailableCohortsTask(getActivity().getApplicationContext(), new LoadAvailableCohortsTask.OnCohortsLoadedCallback() {
                    @Override
                    public void onCohortsLoaded(final List<Cohort> cohorts) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                renderCohortsList(cohorts);
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

    private void initializeResources(View view) {
        cohortListRecyclerView = view.findViewById(R.id.cohorts_list_recycler_view);
        progressBar = view.findViewById(R.id.chorts_load_progress_bar);
        recyclerViewAdapter = new CohortRecyclerViewAdapter(getActivity().getApplicationContext(), allCohortsList, this);
        cohortListRecyclerView.setAdapter(recyclerViewAdapter);
        cohortListRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));
    }

    @Override
    public void onCohortClicked(int position) {
        Cohort cohort = allCohortsList.get(position).getCohort();
        selectedCohorts.add(cohort);
        recyclerViewAdapter.setSelectedCohorts(selectedCohorts);
        recyclerViewAdapter.notifyDataSetChanged();
        EventBus.getDefault().post(new CohortsActionModeEvent(selectedCohorts));
    }

    @Subscribe
    public void actionModeClearedEvent(DestroyActionModeEvent event) {
        selectedCohorts.clear();
        recyclerViewAdapter.setSelectedCohorts(selectedCohorts);
        recyclerViewAdapter.notifyDataSetChanged();
    }

}
