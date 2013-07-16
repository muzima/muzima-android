package com.muzima.view.forms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.muzima.R;
import com.muzima.adapters.NewFormsAdapter;
import com.muzima.controller.FormController;

public class NewFormsListFragment extends FormsListFragment{

    public static NewFormsListFragment newInstance(FormController formController, String noDataMsg, String noDataTip) {
        NewFormsListFragment f = new NewFormsListFragment();
        f.noDataMsg = noDataMsg;
        f.noDataTip = noDataTip;
        f.formController = formController;
        f.setRetainInstance(true);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        listAdapter = new NewFormsAdapter(getActivity(), R.layout.item_forms_list, formController);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

    }
}
