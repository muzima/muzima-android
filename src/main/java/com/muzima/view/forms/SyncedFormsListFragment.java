package com.muzima.view.forms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.muzima.R;
import com.muzima.controller.FormController;

public class SyncedFormsListFragment extends FormsListFragment{

    public static SyncedFormsListFragment newInstance(FormController formController) {
        SyncedFormsListFragment f = new SyncedFormsListFragment();
        f.formController = formController;
        f.setRetainInstance(true);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        listAdapter = null;
        noDataMsg = getActivity().getResources().getString(R.string.no_synced_form_msg);
        noDataTip = getActivity().getResources().getString(R.string.no_synced_form_tip);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

    }
}
