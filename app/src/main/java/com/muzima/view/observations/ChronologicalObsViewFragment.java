package com.muzima.view.observations;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.RecyclerAdapter;
import com.muzima.adapters.observations.ObservationByDateAdapter;
import com.muzima.api.model.Concept;
import com.muzima.api.model.Patient;
import com.muzima.controller.ConceptController;
import com.muzima.controller.EncounterController;
import com.muzima.controller.ObservationController;
import com.muzima.utils.StringUtils;
import com.muzima.view.custom.MuzimaRecyclerView;

import java.util.List;

public class ChronologicalObsViewFragment extends ObservationsListFragment implements RecyclerAdapter.BackgroundListQueryTaskListener {
    private String patientUuid;
    private ObservationByDateAdapter observationByDateAdapter;
    private Patient patient;
    LinearLayout noDataLayout;
    MuzimaRecyclerView conceptsListRecyclerView;

    public ChronologicalObsViewFragment() {

    }

    public ChronologicalObsViewFragment(ObservationController observationController,
                                        Patient patient) {
        this.patient = patient;
        this.observationController = observationController;
    }

    public static ChronologicalObsViewFragment newInstance(ObservationController observationController,
                                                           Patient patient) {

        ChronologicalObsViewFragment f = new ChronologicalObsViewFragment();
        f.observationController = observationController;
        f.patient = patient;

        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onSearchTextChange(String query) {

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        patientUuid = patient.getUuid();

        noDataLayout = view.findViewById(R.id.no_data_layout);
        conceptsListRecyclerView = view.findViewById(R.id.recycler_list);
        conceptsListRecyclerView.setLayoutManager(new LinearLayoutManager(requireActivity().getApplicationContext(), LinearLayoutManager.VERTICAL, false));

        observationByDateAdapter = new ObservationByDateAdapter(requireActivity().getApplicationContext(), patientUuid);
        observationByDateAdapter.setBackgroundListQueryTaskListener(this);
        conceptsListRecyclerView.setLayoutManager(new LinearLayoutManager(requireActivity().getApplicationContext()));
        conceptsListRecyclerView.setAdapter(observationByDateAdapter);
        observationByDateAdapter.reloadData();
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
        MuzimaApplication muzimaApplication = (MuzimaApplication) requireActivity().getApplicationContext();
        String noDataTip = StringUtils.EMPTY;
        try {
            List<Concept> concepts;
            concepts = muzimaApplication.getConceptController().getConcepts();
            if(concepts.size()>0){
                noDataTip = getString(R.string.info_no_observation_for_concept_data_tip);
            }else{
                noDataTip = getString(R.string.info_no_observation_and_concept_data_tip);
            }
        } catch (ConceptController.ConceptFetchException e) {
            Log.e(getClass().getSimpleName(), "Exception while fetching concepts ",e);
        }

        conceptsListRecyclerView.setNoDataLayout(noDataLayout,
                getString(R.string.info_observation_unavailable),
                noDataTip);
    }

    @Override
    public void onQueryTaskCancelled() {
        observationByDateAdapter.cancelBackgroundQueryTask();
    }

    @Override
    public void onQueryTaskCancelled(Object errorDefinition) {}

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }
}
