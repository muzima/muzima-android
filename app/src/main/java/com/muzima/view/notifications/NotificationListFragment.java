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

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.adapters.notification.GeneralProvidersListAdapter;
import com.muzima.adapters.notification.NotificationAdapter;
import com.muzima.adapters.providers.ProvidersAdapter;
import com.muzima.controller.NotificationController;
import com.muzima.controller.ProviderController;
import com.muzima.view.MuzimaListFragment;

public abstract class NotificationListFragment extends MuzimaListFragment implements ListAdapter.BackgroundListQueryTaskListener{

    ProviderController providerController;
    NotificationController notificationController;
    private FrameLayout progressBarContainer;
    private LinearLayout noDataView;
    MuzimaApplication muzimaApplication;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View notificationsLayout = setupMainView(inflater,container);
        list = notificationsLayout.findViewById(R.id.list);
        progressBarContainer = notificationsLayout.findViewById(R.id.progressbarContainer);
        noDataView = notificationsLayout.findViewById(R.id.no_data_layout);

        setupNoDataView(notificationsLayout);

        // Todo no need to do this check after all list adapters are implemented
        if (listAdapter != null) {
            list.setAdapter(listAdapter);
            list.setOnItemClickListener(this);
            if (listAdapter instanceof GeneralProvidersListAdapter){
                ((ProvidersAdapter)listAdapter).setBackgroundListQueryTaskListener(this);
            }else {
                ((NotificationAdapter)listAdapter).setBackgroundListQueryTaskListener(this);
            }
        }
        list.setEmptyView(notificationsLayout.findViewById(R.id.no_data_layout));

        return notificationsLayout;
    }

    private View setupMainView(LayoutInflater inflater, ViewGroup container){
        return inflater.inflate(R.layout.layout_notifications_list, container, false);
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

    @Override
    public void onQueryTaskCancelled(){}

    @Override
    public void onQueryTaskCancelled(Object errorDefinition){}
}
