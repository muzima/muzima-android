/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.adapters.observations;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import com.muzima.controller.ObservationController;
import com.muzima.model.observation.ConceptWithObservations;
import com.muzima.model.observation.Concepts;

public class ObservationsByConceptBackgroundTask extends AsyncTask<Void, Void, Concepts> {

    private ConceptAction conceptAction;
    private ObservationsByConceptAdapter observationsByConceptAdapter;

    public ObservationsByConceptBackgroundTask(ObservationsByConceptAdapter observationsByConceptAdapter, ConceptAction conceptAction) {
        this.observationsByConceptAdapter = observationsByConceptAdapter;
        this.conceptAction = conceptAction;
    }

    @Override
    protected void onPreExecute() {
        if (observationsByConceptAdapter.getBackgroundListQueryTaskListener() != null) {
            observationsByConceptAdapter.getBackgroundListQueryTaskListener().onQueryTaskStarted();
        }
    }
    @Override
    protected Concepts doInBackground(Void... params) {
        Concepts concepts = null;
        try {
            concepts = conceptAction.get();
            if(concepts != null){
                concepts.sortByDate();
            }
        } catch (ObservationController.LoadObservationException e) {
            Log.w("Observations", String.format("Exception while loading observations for %s.", conceptAction), e);
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

        for (ConceptWithObservations conceptsWithObservation : conceptsWithObservations) {
            observationsByConceptAdapter.add(conceptsWithObservation);
        }
        observationsByConceptAdapter.notifyDataSetChanged();

        if (observationsByConceptAdapter.getBackgroundListQueryTaskListener() != null) {
            observationsByConceptAdapter.getBackgroundListQueryTaskListener().onQueryTaskFinish();
        }
    }
}
