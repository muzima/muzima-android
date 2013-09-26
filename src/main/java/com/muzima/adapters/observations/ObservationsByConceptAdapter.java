package com.muzima.adapters.observations;

import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.muzima.R;
import com.muzima.api.model.Observation;
import com.muzima.controller.ConceptController;
import com.muzima.controller.ObservationController;
import com.muzima.model.observation.ConceptWithObservations;
import com.muzima.utils.DateUtils;
import com.muzima.utils.Fonts;

public class ObservationsByConceptAdapter extends ObservationsAdapter<ConceptWithObservations> {

    private static final String TAG = "ObservationsByConceptAdapter";

    public ObservationsByConceptAdapter(FragmentActivity activity, int itemCohortsList,
                                        ConceptController conceptController, ObservationController observationController) {
        super(activity, itemCohortsList, conceptController, observationController);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ObservationsByConceptViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(R.layout.item_observation_by_concept_list, parent, false);
            holder = new ObservationsByConceptViewHolder();
            holder.headerText = (TextView) convertView
                    .findViewById(R.id.observation_header);
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
        new ObservationsByConceptBackgroundTask(this, new ConceptsByPatient(observationController, patientUuid)).execute();
    }

    public void search(String term) {
        new ObservationsByConceptBackgroundTask(this, new ConceptsBySearch(observationController, patientUuid, term)).execute();
    }

    protected class ObservationsByConceptViewHolder extends ViewHolder{
        TextView headerText;

        public ObservationsByConceptViewHolder() {
            super();
        }

        private void renderItem(ConceptWithObservations item) {
            int conceptColor = observationController.getConceptColor(item.getConcept().getUuid());
            headerText.setBackgroundColor(conceptColor);
            addEncounterObservations(item.getObservations());
            headerText.setText(getConceptDisplay(item.getConcept()));
        }

        @Override
        protected void setObservation(LinearLayout layout, Observation observation) {
            int conceptColor = observationController.getConceptColor(observation.getConcept().getUuid());

            TextView observationValue = (TextView) layout.findViewById(R.id.observation_value);
            observationValue.setText(observation.getValueAsString());
            observationValue.setTypeface(Fonts.roboto_medium(getContext()));
            observationValue.setTextColor(conceptColor);

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
