package com.muzima.view;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.MediaAdapter;
import com.muzima.api.model.Media;
import com.muzima.api.model.MediaCategory;
import com.muzima.controller.MediaCategoryController;
import com.muzima.controller.MediaController;
import com.muzima.view.custom.ActivityWithBottomNavigation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

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

        expListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            int previousGroup = -1;

            @Override
            public void onGroupExpand(int groupPosition) {
                if (groupPosition != previousGroup)
                    expListView.collapseGroup(previousGroup);
                previousGroup = groupPosition;
            }
        });

        if(mediaCategories.size()>0) {
            expListView.expandGroup(0);
        }else{
            LinearLayout noDataLayout = findViewById(R.id.no_data_layout);
            noDataLayout.setVisibility(View.VISIBLE);
            expListView.setVisibility(View.GONE);
        }
        // Listview on child click listener
        expListView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            startMediaDisplayActivity(
                    mediaListMap.get(mediaCategories.get(groupPosition)).get(childPosition));
            return false;
        });
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

    private void startMediaDisplayActivity(Media media) {
        String mimeType = media.getMimeType();
        String PATH = Objects.requireNonNull(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)).getAbsolutePath();
        File file = new File(PATH + "/"+media.getName()+"."+mimeType.substring(mimeType.lastIndexOf("/") + 1));
        if(!file.exists()){
            Toast.makeText(this, getString(R.string.info_no_media_not_available), Toast.LENGTH_LONG).show();
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri fileUri = FileProvider.getUriForFile(this, this.getApplicationContext().getPackageName() + ".provider", file);
            intent.setData(fileUri);
            List<ResolveInfo> resInfoList = this.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                this.grantUriPermission(this.getApplicationContext().getPackageName() + ".provider", fileUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        }
    }
}
