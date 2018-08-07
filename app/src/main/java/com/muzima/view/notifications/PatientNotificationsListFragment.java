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
        PatientNotificationsListFragment.patient = patient;
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (listAdapter == null) {
            listAdapter = new PatientNotificationsAdapter(getActivity(), R.layout.item_notifications_list, notificationController, patient);
        }
        noDataMsg = getActivity().getResources().getString(R.string.info_notification_unavailable);
        super.onCreate(savedInstanceState);
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
