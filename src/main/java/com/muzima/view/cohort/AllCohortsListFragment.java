package com.muzima.view.cohort;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.cohort.AllCohortsAdapter;
import com.muzima.controller.CohortController;
import com.muzima.listeners.DownloadListener;
import com.muzima.search.api.util.StringUtil;
import com.muzima.tasks.DownloadMuzimaTask;
import com.muzima.tasks.cohort.DownloadCohortDataTask;
import com.muzima.utils.Constants;
import com.muzima.utils.DateUtils;
import com.muzima.utils.NetworkUtils;

import java.util.Date;
import java.util.List;

import static android.os.AsyncTask.Status.PENDING;
import static android.os.AsyncTask.Status.RUNNING;

public class AllCohortsListFragment extends CohortListFragment implements DownloadListener<Integer[]>{
    private static final String TAG = "AllCohortsListFragment";
    public static final String COHORTS_LAST_SYNCED_TIME = "cohortsSyncedTime";
    public static final long NOT_SYNCED_TIME = -1;

    private ActionMode actionMode;
    private boolean actionModeActive = false;
    private DownloadCohortDataTask cohortDataDownloadTask;
    private OnCohortDataDownloadListener cohortDataDownloadListener;
    private TextView syncText;

    public static AllCohortsListFragment newInstance(CohortController cohortController) {
        AllCohortsListFragment f = new AllCohortsListFragment();
        f.cohortController = cohortController;
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(listAdapter == null){
            listAdapter = new AllCohortsAdapter(getActivity(), R.layout.item_cohorts_list, cohortController);
        }
        noDataMsg = getActivity().getResources().getString(R.string.no_cohorts_available);
        noDataTip = getActivity().getResources().getString(R.string.no_cohorts_available_tip);
        return super.onCreateView(inflater, container, savedInstanceState);
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
        if (cohortDataDownloadTask != null) {
            cohortDataDownloadTask.cancel(false);
        }
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

    @Override
    public void synchronizationComplete(Integer[] status) {
        ((CohortActivity)getActivity()).hideProgressbar();
       if(status[0] == DownloadMuzimaTask.SUCCESS){
            updateSyncText();
        }
        super.synchronizationComplete(status);
    }

    @Override
    public void downloadTaskComplete(Integer[] result) {
        if(cohortDataDownloadListener != null){
            cohortDataDownloadListener.onCohortDataDownloadComplete(result);
        }
    }

    @Override
    public void downloadTaskStart() {
        
    }

    public void setCohortDataDownloadListener(OnCohortDataDownloadListener cohortDataDownloadListener) {
        this.cohortDataDownloadListener = cohortDataDownloadListener;
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
                    if(!NetworkUtils.isConnectedToNetwork(getActivity())){
                        Toast.makeText(getActivity(), "No connection found, please connect your device and try again", Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    if (cohortDataDownloadTask != null &&
                            (cohortDataDownloadTask.getStatus() == PENDING || cohortDataDownloadTask.getStatus() == RUNNING)) {
                        Toast.makeText(getActivity(), "Already fetching cohort data, ignored the request", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
                    cohortDataDownloadTask = new DownloadCohortDataTask((MuzimaApplication) getActivity().getApplication());
                    cohortDataDownloadTask.addDownloadListener(AllCohortsListFragment.this);
                    String usernameKey = getResources().getString(R.string.preference_username);
                    String passwordKey = getResources().getString(R.string.preference_password);
                    String serverKey = getResources().getString(R.string.preference_server);
                    String[] credentials = new String[]{settings.getString(usernameKey, StringUtil.EMPTY),
                            settings.getString(passwordKey, StringUtil.EMPTY),
                            settings.getString(serverKey, StringUtil.EMPTY)};
                    ((CohortActivity)getActivity()).showProgressBar();
                    cohortDataDownloadTask.execute(credentials, getSelectedCohortsArray());

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

    private String[] getSelectedCohortsArray() {
        List<String> selectedCohorts = ((AllCohortsAdapter) listAdapter).getSelectedCohorts();
        String[] selectedCohortsUuids = new String[selectedCohorts.size()];
        return selectedCohorts.toArray(selectedCohortsUuids);
    }

    public interface OnCohortDataDownloadListener{
        public void onCohortDataDownloadComplete(Integer[] result);
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
