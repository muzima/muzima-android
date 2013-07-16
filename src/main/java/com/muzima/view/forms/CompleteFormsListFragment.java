package com.muzima.view.forms;

import android.view.View;
import android.widget.AdapterView;

import com.muzima.adapters.FormsListAdapter;
import com.muzima.controller.FormController;

public class CompleteFormsListFragment extends FormsListFragment{

    public static CompleteFormsListFragment newInstance(FormController formController, String noDataMsg, String noDataTip) {
        CompleteFormsListFragment f = new CompleteFormsListFragment();
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
