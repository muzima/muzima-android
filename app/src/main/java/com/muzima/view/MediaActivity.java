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
