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
import com.muzima.adapters.MuzimaPagerAdapter;
import com.muzima.api.model.Patient;
import com.muzima.controller.NotificationController;
import com.muzima.view.notifications.PatientNotificationsListFragment;

/**
 * Responsible to hold all the notification fragments as multiple pages/tabs.
 */
public class PatientNotificationPagerAdapter extends MuzimaPagerAdapter {

    private static final int TAB_CONSULTATION = 0;
    private final Patient patient;

    public PatientNotificationPagerAdapter(Context context, FragmentManager supportFragmentManager, Patient patient) {
        super(context, supportFragmentManager);
        this.patient = patient;
    }

    public void initPagerViews(){
        pagers = new PagerView[1];
        NotificationController notificationController = ((MuzimaApplication) context.getApplicationContext()).getNotificationController();

        PatientNotificationsListFragment patientNotificationsListFragment = PatientNotificationsListFragment.newInstance(notificationController, patient);

        pagers[TAB_CONSULTATION] = new PagerView("Consultation", patientNotificationsListFragment);
    }
}
