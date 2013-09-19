package com.muzima.adapters.observations;

import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.Concept;
import com.muzima.api.model.Observation;
import com.muzima.controller.ConceptController;
import com.muzima.controller.ObservationController;
import com.muzima.model.observation.ConceptWithObservations;
import com.muzima.utils.DateUtils;
import com.muzima.utils.Fonts;
import com.muzima.view.patients.PatientSummaryActivity;

import java.util.ArrayList;
import java.util.List;

public abstract class ObservationsAdapter<T> extends ListAdapter<T> {
    private static final String TAG = "ObservationsAdapter";
    protected final String patientUuid;
    protected ConceptController conceptController;
    protected ObservationController observationController;

    public ObservationsAdapter(FragmentActivity context, int textViewResourceId,
                               ConceptController conceptController, ObservationController observationController) {
        super(context, textViewResourceId);
        this.conceptController = conceptController;
        this.observationController = observationController;
        patientUuid = context.getIntent().getStringExtra(PatientSummaryActivity.PATIENT_ID);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(R.layout.item_observation_list, parent, false);
            holder = new ViewHolder();
            holder.headerText = (TextView) convertView
                    .findViewById(R.id.observation_header);
            holder.observationScroller = (HorizontalScrollView) convertView
                    .findViewById(R.id.observation_scroller);
            holder.observationLayout = (LinearLayout) convertView
                    .findViewById(R.id.observation_layout);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ConceptWithObservations item = (ConceptWithObservations) getItem(position);

        int conceptColor = observationController.getConceptColor(item.getConcept().getUuid());
        holder.headerText.setBackgroundColor(conceptColor);

        Log.d(TAG, "concept:" + item.getConcept().getName() + " observation num:" + item.getObservations().size());
        holder.addObservations(item.getObservations(), conceptColor);
        holder.setConcept(item.getConcept());
        return convertView;
    }


    protected class ViewHolder {
        TextView headerText;
        HorizontalScrollView observationScroller;
        LinearLayout observationLayout;
        List<LinearLayout> observationViewHolders;

        public ViewHolder() {
            observationViewHolders = new ArrayList<LinearLayout>();
        }

        protected void addObservations(List<Observation> observations, int conceptColor) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            int observationPadding = (int) getContext().getResources().getDimension(R.dimen.observation_element_padding);
            for (int i = 0; i < observations.size(); i++) {
                LinearLayout layout = null;
                if (observationViewHolders.size() <= i) {
                    layout = (LinearLayout) inflater.inflate(R.layout.item_observation_by_concept, null);
                    observationViewHolders.add(layout);
                    observationLayout.addView(layout);
                } else {
                    layout = observationViewHolders.get(i);
                }

                int width = (int) getContext().getResources().getDimension(R.dimen.observation_element_height);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, width);
                layoutParams.setMargins(observationPadding, observationPadding, observationPadding, observationPadding);
                layout.setLayoutParams(layoutParams);

                TextView observationValue = (TextView) layout.findViewById(R.id.observation_value);
                //TODO: Figure out the right type of the observation
                Observation observation = observations.get(i);
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


            if (observations.size() < observationViewHolders.size()) {
                List<LinearLayout> holdersToRemove = new ArrayList<LinearLayout>();
                for (int i = observations.size(); i < observationViewHolders.size(); i++) {
                    holdersToRemove.add(observationViewHolders.get(i));
                }
                removeObservations(holdersToRemove);
            }
        }

        private void removeObservations(List<LinearLayout> holdersToRemove) {
            observationViewHolders.removeAll(holdersToRemove);
            for (LinearLayout linearLayout : holdersToRemove) {
                observationLayout.removeView(linearLayout);
            }
        }

        public void setConcept(Concept concept) {
            String text = concept.getName();
            if(concept.getConceptType().getName().equals(Concept.NUMERIC_TYPE)){
                text += " (" + concept.getUnit() +")";
            }
            headerText.setText(text);
        }
    }

}
