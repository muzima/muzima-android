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
class ObservationsByConceptBackgroundTask extends AsyncTask<Void, Concepts, Concepts> {

    private final ConceptAction conceptAction;
    private final ObservationsByConceptAdapter observationsByConceptAdapter;
    private Boolean isSHRData = false;
    private List<Integer> SHRConcepts = new ArrayList<>();

    public ObservationsByConceptBackgroundTask(ObservationsByConceptAdapter observationsByConceptAdapter,
                                               ConceptAction conceptAction, boolean isSHRData) {
        this.observationsByConceptAdapter = observationsByConceptAdapter;
        this.conceptAction = conceptAction;
        this.isSHRData = isSHRData;

        if (isSHRData) {
            loadComposedSHRConceptId();
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
        if (isSHRData) {
            conceptsWithObservations = getSHRConceptWithObservations();
        } else {
            conceptsWithObservations = getNonSHRConceptWithObservations();
        }

        return conceptsWithObservations;
    }


    @Override
    protected void onPostExecute(Concepts conceptsWithObservations) {
        if (conceptsWithObservations == null) {
            if (isSHRData) {
                Toast.makeText(observationsByConceptAdapter.getContext(), "This patient does not have any SHR data.", Toast.LENGTH_SHORT).show();
            } else
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

    private void loadComposedSHRConceptId() {
        List<Integer> conceptIds = new ArrayList<>();
        conceptIds.add(Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_RESULT.concept_id);
        conceptIds.add(Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_TYPE.concept_id);
        conceptIds.add(Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.concept_id);
        conceptIds.add(Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.concept_id);
        conceptIds.add(Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.SEQUENCE.concept_id);
        conceptIds.add(Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.GROUP.concept_id);


        SHRConcepts = conceptIds;
    }

    private Concepts getNonSHRConceptWithObservations() {
        Concepts conceptsWithObservations = null;
        Concepts temp = null;
        try {
            List<Concept> concepts = conceptAction.getConcepts();
            for (Concept concept : concepts) {
                if (!isCancelled() && !concept.getName().contains("NAME") && !concept.getName().contains("ID") && !concept.getName().contains("TEST FACILITY")) {
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
                    //break;
                }
            }
        } catch (ObservationController.LoadObservationException e) {
            Log.w("Observations", String.format("Exception while loading observations for %s.", conceptAction), e);
        }
        return conceptsWithObservations;
    }

    private Concepts getSHRConceptWithObservations() {
        Concepts conceptsWithObservations = null;
        Concepts temp = null;
        try {
            List<Concept> concepts = conceptAction.getConcepts();

            for (Concept concept : concepts) {
                if (!isCancelled() && SHRConcepts.contains(concept.getId())) {
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
                    // break;
                }
            }
        } catch (ObservationController.LoadObservationException e) {
            Log.w("Observations", String.format("Exception while loading observations for %s.", conceptAction), e);
        }
        return conceptsWithObservations;
    }
}
