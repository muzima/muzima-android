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
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.muzima.R;
import com.muzima.adapters.RecyclerAdapter;
import com.muzima.adapters.cohort.CohortsAdapter;
import com.muzima.model.cohort.CohortItem;
import com.muzima.model.events.CohortSearchEvent;
import com.muzima.model.events.CohortsDownloadedEvent;
import com.muzima.model.events.DestroyActionModeEvent;
import com.muzima.utils.Constants;
import com.muzima.utils.NetworkUtils;
import com.muzima.view.custom.MuzimaRecyclerView;
import com.muzima.view.patients.SyncPatientDataIntent;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AvailableCohortsFragment extends Fragment implements CohortsAdapter.OnCohortClickedListener, RecyclerAdapter.BackgroundListQueryTaskListener {
    private ActionMode actionMode;
    private boolean actionModeActive = false;
    private ProgressBar progressBar;
    private CohortsAdapter cohortsAdapter;
    private boolean cohortsSyncInProgress;
    private MenuItem loadingMenuItem;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cohorts_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MuzimaRecyclerView cohortRecyclerView = view.findViewById(R.id.cohorts_list_recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        cohortsAdapter = new CohortsAdapter(requireActivity().getApplicationContext(), this, Constants.COHORT_LIST_TYPE.ONLINE);
        cohortsAdapter.setBackgroundListQueryTaskListener(this);
        cohortRecyclerView.setAdapter(cohortsAdapter);
        cohortRecyclerView.setLayoutManager(new LinearLayoutManager(requireActivity().getApplicationContext()));
        cohortsAdapter.reloadData();
        cohortRecyclerView.setNoDataLayout(view.findViewById(R.id.no_data_layout),
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

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Subscribe
    public void cohortSearchEvent(CohortSearchEvent event) {
        if (event.getPage() == 2)
            cohortsAdapter.getFilter().filter(event.getSearchTerm());
    }

    @Override
    public void onCohortClicked(boolean isChecked) {
        if (!actionModeActive && isChecked) {
            actionMode = requireActivity().startActionMode(new AvailableCohortsFragment.AvailableCohortsActionModeCallback());
            actionModeActive = true;
        }
        int numOfSelectedCohorts = cohortsAdapter.numberOfCohorts();
        if (numOfSelectedCohorts == 0 && actionModeActive) {
            actionMode.finish();
        }
        Log.d(getClass().getSimpleName(), "isnull:" + (actionMode == null));
        actionMode.setTitle(String.format(Locale.getDefault(), "%d %s", numOfSelectedCohorts, getResources().getString(R.string.general_selected)));
    }

    @Subscribe
    public void onCohortDownloadFinish(CohortsDownloadedEvent event) {
        cohortsSyncInProgress = false;
        cohortsAdapter.reloadData();
        endActionMode();
    }


    @Subscribe
    public void onTabSwitched(DestroyActionModeEvent event) {
        endActionMode();
    }

    final class AvailableCohortsActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            requireActivity().getMenuInflater().inflate(R.menu.menu_cohort_actions, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            loadingMenuItem = menu.findItem(R.id.menu_downloading_action);
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            if (menuItem.getItemId() == R.id.menu_download_action) {
                loadingMenuItem.setActionView(new ProgressBar(requireActivity()));
                loadingMenuItem.setVisible(true);
                menuItem.setVisible(false);
                if (cohortsSyncInProgress) {
                    Toast.makeText(getActivity(), R.string.error_sync_not_allowed, Toast.LENGTH_SHORT).show();
                    endActionMode();
                    return false;
                }

                if (!NetworkUtils.isConnectedToNetwork(requireActivity())) {
                    Toast.makeText(getActivity(), R.string.error_local_connection_unavailable, Toast.LENGTH_SHORT).show();
                    return true;
                }

                syncPatientsAndObservationsInBackgroundService();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            actionModeActive = false;
            cohortsAdapter.clearSelectedCohorts();
        }
    }

    public void endActionMode() {
        if (this.actionMode != null) {
            this.actionMode.finish();
        }
    }

    private void syncPatientsAndObservationsInBackgroundService() {
        cohortsSyncInProgress = true;
        new SyncPatientDataIntent(getActivity(), cohortsAdapter.getSelectedCohortsArray()).start();
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
