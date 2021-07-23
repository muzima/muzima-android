package com.muzima.adapters.cohort;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.tags.TagsAdapter;
import com.muzima.api.model.Tag;
import com.muzima.model.cohort.CohortItem;
import com.muzima.utils.MuzimaPreferences;

import java.util.List;
import java.util.Locale;

public class CohortRecyclerViewAdapter extends RecyclerView.Adapter<CohortRecyclerViewAdapter.ViewHolder> {

    private Context context;
    private List<CohortItem> cohortList;
    private OnCohortClickedListener cohortClickedListener;

    public CohortRecyclerViewAdapter(Context context, List<CohortItem> cohortList, OnCohortClickedListener cohortClickedListener) {
        this.context = context;
        this.cohortList = cohortList;
        this.cohortClickedListener = cohortClickedListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cohort_layout, parent, false);
        return new ViewHolder(view, cohortClickedListener);
    }

    @Override
    public void onBindViewHolder(@NonNull CohortRecyclerViewAdapter.ViewHolder holder, int position) {
        CohortItem cohortItem = cohortList.get(position);
        holder.titleTextView.setText(cohortItem.getCohort().getName());
        holder.clientsCountTextView.setText(String.format(Locale.getDefault(), "%s Clients", String.valueOf(cohortItem.getCohort().getSize())));
        if (((MuzimaApplication) context.getApplicationContext()).getCohortController()
                .isDownloaded(cohortItem.getCohort()))
            holder.iconImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_downloaded));

        if (((MuzimaApplication) context.getApplicationContext()).getCohortController()
                .isDownloaded(cohortItem.getCohort()) && cohortItem.getCohort().getSyncStatus() != 1)
            holder.iconImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_action_sync));

        if (cohortItem.isSelected())
            holder.container.setBackgroundColor(context.getResources().getColor(R.color.hint_blue_opaque));
        else {
            if (MuzimaPreferences.getIsLightModeThemeSelectedPreference(context))
                holder.container.setBackgroundColor(context.getResources().getColor(R.color.primary_white));
            else
                holder.container.setBackgroundColor(context.getResources().getColor(R.color.primary_black));
        }

        Tag tag1 = new Tag();
        tag1.setName("Tag1");
        Tag tag2 = new Tag();
        tag2.setName("Tag2");
        Tag[] tags = new Tag[]{tag1, tag2};
        TagsAdapter tagsAdapter = new TagsAdapter(tags);
        holder.tagsListView.setAdapter(tagsAdapter);
        holder.tagsListView.setLayoutManager(new LinearLayoutManager(context.getApplicationContext(), LinearLayoutManager.HORIZONTAL, false));

    }

    @Override
    public int getItemCount() {
        return cohortList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final View container;
        private final TextView titleTextView;
        private final TextView descriptionTextView;
        private final RecyclerView tagsListView;
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
            tagsListView = itemView.findViewById(R.id.item_cohort_tags_list_view);
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
