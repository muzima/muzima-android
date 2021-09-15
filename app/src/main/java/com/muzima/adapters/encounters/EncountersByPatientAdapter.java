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
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.RecyclerAdapter;
import com.muzima.api.model.Encounter;
import com.muzima.controller.EncounterController;
import com.muzima.tasks.MuzimaAsyncTask;
import com.muzima.utils.DateUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EncountersByPatientAdapter extends RecyclerAdapter<EncountersByPatientAdapter.ViewHolder> {
    protected Context context;
    private BackgroundListQueryTaskListener backgroundListQueryTaskListener;
    private final String patientUuid;
    private final EncounterClickedListener encounterClickedListener;
    private List<Encounter> encounterList;
    final EncounterController encounterController;


    public EncountersByPatientAdapter(Context context, String patientUuid, EncounterClickedListener encounterClickedListener) {
        this.context = context;
        this.patientUuid = patientUuid;
        this.encounterClickedListener =  encounterClickedListener;
        encounterList = new ArrayList<>();
        MuzimaApplication app = (MuzimaApplication) context.getApplicationContext();
        this.encounterController = app.getEncounterController();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_encounter, parent, false), encounterClickedListener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerAdapter.ViewHolder holder, int position) {
        bindViews((EncountersByPatientAdapter.ViewHolder) holder, position);
    }

    private void bindViews(@NotNull EncountersByPatientAdapter.ViewHolder holder, int position) {
        Encounter encounter= encounterList.get(position);

        String location = encounter.getLocation().getName();
        String providerName = encounter.getProvider().getDisplayName();
        String date = DateUtils.getMonthNameFormattedDate(encounter.getEncounterDatetime());
        String encounterType = encounter.getEncounterType().getName();
        encounterType = String.valueOf(encounterType.charAt(0)).toUpperCase()+encounterType.substring(1,encounterType.length()).toLowerCase();

        holder.encounterLocation.setText(location);
        holder.encounterProvider.setText(providerName);
        holder.encounterFormName.setText(encounterType);
        holder.encounterDate.setText(date);
    }

    @Override
    public int getItemCount() {
        return encounterList.size();
    }

    public Encounter getItem(int position) {
        return encounterList.get(position);
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask().execute(patientUuid);
    }

    public void setBackgroundListQueryTaskListener(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        this.backgroundListQueryTaskListener = backgroundListQueryTaskListener;
    }

    public static class ViewHolder extends RecyclerAdapter.ViewHolder implements View.OnClickListener{
        TextView encounterFormName;
        TextView encounterProvider;
        TextView encounterDate;
        TextView encounterLocation;
        private final EncounterClickedListener  encounterClickedListener;

        public ViewHolder(@NonNull View view, EncounterClickedListener encounterClickedListener) {
            super(view);
            this.encounterFormName = view.findViewById(R.id.encounterFormName);
            this.encounterProvider = view.findViewById(R.id.encounterProvider);
            this.encounterDate = view.findViewById(R.id.encounterDate);
            this.encounterLocation = view.findViewById(R.id.encounterLocation);
            this.encounterClickedListener = encounterClickedListener;
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            this.encounterClickedListener.onEncounterClicked(getAdapterPosition());
        }
    }

    public interface EncounterClickedListener {
        void onEncounterClicked(int position);
    }

    private class BackgroundQueryTask extends MuzimaAsyncTask<String, Void, List<Encounter>> {
        @Override
        protected void onPreExecute() {
            if (backgroundListQueryTaskListener != null)
                backgroundListQueryTaskListener.onQueryTaskStarted();
        }

        @Override
        protected List<Encounter> doInBackground(String... params) {
            List<Encounter> encounters = null;
            try {
                encounters = encounterController.getEncountersByPatientUuid(patientUuid);
                if (encounters != null)
                    Collections.sort(encounters, encountersDateTimeComparator);
            } catch (EncounterController.FetchEncounterException e) {
                Log.e(this.getClass().getSimpleName(), "Could not get patient encounters", e);
            }

            return encounters;
        }

        private final Comparator<Encounter> encountersDateTimeComparator = (lhs, rhs) -> {
            if (lhs.getEncounterDatetime()==null)
                return -1;
            if (rhs.getEncounterDatetime()==null)
                return 1;
            return -(lhs.getEncounterDatetime()
                    .compareTo(rhs.getEncounterDatetime()));
        };

        @Override
        protected void onPostExecute(List<Encounter> encounters){
            if(encounters==null){
                Toast.makeText(context, context.getString(R.string.error_encounter_load),Toast.LENGTH_SHORT).show();
                return;
            }
            encounterList.clear();
            encounterList = encounters;
            notifyDataSetChanged();
            backgroundListQueryTaskListener.onQueryTaskFinish();
        }

        @Override
        protected void onBackgroundError(Exception e) {}
    }
}
