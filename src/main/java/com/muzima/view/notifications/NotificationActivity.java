package com.muzima.view.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Notification;
import com.muzima.api.model.Person;
import com.muzima.controller.NotificationController;
import com.muzima.utils.Constants;
import com.muzima.view.BaseActivity;

public class NotificationActivity extends BaseActivity {

    public static final String TAG = "NotificationActivity";
    public static final String NOTIFICATION = "Notification";
    private Notification notification;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_notification);
        Intent intent = getIntent();
        notification= (Notification) intent.getSerializableExtra(NOTIFICATION);
        if (notification != null) {
            displayNotification();
            markAsRead();
        }
	}

    private void displayNotification()  {
        TextView subjectView  = (TextView) findViewById(R.id.subject);
        subjectView.setText(notification.getSubject());

        TextView notificationDate = (TextView) findViewById(R.id.dateSent);
        notificationDate.setText("Sent:"  + notification.getDateCreated().toString());

        TextView sentBy = (TextView) findViewById(R.id.sentBy);
        Person person = notification.getSender();
        if (person != null)
            sentBy.setText(person.getDisplayName());

        TextView details = (TextView) findViewById(R.id.notificationDetail);
        details.setText(notification.getPayload());
    }

    private void markAsRead() {
        NotificationController notificationController = ((MuzimaApplication) getApplicationContext()).getNotificationController();
        notification.setStatus(Constants.NotificationStatusConstants.NOTIFICATION_READ);
        try {
            notificationController.saveNotification(notification);
        } catch (NotificationController.NotificationSaveException e) {
            Log.e(TAG, "Error updating notification " + e.getMessage());
        }
    }
}
