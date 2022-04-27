/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.adapters.observations;

import static com.muzima.utils.ConceptUtils.getConceptNameFromConceptNamesByLocale;
import static com.muzima.utils.Constants.FGH.Concepts.HEALTHWORKER_ASSIGNMENT_CONCEPT_ID;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.R;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Provider;
import com.muzima.controller.EncounterController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.ProviderController;
import com.muzima.utils.DateUtils;
import com.muzima.utils.StringUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class ObsVerticalViewAdapter extends RecyclerView.Adapter<ObsVerticalViewAdapter.ViewHolder> {
    private List<Observation> observationList = new ArrayList<>();
    private final String date;
    final EncounterController encounterController;
    final ObservationController observationController;
    final ProviderController providerController;
    private final String applicationLanguage;
    private final Boolean shouldReplaceProviderIdWithNames;
    private final String patientUuid;

    public ObsVerticalViewAdapter(String date,
                                  EncounterController encounterController, ObservationController observationController,
                                  String applicationLanguage, ProviderController providerController,
                                  boolean shouldReplaceProviderIdWithNames, String patientUuid) {
        this.date = date;
        this.encounterController = encounterController;
        this.observationController = observationController;
        this.applicationLanguage = applicationLanguage;
        this.providerController = providerController;
        this.shouldReplaceProviderIdWithNames = shouldReplaceProviderIdWithNames;
        this.patientUuid = patientUuid;
        observationList = getObservationForDate(date);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.timeline_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Observation observation = observationList.get(position);

        holder.concept.setText(observation.getConcept().getName());
        if (StringUtils.equals(observation.getConcept().getConceptType().getName(), "Complex")) {
            holder.observationValue.setVisibility(View.GONE);
            holder.observationComplexHolder.setVisibility(View.VISIBLE);
        } else {
            holder.observationValue.setVisibility(View.VISIBLE);
            holder.observationComplexHolder.setVisibility(View.GONE);
            if (observation.getConcept().isNumeric())
                holder.observationValue.setText(String.valueOf(observation.getValueNumeric()));

            if (observation.getConcept().isDatetime())
                holder.observationValue.setText(DateUtils.convertDateToStdString(observation.getValueDatetime()));

            if (observation.getConcept().isCoded())
                holder.observationValue.setText(getConceptNameFromConceptNamesByLocale(observation.getValueCoded().getConceptNames(),applicationLanguage));

            if (!observation.getConcept().isNumeric() && !observation.getConcept().isDatetime() && !observation.getConcept().isCoded()){
                if(shouldReplaceProviderIdWithNames && observation.getConcept().getId() == HEALTHWORKER_ASSIGNMENT_CONCEPT_ID){
                    Provider provider = providerController.getProviderBySystemId(observation.getValueAsString());
                    if(provider != null){
                        holder.observationValue.setText(provider.getName());
                    }else {
                        holder.observationValue.setText(observation.getValueAsString());
                    }
                }else {
                    holder.observationValue.setText(observation.getValueText());
                }
            }

        }

        holder.observationDate.setText(DateUtils.getTime(observation.getObservationDatetime()));
    }

    @Override
    public int getItemCount() {
        return observationList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView observationValue;
        private final TextView observationDate;
        private final ImageView observationComplexHolder;
        private final View observationContainer;
        private final TextView concept;

        public ViewHolder(@NonNull View view) {
            super(view);
            View container = view.findViewById(R.id.item_single_obs_container);
            this.observationContainer = view.findViewById(R.id.value_container);
            this.observationValue = view.findViewById(R.id.observation_value);
            this.observationDate = view.findViewById(R.id.item_single_obs_date_text_view);
            this.observationComplexHolder = view.findViewById(R.id.observation_complex);
            this.concept = view.findViewById(R.id.concept);
        }
    }

    public List<Observation> getObservationForDate(String date){
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        observationList = new ArrayList<>();
        try {
            List<Observation> observations = observationController.getObservationsByPatient(patientUuid);
            for (Observation observation : observations){
                if (date.equals(dateFormat.format(observation.getObservationDatetime()))) {
                    observationList.add(observation);
                }
            }
        } catch (ObservationController.LoadObservationException e) {
            e.printStackTrace();
        }
        return observationList;
    }
}
