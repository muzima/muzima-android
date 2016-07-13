/*
 * Copyright (c) 2014. The Trustees of Indiana University.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license with additional
 * healthcare disclaimer. If the user is an entity intending to commercialize any application
 * that uses this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.cohort;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import com.muzima.R;
import com.muzima.adapters.ListAdapter;
import com.muzima.adapters.cohort.CohortsAdapter;
import com.muzima.controller.CohortController;
import com.muzima.view.MuzimaListFragment;

public abstract class CohortListFragment extends MuzimaListFragment implements ListAdapter.BackgroundListQueryTaskListener{
    private static final String TAG = "CohortListFragment";

    protected CohortController cohortController;
    protected FrameLayout progressBarContainer;
    protected LinearLayout noDataView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View formsLayout = setupMainView(inflater,container);
        list = (ListView) formsLayout.findViewById(R.id.list);
        progressBarContainer = (FrameLayout) formsLayout.findViewById(R.id.progressbarContainer);
        noDataView = (LinearLayout) formsLayout.findViewById(R.id.no_data_layout);

        setupNoDataView(formsLayout);

        // Todo no need to do this check after all list adapters are implemented
        if (listAdapter != null) {
            list.setAdapter(listAdapter);
            list.setOnItemClickListener(this);
            ((CohortsAdapter)listAdapter).setBackgroundListQueryTaskListener(this);
        }
        list.setEmptyView(formsLayout.findViewById(R.id.no_data_layout));

        return formsLayout;
    }

    protected View setupMainView(LayoutInflater inflater, ViewGroup container){
        return inflater.inflate(R.layout.layout_list, container, false);
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
}
