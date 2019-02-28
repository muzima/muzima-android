/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.controller;

import android.os.AsyncTask;
import android.util.Log;

import com.muzima.MuzimaApplication;
import com.muzima.api.exception.ValidationFailureException;
import com.muzima.api.model.Form;
import com.muzima.api.model.LastSyncTime;
import com.muzima.api.model.Notification;
import com.muzima.api.model.Tag;
import com.muzima.api.service.FormService;
import com.muzima.api.service.LastSyncTimeService;
import com.muzima.api.service.NotificationService;
import com.muzima.service.SntpService;
import com.muzima.utils.Constants;

import org.apache.lucene.queryParser.ParseException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.muzima.api.model.APIName.DOWNLOAD_NOTIFICATIONS_BY_RECEIVER;
import static com.muzima.api.model.APIName.DOWNLOAD_NOTIFICATIONS_BY_SENDER;
import static com.muzima.utils.Constants.FORM_DISCRIMINATOR_CONSULTATION;

public class NotificationController {

    private final NotificationService notificationService;
    private final MuzimaApplication muzimaApplication;
    private final SntpService sntpService;
    private final NotificationController notificationController;

    public NotificationController(NotificationService notificationService, FormService formService, MuzimaApplication muzimaApplication, SntpService sntpService) {
        this.notificationService = notificationService;
        FormService formService1 = formService;
        this.muzimaApplication = muzimaApplication;
        this.sntpService = sntpService;
        this.notificationController = this;
    }

    public Notification getNotificationByUuid(String uuid) throws NotificationFetchException {
        try {
            return notificationService.getNotificationByUuid(uuid);
        } catch (IOException e) {
            throw new NotificationFetchException(e);
        }
    }

    public List<Notification> getAllNotificationsByReceiver(String receiverUuid, String status) throws NotificationFetchException {
        try {
            return notificationService.getNotificationByReceiver(receiverUuid, status);
        } catch (IOException e) {
            throw new NotificationFetchException(e);
        }
    }

    public List<Notification> getAllNotificationsByReceiver(String receiverUuid) throws NotificationFetchException {
        try {
            return notificationService.getNotificationByReceiver(receiverUuid);
        } catch (IOException e) {
            throw new NotificationFetchException(e);
        }
    }

    private List<Notification> getAllNotificationsBySender(String senderUuid, String status) throws NotificationFetchException {
        try {
            return notificationService.getNotificationBySender(senderUuid, status);
        } catch (IOException e) {
            throw new NotificationFetchException(e);
        }
    }

    public List<Notification> getAllNotificationsBySender(String senderUuid) throws NotificationFetchException {
        try {
            return notificationService.getNotificationBySender(senderUuid);
        } catch (IOException e) {
            throw new NotificationFetchException(e);
        }
    }

    public int getAllNotificationsByReceiverCount(String receiverUuid, String status) throws NotificationFetchException, ParseException {
        List<Notification> notifications = getAllNotificationsByReceiver(receiverUuid, status);
        return notifications == null ? 0 : notifications.size();
    }

    public int getAllNotificationsBySenderCount(String senderUuid, String status) throws NotificationFetchException, ParseException {
        List<Notification> notifications = getAllNotificationsBySender(senderUuid, status);
        return notifications == null ? 0 : notifications.size();
    }

    public List<Notification> getNotificationsForPatient(String patientUuid, String receiverUuid, String status) throws NotificationFetchException {
        try {
            return (notificationService.getNotificationByPatient(patientUuid, receiverUuid, status));
        } catch (IOException e) {
            throw new NotificationFetchException(e);
        }
    }

    public int getNotificationsCountForPatient(String patientUuid, String receiverUuid, String status) throws NotificationFetchException {
        List<Notification> notifications = getNotificationsForPatient(patientUuid, receiverUuid, status);
        return notifications == null ? 0 : notifications.size();
    }

    public boolean patientHasNotifications(String patientUuid, String receiverUuid, String status) throws NotificationFetchException {
        List<Notification> notifications = getNotificationsForPatient(patientUuid, receiverUuid, status);
        return (notifications != null && notifications.size() > 0);
    }

    public List<Notification> downloadNotificationByReceiver(String receiverUuid) throws NotificationDownloadException {
        try {
            LastSyncTimeService lastSyncTimeService = muzimaApplication.getMuzimaContext().getLastSyncTimeService();
            Date lastSyncTimeForNotifications = lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_NOTIFICATIONS_BY_RECEIVER);
            List<Notification> notification =  notificationService.downloadNotificationByReceiver(receiverUuid,lastSyncTimeForNotifications);
            LastSyncTime lastSyncTime = new LastSyncTime(DOWNLOAD_NOTIFICATIONS_BY_RECEIVER, sntpService.getLocalTime());
            lastSyncTimeService.saveLastSyncTime(lastSyncTime);
            Log.e("Receiver Notification Size:"," = "+notification.size()+" = "+lastSyncTimeForNotifications+" and "+lastSyncTime.getLastSyncDate());
            return notification;
        } catch (IOException e) {
            throw new NotificationDownloadException(e);
        }
    }

    public List<Notification> downloadNotificationBySender(String senderUuid) throws NotificationDownloadException {
        try {
            LastSyncTimeService lastSyncTimeService = muzimaApplication.getMuzimaContext().getLastSyncTimeService();
            Date lastSyncTimeForNotifications = lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_NOTIFICATIONS_BY_SENDER);
            List<Notification> senderNotifications = notificationService.downloadNotificationBySender(senderUuid,lastSyncTimeForNotifications);
            LastSyncTime lastSyncTime = new LastSyncTime(DOWNLOAD_NOTIFICATIONS_BY_SENDER, sntpService.getLocalTime());
            lastSyncTimeService.saveLastSyncTime(lastSyncTime);
            Log.e("Sender Notification Size:"," = "+senderNotifications.size()+" = "+lastSyncTimeForNotifications+" and "+lastSyncTime.getLastSyncDate());
            return senderNotifications;
        } catch (IOException e) {
            throw new NotificationDownloadException(e);
        }
    }



    public void saveNotification(Notification notification) throws NotificationSaveException {
        try {
            notificationService.saveNotification(notification);
            new NotificationUploadBackgroundTask().execute();
        } catch (IOException e) {
            throw new NotificationSaveException(e);
        }
    }

    public void saveNotifications(List<Notification> notifications) throws NotificationSaveException {
        try {
            notificationService.saveNotifications(notifications);
            new NotificationsUploadBackgroudTask().execute();
        } catch (IOException e) {
            throw new NotificationSaveException(e);
        }
    }

    public void deleteNotifications(Notification notification) throws NotificationDeleteException {
        try {
            notificationService.deleteNotification(notification);
        } catch (IOException e) {
            throw new NotificationDeleteException(e);
        }
    }

    public void deleteAllNotifications(String receiverUuid) throws NotificationDeleteException, NotificationFetchException, ParseException {
        try {
            notificationService.deleteNotifications(getAllNotificationsByReceiver(receiverUuid, null));
        } catch (IOException e) {
            throw new NotificationDeleteException(e);
        }
    }

    public boolean isConsultationForm(Form form) {
        if (form == null)
            return false;

        Tag[] tags = form.getTags();

        if (tags == null) {
            return false;
        }
        for (Tag tag : tags) {
            if (FORM_DISCRIMINATOR_CONSULTATION.equalsIgnoreCase(tag.getName())) {
                return true;
            }
        }
        return false;
    }

    public static class NotificationDownloadException extends Throwable {
        NotificationDownloadException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class NotificationFetchException extends Throwable {
        NotificationFetchException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class NotificationSaveException extends Throwable {
        NotificationSaveException(Throwable throwable) {
            super(throwable);
        }
    }

    static class NotificationDeleteException extends Throwable {
        NotificationDeleteException(Throwable throwable) {
            super(throwable);
        }
    }

    private class NotificationUploadBackgroundTask extends AsyncTask<Notification,Void,Void> {

        @Override
        protected Void doInBackground(Notification... notifications) {
            List<Notification> senderNotifications = new ArrayList<>();
            try {
                senderNotifications = notificationController.getNotificationByUploadStatus(Constants.NotificationStatusConstants.NOTIFICATION_NOT_UPLOADED);
            }catch (IOException e) {
                Log.e(getClass().getSimpleName(),"Unable to load Notifications"+e);
            }
            for (Notification notification : senderNotifications) {
               try {
                    if(notificationService.uploadNotification(notification)){
                        notification.setUploadStatus(Constants.NotificationStatusConstants.NOTIFICATION_UPLOADED);
                        notificationController.saveNotification(notification);
                    }
                } catch (ValidationFailureException e) {
                    Log.e(getClass().getSimpleName(), "Unable to upload notification.");
                } catch (IOException e) {
                    Log.e(getClass().getSimpleName(),"Encountered an IOException "+e);
                }  catch (NotificationSaveException e) {
                    Log.e(getClass().getSimpleName(), "Unable to Save Notification "+e);
                }

            }
            return  null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private class NotificationsUploadBackgroudTask extends AsyncTask<Notification, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Notification... notifications) {
            List<Notification> senderNotifications = new ArrayList<>();
            try {
                senderNotifications = notificationController.getNotificationByUploadStatus(Constants.NotificationStatusConstants.NOTIFICATION_NOT_UPLOADED);
            } catch (IOException e) {
                Log.e(getClass().getSimpleName(),"Unable to load Notifications"+e);
            }
            for (Notification notification : senderNotifications) {
               try {
                    if(notificationService.uploadNotification(notification)){
                         notification.setUploadStatus(Constants.NotificationStatusConstants.NOTIFICATION_UPLOADED);
                        notificationController.saveNotification(notification);
                    }
                }catch (ValidationFailureException e) {
                    Log.e(getClass().getSimpleName(), "Unable to upload notification. "+e);
                } catch (IOException e) {
                    Log.e(getClass().getSimpleName(),"Encountered an IOException "+e);
                } catch (NotificationSaveException e) {
                    Log.e(getClass().getSimpleName(),"Unable to save notification "+e);
                }

            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    private List<Notification> getNotificationByUploadStatus(String uploadStatus) throws IOException {
        return notificationService.getNotificationByUploadStatus(uploadStatus);
    }
}