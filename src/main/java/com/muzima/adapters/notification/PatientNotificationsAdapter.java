package com.muzima.adapters.notification;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.muzima.MuzimaApplication;
import com.muzima.api.model.Cohort;
import com.muzima.api.model.Notification;
import com.muzima.api.model.Patient;
import com.muzima.controller.NotificationController;
import com.muzima.service.MuzimaSyncService;

import java.util.List;

/**
 * Responsible to populate all notification fetched from DB in the PatientNotificationsListFragment page.
 */
public class PatientNotificationsAdapter extends NotificationsAdapter {
    private static final String TAG = "PatientNotificationsAdapter";
    private final MuzimaSyncService muzimaSyncService;
    private List<Notification> notifications;
    private Patient patient;

    public PatientNotificationsAdapter(Context context, int textViewResourceId, NotificationController notificationController, Patient patient) {
        super(context, textViewResourceId, notificationController);
        muzimaSyncService = ((MuzimaApplication) (getContext().getApplicationContext())).getMuzimaSyncService();
        this.patient = patient;
    }

    @Override
    public void reloadData() {
        new LoadBackgroundQueryTask().execute();
    }

    public void downloadNotificationsAndReload() {
        new DownloadBackgroundQueryTask().execute();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        return view;
    }

    private void highlightCohorts(Cohort cohort, View view) {
    }

    public void onListItemClick(int position) {
//        Cohort cohort = getItem(position);
//        if (!selectedCohortsUuid.contains(cohort.getUuid())) {
//            selectedCohortsUuid.add(cohort.getUuid());
//        } else if (selectedCohortsUuid.contains(cohort.getUuid())) {
//            selectedCohortsUuid.remove(cohort.getUuid());
//        }
        notifyDataSetChanged();
    }

    /**
     * Responsible to define contract to InboxBackgroundQueryTask.
     */
    public abstract class InboxBackgroundQueryTask extends AsyncTask<Void, Void, List<Notification>> {
        @Override
        protected void onPreExecute() {
            if (backgroundListQueryTaskListener != null) {
                backgroundListQueryTaskListener.onQueryTaskStarted();
            }
        }

        @Override
        protected void onPostExecute(List<Notification> inboxNotifications) {
            notifications = inboxNotifications;
            if (notifications == null) {
                Toast.makeText(getContext(), "Something went wrong while fetching notifications from local repo", Toast.LENGTH_SHORT).show();
                return;
            }
            //Removes the current items from the list.
            PatientNotificationsAdapter.this.clear();
            //Adds recently fetched items to the list.
            addAll(inboxNotifications);
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
    public class LoadBackgroundQueryTask extends InboxBackgroundQueryTask {

        @Override
        protected List<Notification> doInBackground(Void... voids) {
            List<Notification> allNotifications = null;
            try {
                Log.i(TAG, "Fetching inbox notifications from Database...");
                allNotifications = notificationController.getNotificationsForPatient(patient.getUuid());
                Log.d(TAG, "#Retrieved " + allNotifications.size() + " inbox notifications from Database.");
            } catch (NotificationController.NotificationFetchException e) {
                Log.w(TAG, "Exception occurred while fetching the inbox notifications" + e);
            }
            return allNotifications;
        }
    }

    /**
     * Responsible to download Notifications from server and to reload the contents from DB. Runs in BackGround.
     */
    public class DownloadBackgroundQueryTask extends InboxBackgroundQueryTask {
        @Override
        protected List<Notification> doInBackground(Void... voids) {
            List<Notification> allNotifications = null;
            try {
                //muzimaSyncService.downloadCohorts();
                allNotifications = notificationController.getNotificationsForPatient(patient.getUuid());
                Log.i(TAG, "#Inbox: " + allNotifications.size());
            } catch (NotificationController.NotificationFetchException e) {
                Log.w(TAG, "Exception occurred while fetching the inbox notifications" + e);
            }
            return allNotifications;
        }
    }
}
