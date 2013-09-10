package com.muzima.view.forms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.muzima.R;
import com.muzima.adapters.forms.CompleteFormsAdapter;
import com.muzima.controller.FormController;

public class CompleteFormsListFragment extends FormsListFragment{

    public static CompleteFormsListFragment newInstance(FormController formController) {
        CompleteFormsListFragment f = new CompleteFormsListFragment();
        f.formController = formController;
        f.setRetainInstance(true);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        listAdapter = new CompleteFormsAdapter(getActivity(), R.layout.item_forms_list, formController);
        noDataMsg = getActivity().getResources().getString(R.string.no_complete_form_msg);
        noDataTip = getActivity().getResources().getString(R.string.no_complete_form_tip);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

    }

}
