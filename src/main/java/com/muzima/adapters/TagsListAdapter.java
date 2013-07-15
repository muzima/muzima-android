package com.muzima.adapters;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.muzima.R;
import com.muzima.api.model.Tag;
import com.muzima.controller.FormController;
import com.muzima.listeners.DownloadListener;
import com.muzima.tasks.DownloadFormTask;

import java.util.List;

public class TagsListAdapter extends FormsListAdapter<Tag> implements DownloadListener<Integer[]>{
    private static final String TAG = "TagsListAdapter";
    private FormController formController;

    public TagsListAdapter(Context context, int textViewResourceId, FormController formController) {
        super(context, textViewResourceId);
        this.formController = formController;
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
            holder.icon = (ImageView) convertView
                    .findViewById(R.id.tag_icon);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        int tagColor = formController.getTagColor(getItem(position).getUuid());
        holder.indicator.setBackgroundColor(tagColor);
        holder.name.setText(getItem(position).getName());
        holder.icon.setBackgroundColor(tagColor);

        return convertView;
    }

    @Override
    public void reloadData() {
        new BackgroundQueryTask().execute();
    }

    @Override
    public void downloadTaskComplete(Integer[] result) {
        if(result[0] == DownloadFormTask.SUCCESS){
            reloadData();
        }
    }

    private static class ViewHolder {
        View indicator;
        TextView name;
        ImageView icon;
    }

    public class BackgroundQueryTask extends AsyncTask<Void, Void, List<Tag>> {

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
            for (Tag tag : forms) {
                add(tag);
            }
            notifyDataSetChanged();
            notifyEmptyDataListener(forms.size() == 0);
        }
    }
}
