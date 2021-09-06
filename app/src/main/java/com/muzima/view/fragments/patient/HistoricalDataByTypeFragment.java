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
import com.muzima.utils.StringUtils;
import com.muzima.view.custom.MuzimaRecyclerView;

public class HistoricalDataByTypeFragment extends Fragment implements ObservationsByTypeAdapter.ConceptInputLabelClickedListener, RecyclerAdapter.BackgroundListQueryTaskListener {
    private final String patientUuid;
    private ObservationsByTypeAdapter observationsByTypeAdapter;

    public HistoricalDataByTypeFragment(String patientUuid) {
        this.patientUuid = patientUuid;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_historical_data_list, container, false);
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
