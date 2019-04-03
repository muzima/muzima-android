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
import android.widget.Toast;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.cohort.AllCohortsAdapter;
import com.muzima.api.model.APIName;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.controller.CohortController;
import com.muzima.utils.DateUtils;
import com.muzima.utils.NetworkUtils;
import com.muzima.view.CheckedLinearLayout;
import com.muzima.view.patients.SyncPatientDataIntent;

import java.io.IOException;
import java.util.Date;

public class AllCohortsListFragment extends CohortListFragment {
    private ActionMode actionMode;
    private boolean actionModeActive = false;
    private OnCohortDataDownloadListener cohortDataDownloadListener;
    private CheckedTextView syncText;
    private boolean cohortsSyncInProgress;

    public static AllCohortsListFragment newInstance(CohortController cohortController) {
        AllCohortsListFragment f = new AllCohortsListFragment();
        f.cohortController = cohortController;
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (listAdapter == null) {
            listAdapter = new AllCohortsAdapter(getActivity(), R.layout.item_cohorts_list, cohortController);
        }
        noDataMsg = getActivity().getResources().getString(R.string.info_cohorts_unavailable);
        noDataTip = getActivity().getResources().getString(R.string.hint_cohort_list_download);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected View setupMainView(LayoutInflater inflater, ViewGroup container) {
        View view = inflater.inflate(R.layout.layout_synced_list, container, false);
        syncText = view.findViewById(R.id.sync_text);
        updateSyncText();
        return view;
    }

    @Override
    protected String getSuccessMsg(Integer[] status) {
        return getString(R.string.info_cohorts_downloaded, status[1]);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        CheckedLinearLayout checkedLinearLayout = (CheckedLinearLayout) view;
        boolean isChecked = checkedLinearLayout.isChecked();
        if (!actionModeActive && isChecked) {
            actionMode = getActivity().startActionMode(new AllCohortsActionModeCallback());
            actionModeActive = true;
        }
        ((AllCohortsAdapter) listAdapter).onListItemClick(position);
        int numOfSelectedCohorts = ((AllCohortsAdapter) listAdapter).numberOfCohorts();
        if (numOfSelectedCohorts == 0 && actionModeActive) {
            actionMode.finish();
        }
        Log.d(getClass().getSimpleName(), "isnull:" + String.valueOf(actionMode==null));
        actionMode.setTitle(String.valueOf(numOfSelectedCohorts));

    }

    public void setCohortDataDownloadListener(OnCohortDataDownloadListener cohortDataDownloadListener) {
        this.cohortDataDownloadListener = cohortDataDownloadListener;
    }

    public void onCohortDownloadFinish() {
        cohortsSyncInProgress = false;
        listAdapter.reloadData();
        listAdapter.notifyDataSetChanged();
        updateSyncText();
        if (cohortDataDownloadListener != null) {
            cohortDataDownloadListener.onCohortDataDownloadComplete();
        }
    }

    public void onCohortDownloadStart() {
        cohortsSyncInProgress = true;
    }

    public void onPatientDownloadFinish() {
        listAdapter.reloadData();
        listAdapter.notifyDataSetChanged();
        if (cohortDataDownloadListener != null) {
            cohortDataDownloadListener.onCohortDataDownloadComplete();
        }
    }

    final class AllCohortsActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            getActivity().getMenuInflater().inflate(R.menu.actionmode_menu_download, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.menu_download:
                    if (cohortsSyncInProgress) {
                        Toast.makeText(getActivity(), R.string.error_sync_not_allowed, Toast.LENGTH_SHORT).show();
                        endActionMode();
                        break;
                    }

                    if (!NetworkUtils.isConnectedToNetwork(getActivity())) {
                        Toast.makeText(getActivity(), R.string.error_local_connection_unavailable, Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    syncPatientsAndObservationsInBackgroundService();

                    endActionMode();
                    return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            actionModeActive = false;
            ((AllCohortsAdapter) listAdapter).clearSelectedCohorts();
            unselectAllItems(list);
        }
    }

    public void endActionMode() {
        if (this.actionMode != null) {
            this.actionMode.finish();
        }
    }

    private void syncPatientsAndObservationsInBackgroundService() {
        ((CohortActivity) getActivity()).showProgressBar();
        new SyncPatientDataIntent(getActivity(), ((AllCohortsAdapter) listAdapter).getSelectedCohortsArray()).start();
    }

    public interface OnCohortDataDownloadListener {
         void onCohortDataDownloadComplete();
    }

    private void updateSyncText() {
        try {
            LastSyncTimeService lastSyncTimeService = ((MuzimaApplication)this.getActivity().getApplicationContext()).getMuzimaContext().getLastSyncTimeService();//((MuzimaApplication)getApplicationContext()).getMuzimaContext().getLastSyncTimeService();
            Date lastSyncedTime = lastSyncTimeService.getLastSyncTimeFor(APIName.DOWNLOAD_COHORTS);
            String lastSyncedMsg = getActivity().getString(R.string.info_last_sync_unavailable);
            if(lastSyncedTime != null){
                lastSyncedMsg = getString(R.string.hint_last_synced,DateUtils.getFormattedDateTime(lastSyncedTime));
            }
            syncText.setText(lastSyncedMsg);
        } catch (IOException e) {
            Log.i(getClass().getSimpleName(),"Error getting cohort last sync time");
        }

    }

}
