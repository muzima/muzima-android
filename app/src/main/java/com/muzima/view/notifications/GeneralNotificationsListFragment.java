/*
 * Copyright (c) 2014 - 2017. The Trustees of Indiana University, Moi University
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
import com.muzima.adapters.notification.GeneralNotificationsListAdapter;
import com.muzima.api.model.Notification;
import com.muzima.controller.NotificationController;

public class GeneralNotificationsListFragment extends NotificationListFragment {

    public static GeneralNotificationsListFragment newInstance(NotificationController notificationController) {
        GeneralNotificationsListFragment f = new GeneralNotificationsListFragment();
        f.notificationController = notificationController;
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (listAdapter == null) {
            listAdapter = new GeneralNotificationsListAdapter(getActivity(), R.layout.item_notifications_list, notificationController);
        }
        noDataMsg = getActivity().getResources().getString(R.string.info_notification_unavailable);
        noDataTip = getActivity().getResources().getString(R.string.hint_notification_sync);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Notification notification = (Notification) listAdapter.getItem(position);
        Intent notificationIntent = new Intent(getActivity(), NotificationActivity.class);
        notificationIntent.putExtra(NotificationActivity.NOTIFICATION, notification);

        startActivityForResult(notificationIntent, 0);
    }
}
