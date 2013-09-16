package com.muzima.adapters.observations;

import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.Concept;
import com.muzima.api.model.Observation;
import com.muzima.controller.ConceptController;
import com.muzima.controller.ObservationController;
import com.muzima.util.Constants;
import com.muzima.utils.DateUtils;
import com.muzima.utils.Fonts;
import com.muzima.view.patients.PatientSummaryActivity;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public abstract class  ObservationsAdapter extends ListAdapter<Observation> {
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

        holder.date.setText(DateUtils.getFormattedDate(observation.getObservationDatetime()));
        holder.value.setTypeface(Fonts.roboto_light(getContext()));

        return convertView;
    }

    private String getObservationDescription(Observation observation) {
        Concept concept = observation.getConcept();
        return concept.getName();
    }

    private String getObservationValue(Observation observation) {
        Concept concept = observation.getConcept();
        if (concept.isCoded()) {
            Concept valueCoded = observation.getValueCoded();
            return valueCoded.getName();
        } else if (concept.isNumeric()) {
            NumberFormat numberFormat = NumberFormat.getNumberInstance();
            DecimalFormat decimalFormat = (DecimalFormat) numberFormat;
            if (!concept.isPrecise()) {
                decimalFormat.applyPattern("#");
            } else {
                decimalFormat.applyPattern("#.###");
            }
            return decimalFormat.format(observation.getValueNumeric());
        } else if (concept.isDatetime()) {
            return DateUtils.getFormattedDateTime(observation.getValueDatetime());
        } else {
            return observation.getValueText();
        }
    }

    protected static class ViewHolder {
        TextView description;
        TextView value;
        TextView date;
    }
}
