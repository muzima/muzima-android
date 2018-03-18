/*
 * Copyright (c) 2014 - 2017. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.adapters.observations;

import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.muzima.R;
import com.muzima.api.model.Observation;
import com.muzima.controller.ConceptController;
import com.muzima.controller.ObservationController;
import com.muzima.model.observation.ConceptWithObservations;
import com.muzima.utils.BackgroundTaskHelper;
import com.muzima.utils.DateUtils;
import com.muzima.utils.Fonts;
import com.muzima.utils.StringUtils;

public class ObservationsByConceptAdapter extends ObservationsAdapter<ConceptWithObservations> {

    private static final String TAG = "ObservationsByConceptAdapter";

    public ObservationsByConceptAdapter(FragmentActivity activity, int itemCohortsList,
                                        ConceptController conceptController,
                                        ObservationController observationController) {
        super(activity, itemCohortsList, null,conceptController, observationController);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ObservationsByConceptViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(R.layout.item_observation_by_concept_list, parent, false);
            holder = new ObservationsByConceptViewHolder();
            holder.headerText = (TextView) convertView.findViewById(R.id.observation_header);
            holder.addObsButton = (ImageButton) convertView.findViewById(R.id.add_individual_obs_imagebutton);
            holder.headerLayout = (RelativeLayout)convertView.findViewById(R.id.observation_header_layout);
            holder.observationLayout = (LinearLayout) convertView
                    .findViewById(R.id.observation_layout);
            convertView.setTag(holder);
        } else {
            holder = (ObservationsByConceptViewHolder) convertView.getTag();
        }

        holder.renderItem(getItem(position));
        return convertView;
    }

    @Override
    public void reloadData() {
        cancelBackgroundQueryTask();
        AsyncTask<Void,?,?> backgroundQueryTask = new ObservationsByConceptBackgroundTask(this,
                new ConceptsByPatient(conceptController, observationController, patientUuid));
        BackgroundTaskHelper.executeInParallel(backgroundQueryTask);
        setRunningBackgroundQueryTask(backgroundQueryTask);
    }

    public void search(String term) {
        cancelBackgroundQueryTask();
        AsyncTask<Void,?,?> backgroundQueryTask = new ObservationsByConceptBackgroundTask(this,
                new ConceptsBySearch(conceptController,observationController, patientUuid, term));
        BackgroundTaskHelper.executeInParallel(backgroundQueryTask);
        setRunningBackgroundQueryTask(backgroundQueryTask);
    }

    protected class ObservationsByConceptViewHolder extends ViewHolder{
        TextView headerText;
        ImageButton addObsButton;
        RelativeLayout headerLayout;

        public ObservationsByConceptViewHolder() {
            super();
        }

        private void renderItem(ConceptWithObservations item) {
            int conceptColor = observationController.getConceptColor(item.getConcept().getUuid());
            headerLayout.setBackgroundColor(conceptColor);
            addObsButton.setBackgroundColor(conceptColor);
            addEncounterObservations(item.getObservations());
            headerText.setText(getConceptDisplay(item.getConcept()));
        }

        @Override
        protected void setObservation(LinearLayout layout, Observation observation) {
            int conceptColor = observationController.getConceptColor(observation.getConcept().getUuid());

            String observationConceptType = observation.getConcept().getConceptType().getName();

            TextView observationValue = (TextView) layout.findViewById(R.id.observation_value);
            ImageView observationComplexHolder = (ImageView) layout.findViewById(R.id.observation_complex);
            if (StringUtils.equals(observationConceptType, "Complex")){
                observationValue.setVisibility(View.GONE);
                observationComplexHolder.setVisibility(View.VISIBLE);
            } else {
                observationValue.setVisibility(View.VISIBLE);
                observationComplexHolder.setVisibility(View.GONE);
                observationValue.setText(observation.getValueAsString());
                observationValue.setTypeface(Fonts.roboto_medium(getContext()));
                observationValue.setTextColor(conceptColor);
            }

            View divider = layout.findViewById(R.id.divider);
            divider.setBackgroundColor(conceptColor);

            TextView observationDateView = (TextView) layout.findViewById(R.id.observation_date);
            observationDateView.setText(DateUtils.getMonthNameFormattedDate(observation.getObservationDatetime()));
            observationDateView.setTypeface(Fonts.roboto_light(getContext()));
            observationDateView.setTextColor(conceptColor);
        }

        @Override
        protected int getObservationLayout() {
            return R.layout.item_observation_by_concept;
        }

        @Override
        protected int getObservationElementHeight() {
            return R.dimen.observation_element_by_concept_height;
        }
    }
}
