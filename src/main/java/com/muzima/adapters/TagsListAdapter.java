package com.muzima.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.muzima.R;
import com.muzima.api.model.Tag;
import com.muzima.controller.FormController;
import com.muzima.listeners.DownloadListener;
import com.muzima.tasks.forms.DownloadFormTask;

import java.util.List;

public class TagsListAdapter extends FormsListAdapter<Tag> implements DownloadListener<Integer[]>, AdapterView.OnItemClickListener {
    private static final String TAG = "TagsListAdapter";
    private FormController formController;
    private TagsChangedListener tagsChangedListener;

    public TagsListAdapter(Context context, int textViewResourceId, FormController formController) {
        super(context, textViewResourceId);
        this.formController = formController;
    }

    public interface TagsChangedListener{
        public void onTagsChanged();
    }

    public void setTagsChangedListener(TagsChangedListener tagsChangedListener) {
        this.tagsChangedListener = tagsChangedListener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            convertView = layoutInflater.inflate(
                    R.layout.item_tags_list, parent, false);
            holder = new ViewHolder();
            holder.indicator = convertView.findViewById(R.id.tag_indicator);
            holder.name = (TextView) convertView
                    .findViewById(R.id.tag_name);
            holder.tagColorIndicator = (FrameLayout) convertView
                    .findViewById(R.id.tag_color_indicator);
            holder.icon = (ImageView) convertView.findViewById(R.id.tag_icon);
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
    public void downloadTaskComplete(Integer[] result) {
        if (result[0] == DownloadFormTask.SUCCESS) {
            reloadData();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Tag tag = getItem(position);

        List<Tag> selectedTags = formController.getSelectedTags();
        if(position == 0){
            selectedTags.clear();
        }else{
            if(selectedTags.contains(tag)){
                selectedTags.remove(tag);
            }else{
                selectedTags.add(tag);
            }
        }
//        formController.setSelectedTags(selectedTags);
        notifyDataSetChanged();
        if(tagsChangedListener != null){
            tagsChangedListener.onTagsChanged();
        }
    }

    private static class ViewHolder {
        View indicator;
        TextView name;
        FrameLayout tagColorIndicator;
        ImageView icon;
    }

    private class BackgroundQueryTask extends AsyncTask<Void, Void, List<Tag>> {
        @Override
        protected List<Tag> doInBackground(Void... voids) {
            List<Tag> allTags = null;
            try {
                allTags = formController.getAllTags();
                Log.i(TAG, "#Tags: " + allTags.size());
            } catch (FormController.FormFetchException e) {
                Log.w(TAG, "Exception occurred while fetching tags" + e);
            }
            return allTags;
        }

        @Override
        protected void onPostExecute(List<Tag> forms) {
            TagsListAdapter.this.clear();

            add(getAllTagsElement());

            for (Tag tag : forms) {
                add(tag);
            }
            notifyDataSetChanged();
            notifyEmptyDataListener(forms.size() == 0);
        }

        private Tag getAllTagsElement() {
            Tag tag = new Tag();
            tag.setName("All");
            return tag;
        }
    }
}
