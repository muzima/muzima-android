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
        Log.d(TAG, "concept:" + item.getConcept().getName() + " observation num:" + item.getObservations().size());
        holder.addObservations(item.getObservations());
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

        protected void addObservations(List<Observation> observations) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            for (int i = 0; i < observations.size(); i++) {
                LinearLayout layout = null;
                if (observationViewHolders.size() <= i) {
                    layout = (LinearLayout) inflater.inflate(R.layout.item_observation_by_concept, null);
                    observationViewHolders.add(layout);
                    observationLayout.addView(layout);
                } else {
                    layout = observationViewHolders.get(i);
                }

                TextView observationValue = (TextView) layout.findViewById(R.id.observation_value);
                //TODO: Figure out the right type of the observation
                Observation observation = observations.get(i);
                observationValue.setText(observation.getValueAsString());
                TextView observationDateView = (TextView) layout.findViewById(R.id.observation_date);
                observationDateView.setText(DateUtils.getFormattedDateTime(observation.getObservationDatetime()));
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
            headerText.setText(concept.getName());
        }
    }

}
