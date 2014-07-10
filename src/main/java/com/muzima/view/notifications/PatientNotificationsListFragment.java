package com.muzima.view.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import com.muzima.R;
import com.muzima.adapters.notification.PatientNotificationsAdapter;
import com.muzima.api.model.Notification;
import com.muzima.api.model.Patient;
import com.muzima.controller.NotificationController;

public class PatientNotificationsListFragment extends NotificationListFragment {
    private static Patient patient;

    public static PatientNotificationsListFragment newInstance(NotificationController notificationController, Patient patient) {
        PatientNotificationsListFragment f = new PatientNotificationsListFragment();
        f.notificationController = notificationController;
        f.patient = patient;
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (listAdapter == null) {
            listAdapter = new PatientNotificationsAdapter(getActivity(), R.layout.item_notifications_list, notificationController, patient);
        }
        noDataMsg = getActivity().getResources().getString(R.string.no_notification_available);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Notification notification = (Notification) listAdapter.getItem(position);
        Intent notificationIntent = new Intent(getActivity(), NotificationActivity.class);
        notificationIntent.putExtra(NotificationActivity.NOTIFICATION, notification);
        notificationIntent.putExtra(NotificationActivity.PATIENT, patient);

        startActivity(notificationIntent);
    }
}
