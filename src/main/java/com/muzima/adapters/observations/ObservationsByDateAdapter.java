package com.muzima.adapters.observations;

import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;
import com.muzima.adapters.ListAdapter;
import com.muzima.adapters.cohort.AllCohortsAdapter;
import com.muzima.api.model.Cohort;
import com.muzima.api.model.Observation;
import com.muzima.controller.CohortController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.PatientController;
import com.muzima.view.patients.PatientSummaryActivity;

import java.util.List;

public class ObservationsByDateAdapter extends ObservationsAdapter {
    private static final String TAG = "ObservationsByDateAdapter";

    public ObservationsByDateAdapter(FragmentActivity activity, int itemCohortsList, ObservationController observationController) {
        super(activity, itemCohortsList, observationController);
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask().execute(patientUuid);
    }

    public class BackgroundQueryTask extends AsyncTask<String, Void, List<Observation>> {

        @Override
        protected List<Observation> doInBackground(String... patientUuid) {
            List<Observation> observations = null;
            try {
                observations = observationController.getObservationsByDate(patientUuid[0]);
                Log.i(TAG, "#Observations: " + observations.size());
            } catch (ObservationController.LoadObservationException e) {
                Log.e(TAG, "Exception occurred while fetching observations " + e);
            }
            return observations;
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
