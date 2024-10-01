/*
 * Copyright (c) Vanderbilt University Medical Center and Lambda Informatics.
 * All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.media.MediaAdapter;
import com.muzima.api.model.Media;
import com.muzima.api.model.MediaCategory;
import com.muzima.controller.MediaCategoryController;
import com.muzima.controller.MediaController;
import com.muzima.utils.MuzimaPreferences;
import com.muzima.utils.StringUtils;
import com.muzima.view.custom.ActivityWithBottomNavigation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MediaActivity extends ActivityWithBottomNavigation {

    private MediaAdapter listAdapter;
    private ExpandableListView expListView;
    private List<MediaCategory> mediaCategories;
    private HashMap<MediaCategory, List<Media>> mediaListMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media);

        loadBottomNavigation();

        expListView = findViewById(R.id.elvMedia);
        prepareListData();

        listAdapter = new MediaAdapter(this, mediaCategories, mediaListMap);
        expListView.setAdapter(listAdapter);

        if(mediaCategories.size()>0) {
            for(int i=0; i < listAdapter.getGroupCount(); i++)
                expListView.expandGroup(i);
        }else{
            LinearLayout noDataLayout = findViewById(R.id.no_data_layout);
            noDataLayout.setVisibility(View.VISIBLE);
            expListView.setVisibility(View.GONE);
        }

        logEvent("VIEW_MEDIA");
    }

    @Override
    protected int getBottomNavigationMenuItemId() {
        return R.id.media;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void prepareListData() {
        mediaCategories = new ArrayList<>();
        mediaListMap = new HashMap<>();
        
        // Adding child data. Get all media categories
        MediaCategoryController mediaCategoryController = ((MuzimaApplication) getApplication()).getMediaCategoryController();
        MediaController mediaController = ((MuzimaApplication) getApplication()).getMediaController();
        try {
            String recentMedia = MuzimaPreferences.getStringPreference(getApplicationContext(), this.getResources().getString(R.string.preference_recently_viewed_media), StringUtils.EMPTY);
            if(!StringUtils.isEmpty(recentMedia)) {
                MediaCategory recentlyViewedMediaCategory = new MediaCategory();
                recentlyViewedMediaCategory.setUuid("5b4574a4-1b3b-4b95-9ba2-821771af47d1");
                recentlyViewedMediaCategory.setName(this.getResources().getString(R.string.general_recently_viewed));

                String[] mediaUuids = recentMedia.split(",");
                List<Media> recentlyViewedMediaList = new ArrayList<>();
                for (int i = 0; i < mediaUuids.length; i++) {
                    String mediaUuid = mediaUuids[i];
                    Media media = mediaController.getMediaByUuid(mediaUuid);
                    if(media != null) {
                        recentlyViewedMediaList.add(media);
                    }
                }

                if(recentlyViewedMediaList.size()>0){
                    mediaCategories.add(recentlyViewedMediaCategory);
                    mediaListMap.put(recentlyViewedMediaCategory, recentlyViewedMediaList);
                }
            }

            List<MediaCategory> mediaCategoryList = mediaCategoryController.getMediaCategories();
            for(MediaCategory mediaCategory:mediaCategoryList){
                List<Media> mediaList = mediaController.getMediaByCategoryUuid(mediaCategory.getUuid());
                if(mediaList.size()>0) {
                    mediaCategories.add(mediaCategory);
                    mediaListMap.put(mediaCategory, mediaList);
                }
            }
        } catch (MediaCategoryController.MediaCategoryFetchException e) {
            Log.e(getClass().getSimpleName(),"Encountered error while fetching media category ",e);
        } catch (MediaController.MediaFetchException e) {
            Log.e(getClass().getSimpleName(),"Encountered error while fetching media ",e);
        }
    }
}
