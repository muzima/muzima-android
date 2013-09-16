package com.muzima.adapters.observations;

import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.muzima.api.model.Concept;
import com.muzima.api.model.Observation;
import com.muzima.controller.ConceptController;
import com.muzima.controller.ObservationController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObservationsByDateAdapter extends ObservationsAdapter {
    private static final String TAG = "ObservationsByDateAdapter";
    private static final String SEARCH = "search";

    public ObservationsByDateAdapter(FragmentActivity activity, int itemCohortsList,
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

    public class BackgroundQueryTask extends AsyncTask<String, Void, List<Observation>> {

        @Override
        protected List<Observation> doInBackground(String... params) {
            if(isSearch(params)){
                Log.d(TAG, "searching observations for query string: " + params[0]);
                try {
                    Map<String, Concept> conceptMap = new HashMap<String, Concept>();
                    List<Observation> observations = observationController.searchObservations(params[0], patientUuid);
                    for (Observation observation : observations) {
                        Concept concept = observation.getConcept();
                        Concept fullConcept = conceptMap.get(concept.getUuid());
                        if (fullConcept == null) {
                            fullConcept = conceptController.getConceptByUuid(concept.getUuid());
                            conceptMap.put(fullConcept.getUuid(), fullConcept);
                        }
                        observation.setConcept(fullConcept);
                    }
                    return observations;
                } catch (ObservationController.LoadObservationException e) {
                    Log.w(TAG, "Exception occurred while searching observations for " + params[0] + " search string. " + e);
                } catch (ConceptController.ConceptFetchException e) {
                    Log.w(TAG, "Exception occurred while searching concept!", e);
                }
            }

            List<Observation> observations = null;

            try {
                observations = observationController.getObservationsByDate(params[0]);
                Map<String, Concept> conceptMap = new HashMap<String, Concept>();
                for (Observation observation : observations) {
                    Concept concept = observation.getConcept();
                    Concept fullConcept = conceptMap.get(concept.getUuid());
                    if (fullConcept == null) {
                        fullConcept = conceptController.getConceptByUuid(concept.getUuid());
                        conceptMap.put(fullConcept.getUuid(), fullConcept);
                    }
                    observation.setConcept(fullConcept);
                }
                Log.i(TAG, "#Observations: " + observations.size());
            } catch (ObservationController.LoadObservationException e) {
                Log.e(TAG, "Exception occurred while fetching observations " + e);
            } catch (ConceptController.ConceptFetchException e) {
                Log.w(TAG, "Exception occurred while searching concept!", e);
            }
            return observations;
        }

        private boolean isSearch(String[] params) {
            return params.length == 2 && SEARCH.equals(params[1]);
        }

        @Override
        protected void onPostExecute(List<Observation> observations) {
            if (observations == null) {
                Toast.makeText(getContext(), "Something went wrong while fetching observations from local repo", Toast.LENGTH_SHORT).show();
                return;
            }

            ObservationsByDateAdapter.this.clear();
            addAll(observations);
            notifyDataSetChanged();
        }
    }
}
