package com.muzima.adapters.observations;

import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.RecyclerAdapter;
import com.muzima.api.model.Concept;
import com.muzima.controller.ConceptController;
import com.muzima.controller.EncounterController;
import com.muzima.controller.ObservationController;
import com.muzima.model.observation.ConceptWithObservations;
import com.muzima.utils.BackgroundTaskHelper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ObservationsByTypeAdapter extends RecyclerAdapter<ObservationsByTypeAdapter.ViewHolder> {
    protected Context context;
    private final String patientUuid;
    private final boolean inputRendering;
    private List<ConceptWithObservations> conceptWithObservationsList;
    private final ConceptInputLabelClickedListener conceptInputLabelClickedListener;
    private BackgroundListQueryTaskListener backgroundListQueryTaskListener;
    private AsyncTask<?, ?, ?> backgroundQueryTask;
    final ConceptController conceptController;
    final EncounterController encounterController;
    final ObservationController observationController;
    private final Boolean isShrData;

    public ObservationsByTypeAdapter(Context context, String patientUuid, Boolean isShrData, boolean inputRendering,
                                     ConceptInputLabelClickedListener conceptInputLabelClickedListener) {
        this.context = context;
        this.patientUuid = patientUuid;
        this.isShrData = isShrData;
        this.inputRendering = inputRendering;
        this.conceptInputLabelClickedListener = conceptInputLabelClickedListener;
        MuzimaApplication app = (MuzimaApplication) context.getApplicationContext();
        this.encounterController = app.getEncounterController();
        this.conceptController = app.getConceptController();
        this.observationController = app.getObservationController();
        conceptWithObservationsList = new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_observation_by_concept_list_2, parent, false), conceptInputLabelClickedListener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerAdapter.ViewHolder holder, int position) {
        bindViews((ObservationsByTypeAdapter.ViewHolder) holder, position);
    }

    private void bindViews(@NotNull ViewHolder holder, int position) {
        ConceptWithObservations conceptWithObservations = conceptWithObservationsList.get(position);

        if (inputRendering)
            holder.titleTextView.setText(String.format(Locale.getDefault(), "+ %s", getConceptDisplay(conceptWithObservations.getConcept())));
        else
            holder.titleTextView.setText(getConceptDisplay(conceptWithObservations.getConcept()));
        holder.obsHorizontalListRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL,false));
        ObsHorizontalViewAdapter observationsListAdapter = new ObsHorizontalViewAdapter(conceptWithObservations.getObservations(), new ObsHorizontalViewAdapter.ObservationClickedListener() {
            @Override
            public void onObservationClicked(int position) {
//                EventBus.getDefault().post(new ClientSummaryObservationSelectedEvent(conceptWithObservationsList.get(position)));
            }
        }, encounterController, observationController, isShrData, inputRendering);
        holder.obsHorizontalListRecyclerView.setAdapter(observationsListAdapter);
    }

    String getConceptDisplay(Concept concept) {
        String text = concept.getName();
        if (concept.getConceptType().getName().equals(Concept.NUMERIC_TYPE)) {
            text += " (" + concept.getUnit() + ")";
        }
        return text;
    }

    @Override
    public int getItemCount() {
        return conceptWithObservationsList.size();
    }

    @Override
    public void reloadData() {
        cancelBackgroundQueryTask();
        AsyncTask<Void, ?, ?> backgroundQueryTask = new ObservationsByTypeBackgroundTask(this,
                new ConceptsByPatient(conceptController, observationController, patientUuid), isShrData);
        BackgroundTaskHelper.executeInParallel(backgroundQueryTask);
        setRunningBackgroundQueryTask(backgroundQueryTask);
    }

    public void setBackgroundListQueryTaskListener(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        this.backgroundListQueryTaskListener = backgroundListQueryTaskListener;
    }

    public BackgroundListQueryTaskListener getBackgroundListQueryTaskListener() {
        return backgroundListQueryTaskListener;
    }

    public void add(List<ConceptWithObservations> conceptWithObservations) {
        conceptWithObservationsList = conceptWithObservations;
    }

    public void clear() {
        if (conceptWithObservationsList != null)
            conceptWithObservationsList.clear();
    }

    public void cancelBackgroundQueryTask() {
        if (backgroundQueryTask != null) {
            backgroundQueryTask.cancel(true);
        }
    }

    void setRunningBackgroundQueryTask(AsyncTask<?, ?, ?> backgroundQueryTask) {
        this.backgroundQueryTask = backgroundQueryTask;
    }

    public static class ViewHolder extends RecyclerAdapter.ViewHolder implements View.OnClickListener{
        private final TextView titleTextView;
        private final RecyclerView obsHorizontalListRecyclerView;
        private final ConceptInputLabelClickedListener conceptInputLabelClickedListener;

        public ViewHolder(@NonNull View itemView, ConceptInputLabelClickedListener conceptInputLabelClickedListener) {
            super(itemView);
            this.titleTextView = itemView.findViewById(R.id.obs_concept);
            this.obsHorizontalListRecyclerView = itemView.findViewById(R.id.obs_list);
            this.conceptInputLabelClickedListener = conceptInputLabelClickedListener;
            this.titleTextView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            this.conceptInputLabelClickedListener.onConceptInputLabelClicked(getAdapterPosition());
        }
    }

    public interface ConceptInputLabelClickedListener {
        void onConceptInputLabelClicked(int position);
    }
}
