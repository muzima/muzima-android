package com.muzima.view.cohort;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.cohort.AllCohortsAdapter;
import com.muzima.adapters.forms.NewFormsAdapter;
import com.muzima.controller.CohortController;
import com.muzima.listeners.DownloadListener;
import com.muzima.search.api.util.StringUtil;
import com.muzima.tasks.DownloadMuzimaTask;
import com.muzima.tasks.cohort.DownloadCohortDataTask;
import com.muzima.tasks.forms.DownloadFormTemplateTask;
import com.muzima.utils.NetworkUtils;
import com.muzima.view.forms.NewFormsListFragment;

import java.util.List;

import static android.os.AsyncTask.Status.PENDING;
import static android.os.AsyncTask.Status.RUNNING;

public class AllCohortsListFragment extends CohortListFragment implements DownloadListener<Integer[]>{
    private static final String TAG = "AllCohortsListFragment";
    private ActionMode actionMode;
    private boolean actionModeActive = false;
    private DownloadCohortDataTask cohortDataDownloadTask;
    private OnCohortDataDownloadListener cohortDataDownloadListener;

    public static AllCohortsListFragment newInstance(CohortController cohortController) {
        AllCohortsListFragment f = new AllCohortsListFragment();
        f.cohortController = cohortController;
        f.setRetainInstance(true);
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
    public void downloadTaskComplete(Integer[] result) {
        if(cohortDataDownloadListener != null){
            cohortDataDownloadListener.onCohortDataDownloadComplete(result);
        }
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

    @Override
    public void formDownloadComplete(Integer[] status) {
        Integer downloadStatus = status[0];
        String msg = "Download Complete with status " + downloadStatus;
        Log.i(TAG, msg);
        if (downloadStatus == DownloadMuzimaTask.SUCCESS) {
            msg = "Downloaded: " + status[1] + " cohorts";
            if (listAdapter != null) {
                listAdapter.reloadData();
            }
        } else if (downloadStatus == DownloadMuzimaTask.DOWNLOAD_ERROR) {
            msg = "An error occurred while downloading cohorts";
        } else if (downloadStatus == DownloadMuzimaTask.AUTHENTICATION_ERROR) {
            msg = "Authentication error occurred while downloading cohorts";
        } else if (downloadStatus == DownloadMuzimaTask.DELETE_ERROR) {
            msg = "An error occurred while deleting existing cohorts";
        } else if (downloadStatus == DownloadMuzimaTask.SAVE_ERROR) {
            msg = "An error occurred while saving the downloaded cohorts";
        } else if (downloadStatus == DownloadMuzimaTask.CANCELLED) {
            msg = "Cohort download task has been cancelled";
        } else if (downloadStatus == DownloadMuzimaTask.CONNECTION_ERROR) {
            msg = "Connection error occurred while downloading cohorts";
        } else if (downloadStatus == DownloadMuzimaTask.PARSING_ERROR) {
            msg = "Parse exception has been thrown while fetching data";
        }else if (downloadStatus == DownloadMuzimaTask.REPLACE_ERROR) {
            msg = "An error occurred while replace existing cohort data";
        }
        Toast.makeText(getActivity().getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    public interface OnCohortDataDownloadListener{
        public void onCohortDataDownloadComplete(Integer[] result);
    }

}
