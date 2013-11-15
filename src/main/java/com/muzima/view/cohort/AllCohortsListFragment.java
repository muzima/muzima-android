package com.muzima.view.cohort;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.Toast;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.R;
import com.muzima.adapters.cohort.AllCohortsAdapter;
import com.muzima.controller.CohortController;
import com.muzima.utils.Constants;
import com.muzima.utils.DateUtils;
import com.muzima.utils.NetworkUtils;
import com.muzima.view.CheckedLinearLayout;

import java.util.Date;

public class AllCohortsListFragment extends CohortListFragment {
    private static final String TAG = "AllCohortsListFragment";
    public static final String COHORTS_LAST_SYNCED_TIME = "cohortsSyncedTime";
    public static final long NOT_SYNCED_TIME = -1;

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
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        boolean isChecked = ((CheckedLinearLayout) view).isChecked();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            isChecked = !isChecked;
        }
        if (!actionModeActive && isChecked) {
            actionMode = getSherlockActivity().startActionMode(new AllCohortsActionModeCallback());
            actionModeActive = true;
        }
        ((AllCohortsAdapter) listAdapter).onListItemClick(position, isChecked);
        int numOfSelectedCohorts = ((AllCohortsAdapter) listAdapter).numberOfCohorts();
        if (numOfSelectedCohorts == 0 && actionModeActive) {
            actionMode.finish();
        }
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
                        if (AllCohortsListFragment.this.actionMode != null) {
                            AllCohortsListFragment.this.actionMode.finish();
                        }
                        break;
                    }

                    if (!NetworkUtils.isConnectedToNetwork(getActivity())) {
                        Toast.makeText(getActivity(), "No connection found, please connect your device and try again", Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    syncPatientsAndObservationsInBackgroundService();

                    if (AllCohortsListFragment.this.actionMode != null) {
                        AllCohortsListFragment.this.actionMode.finish();
                    }
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

    private void syncPatientsAndObservationsInBackgroundService() {
        ((CohortActivity) getActivity()).showProgressBar();
        new SyncPatientDataIntent(getActivity(), ((AllCohortsAdapter) listAdapter).getSelectedCohortsArray()).start();
    }

    public interface OnCohortDataDownloadListener {
        public void onCohortDataDownloadComplete();
    }

    private void updateSyncText() {
        SharedPreferences pref = getActivity().getSharedPreferences(Constants.SYNC_PREF, Context.MODE_PRIVATE);
        long lastSyncedTime = pref.getLong(COHORTS_LAST_SYNCED_TIME, NOT_SYNCED_TIME);
        String lastSyncedMsg = "Not synced yet";
        if (lastSyncedTime != NOT_SYNCED_TIME) {
            lastSyncedMsg = "Last synced on: " + DateUtils.getFormattedDateTime(new Date(lastSyncedTime));
        }
        syncText.setText(lastSyncedMsg);
    }

}
