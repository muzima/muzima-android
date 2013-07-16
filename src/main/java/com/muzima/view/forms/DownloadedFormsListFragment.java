package com.muzima.view.forms;

import android.view.View;
import android.widget.AdapterView;

import com.muzima.adapters.FormsListAdapter;
import com.muzima.controller.FormController;

public class DownloadedFormsListFragment extends FormsListFragment{

    public static DownloadedFormsListFragment newInstance(FormController formController, String noDataMsg, String noDataTip) {
        DownloadedFormsListFragment f = new DownloadedFormsListFragment();
        f.noDataMsg = noDataMsg;
        f.noDataTip = noDataTip;
        f.listAdapter = null;
        f.setRetainInstance(true);
        return f;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

    }
}
