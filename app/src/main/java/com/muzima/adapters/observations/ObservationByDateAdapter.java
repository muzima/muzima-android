/*
 * Copyright (c) Vanderbilt University Medical Center and Lambda Informatics.
 * All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.adapters.observations;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.RecyclerAdapter;
import com.muzima.api.model.Observation;
import com.muzima.api.model.SetupConfigurationTemplate;
import com.muzima.controller.ConceptController;
import com.muzima.controller.DerivedObservationController;
import com.muzima.controller.EncounterController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.ProviderController;
import com.muzima.controller.SetupConfigurationController;
import com.muzima.model.ConceptIcons;
import com.muzima.util.JsonUtils;
import com.muzima.utils.BackgroundTaskHelper;
import com.muzima.utils.DateUtils;
import com.muzima.utils.MuzimaPreferences;

import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ObservationByDateAdapter extends RecyclerAdapter<ObservationsByTypeAdapter.ViewHolder> {
    protected Context context;
    private final String patientUuid;
    private List<String> dates;
    private BackgroundListQueryTaskListener backgroundListQueryTaskListener;
    private AsyncTask<?, ?, ?> backgroundQueryTask;
    final ConceptController conceptController;
    final EncounterController encounterController;
    final ObservationController observationController;
    final ProviderController providerController;
    final SetupConfigurationController setupConfigurationController;
    final DerivedObservationController derivedObservationController;
    private final Boolean shouldReplaceProviderIdWithNames;
    private final List<ConceptIcons> conceptIcons;
    private final MuzimaApplication muzimaApplication;
    Map<String, List<Observation>> datesObservations = new LinkedHashMap<>();

    public ObservationByDateAdapter(Context context, String patientUuid) {
        this.context = context;
        this.patientUuid = patientUuid;
        this.muzimaApplication = (MuzimaApplication) context.getApplicationContext();
        this.encounterController = muzimaApplication.getEncounterController();
        this.conceptController = muzimaApplication.getConceptController();
        this.observationController = muzimaApplication.getObservationController();
        this.providerController = muzimaApplication.getProviderController();
        this.setupConfigurationController = muzimaApplication.getSetupConfigurationController();
        this.derivedObservationController = muzimaApplication.getDerivedObservationController();
        this.shouldReplaceProviderIdWithNames = muzimaApplication.getMuzimaSettingController().isPatientTagGenerationEnabled();
        dates = new ArrayList<>();
        this.conceptIcons = getConceptIcons();
    }

    @NonNull
    @Override
    public ObservationByDateAdapter.ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        return new ObservationByDateAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.timeline, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerAdapter.ViewHolder holder, int position) {
        bindViews((ObservationByDateAdapter.ViewHolder) holder, position);
    }

    private void bindViews(@NotNull ObservationByDateAdapter.ViewHolder holder, int position) {
        String applicationLanguage = MuzimaPreferences.getStringPreference(context, context.getResources().getString(R.string.preference_app_language), context.getResources().getString(R.string.language_english));

        String date = dates.get(position);
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(DateUtils.parse(date));
            holder.obsDay.setText(String.format("%02d",calendar.get(Calendar.DATE)));
            holder.obsMonthYear.setText(String.format(String.format("%02d", calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.YEAR)));
        } catch (ParseException e) {
            Log.e(getClass().getSimpleName(),"Encounter an ParseException",e);

        }

        holder.obsHorizontalListRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        ObsVerticalViewAdapter observationsListAdapter = new ObsVerticalViewAdapter(date, muzimaApplication,
                applicationLanguage, shouldReplaceProviderIdWithNames, patientUuid, context, conceptIcons, datesObservations);

        holder.obsHorizontalListRecyclerView.setAdapter(observationsListAdapter);
    }

    @Override
    public int getItemCount() {
        return dates.size();
    }

    public String getItem(int position) {
        return dates.get(position);
    }

    @Override
    public void reloadData() {
        cancelBackgroundQueryTask();
        AsyncTask<Void, ?, ?> backgroundQueryTask = new ObservationsByDateBackgroundTask(this, observationController, patientUuid, derivedObservationController, context);
        BackgroundTaskHelper.executeInParallel(backgroundQueryTask);
        setRunningBackgroundQueryTask(backgroundQueryTask);
    }

    public void setBackgroundListQueryTaskListener(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        this.backgroundListQueryTaskListener = backgroundListQueryTaskListener;
    }

    public BackgroundListQueryTaskListener getBackgroundListQueryTaskListener() {
        return backgroundListQueryTaskListener;
    }

    public void add(List<String> date, Map<String, List<Observation>> datesObservationsMap) {
        dates = date;
        datesObservations = datesObservationsMap;
    }

    public void clear() {
        if (dates != null)
            dates.clear();
        if(datesObservations != null)
            datesObservations.clear();
    }

    public void cancelBackgroundQueryTask() {
        if (backgroundQueryTask != null) {
            backgroundQueryTask.cancel(true);
        }
    }

    void setRunningBackgroundQueryTask(AsyncTask<?, ?, ?> backgroundQueryTask) {
        this.backgroundQueryTask = backgroundQueryTask;
    }

    public List<ConceptIcons> getConceptIcons(){
        String json = "";
        List<ConceptIcons> conceptIcons = new ArrayList<>();
        try {
            SetupConfigurationTemplate activeSetupConfig = setupConfigurationController.getActiveSetupConfigurationTemplate();
            json = activeSetupConfig.getConfigJson();
        } catch (SetupConfigurationController.SetupConfigurationFetchException e) {
            Log.e(getClass().getSimpleName(),"Exception encountered while fetching setup configs ",e);
        }

        List<Object> concepts = JsonUtils.readAsObjectList(json, "$['config']['concepts']");
        for (Object concept : concepts) {
            ConceptIcons conceptIcon = new ConceptIcons();
            String icon = "";
            net.minidev.json.JSONObject concept1 = (net.minidev.json.JSONObject) concept;
            String conceptUuid = concept1.get("uuid").toString();
            if(concept1.get("icon") != null) {
                icon = concept1.get("icon").toString();
            }
            conceptIcon.setConceptUuid(conceptUuid);
            conceptIcon.setIcon(icon);
            conceptIcons.add(conceptIcon);
        }

        return conceptIcons;
    }

    public static class ViewHolder extends RecyclerAdapter.ViewHolder {
        private final TextView obsDay;
        private final TextView obsMonthYear;
        private final RecyclerView obsHorizontalListRecyclerView;
        private final LinearLayout observationHeaderLayout;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.obsDay = itemView.findViewById(R.id.obs_concept_day);
            this.obsMonthYear = itemView.findViewById(R.id.obs_month_year);
            this.observationHeaderLayout = itemView.findViewById(R.id.value_container_cardview);
            this.obsHorizontalListRecyclerView = itemView.findViewById(R.id.obs_list);
        }
    }
}
