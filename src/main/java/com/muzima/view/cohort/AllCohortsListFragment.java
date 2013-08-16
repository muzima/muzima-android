package com.muzima.view.cohort;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.muzima.R;
import com.muzima.adapters.forms.DownloadedFormsAdapter;
import com.muzima.api.model.Form;
import com.muzima.controller.CohortController;
import com.muzima.controller.FormController;
import com.muzima.view.forms.FormWebViewActivity;
import com.muzima.view.forms.FormsListFragment;
import com.muzima.view.forms.NewFormsListFragment;

public class AllCohortsListFragment extends CohortListFragment{

    public static AllCohortsListFragment newInstance(CohortController cohortController) {
        AllCohortsListFragment f = new AllCohortsListFragment();
        f.cohortController = cohortController;
        f.setRetainInstance(true);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        noDataMsg = getActivity().getResources().getString(R.string.no_cohorts_available);
        noDataTip = getActivity().getResources().getString(R.string.no_cohorts_available_tip);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
    }

}
