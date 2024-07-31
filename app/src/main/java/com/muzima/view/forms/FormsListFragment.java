/*
 * Copyright (c) The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center. All Rights Reserved.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 * this code in a for-profit venture, please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.os.Bundle;
import androidx.annotation.NonNull;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.muzima.R;
import com.muzima.adapters.forms.FormsAdapter;
import com.muzima.controller.FormController;
import com.muzima.controller.ObservationController;
import com.muzima.view.fragments.MuzimaListFragment;

import com.muzima.adapters.ListAdapter;

public abstract class FormsListFragment extends MuzimaListFragment implements ListAdapter.BackgroundListQueryTaskListener {

    FormController formController;
    ObservationController observationController;
    private FrameLayout progressBarContainer;
    private LinearLayout noDataView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View formsLayout = setupMainView(inflater, container);
        list = formsLayout.findViewById(R.id.list);
        progressBarContainer = formsLayout.findViewById(R.id.progressbarContainer);
        noDataView = formsLayout.findViewById(R.id.no_data_layout);

        setupNoDataView(formsLayout);

        // Todo no need to do this check after all list adapters are implemented
        if (listAdapter != null) {
            list.setAdapter(listAdapter);
            list.setOnItemClickListener(this);

            ((FormsAdapter)listAdapter).setBackgroundListQueryTaskListener(this);
        }
        list.setEmptyView(formsLayout.findViewById(R.id.no_data_layout));

        return formsLayout;
    }

    View setupMainView(LayoutInflater inflater, ViewGroup container){
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

    @Override
    public void onQueryTaskCancelled(){
        FormsAdapter formsAdapter = ((FormsAdapter)listAdapter);
        formsAdapter.cancelBackgroundQueryTask();
    }

    @Override
    public void onQueryTaskCancelled(Object errorDefinition){}

}
