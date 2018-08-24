/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.adapters.notification;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.api.model.Notification;
import com.muzima.api.model.Patient;
import com.muzima.api.model.User;
import com.muzima.controller.NotificationController;

import java.util.List;

/**
 * Responsible to populate all notification fetched from DB in the PatientNotificationsListFragment page.
 */
public class PatientNotificationsAdapter extends NotificationAdapter {
    private final Patient patient;

    public PatientNotificationsAdapter(Context context, int textViewResourceId, NotificationController notificationController, Patient patient) {
        super(context, textViewResourceId, notificationController);
        this.patient = patient;
    }

    @Override
    public void reloadData() {
        new LoadBackgroundQueryTask().execute();
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return super.getView(position, convertView, parent);
    }

    /**
     * Responsible to define contract to PatientNotificationsBackgroundQueryTask.
     */
    abstract class PatientNotificationsBackgroundQueryTask extends AsyncTask<Void, Void, List<Notification>> {
        @Override
        protected void onPreExecute() {
            if (backgroundListQueryTaskListener != null) {
                backgroundListQueryTaskListener.onQueryTaskStarted();
            }
        }

        @Override
        protected void onPostExecute(List<Notification> patientNotifications) {
            if (patientNotifications == null) {
                Toast.makeText(getContext(), getContext().getString(R.string.error_notification_fetch), Toast.LENGTH_SHORT).show();
                return;
            }
            //Removes the current items from the list.
            PatientNotificationsAdapter.this.clear();
            //Adds recently fetched items to the list.
            addAll(patientNotifications);
            //Send a data change request to the list, so the page can be reloaded.
            notifyDataSetChanged();

            if (backgroundListQueryTaskListener != null) {
                backgroundListQueryTaskListener.onQueryTaskFinish();
            }
        }

        protected abstract List<Notification> doInBackground(Void... voids);
    }

    /**
     * Responsible to load notifications from database. Runs in Background.
     */
    protected class LoadBackgroundQueryTask extends PatientNotificationsBackgroundQueryTask {

        @Override
        protected List<Notification> doInBackground(Void... voids) {
            List<Notification> patientNotifications = null;
            try {
                Log.i(getClass().getSimpleName(), "Fetching notifications from Database...");
                User authenticatedUser = ((MuzimaApplication) getContext().getApplicationContext()).getAuthenticatedUser();
                if (authenticatedUser != null)
                    patientNotifications = notificationController.getNotificationsForPatient(patient.getUuid(), authenticatedUser.getPerson().getUuid(), null);
                Log.d(getClass().getSimpleName(), "#Retrieved " + patientNotifications.size() + " notifications from Database.");
            } catch (NotificationController.NotificationFetchException e) {
                Log.w(getClass().getSimpleName(), "Exception occurred while fetching the notifications", e);
            }
            return patientNotifications;
        }
    }
}
