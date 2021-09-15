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
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.muzima.R;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.Observation;
import com.muzima.controller.ConceptController;
import com.muzima.controller.EncounterController;
import com.muzima.controller.ObservationController;
import com.muzima.model.observation.EncounterWithObservations;
import com.muzima.utils.BackgroundTaskHelper;
import com.muzima.utils.DateUtils;
import com.muzima.utils.StringUtils;

import java.util.Arrays;
import java.util.List;

public class ObservationsByEncounterAdapter extends ObservationsAdapter<EncounterWithObservations> {

    private Boolean isSHRData = false;

    public ObservationsByEncounterAdapter(FragmentActivity activity, int item_observation_list,
                                          EncounterController encounterController, ConceptController conceptController,
                                          ObservationController observationController,Boolean isSHRData) {
        super(activity,item_observation_list,encounterController, conceptController,observationController);
        this.isSHRData = isSHRData;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ObservationsByEncounterViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(R.layout.item_observation_by_encounter_list, parent, false);
            holder = new ObservationsByEncounterViewHolder();
            holder.observationLayout = convertView
                    .findViewById(R.id.observation_layout);
            holder.headerLayout = convertView.findViewById(R.id.observation_header);
            holder.encounterProvider = convertView.findViewById(R.id.encounter_provider);
            holder.encounterDate = convertView.findViewById(R.id.encounter_date);
            holder.encounterLocation = convertView.findViewById(R.id.encounter_location);
            convertView.setTag(holder);
        } else {
            holder = (ObservationsByEncounterViewHolder) convertView.getTag();
        }

        holder.renderItem(getItem(position));
        return convertView;
    }

    @Override
    public void reloadData() {
        cancelBackgroundQueryTask();
        AsyncTask<Void,?,?> backgroundQueryTask = new ObservationsByEncounterBackgroundTask(this,
                new EncountersByPatient(encounterController,observationController, patientUuid),isSHRData);
        BackgroundTaskHelper.executeInParallel(backgroundQueryTask);
        setRunningBackgroundQueryTask(backgroundQueryTask);
    }

    public void search(String query) {
        cancelBackgroundQueryTask();
        AsyncTask<Void,?,?> backgroundQueryTask = new ObservationsByEncounterBackgroundTask(this,
                new EncountersBySearch(encounterController,observationController, patientUuid, query),isSHRData);
        BackgroundTaskHelper.executeInParallel(backgroundQueryTask);
        setRunningBackgroundQueryTask(backgroundQueryTask);
    }

    protected class ObservationsByEncounterViewHolder extends ViewHolder {
        TextView encounterProvider;
        TextView encounterDate;
        TextView encounterLocation;
        LinearLayout headerLayout ;

        ObservationsByEncounterViewHolder() {
            super();
        }

        private void renderItem(EncounterWithObservations item) {
            addEncounterObservations(item.getObservations());
            setEncounter(item.getEncounter());
            headerLayout.setBackgroundColor(getContext()
                    .getResources().getColor(R.color.observation_by_encounter_header_background));
        }

        @Override
        protected void setObservation(LinearLayout layout, final Observation observation) {
            TextView conceptInfo = layout.findViewById(R.id.concept_info);
            conceptInfo.setText(getConceptDisplay(observation.getConcept()));
            int conceptColor = observationController.getConceptColor(observation.getConcept().getUuid());
            conceptInfo.setTextColor(conceptColor);

            View divider = layout.findViewById(R.id.divider1);
            divider.setBackgroundColor(conceptColor);

            String observationConceptType = observation.getConcept().getConceptType().getName();

            TextView observationValue = layout.findViewById(R.id.observation_value);
            ImageView observationComplexHolder = layout.findViewById(R.id.observation_complex);
            if (StringUtils.equals(observationConceptType, "Complex")){
                observationValue.setVisibility(View.GONE);
                observationComplexHolder.setVisibility(View.VISIBLE);
            } else {
                observationValue.setVisibility(View.VISIBLE);
                observationComplexHolder.setVisibility(View.GONE);
                observationValue.setText(observation.getValueAsString());
                observationValue.setTextColor(conceptColor);
            }

            View divider2 = layout.findViewById(R.id.divider2);
            divider2.setBackgroundColor(conceptColor);

            TextView observationDateView = layout.findViewById(R.id.observation_date);
            observationDateView.setText(DateUtils.getMonthNameFormattedDate(observation.getObservationDatetime()));
            observationDateView.setTextColor(conceptColor);

            layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    displayObservationDetailsDialog(observation, v);
                }
            });

        }

        void setEncounter(Encounter encounter) {
            encounterProvider.setText(encounter.getProvider().getDisplayName());
            String date = "";
            boolean isEncounterForObservationWithNonNullEncounterUuid =
                    !isEncounterForObservationWithNullEncounterUuid(encounter);
            if(isEncounterForObservationWithNonNullEncounterUuid)
                date = DateUtils.getMonthNameFormattedDate(encounter.getEncounterDatetime());
            encounterDate.setText(date);
            encounterLocation.setText(encounter.getLocation().getName());
        }

        private boolean isEncounterForObservationWithNullEncounterUuid(Encounter encounter) {
            return encounter.getProvider().getFamilyName() == null && encounter.getProvider().getMiddleName() == null
                    && encounter.getProvider().getGivenName() == null;
        }

        @Override
        protected int getObservationLayout() {
            return R.layout.item_observation_by_encounter;
        }
        @Override
        protected int getObservationElementHeight() {
            return R.dimen.observation_element_by_encounter_height;
        }
    }

    public void displayObservationDetailsDialog(Observation observation, View view) {
        int conceptColor = observationController.getConceptColor(observation.getConcept().getUuid());
        String observationConceptType = observation.getConcept().getConceptType().getName();
        String encounterDate = DateUtils.getMonthNameFormattedDate(observation.getObservationDatetime());

        TextView encounterDateTextView;
        TextView encounterLocationTextView;
        TextView encounterTypeTextView;
        TextView providerNameTextView;
        TextView providerIdentifierTextView;
        TextView providerIdentifyType;
        TextView conceptNameTextView;
        TextView conceptDescriptionTextView;
        TextView obsValueTextView;
        TextView dateTextView;
        Button dismissDialogButton;

        TextView encounterDetailsHeader;
        TextView providerDetailsHeader;
        TextView conceptDetailsHeader;
        TextView observationDetailsHeader;
        RelativeLayout providerDetails;
        LayoutInflater layoutInflater;
        View obsDetailsDialog;
        final AlertDialog obsDetailsViewDialog;

        layoutInflater = (LayoutInflater) view.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        AlertDialog.Builder addIndividualObservationsDialogBuilder =
                new AlertDialog.Builder(
                        view.getContext()
                );
        obsDetailsDialog = layoutInflater.inflate(R.layout.obs_details_dialog_layout, null);

        addIndividualObservationsDialogBuilder.setView(obsDetailsDialog);
        addIndividualObservationsDialogBuilder.setCancelable(true);

        obsDetailsViewDialog = addIndividualObservationsDialogBuilder.create();
        obsDetailsViewDialog.show();
        dismissDialogButton = obsDetailsDialog.findViewById(R.id.dismiss_dialog_button);
        encounterDateTextView = obsDetailsDialog.findViewById(R.id.encounter_date_value_textview);
        encounterLocationTextView = obsDetailsDialog.findViewById(R.id.encounter_location_value_textview);
        encounterTypeTextView = obsDetailsDialog.findViewById(R.id.encounter_type_value_textview);
        providerNameTextView = obsDetailsDialog.findViewById(R.id.provider_name_value_textview);
        providerIdentifierTextView = obsDetailsDialog.findViewById(R.id.provider_identify_type_value_textView);
        providerIdentifyType = obsDetailsDialog.findViewById(R.id.provider_identify_type);
        conceptNameTextView = obsDetailsDialog.findViewById(R.id.concept_name_value_textview);
        conceptDescriptionTextView = obsDetailsDialog.findViewById(R.id.concept_description_value_textview);
        obsValueTextView = obsDetailsDialog.findViewById(R.id.observertion_value_indicator_textview);
        encounterDetailsHeader = obsDetailsDialog.findViewById(R.id.obs_details_header);
        providerDetailsHeader = obsDetailsDialog.findViewById(R.id.obs_details_second_header);
        conceptDetailsHeader = obsDetailsDialog.findViewById(R.id.obs_details_third_header);
        observationDetailsHeader = obsDetailsDialog.findViewById(R.id.obs_details_fourth_header);
        dateTextView = obsDetailsDialog.findViewById(R.id.observation_description_value_textview);
        providerDetails = obsDetailsDialog.findViewById(R.id.provider_details);

        List<TextView> obsDetailsHeaderTextViews = Arrays.asList(encounterDetailsHeader,providerDetailsHeader,conceptDetailsHeader,observationDetailsHeader);
        dateTextView.setText(observation.getObservationDatetime().toString().substring(0,19));

        for (TextView obsDetailsHeaderTextView : obsDetailsHeaderTextViews) {
            obsDetailsHeaderTextView.setBackgroundColor(conceptColor);
        }

        if (StringUtils.equals(observationConceptType, "Complex")) {
            obsValueTextView.setText("Complex Obs");
        } else {
            String observationValue = observation.getValueAsString();
            obsValueTextView.setText(observationValue);
        }

        conceptNameTextView.setText(observation.getConcept().getName());
        String conceptDescription = observation.getConcept().getUnit();

        if (conceptDescription != null || conceptDescription != "") {
            conceptDescriptionTextView.setText("Unit Name: " + conceptDescription);
        }

        Encounter encounter = observation.getEncounter();
        if (encounter != null) {
            providerIdentifierTextView.setVisibility(View.GONE);
            providerIdentifyType.setVisibility(View.GONE);
            if(encounter.getProvider() != null) {
                providerNameTextView.setText(encounter.getProvider().getDisplayName());
            }else{
                providerNameTextView.setVisibility(View.GONE);
                providerDetails.setVisibility(View.GONE);
                providerDetailsHeader.setVisibility(View.GONE);
            }

            if (encounter.getLocation() != null) {
                String encounterLocation = encounter.getLocation().getName();
                encounterLocationTextView.setText(encounterLocation);
            } else {
                encounterLocationTextView.setText(R.string.general_not_available_text);
            }

            if (encounter.getEncounterType() != null){
                String encounterType = encounter.getEncounterType().getName();
                encounterTypeTextView.setText(encounterType);
            } else {
                encounterTypeTextView.setText(R.string.general_not_available_text);
            }

            encounterDateTextView.setText(encounterDate);
        } else {
            providerNameTextView.setText(R.string.general_not_available_text);
            encounterLocationTextView.setText(R.string.general_not_available_text);
            encounterTypeTextView.setText(R.string.general_not_available_text);
            encounterDateTextView.setText(R.string.general_not_available_text);
            providerIdentifierTextView.setText(R.string.general_not_available_text);
        }

        dismissDialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                obsDetailsViewDialog.dismiss();
                obsDetailsViewDialog.cancel();
            }
        });
    }
}
