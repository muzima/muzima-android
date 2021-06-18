package com.muzima.adapters.cohort;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Cohort;
import com.muzima.model.cohort.CohortItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CohortRecyclerViewAdapter extends RecyclerView.Adapter<CohortRecyclerViewAdapter.ViewHolder> {

    private Context context;
    private List<CohortItem> cohortList;
    private OnCohortClickedListener cohortClickedListener;
    private List<Cohort> selectedCohorts = new ArrayList<>();

    public CohortRecyclerViewAdapter(Context context, List<CohortItem> cohortList, OnCohortClickedListener cohortClickedListener) {
        this.context = context;
        this.cohortList = cohortList;
        this.cohortClickedListener = cohortClickedListener;
    }

    public void setSelectedCohorts(List<Cohort> selectedCohorts) {
        this.selectedCohorts = selectedCohorts;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cohort_layout, parent, false);
        return new ViewHolder(view, cohortClickedListener);
    }

    @Override
    public void onBindViewHolder(@NonNull CohortRecyclerViewAdapter.ViewHolder holder, int position) {
        CohortItem cohort = cohortList.get(position);
        holder.titleTextView.setText(cohort.getCohort().getName());
        holder.clientsCountTextView.setText(String.format(Locale.getDefault(), "%s Clients", String.valueOf(cohort.getCohort().getSize())));
        if (((MuzimaApplication) context.getApplicationContext()).getCohortController()
                .isDownloaded(cohort.getCohort()))
            holder.iconImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_downloaded));
        if (cohort.getCohort().getSyncStatus() == 1)
            holder.iconImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_action_sync));

        for (Cohort selectedCohort : selectedCohorts) {
            if (selectedCohort.getUuid() == cohort.getCohort().getUuid())
                holder.container.setBackgroundColor(context.getResources().getColor(R.color.list_selection_background));
        }

    }

    @Override
    public int getItemCount() {
        return cohortList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final View container;
        private final TextView titleTextView;
        private final TextView descriptionTextView;
        private final ImageView iconImageView;
        private final TextView clientsCountTextView;
        private final OnCohortClickedListener cohortClickedListener;

        public ViewHolder(@NonNull View itemView, OnCohortClickedListener cohortClickedListener) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.item_cohort_name_text_view);
            descriptionTextView = itemView.findViewById(R.id.item_cohort_description_text_view);
            iconImageView = itemView.findViewById(R.id.item_cohort_status_image_view);
            clientsCountTextView = itemView.findViewById(R.id.item_cohort_clients_count_text_view);
            container = itemView.findViewById(R.id.item_cohort_layout);
            this.cohortClickedListener = cohortClickedListener;
            container.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            cohortClickedListener.onCohortClicked(getAdapterPosition());
        }
    }

    public interface OnCohortClickedListener {
        void onCohortClicked(int position);
    }
}
