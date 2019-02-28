/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.adapters.observations;

import android.os.AsyncTask;
import android.util.Log;

import com.muzima.api.model.Encounter;
import com.muzima.controller.ObservationController;
import com.muzima.model.observation.EncounterWithObservations;
import com.muzima.model.observation.Encounters;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class ObservationsByEncounterBackgroundTask extends AsyncTask<Void, Encounters, Encounters> {

    private final EncounterAction encounterAction;
    private final ObservationsByEncounterAdapter observationsByEncounterAdapter;
    private Boolean isSHREncounter = false;

    public ObservationsByEncounterBackgroundTask(ObservationsByEncounterAdapter observationsByEncounterAdapter, EncounterAction encounterAction,Boolean isSHREncounter) {
        this.observationsByEncounterAdapter = observationsByEncounterAdapter;
        this.encounterAction = encounterAction;
        this.isSHREncounter = isSHREncounter;
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
        if (isSHREncounter){
            encountersWithObservations = getSHREncounterWithObservations();
        }else {
            encountersWithObservations = getNonSHREncounterWithObservations();
        }
        return encountersWithObservations;
    }

    private Encounters getSHREncounterWithObservations() {
        Encounters encountersWithObservations = null;
        Encounters temp = null;
        try {
            List<Encounter> encounters = encounterAction.getEncounters();

            Collections.sort(encounters, encountersDateTimeComparator);
            for(Encounter encounter : encounters) {
                if(!isCancelled() && encounter.getEncounterType().getName().contains("encounterType")) {
                    temp = encounterAction.get(encounter);
                    if (temp != null) {
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

    private Encounters getNonSHREncounterWithObservations() {
        Encounters encountersWithObservations = null;
        Encounters temp = null;
        try {
            List<Encounter> encounters = encounterAction.getEncounters();

            Collections.sort(encounters, encountersDateTimeComparator);
            for(Encounter encounter : encounters) {
                if(!isCancelled() ) {
                    temp = encounterAction.get(encounter);
                    if (temp != null) {
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

    private final Comparator<Encounter> encountersDateTimeComparator = new Comparator<Encounter>() {
        @Override
        public int compare(Encounter lhs, Encounter rhs) {
            if (lhs.getEncounterDatetime()==null)
                return -1;
            if (rhs.getEncounterDatetime()==null)
                return 1;
            return -(lhs.getEncounterDatetime()
                    .compareTo(rhs.getEncounterDatetime()));
        }
    };

    @Override
    protected void onPostExecute(Encounters encountersWithObservations) {
        if (encountersWithObservations == null) {
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
