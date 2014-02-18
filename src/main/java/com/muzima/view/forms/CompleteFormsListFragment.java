package com.muzima.view.forms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import com.muzima.R;
import com.muzima.adapters.forms.CompleteFormsAdapter;
import com.muzima.adapters.forms.FormsAdapter;
import com.muzima.controller.FormController;
import com.muzima.model.CompleteForm;
import com.muzima.model.CompleteFormWithPatientData;

public class CompleteFormsListFragment extends FormsFragmentWithSectionedListAdapter implements FormsAdapter.MuzimaClickListener{

    public static CompleteFormsListFragment newInstance(FormController formController) {
        CompleteFormsListFragment f = new CompleteFormsListFragment();
        f.formController = formController;
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        listAdapter = new CompleteFormsAdapter(getActivity(), R.layout.item_forms_list, formController);
        ((CompleteFormsAdapter)listAdapter).setMuzimaClickListener(this);
        noDataMsg = getActivity().getResources().getString(R.string.no_complete_form_msg);
        noDataTip = getActivity().getResources().getString(R.string.no_complete_form_tip);

        if (actionModeActive) {
            actionMode = getSherlockActivity().startActionMode(new DeleteFormsActionModeCallback());
            actionMode.setTitle(String.valueOf(((CompleteFormsAdapter)listAdapter).getSelectedFormsUuid().size()));
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
    }

    @Override
    protected View setupMainView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.layout_list_with_sections, container, false);
    }

    public void onFormUploadFinish() {
        listAdapter.reloadData();
    }

    @Override
    public boolean onItemLongClick() {
        if (!actionModeActive) {
            actionMode = getSherlockActivity().startActionMode(new DeleteFormsActionModeCallback());
            actionModeActive = true;
        }
        int numOfSelectedForms = ((CompleteFormsAdapter)listAdapter).getSelectedFormsUuid().size();
        if (numOfSelectedForms == 0 && actionModeActive) {
            actionMode.finish();
        }
        actionMode.setTitle(String.valueOf(numOfSelectedForms));
        return false;
    }

    @Override
    public void onItemClick(int position) {
        FormViewIntent intent = new FormViewIntent(getActivity(), (CompleteFormWithPatientData) listAdapter.getItem(position));
        getActivity().startActivityForResult(intent, FormsActivity.FORM_VIEW_ACTIVITY_RESULT);
    }
}
