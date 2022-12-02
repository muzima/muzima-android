package com.muzima.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.muzima.R;
import com.muzima.api.model.Media;
import com.muzima.api.model.MediaCategory;

import java.util.HashMap;
import java.util.List;

public class MediaAdapter extends BaseExpandableListAdapter {

    private final Context context;
    private final List<MediaCategory> mediaCategoryList; 
    private final HashMap<MediaCategory, List<Media>> mediaCategoryListHashMap;

    public MediaAdapter(Context context, List<MediaCategory> mediaCategoryList, HashMap<MediaCategory, List<Media>> listChildData) {
        this.context = context;
        this.mediaCategoryList = mediaCategoryList;
        this.mediaCategoryListHashMap = listChildData;
    }

    @Override
    public Object getChild(int groupPosition, int childPosititon) {
        return this.mediaCategoryListHashMap.get(this.mediaCategoryList.get(groupPosition)).get(childPosititon);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        Media media = (Media) getChild(groupPosition, childPosition);
        final String childText = media.getName();
        final String description = media.getDescription();

        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.media_list_item, null);
        }

        TextView tvName = convertView.findViewById(R.id.tvName);
        TextView tvDescription = convertView.findViewById(R.id.tvDescription);
        tvName.setText(childText);
        tvDescription.setText(description);
        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return this.mediaCategoryListHashMap.get(this.mediaCategoryList.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return this.mediaCategoryList.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return this.mediaCategoryList.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        MediaCategory mediaCategory = (MediaCategory) getGroup(groupPosition);
        String headerTitle = mediaCategory.getName();

        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.activity_help_list_group, null);
        }

        TextView mediaCategoryHeader = convertView.findViewById(R.id.lblListHeader);
        mediaCategoryHeader.setTypeface(null, Typeface.BOLD);
        mediaCategoryHeader.setText(headerTitle);

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
