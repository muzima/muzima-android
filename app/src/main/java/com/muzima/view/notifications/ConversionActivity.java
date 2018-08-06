package com.muzima.view.notifications;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Scroller;


import com.muzima.MuzimaApplication;
import com.muzima.adapters.MessageThreadAdapter;
import com.muzima.api.model.Notification;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Person;
import com.muzima.api.model.PersonName;
import com.muzima.controller.NotificationController;
import com.muzima.controller.ProviderController;
import com.muzima.utils.Constants;
import com.muzima.utils.DateUtils;
import com.muzima.view.BaseActivity;
import com.muzima.R;
import com.muzima.api.model.Provider;
import com.muzima.MuzimaApplication;
import com.muzima.view.MainActivity;

import org.apache.lucene.queryParser.ParseException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;



public class ConversionActivity extends BaseActivity {

    Person loggedInUser;
    ListView chatListView;
    List<Notification> chats = new ArrayList<>();
    MessageThreadAdapter adapter;
    MuzimaApplication muzimaApplication;
    NotificationController notificationController;
    ProviderController providerController;
    FloatingActionButton floatingActionButton;
    EditText composeEditText;
    Provider provider;
    List<Notification> patientSentMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.conversation_thread_activity_layout);

        Bundle data = getIntent().getExtras();
        provider = (Provider) data.get("provider");
        getSupportActionBar().setTitle(provider.getName());

        chatListView = (ListView) findViewById(R.id.chat_list_view);
        adapter = new MessageThreadAdapter(chats, this,provider);
        muzimaApplication = (MuzimaApplication) getApplicationContext();
        notificationController = muzimaApplication.getNotificationController();
        providerController = muzimaApplication.getProviderController();

        loggedInUser = muzimaApplication.getAuthenticatedUser().getPerson();

        floatingActionButton = (FloatingActionButton) findViewById(R.id.send_message_fab);
        composeEditText = (EditText) findViewById(R.id.type_message_editText);
        chatListView.setAdapter(adapter);

//        getIncomingMessages();
        try {
            getOutgoingMessages();
        } catch (ParseException e) {
            Log.e(getClass().getSimpleName(),"Unable to obtain outgoing messages");
        }catch(ProviderController.ProviderLoadException e){
            Log.e(getClass().getSimpleName(),"Unable to obtain outgoing messages");
        }catch (NotificationController.NotificationFetchException e){
            Log.e(getClass().getSimpleName(),"Unable to obtain outgoing messages");
        } catch (NotificationController.NotificationSaveException e) {
            e.printStackTrace( );
        }

        setUpMessage();

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
            }
        });

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

    public void setUpMessage(){
//        for (Notification providerSentMessage : allMessagesThreadForPerson) {
//            providerSentMessage.getDateCreated();
//            chats.add(new MessageItem(providerSentMessage.getPayload(),true));
//        }

        chats.addAll(patientSentMessages);
    }

    public void getOutgoingMessages() throws ParseException, ProviderController.ProviderLoadException, NotificationController.NotificationFetchException, NotificationController.NotificationSaveException {
        List<Notification> allMessagesThreadForPerson = new ArrayList<>();
        List<Notification> notificationBySender = new ArrayList<>();
        List<Notification> notificationByReceiver = new ArrayList<>();
        try {
            notificationBySender = notificationController.getAllNotificationsBySender(provider.getPerson().getUuid());
            notificationByReceiver = notificationController.getAllNotificationsByReceiver(provider.getPerson().getUuid());
            allMessagesThreadForPerson.addAll(notificationBySender);
            allMessagesThreadForPerson.addAll(notificationByReceiver);
        } catch (NotificationController.NotificationFetchException e) {
            e.printStackTrace( );
        } catch (ParseException e) {
            e.printStackTrace( );
        }

        for(Notification notification:notificationBySender){
            notification.setStatus(Constants.NotificationStatusConstants.NOTIFICATION_READ);
            notificationController.saveNotification(notification);
        }
        patientSentMessages = allMessagesThreadForPerson;
    }

}
