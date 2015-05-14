/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

/**
 * Copyright 2012 Muzima Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.muzima.adapters.cohort;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
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
