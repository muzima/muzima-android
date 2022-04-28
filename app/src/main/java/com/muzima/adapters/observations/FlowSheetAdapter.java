package com.muzima.adapters.observations;


import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.R;
import com.muzima.api.model.Observation;
import com.muzima.controller.ConceptController;
import com.muzima.controller.ObservationController;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FlowSheetAdapter extends RecyclerView.Adapter<FlowSheetAdapter.ViewHolder> {
    private List<Observation> observationList = new ArrayList<>();
    final ConceptController conceptController;
    final ObservationController observationController;
    private final String applicationLanguage;
    private final Boolean shouldReplaceProviderIdWithNames;
    private final String patientUuid;
    private final String obsGroup;
    List<String> observationDates;

    public FlowSheetAdapter(String obsGroup, ConceptController conceptController, ObservationController observationController,
                            String applicationLanguage, boolean shouldReplaceProviderIdWithNames, String patientUuid) {

        this.conceptController = conceptController;
        this.observationController = observationController;
        this.applicationLanguage = applicationLanguage;
        this.shouldReplaceProviderIdWithNames = shouldReplaceProviderIdWithNames;
        this.patientUuid = patientUuid;
        this.obsGroup = obsGroup;
        observationList = getObservationForGroup(obsGroup, patientUuid);
        observationDates = getObservationDates();
    }

    @NotNull
    @Override
    public FlowSheetAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new FlowSheetAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.obs_group_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull FlowSheetAdapter.ViewHolder holder, int position) {
        Observation observation = observationList.get(position);
        //ToDo: Draw Tables
    }

    @Override
    public int getItemCount() {
        return observationList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TableLayout obsGroupTable;

        public ViewHolder(@NonNull View view) {
            super(view);
            this.obsGroupTable = view.findViewById(R.id.obs_group_table);
        }
    }

    public List<String> getObservationDates(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        List<String> dates = new ArrayList();
        try {
            List<Observation> observations = observationController.getObservationsByPatient(patientUuid);
            Collections.sort(observations, obsDateTimeComparator);
            for (Observation observation : observations) {
                if (observation.getObservationDatetime() != null) {
                    String formattedDate = dateFormat.format(observation.getObservationDatetime());
                    if(dates == null){
                        dates.add(formattedDate);
                    } else if(!dates.contains(formattedDate)){
                        dates.add(formattedDate);
                    }
                }
            }
        } catch (ObservationController.LoadObservationException e) {
            Log.w("Observations", String.format("Exception while loading observations for %s."), e);
        }
        return dates;
    }

    public List<Observation> getObservationForGroup(String obsGroup, String patientUuid){
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        observationList = new ArrayList<>();
        try {
            List<Observation> observations = observationController.getObservationsByPatient(patientUuid);
            observationList = observations;
        } catch (ObservationController.LoadObservationException e) {
            e.printStackTrace();
        }

        return observationList;
    }

    private final Comparator<Observation> obsDateTimeComparator = (lhs, rhs) -> {
        if (lhs.getObservationDatetime()==null)
            return -1;
        if (rhs.getObservationDatetime()==null)
            return 1;
        return -(lhs.getObservationDatetime()
                .compareTo(rhs.getObservationDatetime()));
    };
}
