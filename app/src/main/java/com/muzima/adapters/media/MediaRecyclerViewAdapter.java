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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Media;
import com.muzima.api.model.MediaCategory;
import com.muzima.controller.MediaController;
import com.muzima.utils.MuzimaPreferences;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class MediaRecyclerViewAdapter extends RecyclerView.Adapter<MediaRecyclerViewAdapter.ViewHolder> {
    private HashMap<MediaCategory, List<Media>> mediaCategoryListHashMap;
    Context context;
    int groupPosition;
    List<MediaCategory> mediaCategoryList;
    HashMap<String, Bitmap> bitmaps;

    public MediaRecyclerViewAdapter(Context context, HashMap<MediaCategory, List<Media>> child, int groupPosition, List<MediaCategory> mediaCategory, HashMap<String, Bitmap> bitmaps) {
        this.mediaCategoryListHashMap = child;
        this.context=context;
        this.groupPosition=groupPosition;
        this.mediaCategoryList=mediaCategory;
        this.bitmaps = bitmaps;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        CardView cardView;
        ImageView imageView;

        public ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.itemTextView);
            cardView=itemView.findViewById(R.id.cardView);
            imageView=itemView.findViewById(R.id.mediaImageView);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.media_card_view, parent, false);
        MediaRecyclerViewAdapter.ViewHolder vh = new MediaRecyclerViewAdapter.ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        Media media = (Media) getChild(groupPosition, position);
        String mimeType = media.getMimeType();
        String type = mimeType.substring(mimeType.lastIndexOf("/") + 1);
        final String mediaName = media.getName();
        holder.name.setText(mediaName);

        if(StringUtils.substringBefore(mimeType, "/").equals("image")){
            holder.imageView.setImageBitmap(bitmaps.get("images"));
        }else if(StringUtils.substringBefore(mimeType, "/").equals("video") || StringUtils.substringBefore(mimeType, "/").equals("audio")){
            holder.imageView.setImageBitmap(bitmaps.get("video"));
        }else if(type.equals("pdf")) {
            holder.imageView.setImageBitmap(bitmaps.get("pdf"));
        }else if(type.equals("msword") || type.equals("vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            holder.imageView.setImageBitmap(bitmaps.get("word"));
        }else if(type.equals("vnd.ms-powerpoint") || type.equals("vnd.openxmlformats-officedocument.presentationml.presentation")) {
            holder.imageView.setImageBitmap(bitmaps.get("powerpoint"));
        }else if(type.equals("vnd.ms-excel") || type.equals("vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            holder.imageView.setImageBitmap(bitmaps.get("excel"));
        }else{
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.splash_background);
            Bitmap thumbnail = ThumbnailUtils.extractThumbnail(bitmap,400,400);
            holder.imageView.setImageBitmap(thumbnail);
        }

        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startMediaDisplayActivity(media);
            }
        });
    }

    @Override
    public int getItemCount() {
        return this.mediaCategoryListHashMap.get(this.mediaCategoryList.get(groupPosition)).size();
    }

    public Object getChild(int groupPosition, int childPosititon) {
        return this.mediaCategoryListHashMap.get(this.mediaCategoryList.get(groupPosition)).get(childPosititon);
    }

    private void startMediaDisplayActivity(Media media) {
        String mimeType = media.getMimeType();
        String PATH = Objects.requireNonNull(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)).getAbsolutePath();
        File file = new File(PATH + "/" + media.getName() + "." + mimeType.substring(mimeType.lastIndexOf("/") + 1));
        if(mimeType.substring(mimeType.lastIndexOf("/") + 1).equals("vnd.ms-excel")){
            file = new File(PATH + "/"+media.getName()+".xls");
        }else if(mimeType.substring(mimeType.lastIndexOf("/") + 1).equals("vnd.openxmlformats-officedocument.spreadsheetml.sheet")){
            file = new File(PATH + "/"+media.getName()+".xlsx");
        }else if(mimeType.substring(mimeType.lastIndexOf("/") + 1).equals("msword")){
            file = new File(PATH + "/"+media.getName()+".doc");
        }else if(mimeType.substring(mimeType.lastIndexOf("/") + 1).equals("vnd.openxmlformats-officedocument.wordprocessingml.document")){
            file = new File(PATH + "/"+media.getName()+".docx");
        }else if(mimeType.substring(mimeType.lastIndexOf("/") + 1).equals("vnd.ms-powerpoint")){
            file = new File(PATH + "/"+media.getName()+".ppt");
        }else if(mimeType.substring(mimeType.lastIndexOf("/") + 1).equals("vnd.openxmlformats-officedocument.presentationml.presentation")){
            file = new File(PATH + "/"+media.getName()+".pptx");
        }
        if(!file.exists()){
            Toast.makeText(context, context.getString(R.string.info_no_media_not_available), Toast.LENGTH_LONG).show();
        } else {
            String commaSeparatedMediaUuids = media.getUuid();
            String recentMedia = MuzimaPreferences.getStringPreference(context, context.getResources().getString(R.string.preference_recently_viewed_media), com.muzima.utils.StringUtils.EMPTY);
            if(!com.muzima.utils.StringUtils.isEmpty(recentMedia)) {
                String[] mediaUuids = recentMedia.split(",");
                int j = 0;
                for (int i = 0; i < mediaUuids.length; i++) {
                    String mediaUuid = mediaUuids[i];
                    if(!mediaUuid.equals(media.getUuid())) {
                        MediaController mediaController = ((MuzimaApplication) context.getApplicationContext()).getMediaController();
                        Media med = null;
                        if (j < 2) {
                            try {
                                med = mediaController.getMediaByUuid(mediaUuid);
                                if (med != null) {
                                    commaSeparatedMediaUuids = commaSeparatedMediaUuids.concat(",").concat(mediaUuid);
                                }
                            } catch (MediaController.MediaFetchException e) {
                                Log.e(getClass().getSimpleName(), "Encountered Error while fetching media");
                            }
                        }
                        j++;
                    }
                }
            }

            String disclaimerKey = context.getResources().getString(R.string.preference_recently_viewed_media);
            MuzimaPreferences.setStringPreference(context, disclaimerKey, commaSeparatedMediaUuids);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri fileUri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", file);
            intent.setData(fileUri);
            List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                context.grantUriPermission(context.getPackageName() + ".provider", fileUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
        }
    }
}