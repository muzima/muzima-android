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
import com.muzima.controller.EncounterController;
import com.muzima.controller.ObservationController;
import com.muzima.controller.ProviderController;
import com.muzima.utils.BackgroundTaskHelper;
import com.muzima.utils.DateUtils;

import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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
    private final Boolean shouldReplaceProviderIdWithNames;

    public ObservationByDateAdapter(Context context, String patientUuid) {
        this.context = context;
        this.patientUuid = patientUuid;
        MuzimaApplication app = (MuzimaApplication) context.getApplicationContext();
        this.encounterController = app.getEncounterController();
        this.conceptController = app.getConceptController();
        this.observationController = app.getObservationController();
        this.providerController = app.getProviderController();
        this.shouldReplaceProviderIdWithNames = app.getMuzimaSettingController().isPatientTagGenerationEnabled();
        dates = new ArrayList<>();
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
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        String applicationLanguage = preferences.getString(context.getResources().getString(R.string.preference_app_language), context.getResources().getString(R.string.language_english));

        String date = dates.get(position);
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(DateUtils.parse(date));
            holder.obsDay.setText(String.format("%02d",calendar.get(Calendar.DATE)));
            holder.obsMonthYear.setText(String.format(String.format("%02d", calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.YEAR)));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        holder.obsHorizontalListRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        ObsVerticalViewAdapter observationsListAdapter = new ObsVerticalViewAdapter(date, encounterController, observationController,  applicationLanguage, providerController, shouldReplaceProviderIdWithNames, patientUuid, context);

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
        AsyncTask<Void, ?, ?> backgroundQueryTask = new ObservationsByDateBackgroundTask(this, observationController, patientUuid);
        BackgroundTaskHelper.executeInParallel(backgroundQueryTask);
        setRunningBackgroundQueryTask(backgroundQueryTask);
    }

    public void setBackgroundListQueryTaskListener(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        this.backgroundListQueryTaskListener = backgroundListQueryTaskListener;
    }

    public BackgroundListQueryTaskListener getBackgroundListQueryTaskListener() {
        return backgroundListQueryTaskListener;
    }

    public void add(List<String> date) {
        dates = date;
    }

    public void clear() {
        if (dates != null)
            dates.clear();
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
