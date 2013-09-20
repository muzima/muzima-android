package com.muzima.adapters.observations;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import com.muzima.controller.ObservationController;
import com.muzima.model.observation.Encounters;

public class ObservationsByEncounterBackgroundTask extends AsyncTask<Void, Void, Encounters> {

    private EncounterAction encounterAction;
    private ObservationsByEncounterAdapter observationsByConceptAdapter;

    public ObservationsByEncounterBackgroundTask(ObservationsByEncounterAdapter observationsByEncounterAdapter, EncounterAction encounterAction) {
        this.observationsByConceptAdapter = observationsByEncounterAdapter;
        this.encounterAction = encounterAction;
    }

    @Override
    protected Encounters doInBackground(Void... params) {
        Encounters encounters = null;
        try {
            encounters = encounterAction.get();
            encounters.sortByDate();
        } catch (ObservationController.LoadObservationException e) {
            Log.w("Observations", String.format("Exception while loading observations for %s. Exception stack: %s", encounterAction, e));
        }
        return encounters;
    }

    @Override
    protected void onPostExecute(Encounters encountersWithObservations) {
        if (encountersWithObservations == null) {
            Toast.makeText(observationsByConceptAdapter.getContext(), "Something went wrong while fetching observations from local repo", Toast.LENGTH_SHORT).show();
            return;
        }

        observationsByConceptAdapter.clear();
        observationsByConceptAdapter.addAll(encountersWithObservations);
        observationsByConceptAdapter.notifyDataSetChanged();
    }
}
