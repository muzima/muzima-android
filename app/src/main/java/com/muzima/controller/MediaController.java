package com.muzima.controller;

import static com.muzima.api.model.APIName.DOWNLOAD_MEDIA;

import android.os.Environment;

import com.muzima.api.model.LastSyncTime;
import com.muzima.api.model.Media;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.api.service.MediaService;
import com.muzima.service.SntpService;

import org.apache.lucene.queryParser.ParseException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

    public List<Media> downloadMedia(List<String> uuids, boolean isDeltaSync) throws MediaController.MediaDownloadException {
        try {
            LastSyncTime lastSyncTime = lastSyncTimeService.getFullLastSyncTimeInfoFor(DOWNLOAD_MEDIA);
            Date lastSyncDate = null;
            if(isDeltaSync) {
                if (lastSyncTime != null) {
                    lastSyncDate = lastSyncTime.getLastSyncDate();
                }
            }
            List<Media> mediaList = new ArrayList<>();
            for(String uuid : uuids){
                List<Media> media = mediaService.downloadMedia(lastSyncDate, uuid);
                if(media != null) {
                    mediaList.addAll(media);
                }
            }
            LastSyncTime newLastSyncTime = new LastSyncTime(DOWNLOAD_MEDIA, sntpService.getTimePerDeviceTimeZone());
            lastSyncTimeService.saveLastSyncTime(newLastSyncTime);
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

    public List<Media> getMediaByCategoryID(Integer categoryId) throws MediaController.MediaFetchException {
        try {

            List<Media> media = mediaService.getMediaByCategoryId(categoryId);
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

    public void deleteMedia(List<String> uuids) throws MediaController.MediaSaveException {
        try {
            List<Media> mediaList = new ArrayList<>();
            for(String uuid:uuids){
                Media media = mediaService.getMediaByUuid(uuid);
                if(media != null){
                    mediaList.add(media);
                    //Delete file if exists
                    String PATH = Objects.requireNonNull(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)).getAbsolutePath();
                    File file = new File(PATH + "/"+media.getName());
                    if(file.exists())
                        file.delete();
                }
            }
            if(mediaList.size()>0)
                mediaService.deleteMedia(mediaList);

        } catch (IOException | ParseException e) {
            throw new MediaController.MediaSaveException(e);
        }
    }

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
