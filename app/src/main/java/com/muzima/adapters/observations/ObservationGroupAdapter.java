package com.muzima.adapters.observations;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
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
import com.muzima.controller.ConceptController;
import com.muzima.controller.ObservationController;
import com.muzima.utils.BackgroundTaskHelper;

import org.jetbrains.annotations.NotNull;

import java.util.Calendar;
import java.util.List;

public class ObservationGroupAdapter extends RecyclerAdapter<ObservationsByTypeAdapter.ViewHolder> {
    protected Context context;
    private final String patientUuid;
    private BackgroundListQueryTaskListener backgroundListQueryTaskListener;
    private AsyncTask<?, ?, ?> backgroundQueryTask;
    final ConceptController conceptController;
    final ObservationController observationController;
    private List<String> obsGroups;
    private boolean shouldReplaceProviderIdWithNames;

    public ObservationGroupAdapter(Context context, String patientUuid) {
        this.context = context;
        this.patientUuid = patientUuid;
        MuzimaApplication app = (MuzimaApplication) context.getApplicationContext();
        this.conceptController = app.getConceptController();
        this.observationController = app.getObservationController();
        this.shouldReplaceProviderIdWithNames = app.getMuzimaSettingController().isPatientTagGenerationEnabled();
    }

    @NonNull
    @Override
    public ObservationGroupAdapter.ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        return new ObservationGroupAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.obs_group, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerAdapter.ViewHolder holder, int position) {
        bindViews((ObservationGroupAdapter.ViewHolder) holder, position);
    }

    private void bindViews(@NotNull ObservationGroupAdapter.ViewHolder holder, int position) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        String applicationLanguage = preferences.getString(context.getResources().getString(R.string.preference_app_language), context.getResources().getString(R.string.language_english));

        String obsGroup = obsGroups.get(position);

        holder.obsGroup.setText(obsGroup);

        holder.obsHorizontalListRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        FlowSheetAdapter observationsListAdapter = new FlowSheetAdapter(obsGroup, conceptController, observationController,  applicationLanguage, shouldReplaceProviderIdWithNames, patientUuid);

        holder.obsHorizontalListRecyclerView.setAdapter(observationsListAdapter);
    }

    @Override
    public int getItemCount() {
        return obsGroups.size();
    }

    public String getItem(int position) {
        return obsGroups.get(position);
    }

    @Override
    public void reloadData() {
        cancelBackgroundQueryTask();
        AsyncTask<Void, ?, ?> backgroundQueryTask = new ObservationGroupBackgroundTask(this, conceptController);
        BackgroundTaskHelper.executeInParallel(backgroundQueryTask);
        setRunningBackgroundQueryTask(backgroundQueryTask);
    }

    public void setBackgroundListQueryTaskListener(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        this.backgroundListQueryTaskListener = backgroundListQueryTaskListener;
    }

    public BackgroundListQueryTaskListener getBackgroundListQueryTaskListener() {
        return backgroundListQueryTaskListener;
    }

    public void add(List<String> obsGroup) {
        obsGroups = obsGroup;
    }

    public void clear() {
        if (obsGroups != null)
            obsGroups.clear();
    }

    public void cancelBackgroundQueryTask() {
        if (backgroundQueryTask != null) {
            backgroundQueryTask.cancel(true);
        }
    }

    void setRunningBackgroundQueryTask(AsyncTask<?, ?, ?> backgroundQueryTask) {
        this.backgroundQueryTask = backgroundQueryTask;
    }

    public static class ViewHolder extends RecyclerAdapter.ViewHolder {
        private final TextView obsGroup;
        private final RecyclerView obsHorizontalListRecyclerView;
        private final LinearLayout obsGroupContainer;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.obsGroup = itemView.findViewById(R.id.obs_group);
            this.obsGroupContainer = itemView.findViewById(R.id.obs_group_container);
            this.obsHorizontalListRecyclerView = itemView.findViewById(R.id.obs_list);
        }
    }
}
