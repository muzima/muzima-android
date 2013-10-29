package com.muzima.adapters.observations;

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;
import com.muzima.controller.ObservationController;
import com.muzima.model.observation.Concepts;

public class ObservationsByConceptBackgroundTask extends AsyncTask<Void, Void, Concepts> {

    private ConceptAction conceptAction;
    private ObservationsByConceptAdapter observationsByConceptAdapter;

    public ObservationsByConceptBackgroundTask(ObservationsByConceptAdapter observationsByConceptAdapter, ConceptAction conceptAction) {
        this.observationsByConceptAdapter = observationsByConceptAdapter;
        this.conceptAction = conceptAction;
    }

    @Override
    protected Concepts doInBackground(Void... params) {
        Concepts concepts = null;
        try {
            concepts = conceptAction.get();
            concepts.sortByDate();
        } catch (ObservationController.LoadObservationException e) {
            Log.w("Observations", String.format("Exception while loading observations for %s. Exception stack: %s", conceptAction, e));
        }
        return concepts;
    }

    @Override
    protected void onPostExecute(Concepts conceptsWithObservations) {
        if (conceptsWithObservations == null) {
            Toast.makeText(observationsByConceptAdapter.getContext(), "Something went wrong while fetching observations from local repo", Toast.LENGTH_SHORT).show();
            return;
        }

        observationsByConceptAdapter.clear();
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            observationsByConceptAdapter.addAll(conceptsWithObservations);
        } else {
//            TODO for FROYO
        }
        observationsByConceptAdapter.notifyDataSetChanged();
    }
}
