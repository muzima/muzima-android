package com.muzima.adapters.observations;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import com.muzima.controller.ObservationController;
import com.muzima.model.observation.EncounterWithObservations;
import com.muzima.model.observation.Encounters;

public class ObservationsByEncounterBackgroundTask extends AsyncTask<Void, Void, Encounters> {

    private EncounterAction encounterAction;
    private ObservationsByEncounterAdapter observationsByEncounterAdapter;

    public ObservationsByEncounterBackgroundTask(ObservationsByEncounterAdapter observationsByEncounterAdapter, EncounterAction encounterAction) {
        this.observationsByEncounterAdapter = observationsByEncounterAdapter;
        this.encounterAction = encounterAction;
    }

    @Override
    protected Encounters doInBackground(Void... params) {
        Encounters encounters = null;
        try {
            encounters = encounterAction.get();
            encounters.sortByDate();
        } catch (ObservationController.LoadObservationException e) {
            Log.w("Observations", String.format("Exception while loading observations for %s.", encounterAction), e);
        }
        return encounters;
    }

    @Override
    protected void onPostExecute(Encounters encountersWithObservations) {
        if (encountersWithObservations == null) {
            Toast.makeText(observationsByEncounterAdapter.getContext(), "Something went wrong while fetching observations from local repo", Toast.LENGTH_SHORT).show();
            return;
        }

        for (EncounterWithObservations encountersWithObservation : encountersWithObservations) {
            observationsByEncounterAdapter.add(encountersWithObservation);
        }
        observationsByEncounterAdapter.notifyDataSetChanged();
    }
}
