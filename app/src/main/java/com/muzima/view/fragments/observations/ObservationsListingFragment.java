package com.muzima.view.fragments.observations;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.observations.ObsVerticalListConceptsRecyclerView;
import com.muzima.api.model.Concept;
import com.muzima.api.model.Observation;
import com.muzima.controller.ConceptController;
import com.muzima.controller.ObservationController;
import com.muzima.model.ObsConceptWrapper;
import com.muzima.model.events.ClientSummaryObservationSelectedEvent;
import com.muzima.model.events.ReloadObservationsDataEvent;

import com.muzima.utils.StringUtils;
import com.muzima.view.custom.MuzimaRecyclerView;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ObservationsListingFragment extends Fragment {
    private int category;
    private MuzimaRecyclerView conceptsListRecyclerView;
    private String patientUuid;
    private boolean isSingleElementInputEnabled;
    private ObsVerticalListConceptsRecyclerView adapter;
    private List<ObsConceptWrapper> obsConceptWrapperList = new ArrayList<>();

    public ObservationsListingFragment(int category, String patientUuid, boolean isSingleElementInputEnabled) {
        this.category = category;
        this.patientUuid = patientUuid;
        this.isSingleElementInputEnabled = isSingleElementInputEnabled;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_observations_listings_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable Bundle savedInstanceState) {
        initializeResources(view);
        loadData();
    }

    private void initializeResources(@NotNull View view) {
        conceptsListRecyclerView = view.findViewById(R.id.fragment_observations_list_concepts_recycler_view);
        conceptsListRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext(), LinearLayoutManager.VERTICAL, false));
        adapter = new ObsVerticalListConceptsRecyclerView(getActivity().getApplicationContext(), obsConceptWrapperList, isSingleElementInputEnabled, new ObsVerticalListConceptsRecyclerView.ConceptInputLabelClickedListener() {
            @Override
            public void onConceptInputLabelClicked(int position) {
                if (obsConceptWrapperList.get(position).getConcept().getConceptType().getName().equals(Concept.CODED_TYPE)) {
                    Toast.makeText(getActivity().getApplicationContext(), getResources().getString(R.string.erro_coded_concepts_not_supported), Toast.LENGTH_LONG).show();
                } else {
                    EventBus.getDefault().post(new ClientSummaryObservationSelectedEvent(obsConceptWrapperList.get(position)));
                }
            }
        });
        conceptsListRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext()));
        conceptsListRecyclerView.setAdapter(adapter);
        conceptsListRecyclerView.setNoDataLayout(view.findViewById(R.id.no_data_layout),
                getString(R.string.info_observation_unavailable),
                StringUtils.EMPTY);
    }

    private void loadData() {
        try {
            List<Concept> conceptList = ((MuzimaApplication) getActivity().getApplicationContext()).getConceptController().getConcepts();
            for (Concept concept : conceptList) {
                if (!concept.getConceptType().getName().equals(Concept.CODED_TYPE)) {
                    List<Observation> matchingObservations = ((MuzimaApplication) getActivity().getApplicationContext()).getObservationController().getObservationsByPatientuuidAndConceptId(patientUuid, concept.getId());
                    obsConceptWrapperList.add(new ObsConceptWrapper(concept, matchingObservations));
                }
            }
            adapter.notifyDataSetChanged();
        } catch (ObservationController.LoadObservationException | ConceptController.ConceptFetchException e) {
            e.printStackTrace();
        }
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
        obsConceptWrapperList.clear();
        loadData();
    }
}
