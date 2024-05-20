/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.reports;

import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.adapters.reports.ReportsAdapter;
import com.muzima.controller.PatientReportController;
import com.muzima.view.fragments.MuzimaListFragment;

public abstract class PatientReportListFragment extends MuzimaListFragment implements ListAdapter.BackgroundListQueryTaskListener {

    PatientReportController patientReportController;
    private FrameLayout progressBarContainer;
    private LinearLayout noDataView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View reportsLayout = setupMainView(inflater,container);
        list = reportsLayout.findViewById(R.id.list);
        progressBarContainer = reportsLayout.findViewById(R.id.progressbarContainer);
        noDataView = reportsLayout.findViewById(R.id.no_data_layout);

        setupNoDataView(reportsLayout);

        if (listAdapter != null) {
            list.setAdapter(listAdapter);
            list.setOnItemClickListener(this);
            ((ReportsAdapter)listAdapter).setBackgroundListQueryTaskListener(this);
        }
        list.setEmptyView(reportsLayout.findViewById(R.id.no_data_layout));

        return reportsLayout;
    }

    View setupMainView(LayoutInflater inflater, ViewGroup container){
        return inflater.inflate(R.layout.layout_notifications_list, container, false);
    }

    protected abstract String getSuccessMsg(Integer[] status);

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
