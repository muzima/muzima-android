package com.muzima.adapters.observations;

import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;
import com.muzima.api.model.Observation;
import com.muzima.controller.ObservationController;

import java.util.List;

public class ObservationsByDateAdapter extends ObservationsAdapter {
    private static final String TAG = "ObservationsByDateAdapter";
    private static final String SEARCH = "search";

    public ObservationsByDateAdapter(FragmentActivity activity, int itemCohortsList, ObservationController observationController) {
        super(activity, itemCohortsList, observationController);
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask().execute(patientUuid);
    }

    public void search(String term) {
        new BackgroundQueryTask().execute(term, SEARCH);
    }

    public class BackgroundQueryTask extends AsyncTask<String, Void, List<Observation>> {

        @Override
        protected List<Observation> doInBackground(String... params) {
            if(isSearch(params)){
                Log.d(TAG, "searching observations for query string: " + params[0]);
                try {
                    return observationController.searchObservations(params[0], patientUuid);
                }  catch (ObservationController.LoadObservationException e) {
                    Log.w(TAG, "Exception occurred while searching observations for " + params[0] + " search string. " + e);
                }
            }

            List<Observation> observations = null;

            try {
                observations = observationController.getObservationsByDate(params[0]);
                Log.i(TAG, "#Observations: " + observations.size());
            } catch (ObservationController.LoadObservationException e) {
                Log.e(TAG, "Exception occurred while fetching observations " + e);
            }
            return observations;
        }

        private boolean isSearch(String[] params) {
            return params.length == 2 && SEARCH.equals(params[1]);
        }

        @Override
        protected void onPostExecute(List<Observation> observations) {
            if (observations == null) {
                Toast.makeText(getContext(), "Something went wrong while fetching observations from local repo", Toast.LENGTH_SHORT).show();
                return;
            }

            ObservationsByDateAdapter.this.clear();
            addAll(observations);
            notifyDataSetChanged();
        }
    }
}
