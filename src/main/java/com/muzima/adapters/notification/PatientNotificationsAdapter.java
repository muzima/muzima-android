package com.muzima.adapters.notification;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.muzima.MuzimaApplication;
import com.muzima.api.model.Notification;
import com.muzima.api.model.Patient;
import com.muzima.api.model.User;
import com.muzima.controller.NotificationController;

import java.util.List;

/**
 * Responsible to populate all notification fetched from DB in the PatientNotificationsListFragment page.
 */
public class PatientNotificationsAdapter extends NotificationsAdapter {
    private static final String TAG = "PatientNotificationsAdapter";
    private List<Notification> notifications;
    private Patient patient;

    public PatientNotificationsAdapter(Context context, int textViewResourceId, NotificationController notificationController, Patient patient) {
        super(context, textViewResourceId, notificationController);
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

    public void onListItemClick(int position) {
        notifyDataSetChanged();
    }

    /**
     * Responsible to define contract to PatientNotificationsBackgroundQueryTask.
     */
    public abstract class PatientNotificationsBackgroundQueryTask extends AsyncTask<Void, Void, List<Notification>> {
        @Override
        protected void onPreExecute() {
            if (backgroundListQueryTaskListener != null) {
                backgroundListQueryTaskListener.onQueryTaskStarted();
            }
        }

        @Override
        protected void onPostExecute(List<Notification> patientNotifications) {
            notifications = patientNotifications;
            if (notifications == null) {
                Toast.makeText(getContext(), "Something went wrong while fetching notifications from local repo", Toast.LENGTH_SHORT).show();
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
    public class LoadBackgroundQueryTask extends PatientNotificationsBackgroundQueryTask {

        @Override
        protected List<Notification> doInBackground(Void... voids) {
            List<Notification> patientNotifications = null;
            try {
                Log.i(TAG, "Fetching notifications from Database...");
                User authenticatedUser = ((MuzimaApplication) getContext().getApplicationContext()).getAuthenticatedUser();
                if (authenticatedUser != null)
                    patientNotifications = notificationController.getNotificationsForPatient(patient.getUuid(), authenticatedUser.getPerson().getUuid(), null);
                Log.d(TAG, "#Retrieved " + patientNotifications.size() + " notifications from Database.");
            } catch (NotificationController.NotificationFetchException e) {
                Log.w(TAG, "Exception occurred while fetching the notifications", e);
            }
            return patientNotifications;
        }
    }

    /**
     * Responsible to download Notifications from server and to reload the contents from DB. Runs in BackGround.
     */
    public class DownloadBackgroundQueryTask extends PatientNotificationsBackgroundQueryTask {
        @Override
        protected List<Notification> doInBackground(Void... voids) {
            List<Notification> patientNotifications = null;
            try {
                User authenticatedUser = ((MuzimaApplication) getContext().getApplicationContext()).getAuthenticatedUser();
                if (authenticatedUser != null)
                    patientNotifications = notificationController.getNotificationsForPatient(patient.getUuid(),authenticatedUser.getPerson().getUuid(), null);
                Log.i(TAG, "#Retrieved: " + patientNotifications.size());
            } catch (NotificationController.NotificationFetchException e) {
                Log.w(TAG, "Exception occurred while fetching the notifications", e);
            }
            return patientNotifications;
        }
    }
}
