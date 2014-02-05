package com.muzima.controller;

import com.muzima.api.model.Form;
import com.muzima.api.model.FormData;
import com.muzima.api.model.Notification;
import com.muzima.api.model.Tag;
import com.muzima.api.service.FormService;
import com.muzima.api.service.NotificationService;
import org.apache.lucene.queryParser.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.muzima.utils.Constants.FORM_DISCRIMINATOR_CONSULTATION;
import static com.muzima.utils.Constants.STATUS_UPLOADED;

public class NotificationController {
    private NotificationService notificationService;
    private FormService formService;

    public NotificationController(NotificationService notificationService, FormService formService) {
        this.notificationService = notificationService;
        this.formService = formService;
    }

    public Notification getNotificationByUuid(String uuid) throws NotificationFetchException, ParseException {
        //WIN: Can we hide the ParseException from the service consumers
        try {
            return notificationService.getNotificationByUuid(uuid);
        } catch (IOException e) {
            throw new NotificationFetchException(e);
        }
    }
    public List<Notification> getAllNotificationsBySender(String sender) throws NotificationFetchException, ParseException {
        //WIN: Can we hide the ParseException from the service consumers
        try {
            return notificationService.getNotificationBySender(sender);
            //WIN: should we rename this method to getNotificationsBySender since we are returning a list?
        } catch (IOException e) {
            throw new NotificationFetchException(e);
        }
    }

    public int getTotalNotificationsBySenderCount(String sender) throws NotificationFetchException, ParseException {
        try {
            //WIN: I think i will need this method [notificationService.countAllNotificationsBySender(sender)] on the service

            // Hack use this before Win writes the service method
            List<Notification> notifications =  notificationService.getNotificationBySender(sender);
            if (notifications != null)
                return  notifications.size();
            else
                return 0;
        } catch (IOException e) {
            throw new NotificationFetchException(e);
        }
    }

    public int getNotificationsCountForPatient(String patientUuid) throws NotificationFetchException {
        System.out.println("am inside getNotificationsCountForPatient");
        int count=0;
        try {
            List<FormData> allFormData = formService.getFormDataByPatient(patientUuid, STATUS_UPLOADED);
            Form form;
            for (FormData formData : allFormData) {
                Notification notification = notificationService.getNotificationByUuid(formData.getUuid());                 form = formService.getFormByUuid(formData.getTemplateUuid());
                if (isConsultationForm(form) && notification != null)
                    count++;
            }
            return count;
        } catch (IOException e) {
            throw new NotificationFetchException(e);
        }
    }

    public List<Notification> getNotificationsForPatient(String patientUuid) throws NotificationFetchException {
        System.out.println("am inside getNotificationsForPatient");
        try {
            List<Notification> patientNotifications = new ArrayList<Notification>();
            List<FormData> allFormData = formService.getFormDataByPatient(patientUuid, STATUS_UPLOADED);
            Form form;
            for (FormData formData : allFormData) {
                Notification notification = notificationService.getNotificationByUuid(formData.getUuid());                 form = formService.getFormByUuid(formData.getTemplateUuid());
                if (isConsultationForm(form) && notification != null)
                    patientNotifications.add(notification);
            }
            if (patientNotifications.size() <  1)
                patientNotifications = getTestInboxNotifications();
            return patientNotifications;
        } catch (IOException e) {
            throw new NotificationFetchException(e);
        }
    }

    public List<Notification> downloadNotificationBySender(String sender) throws NotificationDownloadException, ParseException {
        //WIN: Can we hide the ParseException from the service consumers
        try {
            return notificationService.downloadNotificationBySender(sender);
            //WIN: should we rename this method to downloadNotificationsBySender since we are returning a list?
        } catch (IOException e) {
            throw new NotificationDownloadException(e);
        }
    }

    public void saveNotification(Notification notification) throws NotificationSaveException {
        try {
            notificationService.saveNotification(notification);
        } catch (IOException e) {
            throw new NotificationSaveException(e);
        }
    }

   public void saveNotifications(List<Notification> notifications) throws NotificationSaveException {
        try {
            notificationService.saveNotifications(notifications);
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

    public boolean isConsultationForm(Form form){
        if (form == null)
            return false;

        Tag[] tags = form.getTags();

        if(tags == null){
            return false;
        }
        for (Tag tag : tags) {
            if(FORM_DISCRIMINATOR_CONSULTATION.equalsIgnoreCase(tag.getName())){
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

    private List<Notification> getTestInboxNotifications() {
        List<Notification> notifications = new ArrayList<Notification>();

        for (int i=0; i<=3; i++) {
            Notification nt = new Notification();
            nt.setSubject("InboxMsg-" + i);
            nt.setPayload("Inbox Payload Message-" + i);
            nt.setUuid("Inbox UUID-" + i);
            nt.setUri("Inbox URI-" + i);
            if (i == 0 || i == 2)
                nt.setStatus("read");
            else
                nt.setStatus("unread");
            notifications.add(nt);
        }
        return notifications;
    }
}
