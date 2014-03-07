package com.muzima.controller;

import com.muzima.api.model.Form;
import com.muzima.api.model.FormData;
import com.muzima.api.model.Notification;
import com.muzima.api.model.Tag;
import com.muzima.api.service.FormService;
import com.muzima.api.service.NotificationService;
import com.muzima.search.api.util.StringUtil;
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

    public int getAllNotificationsByReceiverCount(String receiverUuid, String status) throws NotificationFetchException, ParseException {
        List<Notification> notifications =  getAllNotificationsByReceiver(receiverUuid, status);
        return   notifications == null ? 0 : notifications.size();
    }

    public List<Notification> getNotificationsForPatient(String patientUuid, String receiverUuid, String status) throws NotificationFetchException {
        try {
            List<Notification> patientNotifications = new ArrayList<Notification>();
            List<FormData> allFormData = formService.getFormDataByPatient(patientUuid, STATUS_UPLOADED);
            Form form;
            for (FormData formData : allFormData) {
                Notification notification = notificationService.getNotificationByUuid(formData.getUuid());
                form = formService.getFormByUuid(formData.getTemplateUuid());
                if (isConsultationForm(form) && notification != null) {

                    //if receiverUuid is null and status is null the we add the notification to list
                    if (StringUtil.isEmpty(receiverUuid) && StringUtil.isEmpty(status)) {
                        // no filtering return all notifications for patient
                        patientNotifications.add(notification);
                    } else if (StringUtil.isEmpty(status))  {
                        // status not passed filter by receiverUuid only
                        if (StringUtil.equals(notification.getReceiver().getUuid(), receiverUuid))
                            patientNotifications.add(notification);
                    } else if (StringUtil.isEmpty(receiverUuid)){
                        // receiverUuid not passed filter by status only
                        if (StringUtil.equals(notification.getStatus(),status))
                            patientNotifications.add(notification);
                    } else {
                         // filter by both receiverUuid
                        if (StringUtil.equals(notification.getReceiver().getUuid(), receiverUuid) && StringUtil.equals(notification.getStatus(),status) )
                            patientNotifications.add(notification);
                    }
                }
            }

//            if (patientNotifications.size() < 1 ) {
//                return getTestInboxNotifications();
//            }
            return patientNotifications;
        } catch (IOException e) {
            throw new NotificationFetchException(e);
        }
    }

    public int getNotificationsCountForPatient(String patientUuid, String receiverUuid, String status) throws NotificationFetchException {
        List<Notification> notifications =  getNotificationsForPatient(patientUuid, receiverUuid, status);
        return   notifications == null ? 0 : notifications.size();
    }

    public boolean patientHasNotifications(String patientUuid, String receiverUuid, String status) throws NotificationFetchException {
        List<Notification> notifications =  getNotificationsForPatient(patientUuid, receiverUuid, status);
        return (notifications != null && notifications.size() > 0);
    }

    public List<Notification> downloadNotificationByReceiver(String receiverUuid) throws NotificationDownloadException, ParseException {
        try {
            return notificationService.downloadNotificationByReceiver(receiverUuid);
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

    public void deleteAllNotifications(String receiverUuid) throws NotificationDeleteException, NotificationFetchException, ParseException {
        try {
            notificationService.deleteNotifications(getAllNotificationsByReceiver(receiverUuid, null));
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
//
//    private List<Notification> getTestInboxNotifications() {
//        List<Notification> notifications = new ArrayList<Notification>();
//        Person receiver = new Person();
//        receiver.setUuid("suruwere");
//
//        for (int i=0; i<=3; i++) {
//            Notification nt = new Notification();
//            nt.setSubject("InboxMsg-" + i);
//            nt.setPayload("Inbox Payload Message-" + i);
//            nt.setDateCreated(new Date());
//            nt.setUuid("Inbox UUID-" + i);
//            nt.setReceiver(receiver);
//            if (i == 0 || i == 2)
//                nt.setStatus("read");
//            else
//                nt.setStatus("unread");
//            notifications.add(nt);
//        }
//        return notifications;
//    }
}
