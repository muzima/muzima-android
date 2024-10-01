/*
 * Copyright (c) Vanderbilt University Medical Center and Lambda Informatics.
 * All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.adapters.observations;

import static com.muzima.utils.ConceptUtils.getConceptNameFromConceptNamesByLocale;
import static com.muzima.utils.Constants.FGH.Concepts.HEALTHWORKER_ASSIGNMENT_CONCEPT_ID;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Concept;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Provider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class ObsListAdapter extends RecyclerView.Adapter<ObsListAdapter.ViewHolder> {
    private Context context;
    private ArrayList<Observation> observations;
    private boolean shouldReplaceProviderIdWithNames;
    private Concept concept;
    private String applicationLanguage;
    private MuzimaApplication app;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    public ObsListAdapter(ArrayList<Observation> observations, Context context, boolean shouldReplaceProviderIdWithNames, Concept concept, MuzimaApplication muzimaApplication, String applicationLanguage) {
        this.observations = observations;
        this.context = context;
        this.shouldReplaceProviderIdWithNames = shouldReplaceProviderIdWithNames;
        this.concept = concept;
        this.app = muzimaApplication;
        this.applicationLanguage = applicationLanguage;
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View v =layoutInflater.inflate(R.layout.table_layout,parent,false);
        return new ViewHolder(v);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Observation observation = observations.get(position);
        String value = "";
        if (shouldReplaceProviderIdWithNames && observation.getConcept().getConceptid() == HEALTHWORKER_ASSIGNMENT_CONCEPT_ID) {
            Provider provider = app.getProviderController().getProviderBySystemId(observation.getValueText());
            if (provider != null) {
                value = provider.getName();
            } else {
                value = observation.getValueText();
            }
        } else {
            if (concept.isNumeric()) {
                value = String.valueOf(observation.getValueNumeric());
            } else if (concept.isCoded()) {
                value = getConceptNameFromConceptNamesByLocale(observation.getValueCoded().getConceptNames(), applicationLanguage);
            } else if (concept.isDatetime()) {
                if(observation.getValueDatetime() != null)
                    value = dateFormat.format(observation.getValueDatetime());
            } else {
                value = observation.getValueText();
            }
        }

        String conceptUnits = concept.getUnit();

        if (conceptUnits != null && conceptUnits != "") {
            value = value.concat(" ".concat(conceptUnits));
        }

        String obsDateValue = dateFormat.format(observation.getObservationDatetime());
        holder.obsValue.setText(value);
        holder.obsDate.setText(obsDateValue);
    }
    @Override
    public int getItemCount() {
        return observations.size();
    }
    class ViewHolder extends RecyclerView.ViewHolder{
        TextView obsDate,obsValue;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            obsDate = (TextView) itemView.findViewById(R.id.obs_date);
            obsValue = (TextView) itemView.findViewById(R.id.obs_value);
        }
    }
}
