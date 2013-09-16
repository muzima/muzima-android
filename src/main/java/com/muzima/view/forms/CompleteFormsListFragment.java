package com.muzima.view.forms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import com.muzima.R;
import com.muzima.adapters.forms.CompleteFormsAdapter;
import com.muzima.controller.FormController;
import com.muzima.model.FormWithData;

public class CompleteFormsListFragment extends FormsFragmentWithSectionedListAdapter {

    public static CompleteFormsListFragment newInstance(FormController formController) {
        CompleteFormsListFragment f = new CompleteFormsListFragment();
        f.formController = formController;
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        listAdapter = new CompleteFormsAdapter(getActivity(), R.layout.item_forms_list, formController);
        noDataMsg = getActivity().getResources().getString(R.string.no_complete_form_msg);
        noDataTip = getActivity().getResources().getString(R.string.no_complete_form_tip);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        FormViewIntent intent = new FormViewIntent(getActivity(), (FormWithData) listAdapter.getItem(position));
        getActivity().startActivityForResult(intent, FormsActivity.FORM_VIEW_ACTIVITY_RESULT);
    }

    @Override
    protected View setupMainView(LayoutInflater inflater, ViewGroup container){
        return inflater.inflate(R.layout.layout_list_with_sections, container, false);
    }

}
