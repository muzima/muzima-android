/*
 * Copyright (c) 2014 - 2017. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.adapters.observations;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import com.muzima.R;
import com.muzima.api.model.Concept;
import com.muzima.controller.ObservationController;
import com.muzima.model.observation.ConceptWithObservations;
import com.muzima.model.observation.Concepts;
import com.muzima.utils.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * AsyncTask Class that ochestrate a background non ui thread that loads
 * Observation data by concept from local storage.
 *
 * @see AsyncTask
 * @see com.muzima.api.model.Observation
 * @see Concept
 */
public class ObservationsByConceptBackgroundTask extends AsyncTask<Void, Concepts, Concepts> {

    private ConceptAction conceptAction;
    private ObservationsByConceptAdapter observationsByConceptAdapter;
    private Boolean isShrData = false;
    private List<String> shrConcepts = new ArrayList<>();

    public ObservationsByConceptBackgroundTask(ObservationsByConceptAdapter observationsByConceptAdapter,
                                               ConceptAction conceptAction, boolean isShrData) {
        this.observationsByConceptAdapter = observationsByConceptAdapter;
        this.conceptAction = conceptAction;
        this.isShrData = isShrData;

        if (isShrData){
            loadComposedShrConceptId();
        }
    }

    @Override
    protected void onPreExecute() {
        if (observationsByConceptAdapter.getBackgroundListQueryTaskListener() != null) {
            observationsByConceptAdapter.getBackgroundListQueryTaskListener().onQueryTaskStarted();
        }
    }

    @Override
    protected Concepts doInBackground(Void... params) {
        Concepts conceptsWithObservations = null;
        if (isShrData){
            conceptsWithObservations = getShrConceptWithObservations();
        }else {
            conceptsWithObservations = getNonShrConceptWithObservations();
        }
        return conceptsWithObservations;
    }



    @Override
    protected void onPostExecute(Concepts conceptsWithObservations) {
        if (conceptsWithObservations == null) {
            Toast.makeText(observationsByConceptAdapter.getContext(), observationsByConceptAdapter.getContext().getString(R.string.error_observation_fetch), Toast.LENGTH_SHORT).show();
            return;
        }

        if (observationsByConceptAdapter.getBackgroundListQueryTaskListener() != null) {
            observationsByConceptAdapter.getBackgroundListQueryTaskListener().onQueryTaskFinish();
        }
    }

    @Override
    protected void onProgressUpdate(Concepts... conceptsWithObservations) {
        if (conceptsWithObservations == null) {
            return;
        }

        for (Concepts concepts : conceptsWithObservations) {
            for (ConceptWithObservations conceptsWithObservation : concepts) {
                observationsByConceptAdapter.add(conceptsWithObservation);
            }
        }
        observationsByConceptAdapter.notifyDataSetChanged();
    }

    private void loadComposedShrConceptId() {
        List<String> conceptIds = new ArrayList<>();

        conceptIds.add(Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_RESULT.name);
        conceptIds.add(Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_TYPE.name);
        conceptIds.add(Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_FACILITY.name);
        conceptIds.add(Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.name);
        conceptIds.add(Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.name);


        shrConcepts = conceptIds;
    }

    private Concepts getNonShrConceptWithObservations() {
        Concepts conceptsWithObservations = null;
        Concepts temp = null;
        try {
            List<Concept> concepts = conceptAction.getConcepts();
            for (Concept concept : concepts) {
                if (!isCancelled()) {
                    temp = conceptAction.get(concept);
                    if (temp != null) {
                        temp.sortByDate();
                        if (conceptsWithObservations == null) {
                            conceptsWithObservations = temp;
                        } else {
                            conceptsWithObservations.addAll(temp);
                        }
                        publishProgress(temp);
                    }
                } else {
                    break;
                }
            }
        } catch (ObservationController.LoadObservationException e) {
            Log.w("Observations", String.format("Exception while loading observations for %s.", conceptAction), e);
        }
        return conceptsWithObservations;
    }

    private Concepts getShrConceptWithObservations() {
        Concepts conceptsWithObservations = null;
        Concepts temp = null;
        try {
            List<Concept> concepts = conceptAction.getConcepts();
            for (Concept concept : concepts) {
                if (!isCancelled() && shrConcepts.contains(concept.getName())) {
                    temp = conceptAction.get(concept);
                    if (temp != null) {
                        temp.sortByDate();
                        if (conceptsWithObservations == null) {
                            conceptsWithObservations = temp;
                        } else {
                            conceptsWithObservations.addAll(temp);
                        }
                        publishProgress(temp);
                    }
                } else {
                    //no fallback required here any longer.
                }
            }
        } catch (ObservationController.LoadObservationException e) {
            Log.w("Observations", String.format("Exception while loading observations for %s.", conceptAction), e);
        }
        return conceptsWithObservations;
    }
}
