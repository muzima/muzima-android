/*
 * Copyright (c) Vanderbilt University Medical Center and Lambda Informatics.
 * All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.controller;

import static com.muzima.api.model.APIName.DOWNLOAD_MEDIA_CATEGORIES;


import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.api.model.LastSyncTime;
import com.muzima.api.model.MediaCategory;
import com.muzima.api.model.SetupConfigurationTemplate;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.api.service.MediaCategoryService;
import com.muzima.api.service.SetupConfigurationService;
import com.muzima.service.SntpService;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class MediaCategoryController {
    private final MediaCategoryService mediaCategoryService;
    private final LastSyncTimeService lastSyncTimeService;
    private final SntpService sntpService;
    private final SetupConfigurationService setupConfigurationService;
    private final MuzimaApplication muzimaApplication;
    
    public MediaCategoryController(MediaCategoryService mediaCategoryService, LastSyncTimeService lastSyncTimeService,
                                   SntpService sntpService, SetupConfigurationService setupConfigurationService, MuzimaApplication muzimaApplication) {
        this.mediaCategoryService = mediaCategoryService;
        this.lastSyncTimeService = lastSyncTimeService;
        this.sntpService = sntpService;
        this.setupConfigurationService = setupConfigurationService;
        this.muzimaApplication = muzimaApplication;
    }

    public List<MediaCategory> downloadMediaCategory(List<String> mediaCategoryUuids, boolean isDelta)
            throws MediaCategoryController.MediaCategoryDownloadException {
        try {
            LastSyncTime lastSyncTime = lastSyncTimeService.getFullLastSyncTimeInfoFor(DOWNLOAD_MEDIA_CATEGORIES);
            Date lastSyncDate = null;
            if(isDelta) {
                if (lastSyncTime != null) {
                    lastSyncDate = lastSyncTime.getLastSyncDate();
                }
            }
            List<MediaCategory> mediaCategory =  mediaCategoryService.downloadMediaCategories(mediaCategoryUuids, lastSyncDate);
            LastSyncTime newLastSyncTime = new LastSyncTime(DOWNLOAD_MEDIA_CATEGORIES, sntpService.getTimePerDeviceTimeZone());
            lastSyncTimeService.saveLastSyncTime(newLastSyncTime);
            return  mediaCategory;
        } catch (IOException e) {
            throw new MediaCategoryController.MediaCategoryDownloadException(e);
        }
    }

    public List<MediaCategory> getMediaCategories() throws MediaCategoryController.MediaCategoryFetchException {
        try {
            List<MediaCategory> mediaCategory = getMediaCategoryAsPerConfigOrder();
            return mediaCategory;
        } catch (IOException e) {
            throw new MediaCategoryController.MediaCategoryFetchException(e);
        }
    }


    public ArrayList<MediaCategory> getMediaCategoryAsPerConfigOrder() throws IOException {
        List<SetupConfigurationTemplate> setupConfigurationTemplates = setupConfigurationService.getSetupConfigurationTemplates();

        SetupConfigurationTemplate activeSetupConfig = null;
        try{
            activeSetupConfig = muzimaApplication.getSetupConfigurationController().getActiveSetupConfigurationTemplate();
        } catch (SetupConfigurationController.SetupConfigurationFetchException e) {
            Log.d(getClass().getSimpleName(), "Cannot load active config",e);
        }

        if(activeSetupConfig == null){
            List<SetupConfigurationTemplate> configurationTemplates = setupConfigurationService.getSetupConfigurationTemplates();
            if(configurationTemplates.size() > 0)
                activeSetupConfig = configurationTemplates.get(0);
        }

        ArrayList<MediaCategory> mediaCategories = new ArrayList<>();

        if(activeSetupConfig != null) {
            org.json.JSONObject object = null;
            try {
                object = new org.json.JSONObject(activeSetupConfig.getConfigJson());
                org.json.JSONObject categories = object.getJSONObject("config");
                org.json.JSONArray categoriesArray = categories.getJSONArray("mediaCategories");
                for (int i = 0; i < categoriesArray.length(); i++) {
                    org.json.JSONObject categoriesObject = categoriesArray.getJSONObject(i);
                    MediaCategory mediaCategory = mediaCategoryService.getMediaCategoriesByUuid(categoriesObject.get("uuid").toString());
                    if (mediaCategory != null) {
                        mediaCategories.add(mediaCategory);
                    } else {
                        Log.d(getClass().getSimpleName(), "Could not find media category with uuid = " + categoriesObject.get("uuid").toString() +
                                " specified in setup config with uuid = " + activeSetupConfig.getUuid());
                    }
                }
            } catch (JSONException e) {
                Log.d(getClass().getSimpleName(), "Encountered JsonException while sorting media categories");
            }
        }

        return mediaCategories;
    }

    public void saveMediaCategory(List<MediaCategory> mediaCategories) throws MediaCategoryController.MediaCategorySaveException {
        try {
            mediaCategoryService.saveMediaCategories(mediaCategories);
        } catch (IOException e) {
            throw new MediaCategoryController.MediaCategorySaveException(e);
        }
    }

    public void updateMediaCategory(List<MediaCategory> mediaCategory) throws MediaCategoryController.MediaCategorySaveException {
        try {
            mediaCategoryService.updateMediaCategory(mediaCategory);
        } catch (IOException e) {
            throw new MediaCategoryController.MediaCategorySaveException(e);
        }
    }

    public void deleteMediaCategory(List<String> uuids) throws MediaCategoryController.MediaCategorySaveException {
        try {
            List<MediaCategory> mediaCategoryList = new ArrayList<>();
            for(String uuid:uuids){
                MediaCategory mediaCategory = mediaCategoryService.getMediaCategoriesByUuid(uuid);
                if(mediaCategory != null){
                    mediaCategoryList.add(mediaCategory);
                }
            }
            if(mediaCategoryList.size()>0)
                mediaCategoryService.deleteMediaCategories(mediaCategoryList);
        } catch (IOException e) {
            throw new MediaCategoryController.MediaCategorySaveException(e);
        }
    }

    public static class MediaCategoryFetchException extends Throwable {
        MediaCategoryFetchException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class MediaCategorySaveException extends Throwable {
        MediaCategorySaveException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class MediaCategoryDownloadException extends Throwable {
        MediaCategoryDownloadException(Throwable throwable) {
            super(throwable);
        }
    }
}
