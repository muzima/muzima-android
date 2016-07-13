/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.adapters.notification;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import com.muzima.MuzimaApplication;
import com.muzima.adapters.MuzimaPagerAdapter;
import com.muzima.api.model.Patient;
import com.muzima.controller.NotificationController;
import com.muzima.view.cohort.AllCohortsListFragment;
import com.muzima.view.notifications.GeneralNotificationsListFragment;
import com.muzima.view.notifications.PatientNotificationsListFragment;
import com.muzima.view.notifications.PatientsNotificationsListFragment;

/**
 * Responsible to hold all the notification fragments as multiple pages/tabs.
 */
public class NotificationsPagerAdapter extends MuzimaPagerAdapter {
    private static final String TAG = "NotificationPagerAdapter";

    public static final int TAB_PATIENT = 0;
    public static final int TAB_GENERAL = 1;

    public NotificationsPagerAdapter(Context context, FragmentManager supportFragmentManager) {
        super(context, supportFragmentManager);
    }

    public void initPagerViews(){

        pagers = new PagerView[2];
        NotificationController notificationController = ((MuzimaApplication) context.getApplicationContext()).getNotificationController();

        PatientsNotificationsListFragment patientsNotificationsListFragment = PatientsNotificationsListFragment.newInstance(notificationController);
        GeneralNotificationsListFragment generalNotificationsListFragment = GeneralNotificationsListFragment.newInstance(notificationController);

        pagers[TAB_PATIENT] = new PagerView("Client Notifications", patientsNotificationsListFragment);
        pagers[TAB_GENERAL] = new PagerView("General Notifications", generalNotificationsListFragment);
    }
}
