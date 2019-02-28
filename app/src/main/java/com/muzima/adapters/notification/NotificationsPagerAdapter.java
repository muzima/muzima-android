/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.adapters.notification;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.MuzimaPagerAdapter;
import com.muzima.controller.NotificationController;
import com.muzima.view.notifications.GeneralNotificationsListFragment;
import com.muzima.view.notifications.PatientsNotificationsListFragment;

/**
 * Responsible to hold all the notification fragments as multiple pages/tabs.
 */
public class NotificationsPagerAdapter extends MuzimaPagerAdapter {

    private static final int TAB_PATIENT = 1;
    private static final int TAB_GENERAL = 0;

    public NotificationsPagerAdapter(Context context, FragmentManager supportFragmentManager) {
        super(context, supportFragmentManager);
    }

    public void initPagerViews(){

        pagers = new PagerView[2];
        NotificationController notificationController = ((MuzimaApplication) context.getApplicationContext()).getNotificationController();
        MuzimaApplication muzimaApplication = ((MuzimaApplication)context.getApplicationContext());

        PatientsNotificationsListFragment patientsNotificationsListFragment = PatientsNotificationsListFragment.newInstance(notificationController);
        GeneralNotificationsListFragment generalNotificationsListFragment = GeneralNotificationsListFragment.newInstance(muzimaApplication);

        pagers[TAB_GENERAL] = new PagerView(context.getString(R.string.title_general_notifications), generalNotificationsListFragment);
        pagers[TAB_PATIENT] = new PagerView(context.getString(R.string.title_client_notifications), patientsNotificationsListFragment);


    }
}
