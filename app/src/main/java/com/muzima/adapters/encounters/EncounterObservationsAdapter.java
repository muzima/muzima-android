/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */
package com.muzima.adapters.encounters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.muzima.R;
import com.muzima.adapters.RecyclerAdapter;
import com.muzima.api.model.Observation;
import com.muzima.controller.ObservationController;
import com.muzima.model.observation.EncounterWithObservations;
import com.muzima.model.observation.Encounters;
import com.muzima.tasks.MuzimaAsyncTask;
import com.muzima.utils.DateUtils;
import com.muzima.utils.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class EncounterObservationsAdapter extends RecyclerAdapter<EncountersByPatientAdapter.ViewHolder> {
    protected Context context;
    private BackgroundListQueryTaskListener backgroundListQueryTaskListener;
    private final String encounterUuid;
    private final ObservationController observationController;
    private List<Observation> observationList;

    public EncounterObservationsAdapter(Context context, ObservationController observationController, String encounterUuid){
        this.context = context;
        this.encounterUuid = encounterUuid;
        this.observationController = observationController;
        this.observationList = new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_encounter_observation, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerAdapter.ViewHolder holder, int position) {
        bindViews((ViewHolder) holder, position);
    }

    private void bindViews(@NotNull ViewHolder holder, int position) {
        Observation observation = observationList.get(position);
        holder.setObservation(observation);
    }

    @Override
    public int getItemCount() {
        return observationList.size();
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask().execute(encounterUuid);
    }

    public void setBackgroundListQueryTaskListener(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        this.backgroundListQueryTaskListener = backgroundListQueryTaskListener;
    }

    private class ViewHolder extends RecyclerAdapter.ViewHolder {
        TextView conceptQuestion;
        TextView observationDate;
        TextView observationValue;
        ImageView observationComplex;
        View divider;

        public ViewHolder(@NonNull View view) {
            super(view);
            this.conceptQuestion = view.findViewById(R.id.observationHeader);
            this.observationDate = view.findViewById(R.id.observationDate);
            this.observationValue = view.findViewById(R.id.observationValue);
            this.observationComplex = view.findViewById(R.id.observationComplex);
            this.divider = view.findViewById(R.id.divider);
        }

        void setObservation(Observation observation) {
            int conceptColor = observationController.getConceptColor(observation.getConcept().getUuid());
            String observationConceptType = observation.getConcept().getConceptType().getName();

            if (StringUtils.equals(observationConceptType, "Complex")){
                observationValue.setVisibility(View.GONE);
                observationComplex.setVisibility(View.VISIBLE);
            } else {
                observationValue.setVisibility(View.VISIBLE);
                observationComplex.setVisibility(View.GONE);

                observationValue.setTextColor(conceptColor);
                observationValue.setText(observation.getValueAsString());
            }

            divider.setBackgroundColor(conceptColor);

            observationDate.setText(DateUtils.getMonthNameFormattedDate(observation.getObservationDatetime()));
            observationDate.setTextColor(conceptColor);

            conceptQuestion.setBackgroundColor(conceptColor);
            conceptQuestion.setText(observation.getConcept().getName());
        }
    }

    private class BackgroundQueryTask extends MuzimaAsyncTask<String, Void, List<Observation>> {
        @Override
        protected void onPreExecute() {
            if (backgroundListQueryTaskListener != null)
                backgroundListQueryTaskListener.onQueryTaskStarted();
        }

        @Override
        protected List<Observation> doInBackground(String... params) {
            List<Observation> observations = null;
            try {
                observations = new ArrayList<>();
                Encounters encounterWithObservations = observationController.getObservationsByEncounterUuid(encounterUuid);

                for (EncounterWithObservations encounterWithObs : encounterWithObservations) {
                    observations.addAll(encounterWithObs.getObservations());
                }
            } catch (ObservationController.LoadObservationException e) {
                Log.e(this.getClass().getSimpleName(), "Could not get Observations", e);
            }
            return observations;
        }

        @Override
        protected void onPostExecute(List<Observation> observations){
            if(observations==null){
                Toast.makeText(context, context.getString(R.string.error_observation_load), Toast.LENGTH_SHORT).show();
                return;
            }
            observationList.clear();
            observationList = observations;
            notifyDataSetChanged();
            backgroundListQueryTaskListener.onQueryTaskFinish();
        }

        @Override
        protected void onBackgroundError(Exception e) {}
    }
}
