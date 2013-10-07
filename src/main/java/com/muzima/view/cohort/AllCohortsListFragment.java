package com.muzima.view.cohort;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.R;
import com.muzima.adapters.cohort.AllCohortsAdapter;
import com.muzima.controller.CohortController;
import com.muzima.service.DataSyncService;
import com.muzima.utils.Constants;
import com.muzima.utils.DateUtils;
import com.muzima.utils.NetworkUtils;
import com.muzima.view.patients.MuzimaFragmentActivity;

import java.util.Date;
import java.util.List;

import static com.muzima.utils.Constants.DataSyncServiceConstants.COHORT_IDS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.CREDENTIALS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_PATIENTS;
import static com.muzima.utils.Constants.DataSyncServiceConstants.SYNC_TYPE;

public class AllCohortsListFragment extends CohortListFragment {
    private static final String TAG = "AllCohortsListFragment";
    public static final String COHORTS_LAST_SYNCED_TIME = "cohortsSyncedTime";
    public static final long NOT_SYNCED_TIME = -1;

    private ActionMode actionMode;
    private boolean actionModeActive = false;
    private OnCohortDataDownloadListener cohortDataDownloadListener;
    private TextView syncText;
    private boolean cohortsSyncInProgress;

    public static AllCohortsListFragment newInstance(CohortController cohortController) {
        AllCohortsListFragment f = new AllCohortsListFragment();
        f.cohortController = cohortController;
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if(listAdapter == null){
            listAdapter = new AllCohortsAdapter(getActivity(), R.layout.item_cohorts_list, cohortController);
        }
        noDataMsg = getActivity().getResources().getString(R.string.no_cohorts_available);
        noDataTip = getActivity().getResources().getString(R.string.no_cohorts_available_tip);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected View setupMainView(LayoutInflater inflater, ViewGroup container) {
        View view = inflater.inflate(R.layout.layout_synced_list, container, false);
        syncText = (TextView) view.findViewById(R.id.sync_text);
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
        if (!actionModeActive) {
            actionMode = getSherlockActivity().startActionMode(new AllCohortsActionModeCallback());
            actionModeActive = true;
        }
        ((AllCohortsAdapter) listAdapter).onListItemClick(position);
        int numOfSelectedCohorts = ((AllCohortsAdapter) listAdapter).getSelectedCohorts().size();
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
        if(cohortDataDownloadListener != null){
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

                    if(!NetworkUtils.isConnectedToNetwork(getActivity())){
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
        }
    }

    private void syncPatientsAndObservationsInBackgroundService() {
        Intent intent = new Intent(getActivity(), DataSyncService.class);
        intent.putExtra(SYNC_TYPE, SYNC_PATIENTS);
        intent.putExtra(CREDENTIALS, ((MuzimaFragmentActivity) getActivity()).credentials().getCredentialsArray());
        intent.putExtra(COHORT_IDS, getSelectedCohortsArray());
        ((CohortActivity)getActivity()).showProgressBar();
        getActivity().startService(intent);
    }

    private String[] getSelectedCohortsArray() {
        List<String> selectedCohorts = ((AllCohortsAdapter) listAdapter).getSelectedCohorts();
        String[] selectedCohortsUuids = new String[selectedCohorts.size()];
        return selectedCohorts.toArray(selectedCohortsUuids);
    }

    public interface OnCohortDataDownloadListener{
        public void onCohortDataDownloadComplete();
    }

    private void updateSyncText() {
        SharedPreferences pref = getActivity().getSharedPreferences(Constants.SYNC_PREF, Context.MODE_PRIVATE);
        long lastSyncedTime = pref.getLong(COHORTS_LAST_SYNCED_TIME, NOT_SYNCED_TIME);
        String lastSyncedMsg = "Not synced yet";
        if(lastSyncedTime != NOT_SYNCED_TIME){
            lastSyncedMsg = "Last synced on: " + DateUtils.getFormattedDateTime(new Date(lastSyncedTime));
        }
        syncText.setText(lastSyncedMsg);
    }

}
