/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.adapters.patients;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;

import androidx.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.api.model.PatientTag;
import com.muzima.controller.PatientController;
import com.muzima.tasks.MuzimaAsyncTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible to list down the tags in the TagDrawer.
 */
public class PatientTagsListAdapter extends ListAdapter<PatientTag> implements AdapterView.OnItemClickListener {

    private final PatientController patientController;
    PatientsLocalSearchAdapter patientsLocalSearchAdapter;

    public PatientTagsListAdapter(Context context, int textViewResourceId,PatientController patientController) {
        super(context, textViewResourceId);
        this.patientController = patientController;
        patientsLocalSearchAdapter = new PatientsLocalSearchAdapter(context,
                R.layout.layout_list, patientController, new ArrayList<String>(), null);
    }

    public void onTagsChanged() {
        patientsLocalSearchAdapter.search("");
        patientsLocalSearchAdapter.notifyDataSetChanged();
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        com.muzima.adapters.patients.PatientTagsListAdapter.ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(
                    R.layout.item_tags_list, parent, false);
            holder = new com.muzima.adapters.patients.PatientTagsListAdapter.ViewHolder();
            holder.indicator = convertView.findViewById(R.id.tag_indicator);
            holder.name = convertView
                    .findViewById(R.id.tag_name);
            holder.tagColorIndicator = convertView
                    .findViewById(R.id.tag_color_indicator);
            holder.icon = convertView.findViewById(R.id.tag_icon);
            convertView.setTag(holder);
        }

        holder = (com.muzima.adapters.patients.PatientTagsListAdapter.ViewHolder) convertView.getTag();
        int tagColor = patientController.getTagColor(getItem(position).getUuid());
        if (position == 0) {
            tagColor = Color.parseColor("#333333");
        }

        Resources resources = getContext().getResources();
        List<PatientTag> selectedTags = patientController.getSelectedTags();
        if (selectedTags.isEmpty()) {
            if (position == 0) {
                markItemSelected(holder, tagColor, resources);
            } else {
                markItemUnselected(holder, resources);
            }
        } else {
            if (selectedTags.contains(getItem(position))) {
                markItemSelected(holder, tagColor, resources);
            } else {
                markItemUnselected(holder, resources);
            }
        }
        holder.tagColorIndicator.setBackgroundColor(tagColor);
        holder.name.setText(getItem(position).getName());
        return convertView;
    }

    private void markItemUnselected(com.muzima.adapters.patients.PatientTagsListAdapter.ViewHolder holder, Resources resources) {
        holder.icon.setImageDrawable(resources.getDrawable(R.drawable.ic_action_close));
        int drawerColor = resources.getColor(R.color.drawer_background);
        holder.indicator.setBackgroundColor(drawerColor);
    }

    private void markItemSelected(com.muzima.adapters.patients.PatientTagsListAdapter.ViewHolder holder, int tagColor, Resources resources) {
        holder.icon.setImageDrawable(resources.getDrawable(R.drawable.ic_accept));
        holder.indicator.setBackgroundColor(tagColor);
    }

    @Override
    public void reloadData() {
        new com.muzima.adapters.patients.PatientTagsListAdapter.BackgroundQueryTask().execute();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        PatientTag tag = getItem(position);

        List<PatientTag> selectedTags = patientController.getSelectedTags();
        if (position == 0) {
            selectedTags.clear();
        } else {
            if (selectedTags.contains(tag)) {
                selectedTags.remove(tag);
            } else {
                selectedTags.add(tag);
            }
        }

        notifyDataSetChanged();
        onTagsChanged();
    }

    private static class ViewHolder {
        View indicator;
        TextView name;
        FrameLayout tagColorIndicator;
        ImageView icon;
    }

    /**
     * Responsible to fetch all the tags that are available in the DB.
     */
    private class BackgroundQueryTask extends MuzimaAsyncTask<Void, Void, List<PatientTag>> {
        @Override
        protected void onPreExecute() {

        }

        @Override
        protected List<PatientTag> doInBackground(Void... voids) {
            List<PatientTag> allTags = null;
            try {
                allTags = patientController.getAllTags();
                Log.i(getClass().getSimpleName(), "#Tags: " + allTags.size());
            } catch (PatientController.PatientLoadException e) {
                Log.w(getClass().getSimpleName(), "Exception occurred while fetching tags", e);
            }
            return allTags;
        }

        @Override
        protected void onPostExecute(List<PatientTag> tags) {
            if(tags == null){
                Toast.makeText(getContext(), getContext().getString(R.string.error_tag_fetch), Toast.LENGTH_SHORT).show();
                return;
            }

            com.muzima.adapters.patients.PatientTagsListAdapter.this.clear();
            if (!tags.isEmpty()) {
                add(getAllTagsElement());
            }

            for (PatientTag tag : tags) {
                add(tag);
            }
            notifyDataSetChanged();
        }

        @Override
        protected void onBackgroundError(Exception e) {

        }

        private PatientTag getAllTagsElement() {
            PatientTag tag = new PatientTag();
            tag.setName("All");
            return tag;
        }
    }
}
