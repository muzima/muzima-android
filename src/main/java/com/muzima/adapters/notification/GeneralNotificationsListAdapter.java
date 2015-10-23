/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.adapters.notification;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.muzima.MuzimaApplication;
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
public class GeneralNotificationsListAdapter extends NotificationAdapter {
    private static final String TAG = "GeneralNotificationsListAdapter";

    public GeneralNotificationsListAdapter(Context context, int textViewResourceId, NotificationController notificationController) {
        super(context, textViewResourceId, notificationController);
    }

    @Override
    public void reloadData() {
        new LoadBackgroundQueryTask().execute();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        return view;
    }

    /**
     * Responsible to define contract to PatientNotificationsBackgroundQueryTask.
     */
    public abstract class NotificationsListBackgroundQueryTask extends AsyncTask<Void, Void, List<Notification>> {
        @Override
        protected void onPreExecute() {
            if (backgroundListQueryTaskListener != null) {
                backgroundListQueryTaskListener.onQueryTaskStarted();
            }
        }

        @Override
        protected void onPostExecute(List<Notification> allNotifications) {
            if (allNotifications == null) {
                Toast.makeText(getContext(), "Something went wrong while fetching notifications from local repo", Toast.LENGTH_SHORT).show();
                return;
            }
            //Removes the current items from the list.
            GeneralNotificationsListAdapter.this.clear();
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
    public class LoadBackgroundQueryTask extends NotificationsListBackgroundQueryTask {

        @Override
        protected List<Notification> doInBackground(Void... voids) {
            List<Notification> allNotifications = null;
            List<Notification> filteredNotifications = new ArrayList<Notification>();
            try {
                Log.i(TAG, "Fetching general notifications from Database...");
                User authenticatedUser = ((MuzimaApplication) getContext().getApplicationContext()).getAuthenticatedUser();
                if (authenticatedUser != null) {
                    allNotifications = notificationController.getAllNotificationsByReceiver(authenticatedUser.getPerson().getUuid(), null);
                    PatientController patientController = ((MuzimaApplication) getContext().getApplicationContext()).getPatientController();
                    for (Notification notification : allNotifications) {
                        if (notification.getPatient() != null) {
                            String patientUuid = notification.getPatient().getUuid();
                            if (StringUtils.isEmpty(patientUuid))
                                filteredNotifications.add(notification);
                            else {
                                if (patientController.getPatientByUuid(patientUuid) == null)
                                    filteredNotifications.add(notification);
                            }
                        } else
                            filteredNotifications.add(notification);
                    }
                }
                Log.d(TAG, "#Retrieved " + (allNotifications != null ? allNotifications.size():0) + " notifications from Database." +
                        " And filtered " + (filteredNotifications != null ? filteredNotifications.size():0) + " general notifications");
            } catch (NotificationController.NotificationFetchException e) {
                Log.e(TAG, "Exception occurred while fetching the notifications", e);
            } catch (ParseException e) {
                Log.e(TAG, "Exception occurred while fetching the notifications", e);
            } catch (PatientController.PatientLoadException e) {
                Log.e(TAG, "Exception occurred while fetching the notifications", e);
            }
            return filteredNotifications;
        }
    }
}
