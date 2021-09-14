/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

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
import com.muzima.R;
import com.muzima.adapters.RecyclerAdapter;
import com.muzima.adapters.cohort.CohortsAdapter;
import com.muzima.model.events.CohortSearchEvent;
import com.muzima.model.events.CohortsDownloadedEvent;
import com.muzima.utils.Constants;
import com.muzima.view.custom.MuzimaRecyclerView;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class DownloadedCohortsFragment extends Fragment implements CohortsAdapter.OnCohortClickedListener, RecyclerAdapter.BackgroundListQueryTaskListener {
    private ProgressBar progressBar;
    private CohortsAdapter cohortsAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cohorts_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MuzimaRecyclerView cohortListRecyclerView = view.findViewById(R.id.cohorts_list_recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        cohortsAdapter = new CohortsAdapter(requireActivity().getApplicationContext(), this, Constants.COHORT_LIST_TYPE.DOWNLOADED);
        cohortsAdapter.setBackgroundListQueryTaskListener(this);
        cohortListRecyclerView.setLayoutManager(new LinearLayoutManager(requireActivity().getApplicationContext()));
        cohortListRecyclerView.setAdapter(cohortsAdapter);
        cohortsAdapter.reloadData();
        cohortListRecyclerView.setNoDataLayout(view.findViewById(R.id.no_data_layout),
                getString(R.string.info_no_cohort_download),
                getString(R.string.hint_cohort_sync));
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
            cohortsAdapter.getFilter().filter(event.getSearchTerm());
    }

    @Subscribe
    public void onCohortDownloadFinish(CohortsDownloadedEvent event) {
        cohortsAdapter.reloadData();
    }

    @Override
    public void onCohortClicked(boolean isChecked) {
        cohortsAdapter.clearSelectedCohorts();
    }

    @Override
    public void onQueryTaskStarted() {}

    @Override
    public void onQueryTaskFinish() {
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onQueryTaskCancelled() {}
}
