package com.muzima.adapters.observations;

import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.muzima.R;
import com.muzima.controller.ConceptController;
import com.muzima.controller.ObservationController;
import com.muzima.model.observation.EncounterWithObservations;
import com.muzima.utils.CustomColor;
import com.muzima.utils.DateUtils;

public class ObservationsByEncounterAdapter extends ObservationsAdapter<EncounterWithObservations> {
    public ObservationsByEncounterAdapter(FragmentActivity activity, int item_observation_list, ConceptController conceptController, ObservationController observationController) {
        super(activity,item_observation_list,conceptController,observationController);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(R.layout.item_observation_by_encounter_list, parent, false);
            holder = new ViewHolder();
            holder.observationLayout = (LinearLayout) convertView
                    .findViewById(R.id.observation_layout);
            holder.headerLayout = (LinearLayout) convertView.findViewById(R.id.observation_header);
            holder.encounterType = (TextView) convertView.findViewById(R.id.encounter_type);
            holder.encounterDate = (TextView) convertView.findViewById(R.id.encounter_date);
            holder.encounterLocation = (TextView) convertView.findViewById(R.id.encounter_location);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        renderItem(position, holder);
        return convertView;
    }


    protected void renderItem(int position, ViewHolder holder) {
        EncounterWithObservations item = getItem(position);
        holder.addEncounterObservations(item.getObservations());
        holder.setEncounter(item.getEncounter());

        holder.headerLayout.setBackgroundColor(observationController.getEncounterColor(item.getEncounter().getEncounterType().getUuid()));
    }

    @Override
    public void reloadData() {
        new ObservationsByEncounterBackgroundTask(this, new EncountersByPatient(observationController, patientUuid)).execute();
    }

    public void search(String query) {
        new ObservationsByEncounterBackgroundTask(this, new EncountersBySearch(observationController, patientUuid, query)).execute();
    }
}
