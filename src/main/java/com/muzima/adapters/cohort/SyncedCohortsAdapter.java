package com.muzima.adapters.cohort;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import com.muzima.R;
import com.muzima.api.model.Cohort;
import com.muzima.controller.CohortController;

import java.util.ArrayList;
import java.util.List;

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
                syncedCohorts = cohortController.getSyncedCohort();
                Log.i(TAG, "#Synced Cohorts: " + syncedCohorts.size());
            } catch (CohortController.CohortFetchException e) {
                Log.e(TAG, "Exception occurred while fetching local synced cohorts " + e);
            }
            return syncedCohorts;
        }

        @Override
        protected void onPostExecute(List<Cohort> cohorts) {
            if(cohorts == null){
                Toast.makeText(getContext(), "Something went wrong while fetching cohorts from local repo", Toast.LENGTH_SHORT).show();
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
