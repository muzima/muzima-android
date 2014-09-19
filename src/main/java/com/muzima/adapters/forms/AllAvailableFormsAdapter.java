/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

/**
 * Copyright 2012 Muzima Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.muzima.adapters.forms;

import android.app.Activity;
import android.content.Context;
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
import com.muzima.search.api.util.StringUtil;
import com.muzima.service.MuzimaSyncService;
import com.muzima.tasks.FormsAdapterBackgroundQueryTask;
import com.muzima.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible to list down all the available forms in the server including Tags and img to indicate whether downloaded or not.
 */
public class AllAvailableFormsAdapter extends FormsAdapter<AvailableForm> implements TagsListAdapter.TagsChangedListener {
    private static final String TAG = "AllAvailableFormsAdapter";
    private final MuzimaSyncService muzimaSyncService;


    public AllAvailableFormsAdapter(Context context, int textViewResourceId, FormController formController) {
        super(context, textViewResourceId, formController);
        muzimaSyncService = ((MuzimaApplication) (getContext().getApplicationContext())).getMuzimaSyncService();

    }

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

    protected void addTags(ViewHolder holder, AvailableForm form) {
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
                    textView.setText(StringUtil.EMPTY);
                }
            }

            //remove existing extra tags which are present because of recycled list view
            if (tags.length < holder.tags.size()) {
                List<TextView> tagsToRemove = new ArrayList<TextView>();
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

    public void downloadFormTemplatesAndReload(final Activity activity) {

        Runnable retryAction = new Runnable() {
            @Override
            public void run() {
                downloadFormTemplatesAndReload(activity);
            }
        };
        Runnable defaultAction = new Runnable() {
            @Override
            public void run() {
                new DownloadBackgroundQueryTask(AllAvailableFormsAdapter.this).execute();
            }
        };
        NetworkUtils.checkAndExecuteInternetBasedOperation(activity,retryAction,defaultAction);
    }

    @Override
    public void onTagsChanged() {
        reloadData();
    }

    /**
     * Responsible to download all the form names based on Tags from Server.
     */
    public class DownloadBackgroundQueryTask extends FormsAdapterBackgroundQueryTask<AvailableForm> {

        public DownloadBackgroundQueryTask(FormsAdapter adapter) {
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
                    Log.i(TAG, "#Forms: " + allForms.size());
                } catch (FormController.FormFetchException e) {
                    Log.w(TAG, "Exception occurred while fetching local forms ", e);
                }
            }
            return allForms;
        }
    }

    /**
     * Responsible to fetch the forms from the local DB based on the selected tags.
     */
    public class BackgroundQueryTask extends FormsAdapterBackgroundQueryTask<AvailableForm> {

        public BackgroundQueryTask(FormsAdapter formsAdapter) {
            super(formsAdapter);
        }

        @Override
        protected AvailableForms doInBackground(Void... voids) {
            AvailableForms allForms = null;
            if (adapterWeakReference.get() != null) {
                try {
                    FormsAdapter formsAdapter = adapterWeakReference.get();
                    allForms = formsAdapter.getFormController().getAvailableFormByTags(getSelectedTagUuids(), true);
                    Log.i(TAG, "#Forms: " + allForms.size());
                } catch (FormController.FormFetchException e) {
                    Log.w(TAG, "Exception occurred while fetching local forms.", e);
                }
            }
            return allForms;
        }

    }

}
