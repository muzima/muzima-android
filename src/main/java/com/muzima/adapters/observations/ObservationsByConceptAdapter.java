package com.muzima.adapters.observations;

import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.muzima.R;
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
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(R.layout.item_observation_by_concept_list, parent, false);
            holder = new ViewHolder();
            holder.headerText = (TextView) convertView
                    .findViewById(R.id.observation_header);
            holder.observationLayout = (LinearLayout) convertView
                    .findViewById(R.id.observation_layout);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        renderItem(position, holder);
        return convertView;
    }

    protected void renderItem(int position, ViewHolder holder) {
        ConceptWithObservations item = getItem(position);

        int conceptColor = observationController.getConceptColor(item.getConcept().getUuid());
        holder.headerText.setBackgroundColor(conceptColor);
        holder.addObservations(item.getObservations(), conceptColor);
        holder.headerText.setText(holder.getConceptDisplay(item.getConcept()));
    }

    @Override
    public void reloadData() {
        new ObservationsByConceptBackgroundTask(this, new ConceptsByPatient(observationController, patientUuid)).execute();
    }

    public void search(String term) {
        new ObservationsByConceptBackgroundTask(this, new ConceptsBySearch(observationController, patientUuid, term)).execute();
    }
}
