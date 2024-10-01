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

import static com.muzima.api.model.APIName.DOWNLOAD_MEDIA;

import android.os.Environment;

import com.muzima.api.model.LastSyncTime;
import com.muzima.api.model.Media;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.api.service.MediaService;
import com.muzima.service.SntpService;
import com.muzima.utils.MemoryUtil;

import org.apache.lucene.queryParser.ParseException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class MediaController {
    private final MediaService mediaService;
    private final LastSyncTimeService lastSyncTimeService;
    private final SntpService sntpService;

    public MediaController(MediaService mediaService, LastSyncTimeService lastSyncTimeService, SntpService sntpService) {
        this.mediaService = mediaService;
        this.lastSyncTimeService = lastSyncTimeService;
        this.sntpService = sntpService;
    }

    public List<Media> downloadMedia(List<String> mediaCategoryUuids, boolean isDeltaSync) throws MediaController.MediaDownloadException {
        try {
            LastSyncTime lastSyncTime = lastSyncTimeService.getFullLastSyncTimeInfoFor(DOWNLOAD_MEDIA);
            Date lastSyncDate = null;
            if(isDeltaSync) {
                if (lastSyncTime != null) {
                    lastSyncDate = lastSyncTime.getLastSyncDate();
                }
            }
            List<Media> mediaList = mediaService.downloadMedia(lastSyncDate, mediaCategoryUuids);
            long mediaSize = MemoryUtil.getTotalMediaFileSize(mediaList);
            long availableSpace = MemoryUtil.getAvailableInternalMemorySize();
            if(mediaSize<availableSpace) {
                LastSyncTime newLastSyncTime = new LastSyncTime(DOWNLOAD_MEDIA, sntpService.getTimePerDeviceTimeZone());
                lastSyncTimeService.saveLastSyncTime(newLastSyncTime);
            }else{
                mediaList = new ArrayList<>();
            }
            return  mediaList;
        } catch (IOException e) {
            throw new MediaController.MediaDownloadException(e);
        }
    }

    public List<Media> getMedia() throws MediaController.MediaFetchException {
        try {
            List<Media> media = mediaService.getMedia();
            return media;
        } catch (IOException e) {
            throw new MediaController.MediaFetchException(e);
        }
    }

    public Media getMediaByUuid(String mediaUuid) throws MediaController.MediaFetchException {
        try {
            Media media = mediaService.getMediaByUuid(mediaUuid);
            return media;
        } catch (IOException e) {
            throw new MediaController.MediaFetchException(e);
        }
    }

    public List<Media> getMediaByCategoryUuid(String categoryUuid) throws MediaController.MediaFetchException {
        try {
            List<Media> media = mediaService.getMediaByCategoryUuid(categoryUuid);
            Collections.sort(media, mediaOrderComparator);
            return media;
        } catch (IOException | ParseException e) {
            throw new MediaController.MediaFetchException(e);
        }
    }

    public void saveMedia(List<Media> media) throws MediaController.MediaSaveException {
        try {
            mediaService.saveMedia(media);
        } catch (IOException e) {
            throw new MediaController.MediaSaveException(e);
        }
    }

    public void updateMedia(List<Media> media) throws MediaController.MediaSaveException {
        try {
            mediaService.updateMedia(media);
        } catch (IOException e) {
            throw new MediaController.MediaSaveException(e);
        }
    }

    private final Comparator<Media> mediaOrderComparator = new Comparator<Media>() {
        @Override
        public int compare(Media lhs, Media rhs) {
            return lhs.getOrder()-rhs.getOrder();
        }
    };

    public static class MediaFetchException extends Throwable {
        MediaFetchException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class MediaSaveException extends Throwable {
        MediaSaveException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class MediaDownloadException extends Throwable {
        MediaDownloadException(Throwable throwable) {
            super(throwable);
        }
    }
}
