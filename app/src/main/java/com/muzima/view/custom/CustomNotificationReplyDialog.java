/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.custom;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.muzima.R;
import com.muzima.api.model.Notification;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Person;
import com.muzima.controller.NotificationController;
import com.muzima.utils.Constants;

import java.util.Date;
import java.util.UUID;

public class CustomNotificationReplyDialog extends Dialog {

    private final Patient notificationPatient;
    private final Person sender;
    private final String notificationSubject;
    private final Context context;
    private final NotificationController notificationController;

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

        ImageView sendReplyImageView = findViewById(R.id.send1_notification_reply_image_button);
        replyEditText = findViewById(R.id.reply_edit_text);

        sendReplyImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveNotificationReply();
            }
        });
    }

    private void saveNotificationReply() {
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
            replyNotification.setUploadStatus(Constants.NotificationStatusConstants.NOTIFICATION_NOT_UPLOADED);
            replyNotification.setPayload(replyEditText.getText().toString());
            notificationController.saveNotification(replyNotification);

            Toast.makeText(context, "Your reply was sent successfully ", Toast.LENGTH_LONG).show();
        } catch (NotificationController.NotificationSaveException e) {
            e.printStackTrace();
        }
    }
}
