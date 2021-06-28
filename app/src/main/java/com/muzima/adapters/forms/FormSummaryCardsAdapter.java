package com.muzima.adapters.forms;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.R;
import com.muzima.model.SummaryCard;

import java.util.List;

public class FormSummaryCardsAdapter extends RecyclerView.Adapter<FormSummaryCardsAdapter.ViewHolder> {

    private Context context;
    private List<SummaryCard> summaryList;
    private OnCardClickedListener cardClickedListener;

    public FormSummaryCardsAdapter(Context context, List<SummaryCard> summaryList, OnCardClickedListener cardClickedListener) {
        this.context = context;
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
        SummaryCard summaryCard = summaryList.get(position);
        if (summaryCard.getTitle().equalsIgnoreCase(context.getResources().getString(R.string.info_incomplete_form))){
            holder.containerView.setBackground(context.getResources().getDrawable(R.drawable.incomplete_forms_gradient));
        }

        holder.titleTextView.setText(summaryCard.getTitle());
        holder.countTextView.setText(String.valueOf(summaryCard.getCount()));
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

            containerView.setOnClickListener(this);
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
