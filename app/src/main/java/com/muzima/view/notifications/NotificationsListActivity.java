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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.MuzimaPagerAdapter;
import com.muzima.adapters.notification.NotificationsPagerAdapter;
import com.muzima.api.model.Cohort;
import com.muzima.api.model.User;
import com.muzima.controller.CohortController;
import com.muzima.utils.Constants;
import com.muzima.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

public class NotificationsListActivity extends NotificationActivityBase {
    private static final String NOTIFICATIONS = "Notifications";
    private MenuItem menubarSyncButton;
    private boolean notificationsSyncInProgress;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_pager);
        initPager();
        initPagerIndicator();
        setTitle(NOTIFICATIONS);

        FrameLayout progressBarContainer = findViewById(R.id.progressbarContainer);
    }

    @Override
    protected MuzimaPagerAdapter createNotificationsPagerAdapter() {
        return new NotificationsPagerAdapter(getApplicationContext(), getSupportFragmentManager());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.notification_list_menu, menu);
        menubarSyncButton = menu.findItem(R.id.menu_load);
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_load:
                if (notificationsSyncInProgress) {
                    Toast.makeText(this, R.string.error_sync_not_allowed, Toast.LENGTH_SHORT).show();
                    return true;
                }

                if (!NetworkUtils.isConnectedToNetwork(this)) {
                    Toast.makeText(this, R.string.error_local_connection_unavailable, Toast.LENGTH_SHORT).show();
                    return true;
                }

                syncAllNotificationsInBackgroundService();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void syncAllNotificationsInBackgroundService() {
        notificationsSyncInProgress = true;
        onNotificationDownloadStart();
        showProgressBar();

        User authenticatedUser = ((MuzimaApplication) getApplicationContext()).getAuthenticatedUser();
        if (authenticatedUser != null) {
            // get downloaded cohorts and sync obs and encounters
            List<String> downloadedCohortsUuid = null;
            List<Cohort> downloadedCohorts;
            CohortController cohortController = ((MuzimaApplication) getApplicationContext()).getCohortController();
            try {
                downloadedCohorts = cohortController.getSyncedCohorts();
                downloadedCohortsUuid = new ArrayList<>();
                for (Cohort cohort : downloadedCohorts) {
                    downloadedCohortsUuid.add(cohort.getUuid());
                }

            } catch (CohortController.CohortFetchException e) {
                e.printStackTrace();
            }
            new SyncNotificationsIntent(this, authenticatedUser.getPerson().getUuid(), getDownloadedCohortsArray(downloadedCohortsUuid)).start();
        } else
            Toast.makeText(this, getString(R.string.error_notification_download), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        int syncType = intent.getIntExtra(Constants.DataSyncServiceConstants.SYNC_TYPE, -1);

        if (syncType == Constants.DataSyncServiceConstants.SYNC_NOTIFICATIONS) {
            hideProgressbar();
            onNotificationDownloadFinish();
        }
    }

    private String[] getDownloadedCohortsArray(List<String> CohortUuids) {
        return CohortUuids.toArray(new String[CohortUuids.size()]);
    }

    private void hideProgressbar() {
        menubarSyncButton.setActionView(null);
    }

    private void showProgressBar() {
        menubarSyncButton.setActionView(R.layout.refresh_menuitem);
    }

    private void onNotificationDownloadFinish() {
        notificationsSyncInProgress = false;
        notificationPagerAdapter.reloadData();
    }

    private void onNotificationDownloadStart() {
        notificationsSyncInProgress = true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        notificationPagerAdapter.reloadData();
    }
}