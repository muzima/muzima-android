/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.adapters.forms;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.tags.TagsAdapter;
import com.muzima.api.model.Tag;
import com.muzima.controller.FormController;
import com.muzima.model.FormItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FormsRecyclerViewAdapter extends Adapter<FormsRecyclerViewAdapter.ViewHolder> {
    private final Context context;
    private final List<FormItem> formList;
    private List<FormItem> itemsCopy = new ArrayList<>();
    private final OnFormClickedListener onFormClickedListener;

    public FormsRecyclerViewAdapter(Context context, List<FormItem> formList, OnFormClickedListener onFormClickedListener) {
        this.context = context;
        this.formList = formList;
        this.onFormClickedListener = onFormClickedListener;
    }

    public void setItemsCopy(List<FormItem> itemsCopy, String source) {
        this.itemsCopy = itemsCopy;
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
            FormItem form = formList.get(position);
            holder.titleTextView.setText(form.getForm().getName());
            holder.descriptionTextView.setText(form.getForm().getDescription());
            holder.formVersionCodeTextView.setText(String.format(Locale.getDefault(), "v%s", form.getForm().getVersion()));
            if (((MuzimaApplication) context.getApplicationContext()).getFormController()
                    .isFormDownloaded(form.getForm()))
                holder.iconImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_downloaded));
            else
                holder.iconImageView.setVisibility(View.GONE);

            TagsAdapter adapter = new TagsAdapter(form.getForm().getTags());
            holder.tagsListView.setLayoutManager(new LinearLayoutManager(context.getApplicationContext(), LinearLayoutManager.HORIZONTAL, false));
            holder.tagsListView.setAdapter(adapter);
            if (form.getForm().getTags().length > 0)
                holder.tagsListView.setVisibility(View.VISIBLE);
            if (form.isSelected())
                holder.container.setBackgroundColor(context.getResources().getColor(R.color.hint_blue_opaque));
            else {
                holder.container.setBackgroundColor(context.getResources().getColor(R.color.transparent));
            }
        } catch (FormController.FormFetchException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return formList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
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

    public void filter(String searchKey) {
        formList.clear();
        for (FormItem formItem : itemsCopy) {
            if (formItem.getForm().getName().contains(searchKey) ||
                    formItem.getForm().getDescription().contains(searchKey) ||
                    formItem.getForm().getDiscriminator().contains(searchKey)) {
                formList.add(formItem);
            }
        }

        if (formList.isEmpty()) {
            for (FormItem formItem : itemsCopy) {
                boolean tagsMatch = false;
                for (Tag tag : formItem.getForm().getTags()) {
                    if (tag.getName().startsWith(searchKey))
                        tagsMatch = true;
                }

                if (tagsMatch)
                    formList.add(formItem);
            }
        }

        if (formList.isEmpty())
            formList.addAll(itemsCopy);

        notifyDataSetChanged();
    }
}
