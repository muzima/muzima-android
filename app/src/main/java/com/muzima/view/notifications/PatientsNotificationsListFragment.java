/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
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
import com.muzima.adapters.notification.PatientNotificationsListAdapter;
import com.muzima.api.model.Notification;
import com.muzima.api.model.Patient;
import com.muzima.controller.NotificationController;
import com.muzima.view.patients.PatientSummaryActivity;

public class PatientsNotificationsListFragment extends NotificationListFragment {

    public static PatientsNotificationsListFragment newInstance(NotificationController notificationController) {
        PatientsNotificationsListFragment f = new PatientsNotificationsListFragment();
        f.notificationController = notificationController;
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (listAdapter == null) {
            listAdapter = new PatientNotificationsListAdapter(getActivity(), R.layout.item_notifications_list, notificationController);
        }
        noDataMsg = getActivity().getResources().getString(R.string.info_notification_unavailable);
        noDataTip = getActivity().getResources().getString(R.string.hint_notification_sync);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Notification notification = (Notification) listAdapter.getItem(position);
        Patient patient = notification.getPatient();
        Intent intent = new Intent(getActivity(), PatientSummaryActivity.class);
        intent.putExtra(PatientSummaryActivity.PATIENT, patient);
        startActivityForResult(intent, 1);
    }
}
