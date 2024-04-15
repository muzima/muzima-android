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

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Concept;
import com.muzima.api.model.ConceptName;
import com.muzima.api.model.DerivedObservation;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Provider;
import com.muzima.controller.DerivedConceptController;
import com.muzima.controller.DerivedObservationController;
import com.muzima.controller.EncounterController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.ProviderController;
import com.muzima.model.ConceptIcons;
import com.muzima.utils.DateUtils;
import com.muzima.utils.FontManager;
import com.muzima.utils.StringUtils;

import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.muzima.utils.ConceptUtils;
import com.muzima.utils.Constants;

public class ObsVerticalViewAdapter extends RecyclerView.Adapter<ObsVerticalViewAdapter.ViewHolder> {
    private List<Observation> observationList = new ArrayList<>();
    private final String date;
    final EncounterController encounterController;
    final ObservationController observationController;
    final ProviderController providerController;
    private final String applicationLanguage;
    private final Boolean shouldReplaceProviderIdWithNames;
    private final String patientUuid;
    private final Context context;
    private final List<ConceptIcons> conceptIcons;
    final DerivedObservationController derivedObservationController;
    final DerivedConceptController derivedConceptController;

    public ObsVerticalViewAdapter(String date, MuzimaApplication muzimaApplication, String applicationLanguage,
                                  boolean shouldReplaceProviderIdWithNames, String patientUuid, Context context,
                                  List<ConceptIcons> conceptIcons) {
        this.date = date;
        this.encounterController = muzimaApplication.getEncounterController();
        this.observationController = muzimaApplication.getObservationController();
        this.applicationLanguage = applicationLanguage;
        this.providerController = muzimaApplication.getProviderController();
        this.derivedObservationController = muzimaApplication.getDerivedObservationController();
        this.derivedConceptController = muzimaApplication.getDerivedConceptController();
        this.shouldReplaceProviderIdWithNames = shouldReplaceProviderIdWithNames;
        this.patientUuid = patientUuid;
        observationList = getObservationForDate(date);
        this.context = context;
        this.conceptIcons = conceptIcons;
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.timeline_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Observation observation = observationList.get(position);

        holder.concept.setText(ConceptUtils.getConceptNameFromConceptNamesByLocale(observation.getConcept().getConceptNames(),applicationLanguage));
        holder.conceptIcon.setTypeface(FontManager.getTypeface(context,FontManager.FONTAWESOME));
        String icon = getConceptIcon(observation.getConcept().getUuid());
        holder.conceptIcon.setText(StringUtils.isEmpty(icon) ? "edit" : icon);

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
                holder.observationValue.setText(ConceptUtils.getConceptNameFromConceptNamesByLocale(observation.getValueCoded().getConceptNames(),applicationLanguage));

            if (observation.getConcept().isBoolean())
                holder.observationValue.setText(String.valueOf(observation.isValueBoolean()));

            if (!observation.getConcept().isNumeric() && !observation.getConcept().isDatetime() && !observation.getConcept().isCoded() && !observation.getConcept().isBoolean()){
                if(shouldReplaceProviderIdWithNames && observation.getConcept().getId() == Constants.FGH.Concepts.HEALTHWORKER_ASSIGNMENT_CONCEPT_ID){
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
        private final TextView conceptIcon;

        public ViewHolder(@NonNull View view) {
            super(view);
            View container = view.findViewById(R.id.item_single_obs_container);
            this.observationContainer = view.findViewById(R.id.value_container);
            this.observationValue = view.findViewById(R.id.observation_value);
            this.conceptIcon = view.findViewById(R.id.concept_icon);
            this.observationDate = view.findViewById(R.id.item_single_obs_date_text_view);
            this.observationComplexHolder = view.findViewById(R.id.observation_complex);
            this.concept = view.findViewById(R.id.concept);
        }
    }

    public List<Observation> getObservationForDate(String date){
        observationList = new ArrayList<>();
        List<String> observationUuids = new ArrayList<>();
        try {
            List<Observation> observations = observationController.getObservationsByPatientAndObservationDatetime(patientUuid, DateUtils.parse(date));
            Collections.sort(observations, obsDateTimeComparator);
            observationList.addAll(observations);

            List<DerivedObservation> derivedObservations = derivedObservationController.getDerivedObservationsByPatientUuidAndCreationDate(patientUuid,DateUtils.parse(date));
            Collections.sort(derivedObservations, derivedObsDateTimeComparator);
            for (DerivedObservation derivedObservation : derivedObservations) {
                observationUuids.add(derivedObservation.getUuid());
                Observation observation = new Observation();

                List<ConceptName> conceptNames = new ArrayList<>();
                ConceptName conceptName = new ConceptName();
                conceptName.setName(ConceptUtils.getDerivedConceptNameFromConceptNamesByLocale(derivedObservation.getDerivedConcept().getDerivedConceptName(), applicationLanguage));
                conceptName.setLocale(applicationLanguage);
                conceptNames.add(conceptName);

                Concept concept = new Concept();
                concept.setUuid(derivedObservation.getDerivedConcept().getUuid());
                concept.setConceptNames(conceptNames);
                concept.setConceptType(derivedObservation.getDerivedConcept().getConceptType());

                observation.setUuid(derivedObservation.getUuid());
                observation.setPerson(derivedObservation.getPerson());
                observation.setConcept(concept);
                observation.setValueCoded(derivedObservation.getValueCoded());
                observation.setValueDatetime(derivedObservation.getValueDatetime());
                observation.setValueNumeric(derivedObservation.getValueNumeric());
                observation.setValueText(derivedObservation.getValueText());
                observation.setValueBoolean(derivedObservation.isValueBoolean());
                observation.setObservationDatetime(derivedObservation.getDateCreated());

                observationList.add(observation);
            }
        } catch (ObservationController.LoadObservationException e) {
            Log.e(getClass().getSimpleName(),"Exception encountered while loading Observations ",e);
        } catch (DerivedObservationController.DerivedObservationFetchException e) {
            Log.e(getClass().getSimpleName(),"Exception encountered while loading Derived Observations ",e);
        } catch (ParseException e) {
            Log.e(getClass().getSimpleName(),"Exception encountered while Parsing data ",e);
        }
        return observationList;
    }

    public String getConceptIcon(String conceptUuid){
        String icon = "";
        for(ConceptIcons conceptIcon : conceptIcons){
            if(conceptUuid.equals(conceptIcon.getConceptUuid())){
                icon = conceptIcon.getIcon();
            }
        }
        return icon;
    }

    private final Comparator<Observation> obsDateTimeComparator = (lhs, rhs) -> {
        if (lhs.getObservationDatetime()==null)
            return -1;
        if (rhs.getObservationDatetime()==null)
            return 1;
        return -(lhs.getObservationDatetime()
                .compareTo(rhs.getObservationDatetime()));
    };

    private final Comparator<DerivedObservation> derivedObsDateTimeComparator = (lhs, rhs) -> {
        if (lhs.getDateCreated()==null)
            return -1;
        if (rhs.getDateCreated()==null)
            return 1;
        return -(lhs.getDateCreated()
                .compareTo(rhs.getDateCreated()));
    };
}
