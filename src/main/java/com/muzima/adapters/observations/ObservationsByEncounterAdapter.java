package com.muzima.adapters.observations;

import android.support.v4.app.FragmentActivity;
import android.util.Log;
import com.muzima.controller.ConceptController;
import com.muzima.controller.ObservationController;
import com.muzima.model.observation.ConceptWithObservations;
import com.muzima.model.observation.EncounterWithObservations;

public class ObservationsByEncounterAdapter extends ObservationsAdapter<EncounterWithObservations> {
    public ObservationsByEncounterAdapter(FragmentActivity activity, int item_observation_list, ConceptController conceptController, ObservationController observationController) {
        super(activity,item_observation_list,conceptController,observationController);
    }

    @Override
    protected void renderItem(int position, ViewHolder holder) {
        EncounterWithObservations item = getItem(position);

//        holder.headerText.setBackgroundColor(conceptColor);

        holder.addEncounterObservations(item.getObservations());
        holder.setEncounter(item.getEncounter());

    }

    @Override
    public void reloadData() {
        new ObservationsByEncounterBackgroundTask(this, new EncountersByPatient(observationController, patientUuid)).execute();
    }
}
