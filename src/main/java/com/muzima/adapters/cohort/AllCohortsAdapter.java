package com.muzima.adapters.cohort;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.muzima.R;
import com.muzima.api.model.Cohort;
import com.muzima.api.model.Form;
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

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        highlightIfSelected(view, getItem(position));
        return view;
    }

    private void highlightIfSelected(View convertView, Cohort cohort) {
        if (selectedCohortsUuid.contains(cohort.getUuid())) {
            convertView.setBackgroundColor(getContext().getResources().getColor(R.color.listitem_state_pressed));
        } else {
            convertView.setBackgroundColor(Color.WHITE);
        }
    }

    public void onListItemClick(int position) {
        Cohort cohort = getItem(position);
        if (selectedCohortsUuid.contains(cohort.getUuid())) {
            selectedCohortsUuid.remove(cohort.getUuid());
        } else {
            selectedCohortsUuid.add(cohort.getUuid());
        }
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
