/*
 * Copyright (c) Vanderbilt University Medical Center and Lambda Informatics.
 * All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.adapters.media;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.media.ThumbnailUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.R;
import com.muzima.api.model.Media;
import com.muzima.api.model.MediaCategory;

import java.util.HashMap;
import java.util.List;

public class MediaAdapter extends BaseExpandableListAdapter {

    private final Context context;
    private final List<MediaCategory> mediaCategoryList; 
    private final HashMap<MediaCategory, List<Media>> mediaCategoryListHashMap;
    RecyclerView recyclerView;
    private HashMap<String, Bitmap> bitmaps = new HashMap<>();;

    public MediaAdapter(Context context, List<MediaCategory> mediaCategoryList, HashMap<MediaCategory, List<Media>> listChildData) {
        this.context = context;
        this.mediaCategoryList = mediaCategoryList;
        this.mediaCategoryListHashMap = listChildData;

        bitmaps.put("images", ThumbnailUtils.extractThumbnail(BitmapFactory.decodeResource(context.getResources(), R.drawable.image_icon), 150, 150));
        bitmaps.put("pdf", ThumbnailUtils.extractThumbnail(BitmapFactory.decodeResource(context.getResources(), R.drawable.pdf_file_icon), 150, 150));
        bitmaps.put("excel", ThumbnailUtils.extractThumbnail(BitmapFactory.decodeResource(context.getResources(), R.drawable.excell_icon), 150, 150));
        bitmaps.put("word", ThumbnailUtils.extractThumbnail(BitmapFactory.decodeResource(context.getResources(), R.drawable.ms_word_document), 150, 150));
        bitmaps.put("powerpoint", ThumbnailUtils.extractThumbnail(BitmapFactory.decodeResource(context.getResources(), R.drawable.ms_powerpoint), 150, 150));
        bitmaps.put("video", ThumbnailUtils.extractThumbnail(BitmapFactory.decodeResource(context.getResources(), R.drawable.videos_audio_icon), 150, 150));
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
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.media_item, parent,
                    false);
            recyclerView = (RecyclerView) convertView.findViewById(R.id.recyclerview);
            MediaRecyclerViewAdapter sbc=new MediaRecyclerViewAdapter(context,
                    mediaCategoryListHashMap,groupPosition,mediaCategoryList, bitmaps);
            recyclerView.setLayoutManager(new GridLayoutManager(context,3));
            recyclerView.setAdapter(sbc);
        }

        convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.media_item,parent,false);
        recyclerView = (RecyclerView) convertView.findViewById(R.id.recyclerview);
        MediaRecyclerViewAdapter sbc=new MediaRecyclerViewAdapter(context,
                mediaCategoryListHashMap,groupPosition,mediaCategoryList, bitmaps);
        recyclerView.setLayoutManager(new GridLayoutManager(context,3));
        recyclerView.setAdapter(sbc);
        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return 1;
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
            convertView = infalInflater.inflate(R.layout.media_category_title, null);
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
