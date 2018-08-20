package com.muzima.view.notifications;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Scroller;
import android.widget.Toast;


import com.muzima.adapters.MessageThreadAdapter;
import com.muzima.api.model.Notification;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Person;
import com.muzima.api.model.PersonName;
import com.muzima.controller.NotificationController;
import com.muzima.controller.ProviderController;
import com.muzima.scheduler.MuzimaJobScheduler;
import com.muzima.utils.Constants;
import com.muzima.view.BaseActivity;
import com.muzima.R;
import com.muzima.api.model.Provider;
import com.muzima.MuzimaApplication;

import org.apache.lucene.queryParser.ParseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.muzima.utils.Constants.DataSyncServiceConstants.MuzimaJobSchedularConstants.MESSAGE_SYNC_JOB_ID;
import static com.muzima.utils.Constants.DataSyncServiceConstants.MuzimaJobSchedularConstants.MUZIMA_JOB_PERIODIC;


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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.conversation_thread_activity_layout);

        Bundle data = getIntent().getExtras();
        provider = (Provider) data.get("provider");
        getSupportActionBar().setTitle(provider.getName());

        ListView chatListView = findViewById(R.id.chat_list_view);
        adapter = new MessageThreadAdapter(chats, this, provider);
        MuzimaApplication muzimaApplication = (MuzimaApplication) getApplicationContext();
        notificationController = muzimaApplication.getNotificationController();
        ProviderController providerController = muzimaApplication.getProviderController();

        loggedInUser = muzimaApplication.getAuthenticatedUser().getPerson();

        final FloatingActionButton floatingActionButton = findViewById(R.id.send_message_fab);
        composeEditText = findViewById(R.id.type_message_editText);
        chatListView.setAdapter(adapter);

        floatingActionButton.setBackgroundColor(getResources().getColor(R.color.hint_text_grey));

//        getIncomingMessages();
        try {
            getOutgoingMessages();
        } catch (NotificationController.NotificationSaveException e) {
            Log.e(getClass().getSimpleName(), e.getMessage());
        }

        setUpMessage();
        Log.e(getClass().getSimpleName(), "Click Logged");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            floatingActionButton.setBackground(getDrawable(R.drawable.ic_action_need_attention));
        }
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(getClass().getSimpleName(), "Click Logged");
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
                        floatingActionButton.setBackground(getDrawable(R.drawable.ic_action_send));
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        floatingActionButton.setBackground(getDrawable(R.drawable.ic_action_need_attention));
                    }
                }
            }
        });

//        if (!composeEditText.getText().toString().isEmpty()){
//                    floatingActionButton.setBackgroundColor(getResources().getColor(R.color.primary_blue));
//                }
//                return false;
//            }
//        });

        composeEditText.setScroller(new Scroller(getApplicationContext()));
        composeEditText.setVerticalScrollBarEnabled(true);
        composeEditText.setMovementMethod(new ScrollingMovementMethod());

    }

    private com.muzima.api.model.Notification createNotificationFromMessage(String messageItem) {
        List<PersonName> personNames = new ArrayList<>();
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
//        for (Notification providerSentMessage : allMessagesThreadForPerson) {
//            providerSentMessage.getDateCreated();
//            chats.add(new MessageItem(providerSentMessage.getPayload(),true));
//        }

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
