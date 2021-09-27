/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.muzima.R;
import com.muzima.adapters.forms.CompleteFormsWithDataAdapter;
import com.muzima.adapters.forms.FormsAdapter;
import com.muzima.api.model.Concept;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.FormData;
import com.muzima.api.model.Observation;
import com.muzima.controller.FormController;
import com.muzima.controller.ObservationController;
import com.muzima.model.CompleteFormWithPatientData;
import com.muzima.model.FormWithData;
import com.muzima.utils.Constants;
import com.muzima.utils.CustomColor;
import com.muzima.utils.DateUtils;
import com.muzima.utils.StringUtils;
import com.muzima.utils.ThemeUtils;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import static com.muzima.view.patients.PatientSummaryActivity.PATIENT_UUID;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CompleteFormsListFragment extends FormsWithDataListFragment implements FormsAdapter.MuzimaClickListener{
    public static CompleteFormsListFragment newInstance(FormController formController, ObservationController observationController) {
        CompleteFormsListFragment f = new CompleteFormsListFragment();
        f.formController = formController;
        f.observationController = observationController;
        return f;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if(isVisibleToUser && isResumed()){
            logEvent("VIEW_COMPLETED_FORMS");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Store our shared preference
        SharedPreferences sp = getActivity().getSharedPreferences("COMPLETED_FORM_AREA_IN_FOREGROUND", Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean("active", true);
        ed.commit();
        reloadData();
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences sp = getActivity().getSharedPreferences("COMPLETED_FORM_AREA_IN_FOREGROUND", Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean("active", false);
        ed.commit();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        String filterPatientUuid = getActivity().getIntent().getStringExtra(PATIENT_UUID);
        listAdapter = new CompleteFormsWithDataAdapter(getActivity(), R.layout.item_form_with_data_layout, filterPatientUuid, formController, observationController);
        ((CompleteFormsWithDataAdapter)listAdapter).setMuzimaClickListener(this);
        noDataMsg = getActivity().getResources().getString(R.string.info_complete_form_unavailable);
        noDataTip = getActivity().getResources().getString(R.string.hint_complete_form_unavailable);

        if (actionModeActive) {
            actionMode = getActivity().startActionMode(new DeleteFormsActionModeCallback());
            actionMode.setTitle(String.valueOf(((CompleteFormsWithDataAdapter)listAdapter).getSelectedFormsUuids().size()));
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
    }

    @Override
    protected View setupMainView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.layout_list, container, false);
    }

    public void onFormUploadFinish() {
        reloadData();
    }

    @Override
    public void onItemLongClick() {
        int numOfSelectedForms = ((CompleteFormsWithDataAdapter)listAdapter).getSelectedFormsUuids().size();
        if (numOfSelectedForms > 0 && !actionModeActive) {
            actionMode = getActivity().startActionMode(new DeleteFormsActionModeCallback());
            actionModeActive = true;

        }
        if (numOfSelectedForms == 0 && actionModeActive) {
            actionMode.finish();
        }

        if(numOfSelectedForms > 0) {
            actionMode.setTitle(String.valueOf(numOfSelectedForms));
        }
    }

    @Override
    public void onItemClick(int position, View view) {
        if(!actionModeActive) {
            CompleteFormWithPatientData completeFormWithPatientData = (CompleteFormWithPatientData) listAdapter.getItem(position);
            if (completeFormWithPatientData.getDiscriminator() != null) {
                if (!completeFormWithPatientData.getDiscriminator().equals(Constants.FORM_JSON_DISCRIMINATOR_INDIVIDUAL_OBS)
                        && !completeFormWithPatientData.getDiscriminator().equals(Constants.FORM_JSON_DISCRIMINATOR_SHR_REGISTRATION)) {
                    FormViewIntent intent = new FormViewIntent(getActivity(), (CompleteFormWithPatientData) listAdapter.getItem(position));
                    getActivity().startActivityForResult(intent, FormsWithDataActivity.FORM_VIEW_ACTIVITY_RESULT);
                }else if(completeFormWithPatientData.getDiscriminator().equals(Constants.FORM_JSON_DISCRIMINATOR_INDIVIDUAL_OBS)){
                    displayObservationDetailsDialog((CompleteFormWithPatientData) listAdapter.getItem(position), view);
                }
            }
        }
    }

    public void displayObservationDetailsDialog(FormWithData formWithData, View view) {
        AlertDialog obsDetailsViewDialog;
        try {
            FormData formData = formController.getFormDataByUuid(formWithData.getFormDataUuid());
            List<FormData> formDataList = new ArrayList<>();
            formDataList.add(formData);
            String jsonPayload = formData.getJsonPayload();
            org.json.JSONObject responseJSON = new org.json.JSONObject(jsonPayload);
            org.json.JSONObject encounterObject = responseJSON.getJSONObject("encounter");
            String encounterUuid = encounterObject.getString("encounter.encounter_uuid");
            String provider = encounterObject.getString("encounter.provider_id");
            List<Observation> observations = observationController.getObservationByEncounterUuid(encounterUuid);

            Observation observation = observations.get(0);
            Concept concept = observationController.getConceptForObs(observation.getConcept().getUuid());
            observation.setConcept(concept);
            int conceptColor = observationController.getConceptColor(observation.getConcept().getUuid());
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
            Button deleteObsButton;

            TextView encounterDetailsHeader;
            TextView providerDetailsHeader;
            TextView conceptDetailsHeader;
            TextView observationDetailsHeader;
            RelativeLayout providerDetails;

            LayoutInflater layoutInflater = (LayoutInflater) view.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            AlertDialog.Builder addIndividualObservationsDialogBuilder =
                    new androidx.appcompat.app.AlertDialog.Builder(
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
            providerIdentifyType = obsDetailsDialog.findViewById(R.id.provider_identify_type);
            conceptNameTextView = obsDetailsDialog.findViewById(R.id.concept_name_value_textview);
            conceptDescriptionTextView = obsDetailsDialog.findViewById(R.id.concept_description_value_textview);
            obsValueTextView = obsDetailsDialog.findViewById(R.id.observertion_value_indicator_textview);
            encounterDetailsHeader = obsDetailsDialog.findViewById(R.id.obs_details_header);
            providerDetailsHeader = obsDetailsDialog.findViewById(R.id.obs_details_second_header);
            conceptDetailsHeader = obsDetailsDialog.findViewById(R.id.obs_details_third_header);
            observationDetailsHeader = obsDetailsDialog.findViewById(R.id.obs_details_fourth_header);
            dateTextView = obsDetailsDialog.findViewById(R.id.observation_description_value_textview);
            deleteObsButton = obsDetailsDialog.findViewById(R.id.delete_dialog_button);
            deleteObsButton.setVisibility(View.VISIBLE);

            List<TextView> obsDetailsHeaderTextViews = Arrays.asList(encounterDetailsHeader,providerDetailsHeader,conceptDetailsHeader,observationDetailsHeader);
            dateTextView.setText(observation.getObservationDatetime().toString().substring(0,19));

            for (TextView obsDetailsHeaderTextView : obsDetailsHeaderTextViews) {
                obsDetailsHeaderTextView.setBackgroundColor(conceptColor);
            }

            String observationValue = observation.getValueAsString();
            obsValueTextView.setText(observationValue);

            String conceptName = observation.getConcept().getName();
            conceptNameTextView.setText(conceptName);
            String conceptUnits = observation.getConcept().getUnit();

            if (!StringUtils.isEmpty(conceptUnits)) {
                conceptDescriptionTextView.setText("Unit Name: " + conceptUnits);
            }

            //ToDo: Delink Provider from Person, since Provider is not necessarily a Person OpenMRS
            providerIdentifierTextView.setVisibility(View.GONE);
            providerIdentifyType.setVisibility(View.GONE);
            
            providerNameTextView.setText(provider);
            encounterLocationTextView.setText(R.string.individual_obs);
            encounterTypeTextView.setText(R.string.individual_obs);
            encounterDateTextView.setText(encounterDate);
            providerIdentifierTextView.setText(provider);


            dismissDialogButton.setOnClickListener(v -> {
                obsDetailsViewDialog.dismiss();
                obsDetailsViewDialog.cancel();
            });

            deleteObsButton.setOnClickListener( v -> {
                showWarningDialog(formDataList);
                obsDetailsViewDialog.dismiss();
                obsDetailsViewDialog.cancel();
            });

        } catch (FormController.FormDataFetchException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showWarningDialog(List<FormData> formDataList) {
        new android.app.AlertDialog.Builder(getContext())
                .setCancelable(true)
                .setIcon(ThemeUtils.getIconWarning(getContext()))
                .setTitle(getResources().getString(R.string.delete_individual_obs))
                .setMessage(getResources().getString(R.string.warning_individual_form_data_delete))
                .setPositiveButton(getString(R.string.general_yes), DeleteObs(formDataList))
                .setNegativeButton(getString(R.string.general_no), null)
                .create()
                .show();
    }

    private Dialog.OnClickListener DeleteObs(List<FormData> formDataList) {
        return new Dialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    formController.deleteEncounterFormDataAndRelatedPatientData(formDataList);
                } catch (FormController.FormDataDeleteException e) {
                    e.printStackTrace();
                }
                reloadData();
            }
        };
    }
}
