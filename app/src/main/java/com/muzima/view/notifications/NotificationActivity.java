/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.notifications;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Encounter;
import com.muzima.api.model.Notification;
import com.muzima.api.model.Patient;
import com.muzima.api.model.Person;
import com.muzima.api.model.Provider;
import com.muzima.controller.EncounterController;
import com.muzima.controller.NotificationController;
import com.muzima.controller.ProviderController;
import com.muzima.utils.Constants;
import com.muzima.utils.DateUtils;
import com.muzima.view.BaseActivity;
import com.muzima.view.custom.CustomNotificationReplyDialog;
import com.muzima.view.observations.ObservationsActivity;

import java.util.List;

public class NotificationActivity extends BaseActivity {

    public static final String NOTIFICATION = "Notification";
    public static final String PATIENT = "patient";
    private Notification notification;
    private Encounter notificationEncounter;
    private Patient notificationPatient;
    private Provider loggedInProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        NotificationController notificationController = ((MuzimaApplication) getApplicationContext()).getNotificationController();

        ImageView replyNotiticationImageView = findViewById(R.id.reply_notification_image_icon);

        loggedInProvider = null;

        Intent intent = getIntent();
        notification = (Notification) intent.getSerializableExtra(NOTIFICATION);
        Log.e(getClass().getSimpleName()," Sender is "+notification.getSender().getUuid()+" and Receiver is "+notification.getReceiver().getUuid());
        notificationPatient = (Patient) intent.getSerializableExtra(PATIENT);
        if (notification != null) {

            if (notificationPatient != null)
                getNotificationEncounter(notificationPatient);

            displayNotification();
            markAsRead(notification);
        }

        replyNotiticationImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new CustomNotificationReplyDialog(NotificationActivity.this,notificationPatient,((MuzimaApplication)getApplicationContext()).getAuthenticatedUser().getPerson(),notification.getSubject(),((MuzimaApplication)getApplicationContext()).getNotificationController()).show();
            }
        });
    }

    private void displayNotification()  {
        TextView subjectView  = findViewById(R.id.subject);
        subjectView.setText(notification.getSubject());

        TextView notificationDate = findViewById(R.id.dateSent);
        notificationDate.setText(String.format("Sent: %s", DateUtils.getMonthNameFormattedDate(notification.getDateCreated())));

        TextView sentBy = findViewById(R.id.sentBy);
        Person person = notification.getSender();
        if (person != null)
            sentBy.setText(person.getDisplayName());

        TextView details = findViewById(R.id.notificationDetail);
        details.setText(notification.getPayload());

        //hide view form button if form is not available
        if (notificationEncounter == null) {
            View viewEncounterButton = findViewById(R.id.viewEncounter);
            viewEncounterButton.setVisibility(View.GONE);
        }
    }

    private void markAsRead(Notification notification) {
        NotificationController notificationController = ((MuzimaApplication) getApplicationContext()).getNotificationController();
        notification.setStatus(Constants.NotificationStatusConstants.NOTIFICATION_READ);
        try {
            notificationController.saveNotification(notification);
        } catch (NotificationController.NotificationSaveException e) {
            Log.e(getClass().getSimpleName(), "Error updating notification " + e.getMessage(), e);
        }
    }

    /**
     * Called when the user clicks the Forms area
     */
    public void encounterView(View view) {
        if (notificationEncounter != null) {
            Intent intent = new Intent(this, ObservationsActivity.class);
            intent.putExtra(PATIENT, notificationEncounter.getPatient());
            startActivity(intent);
        }else
            Toast.makeText(this, getString(R.string.error_form_load_fail), Toast.LENGTH_SHORT).show();
    }

    private void getNotificationEncounter(Patient patient) {
        EncounterController encounterController = ((MuzimaApplication) getApplicationContext()).getEncounterController();
        try {
            List<Encounter> encounters = encounterController.getEncountersByPatientUuid(patient.getUuid());
            for (Encounter encounter : encounters) {
                String formDataUuid = encounter.getFormDataUuid();
                if (formDataUuid != null && formDataUuid.equals(notification.getSource()))
                    notificationEncounter =  encounter;
            }
        } catch (EncounterController.DownloadEncounterException e) {
            Log.e(getClass().getSimpleName(), "Error getting encounter data " + e.getMessage(), e);
        }
    }

    private class LoggedProviderFetchBackgrounudTask extends AsyncTask<Void,Void,Provider>{


        @Override
        protected Provider doInBackground(Void... voids) {
            try {
                return ((MuzimaApplication)getApplicationContext()).getProviderController().getLoggedInProvider(
                        ((MuzimaApplication)getApplicationContext()).getAuthenticatedUser().getSystemId());
            } catch (ProviderController.ProviderLoadException e) {
                Log.e(getClass().getSimpleName(),"Provider fetch error"+e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(Provider provider) {
            super.onPostExecute(provider);
            loggedInProvider = provider;
        }
    }
}