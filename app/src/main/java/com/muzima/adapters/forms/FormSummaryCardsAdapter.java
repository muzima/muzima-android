package com.muzima.adapters.forms;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.R;
import com.muzima.model.FormsSummary;

import java.util.List;

public class FormSummaryCardsAdapter extends RecyclerView.Adapter<FormSummaryCardsAdapter.ViewHolder> {

    private List<FormsSummary> summaryList;
    private OnCardClickedListener cardClickedListener;

    public FormSummaryCardsAdapter(List<FormsSummary> summaryList, OnCardClickedListener cardClickedListener) {
        this.summaryList = summaryList;
        this.cardClickedListener = cardClickedListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_forms_summary_card, parent, false);
        return new ViewHolder(view, cardClickedListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FormsSummary formsSummary = summaryList.get(position);
        holder.titleTextView.setText(formsSummary.getTitle());
        holder.countTextView.setText(String.valueOf(formsSummary.getCount()));
    }

    @Override
    public int getItemCount() {
        return summaryList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private View containerView;
        private TextView countTextView;
        private TextView titleTextView;
        private OnCardClickedListener cardClickedListener;

        public ViewHolder(@NonNull View itemView, OnCardClickedListener cardClickedListener) {
            super(itemView);
            containerView = itemView.findViewById(R.id.item__forms_view);
            countTextView = itemView.findViewById(R.id.item_forms_count_view);
            titleTextView = itemView.findViewById(R.id.item_forms_title_text_view);

            this.cardClickedListener = cardClickedListener;
        }

        @Override
        public void onClick(View view) {
            cardClickedListener.onCardClicked(getAdapterPosition());
        }
    }

    public interface OnCardClickedListener {
        void onCardClicked(int position);
    }
}
