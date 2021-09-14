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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
import com.muzima.api.model.Patient;
import com.muzima.controller.ConceptController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.ProviderController;
import com.muzima.model.observation.ConceptWithObservations;
import com.muzima.utils.BackgroundTaskHelper;
import com.muzima.utils.Constants;
import com.muzima.utils.DateUtils;
import com.muzima.utils.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


public class ObservationsByConceptAdapter extends ObservationsAdapter<ConceptWithObservations> {

    private LayoutInflater layoutInflater;
    private View addNewObservationValuesDialog;
    private View obsDetailsDialog;
    private androidx.appcompat.app.AlertDialog addIndividualObsDialog;
    private androidx.appcompat.app.AlertDialog obsDetailsViewDialog;
    private HashMap<Integer, Concept> rederedConceptsVisualizationMap = new HashMap<>(); //enable visualization of what is rendered on the UI. for ease of access.
    private EditText obsDialogEditText;
    private Button obsDialogAddButton;
    private TextView headerText;
    private Boolean isShrData;
    private List<Integer> shrConcepts;
    private MuzimaApplication muzimaApplication;
    private ProviderController providerController;
    private Patient patient;

    public ObservationsByConceptAdapter(FragmentActivity activity, int itemCohortsList,
                                        ConceptController conceptController,
                                        ObservationController observationController, Boolean isShrData, Patient patient) {
        super(activity, itemCohortsList, null, conceptController, observationController);
        this.isShrData = isShrData;
        loadComposedShrConceptId();
        this.muzimaApplication = (MuzimaApplication) getContext().getApplicationContext();
        this.providerController = muzimaApplication.getProviderController();
        this.patient = patient;
    }

    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
        /**
         * Prepare add obs dialog
         */
        layoutInflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        androidx.appcompat.app.AlertDialog.Builder addIndividualObservationsDialogBuilder =
                new androidx.appcompat.app.AlertDialog.Builder(
                        parent.getContext()
                );

        ObservationsByConceptViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(R.layout.item_observation_by_concept_list, parent, false);
            holder = new ObservationsByConceptViewHolder();
            holder.headerText = (TextView) convertView.findViewById(R.id.observation_header);
            holder.addObsButton = (ImageButton) convertView.findViewById(R.id.add_individual_obs_imagebutton);
            holder.addObsButton.setVisibility(View.VISIBLE);
            holder.headerLayout = (RelativeLayout) convertView.findViewById(R.id.observation_header_layout);
            holder.observationLayout = (LinearLayout) convertView
                    .findViewById(R.id.observation_layout);
            convertView.setTag(holder);


            holder.addObsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });

        } else {
            holder = (ObservationsByConceptViewHolder) convertView.getTag();
        }

        holder.renderItem(getItem(position));
        /**
         * Update Concepts Map
         */
        Concept conceptAtThisPosition = getItem(position).getConcept();
        rederedConceptsVisualizationMap.put(position, conceptAtThisPosition);

        return convertView;
    }

    @Override
    public void reloadData() {
        cancelBackgroundQueryTask();
        AsyncTask<Void, ?, ?> backgroundQueryTask = new ObservationsByConceptBackgroundTask(this,
                new ConceptsByPatient(conceptController, observationController, patientUuid), isShrData);
        BackgroundTaskHelper.executeInParallel(backgroundQueryTask);
        setRunningBackgroundQueryTask(backgroundQueryTask);
    }

    public void search(String term) {
        cancelBackgroundQueryTask();
        AsyncTask<Void, ?, ?> backgroundQueryTask = new ObservationsByConceptBackgroundTask(this,
                new ConceptsBySearch(conceptController, observationController, patientUuid, term), isShrData);
        BackgroundTaskHelper.executeInParallel(backgroundQueryTask);
        setRunningBackgroundQueryTask(backgroundQueryTask);
    }


    protected class ObservationsByConceptViewHolder extends ViewHolder {
        ImageButton addObsButton;
        RelativeLayout headerLayout;
        TextView headerText;

        public ObservationsByConceptViewHolder() {
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
            boolean isConceptCoded = observation.getConcept().isCoded();

            TextView observationValue = (TextView) layout.findViewById(R.id.observation_value);
            ImageView shrEnabledImage = (ImageView) layout.findViewById(R.id.shr_card_obs_image_view);

            if (isShrData) {
                shrEnabledImage.setVisibility(View.VISIBLE);
            } else {
                shrEnabledImage.setVisibility(View.INVISIBLE);
            }

            if (shrConcepts.contains(observation.getConcept().getId())) {
                shrEnabledImage.setVisibility(View.VISIBLE);
            }

            ImageView observationComplexHolder = (ImageView) layout.findViewById(R.id.observation_complex);
            if (StringUtils.equals(observationConceptType, "Complex")) {
                observationValue.setVisibility(View.GONE);
                observationComplexHolder.setVisibility(View.VISIBLE);
            } else {
                observationValue.setVisibility(View.VISIBLE);
                observationComplexHolder.setVisibility(View.GONE);
                observationValue.setText(observation.getValueAsString());
                observationValue.setTextColor(conceptColor);
            }

            //Disabling Individual obs for coded concepts until MUZIMA-620 is worked on
            //ToDo: Fix MUZIMA-620
            if(isConceptCoded){
                addObsButton.setVisibility(View.GONE);
            }else if(StringUtils.equals(observationConceptType, "Complex")){
                addObsButton.setVisibility(View.GONE);
            }else{
                addObsButton.setVisibility(View.VISIBLE);
            }

            View divider = layout.findViewById(R.id.divider);
            divider.setBackgroundColor(conceptColor);
            divider.setFocusable(true);
            divider.setClickable(true);

            TextView observationDateView = (TextView) layout.findViewById(R.id.observation_date);
            observationDateView.setText(DateUtils.getMonthNameFormattedDate(observation.getObservationDatetime()));
            observationDateView.setTextColor(conceptColor);

            layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    displayObservationDetailsDialog(observation, v);
                }
            });

            addObsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Concept concept = observation.getConcept();
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

    private void loadComposedShrConceptId() {
        List<Integer> conceptIds = new ArrayList<>();
        conceptIds.add(Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_RESULT.concept_id);
        conceptIds.add(Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_TYPE.concept_id);
        conceptIds.add(Constants.Shr.KenyaEmr.CONCEPTS.HIV_TESTS.TEST_STRATEGY.concept_id);
        conceptIds.add(Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.VACCINE.concept_id);
        conceptIds.add(Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.SEQUENCE.concept_id);
        conceptIds.add(Constants.Shr.KenyaEmr.CONCEPTS.IMMUNIZATION.GROUP.concept_id);

        shrConcepts = conceptIds;
    }

    public void displayObservationDetailsDialog(Observation observation, View view) {

        int conceptColor = observationController.getConceptColor(observation.getConcept().getUuid());
        String observationConceptType = observation.getConcept().getConceptType().getName();
        String encounterDate = DateUtils.getMonthNameFormattedDate(observation.getObservationDatetime());

        /**
         * Prepare add obs dialog
         */
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

        layoutInflater = (LayoutInflater) view.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        androidx.appcompat.app.AlertDialog.Builder addIndividualObservationsDialogBuilder =
                new androidx.appcompat.app.AlertDialog.Builder(
                        view.getContext()
                );
        obsDetailsDialog = layoutInflater.inflate(R.layout.obs_details_dialog_layout, null);

        addIndividualObservationsDialogBuilder.setView(obsDetailsDialog);
        addIndividualObservationsDialogBuilder
                .setCancelable(true);

        obsDetailsViewDialog = addIndividualObservationsDialogBuilder.create();
        obsDetailsViewDialog.show();

        dismissDialogButton = (Button) obsDetailsDialog.findViewById(R.id.dismiss_dialog_button);
        encounterDateTextView = (TextView) obsDetailsDialog.findViewById(R.id.encounter_date_value_textview);
        encounterLocationTextView = (TextView) obsDetailsDialog.findViewById(R.id.encounter_location_value_textview);
        encounterTypeTextView = (TextView) obsDetailsDialog.findViewById(R.id.encounter_type_value_textview);
        providerNameTextView = (TextView) obsDetailsDialog.findViewById(R.id.provider_name_value_textview);
        providerIdentifierTextView = (TextView) obsDetailsDialog.findViewById(R.id.provider_identify_type_value_textView);
        providerIdentifyType = (TextView) obsDetailsDialog.findViewById(R.id.provider_identify_type);
        conceptNameTextView = (TextView) obsDetailsDialog.findViewById(R.id.concept_name_value_textview);
        conceptDescriptionTextView = (TextView) obsDetailsDialog.findViewById(R.id.concept_description_value_textview);
        obsValueTextView = (TextView)obsDetailsDialog.findViewById(R.id.observertion_value_indicator_textview);
        encounterDetailsHeader = (TextView) obsDetailsDialog.findViewById(R.id.obs_details_header);
        providerDetailsHeader = (TextView) obsDetailsDialog.findViewById(R.id.obs_details_second_header);
        conceptDetailsHeader = (TextView) obsDetailsDialog.findViewById(R.id.obs_details_third_header);
        observationDetailsHeader = (TextView) obsDetailsDialog.findViewById(R.id.obs_details_fourth_header);
        dateTextView = (TextView)obsDetailsDialog.findViewById(R.id.observation_description_value_textview);
        providerDetails = (RelativeLayout) obsDetailsDialog.findViewById(R.id.provider_details);

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
        Encounter enc;
        try {
            enc = muzimaApplication.getEncounterController().getEncounterByUuid(encounter.getUuid());
            //ToDo: Delink Provider from Person, since Provider is not necessarily a Person OpenMRS
            providerIdentifierTextView.setVisibility(View.GONE);
            providerIdentifyType.setVisibility(View.GONE);

            if (enc != null) {
                if(enc.getProvider() != null) {
                    providerNameTextView.setText(enc.getProvider().getDisplayName());
                }else{
                    providerNameTextView.setVisibility(View.GONE);
                    providerDetails.setVisibility(View.GONE);
                    providerDetailsHeader.setVisibility(View.GONE);
                }

                if (enc.getLocation() != null) {
                    String encounterLocation = enc.getLocation().getName();
                    encounterLocationTextView.setText(encounterLocation);
                } else {
                    encounterLocationTextView.setText(R.string.general_not_available_text);
                }

                if (enc.getEncounterType() != null){
                    String encounterType = enc.getEncounterType().getName();
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

        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), "An IOException was encountered while fetching encounter ",e);
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
