package com.muzima.adapters.observations;

import android.support.v4.app.FragmentActivity;
import android.util.Log;
import com.muzima.controller.ConceptController;
import com.muzima.controller.ObservationController;
import com.muzima.model.observation.ConceptWithObservations;

public class ObservationsByConceptAdapter extends ObservationsAdapter<ConceptWithObservations> {

    private static final String TAG = "ObservationsByConceptAdapter";

    public ObservationsByConceptAdapter(FragmentActivity activity, int itemCohortsList,
                                        ConceptController conceptController, ObservationController observationController) {
        super(activity, itemCohortsList, conceptController, observationController);
    }

    @Override
    protected void renderItem(int position, ViewHolder holder) {
        ConceptWithObservations item = getItem(position);

        int conceptColor = observationController.getConceptColor(item.getConcept().getUuid());
        holder.headerText.setBackgroundColor(conceptColor);
        holder.addObservations(item.getObservations(), conceptColor);
        holder.setConcept(item.getConcept());
    }

    @Override
    public void reloadData() {
        new ObservationsByConceptBackgroundTask(this, new ConceptsByPatient(observationController, patientUuid)).execute();
    }

    public void search(String term) {
        new ObservationsByConceptBackgroundTask(this, new ConceptsBySearch(observationController, patientUuid, term)).execute();
    }


}
