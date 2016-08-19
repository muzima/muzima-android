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
import com.muzima.api.model.Encounter;
import com.muzima.controller.ObservationController;
import com.muzima.model.observation.EncounterWithObservations;
import com.muzima.model.observation.Encounters;

import java.util.List;

public class ObservationsByEncounterBackgroundTask extends AsyncTask<Void, Encounters, Encounters> {

    private EncounterAction encounterAction;
    private ObservationsByEncounterAdapter observationsByEncounterAdapter;

    public ObservationsByEncounterBackgroundTask(ObservationsByEncounterAdapter observationsByEncounterAdapter, EncounterAction encounterAction) {
        this.observationsByEncounterAdapter = observationsByEncounterAdapter;
        this.encounterAction = encounterAction;
    }

    @Override
    protected void onPreExecute() {
        if (observationsByEncounterAdapter.getBackgroundListQueryTaskListener() != null) {
            observationsByEncounterAdapter.getBackgroundListQueryTaskListener().onQueryTaskStarted();
        }
    }

    @Override
    protected Encounters doInBackground(Void... params) {
        Encounters encountersWithObservations = null;
        Encounters temp = null;
        try {
            List<Encounter> encounters = encounterAction.getEncounters();
            for(Encounter encounter : encounters) {
                if(!isCancelled()) {
                    temp = encounterAction.get(encounter);
                    if (temp != null) {
                        System.out.println(temp.size());
                        temp.sortByDate();
                        if (encountersWithObservations == null) {
                            encountersWithObservations = temp;
                        } else {
                            encountersWithObservations.addAll(temp);
                        }
                        publishProgress(temp);
                    }
                } else {
                    break;
                }
            }
        } catch (ObservationController.LoadObservationException e) {
            Log.w("Observations", String.format("Exception while loading observations for %s.", encounterAction), e);
        }
        return encountersWithObservations;
    }

    @Override
    protected void onPostExecute(Encounters encountersWithObservations) {
        if (encountersWithObservations == null) {
            Toast.makeText(observationsByEncounterAdapter.getContext(), "Something went wrong while fetching observations from local repo", Toast.LENGTH_SHORT).show();
            return;
        }
        if (observationsByEncounterAdapter.getBackgroundListQueryTaskListener() != null) {
            observationsByEncounterAdapter.getBackgroundListQueryTaskListener().onQueryTaskFinish();
        }
    }

    @Override
    protected void onProgressUpdate(Encounters... encountersWithObservations) {
        if (encountersWithObservations == null) {
            return;
        }

        for (Encounters encounters : encountersWithObservations) {
            for (EncounterWithObservations encountersWithObservation : encounters) {
                observationsByEncounterAdapter.add(encountersWithObservation);
            }
        }
        observationsByEncounterAdapter.notifyDataSetChanged();
    }
}
