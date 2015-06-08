/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */
package com.muzima.adapters.encounters;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.muzima.R;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.Observation;
import com.muzima.controller.ObservationController;
import com.muzima.search.api.util.StringUtil;
import com.muzima.utils.DateUtils;
import com.muzima.utils.Fonts;

import java.util.ArrayList;
import java.util.List;

public class EncounterObservationsAdapter  extends ObservationsAdapter  {
    protected BackgroundListQueryTaskListener backgroundListQueryTaskListener;
    protected String encounterUuid;
    protected Encounter encounter;
    public EncounterObservationsAdapter(Activity activity, int textViewResourceId, ObservationController observationController, Encounter encounter){
        super(activity, textViewResourceId, observationController);
        this.encounter = encounter;
        encounterUuid = encounter.getUuid();
    }
    @Override
    public void reloadData() {
        new BackgroundQueryTask().execute(encounterUuid);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        Observation observation =getItem(position);
        Context context = getContext();
        EncounterObservationsViewHolder holder;
        if(convertView==null){
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            convertView = layoutInflater.inflate(R.layout.item_encounter_observation,parent,false);
            holder=new EncounterObservationsViewHolder();
            holder.conceptQuestion = (TextView)convertView.findViewById(R.id.observationHeader);
            holder.observationValue = (TextView)convertView.findViewById(R.id.observationValue);
            holder.observationDate = (TextView)convertView.findViewById(R.id.observationDate);
            holder.observationComplex = (ImageView)convertView.findViewById(R.id.observationComplex);
            holder.divider = convertView.findViewById(R.id.divider);
            convertView.setTag(holder);
        }else{
            holder = (EncounterObservationsViewHolder)convertView.getTag();
        }
        holder.setObservation(observation);

        return convertView;
    }

    public void setBackgroundListQueryTaskListener(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        this.backgroundListQueryTaskListener = backgroundListQueryTaskListener;
    }

    private class EncounterObservationsViewHolder extends ViewHolder {
        public TextView conceptQuestion;
        public TextView observationDate;
        public TextView observationValue;
        public ImageView observationComplex;
        public View divider;
        protected void setObservation(Observation observation) {
            int conceptColor = observationController.getConceptColor(observation.getConcept().getUuid());

            String observationConceptType = observation.getConcept().getConceptType().getName();

            if (StringUtil.equals(observationConceptType, "Complex")){
                observationValue.setVisibility(View.GONE);
                observationComplex.setVisibility(View.VISIBLE);
            } else {
                observationValue.setVisibility(View.VISIBLE);
                observationComplex.setVisibility(View.GONE);

                observationValue.setTypeface(Fonts.roboto_medium(getContext()));
                observationValue.setTextColor(conceptColor);
                observationValue.setText(observation.getValueAsString());
            }

            divider.setBackgroundColor(conceptColor);

            observationDate.setText(DateUtils.getMonthNameFormattedDate(observation.getObservationDatetime()));
            observationDate.setTypeface(Fonts.roboto_light(getContext()));
            observationDate.setTextColor(conceptColor);

            conceptQuestion.setBackgroundColor(conceptColor);
            conceptQuestion.setText(observation.getConcept().getName());
        }
    }

    private class BackgroundQueryTask extends AsyncTask<String, Void, List<Observation>> {
        @Override
        protected void onPreExecute() {
            if (backgroundListQueryTaskListener != null) {
                backgroundListQueryTaskListener.onQueryTaskStarted();
            }
        }

        @Override
        protected List<Observation> doInBackground(String... params) {
            List<Observation> observations = null;
             try {
                 observations=new ArrayList<Observation>();
                 List<Observation>observationsByPatient = observationController.getObservationsByPatient(encounter.getPatient().getUuid());

                 for(Observation obs:observationsByPatient){
                     if(obs.getEncounter().getUuid().equals(encounter.getUuid())){
                         observations.add(obs);
                     }
                 }

             }catch(ObservationController.LoadObservationException e){
                Log.e(this.getClass().getSimpleName(),"Could not get Observations", e);
            }
            return observations;
        }

        @Override
        protected void onPostExecute(List<Observation> observations){
            if(observations==null){
                Toast.makeText(getContext(), "Observations could not be loaded", Toast.LENGTH_SHORT).show();
                return;
            }
            clear();
            addAll(observations);
            notifyDataSetChanged();
            backgroundListQueryTaskListener.onQueryTaskFinish();
        }
    }

}
