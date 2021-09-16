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

import android.content.Intent;
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
import com.muzima.adapters.encounters.EncountersByPatientAdapter;
import com.muzima.api.model.Encounter;
import com.muzima.utils.StringUtils;
import com.muzima.view.custom.MuzimaRecyclerView;
import com.muzima.view.encounters.EncounterSummaryActivity;

public class HistoricalDataByEncounterFragment extends Fragment implements EncountersByPatientAdapter.EncounterClickedListener, RecyclerAdapter.BackgroundListQueryTaskListener {
    private final String patientUuid;
    private EncountersByPatientAdapter encountersByPatientAdapter;

    public HistoricalDataByEncounterFragment(String patientUuid) {
        this.patientUuid = patientUuid;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MuzimaRecyclerView encounterRecyclerView = view.findViewById(R.id.recycler_list);
        encounterRecyclerView.setLayoutManager(new LinearLayoutManager(requireActivity().getApplicationContext(), LinearLayoutManager.VERTICAL, false));
        encountersByPatientAdapter = new EncountersByPatientAdapter(requireActivity().getApplicationContext(), patientUuid, this);
        encountersByPatientAdapter.setBackgroundListQueryTaskListener(this);
        encounterRecyclerView.setLayoutManager(new LinearLayoutManager(requireActivity().getApplicationContext()));
        encounterRecyclerView.setAdapter(encountersByPatientAdapter);
        encountersByPatientAdapter.reloadData();
        encounterRecyclerView.setNoDataLayout(view.findViewById(R.id.no_data_layout),
                getString(R.string.info_encounter_unavailable),
                StringUtils.EMPTY);
    }

    @Override
    public void onEncounterClicked(int position) {
        Encounter encounter = encountersByPatientAdapter.getItem(position);
        Intent intent = new Intent(requireActivity(), EncounterSummaryActivity.class);
        intent.putExtra(EncounterSummaryActivity.ENCOUNTER, encounter);
        startActivity(intent);
    }

    @Override
    public void onQueryTaskStarted() {}

    @Override
    public void onQueryTaskFinish() {}

    @Override
    public void onQueryTaskCancelled() {}
}
