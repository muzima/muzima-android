package com.muzima.adapters.cohort;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.muzima.api.model.Cohort;
import com.muzima.controller.CohortController;

import java.util.ArrayList;
import java.util.List;

public class AllCohortsAdapter extends CohortsAdapter{
    private static final String TAG = "AllCohortsAdapter";
    private List<String> selectedCohortsUuid;

    public AllCohortsAdapter(Context context, int textViewResourceId, CohortController cohortController) {
        super(context, textViewResourceId, cohortController);
        selectedCohortsUuid = new ArrayList<String>();
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask().execute();
    }

    public List<String> getSelectedCohorts() {
        return selectedCohortsUuid;
    }

    public void clearSelectedCohorts() {
        selectedCohortsUuid.clear();
        notifyDataSetChanged();
    }

    public class BackgroundQueryTask extends AsyncTask<Void, Void, List<Cohort>> {

        @Override
        protected List<Cohort> doInBackground(Void... voids) {
            List<Cohort> allCohorts = null;
            try {
                allCohorts = cohortController.getAllCohorts();
                Log.i(TAG, "#Cohorts: " + allCohorts.size());
            } catch (CohortController.CohortFetchException e) {
                Log.w(TAG, "Exception occurred while fetching local cohorts " + e);
            }
            return allCohorts;
        }

        @Override
        protected void onPostExecute(List<Cohort> cohorts) {
            AllCohortsAdapter.this.clear();
            for (Cohort cohort : cohorts) {
                add(cohort);
            }
            notifyDataSetChanged();
        }
    }
}
