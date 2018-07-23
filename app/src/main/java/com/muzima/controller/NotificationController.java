/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
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
import java.util.Date;
import java.util.List;

import static com.muzima.api.model.APIName.DOWNLOAD_NOTIFICATIONS;
import static com.muzima.utils.Constants.FORM_DISCRIMINATOR_CONSULTATION;

public class NotificationController {

    private NotificationService notificationService;
    private FormService formService;
    private MuzimaApplication muzimaApplication;
    private SntpService sntpService;
    private NotificationController notificationController;

    public NotificationController(NotificationService notificationService, FormService formService, MuzimaApplication muzimaApplication, SntpService sntpService) {
        this.notificationService = notificationService;
        this.formService = formService;
        this.muzimaApplication = muzimaApplication;
        this.sntpService = sntpService;
        this.notificationController = this;
    }

    public Notification getNotificationByUuid(String uuid) throws NotificationFetchException, ParseException {
        try {
            return notificationService.getNotificationByUuid(uuid);
        } catch (IOException e) {
            throw new NotificationFetchException(e);
        }
    }

    public List<Notification> getAllNotificationsByReceiver(String receiverUuid, String status) throws NotificationFetchException, ParseException {
        try {
            return notificationService.getNotificationByReceiver(receiverUuid, status);
        } catch (IOException e) {
            throw new NotificationFetchException(e);
        }
    }

    public List<Notification> getAllNotificationsByReceiver(String receiverUuid) throws NotificationFetchException, ParseException {
        try {
            return notificationService.getNotificationByReceiver(receiverUuid);
        } catch (IOException e) {
            throw new NotificationFetchException(e);
        }
    }

    public List<Notification> getAllNotificationsBySender(String senderUuid, String status) throws NotificationFetchException, ParseException {
        try {
            return notificationService.getNotificationBySender(senderUuid, status);
        } catch (IOException e) {
            throw new NotificationFetchException(e);
        }
    }

    public List<Notification> getAllNotificationsBySender(String senderUuid) throws NotificationFetchException, ParseException {
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
            Date lastSyncTimeForNotifications = lastSyncTimeService.getLastSyncTimeFor(DOWNLOAD_NOTIFICATIONS);
            List<Notification> notification =  notificationService.downloadNotificationByReceiver(receiverUuid,lastSyncTimeForNotifications);
            LastSyncTime lastSyncTime = new LastSyncTime(DOWNLOAD_NOTIFICATIONS, sntpService.getLocalTime());
            lastSyncTimeService.saveLastSyncTime(lastSyncTime);
            Log.e("Notification Size:"," = "+notification.size()+" = "+lastSyncTimeForNotifications+" and "+lastSyncTime.getLastSyncDate());
            return notification;
        } catch (IOException e) {
            throw new NotificationDownloadException(e);
        }
    }

    public void saveNotification(Notification notification) throws NotificationSaveException {
        try {
            Log.e("TAG", "Notification" + notification.toString());
            if(notification.getUploadStatus()==null){
                notification.setUploadStatus(Constants.NotificationStatusConstants.NOTIFICATION_UPLOADED);
                notificationService.saveNotification(notification);
                Log.e("Uploaded: ","Notification Uploaded for Null status");
            }
            else if (!(notification.getUploadStatus().equals(Constants.NotificationStatusConstants.NOTIFICATION_NOT_UPLOADED))) {
                //if(notificationService.uploadNotification(notification)){
                    notification.setUploadStatus(Constants.NotificationStatusConstants.NOTIFICATION_UPLOADED);
                    notificationService.saveNotification(notification);
                    Log.e("Uploaded: ","Notification Uploaded");
               // }
            }else{
                notificationService.saveNotification(notification);
                Log.e("Uploaded: ","Notification Saved");
            }
        } catch (IOException e) {
            throw new NotificationSaveException(e);
        } /*catch (ValidationFailureException e) {
            Log.e(getClass().getSimpleName(), "Unable to upload notification.");
        }*/
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
        public NotificationDownloadException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class NotificationFetchException extends Throwable {
        public NotificationFetchException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class NotificationSaveException extends Throwable {
        public NotificationSaveException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class NotificationDeleteException extends Throwable {
        public NotificationDeleteException(Throwable throwable) {
            super(throwable);
        }
    }

    private class NotificationUploadBackgroundTask extends AsyncTask<Notification,Void,Void> {

        @Override
        protected Void doInBackground(Notification... notifications) {

            for (Notification notification : notifications) {
                if (!(notification.getUploadStatus().equals(Constants.NotificationStatusConstants.NOTIFICATION_NOT_UPLOADED))) {
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
            }
            return  null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

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
            for (Notification notification : notifications) {
                if (!(notification.getUploadStatus().equals(Constants.NotificationStatusConstants.NOTIFICATION_NOT_UPLOADED))) {
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
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }
}