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

import androidx.core.content.FileProvider;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.MediaAdapter;
import com.muzima.api.model.Media;
import com.muzima.api.model.MediaCategory;
import com.muzima.controller.MediaCategoryController;
import com.muzima.controller.MediaController;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class MediaActivity extends BaseActivity{

    private MediaAdapter listAdapter;
    private ExpandableListView expListView;
    private List<MediaCategory> mediaCategories;
    private HashMap<MediaCategory, List<Media>> mediaListMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media);

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

        expListView.expandGroup(0);

        // Listview on child click listener
        expListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                startHelpContentDisplayActivity(
                        mediaListMap.get(mediaCategories.get(groupPosition)).get(childPosition).getName());
                return false;
            }
        });
        logEvent("VIEW_HELP");
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
                mediaCategories.add(mediaCategory);
                List<Media> mediaList = mediaController.getMediaByCategoryID(mediaCategory.getId());
                mediaListMap.put(mediaCategory,mediaList);
            }

        } catch (MediaCategoryController.MediaCategoryFetchException e) {
            Log.e(getClass().getSimpleName(),"Encountered error while fetching media category ",e);
        } catch (MediaController.MediaFetchException e) {
            Log.e(getClass().getSimpleName(),"Encountered error while fetching media ",e);
        }
    }

    private void startHelpContentDisplayActivity(String filePath) {
        String PATH = Objects.requireNonNull(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)).getAbsolutePath();
        File file = new File(PATH + "/"+filePath);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        if (Build.VERSION.SDK_INT >= 24) {
            Uri fileUri = FileProvider.getUriForFile(this, this.getApplicationContext().getPackageName() + ".provider", file);
            intent.setData(fileUri);
            List<ResolveInfo> resInfoList = this.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                this.grantUriPermission(this.getApplicationContext().getPackageName() + ".provider", fileUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } else {
            intent.setAction(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
            intent.setData(Uri.fromFile(file));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        startActivity(intent);
    }

}
