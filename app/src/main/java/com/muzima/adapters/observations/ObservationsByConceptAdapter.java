/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.adapters.observations;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Concept;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.Observation;
import com.muzima.api.model.Person;
import com.muzima.api.model.PersonAttribute;
import com.muzima.api.model.Provider;
import com.muzima.controller.ConceptController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.ProviderController;
import com.muzima.model.observation.ConceptWithObservations;
import com.muzima.utils.BackgroundTaskHelper;
import com.muzima.utils.Constants;
import com.muzima.utils.DateUtils;
import com.muzima.utils.Fonts;
import com.muzima.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ObservationsByConceptAdapter extends ObservationsAdapter<ConceptWithObservations> {

    private static final String TAG = "ObservationsByConceptAdapter";
    private LayoutInflater layoutInflater;
    private android.support.v7.app.AlertDialog addIndividualObsDialog;
    private android.support.v7.app.AlertDialog obsDetailsViewDialog;
    private TextView headerText;
    private final Boolean isSHRData;
    private List<Integer> SHRConcepts;
    private final ProviderController providerController;

    public ObservationsByConceptAdapter(FragmentActivity activity, int itemCohortsList,
                                        ConceptController conceptController,
                                        ObservationController observationController, Boolean isSHRData) {
        super(activity, itemCohortsList, null, conceptController, observationController);
        this.isSHRData = isSHRData;
        loadComposedSHRConceptId();
        MuzimaApplication muzimaApplication = (MuzimaApplication) getContext().getApplicationContext();
        this.providerController = muzimaApplication.getProviderController();

    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
        layoutInflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        android.support.v7.app.AlertDialog.Builder addIndividualObservationsDialogBuilder =
                new android.support.v7.app.AlertDialog.Builder(
                        parent.getContext()
                );
        View addNewObservationValuesDialog = layoutInflater.inflate(R.layout.add_individual_obs_dialog_layout, null);

        addIndividualObservationsDialogBuilder.setView(addNewObservationValuesDialog);
        addIndividualObservationsDialogBuilder
                .setCancelable(true);

        addIndividualObsDialog = addIndividualObservationsDialogBuilder.create();

        EditText obsDialogEditText = addNewObservationValuesDialog.findViewById(R.id.obs_new_value_edittext);
        Button obsDialogAddButton = addNewObservationValuesDialog.findViewById(R.id.add_new_obs_button);

        obsDialogAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addIndividualObsDialog.cancel();
            }
        });

        ObservationsByConceptViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(R.layout.item_observation_by_concept_list, parent, false);
            holder = new ObservationsByConceptViewHolder();
            holder.headerText = convertView.findViewById(R.id.observation_header);
            holder.addObsButton = convertView.findViewById(R.id.add_individual_obs_imagebutton);
            //Disabling Add Obs Button until MUZIMA-615 is fixed
            //ToDo: Fix MUZIMA-615
            holder.addObsButton.setVisibility(View.GONE);
            holder.headerLayout = convertView.findViewById(R.id.observation_header_layout);
            holder.observationLayout = convertView
                    .findViewById(R.id.observation_layout);
            convertView.setTag(holder);

            holder.addObsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addObservation();
                }
            });

        } else {
            holder = (ObservationsByConceptViewHolder) convertView.getTag();
        }

        holder.renderItem(getItem(position));
        Concept conceptAtThisPosition = getItem(position).getConcept();
       // rederedConceptsVisualizationMap.put(position, conceptAtThisPosition);
        return convertView;
    }


    private void addObservation() {
        addIndividualObsDialog.show();
        //TODO Develop add obs logic.
    }


    @Override
    public void reloadData() {
        cancelBackgroundQueryTask();
        AsyncTask<Void, ?, ?> backgroundQueryTask = new ObservationsByConceptBackgroundTask(this,
                new ConceptsByPatient(conceptController, observationController, patientUuid), isSHRData);
        BackgroundTaskHelper.executeInParallel(backgroundQueryTask);
        setRunningBackgroundQueryTask(backgroundQueryTask);
    }

    public void search(String term) {
        cancelBackgroundQueryTask();
        AsyncTask<Void, ?, ?> backgroundQueryTask = new ObservationsByConceptBackgroundTask(this,
                new ConceptsBySearch(conceptController, observationController, patientUuid, term), isSHRData);
        BackgroundTaskHelper.executeInParallel(backgroundQueryTask);
        setRunningBackgroundQueryTask(backgroundQueryTask);
    }


    protected class ObservationsByConceptViewHolder extends ViewHolder {
        ImageButton addObsButton;
        RelativeLayout headerLayout;
        TextView headerText;

        ObservationsByConceptViewHolder() {
            super();
        }

        private void renderItem(ConceptWithObservations item) { //obs display outer loop
            int conceptColor = observationController.getConceptColor(item.getConcept().getUuid());
            headerLayout.setBackgroundColor(conceptColor);
            addObsButton.setBackgroundColor(conceptColor);
            addEncounterObservations(item.getObservations());
            headerText.setText(getConceptDisplay(item.getConcept()));
        }

        @Override
        protected void setObservation(LinearLayout layout, final Observation observation) {
            int conceptColor = observationController.getConceptColor(observation.getConcept().getUuid());

            String observationConceptType = observation.getConcept().getConceptType().getName();

            TextView observationValue = layout.findViewById(R.id.observation_value);
            ImageView SHREnabledImage = layout.findViewById(R.id.SHR_card_obs_image_view);

            if (isSHRData) {
                SHREnabledImage.setVisibility(View.VISIBLE);
            } else {
                SHREnabledImage.setVisibility(View.INVISIBLE);
            }

            if (SHRConcepts.contains(observation.getConcept().getId())) {
                SHREnabledImage.setVisibility(View.VISIBLE);
            }

            ImageView observationComplexHolder = layout.findViewById(R.id.observation_complex);
            if (StringUtils.equals(observationConceptType, "Complex")) {
                observationValue.setVisibility(View.GONE);
                observationComplexHolder.setVisibility(View.VISIBLE);
            } else {
                observationValue.setVisibility(View.VISIBLE);
                observationComplexHolder.setVisibility(View.GONE);
                observationValue.setText(observation.getValueAsString());
                observationValue.setTypeface(Fonts.roboto_medium(getContext()));
                observationValue.setTextColor(conceptColor);
            }

            View divider = layout.findViewById(R.id.divider);
            divider.setBackgroundColor(conceptColor);
            divider.setFocusable(true);
            divider.setClickable(true);

            TextView observationDateView = layout.findViewById(R.id.observation_date);
            observationDateView.setText(DateUtils.getMonthNameFormattedDate(observation.getObservationDatetime()));
            observationDateView.setTypeface(Fonts.roboto_light(getContext()));
            observationDateView.setTextColor(conceptColor);

            layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    displayObservationDetailsDialog(observation, v);
                }
            });

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

    private void loadComposedSHRConceptId() {
        List<Integer> conceptIds = new ArrayList<>();
        conceptIds.add(Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_RESULT.concept_id);
        conceptIds.add(Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_TYPE.concept_id);
        conceptIds.add(Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.concept_id);
        conceptIds.add(Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.concept_id);
        conceptIds.add(Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.SEQUENCE.concept_id);
        conceptIds.add(Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.GROUP.concept_id);

        SHRConcepts = conceptIds;
    }

    private void displayObservationDetailsDialog(Observation observation, View view) {

        int conceptColor = observationController.getConceptColor(observation.getConcept().getUuid());
        String observationConceptType = observation.getConcept().getConceptType().getName();
        String encounterDate = DateUtils.getMonthNameFormattedDate(observation.getObservationDatetime());


        TextView encounterDateTextView;
        TextView encounterLocationTextView;
        TextView encounterTypeTextView;
        TextView providerNameTextView;
        TextView providerIdentifierTextView;
        TextView conceptNameTextView;
        TextView conceptDescriptionTextView;
        TextView obsValueTextView;
        TextView dateTextView;
        Button dismissDialogButton;

        TextView encounterDetailsHeader;
        TextView providerDetailsHeader;
        TextView conceptDetailsHeader;
        TextView observationDetailsHeader;


        layoutInflater = (LayoutInflater) view.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        android.support.v7.app.AlertDialog.Builder addIndividualObservationsDialogBuilder =
                new android.support.v7.app.AlertDialog.Builder(
                        view.getContext()
                );
        View obsDetailsDialog = layoutInflater.inflate(R.layout.obs_details_dialog_layout, null);

        addIndividualObservationsDialogBuilder.setView(obsDetailsDialog);
        addIndividualObservationsDialogBuilder
                .setCancelable(true);

        obsDetailsViewDialog = addIndividualObservationsDialogBuilder.create();
        obsDetailsViewDialog.show();


        dismissDialogButton = obsDetailsDialog.findViewById(R.id.dismiss_dialog_button);
        encounterDateTextView = obsDetailsDialog.findViewById(R.id.encounter_date_value_textview);
        encounterLocationTextView = obsDetailsDialog.findViewById(R.id.encounter_location_value_textview);
        encounterTypeTextView = obsDetailsDialog.findViewById(R.id.encounter_type_value_textview);
        providerNameTextView = obsDetailsDialog.findViewById(R.id.provider_name_value_textview);
        providerIdentifierTextView = obsDetailsDialog.findViewById(R.id.provider_identify_type_value_textView);
        conceptNameTextView = obsDetailsDialog.findViewById(R.id.concept_name_value_textview);
        conceptDescriptionTextView = obsDetailsDialog.findViewById(R.id.concept_description_value_textview);
        obsValueTextView = obsDetailsDialog.findViewById(R.id.observertion_value_indicator_textview);
        encounterDetailsHeader = obsDetailsDialog.findViewById(R.id.obs_details_header);
        providerDetailsHeader = obsDetailsDialog.findViewById(R.id.obs_details_second_header);
        conceptDetailsHeader = obsDetailsDialog.findViewById(R.id.obs_details_third_header);
        observationDetailsHeader = obsDetailsDialog.findViewById(R.id.obs_details_fourth_header);
        dateTextView = obsDetailsDialog.findViewById(R.id.observation_description_value_textview);

        List<TextView> obsDetailsHeaderTextViews = Arrays.asList(encounterDetailsHeader,providerDetailsHeader,conceptDetailsHeader,observationDetailsHeader);
        dateTextView.setText(observation.getObservationDatetime().toString().substring(0,19));

        for (TextView obsDetailsHeaderTextView : obsDetailsHeaderTextViews) {
            obsDetailsHeaderTextView.setBackgroundColor(conceptColor);
        }

        if (StringUtils.equals(observationConceptType, "Complex")) {
            obsValueTextView.setText(R.string.complex_obs);
        } else {
            String observationValue = observation.getValueAsString();
            obsValueTextView.setText(observationValue);
        }

        conceptNameTextView.setText(observation.getConcept().getName());
        String conceptDescription = observation.getConcept().getUnit();

        if (!conceptDescription.isEmpty()) {
            conceptDescriptionTextView.setText(String.format("Unit Name: %s", conceptDescription));
        }

        Encounter encounter = observation.getEncounter();
        try {
           //ToDo: Get Encounter provider instead of first provider from local repo
            //ToDo: Delink Provider from Person, since Provider is not necessarily a Person OpenMRS

            Provider provider = providerController.getAllProviders().get(0);
            providerNameTextView.setText(provider.getName());
            providerIdentifierTextView.setText(provider.getIdentifier());

        } catch (ProviderController.ProviderLoadException e) {
            Log.e("LOG",e.getMessage());
        }

        if (encounter != null) {
            Person provider = encounter.getProvider();

            if (provider != null) {
                String providerName = encounter.getProvider().getDisplayName();

                PersonAttribute providerIdentifier = encounter.getProvider().getAtributes().get(0);
                String attributeType = providerIdentifier.getAttributeType().getName();
                String attributeValue = providerIdentifier.getAttribute();
                providerIdentifierTextView.setText(attributeValue);
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
