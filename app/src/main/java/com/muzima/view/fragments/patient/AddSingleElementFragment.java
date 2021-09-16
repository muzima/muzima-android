/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.fragments.patient;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.muzima.R;
import com.muzima.adapters.RecyclerAdapter;
import com.muzima.adapters.observations.ObservationsByTypeAdapter;
import com.muzima.api.model.Concept;
import com.muzima.model.events.ClientSummaryObservationSelectedEvent;
import com.muzima.model.events.ReloadObservationsDataEvent;
import com.muzima.model.observation.ConceptWithObservations;
import com.muzima.utils.StringUtils;
import com.muzima.view.custom.MuzimaRecyclerView;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class AddSingleElementFragment extends Fragment implements ObservationsByTypeAdapter.ConceptInputLabelClickedListener, RecyclerAdapter.BackgroundListQueryTaskListener {
    private final String patientUuid;
    private ObservationsByTypeAdapter observationsByTypeAdapter;

    public AddSingleElementFragment(String patientUuid) {
        this.patientUuid = patientUuid;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MuzimaRecyclerView conceptsListRecyclerView = view.findViewById(R.id.recycler_list);
        conceptsListRecyclerView.setLayoutManager(new LinearLayoutManager(requireActivity().getApplicationContext(), LinearLayoutManager.VERTICAL, false));

        observationsByTypeAdapter = new ObservationsByTypeAdapter(requireActivity().getApplicationContext(), patientUuid,
                false, true, this);
        observationsByTypeAdapter.setBackgroundListQueryTaskListener(this);
        conceptsListRecyclerView.setLayoutManager(new LinearLayoutManager(requireActivity().getApplicationContext()));
        conceptsListRecyclerView.setAdapter(observationsByTypeAdapter);
        observationsByTypeAdapter.reloadData();
        conceptsListRecyclerView.setNoDataLayout(view.findViewById(R.id.no_data_layout),
                getString(R.string.info_observation_unavailable),
                StringUtils.EMPTY);
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            EventBus.getDefault().register(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onReloadDataEvent(ReloadObservationsDataEvent event) {
        observationsByTypeAdapter.reloadData();
    }

    @Override
    public void onConceptInputLabelClicked(int position) {
        ConceptWithObservations conceptWithObservations = observationsByTypeAdapter.getItem(position);
        if (conceptWithObservations.getConcept().getConceptType().getName().equals(Concept.CODED_TYPE)) {
            Toast.makeText(requireActivity().getApplicationContext(), getResources().getString(R.string.erro_coded_concepts_not_supported), Toast.LENGTH_LONG).show();
        } else {
            EventBus.getDefault().post(new ClientSummaryObservationSelectedEvent(conceptWithObservations));
        }
    }

    @Override
    public void onQueryTaskStarted() {}

    @Override
    public void onQueryTaskFinish() {}

    @Override
    public void onQueryTaskCancelled() {
        observationsByTypeAdapter.cancelBackgroundQueryTask();
    }
}
