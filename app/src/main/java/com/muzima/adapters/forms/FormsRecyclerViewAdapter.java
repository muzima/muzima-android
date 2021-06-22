package com.muzima.adapters.forms;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Form;
import com.muzima.controller.FormController;

import java.util.List;
import java.util.Locale;

public class FormsRecyclerViewAdapter extends RecyclerView.Adapter<FormsRecyclerViewAdapter.ViewHolder> {
    private Context context;
    private List<Form> formList;
    private OnFormClickedListener onFormClickedListener;

    public FormsRecyclerViewAdapter(Context context, List<Form> formList, OnFormClickedListener onFormClickedListener) {
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
    public void onBindViewHolder(@NonNull FormsRecyclerViewAdapter.ViewHolder holder, int position) {
        try {
            Form form = formList.get(position);
            holder.titleTextView.setText(form.getName());
            holder.descriptionTextView.setText(form.getDescription());
            holder.formVersionCodeTextView.setText(String.format(Locale.getDefault(), "v%s",form.getVersion()));
            if (((MuzimaApplication) context.getApplicationContext()).getFormController()
                    .isFormDownloaded(form))
                holder.iconImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_downloaded));
        } catch (FormController.FormFetchException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return formList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final View container;
        private final TextView titleTextView;
        private final TextView descriptionTextView;
        private final ListView tagsListView;
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
