/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.adapters.notification.NotificationAdapter;
import com.muzima.controller.NotificationController;
import com.muzima.view.MuzimaListFragment;

public abstract class NotificationListFragment extends MuzimaListFragment implements ListAdapter.BackgroundListQueryTaskListener{

    protected NotificationController notificationController;
    protected FrameLayout progressBarContainer;
    protected LinearLayout noDataView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View notificationsLayout = setupMainView(inflater,container);
        list = (ListView) notificationsLayout.findViewById(R.id.list);
        progressBarContainer = (FrameLayout) notificationsLayout.findViewById(R.id.progressbarContainer);
        noDataView = (LinearLayout) notificationsLayout.findViewById(R.id.no_data_layout);

        setupNoDataView(notificationsLayout);

        // Todo no need to do this check after all list adapters are implemented
        if (listAdapter != null) {
            list.setAdapter(listAdapter);
            list.setOnItemClickListener(this);
            ((NotificationAdapter)listAdapter).setBackgroundListQueryTaskListener(this);
        }
        list.setEmptyView(notificationsLayout.findViewById(R.id.no_data_layout));

        return notificationsLayout;
    }

    protected View setupMainView(LayoutInflater inflater, ViewGroup container){
        return inflater.inflate(R.layout.layout_list, container, false);
    }

    @Override
    public void onQueryTaskStarted() {
        list.setVisibility(View.INVISIBLE);
        noDataView.setVisibility(View.INVISIBLE);
        progressBarContainer.setVisibility(View.VISIBLE);
    }

    @Override
    public void onQueryTaskFinish() {
        list.setVisibility(View.VISIBLE);
        progressBarContainer.setVisibility(View.INVISIBLE);
    }
}
