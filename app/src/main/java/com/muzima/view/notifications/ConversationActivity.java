/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.notifications;

import android.os.Build;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Scroller;
import android.widget.Toast;


import com.muzima.adapters.MessageThreadAdapter;
import com.muzima.api.model.Notification;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Person;
import com.muzima.controller.NotificationController;
import com.muzima.scheduler.MuzimaJobScheduleBuilder;
import com.muzima.utils.Constants;
import com.muzima.utils.ThemeUtils;
import com.muzima.view.BaseActivity;
import com.muzima.R;
import com.muzima.api.model.Provider;
import com.muzima.MuzimaApplication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;


public class ConversationActivity extends BaseActivity {

    private Person loggedInUser;
    private final List<Notification> chats = new ArrayList<>();
    private MessageThreadAdapter adapter;
    private NotificationController notificationController;
    private EditText composeEditText;
    private Provider provider;
    private List<Notification> patientSentMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.getInstance().onCreate(this,false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.conversation_thread_activity_layout);

        Bundle data = getIntent().getExtras();
        provider = (Provider) data.get("provider");
        getActionBar().setTitle(provider.getName());

        ListView chatListView = findViewById(R.id.chat_list_view);
        adapter = new MessageThreadAdapter(chats, this, provider);
        MuzimaApplication muzimaApplication = (MuzimaApplication) getApplicationContext();
        notificationController = muzimaApplication.getNotificationController();

        if(muzimaApplication.getAuthenticatedUser() != null) {
            loggedInUser = muzimaApplication.getAuthenticatedUser().getPerson();
        }

        final FloatingActionButton floatingActionButton = findViewById(R.id.send_message_fab);
        composeEditText = findViewById(R.id.type_message_editText);
        chatListView.setAdapter(adapter);

        floatingActionButton.setBackgroundColor(getResources().getColor(R.color.hint_text_grey));

        try {
            getOutgoingMessages();
        } catch (NotificationController.NotificationSaveException e) {
            Log.e(getClass().getSimpleName(), e.getMessage());
        }

        setUpMessage();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            floatingActionButton.setBackground(getDrawable(R.drawable.ic_accept));
        }
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!composeEditText.getText().toString().isEmpty()) {
                    List<Notification> messageItems = null;
                    Notification notification = createNotificationFromMessage(composeEditText.getText().toString());
                    messageItems = Collections.singletonList(notification);
                    adapter.updateResults(messageItems);
                    adapter.notifyDataSetChanged();
                    composeEditText.setText("");

                    try {
                        notificationController.saveNotification(notification);
                    } catch (NotificationController.NotificationSaveException e) {
                        e.printStackTrace();
                    }

                    MuzimaJobScheduleBuilder muzimaJobScheduleBuilder = new MuzimaJobScheduleBuilder(getApplicationContext());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        //run immediately
                        muzimaJobScheduleBuilder.schedulePeriodicBackgroundJob(0,false);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), R.string.error_empty_message_text, Toast.LENGTH_SHORT).show();
                }

            }
        });

        composeEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() != 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        floatingActionButton.setBackground(getDrawable(R.drawable.ic_accept));
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        floatingActionButton.setBackground(getDrawable(R.drawable.ic_accept));
                    }
                }
            }
        });

        composeEditText.setScroller(new Scroller(getApplicationContext()));
        composeEditText.setVerticalScrollBarEnabled(true);
        composeEditText.setMovementMethod(new ScrollingMovementMethod());

    }

    private com.muzima.api.model.Notification createNotificationFromMessage(String messageItem) {
        com.muzima.api.model.Notification notification = new com.muzima.api.model.Notification();
        notification.setPatient(new Patient());
        notification.setPayload(messageItem);
        notification.setStatus(Constants.NotificationStatusConstants.NOTIFICATION_UNREAD);
        notification.setUploadStatus(Constants.NotificationStatusConstants.NOTIFICATION_NOT_UPLOADED);
        notification.setReceiver(provider.getPerson());
        notification.setSender(loggedInUser);
        notification.setSource("Mobile Device");
        notification.setSubject("Provider Message");
        notification.setDateCreated(new Date());

        return notification;
    }

    private void setUpMessage() {
        chats.addAll(patientSentMessages);
    }

    private void getOutgoingMessages() throws NotificationController.NotificationSaveException {
        List<Notification> allMessagesThreadForPerson = new ArrayList<>();
        List<Notification> notificationBySender = new ArrayList<>();
        List<Notification> notificationByReceiver = new ArrayList<>();
        try {
            notificationBySender = notificationController.getAllNotificationsBySender(provider.getPerson().getUuid());
            notificationByReceiver = notificationController.getAllNotificationsByReceiver(provider.getPerson().getUuid());
            allMessagesThreadForPerson.addAll(notificationBySender);
            allMessagesThreadForPerson.addAll(notificationByReceiver);
        } catch (NotificationController.NotificationFetchException e) {
            Log.e(getClass().getSimpleName(), e.getMessage());
        }

        for (Notification notification : notificationBySender) {
            notification.setStatus(Constants.NotificationStatusConstants.NOTIFICATION_READ);
            notificationController.saveNotification(notification);
        }
        patientSentMessages = allMessagesThreadForPerson;
    }

}
