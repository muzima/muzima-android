package com.muzima.view.custom;

import android.app.Dialog;
import android.content.Context;
import android.media.Image;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Notification;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Person;
import com.muzima.controller.NotificationController;
import com.muzima.utils.Constants;

import java.util.Date;
import java.util.UUID;

public class CustomNotificationReplyDialog extends Dialog {

    private Patient notificationPatient;
    private Person sender;
    private String notificationSubject;
    private Context context;
    private NotificationController notificationController;

    private ImageView sendReplyImageView;
    private EditText replyEditText;

    public CustomNotificationReplyDialog(@NonNull Context context, Patient notificationPatient, Person sender, String subject, NotificationController notificationController) {
        super(context);
        this.notificationPatient = notificationPatient;
        this.sender = sender;
        this.context = context;
        this.notificationSubject = subject;
        this.notificationController = notificationController;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.reply_notification_dialog_layout);

        sendReplyImageView = (ImageView) findViewById(R.id.send1_notification_reply_image_button);
        replyEditText = (EditText) findViewById(R.id.reply_edit_text);

        sendReplyImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveNotificationReply();
            }
        });
    }

    private boolean saveNotificationReply() {

        try {

            Notification replyNotification = new Notification();
            replyNotification.setStatus(Constants.NotificationStatusConstants.NOTIFICATION_UNREAD);
            replyNotification.setPatient(notificationPatient);
            replyNotification.setReceiver(notificationPatient);
            replyNotification.setSender(sender);
            replyNotification.setSource("mUzima Mobile Device");
            replyNotification.setDateCreated(new Date());
            replyNotification.setUuid(UUID.randomUUID().toString());
            replyNotification.setSubject(notificationSubject);
            replyNotification.setPayload(replyEditText.getText().toString());
            notificationController.saveNotification(replyNotification);

            Toast.makeText(context, "Your reply was sent successfully ", Toast.LENGTH_LONG).show();
            return true;
        } catch (NotificationController.NotificationSaveException e) {
            e.printStackTrace();
            return false;
        }
    }

}
