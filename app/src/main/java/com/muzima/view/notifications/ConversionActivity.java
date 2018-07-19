package com.muzima.view.notifications;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

import com.muzima.adapters.MessageThreadAdapter;
import com.muzima.api.model.Notification;
import com.muzima.api.model.Person;
import com.muzima.api.model.PersonName;
import com.muzima.controller.NotificationController;
import com.muzima.controller.ProviderController;
import com.muzima.utils.Constants;
import com.muzima.view.BaseActivity;
import com.muzima.R;
import com.muzima.api.model.Provider;
import com.muzima.MuzimaApplication;
import com.muzima.view.MainActivity;

import org.apache.lucene.queryParser.ParseException;

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
    List<Notification> patientSentMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.conversation_thread_activity_layout);

        Bundle data = getIntent().getExtras();
        Provider provider = (Provider) data.get("provider");
        getSupportActionBar().setTitle(provider.getName());

        loggedInUser = muzimaApplication.getAuthenticatedUser().getPerson();

        chatListView = findViewById(R.id.chat_list_view);
        adapter = new MessageThreadAdapter(chats, this);
        muzimaApplication = (MuzimaApplication) getApplicationContext();
        notificationController = muzimaApplication.getNotificationController();
        providerController = muzimaApplication.getProviderController();

        floatingActionButton = findViewById(R.id.send_message_fab);
        composeEditText = findViewById(R.id.type_message_editText);
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
        }

        setUpMessage();

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                List<Notification> messageItems = Collections.singletonList(new Notification(composeEditText.getText().toString()));
                adapter.updateResults(messageItems);
                adapter.notifyDataSetChanged();
                composeEditText.setText("");

                com.muzima.api.model.Notification notification = createNotificationFromMessage(messageItems.get(0));

                try {
                    notificationController.saveNotification(notification);
                } catch (NotificationController.NotificationSaveException e) {
                    e.printStackTrace();
                }

            }
        });

    }

    private com.muzima.api.model.Notification createNotificationFromMessage(String messageItem) {
        List<PersonName> personNames = new ArrayList<>();

        Person receiver = new Person();
        PersonName familyName = new PersonName();
        PersonName givenName = new PersonName();
        familyName.setFamilyName(loggedInUser.getFamilyName());
        givenName.setGivenName(.getName());


        personNames.add(familyName);
        personNames.add(givenName);

        receiver.setUuid(MainActivity.globleProvider.getUuid());
        receiver.setNames(personNames);
        receiver.setUuid(MainActivity.globleProvider.getUuid());

        com.muzima.api.model.Notification notification = new com.muzima.api.model.Notification();
        notification.setPatient(MainActivity.globalPhrPatient);
        notification.setPayload(messageItem.getChatMessage());
        notification.setStatus(Constants.NotificationStatusConstants.NOTIFICATION_UNREAD);
        notification.setReceiver(receiver);
        notification.setSender(MainActivity.globalPhrPatient);
        notification.setSource("Mobile Device");
        notification.setSubject("PHR Message");
        notification.setDateCreated(new Date());

        return notification;
    }

    public void setUpMessage(){
//        for (Notification providerSentMessage : providerSentMessages) {
//            providerSentMessage.getDateCreated();
//            chats.add(new MessageItem(providerSentMessage.getPayload(),true));
//        }

        chats.addAll(patientSentMessages);
    }

    public void getOutgoingMessages() throws ParseException, ProviderController.ProviderLoadException, NotificationController.NotificationFetchException {
        patientSentMessages = notificationController.getAllNotificationsByReceiver(providerController.getAllProviders().get(0).getUuid(),null);
    }

}
