package com.muzima.controller;

import static com.muzima.api.model.APIName.DOWNLOAD_MEDIA_CATEGORIES;


import com.muzima.api.model.LastSyncTime;
import com.muzima.api.model.MediaCategory;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.api.service.MediaCategoryService;
import com.muzima.service.SntpService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class MediaCategoryController {
    private final MediaCategoryService mediaCategoryService;
    private final LastSyncTimeService lastSyncTimeService;
    private final SntpService sntpService;
    
    public MediaCategoryController(MediaCategoryService mediaCategoryService, LastSyncTimeService lastSyncTimeService, SntpService sntpService) {
        this.mediaCategoryService = mediaCategoryService;
        this.lastSyncTimeService = lastSyncTimeService;
        this.sntpService = sntpService;
    }

    public List<MediaCategory> downloadMediaCategory(List<String> mediaCategoryUuids, boolean isDelta) throws MediaCategoryController.MediaCategoryDownloadException {
        try {
            LastSyncTime lastSyncTime = lastSyncTimeService.getFullLastSyncTimeInfoFor(DOWNLOAD_MEDIA_CATEGORIES);
            Date lastSyncDate = null;
            if (lastSyncTime != null) {
                lastSyncDate = lastSyncTime.getLastSyncDate();
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
            List<MediaCategory> mediaCategory = mediaCategoryService.getMediaCategories();
            Collections.sort(mediaCategory, mediaCategoryOrderComparator);
            return mediaCategory;
        } catch (IOException e) {
            throw new MediaCategoryController.MediaCategoryFetchException(e);
        }
    }

    public void saveMediaCategory(List<MediaCategory> mediaCategories) throws MediaCategoryController.MediaCategorySaveException {
        try {
            mediaCategoryService.saveMediaCategories(mediaCategories);
        } catch (IOException e) {
            throw new MediaCategoryController.MediaCategorySaveException(e);
        }
    }

    public void updateMediaCategory(MediaCategory mediaCategory) throws MediaCategoryController.MediaCategorySaveException {
        try {
            mediaCategoryService.updateMediaCategory(mediaCategory);
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

    private final Comparator<MediaCategory> mediaCategoryOrderComparator = new Comparator<MediaCategory>() {
        @Override
        public int compare(MediaCategory lhs, MediaCategory rhs) {
            return lhs.getOrder()-rhs.getOrder();
        }
    };

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
