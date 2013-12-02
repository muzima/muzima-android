package com.muzima.adapters.cohort;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.muzima.MuzimaApplication;
import com.muzima.api.model.Cohort;
import com.muzima.controller.CohortController;
import com.muzima.service.MuzimaSyncService;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible to populate all cohorts fetched from DB in the AllCohortsListFragment page.
 */
public class AllCohortsAdapter extends CohortsAdapter {
    private static final String TAG = "AllCohortsAdapter";
    private final MuzimaSyncService muzimaSyncService;
    private List<String> selectedCohortsUuid;

    public AllCohortsAdapter(Context context, int textViewResourceId, CohortController cohortController) {
        super(context, textViewResourceId, cohortController);
        selectedCohortsUuid = new ArrayList<String>();
        muzimaSyncService = ((MuzimaApplication) (getContext().getApplicationContext())).getMuzimaSyncService();
    }

    @Override
    public void reloadData() {
        new LoadBackgroundQueryTask().execute();
    }

    public void downloadCohortAndReload() {
        new DownloadBackgroundQueryTask().execute();
    }

    public List<String> getSelectedCohorts() {
        return selectedCohortsUuid;
    }

    public String[] getSelectedCohortsArray() {
        return getSelectedCohorts().toArray(new String[getSelectedCohorts().size()]);
    }

    public void clearSelectedCohorts() {
        selectedCohortsUuid.clear();
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        ViewHolder holder = (ViewHolder) view.getTag();
        Cohort cohort = getItem(position);
        if (cohortController.isDownloaded(cohort)) {
            holder.displayDownloadImage();
        }
        return view;
    }

    public void onListItemClick(int position, boolean selected) {
        Cohort cohort = getItem(position);
        if (selected && !selectedCohortsUuid.contains(cohort.getUuid())) {
            selectedCohortsUuid.add(cohort.getUuid());
        } else if (!selected && selectedCohortsUuid.contains(cohort.getUuid())) {
            selectedCohortsUuid.remove(cohort.getUuid());
        }
        notifyDataSetChanged();
    }

    public int numberOfCohorts() {
        return getSelectedCohorts().size();
    }

    /**
     * Responsible to define contract to CohortBackgroundQueryTasks.
     */
    public abstract class CohortBackgroundQueryTask extends AsyncTask<Void, Void, List<Cohort>> {
        @Override
        protected void onPreExecute() {
            if (backgroundListQueryTaskListener != null) {
                backgroundListQueryTaskListener.onQueryTaskStarted();
            }
        }

        @Override
        protected void onPostExecute(List<Cohort> cohorts) {
            if (cohorts == null) {
                Toast.makeText(getContext(), "Something went wrong while fetching cohorts from local repo", Toast.LENGTH_SHORT).show();
                return;
            }
            //Removes the current items from the list.
            AllCohortsAdapter.this.clear();
            //Adds recently fetched items to the list.
            addAll(cohorts);
            //Send a data change request to the list, so the page can be reloaded.
            notifyDataSetChanged();

            if (backgroundListQueryTaskListener != null) {
                backgroundListQueryTaskListener.onQueryTaskFinish();
            }
        }

        protected abstract List<Cohort> doInBackground(Void... voids);
    }

    /**
     * Responsible to load cohorts from database. Runs in Background.
     */
    public class LoadBackgroundQueryTask extends CohortBackgroundQueryTask {

        @Override
        protected List<Cohort> doInBackground(Void... voids) {
            List<Cohort> allCohorts = null;
            try {
                Log.i(TAG, "Fetching Cohorts from Database...");
                allCohorts = cohortController.getAllCohorts();
                Log.d(TAG, "#Retrieved " + allCohorts.size() + " Cohorts from Database.");
            } catch (CohortController.CohortFetchException e) {
                Log.w(TAG, "Exception occurred while fetching local cohorts " + e);
            }
            return allCohorts;
        }
    }

    /**
     * Responsible to download Cohorts from server and to reload the contents from DB. Runs in BackGround.
     */
    public class DownloadBackgroundQueryTask extends CohortBackgroundQueryTask {
        @Override
        protected List<Cohort> doInBackground(Void... voids) {
            List<Cohort> allCohorts = null;
            try {
                muzimaSyncService.downloadCohorts();
                allCohorts = cohortController.getAllCohorts();
                Log.i(TAG, "#Cohorts: " + allCohorts.size());
            } catch (CohortController.CohortFetchException e) {
                Log.w(TAG, "Exception occurred while fetching the downloaded cohorts" + e);
            }
            return allCohorts;
        }
    }
}
