/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.view.forms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import com.muzima.R;
import com.muzima.adapters.forms.FormsAdapter;
import com.muzima.adapters.forms.IncompleteFormsAdapter;
import com.muzima.controller.FormController;
import com.muzima.model.FormWithData;

public class IncompleteFormsListFragment extends FormsFragmentWithSectionedListAdapter implements FormsAdapter.MuzimaClickListener{

    public static IncompleteFormsListFragment newInstance(FormController formController) {
        IncompleteFormsListFragment f = new IncompleteFormsListFragment();
        f.formController = formController;
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        listAdapter = new IncompleteFormsAdapter(getActivity(), R.layout.item_forms_list_selectable, formController);
        ((IncompleteFormsAdapter)listAdapter).setMuzimaClickListener(this);
        noDataMsg = getActivity().getResources().getString(R.string.info_incomplete_form_unvailable);
        noDataTip = getActivity().getResources().getString(R.string.hint_incomplete_form_unavailable);

        if (actionModeActive) {
            actionMode = getActivity().startActionMode(new DeleteFormsActionModeCallback());
            actionMode.setTitle(String.valueOf(((IncompleteFormsAdapter)listAdapter).getSelectedFormsUuid().size()));
        }

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

    @Override
    public void onItemLongClick() {
        if (!actionModeActive) {
            actionMode = getActivity().startActionMode(new DeleteFormsActionModeCallback());
            actionModeActive = true;
        }
        int numOfSelectedForms = ((IncompleteFormsAdapter)listAdapter).getSelectedFormsUuid().size();
        if (numOfSelectedForms == 0 && actionModeActive) {
            actionMode.finish();
        }
        actionMode.setTitle(String.valueOf(numOfSelectedForms));
    }

    @Override
    public void onItemClick(int position) {
        FormViewIntent intent = new FormViewIntent(getActivity(), (FormWithData) listAdapter.getItem(position));
        getActivity().startActivityForResult(intent, FormsActivity.FORM_VIEW_ACTIVITY_RESULT);
    }
}
