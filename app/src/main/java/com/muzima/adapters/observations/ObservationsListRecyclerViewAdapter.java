package com.muzima.adapters.observations;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.R;
import com.muzima.api.model.Observation;
import com.muzima.utils.DateUtils;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ObservationsListRecyclerViewAdapter extends RecyclerView.Adapter<ObservationsListRecyclerViewAdapter.ViewHolder> {
    private List<Observation> observationList;
    private ObservationClickedListener observationClickedListener;

    public ObservationsListRecyclerViewAdapter(List<Observation> observationList, ObservationClickedListener observationClickedListener) {
        this.observationList = observationList;
        this.observationClickedListener = observationClickedListener;
    }

    @NonNull
    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_single_obs_view_layout, parent, false), observationClickedListener);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull ViewHolder holder, int position) {
        Observation observation = observationList.get(position);
        holder.valueTextView.setText(observation.getValueText());
        holder.dateTextView.setText(DateUtils.convertDateToStdString(observation.getValueDatetime()));
    }

    @Override
    public int getItemCount() {
        return observationList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final View container;
        private final TextView valueTextView;
        private final TextView dateTextView;
        private final ObservationClickedListener observationClickedListener;

        public ViewHolder(@NonNull @NotNull View itemView, ObservationClickedListener clickedListener) {
            super(itemView);
            this.container = itemView.findViewById(R.id.item_single_obs_container);
            this.valueTextView = itemView.findViewById(R.id.item_single_obs_value_text_view);
            this.dateTextView = itemView.findViewById(R.id.item_single_obs_date_text_view);
            this.observationClickedListener = clickedListener;
            this.container.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            this.observationClickedListener.onObservationClicked(getAdapterPosition());
        }
    }

    public interface ObservationClickedListener {
        void onObservationClicked(int position);
    }
}
