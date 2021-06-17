package com.muzima.adapters.cohort;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.R;
import com.muzima.api.model.CohortFilter;

import java.util.List;

public class CohortFilterAdapter extends RecyclerView.Adapter<CohortFilterAdapter.ViewHolder> {

    private List<CohortFilter> cohortList;
    private CohortFilterClickedListener filterClickedListener;

    public CohortFilterAdapter(List<CohortFilter> cohortList, CohortFilterClickedListener filterClickedListener) {
        this.cohortList = cohortList;
        this.filterClickedListener = filterClickedListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cohort_filter, parent, false);
        return new ViewHolder(view, filterClickedListener);
    }

    @Override
    public void onBindViewHolder(@NonNull CohortFilterAdapter.ViewHolder holder, int position) {
        CohortFilter cohort = cohortList.get(position);
        holder.checkBox.setText(cohort.getCohort().getName());
        holder.checkBox.setChecked(cohort.isSelected());
    }

    @Override
    public int getItemCount() {
        return cohortList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final View container;
        private final CheckBox checkBox;
        private final CohortFilterClickedListener cohortFilterClickedListener;

        public ViewHolder(@NonNull View itemView, CohortFilterClickedListener listener) {
            super(itemView);
            this.checkBox = itemView.findViewById(R.id.item_cohort_filter_checkbox);
            this.container = itemView.findViewById(R.id.item_cohort_filter_container);
            this.cohortFilterClickedListener = listener;
            this.checkBox.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            this.cohortFilterClickedListener.onCohortFilterClicked(getAdapterPosition());
        }
    }

    public interface CohortFilterClickedListener {
        void onCohortFilterClicked(int position);
    }
}
