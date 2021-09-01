package com.muzima.view.fragments.cohorts;

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
import com.muzima.tasks.LoadDownloadedCohortsTask;

import com.muzima.utils.StringUtils;
import com.muzima.view.custom.MuzimaRecyclerView;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

public class DownloadedCohortsFragment extends Fragment implements CohortRecyclerViewAdapter.OnCohortClickedListener {
    private ProgressBar progressBar;
    private MuzimaRecyclerView cohortListRecyclerView;
    private CohortRecyclerViewAdapter recyclerViewAdapter;
    private List<CohortItem> downloadedCohortsList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cohorts_list, container, false);
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
        if (event.getPage() == 1)
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
                                List<Cohort> downloadedList = new ArrayList<>();
                                for (Cohort cohort : cohortList) {
                                    if (((MuzimaApplication) getActivity().getApplicationContext()).getCohortController()
                                            .isDownloaded(cohort)) {
                                        downloadedList.add(cohort);
                                    }
                                }
                                renderCohortsList(downloadedList);
                            }
                        });
                    }
                }));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        cohortListRecyclerView = view.findViewById(R.id.cohorts_list_recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        recyclerViewAdapter = new CohortRecyclerViewAdapter(getActivity().getApplicationContext(), downloadedCohortsList, this);
        cohortListRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));
        cohortListRecyclerView.setAdapter(recyclerViewAdapter);
        cohortListRecyclerView.setNoDataLayout(view.findViewById(R.id.no_data_layout),
                getString(R.string.info_cohorts_unavailable),
                StringUtils.EMPTY);

        loadData();
    }

    private void loadData() {
        cohortListRecyclerView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        ((MuzimaApplication) getActivity().getApplicationContext()).getExecutorService()
                .execute(new LoadDownloadedCohortsTask(getActivity().getApplicationContext(), new LoadDownloadedCohortsTask.OnDownloadedCohortsLoadedCallback() {
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
        downloadedCohortsList.clear();
        for (Cohort cohort : cohorts) {
            downloadedCohortsList.add(new CohortItem(cohort));
        }
        recyclerViewAdapter.notifyDataSetChanged();
        progressBar.setVisibility(View.GONE);
        cohortListRecyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCohortClicked(int position) {
        if (downloadedCohortsList.isEmpty() || position < 0) return;
        CohortItem selectedCohortItem = downloadedCohortsList.get(position);
        selectedCohortItem.setSelected(!selectedCohortItem.isSelected());
        recyclerViewAdapter.notifyDataSetChanged();
        EventBus.getDefault().post(new CohortsActionModeEvent(downloadedCohortsList));
    }

    @Subscribe
    public void actionModeClearedEvent(DestroyActionModeEvent event) {
        for (CohortItem cohortItem : downloadedCohortsList) {
            cohortItem.setSelected(false);
        }
        recyclerViewAdapter.notifyDataSetChanged();
    }
}
