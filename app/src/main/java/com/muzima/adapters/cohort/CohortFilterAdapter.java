package com.muzima.adapters.cohort;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.R;
import com.muzima.model.CohortFilter;
import com.muzima.utils.MuzimaPreferences;

import java.util.List;

public class CohortFilterAdapter extends RecyclerView.Adapter<CohortFilterAdapter.ViewHolder> {
    private Context context;
    private List<CohortFilter> cohortList;
    private CohortFilterClickedListener filterClickedListener;

    public CohortFilterAdapter(Context context, List<CohortFilter> cohortList, CohortFilterClickedListener filterClickedListener) {
        this.context = context;
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
        if (cohort.getCohort() == null) {
            holder.checkBox.setText(context.getResources().getString(R.string.general_all_clients));
        } else {
            holder.checkBox.setText(cohort.getCohort().getName());
        }
        holder.checkBox.setChecked(cohort.isSelected());
        if (cohort.isSelected())
            holder.container.setBackground(context.getResources().getDrawable(R.drawable.global_highlight_background));
        else {
            if (MuzimaPreferences.getIsLightModeThemeSelectedPreference(context))
                holder.container.setBackgroundColor(context.getResources().getColor(R.color.primary_white));
            else
                holder.container.setBackgroundColor(context.getResources().getColor(R.color.primary_black));
        }

        if (!cohort.isCheckboxPadded()) {
            applyCheckBoxPadding(holder.checkBox);
            cohort.setCheckboxPadded(true);
        }
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

    private void applyCheckBoxPadding(CheckBox checkBox) {
        final float scale = context.getResources().getDisplayMetrics().density;
        if (checkBox.getPaddingLeft() <= 7) {
            checkBox.setPadding(checkBox.getPaddingLeft() + (int) (20.0f * scale + 0.5f),
                    checkBox.getPaddingTop(),
                    checkBox.getPaddingRight(),
                    checkBox.getPaddingBottom());
        }
    }
}
