package com.muzima.adapters.observations;

import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.Observation;
import com.muzima.controller.ObservationController;
import com.muzima.util.Constants;
import com.muzima.utils.DateUtils;
import com.muzima.utils.Fonts;
import com.muzima.view.patients.PatientSummaryActivity;

public abstract class  ObservationsAdapter extends ListAdapter<Observation> {
    protected final String patientUuid;
    protected ObservationController observationController;

    public ObservationsAdapter(FragmentActivity context, int textViewResourceId, ObservationController observationController) {
        super(context, textViewResourceId);
        this.observationController = observationController;
        patientUuid = context.getIntent().getStringExtra(PatientSummaryActivity.PATIENT_ID);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(
                    R.layout.item_observation_list, parent, false);
            holder = new ViewHolder();
            holder.description = (TextView) convertView
                    .findViewById(R.id.observation_description);
            holder.value = (TextView) convertView
                    .findViewById(R.id.observation_value);
            holder.date = (TextView) convertView
                    .findViewById(R.id.observation_date);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Observation observation = getItem(position);

        holder.description.setText(getObservationDescription(observation));
        holder.description.setTypeface(Fonts.roboto_medium(getContext()));

        holder.value.setText(getObservationValue(observation));
        holder.value.setTypeface(Fonts.roboto_light(getContext()));

        holder.date.setText(DateUtils.getFormattedDate(observation.getObservationDate()));
        holder.value.setTypeface(Fonts.roboto_light(getContext()));

        return convertView;
    }

    private String getObservationDescription(Observation observation) {
        return observation.getQuestionName();
    }

    private String getObservationValue(Observation observation) {
        switch (observation.getDataType()){
            //TODO to add some extra info like unit of measure
            case Constants.TYPE_NUMERIC:
                return observation.getValue();
            case Constants.TYPE_DATE:
                return observation.getValue();
            case Constants.TYPE_STRING:
                return observation.getValue();
        }
        return observation.getValue();
    }

    protected static class ViewHolder {
        TextView description;
        TextView value;
        TextView date;
    }
}
