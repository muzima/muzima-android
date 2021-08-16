package com.muzima.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.R;
import com.muzima.model.IncompleteFormWithPatientData;

import java.util.List;

public class IncompleteFormsWithDataAdapter extends RecyclerView.Adapter<IncompleteFormsWithDataAdapter.ViewHolder> {
    private Context context;
    private List<IncompleteFormWithPatientData> formList;
    private OnFormClickedListener onFormClickedListener;

    public IncompleteFormsWithDataAdapter(Context context, List<IncompleteFormWithPatientData> formList, OnFormClickedListener onFormClickedListener) {
        this.context = context;
        this.formList = formList;
        this.onFormClickedListener = onFormClickedListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_form_layout, parent, false);
        return new ViewHolder(view, onFormClickedListener);
    }

    @Override
    public void onBindViewHolder(@NonNull IncompleteFormsWithDataAdapter.ViewHolder holder, int position) {
        IncompleteFormWithPatientData form = formList.get(position);
        holder.titleTextView.setText(form.getName());
        holder.descriptionTextView.setText(form.getDescription());
    }

    @Override
    public int getItemCount() {
        return formList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final View container;
        private final TextView titleTextView;
        private final TextView descriptionTextView;
        private final RecyclerView tagsListView;
        private final ImageView iconImageView;
        private final TextView formVersionCodeTextView;
        private final OnFormClickedListener onFormClickedListener;

        public ViewHolder(@NonNull View itemView, OnFormClickedListener formClickedListener) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.item_form_name_text_view);
            descriptionTextView = itemView.findViewById(R.id.item_form_description_text_view);
            iconImageView = itemView.findViewById(R.id.item_form_status_image_view);
            formVersionCodeTextView = itemView.findViewById(R.id.item_form_version_code_text_view);
            container = itemView.findViewById(R.id.item_form_layout);
            tagsListView = itemView.findViewById(R.id.item_form_tags_list_view);
            onFormClickedListener = formClickedListener;
            container.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            onFormClickedListener.onFormClicked(getAdapterPosition());
        }
    }

    public interface OnFormClickedListener {
        void onFormClicked(int position);
    }
}
