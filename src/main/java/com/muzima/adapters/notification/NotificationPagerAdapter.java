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
public class NotificationPagerAdapter extends MuzimaPagerAdapter {
    private static final String TAG = "NotificationPagerAdapter";

    public static final int TAB_CONSULTATION = 0;
    private final Patient patient;

    public NotificationPagerAdapter(Context context, FragmentManager supportFragmentManager, Patient patient) {
        super(context, supportFragmentManager);
        this.patient = patient;
    }

    public void initPagerViews(){
        pagers = new PagerView[1];
        NotificationController notificationController = ((MuzimaApplication) context.getApplicationContext()).getNotificationController();

        PatientNotificationsListFragment patientNotificationsListFragment = PatientNotificationsListFragment.newInstance(notificationController, patient);

        pagers[TAB_CONSULTATION] = new PagerView("Consultation", patientNotificationsListFragment);
    }

    public void onNotificationDownloadStart() {
        //((PatientNotificationsListFragment)pagers[TAB_CONSULTATION].fragment).onNotificationDownloadStart();
    }

    public void onNotificationDownloadFinish() {
        //((PatientNotificationsListFragment)pagers[TAB_CONSULTATION].fragment).onNotificationDownloadFinish();
    }
}
