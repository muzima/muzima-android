/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */
package com.muzima.adapters.forms;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
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
import com.muzima.api.model.Tag;
import com.muzima.controller.FormController;

import java.util.List;

/**
 * Responsible to list down the tags in the TagDrawer.
 */
public class TagsListAdapter extends ListAdapter<Tag> implements AdapterView.OnItemClickListener {

    private final FormController formController;
    private TagsChangedListener tagsChangedListener;

    public TagsListAdapter(Context context, int textViewResourceId, FormController formController) {
        super(context, textViewResourceId);
        this.formController = formController;
    }

    public interface TagsChangedListener {
        void onTagsChanged();
    }

    public void setTagsChangedListener(TagsChangedListener tagsChangedListener) {
        this.tagsChangedListener = tagsChangedListener;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(
                    R.layout.item_tags_list, parent, false);
            holder = new ViewHolder();
            holder.indicator = convertView.findViewById(R.id.tag_indicator);
            holder.name = convertView
                    .findViewById(R.id.tag_name);
            holder.tagColorIndicator = convertView
                    .findViewById(R.id.tag_color_indicator);
            holder.icon = convertView.findViewById(R.id.tag_icon);
            convertView.setTag(holder);
        }

        holder = (ViewHolder) convertView.getTag();
        int tagColor = formController.getTagColor(getItem(position).getUuid());
        if (position == 0) {
            tagColor = Color.parseColor("#333333");
        }

        Resources resources = getContext().getResources();
        List<Tag> selectedTags = formController.getSelectedTags();
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

    private void markItemUnselected(ViewHolder holder, Resources resources) {
        holder.icon.setImageDrawable(resources.getDrawable(R.drawable.ic_cancel));
        int drawerColor = resources.getColor(R.color.drawer_background);
        holder.indicator.setBackgroundColor(drawerColor);
    }

    private void markItemSelected(ViewHolder holder, int tagColor, Resources resources) {
        holder.icon.setImageDrawable(resources.getDrawable(R.drawable.ic_accept));
        holder.indicator.setBackgroundColor(tagColor);
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask().execute();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Tag tag = getItem(position);

        List<Tag> selectedTags = formController.getSelectedTags();
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
        if (tagsChangedListener != null) {
            tagsChangedListener.onTagsChanged();
        }
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
    private class BackgroundQueryTask extends AsyncTask<Void, Void, List<Tag>> {
        @Override
        protected List<Tag> doInBackground(Void... voids) {
            List<Tag> allTags = null;
            try {
                allTags = formController.getAllTagsExcludingRegistrationTag();
                Log.i(getClass().getSimpleName(), "#Tags: " + allTags.size());
            } catch (FormController.FormFetchException e) {
                Log.w(getClass().getSimpleName(), "Exception occurred while fetching tags", e);
            }
            return allTags;
        }

        @Override
        protected void onPostExecute(List<Tag> tags) {
            if(tags == null){
                Toast.makeText(getContext(), getContext().getString(R.string.error_tag_fetch), Toast.LENGTH_SHORT).show();
                return;
            }

            TagsListAdapter.this.clear();

            if (!tags.isEmpty()) {
                add(getAllTagsElement());
            }

            for (Tag tag : tags) {
                add(tag);
            }
            notifyDataSetChanged();
        }


        private Tag getAllTagsElement() {
            Tag tag = new Tag();
            tag.setName("All");
            return tag;
        }
    }
}
