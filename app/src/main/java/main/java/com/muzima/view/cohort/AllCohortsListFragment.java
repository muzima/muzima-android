/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.cohort;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.Toast;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
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
    private static final String TAG = "AllCohortsListFragment";
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
        noDataMsg = getActivity().getResources().getString(R.string.no_cohorts_available);
        noDataTip = getActivity().getResources().getString(R.string.no_cohorts_available_tip);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected View setupMainView(LayoutInflater inflater, ViewGroup container) {
        View view = inflater.inflate(R.layout.layout_synced_list, container, false);
        syncText = (CheckedTextView) view.findViewById(R.id.sync_text);
        updateSyncText();
        return view;
    }

    @Override
    protected String getSuccessMsg(Integer[] status) {
        return "Downloaded: " + status[1] + " cohorts";
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        CheckedLinearLayout checkedLinearLayout = (CheckedLinearLayout) view;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            checkedLinearLayout.toggle();
        }
        boolean isChecked = checkedLinearLayout.isChecked();
        if (!actionModeActive && isChecked) {
            actionMode = getSherlockActivity().startActionMode(new AllCohortsActionModeCallback());
            actionModeActive = true;
        }
        ((AllCohortsAdapter) listAdapter).onListItemClick(position);
        int numOfSelectedCohorts = ((AllCohortsAdapter) listAdapter).numberOfCohorts();
        if (numOfSelectedCohorts == 0 && actionModeActive) {
            actionMode.finish();
        }
        Log.d(TAG, "isnull:" + String.valueOf(actionMode==null));
        actionMode.setTitle(String.valueOf(numOfSelectedCohorts));

    }

    public void setCohortDataDownloadListener(OnCohortDataDownloadListener cohortDataDownloadListener) {
        this.cohortDataDownloadListener = cohortDataDownloadListener;
    }

    public void onCohortDownloadFinish() {
        cohortsSyncInProgress = false;
        listAdapter.reloadData();
        updateSyncText();
    }

    public void onCohortDownloadStart() {
        cohortsSyncInProgress = true;
    }

    public void onPatientDownloadFinish() {
        if (cohortDataDownloadListener != null) {
            cohortDataDownloadListener.onCohortDataDownloadComplete();
        }
    }

    public final class AllCohortsActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            getSherlockActivity().getSupportMenuInflater().inflate(R.menu.actionmode_menu_download, menu);
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
                        Toast.makeText(getActivity(), "Action not allowed while sync is in progress", Toast.LENGTH_SHORT).show();
                        endActionMode();
                        break;
                    }

                    if (!NetworkUtils.isConnectedToNetwork(getActivity())) {
                        Toast.makeText(getActivity(), "No connection found, please connect your device and try again", Toast.LENGTH_SHORT).show();
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
        public void onCohortDataDownloadComplete();
    }

    private void updateSyncText() {
        try {
            LastSyncTimeService lastSyncTimeService = ((MuzimaApplication)this.getActivity().getApplicationContext()).getMuzimaContext().getLastSyncTimeService();//((MuzimaApplication)getApplicationContext()).getMuzimaContext().getLastSyncTimeService();
            Date lastSyncedTime = lastSyncTimeService.getLastSyncTimeFor(APIName.DOWNLOAD_COHORTS);
            String lastSyncedMsg = "Not synced yet";
            if(lastSyncedTime != null){
                lastSyncedMsg = "Last synced on: " + DateUtils.getFormattedDateTime(lastSyncedTime);
            }
            syncText.setText(lastSyncedMsg);
        } catch (IOException e) {
            Log.i(TAG,"Error getting cohort last sync time");
        }

    }

}
