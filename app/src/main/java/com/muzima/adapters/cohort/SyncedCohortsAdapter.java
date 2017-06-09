/*
 * Copyright (c) 2014 - 2017. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */
package com.muzima.adapters.cohort;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import com.muzima.R;
import com.muzima.api.model.Cohort;
import com.muzima.controller.CohortController;

import java.util.List;

/**
 * Responsible to populate synced cohorts fetched from DB in the SyncedCohortsListFragment page.
 */
public class SyncedCohortsAdapter extends CohortsAdapter{
    private static final String TAG = "SyncedCohortsAdapter";

    public SyncedCohortsAdapter(Context context, int textViewResourceId, CohortController cohortController) {
        super(context, textViewResourceId, cohortController);
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask().execute();
    }

    public class BackgroundQueryTask extends AsyncTask<Void, Void, List<Cohort>> {

        @Override
        protected void onPreExecute() {
            if(backgroundListQueryTaskListener != null){
                backgroundListQueryTaskListener.onQueryTaskStarted();
            }
        }

        @Override
        protected List<Cohort> doInBackground(Void... voids) {
            List<Cohort> syncedCohorts = null;
            try {
                syncedCohorts = cohortController.getSyncedCohorts();
                Log.i(TAG, "#Synced Cohorts: " + syncedCohorts.size());
            } catch (CohortController.CohortFetchException e) {
                Log.e(TAG, "Exception occurred while fetching local synced cohorts ", e);
            }
            return syncedCohorts;
        }

        @Override
        protected void onPostExecute(List<Cohort> cohorts) {
            if(cohorts == null){
                Toast.makeText(getContext(), getContext().getString(R.string.error_cohort_fetch), Toast.LENGTH_SHORT).show();
                return;
            }

            SyncedCohortsAdapter.this.clear();
            for (Cohort cohort : cohorts) {
                add(cohort);
            }
            notifyDataSetChanged();

            if(backgroundListQueryTaskListener != null){
                backgroundListQueryTaskListener.onQueryTaskFinish();
            }
        }
    }
}
