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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.muzima.R;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.Observation;
import com.muzima.controller.ConceptController;
import com.muzima.controller.ObservationController;
import com.muzima.model.observation.EncounterWithObservations;
import com.muzima.search.api.util.StringUtil;
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
            holder.encounterProvider = (TextView) convertView.findViewById(R.id.encounter_provider);
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
        public TextView encounterProvider;
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

            String observationConceptType = observation.getConcept().getConceptType().getName();

            TextView observationValue = (TextView) layout.findViewById(R.id.observation_value);
            ImageView observationComplexHolder = (ImageView) layout.findViewById(R.id.observation_complex);
            if (StringUtil.equals(observationConceptType, "Complex")){
                observationValue.setVisibility(View.GONE);
                observationComplexHolder.setVisibility(View.VISIBLE);
            } else {
                observationValue.setVisibility(View.VISIBLE);
                observationComplexHolder.setVisibility(View.GONE);
                observationValue.setText(observation.getValueAsString());
                observationValue.setTypeface(Fonts.roboto_medium(getContext()));
                observationValue.setTextColor(conceptColor);
            }

            View divider2 = layout.findViewById(R.id.divider2);
            divider2.setBackgroundColor(conceptColor);

            TextView observationDateView = (TextView) layout.findViewById(R.id.observation_date);
            observationDateView.setText(DateUtils.getMonthNameFormattedDate(observation.getObservationDatetime()));
            observationDateView.setTypeface(Fonts.roboto_light(getContext()));
            observationDateView.setTextColor(conceptColor);
        }

        public void setEncounter(Encounter encounter) {
            encounterProvider.setText(encounter.getProvider().getDisplayName());
            String date = "";
            boolean isEncounterForObservationWithNonNullEncounterUuid = !isEncounterForObservationWithNullEncounterUuid(encounter);
            if(isEncounterForObservationWithNonNullEncounterUuid)
                date = DateUtils.getMonthNameFormattedDate(encounter.getEncounterDatetime());
            encounterDate.setText(date);
            encounterLocation.setText(encounter.getLocation().getName());
        }

        private boolean isEncounterForObservationWithNullEncounterUuid(Encounter encounter) {
            return encounter.getProvider().getFamilyName() == null && encounter.getProvider().getMiddleName() == null
                    && encounter.getProvider().getGivenName() == null;
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
