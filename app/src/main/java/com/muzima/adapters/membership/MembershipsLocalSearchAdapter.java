/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.adapters.membership;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.CohortMembership;
import com.muzima.controller.CohortController;
import com.muzima.utils.Constants;

import java.util.List;

public class MembershipsLocalSearchAdapter extends ListAdapter<CohortMembership> {
    private static final String TAG = "MembershipsLocalSearchAdapter";
    public static final String SEARCH = "search";
    public static final String STATUS = "status";
    private final MembershipAdapterHelper membershipAdapterHelper;
    private CohortController cohortController;
    private final String cohortId;
    private Context context;
    private AsyncTask<String, List<CohortMembership>, List<CohortMembership>> backgroundQueryTask;
    protected BackgroundListQueryTaskListener backgroundListQueryTaskListener;

    public MembershipsLocalSearchAdapter(Context context, int textViewResourceId,
                                         CohortController cohortController,
                                         String cohortId) {
        super(context, textViewResourceId);
        this.context = context;
        this.cohortController = cohortController;
        this.cohortId = cohortId;
        this.membershipAdapterHelper = new MembershipAdapterHelper(context, textViewResourceId);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return membershipAdapterHelper.createPatientRow(getItem(position), convertView, parent, getContext());
    }

    @Override
    public void reloadData() {
        cancelBackgroundTask();
        backgroundQueryTask = new BackgroundQueryTask().execute(cohortId);
    }

    public void search(String text) {
        cancelBackgroundTask();
        backgroundQueryTask = new BackgroundQueryTask().execute(text, SEARCH);
    }

    public void filterActive(boolean status) {
        cancelBackgroundTask();
        backgroundQueryTask = new BackgroundQueryTask().execute(String.valueOf(status), STATUS);
    }

    public void setBackgroundListQueryTaskListener(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        this.backgroundListQueryTaskListener = backgroundListQueryTaskListener;
    }

    public void cancelBackgroundTask(){
        if(backgroundQueryTask != null){
            backgroundQueryTask.cancel(true);
        }
    }


    private class BackgroundQueryTask extends AsyncTask<String, List<CohortMembership>, List<CohortMembership>> {

        @Override
        protected void onPreExecute() {
            membershipAdapterHelper.onPreExecute(backgroundListQueryTaskListener);
            MembershipsLocalSearchAdapter.this.clear();
        }

        @SuppressLint("LongLogTag")
        @Override
        protected List<CohortMembership> doInBackground(String... params) {
            List<CohortMembership> memberships = null;

            if (isSearch(params) || isStatus(params)) {
                try {
                    return cohortController.searchCohortMembershipLocally(params[0], cohortId);
                } catch (CohortController.CohortMembershipLoadException e) {
                    Log.w(TAG, String.format("Exception while searching %s", params[0]), e);
                }
            }

            String cohortUuid = params[0];
            try {
                int pageSize = Constants.COHORT_MEMBERSHIP_LOAD_PAGE_SIZE;
                if (cohortUuid != null) {
                    int membershipCount = cohortController.countMemberships(cohortUuid);
                    if(membershipCount <= pageSize){
                        memberships = cohortController.getCohortMemberships(cohortUuid);
                    } else {
                        int pages = Double.valueOf(Math.ceil(membershipCount / pageSize)).intValue();
                        List<CohortMembership> temp;
                        for (int page = 1; page <= pages; page++) {
                            if(!isCancelled()) {
                                if (memberships == null) {
                                    memberships = cohortController.getCohortMemberships(cohortUuid,
                                            page, pageSize);
                                    if (memberships != null) {
                                        publishProgress(memberships);
                                    }
                                } else {
                                    temp = cohortController.getCohortMemberships(cohortUuid, page,
                                            pageSize);
                                    if (temp != null) {
                                        memberships.addAll(temp);
                                        publishProgress(temp);
                                    }
                                }
                            }
                        }
                    }

                } else {
                    int membershipCount = cohortController.countAllCohortMemberships();
                    if(membershipCount <= pageSize){
                        memberships = cohortController.getAllCohortMemberships();
                    } else {
                        int pages = Double.valueOf(Math.ceil(membershipCount / pageSize)).intValue();
                        List<CohortMembership> temp;
                        for (int page = 1; page <= pages; page++) {
                            if(!isCancelled()) {
                                if (memberships == null) {
                                    memberships = cohortController.getCohortMemberships(page, pageSize);
                                    if (memberships != null) {
                                        publishProgress(memberships);
                                    }
                                } else {
                                    temp = cohortController.getCohortMemberships(page, pageSize);
                                    if (temp != null) {
                                        memberships.addAll(temp);
                                        publishProgress(temp);
                                    }
                                }
                            } else {
                                break;
                            }
                        }
                    }
                }
            } catch (CohortController.CohortMembershipLoadException e) {
                Log.w(TAG, "Exception occured while fetching cohort memberships", e);
            }
            return memberships;
        }

        private boolean isSearch(String[] params) {
            return params.length == 2 && SEARCH.equals(params[1]);
        }

        private boolean isStatus(String[] params) {
            return params.length == 2 && STATUS.equals(params[1]);
        }

        @Override
        protected void onPostExecute(List<CohortMembership> memberships) {
            membershipAdapterHelper.onPostExecute(memberships, MembershipsLocalSearchAdapter.this,
                    backgroundListQueryTaskListener);
        }

        @Override
        protected void onProgressUpdate(List<CohortMembership>... memberships) {
            for (List<CohortMembership> membershipList : memberships) {
                membershipAdapterHelper.onProgressUpdate(membershipList,
                        MembershipsLocalSearchAdapter.this, backgroundListQueryTaskListener);
            }
        }
    }
}