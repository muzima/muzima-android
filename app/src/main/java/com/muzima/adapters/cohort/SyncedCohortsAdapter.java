/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
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
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.muzima.R;
import com.muzima.api.model.Cohort;
import com.muzima.controller.CohortController;

import java.util.List;

/**
 * Responsible to populate synced cohorts fetched from DB in the SyncedCohortsListFragment page.
 */
public class SyncedCohortsAdapter extends CohortsAdapter{

    public SyncedCohortsAdapter(Context context, int textViewResourceId, CohortController cohortController) {
        super(context, textViewResourceId, cohortController);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        convertView = super.getView(position,convertView,parent);
        ViewHolder holder = (ViewHolder) convertView.getTag();

        int cohortSize = 0;
        Cohort cohort = getItem(position);
        try {
            cohortSize = cohortController.countCohortMembers(cohort);
        } catch (CohortController.CohortFetchException e) {
            Log.e(getClass().getSimpleName(), "Could not count cohort members",e);
        }
        holder.setTextToName(cohort.getName() + "("+cohortSize+ ")");

        if(cohort.isUpdateAvailable()){
            holder.setPendingUpdateTextColor();
        } else {
            holder.setDefaultTextColor();
        }
        return convertView;
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask().execute();
    }

    class BackgroundQueryTask extends AsyncTask<Void, Void, List<Cohort>> {

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
                Log.i(getClass().getSimpleName(), "#Synced Cohorts: " + syncedCohorts.size());
            } catch (CohortController.CohortFetchException e) {
                Log.e(getClass().getSimpleName(), "Exception occurred while fetching local synced cohorts ", e);
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
