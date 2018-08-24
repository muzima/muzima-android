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
import com.muzima.api.model.User;
import com.muzima.controller.NotificationController;
import com.muzima.controller.PatientController;
import com.muzima.utils.StringUtils;
import org.apache.lucene.queryParser.ParseException;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible to populate all notification fetched from DB in the PatientNotificationsListFragment page.
 */
public class PatientNotificationsListAdapter extends NotificationAdapter {

    public PatientNotificationsListAdapter(Context context, int textViewResourceId, NotificationController notificationController) {
        super(context, textViewResourceId, notificationController);
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
    abstract class NotificationsListBackgroundQueryTask extends AsyncTask<Void, Void, List<Notification>> {
        @Override
        protected void onPreExecute() {
            if (backgroundListQueryTaskListener != null) {
                backgroundListQueryTaskListener.onQueryTaskStarted();
            }
        }

        @Override
        protected void onPostExecute(List<Notification> allNotifications) {
            if (allNotifications == null) {
                Toast.makeText(getContext(), getContext().getString(R.string.error_notification_fetch), Toast.LENGTH_SHORT).show();
                return;
            }
            //Removes the current items from the list.
            PatientNotificationsListAdapter.this.clear();
            //Adds recently fetched items to the list.
            addAll(allNotifications);
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
    protected class LoadBackgroundQueryTask extends NotificationsListBackgroundQueryTask {

        @Override
        protected List<Notification> doInBackground(Void... voids) {
            List<Notification> allNotifications = null;
            List<Notification> filteredNotifications = new ArrayList<>();
            try {
                Log.i(getClass().getSimpleName(), "Fetching patient notifications from Database...");
                User authenticatedUser = ((MuzimaApplication) getContext().getApplicationContext()).getAuthenticatedUser();
                if (authenticatedUser != null) {
                    allNotifications = notificationController.getAllNotificationsByReceiver(authenticatedUser.getPerson().getUuid(), null);
                    PatientController patientController = ((MuzimaApplication) getContext().getApplicationContext()).getPatientController();
                    for (Notification notification : allNotifications) {
                        if (notification.getPatient() != null) {
                            String patientUuid = notification.getPatient().getUuid();
                            if (!StringUtils.isEmpty(patientUuid)) {
                                if (patientController.getPatientByUuid(patientUuid) != null)
                                    filteredNotifications.add(notification);
                            }
                        }
                    }
                }
                Log.d(getClass().getSimpleName(), "#Retrieved " + (allNotifications != null ? allNotifications.size():0) + " notifications from Database." +
                        " And filtered " + (filteredNotifications != null ? filteredNotifications.size():0) + " client notifications");
            } catch (NotificationController.NotificationFetchException | PatientController.PatientLoadException e) {
                Log.e(getClass().getSimpleName(), "Exception occurred while fetching the notifications", e);
            }
            return filteredNotifications;
        }
    }
}
