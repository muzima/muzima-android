package com.muzima.adapters.observations;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.R;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.Observation;
import com.muzima.controller.EncounterController;
import com.muzima.controller.ObservationController;
import com.muzima.utils.DateUtils;

import com.muzima.utils.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ObsHorizontalViewAdapter extends RecyclerView.Adapter<ObsHorizontalViewAdapter.ViewHolder> {
    private List<Observation> observationList;
    private ObservationClickedListener observationClickedListener;
    private AlertDialog obsDetailsViewDialog;
    private LayoutInflater layoutInflater;
    private View obsDetailsDialog;
    final EncounterController encounterController;
    final ObservationController observationController;

    public ObsHorizontalViewAdapter(List<Observation> observationList, ObservationClickedListener observationClickedListener,
                                    EncounterController encounterController, ObservationController observationController) {
        this.observationList = observationList;
        this.observationClickedListener = observationClickedListener;
        this.encounterController = encounterController;
        this.observationController = observationController;
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_single_obs_view_layout, parent, false), observationClickedListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Observation observation = observationList.get(position);
        if (observation.getConcept().isNumeric()) {
            holder.valueTextView.setText(String.valueOf(observation.getValueNumeric()));
        }

        if (observation.getConcept().isDatetime()) {
            holder.valueTextView.setText(DateUtils.convertDateToStdString(observation.getValueDatetime()));
        }

        if (!observation.getConcept().isNumeric() && !observation.getConcept().isDatetime() && !observation.getConcept().isCoded()) {
            holder.valueTextView.setText(observation.getValueText());
        }
        holder.dateTextView.setText(DateUtils.convertDateToHumanReadableString(observation.getObservationDatetime()));
    }

    @Override
    public int getItemCount() {
        return observationList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView valueTextView;
        private final TextView dateTextView;
        private final ObservationClickedListener observationClickedListener;

        public ViewHolder(@NonNull View view, ObservationClickedListener clickedListener) {
            super(view);
            View container = view.findViewById(R.id.item_single_obs_container);
            this.valueTextView = view.findViewById(R.id.item_single_obs_value_text_view);
            this.dateTextView = view.findViewById(R.id.item_single_obs_date_text_view);
            this.observationClickedListener = clickedListener;
            container.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Observation obs = observationList.get(getAdapterPosition());
            displayObservationDetailsDialog(obs, v);

            this.observationClickedListener.onObservationClicked(getAdapterPosition());
        }
    }

    public interface ObservationClickedListener {
        void onObservationClicked(int position);
    }

    public void displayObservationDetailsDialog(Observation observation, View view) {
        int conceptColor = observationController.getConceptColor(observation.getConcept().getUuid());
        String observationConceptType = observation.getConcept().getConceptType().getName();
        String encounterDate = DateUtils.getMonthNameFormattedDate(observation.getObservationDatetime());

        /*
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
        AlertDialog.Builder addIndividualObservationsDialogBuilder =
                new androidx.appcompat.app.AlertDialog.Builder(
                        view.getContext()
                );
        obsDetailsDialog = layoutInflater.inflate(R.layout.obs_details_dialog_layout, null);

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
        Encounter enc;
        try {
            enc = encounterController.getEncounterByUuid(encounter.getUuid());
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
