package com.muzima.adapters.observations;

import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.muzima.R;
import com.muzima.api.model.Observation;
import com.muzima.controller.ConceptController;
import com.muzima.controller.ObservationController;
import com.muzima.model.observation.ConceptWithObservations;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ObservationsByConceptAdapter extends ObservationsAdapter<ConceptWithObservations> {
    private static final String TAG = "ObservationsByConceptAdapter";
    private static final String SEARCH = "search";

    public ObservationsByConceptAdapter(FragmentActivity activity, int itemCohortsList,
                                        ConceptController conceptController, ObservationController observationController) {
        super(activity, itemCohortsList, conceptController, observationController);
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask().execute(patientUuid);
    }

    public void search(String term) {
        new BackgroundQueryTask().execute(term, SEARCH);
    }

    public class BackgroundQueryTask extends AsyncTask<String, Void, List<ConceptWithObservations>> {

        @Override
        protected List<ConceptWithObservations> doInBackground(String... params) {
            List<ConceptWithObservations> conceptWithObservations = null;
            Log.d(TAG, "searching observations for query string: " + params[0]);
            try {
                conceptWithObservations = observationController.getConceptWithObservations(patientUuid);
                Collections.sort(conceptWithObservations, new Comparator<ConceptWithObservations>() {
                    @Override
                    public int compare(ConceptWithObservations lhs, ConceptWithObservations rhs) {
                        return -(lhs.getObservations().get(0).getObservationDatetime()
                                .compareTo(rhs.getObservations().get(0).getObservationDatetime()));
                    }
                });

            } catch (ObservationController.LoadObservationException e) {
                Log.w(TAG, "Exception occurred while searching observations for " + params[0] + " search string. " + e);
            }
            return conceptWithObservations;
        }

        @Override
        protected void onPostExecute(List<ConceptWithObservations> conceptsWithObservations) {
            if (conceptsWithObservations == null) {
                Toast.makeText(getContext(), "Something went wrong while fetching observations from local repo", Toast.LENGTH_SHORT).show();
                return;
            }

            ObservationsByConceptAdapter.this.clear();
            addAll(conceptsWithObservations);
            notifyDataSetChanged();
        }
    }
}
