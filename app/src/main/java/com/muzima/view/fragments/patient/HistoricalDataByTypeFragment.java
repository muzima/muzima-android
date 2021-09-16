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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.muzima.R;
import com.muzima.adapters.RecyclerAdapter;
import com.muzima.adapters.observations.ObservationsByTypeAdapter;
import com.muzima.model.events.ReloadObservationsDataEvent;
import com.muzima.utils.StringUtils;
import com.muzima.view.custom.MuzimaRecyclerView;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class HistoricalDataByTypeFragment extends Fragment implements ObservationsByTypeAdapter.ConceptInputLabelClickedListener, RecyclerAdapter.BackgroundListQueryTaskListener {
    private final String patientUuid;
    private ObservationsByTypeAdapter observationsByTypeAdapter;

    public HistoricalDataByTypeFragment(String patientUuid) {
        this.patientUuid = patientUuid;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MuzimaRecyclerView obsByTypeRecyclerView = view.findViewById(R.id.recycler_list);
        obsByTypeRecyclerView.setLayoutManager(new LinearLayoutManager(requireActivity().getApplicationContext(), LinearLayoutManager.VERTICAL, false));

        observationsByTypeAdapter = new ObservationsByTypeAdapter(requireActivity().getApplicationContext(), patientUuid,
                false, false, this);
        observationsByTypeAdapter.setBackgroundListQueryTaskListener(this);
        obsByTypeRecyclerView.setLayoutManager(new LinearLayoutManager(requireActivity().getApplicationContext()));
        obsByTypeRecyclerView.setAdapter(observationsByTypeAdapter);
        observationsByTypeAdapter.reloadData();
        obsByTypeRecyclerView.setNoDataLayout(view.findViewById(R.id.no_data_layout),
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
        observationsByTypeAdapter.notifyDataSetChanged();
    }

    @Override
    public void onConceptInputLabelClicked(int position) {}

    @Override
    public void onQueryTaskStarted() {}

    @Override
    public void onQueryTaskFinish() {}

    @Override
    public void onQueryTaskCancelled() {
        observationsByTypeAdapter.cancelBackgroundQueryTask();
    }
}
