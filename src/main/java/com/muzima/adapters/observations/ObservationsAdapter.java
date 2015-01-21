/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.adapters.observations;

import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.Concept;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Patient;
import com.muzima.controller.ConceptController;
import com.muzima.controller.ObservationController;
import com.muzima.view.patients.PatientSummaryActivity;

import java.util.ArrayList;
import java.util.List;

public abstract class ObservationsAdapter<T> extends ListAdapter<T> {
    private static final String TAG = "ObservationsAdapter";
    protected final String patientUuid;
    protected ConceptController conceptController;
    protected ObservationController observationController;
    protected BackgroundListQueryTaskListener backgroundListQueryTaskListener;

    public BackgroundListQueryTaskListener getBackgroundListQueryTaskListener() {
        return backgroundListQueryTaskListener;
    }

    public ObservationsAdapter(FragmentActivity context, int textViewResourceId,
                               ConceptController conceptController, ObservationController observationController) {
        super(context, textViewResourceId);
        this.conceptController = conceptController;
        this.observationController = observationController;
        Patient patient = (Patient) context.getIntent().getSerializableExtra(PatientSummaryActivity.PATIENT);
        patientUuid = patient.getUuid();
    }

    public void setBackgroundListQueryTaskListener(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
            this.backgroundListQueryTaskListener = backgroundListQueryTaskListener;
    }

    protected abstract class ViewHolder{

        protected LayoutInflater inflater;
        protected LinearLayout observationLayout;
        List<LinearLayout> observationViewHolders;

        protected ViewHolder() {
            observationViewHolders = new ArrayList<LinearLayout>();
            inflater = LayoutInflater.from(getContext());
        }

        protected void addEncounterObservations(List<Observation> observations) {
            for (int i = 0; i < observations.size(); i++) {
                LinearLayout layout = getLinearLayoutForObservation(i);
                Observation observation = observations.get(i);

                setObservation(layout, observation);
            }

            shrink(observations.size());
        }

        protected LinearLayout getLinearLayoutForObservation(int i) {
            LinearLayout layout;
            if (observationViewHolders.size() <= i) {
                layout = (LinearLayout) inflater.inflate(getObservationLayout(), null);
                observationViewHolders.add(layout);
                observationLayout.addView(layout);
            } else {
                layout = observationViewHolders.get(i);
            }

            setStyle(layout);
            return layout;
        }

        protected abstract void setObservation(LinearLayout layout, Observation observation);

        protected abstract int getObservationLayout();

        private void shrink(int startIndex) {
            List<LinearLayout> holdersToRemove = new ArrayList<LinearLayout>();
            for (int i = startIndex; i < observationViewHolders.size(); i++) {
                holdersToRemove.add(observationViewHolders.get(i));
            }
            removeObservations(holdersToRemove);
        }

        private void setStyle(LinearLayout layout) {
            int observationPadding = (int) getContext().getResources().getDimension(R.dimen.observation_element_padding);
            int width = (int) getContext().getResources().getDimension(getObservationElementHeight());
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, width);
            layoutParams.setMargins(observationPadding, observationPadding, observationPadding, observationPadding);
            layout.setLayoutParams(layoutParams);
        }

        protected abstract int getObservationElementHeight();

        private void removeObservations(List<LinearLayout> holdersToRemove) {
            observationViewHolders.removeAll(holdersToRemove);
            for (LinearLayout linearLayout : holdersToRemove) {
                observationLayout.removeView(linearLayout);
            }
        }

        protected String getConceptDisplay(Concept concept) {
            String text = concept.getName();
            if(concept.getConceptType().getName().equals(Concept.NUMERIC_TYPE)){
                text += " (" + concept.getUnit() +")";
            }
            return text;
        }
    }
}
