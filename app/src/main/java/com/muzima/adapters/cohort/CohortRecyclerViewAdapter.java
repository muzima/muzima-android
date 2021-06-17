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

import java.util.List;
import java.util.Locale;

public class CohortRecyclerViewAdapter extends RecyclerView.Adapter<CohortRecyclerViewAdapter.ViewHolder> {

    private Context context;
    private List<Cohort> cohortList;

    public CohortRecyclerViewAdapter(Context context, List<Cohort> cohortList) {
        this.context = context;
        this.cohortList = cohortList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cohort_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CohortRecyclerViewAdapter.ViewHolder holder, int position) {
        Cohort cohort = cohortList.get(position);
        holder.titleTextView.setText(cohort.getName());
        holder.clientsCountTextView.setText(String.format(Locale.getDefault(), "%s Clients",String.valueOf(cohort.getSize())));
        if(((MuzimaApplication) context.getApplicationContext()).getCohortController()
                .isDownloaded(cohort))
            holder.iconImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_downloaded));
    }

    @Override
    public int getItemCount() {
        return cohortList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView titleTextView;
        private final TextView descriptionTextView;
        private final ImageView iconImageView;
        private final TextView clientsCountTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.item_cohort_name_text_view);
            descriptionTextView = itemView.findViewById(R.id.item_cohort_description_text_view);
            iconImageView = itemView.findViewById(R.id.item_cohort_status_image_view);
            clientsCountTextView = itemView.findViewById(R.id.item_cohort_clients_count_text_view);
        }
    }

    public interface OnCohortClickedListener {
        void onCohortClicked(int position);
    }
}
