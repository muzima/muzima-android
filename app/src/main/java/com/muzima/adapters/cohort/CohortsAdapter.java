/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.adapters.cohort;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.RecyclerAdapter;
import com.muzima.api.model.Cohort;
import com.muzima.controller.CohortController;
import com.muzima.model.cohort.CohortItem;
import com.muzima.tasks.MuzimaAsyncTask;
import com.muzima.utils.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Responsible to populate synced cohorts fetched from DB in the Cohort Fragment page.
 */
public class CohortsAdapter extends RecyclerAdapter<CohortsAdapter.ViewHolder> implements Filterable {
    private final Context context;
    private final List<CohortItem> cohortList = new ArrayList<>();
    private List<CohortItem> filteredCohortList = new ArrayList<>();
    private final CohortsAdapter.OnCohortClickedListener cohortClickedListener;
    private MuzimaAsyncTask<Void, Void, List<Cohort>> backgroundQueryTask;
    private BackgroundListQueryTaskListener backgroundListQueryTaskListener;
    private final CohortController cohortController;
    private final int cohortType;
    private final List<String> selectedCohortsUuid;

    public CohortsAdapter(Context context, OnCohortClickedListener cohortClickedListener, int cohortType) {
        this.context = context;
        this.cohortClickedListener = cohortClickedListener;
        this.cohortType = cohortType;
        selectedCohortsUuid = new ArrayList<>();
        cohortController = ((MuzimaApplication) context.getApplicationContext()).getCohortController();
    }

    @NonNull
    @Override
    public CohortsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cohort_layout, parent, false);
        return new ViewHolder(view, cohortClickedListener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerAdapter.ViewHolder holder, int position) {
        bindViews((ViewHolder) holder, position);
    }

    private void bindViews(@NonNull CohortsAdapter.ViewHolder holder, int position) {
        CohortItem cohortItem = filteredCohortList.get(position);
        holder.titleTextView.setText(cohortItem.getCohort().getName());
        holder.descriptionTextView.setText(cohortItem.getCohort().getDescription());
        holder.clientsCountTextView.setText(String.format(Locale.getDefault(), "%s Clients", String.valueOf(cohortItem.getCohort().getSize())));
        if (((MuzimaApplication) context.getApplicationContext()).getCohortController()
                .isDownloaded(cohortItem.getCohort())) {
            if (((MuzimaApplication) context.getApplicationContext()).getCohortController()
                    .isDownloaded(cohortItem.getCohort()) && cohortItem.getCohort().isUpdateAvailable())
                holder.iconImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_action_sync));
            else
                holder.iconImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_downloaded));
        } else
            holder.iconImageView.setImageDrawable(null);

        //        Tag tag1 = new Tag();
//        tag1.setName("Tag1");
//        Tag tag2 = new Tag();
//        tag2.setName("Tag2");
//        Tag[] tags = new Tag[]{tag1, tag2};
//        TagsAdapter tagsAdapter = new TagsAdapter(tags);
//        holder.tagsListView.setAdapter(tagsAdapter);
//        holder.tagsListView.setLayoutManager(new LinearLayoutManager(context.getApplicationContext(), LinearLayoutManager.HORIZONTAL, false));

        highlightCohorts(cohortItem, holder.container);
    }

    private void highlightCohorts(CohortItem cohortItem, View view) {
        if (selectedCohortsUuid.contains(cohortItem.getCohort().getUuid())) {
            view.setBackgroundResource(R.color.hint_blue_opaque);
        } else {
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = context.getTheme();
            theme.resolveAttribute(R.attr.primaryBackgroundColor, typedValue, true);
            view.setBackgroundResource(typedValue.resourceId);
        }
    }

    @Override
    public int getItemCount() {
        return filteredCohortList.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();
                if (charString.isEmpty()) {
                    filteredCohortList = cohortList;
                } else {
                    List<CohortItem> filteredList = new ArrayList<>();
                    for (CohortItem item : cohortList) {
                        Cohort cohort = item.getCohort();
                        if (cohort.getName().toLowerCase().contains(charString.toLowerCase())
                                || cohort.getDescription().toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(item);
                        }
                    }
                    filteredCohortList = filteredList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = filteredCohortList;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                filteredCohortList = (ArrayList<CohortItem>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    @Override
    public void reloadData() {
        cancelBackgroundTask();
        backgroundQueryTask = new BackgroundQueryTask();
        backgroundQueryTask.execute();
    }

    public List<String> getSelectedCohorts() {
        return selectedCohortsUuid;
    }

    public int numberOfCohorts() {
        return getSelectedCohorts().size();
    }

    public String[] getSelectedCohortsArray() {
        return getSelectedCohorts().toArray(new String[getSelectedCohorts().size()]);
    }

    public void clearSelectedCohorts() {
        selectedCohortsUuid.clear();
        notifyDataSetChanged();
    }

    public void setBackgroundListQueryTaskListener(BackgroundListQueryTaskListener backgroundListQueryTaskListener) {
        this.backgroundListQueryTaskListener = backgroundListQueryTaskListener;
    }

    public void cancelBackgroundTask(){
        if(backgroundQueryTask != null){
            backgroundQueryTask.cancel();
        }
    }

    public class ViewHolder extends RecyclerAdapter.ViewHolder implements View.OnClickListener {
        private final View container;
        private final TextView titleTextView;
        private final TextView descriptionTextView;
        private final ImageView iconImageView;
        private final TextView clientsCountTextView;
        private final CohortsAdapter.OnCohortClickedListener cohortClickedListener;

        public ViewHolder(@NonNull View view, CohortsAdapter.OnCohortClickedListener cohortClickedListener) {
            super(view);
            titleTextView = view.findViewById(R.id.item_cohort_name_text_view);
            descriptionTextView = view.findViewById(R.id.item_cohort_description_text_view);
            iconImageView = view.findViewById(R.id.item_cohort_status_image_view);
            clientsCountTextView = view.findViewById(R.id.item_cohort_clients_count_text_view);
            container = view.findViewById(R.id.item_cohort_layout);
            this.cohortClickedListener = cohortClickedListener;
            container.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            Cohort cohort = filteredCohortList.get(getAdapterPosition()).getCohort();
            boolean isChecked = false;
            if (!selectedCohortsUuid.contains(cohort.getUuid())) {
                selectedCohortsUuid.add(cohort.getUuid());
                isChecked = true;
            } else
                selectedCohortsUuid.remove(cohort.getUuid());

            notifyDataSetChanged();

            cohortClickedListener.onCohortClicked(isChecked);
        }
    }

    public interface OnCohortClickedListener {
        void onCohortClicked(boolean isChecked);
    }

    private class BackgroundQueryTask extends MuzimaAsyncTask<Void, Void, List<Cohort>> {

        @Override
        protected void onPreExecute() {
            if(backgroundListQueryTaskListener != null){
                backgroundListQueryTaskListener.onQueryTaskStarted();
            }
        }

        @Override
        protected List<Cohort> doInBackground(Void... voids) {
            List<Cohort> loadedCohorts = null;
            try {
                if (cohortType == Constants.COHORT_LIST_TYPE.ALL) {
                    loadedCohorts = cohortController.getAllCohorts();
                    Log.i(getClass().getSimpleName(), "#Retrieved " + loadedCohorts.size() + " Cohorts from Database.");
                } else if (cohortType == Constants.COHORT_LIST_TYPE.DOWNLOADED) {
                    loadedCohorts = cohortController.getSyncedCohorts();
                    Log.i(getClass().getSimpleName(), "#Synced Cohorts: " + loadedCohorts.size());
                } else {
                    loadedCohorts = cohortController.getUnSyncedCohorts();
                    Log.i(getClass().getSimpleName(), "#Unsynced Cohorts: " + loadedCohorts.size());
                }
            } catch (CohortController.CohortFetchException e) {
                Log.e(getClass().getSimpleName(), "Exception occurred while fetching local synced cohorts ", e);
            }
            return loadedCohorts;
        }

        @Override
        protected void onPostExecute(List<Cohort> cohorts) {
            if(cohorts == null){
                Toast.makeText(context, context.getString(R.string.error_cohort_fetch), Toast.LENGTH_SHORT).show();
                return;
            }

            cohortList.clear();
            Collections.sort(cohorts, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
            for (Cohort cohort : cohorts) {
                cohortList.add(new CohortItem(cohort));
            }

            filteredCohortList = cohortList;
            notifyDataSetChanged();

            if(backgroundListQueryTaskListener != null)
                backgroundListQueryTaskListener.onQueryTaskFinish();
        }

        @Override
        protected void onBackgroundError(Exception e) {}
    }
}

