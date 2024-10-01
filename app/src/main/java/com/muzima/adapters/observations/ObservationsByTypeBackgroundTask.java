/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.adapters.observations;

import android.os.AsyncTask;
import android.util.Log;
import com.muzima.api.model.Concept;
import com.muzima.controller.ObservationController;
import com.muzima.model.observation.ConceptWithObservations;
import com.muzima.model.observation.Concepts;
import com.muzima.utils.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * AsyncTask Class that orchestrate a background non ui thread that loads
 * Observation data by concept from local storage.
 *
 * @see AsyncTask
 * @see com.muzima.api.model.Observation
 * @see Concept
 */
class ObservationsByTypeBackgroundTask extends AsyncTask<Void, Concepts, Concepts> {

    private final ConceptAction conceptAction;
    private final ObservationsByTypeAdapter observationsByTypeAdapter;
    private final Boolean isSHRData;
    private final Boolean isAddSingleElement;
    private List<Integer> SHRConcepts = new ArrayList<>();

    public ObservationsByTypeBackgroundTask(ObservationsByTypeAdapter observationsByTypeAdapter,
                                            ConceptAction conceptAction, boolean isSHRData, boolean isAddSingleElement) {
        this.observationsByTypeAdapter = observationsByTypeAdapter;
        this.conceptAction = conceptAction;
        this.isSHRData = isSHRData;
        this.isAddSingleElement = isAddSingleElement;

        if (isSHRData) {
            loadComposedSHRConceptId();
        }
    }

    @Override
    protected void onPreExecute() {
        if (observationsByTypeAdapter.getBackgroundListQueryTaskListener() != null) {
            observationsByTypeAdapter.getBackgroundListQueryTaskListener().onQueryTaskStarted();
        }
    }

    @Override
    protected Concepts doInBackground(Void... params) {
        Concepts conceptsWithObservations;

        if (!isAddSingleElement) {
            if (isSHRData)
                conceptsWithObservations = getSHRConceptWithObservations();
            else
                conceptsWithObservations = getNonSHRConceptWithObservations();
        } else
            conceptsWithObservations = getAddableConcepts();

        return conceptsWithObservations;
    }


    @Override
    protected void onPostExecute(Concepts conceptsWithObservations) {
        if (conceptsWithObservations != null) {
            observationsByTypeAdapter.clear();
            observationsByTypeAdapter.add(conceptsWithObservations);
            observationsByTypeAdapter.notifyDataSetChanged();
        }

        if (observationsByTypeAdapter.getBackgroundListQueryTaskListener() != null) {
            observationsByTypeAdapter.getBackgroundListQueryTaskListener().onQueryTaskFinish();
        }
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
        Concepts temp;
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
                    }
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
                if (!isCancelled() && SHRConcepts.contains(concept.getConceptid())) {
                    temp = conceptAction.get(concept);
                    if (temp != null) {
                        temp.sortByDate();
                        if (conceptsWithObservations == null) {
                            conceptsWithObservations = temp;
                        } else {
                            conceptsWithObservations.addAll(temp);
                        }
                    }
                }
            }
        } catch (ObservationController.LoadObservationException e) {
            Log.w("Observations", String.format("Exception while loading observations for %s.", conceptAction), e);
        }
        return conceptsWithObservations;
    }

    private Concepts getAddableConcepts() {
        Concepts conceptsWithObservations = null;
        Concepts temp;
        try {
            List<Concept> concepts = conceptAction.getConcepts();
            for (Concept concept : concepts) {
                if (!concept.getConceptType().getConceptTypeName().equals(Concept.CODED_TYPE)) {
                    temp = conceptAction.get(concept);
                    if (temp != null) {
                        if (temp.size() > 0) {
                            temp.sortByDate();
                            if (conceptsWithObservations == null)
                                conceptsWithObservations = temp;
                            else
                                conceptsWithObservations.addAll(temp);
                        } else {
                            if (conceptsWithObservations == null)
                                conceptsWithObservations = new Concepts();

                            ConceptWithObservations cwo = new ConceptWithObservations();
                            cwo.setConcept(concept);
                            conceptsWithObservations.add(cwo);
                        }
                    }
                }
            }
        } catch (ObservationController.LoadObservationException e) {
            Log.w("Observations", String.format("Exception while loading observations for %s.", conceptAction), e);
        }
        return conceptsWithObservations;
    }
}
