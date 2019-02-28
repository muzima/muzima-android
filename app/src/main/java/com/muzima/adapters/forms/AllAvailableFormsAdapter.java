/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */
package com.muzima.adapters.forms;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Tag;
import com.muzima.controller.FormController;
import com.muzima.model.AvailableForm;
import com.muzima.model.collections.AvailableForms;
import com.muzima.utils.StringUtils;
import com.muzima.service.MuzimaSyncService;
import com.muzima.tasks.FormsAdapterBackgroundQueryTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible to list down all the available forms in the server including Tags and img to indicate whether downloaded or not.
 */
public class AllAvailableFormsAdapter extends FormsAdapter<AvailableForm> implements TagsListAdapter.TagsChangedListener {
    private final MuzimaSyncService muzimaSyncService;


    public AllAvailableFormsAdapter(Context context, int textViewResourceId, FormController formController) {
        super(context, textViewResourceId, formController);
        muzimaSyncService = ((MuzimaApplication) (getContext().getApplicationContext())).getMuzimaSyncService();

    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = super.getView(position, convertView, parent);

        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        addTags(viewHolder, getItem(position));
        markIfDownloaded(viewHolder.downloadedImg, getItem(position));
        return convertView;
    }


    @Override
    protected int getFormItemLayout() {
        return R.layout.item_forms_list_selectable;
    }

    private void markIfDownloaded(View downloadedImg, AvailableForm form) {
        if (form.isDownloaded()) {
            downloadedImg.setVisibility(View.VISIBLE);
        } else {
            downloadedImg.setVisibility(View.GONE);
        }
    }

    private void addTags(ViewHolder holder, AvailableForm form) {
        Tag[] tags = form.getTags();
        if (tags.length > 0) {
            holder.tagsScroller.setVisibility(View.VISIBLE);
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());

            //add update tags
            for (int i = 0; i < tags.length; i++) {
                TextView textView = null;
                if (holder.tags.size() <= i) {
                    textView = newTextView(layoutInflater);
                    holder.addTag(textView);
                }
                textView = holder.tags.get(i);
                textView.setBackgroundColor(formController.getTagColor(tags[i].getUuid()));
                List<Tag> selectedTags = formController.getSelectedTags();
                if (selectedTags.isEmpty() || selectedTags.contains(tags[i])) {
                    textView.setText(tags[i].getName());
                } else {
                    textView.setText(StringUtils.EMPTY);
                }
            }

            //remove existing extra tags which are present because of recycled list view
            if (tags.length < holder.tags.size()) {
                List<TextView> tagsToRemove = new ArrayList<>();
                for (int i = tags.length; i < holder.tags.size(); i++) {
                    tagsToRemove.add(holder.tags.get(i));
                }
                holder.removeTags(tagsToRemove);
            }
        } else {
            holder.tagsScroller.setVisibility(View.GONE);
        }
    }

    private TextView newTextView(LayoutInflater layoutInflater) {
        TextView textView = (TextView) layoutInflater.inflate(R.layout.tag, null, false);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        layoutParams.setMargins(1, 0, 0, 0);
        textView.setLayoutParams(layoutParams);
        return textView;
    }

    public void clearSelectedForms() {
        notifyDataSetChanged();
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask(this).execute();
    }

    public void downloadFormTemplatesAndReload() {
        new DownloadBackgroundQueryTask(this).execute();
    }

    @Override
    public void onTagsChanged() {
        reloadData();
    }

    /**
     * Responsible to download all the form names based on Tags from Server.
     */
    class DownloadBackgroundQueryTask extends FormsAdapterBackgroundQueryTask<AvailableForm> {

        DownloadBackgroundQueryTask(FormsAdapter adapter) {
            super(adapter);
        }

        @Override
        protected AvailableForms doInBackground(Void... voids) {
            AvailableForms allForms = null;
            if (adapterWeakReference.get() != null) {
                try {
                    FormsAdapter formsAdapter = adapterWeakReference.get();
                    muzimaSyncService.downloadForms();
                    allForms = formsAdapter.getFormController().getAvailableFormByTags(getSelectedTagUuids());
                    Log.i(getClass().getSimpleName(), "#Forms: " + allForms.size());
                } catch (FormController.FormFetchException e) {
                    Log.w(getClass().getSimpleName(), "Exception occurred while fetching local forms ", e);
                }
            }
            return allForms;
        }
    }

    /**
     * Responsible to fetch the forms from the local DB based on the selected tags.
     */
    class BackgroundQueryTask extends FormsAdapterBackgroundQueryTask<AvailableForm> {

        BackgroundQueryTask(FormsAdapter formsAdapter) {
            super(formsAdapter);
        }

        @Override
        protected AvailableForms doInBackground(Void... voids) {
            AvailableForms allForms = null;
            if (adapterWeakReference.get() != null) {
                try {
                    FormsAdapter formsAdapter = adapterWeakReference.get();
                    allForms = formsAdapter.getFormController().getAvailableFormByTags(getSelectedTagUuids(), true);
                    Log.i(getClass().getSimpleName(), "#Forms: " + allForms.size());
                } catch (FormController.FormFetchException e) {
                    Log.w(getClass().getSimpleName(), "Exception occurred while fetching local forms.", e);
                }
            }
            return allForms;
        }

    }

}
