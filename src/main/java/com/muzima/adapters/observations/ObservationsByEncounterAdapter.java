package com.muzima.adapters.observations;

import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.muzima.R;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.Observation;
import com.muzima.controller.ConceptController;
import com.muzima.controller.ObservationController;
import com.muzima.model.observation.EncounterWithObservations;
import com.muzima.utils.DateUtils;
import com.muzima.utils.Fonts;

public class ObservationsByEncounterAdapter extends ObservationsAdapter<EncounterWithObservations> {
    public ObservationsByEncounterAdapter(FragmentActivity activity, int item_observation_list, ConceptController conceptController, ObservationController observationController) {
        super(activity,item_observation_list,conceptController,observationController);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ObservationsByEncounterViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(R.layout.item_observation_by_encounter_list, parent, false);
            holder = new ObservationsByEncounterViewHolder();
            holder.observationLayout = (LinearLayout) convertView
                    .findViewById(R.id.observation_layout);
            holder.headerLayout = (LinearLayout) convertView.findViewById(R.id.observation_header);
            holder.encounterType = (TextView) convertView.findViewById(R.id.encounter_type);
            holder.encounterDate = (TextView) convertView.findViewById(R.id.encounter_date);
            holder.encounterLocation = (TextView) convertView.findViewById(R.id.encounter_location);
            convertView.setTag(holder);
        } else {
            holder = (ObservationsByEncounterViewHolder) convertView.getTag();
        }

        holder.renderItem(getItem(position));
        return convertView;
    }


    @Override
    public void reloadData() {
        new ObservationsByEncounterBackgroundTask(this, new EncountersByPatient(observationController, patientUuid)).execute();
    }

    public void search(String query) {
        new ObservationsByEncounterBackgroundTask(this, new EncountersBySearch(observationController, patientUuid, query)).execute();
    }

    protected class ObservationsByEncounterViewHolder extends ViewHolder {
        public TextView encounterType;
        public TextView encounterDate;
        public TextView encounterLocation;
        public LinearLayout headerLayout ;

        public ObservationsByEncounterViewHolder() {
            super();
        }

        private void renderItem(EncounterWithObservations item) {
            addEncounterObservations(item.getObservations());
            setEncounter(item.getEncounter());
            headerLayout.setBackgroundColor(getContext().getResources().getColor(R.color.observation_by_encounter_header_background));
        }

        @Override
        protected void setObservation(LinearLayout layout, Observation observation) {
            TextView conceptInfo = (TextView) layout.findViewById(R.id.concept_info);
            conceptInfo.setText(getConceptDisplay(observation.getConcept()));
            conceptInfo.setTypeface(Fonts.roboto_medium(getContext()));
            int conceptColor = observationController.getConceptColor(observation.getConcept().getUuid());
            conceptInfo.setTextColor(conceptColor);

            View divider = layout.findViewById(R.id.divider1);
            divider.setBackgroundColor(conceptColor);

            TextView observationValue = (TextView) layout.findViewById(R.id.observation_value);
            observationValue.setText(observation.getValueAsString());
            observationValue.setTypeface(Fonts.roboto_medium(getContext()));
            observationValue.setTextColor(conceptColor);

            View divider2 = layout.findViewById(R.id.divider2);
            divider2.setBackgroundColor(conceptColor);

            TextView observationDateView = (TextView) layout.findViewById(R.id.observation_date);
            observationDateView.setText(DateUtils.getMonthNameFormattedDate(observation.getObservationDatetime()));
            observationDateView.setTypeface(Fonts.roboto_light(getContext()));
            observationDateView.setTextColor(conceptColor);
        }

        public void setEncounter(Encounter encounter) {
            encounterType.setText(encounter.getEncounterType().getName());
            encounterDate.setText(DateUtils.getMonthNameFormattedDate(encounter.getEncounterDatetime()));
            encounterLocation.setText(encounter.getLocation().getName());
        }

        @Override
        protected int getObservationLayout() {
            return R.layout.item_observation_by_encounter;
        }

        @Override
        protected int getObservationElementHeight() {
            return R.dimen.observation_element_by_encounter_height;
        }
    }
}
