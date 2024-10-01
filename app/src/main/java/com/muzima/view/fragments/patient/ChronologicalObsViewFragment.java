/*
 * Copyright (c) Vanderbilt University Medical Center and Lambda Informatics.
 * All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.fragments.patient;

import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.RecyclerAdapter;
import com.muzima.adapters.observations.ObservationByDateAdapter;
import com.muzima.api.model.Concept;
import com.muzima.controller.ConceptController;
import com.muzima.utils.StringUtils;
import com.muzima.view.custom.MuzimaRecyclerView;

import java.util.List;

public class ChronologicalObsViewFragment extends Fragment implements RecyclerAdapter.BackgroundListQueryTaskListener {
    private String patientUuid;
    private ObservationByDateAdapter observationByDateAdapter;
    MuzimaRecyclerView conceptsListRecyclerView;
    LinearLayout noDataLayout;
    boolean isPatientSummaryListing;

    public ChronologicalObsViewFragment() {

    }

    public static ChronologicalObsViewFragment newInstance(String patientUuid,  boolean isPatientSummaryListing) {
        ChronologicalObsViewFragment f = new ChronologicalObsViewFragment();
        f.isPatientSummaryListing = isPatientSummaryListing;
        f.patientUuid = patientUuid;
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        noDataLayout = view.findViewById(R.id.no_data_layout);
        conceptsListRecyclerView = view.findViewById(R.id.recycler_list);
        conceptsListRecyclerView.setLayoutManager(new LinearLayoutManager(requireActivity().getApplicationContext(), LinearLayoutManager.VERTICAL, false));

        observationByDateAdapter = new ObservationByDateAdapter(requireActivity().getApplicationContext(), patientUuid);
        observationByDateAdapter.setBackgroundListQueryTaskListener(this);
        conceptsListRecyclerView.setLayoutManager(new LinearLayoutManager(requireActivity().getApplicationContext()));
        conceptsListRecyclerView.setAdapter(observationByDateAdapter);
        observationByDateAdapter.reloadData();

        if(isPatientSummaryListing) {
            MuzimaApplication muzimaApplication = (MuzimaApplication) requireActivity().getApplicationContext();
            ImageView noDataImage = view.findViewById(R.id.no_data_image);
            TextView textView = view.findViewById(R.id.no_data_msg);
            Typeface typeface = ResourcesCompat.getFont(muzimaApplication, R.font.roboto_light);
            textView.setTypeface(typeface);
            noDataImage.setVisibility(View.GONE);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }


    @Override
    public void onQueryTaskStarted() {
        conceptsListRecyclerView.setNoDataLayout(noDataLayout,
                getString(R.string.info_observation_load),
                StringUtils.EMPTY);
    }

    @Override
    public void onQueryTaskFinish() {
        String noDataTip = StringUtils.EMPTY;
        if(getActivity() != null) {
            MuzimaApplication muzimaApplication = (MuzimaApplication) requireActivity().getApplicationContext();
            try {
                List<Concept> concepts;
                concepts = muzimaApplication.getConceptController().getActiveConfigConcepts();
                if (concepts.size() > 0) {
                    noDataTip = getString(R.string.info_no_observation_for_concept_data_tip);
                } else {
                    noDataTip = getString(R.string.info_no_observation_and_concept_data_tip);
                }
            } catch (ConceptController.ConceptFetchException e) {
                Log.e(getClass().getSimpleName(), "Exception while fetching concepts ", e);
            }

            conceptsListRecyclerView.setNoDataLayout(noDataLayout,
                    getString(R.string.info_observation_unavailable),
                    noDataTip);
        }
    }

    @Override
    public void onQueryTaskCancelled() {
        observationByDateAdapter.cancelBackgroundQueryTask();
    }

    @Override
    public void onQueryTaskCancelled(Object errorDefinition) {}

}
