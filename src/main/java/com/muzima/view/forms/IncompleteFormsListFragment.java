package com.muzima.view.forms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import com.muzima.R;
import com.muzima.adapters.forms.IncompleteFormsAdapter;
import com.muzima.controller.FormController;
import com.muzima.model.FormWithData;

public class IncompleteFormsListFragment extends FormsListFragment {

    public static IncompleteFormsListFragment newInstance(FormController formController) {
        IncompleteFormsListFragment f = new IncompleteFormsListFragment();
        f.formController = formController;
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        listAdapter = new IncompleteFormsAdapter(getActivity(), R.layout.item_forms_list, formController);
        noDataMsg = getActivity().getResources().getString(R.string.no_incomplete_form_msg);
        noDataTip = getActivity().getResources().getString(R.string.no_incomplete_form_tip);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        startActivity(new FormViewIntent(getActivity(), (FormWithData) listAdapter.getItem(position)));
    }

    @Override
    protected View setupMainView(LayoutInflater inflater, ViewGroup container){
        return inflater.inflate(R.layout.layout_list_with_sections, container, false);
    }
}
