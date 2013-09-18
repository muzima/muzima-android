package com.muzima.adapters.observations;

import android.support.v4.app.FragmentActivity;
import com.muzima.controller.ConceptController;
import com.muzima.controller.ObservationController;
import com.muzima.model.observation.ConceptWithObservations;

public class ObservationsByConceptAdapter extends ObservationsAdapter<ConceptWithObservations> {

    public ObservationsByConceptAdapter(FragmentActivity activity, int itemCohortsList,
                                        ConceptController conceptController, ObservationController observationController) {
        super(activity, itemCohortsList, conceptController, observationController);
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask(this, new ConceptsByPatient(observationController, patientUuid)).execute();
    }

    public void search(String term) {
        new BackgroundQueryTask(this, new ConceptsBySearch(observationController, patientUuid, term)).execute();
    }


}
