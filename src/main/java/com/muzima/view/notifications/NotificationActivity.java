package com.muzima.view.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.FormData;
import com.muzima.api.model.Notification;
import com.muzima.api.model.Person;
import com.muzima.controller.FormController;
import com.muzima.controller.NotificationController;
import com.muzima.model.CompleteFormWithPatientData;
import com.muzima.model.FormWithData;
import com.muzima.utils.Constants;
import com.muzima.view.BaseActivity;
import com.muzima.view.forms.FormViewIntent;
import com.muzima.view.forms.FormsActivity;

public class NotificationActivity extends BaseActivity {

    public static final String TAG = "NotificationActivity";
    public static final String NOTIFICATION = "Notification";
    private Notification notification;
    private CompleteFormWithPatientData completeFormWithPatientData;
    private View viewFormButton;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_notification);
        Intent intent = getIntent();
        notification= (Notification) intent.getSerializableExtra(NOTIFICATION);
        if (notification != null) {
            getNotificationForm();
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

        //hide view form button if form is not available
        if (completeFormWithPatientData == null) {
            viewFormButton = findViewById(R.id.viewNotificationForm);
            viewFormButton.setVisibility(View.GONE);
        }

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

    /**
     * Called when the user clicks the Forms area
     */
    public void formView(View view) {
        if (completeFormWithPatientData != null) {
            FormViewIntent intent = new FormViewIntent(this,completeFormWithPatientData);
            startActivityForResult(intent, FormsActivity.FORM_VIEW_ACTIVITY_RESULT);
        }else
            Toast.makeText(this, "Could not load form", Toast.LENGTH_SHORT).show();
    }

    private void getNotificationForm() {
        FormController formController = ((MuzimaApplication) getApplicationContext()).getFormController();
        try {
            completeFormWithPatientData = formController.getCompleteFormDataByUuid(notification.getUuid());
        } catch (FormController.FormDataFetchException e) {
            Log.e(TAG, "Error getting form data " + e.getMessage());
        } catch (FormController.FormFetchException e) {
            Log.e(TAG, "Error getting form data " + e.getMessage());
        }
    }
}
