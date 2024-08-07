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

import static com.muzima.utils.Constants.FGH.TagsUuids.ALL_CONTACTS_VISITED_TAG_UUID;
import static com.muzima.utils.Constants.FGH.TagsUuids.ALREADY_ASSIGNED_TAG_UUID;
import static com.muzima.utils.Constants.FGH.TagsUuids.AWAITING_ASSIGNMENT_TAG_UUID;
import static com.muzima.utils.Constants.FGH.TagsUuids.HAS_SEXUAL_PARTNER_TAG_UUID;
import static com.muzima.utils.Constants.FGH.TagsUuids.NOT_ALL_CONTACTS_VISITED_TAG_UUID;
import static com.muzima.utils.Constants.FGH.TagsUuids.NO_INTERVENTION_NEEDED_UUID;

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
import com.muzima.controller.MuzimaSettingController;
import com.muzima.controller.PatientController;
import com.muzima.tasks.MuzimaAsyncTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Responsible to list down the tags in the TagDrawer.
 */
public class PatientTagsListAdapter extends ListAdapter<PatientTag> implements AdapterView.OnItemClickListener {

    private final PatientController patientController;
    PatientsLocalSearchAdapter patientsLocalSearchAdapter;
    private final  MuzimaSettingController muzimaSettingController;

    public PatientTagsListAdapter(Context context, int textViewResourceId, PatientController patientController, MuzimaSettingController muzimaSettingController) {
        super(context, textViewResourceId);
        this.patientController = patientController;
        this.muzimaSettingController = muzimaSettingController;
        patientsLocalSearchAdapter = new PatientsLocalSearchAdapter(context, patientController, null,null, null, muzimaSettingController);
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
            holder.description = convertView
                    .findViewById(R.id.tag_description);
            holder.tagColorIndicator = convertView
                    .findViewById(R.id.tag_color_indicator);
            holder.icon = convertView.findViewById(R.id.tag_icon);
            convertView.setTag(holder);
        }

        holder = (com.muzima.adapters.patients.PatientTagsListAdapter.ViewHolder) convertView.getTag();
        PatientTag patientTag = getItem(position);
        int tagColor = patientController.getTagColor(patientTag.getUuid());
        if (position == 0) {
            tagColor = Color.parseColor("#333333");
        }

        Resources resources = getContext().getResources();
        List<PatientTag> selectedTags = patientController.getSelectedTags();
        if (selectedTags.isEmpty()) {
            if (selectedTags.contains(patientTag)) {
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
        holder.name.setText(patientTag.getName());

        if(ALREADY_ASSIGNED_TAG_UUID.equals(patientTag.getUuid())){
            holder.description.setText(getContext().getString(R.string.general_already_assigned));
        } else if(AWAITING_ASSIGNMENT_TAG_UUID.equals(patientTag.getUuid())){
            holder.description.setText(getContext().getString(R.string.general_awaiting_assignment));
        } else if(HAS_SEXUAL_PARTNER_TAG_UUID.equals(patientTag.getUuid())){
            holder.description.setText(getContext().getString(R.string.general_has_sexual_partner));
        } else if(ALL_CONTACTS_VISITED_TAG_UUID.equals(patientTag.getUuid())){
            holder.description.setText(getContext().getString(R.string.general_all_contacts_visited));
        }else if(NOT_ALL_CONTACTS_VISITED_TAG_UUID.equals(patientTag.getUuid())){
            holder.description.setText(getContext().getString(R.string.general_not_all_contacts_visited));
        }else if(NO_INTERVENTION_NEEDED_UUID.equals(patientTag.getUuid())){
            holder.description.setText(getContext().getString(R.string.general_no_intervention_needed));
        }else if(patientTag.getDescription() != null){
            holder.description.setText(patientTag.getDescription());
        }
        return convertView;
    }

    private void markItemUnselected(com.muzima.adapters.patients.PatientTagsListAdapter.ViewHolder holder, Resources resources) {
        holder.icon.setImageDrawable(getContext().getDrawable(R.drawable.ic_action_close));
        int drawerColor = getContext().getColor(R.color.drawer_background);
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
        TextView description;
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

            PatientTag AATag = null;
            PatientTag ALTag = null;
            PatientTag NATag = null;
            PatientTag PTag = null;
            PatientTag VTag = null;
            PatientTag NVTag = null;
            List<PatientTag> otherTags = new ArrayList<>();
            for (PatientTag tag : tags) {
                if(tag.getUuid().equals(AWAITING_ASSIGNMENT_TAG_UUID))
                    AATag = tag;
                else if(tag.getUuid().equals(ALREADY_ASSIGNED_TAG_UUID))
                    ALTag = tag;
                else if(tag.getUuid().equals(HAS_SEXUAL_PARTNER_TAG_UUID))
                    PTag = tag;
                else if(tag.getUuid().equals(ALL_CONTACTS_VISITED_TAG_UUID))
                    VTag = tag;
                else if(tag.getUuid().equals(NOT_ALL_CONTACTS_VISITED_TAG_UUID))
                    NVTag = tag;
                else if(tag.getUuid().equals(NO_INTERVENTION_NEEDED_UUID))
                    NATag = tag;
                else
                    otherTags.add(tag);
            }

            Collections.sort(otherTags, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));

            if(AATag!=null)
                add(AATag);
            if(ALTag!=null)
                add(ALTag);
            if(NATag!=null)
                add(NATag);
            if(PTag!=null)
                add(PTag);
            if(VTag!=null)
                add(VTag);
            if(NVTag!=null)
                add(NVTag);
            if(otherTags.size()>0)
                addAll(otherTags);

            notifyDataSetChanged();
        }

        @Override
        protected void onBackgroundError(Exception e) {

        }

        private PatientTag getAllTagsElement() {
            PatientTag tag = new PatientTag();
            tag.setName(getContext().getString(R.string.general_all));
            tag.setDescription(getContext().getString(R.string.general_all_clients));
            return tag;
        }
    }
}
